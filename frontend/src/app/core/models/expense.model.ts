export interface BillItem {
  productName: string;
  price: number;
  quantity: number;
}

export interface Bill {
  billsHistoryId: number;
  name: string;
  recordedAt: string;
  totalAmount: number;
  exceededLimit: boolean;
  itemsSummary: BillItem[];
}
