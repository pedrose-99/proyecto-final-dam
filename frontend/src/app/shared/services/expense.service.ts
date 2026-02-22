import { Injectable } from '@angular/core';
import { Apollo } from 'apollo-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { CREATE_BILL_FROM_LIST, GET_BILLS_HISTORY, GET_SPENDING_LIMITS, SAVE_SPENDING_LIMIT, GET_EXPENSE_SUMMARY } from '../../core/graphql/queries';
import { Bill, SpendingLimit, MonthlyExpenseSummary } from '../../core/models/expense.model';

@Injectable({
  providedIn: 'root'
})
export class ExpenseService {
  constructor(private apollo: Apollo) {}

  createBillFromList(listId: number, billName: string, purchaseDate?: string): Observable<Bill> {
    return this.apollo.mutate<{ createBillFromList: Bill }>({
      mutation: CREATE_BILL_FROM_LIST,
      variables: { listId: listId.toString(), billName, purchaseDate: purchaseDate || null }
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

  getSpendingLimits(): Observable<SpendingLimit[]> {
    return this.apollo.query<{ getSpendingLimits: SpendingLimit[] }>({
      query: GET_SPENDING_LIMITS,
      fetchPolicy: 'network-only'
    }).pipe(
      map(result => (result.data?.getSpendingLimits || []) as SpendingLimit[])
    );
  }

  saveSpendingLimit(amount: number, type: string): Observable<SpendingLimit> {
    return this.apollo.mutate<{ saveSpendingLimit: SpendingLimit }>({
      mutation: SAVE_SPENDING_LIMIT,
      variables: { amount, type }
    }).pipe(
      map(result => result.data?.saveSpendingLimit as SpendingLimit)
    );
  }

  getExpenseSummary(period: string = 'MONTHLY', offset: number = 0): Observable<MonthlyExpenseSummary[]> {
    return this.apollo.query<{ getExpenseSummary: MonthlyExpenseSummary[] }>({
      query: GET_EXPENSE_SUMMARY,
      variables: { period, offset },
      fetchPolicy: 'network-only'
    }).pipe(
      map(result => (result.data?.getExpenseSummary || []) as MonthlyExpenseSummary[])
    );
  }
}
