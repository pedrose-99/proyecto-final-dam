import { Injectable } from '@angular/core';
import { Apollo } from 'apollo-angular';
import { Observable, map, of, forkJoin } from 'rxjs';
import { Product, ProductPage, ProductSearchResult, ProductFilters } from '../../core/models/product.model';
import { Category } from '../../core/models/category.model';
import { Store } from '../../core/models/store.model';
import {
  GET_ALL_PRODUCTS,
  GET_ALL_CATEGORIES,
  GET_ALL_STORES,
  GET_PRODUCTS_BY_CATEGORY,
  GET_PRODUCTS_BY_STORE,
  GET_STORES_BY_PRODUCT,
  SEARCH_PRODUCTS,
  ADD_TO_FAVORITES,
  REMOVE_FROM_FAVORITES,
  SEARCH_PRODUCTS_BY_STORE
} from '../../core/graphql/queries';

@Injectable({
  providedIn: 'root'
})
export class ProductService {

  constructor(private apollo: Apollo) {}

  clearCache(): void {
  }

  getProducts(filters: ProductFilters, page: number = 0, size: number = 24): Observable<ProductPage> {

    // Prioridad 1: Si hay búsqueda Y una sola tienda
    if (filters.search && filters.storeIds?.length === 1) {
      return this.apollo.query<any>({
        query: SEARCH_PRODUCTS_BY_STORE,
        variables: {
          query: filters.search,
          storeId: filters.storeIds[0].toString(),
          page: page,
          size: size
        },
        fetchPolicy: 'network-only'
      }).pipe(
        map(result => this.mapGraphQLPageToProductPage(result.data?.searchProductsByStore, filters))
      );
    }
    // Prioridad 2: Si hay búsqueda Y múltiples tiendas, buscar en cada una y combinar
    else if (filters.search && filters.storeIds && filters.storeIds.length > 1) {
      const queries = filters.storeIds.map(storeId =>
        this.apollo.query<any>({
          query: SEARCH_PRODUCTS_BY_STORE,
          variables: {
            query: filters.search,
            storeId: storeId.toString(),
            page: 0,
            size: 200
          },
          fetchPolicy: 'network-only'
        })
      );

      return forkJoin(queries).pipe(
        map(results => {
          let allProducts: Product[] = [];
          const seenIds = new Set<number>();

          results.forEach(result => {
            const products = (result.data?.searchProductsByStore?.content || [])
              .map((p: any) => this.mapToProduct(p));
            products.forEach((p: Product) => {
              if (!seenIds.has(p.id)) {
                seenIds.add(p.id);
                allProducts.push(p);
              }
            });
          });

          const sorted = this.applySorting(allProducts, filters.sortBy);
          const totalElements = sorted.length;
          const start = page * size;
          const end = start + size;

          return {
            content: sorted.slice(start, end),
            totalElements,
            totalPages: Math.ceil(totalElements / size),
            size,
            number: page,
            first: page === 0,
            last: end >= totalElements
          };
        })
      );
    }
    // Prioridad 3: Solo búsqueda
    else if (filters.search) {
      return this.apollo.query<any>({
        query: SEARCH_PRODUCTS,
        variables: { query: filters.search, page: page, size: size },
        fetchPolicy: 'network-only'
      }).pipe(
        map(result => this.mapGraphQLPageToProductPage(result.data?.searchProducts, filters))
      );
    }
    // Prioridad 4: Una sola tienda (sin búsqueda)
    else if (filters.storeIds && filters.storeIds.length === 1) {
      return this.apollo.query<any>({
        query: GET_PRODUCTS_BY_STORE,
        variables: {
          storeId: filters.storeIds[0].toString(),
          page: page,
          size: size
        },
        fetchPolicy: 'network-only'
      }).pipe(
        map(result => this.mapGraphQLPageToProductPage(result.data?.productsByStore, filters))
      );
    }
    // Prioridad 5: Múltiples tiendas (sin búsqueda)
    else if (filters.storeIds && filters.storeIds.length > 1) {
      const queries = filters.storeIds.map(storeId =>
        this.apollo.query<any>({
          query: GET_PRODUCTS_BY_STORE,
          variables: {
            storeId: storeId.toString(),
            page: 0,
            size: 200
          },
          fetchPolicy: 'network-only'
        })
      );

      return forkJoin(queries).pipe(
        map(results => {
          let allProducts: Product[] = [];
          const seenIds = new Set<number>();

          results.forEach(result => {
            const products = (result.data?.productsByStore?.content || [])
              .map((p: any) => this.mapToProduct(p));
            products.forEach((p: Product) => {
              if (!seenIds.has(p.id)) {
                seenIds.add(p.id);
                allProducts.push(p);
              }
            });
          });

          const sorted = this.applySorting(allProducts, filters.sortBy);
          const totalElements = sorted.length;
          const start = page * size;
          const end = start + size;

          return {
            content: sorted.slice(start, end),
            totalElements,
            totalPages: Math.ceil(totalElements / size),
            size,
            number: page,
            first: page === 0,
            last: end >= totalElements
          };
        })
      );
    }
    // Prioridad 6: Categoría(s)
    else if ((filters.categoryIds && filters.categoryIds.length > 0) || filters.categoryId) {
      const catIds = filters.categoryIds && filters.categoryIds.length > 0
        ? filters.categoryIds
        : [filters.categoryId!];

      if (catIds.length === 1) {
        return this.apollo.query<any>({
          query: GET_PRODUCTS_BY_CATEGORY,
          variables: {
            categoryId: catIds[0].toString(),
            page: page,
            size: size
          },
          fetchPolicy: 'network-only'
        }).pipe(
          map(result => this.mapGraphQLPageToProductPage(result.data?.productsByCategoryPaginated, filters))
        );
      }

      const queries = catIds.map(catId =>
        this.apollo.query<any>({
          query: GET_PRODUCTS_BY_CATEGORY,
          variables: {
            categoryId: catId.toString(),
            page: 0,
            size: 200
          },
          fetchPolicy: 'network-only'
        })
      );

      return forkJoin(queries).pipe(
        map(results => {
          let allProducts: Product[] = [];
          const seenIds = new Set<number>();

          results.forEach(result => {
            const products = (result.data?.productsByCategoryPaginated?.content || [])
              .map((p: any) => this.mapToProduct(p));
            products.forEach((p: Product) => {
              if (!seenIds.has(p.id)) {
                seenIds.add(p.id);
                allProducts.push(p);
              }
            });
          });

          const sorted = this.applySorting(allProducts, filters.sortBy);
          const totalElements = sorted.length;
          const start = page * size;
          const end = start + size;

          return {
            content: sorted.slice(start, end),
            totalElements,
            totalPages: Math.ceil(totalElements / size),
            size,
            number: page,
            first: page === 0,
            last: end >= totalElements
          };
        })
      );
    }
    // Sin filtros: paginación directa del servidor
    else {
      return this.apollo.query<any>({
        query: GET_ALL_PRODUCTS,
        variables: { page: page, size: size },
        fetchPolicy: 'network-only'
      }).pipe(
        map(result => this.mapGraphQLPageToProductPage(result.data?.allProducts, filters))
      );
    }
  }

  private mapGraphQLPageToProductPage(graphqlPage: any, filters: ProductFilters): ProductPage {
    if (!graphqlPage) {
      return {
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: 24,
        number: 0,
        first: true,
        last: true
      };
    }

    let content = (graphqlPage.content || []).map((p: any) => this.mapToProduct(p));

    // Aplicar ordenamiento en cliente si no es relevancia
    content = this.applySorting(content, filters.sortBy);

    return {
      content,
      totalElements: graphqlPage.totalElements,
      totalPages: graphqlPage.totalPages,
      size: graphqlPage.size,
      number: graphqlPage.number,
      first: graphqlPage.first,
      last: graphqlPage.last
    };
  }

  private applySorting(products: Product[], sortBy?: string): Product[] {
    if (!sortBy || sortBy === 'relevance') {
      return products;
    }

    const sorted = [...products];
    switch (sortBy) {
      case 'name_asc':
        sorted.sort((a, b) => (a.name || '').localeCompare(b.name || ''));
        break;
      case 'name_desc':
        sorted.sort((a, b) => (b.name || '').localeCompare(a.name || ''));
        break;
    }
    return sorted;
  }

  private normalizeText(value: string): string {
    return (value || '')
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '');
  }

  private filterByCategoryNames(products: Product[], categoryNames: string[]): Product[] {
    const normalizedNames = categoryNames
      .map(name => this.normalizeText(name))
      .filter(name => name.length > 0);

    if (normalizedNames.length === 0) {
      return products;
    }

    return products.filter(product => {
      const category = this.normalizeText(product.categoryName || '');
      // Coincidencia exacta del nombre completo de la categoría
      return normalizedNames.some(name => category.includes(name));
    });
  }

  private mapToProduct(data: any): Product {
    return {
      id: data.productId,
      name: data.name || '',
      brand: data.brand || null,
      imageUrl: data.imageUrl || null,
      categoryName: data.categoryName || '',
      isFavorite: data.isFavorite || false
    };
  }

  searchProductsByStore(query: string, storeId: number, limit: number = 10): Observable<ProductSearchResult[]> {
    if (!query || query.length < 2) {
      return of([]);
    }

    return this.apollo.query<any>({
      query: SEARCH_PRODUCTS_BY_STORE,
      variables: { query, storeId: storeId.toString(), page: 0, size: limit },
      fetchPolicy: 'network-only'
    }).pipe(
      map(result => {
        const products = result.data?.searchProductsByStore?.content || [];
        return products.map((p: any) => ({
          id: p.productId,
          name: p.name || '',
          brand: p.brand || null,
          imageUrl: p.imageUrl || null,
          categoryName: p.categoryName || '',
          currentPrice: p.currentPrice ?? null
        }));
      })
    );
  }

  searchProducts(query: string, limit: number = 5): Observable<ProductSearchResult[]> {
    if (!query || query.length < 2) {
      return of([]);
    }

    return this.apollo.query<any>({
      query: SEARCH_PRODUCTS,
      variables: { query: query, page: 0, size: limit },
      fetchPolicy: 'network-only'
    }).pipe(
      map(result => {
        const products = result.data?.searchProducts?.content || [];
        return products.map((p: any) => ({
          id: p.productId,
          name: p.name || '',
          brand: p.brand || null,
          imageUrl: p.imageUrl || null,
          categoryName: p.categoryName || ''
        }));
      })
    );
  }

  getCategories(): Observable<Category[]> {
    return this.apollo.query<any>({
      query: GET_ALL_CATEGORIES,
      fetchPolicy: 'network-only'
    }).pipe(
      map(result => {
        console.log('GraphQL allCategories response:', result);
        const categories = result.data?.allCategories || [];
        return categories.map((c: any) => ({
          id: parseInt(c.categoryId) || 0,
          name: c.name || '',
          parentId: null,
          productCount: 0
        }));
      })
    );
  }

  getStores(): Observable<Store[]> {
    return this.apollo.query<any>({
      query: GET_ALL_STORES,
      fetchPolicy: 'network-only'
    }).pipe(
      map(result => {
        const stores = result.data?.allStores || [];
        return stores.map((s: any) => ({
          id: parseInt(s.storeId) || 0,
          name: s.name || '',
          logoUrl: s.logo || null,
          productCount: s.productCount || 0
        }));
      })
    );
  }

  getProductStores(productId: number): Observable<any[]> {
    return this.apollo.watchQuery<any>({
      query: GET_STORES_BY_PRODUCT,
      variables: { productId: productId.toString() }
    }).valueChanges.pipe(
      map(result => result.data.storesByProduct || [])
    );
  }

  addToFavorites(productId: number): Observable<void> {
    return this.apollo.mutate<any>({
      mutation: ADD_TO_FAVORITES,
      variables: { productId: productId.toString() }
    }).pipe(
      map(() => void 0)
    );
  }

  removeFromFavorites(productId: number): Observable<void> {
    return this.apollo.mutate<any>({
      mutation: REMOVE_FROM_FAVORITES,
      variables: { productId: productId.toString() }
    }).pipe(
      map(() => void 0)
    );
  }

}
