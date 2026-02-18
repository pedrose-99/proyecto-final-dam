export interface BillItem {
  productName: string;
  price: number;
  quantity: number;
  storeName?: string;
}

export interface Bill {
  billsHistoryId: number;
  name: string;
  recordedAt: string;
  totalAmount: number;
  exceededLimit: boolean;
  itemsSummary: BillItem[];
}

export interface SpendingLimit {
  limitId: number;
  amount: number;
  type: string;
  isActive: boolean;
}

export interface MonthlyExpenseSummary {
  periodLabel: string;
  totalAmount: number;
  billCount: number;
  exceededCount: number;
}
