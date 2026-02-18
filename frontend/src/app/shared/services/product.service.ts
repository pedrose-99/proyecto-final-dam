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
  REMOVE_FROM_FAVORITES
  SEARCH_PRODUCTS_BY_STORE
} from '../../core/graphql/queries';

@Injectable({
  providedIn: 'root'
})
export class ProductService {

  constructor(private apollo: Apollo) {}

  getProducts(filters: ProductFilters, page: number = 0, size: number = 24): Observable<ProductPage> {
    // Si hay filtro por UNA sola tienda, usar paginacion del servidor directamente
    if (filters.storeIds && filters.storeIds.length === 1) {
      return this.apollo.query<any>({
        query: GET_PRODUCTS_BY_STORE,
        variables: {
          storeId: filters.storeIds[0].toString(),
          page: page,
          size: size
        },
        fetchPolicy: 'network-only'
      }).pipe(
        map(result => {
          return this.mapGraphQLPageToProductPage(result.data?.productsByStore, filters);
        })
      );
    }

    // Si hay filtro por MULTIPLES tiendas, cargar todos de cada tienda y combinar
    if (filters.storeIds && filters.storeIds.length > 1) {
      const queries = filters.storeIds.map(storeId =>
        this.apollo.query<any>({
          query: GET_PRODUCTS_BY_STORE,
          variables: {
            storeId: storeId.toString(),
            page: 0,
            size: 10000
          },
          fetchPolicy: 'network-only'
        })
      );

      return forkJoin(queries).pipe(
        map(results => {
          // Combinar productos de todas las tiendas
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

          // Aplicar ordenamiento
          allProducts = this.applySorting(allProducts, filters.sortBy);

          // Aplicar paginación manual
          const totalElements = allProducts.length;
          const start = page * size;
          const end = start + size;
          const paginatedContent = allProducts.slice(start, end);

          return {
            content: paginatedContent,
            totalElements: totalElements,
            totalPages: Math.ceil(totalElements / size),
            size: size,
            number: page,
            first: page === 0,
            last: end >= totalElements
          };
        })
      );
    }

    // Si hay filtro por múltiples categorías, hacer queries paralelas y combinar
    if (filters.categoryIds && filters.categoryIds.length > 0) {
      const queries = filters.categoryIds.map(catId =>
        this.apollo.query<any>({
          query: GET_PRODUCTS_BY_CATEGORY,
          variables: {
            categoryId: catId.toString(),
            page: 0,
            size: 200 // Cargar suficientes de cada categoría
          },
          fetchPolicy: 'network-only'
        })
      );

      return forkJoin(queries).pipe(
        map(results => {
          // Combinar productos de todas las categorías
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

          // Aplicar ordenamiento
          allProducts = this.applySorting(allProducts, filters.sortBy);

          // Aplicar paginación manual
          const totalElements = allProducts.length;
          const start = page * size;
          const end = start + size;
          const paginatedContent = allProducts.slice(start, end);

          return {
            content: paginatedContent,
            totalElements: totalElements,
            totalPages: Math.ceil(totalElements / size),
            size: size,
            number: page,
            first: page === 0,
            last: end >= totalElements
          };
        })
      );
    }

    // Si hay filtro por una sola categoria, usar query especifica
    if (filters.categoryId) {
      return this.apollo.query<any>({
        query: GET_PRODUCTS_BY_CATEGORY,
        variables: {
          categoryId: filters.categoryId.toString(),
          page: page,
          size: size
        },
        fetchPolicy: 'network-only'
      }).pipe(
        map(result => {
          return this.mapGraphQLPageToProductPage(result.data?.productsByCategoryPaginated, filters);
        })
      );
    }

    // Si hay búsqueda por texto, usar query de búsqueda del servidor
    if (filters.search) {
      return this.apollo.query<any>({
        query: SEARCH_PRODUCTS,
        variables: { query: filters.search, page: page, size: size },
        fetchPolicy: 'network-only'
      }).pipe(
        map(result => {
          return this.mapGraphQLPageToProductPage(result.data?.searchProducts, filters);
        })
      );
    }

    // Query general de todos los productos
    console.log('[DEBUG] Enviando query allProducts con page:', page, 'size:', size);
    return this.apollo.query<any>({
      query: GET_ALL_PRODUCTS,
      variables: { page: page, size: size },
      fetchPolicy: 'network-only'
    }).pipe(
      map(result => {
        console.log('[DEBUG] Respuesta allProducts:', result);
        const mapped = this.mapGraphQLPageToProductPage(result.data?.allProducts, filters);
        console.log('[DEBUG] Productos mapeados:', mapped.content.length, 'de', mapped.totalElements);
        return mapped;
      })
    );
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

  // Estas funciones necesitaran mutations en el backend
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

  addToList(productId: number, listId?: number): Observable<void> {
    // TODO: Implementar mutation GraphQL
    console.log('addToList - pendiente mutation GraphQL', productId, listId);
    return of(void 0);
  }
}
