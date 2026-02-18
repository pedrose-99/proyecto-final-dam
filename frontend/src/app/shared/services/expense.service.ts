import { Injectable } from '@angular/core';
import { Apollo } from 'apollo-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { CREATE_BILL_FROM_LIST, GET_BILLS_HISTORY } from '../../core/graphql/queries';
import { Bill } from '../../core/models/expense.model';

@Injectable({
  providedIn: 'root'
})
export class ExpenseService {
  constructor(private apollo: Apollo) {}

  createBillFromList(listId: number, billName: string): Observable<Bill> {
    return this.apollo.mutate<{ createBillFromList: Bill }>({
      mutation: CREATE_BILL_FROM_LIST,
      variables: { listId: listId.toString(), billName }
    }).pipe(
      map(result => result.data?.createBillFromList as Bill)
    );
  }

  getBillsHistory(filter?: string, month?: number, year?: number): Observable<Bill[]> {
    return this.apollo.query<{ getBillsHistory: Bill[] }>({
      query: GET_BILLS_HISTORY,
      variables: { filter: filter || null, month: month || null, year: year || null },
      fetchPolicy: 'network-only'
    }).pipe(
      map(result => (result.data?.getBillsHistory || []) as Bill[])
    );
  }
}
