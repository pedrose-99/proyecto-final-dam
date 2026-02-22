import { Injectable } from '@angular/core';
import { Apollo } from 'apollo-angular';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { GET_STORES_BY_PRODUCT, GET_PRODUCT_COMPARISON, GET_PRICE_HISTORY, GET_PRODUCTS_BY_CATEGORY } from '../graphql/queries';

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  constructor(private apollo: Apollo) {}

  getComparison(productId: string): Observable<any> {
    return this.apollo.watchQuery<any>({
      query: GET_PRODUCT_COMPARISON,
      variables: { productId }
    }).valueChanges.pipe(
      map(result => {
        if (result.error) {
          console.error('Error de Apollo:', result.error);
        }
        return result.data?.compareProduct || null;
      })
    );
  }

  getPricesByProduct(productId: string): Observable<any> {
    return this.apollo.watchQuery<any>({
      query: GET_STORES_BY_PRODUCT,
      variables: { productId }
    }).valueChanges.pipe(
      map(result => result.data?.storesByProduct || [])
    );
  }
  getPriceHistory(productId: string) {
    return this.apollo.query({
      query: GET_PRICE_HISTORY,
      variables: { productId },
      fetchPolicy: 'network-only'
    }).pipe(map((result: any) => result.data.getPriceHistory));
  }

  getRelatedProducts(categoryId: string, excludeProductId: number): Observable<any[]> {
    const randomPage = Math.floor(Math.random() * 5);
    return this.apollo.query<any>({
      query: GET_PRODUCTS_BY_CATEGORY,
      variables: { categoryId, page: randomPage, size: 30 },
      fetchPolicy: 'network-only'
    }).pipe(
      map(result => {
        const products = (result.data?.productsByCategoryPaginated?.content || [])
          .filter((p: any) => p.productId !== excludeProductId);
        for (let i = products.length - 1; i > 0; i--) {
          const j = Math.floor(Math.random() * (i + 1));
          [products[i], products[j]] = [products[j], products[i]];
        }
        return products.slice(0, 6);
      })
    );
  }
}