import { Injectable } from '@angular/core';
import { Apollo } from 'apollo-angular';
import { Observable, map } from 'rxjs';
import { Product } from '../../core/models/product.model';
import { 
  GET_MY_FAVORITES, 
  IS_FAVORITE, 
  ADD_TO_FAVORITES, 
  REMOVE_FROM_FAVORITES 
} from '../../core/graphql/queries';

@Injectable({
  providedIn: 'root'
})
export class FavoriteService {

  constructor(private apollo: Apollo) {}

  getMyFavorites(): Observable<Product[]> {
    return this.apollo.query<any>({
      query: GET_MY_FAVORITES,
      fetchPolicy: 'network-only'
    }).pipe(
      map(result => {
        const favorites = result.data?.myFavorites || [];
        return favorites.map((p: any) => ({
          id: parseInt(p.productId) || 0,
          name: p.name || '',
          brand: p.brand || null,
          imageUrl: p.imageUrl || null,
          categoryName: p.categoryName || '',
          isFavorite: true
        }));
      })
    );
  }

  isFavorite(productId: number): Observable<boolean> {
    return this.apollo.query<any>({
      query: IS_FAVORITE,
      variables: { productId: productId.toString() },
      fetchPolicy: 'network-only'
    }).pipe(
      map(result => result.data?.isFavorite || false)
    );
  }

  addToFavorites(productId: number): Observable<boolean> {
    return this.apollo.mutate<any>({
      mutation: ADD_TO_FAVORITES,
      variables: { productId: productId.toString() }
    }).pipe(
      map(result => result.data?.addToFavorites || false)
    );
  }

  removeFromFavorites(productId: number): Observable<boolean> {
    return this.apollo.mutate<any>({
      mutation: REMOVE_FROM_FAVORITES,
      variables: { productId: productId.toString() }
    }).pipe(
      map(result => result.data?.removeFromFavorites || false)
    );
  }
}
