import { Injectable } from '@angular/core';
import { Apollo } from 'apollo-angular';
import { Observable, map, of, forkJoin, switchMap, catchError } from 'rxjs';
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

  private productCache = new Map<string, { data: Product[], timestamp: number }>();
  private CACHE_TTL = 5 * 60 * 1000; // 5 minutos

  constructor(private apollo: Apollo) {}

  clearCache(): void {
    this.productCache.clear();
  }

  private getCacheKey(filters: ProductFilters): string {
    return JSON.stringify({
      search: filters.search,
      storeIds: filters.storeIds,
      categoryId: filters.categoryId,
      categoryIds: filters.categoryIds
    });
  }

  getProducts(filters: ProductFilters, page: number = 0, size: number = 24): Observable<ProductPage> {
    let baseQuery$: Observable<Product[]>;

    // Prioridad 1: Búsqueda + 1 tienda
    if (filters.search && filters.storeIds?.length === 1) {
      baseQuery$ = this.apollo.query<any>({
        query: SEARCH_PRODUCTS_BY_STORE,
        variables: { 
          query: filters.search, 
          storeId: filters.storeIds[0].toString(),
          page: 0, 
          size: 1000 
        },
        fetchPolicy: 'network-only'
      }).pipe(
        map(result => (result.data?.searchProductsByStore?.content || []).map((p: any) => this.mapToProduct(p)))
      );
    }
    // Prioridad 2: Búsqueda + múltiples tiendas
    else if (filters.search && filters.storeIds && filters.storeIds.length > 1) {
      const queries = filters.storeIds.map(storeId =>
        this.apollo.query<any>({
          query: SEARCH_PRODUCTS_BY_STORE,
          variables: {
            query: filters.search,
            storeId: storeId.toString(),
            page: 0,
            size: 1000
          },
          fetchPolicy: 'network-only'
        })
      );

      baseQuery$ = forkJoin(queries).pipe(
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
          return allProducts;
        })
      );
    }
    // Prioridad 3: Búsqueda + categorías
    else if (filters.search && filters.categoryIds && filters.categoryIds.length > 0) {
      if (filters.categoryNames && filters.categoryNames.length > 0) {
        baseQuery$ = this.apollo.query<any>({
          query: GET_ALL_PRODUCTS,
          variables: { page: 0, size: 10000 },
          fetchPolicy: 'network-only'
        }).pipe(
          map(result => (result.data?.allProducts?.content || []).map((p: any) => this.mapToProduct(p))),
          map(products => {
            const categoryFiltered = this.filterByCategoryNames(products, filters.categoryNames || []);
            const query = this.normalizeText(filters.search || '');
            return categoryFiltered.filter(p => {
              const productText = this.normalizeText(`${p.name} ${p.brand || ''}`);
              return productText.includes(query);
            });
          })
        );
      } else {
        const queries = filters.categoryIds.map(catId =>
          this.apollo.query<any>({
            query: GET_PRODUCTS_BY_CATEGORY,
            variables: {
              categoryId: catId.toString(),
              page: 0,
              size: 10000
            },
            fetchPolicy: 'network-only'
          })
        );

        baseQuery$ = forkJoin(queries).pipe(
          map(results => {
            let allProductsInCategory: Product[] = [];
            const seenIds = new Set<number>();

            results.forEach(result => {
              const products = (result.data?.productsByCategoryPaginated?.content || [])
                .map((p: any) => this.mapToProduct(p));
              products.forEach((p: Product) => {
                if (!seenIds.has(p.id)) {
                  seenIds.add(p.id);
                  allProductsInCategory.push(p);
                }
              });
            });
            
            // Filtrar por búsqueda en cliente
            return allProductsInCategory.filter(p => {
              const name = (p.name || '').toLowerCase();
              const brand = (p.brand || '').toLowerCase();
              const query = (filters.search || '').toLowerCase();
              return name.includes(query) || brand.includes(query);
            });
          })
        );
      }
    }
    // Prioridad 4: Solo búsqueda - intentar búsqueda específica por tienda
    else if (filters.search) {
      baseQuery$ = this.getStores().pipe(
        switchMap(stores => {
          const storeIds = stores.map(s => s.id);
          console.log('[DEBUG-SEARCH] Buscando "' + filters.search + '" en tiendas:', storeIds);
          
          if (storeIds.length === 0) {
            console.log('[DEBUG-SEARCH] No hay tiendas disponibles');
            return of([]);
          }
          
          // Crear consultas SEARCH_PRODUCTS_BY_STORE para TODAS las tiendas en paralelo
          const queries = storeIds.map(storeId =>
            this.apollo.query<any>({
              query: SEARCH_PRODUCTS_BY_STORE,
              variables: {
                query: filters.search,
                storeId: storeId.toString(),
                page: 0,
                size: 1000
              },
              fetchPolicy: 'network-only'
            }).pipe(
              catchError(err => {
                console.warn(`[DEBUG-SEARCH] Error en tienda ${storeId}:`, err);
                return of({ data: { searchProductsByStore: { content: [] } } });
              })
            )
          );

          return forkJoin(queries).pipe(
            map(results => {
              let allProducts: Product[] = [];
              const seenIds = new Set<number>();

              results.forEach((result, index) => {
                const storeId = storeIds[index];
                const products = (result.data?.searchProductsByStore?.content || [])
                  .map((p: any) => this.mapToProduct(p));
                console.log(`[DEBUG-SEARCH] Tienda ${storeId}: ${products.length} productos`);
                
                products.forEach((p: Product) => {
                  if (!seenIds.has(p.id)) {
                    seenIds.add(p.id);
                    allProducts.push(p);
                  }
                });
              });
              
              console.log('[DEBUG-SEARCH] Total productos únicos: ' + allProducts.length);
              return allProducts;
            })
          );
        })
      );
    }
    // Prioridad 5: Tienda(s) + Categoría(s) - sin búsqueda
    else if ((filters.storeIds && filters.storeIds.length > 0) && (filters.categoryIds && filters.categoryIds.length > 0)) {
      // Estrategia: obtener productos de categoría(s) y verificar que estén en tienda(s)
      const categoryQueries = filters.categoryIds.map(catId =>
        this.apollo.query<any>({
          query: GET_PRODUCTS_BY_CATEGORY,
          variables: {
            categoryId: catId.toString(),
            page: 0,
            size: 10000
          },
          fetchPolicy: 'network-only'
        })
      );

      baseQuery$ = forkJoin(categoryQueries).pipe(
        // Primero obtener productos de categoría
        switchMap(categoryResults => {
          // Productos de todas las categorías
          const categoryProducts: Product[] = [];
          const categoryProductIds = new Set<number>();

          categoryResults.forEach(result => {
            const products = (result.data?.productsByCategoryPaginated?.content || [])
              .map((p: any) => this.mapToProduct(p));
            products.forEach((p: Product) => {
              if (!categoryProductIds.has(p.id)) {
                categoryProductIds.add(p.id);
                categoryProducts.push(p);
              }
            });
          });

          // Ahora obtener productos de tienda(s) para intersección
          const storeQueries = filters.storeIds!.map(storeId =>
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

          return forkJoin(storeQueries).pipe(
            map(storeResults => {
              // IDs de productos en las tiendas
              const storeProductIds = new Set<number>();
              storeResults.forEach(result => {
                const products = (result.data?.productsByStore?.content || [])
                  .map((p: any) => this.mapToProduct(p));
                products.forEach((p: Product) => {
                  storeProductIds.add(p.id);
                });
              });

              // Retornar productos de categoría que existen en tiendas
              return categoryProducts.filter(p => storeProductIds.has(p.id));
            })
          );
        })
      );
    }
    // Prioridad 6: Múltiples tiendas (sin categoría, sin búsqueda)
    else if (filters.storeIds && filters.storeIds.length > 1) {
      const queries = filters.storeIds.map(storeId =>
        this.apollo.query<any>({
          query: GET_PRODUCTS_BY_STORE,
          variables: {
            storeId: storeId.toString(),
            page: 0,
            size: 50000
          },
          fetchPolicy: 'network-only'
        })
      );

      baseQuery$ = forkJoin(queries).pipe(
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
          return allProducts;
        })
      );
    }
    // Prioridad 7: Una tienda (sin categoría, sin búsqueda)
    else if (filters.storeIds && filters.storeIds.length === 1) {
      baseQuery$ = this.apollo.query<any>({
        query: GET_PRODUCTS_BY_STORE,
        variables: {
          storeId: filters.storeIds[0].toString(),
          page: 0,
          size: 50000
        },
        fetchPolicy: 'network-only'
      }).pipe(
        map(result => (result.data?.productsByStore?.content || []).map((p: any) => this.mapToProduct(p)))
      );
    }
    // Prioridad 8: Múltiples categorías (sin tienda, sin búsqueda)
    else if (filters.categoryIds && filters.categoryIds.length > 0) {
      // Obtener TODOS los productos y filtrar por nombre de categoría
      // Esto permite búsqueda parcial y encuentra productos de todas las tiendas
      // con categorías similares (ej: "Tomate frito" incluye "Tomate")
      baseQuery$ = this.apollo.query<any>({
        query: GET_ALL_PRODUCTS,
        variables: { page: 0, size: 10000 },
        fetchPolicy: 'network-only'
      }).pipe(
        map(result => (result.data?.allProducts?.content || []).map((p: any) => this.mapToProduct(p))),
        map(products => {
          if (!filters.categoryNames || filters.categoryNames.length === 0) {
            return products;
          }
          
          const filtered = this.filterByCategoryNames(products, filters.categoryNames);
          console.log('[DEBUG-CATEGORY] Total productos filtrados por categoría:', filtered.length);
          return filtered;
        })
      );
    }
    // Prioridad 9: Una categoría (sin tienda, sin búsqueda)
    else if (filters.categoryId) {
      baseQuery$ = this.apollo.query<any>({
        query: GET_PRODUCTS_BY_CATEGORY,
        variables: {
          categoryId: filters.categoryId.toString(),
          page: 0,
          size: 50000
        },
        fetchPolicy: 'network-only'
      }).pipe(
        map(result => (result.data?.productsByCategoryPaginated?.content || []).map((p: any) => this.mapToProduct(p)))
      );
    }
    // Prioridad 10: Sin filtros
    else {
      baseQuery$ = this.apollo.query<any>({
        query: GET_ALL_PRODUCTS,
        variables: { page: 0, size: 50000 },
        fetchPolicy: 'network-only'
      }).pipe(
        map(result => (result.data?.allProducts?.content || []).map((p: any) => this.mapToProduct(p)))
      );
    }

    // Aplicar ordenamiento, filtrado y paginación
    return baseQuery$.pipe(
      tap(allProducts => {
        // Guardar en caché antes de paginar
        this.productCache.set(cacheKey, { data: allProducts, timestamp: Date.now() });
      }),
      map(allProducts => {
        console.log('[DEBUG-SERVICE] Raw products from backend:', allProducts.length, 'productos');
        
        let filtered = this.applySorting(allProducts, filters.sortBy);

        const totalElements = filtered.length;
        const start = page * size;
        const end = start + size;
        const paginatedContent = filtered.slice(start, end);

        console.log('[DEBUG-SERVICE] Después de paginación:', paginatedContent.length, 'de', totalElements);

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
