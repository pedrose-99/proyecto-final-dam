import { Injectable } from '@angular/core';
import { Apollo } from 'apollo-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  GET_MY_SHOPPING_LISTS,
  GET_SHOPPING_LIST_BY_ID,
  CREATE_SHOPPING_LIST,
  DELETE_SHOPPING_LIST,
  ADD_ITEM_TO_LIST,
  UPDATE_LIST_ITEM,
  REMOVE_LIST_ITEM,
  OPTIMIZE_SHOPPING_LIST,
  OPTIMIZE_BY_STORE,
  CREATE_SUBLISTS
} from '../../core/graphql/queries';
import { ShoppingList, OptimizedList, OptimizedStore, SublistInput } from '../../core/models/shopping-list.model';

@Injectable({
  providedIn: 'root'
})
export class ShoppingListService {
  constructor(private apollo: Apollo) {}

  getMyLists(): Observable<ShoppingList[]> {
    return this.apollo.query<{ myShoppingLists: ShoppingList[] }>(
      {
        query: GET_MY_SHOPPING_LISTS,
        fetchPolicy: 'network-only'
      }
    ).pipe(
      map(result => (result.data?.myShoppingLists || []) as ShoppingList[])
    );
  }

  getListById(listId: number): Observable<ShoppingList> {
    return this.apollo.query<{ shoppingListById: ShoppingList }>(
      {
        query: GET_SHOPPING_LIST_BY_ID,
        variables: { listId: listId.toString() },
        fetchPolicy: 'network-only'
      }
    ).pipe(
      map(result => result.data?.shoppingListById as ShoppingList)
    );
  }

  createList(name: string, groupId?: number): Observable<ShoppingList> {
    return this.apollo.mutate<{ createShoppingList: ShoppingList }>(
      {
        mutation: CREATE_SHOPPING_LIST,
        variables: { name, groupId: groupId ? groupId.toString() : null }
      }
    ).pipe(
      map(result => result.data?.createShoppingList as ShoppingList)
    );
  }

  deleteList(listId: number): Observable<boolean> {
    return this.apollo.mutate<{ deleteShoppingList: boolean }>(
      {
        mutation: DELETE_SHOPPING_LIST,
        variables: { listId: listId.toString() }
      }
    ).pipe(
      map(result => result.data?.deleteShoppingList || false)
    );
  }

  addItem(listId: number, productId: number | null, genericName: string | null, quantity: number = 1): Observable<ShoppingList> {
    return this.apollo.mutate<{ addItemToList: ShoppingList }>(
      {
        mutation: ADD_ITEM_TO_LIST,
        variables: {
          listId: listId.toString(),
          productId: productId ? productId.toString() : null,
          genericName,
          quantity
        }
      }
    ).pipe(
      map(result => result.data?.addItemToList as ShoppingList)
    );
  }

  updateItem(listId: number, itemId: number, quantity?: number, checked?: boolean): Observable<ShoppingList> {
    return this.apollo.mutate<{ updateListItem: ShoppingList }>(
      {
        mutation: UPDATE_LIST_ITEM,
        variables: {
          listId: listId.toString(),
          itemId: itemId.toString(),
          quantity,
          checked
        }
      }
    ).pipe(
      map(result => result.data?.updateListItem as ShoppingList)
    );
  }

  removeItem(listId: number, itemId: number): Observable<ShoppingList> {
    return this.apollo.mutate<{ removeListItem: ShoppingList }>(
      {
        mutation: REMOVE_LIST_ITEM,
        variables: {
          listId: listId.toString(),
          itemId: itemId.toString()
        }
      }
    ).pipe(
      map(result => result.data?.removeListItem as ShoppingList)
    );
  }

  optimize(listId: number, storeIds: number[]): Observable<OptimizedList> {
    return this.apollo.query<{ optimizeShoppingList: OptimizedList }>(
      {
        query: OPTIMIZE_SHOPPING_LIST,
        variables: {
          listId: listId.toString(),
          storeIds: storeIds.map(id => id.toString())
        },
        fetchPolicy: 'network-only'
      }
    ).pipe(
      map(result => result.data?.optimizeShoppingList as OptimizedList)
    );
  }

  optimizeByStore(listId: number, storeIds: number[]): Observable<OptimizedStore[]> {
    return this.apollo.query<{ optimizeByStore: OptimizedStore[] }>(
      {
        query: OPTIMIZE_BY_STORE,
        variables: {
          listId: listId.toString(),
          storeIds: storeIds.map(id => id.toString())
        },
        fetchPolicy: 'network-only'
      }
    ).pipe(
      map(result => result.data?.optimizeByStore as OptimizedStore[])
    );
  }

  createSublists(originalListName: string, sublists: SublistInput[]): Observable<ShoppingList[]> {
    return this.apollo.mutate<{ createSublists: ShoppingList[] }>(
      {
        mutation: CREATE_SUBLISTS,
        variables: {
          originalListName,
          sublists: sublists.map(s => ({
            storeName: s.storeName,
            items: s.items.map(i => ({
              productId: i.productId.toString(),
              quantity: i.quantity
            }))
          }))
        }
      }
    ).pipe(
      map(result => result.data?.createSublists as ShoppingList[])
    );
  }
}
