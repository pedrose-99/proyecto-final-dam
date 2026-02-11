import { Injectable } from '@angular/core';
import { Apollo } from 'apollo-angular';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { GET_STORES_BY_PRODUCT, GET_PRODUCT_COMPARISON, GET_PRICE_HISTORY } from '../graphql/queries';

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  constructor(private apollo: Apollo) {}

  // Trae la información completa para la ficha
  getComparison(productId: string): Observable<any> {
    return this.apollo.watchQuery<any>({
      query: GET_PRODUCT_COMPARISON,
      variables: { productId }
    }).valueChanges.pipe(
      map(result => {
        // Corregido: 'error' en singular
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
}