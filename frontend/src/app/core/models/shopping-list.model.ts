export interface ShoppingList {
  listId: number;
  name: string;
  createdAt: string;
  updatedAt: string;
  items: ListItem[];
}

export interface ListItem {
  itemId: number;
  productId: number | null;
  displayName: string;
  imageUrl: string | null;
  quantity: number;
  checked: boolean;
  isGeneric: boolean;
}

export interface OptimizedList {
  totalCost: number;
  storeGroups: OptimizedStore[];
  notFound: string[];
}

export interface OptimizedStore {
  storeId: number;
  storeName: string;
  storeLogo: string;
  subtotal: number;
  items: OptimizedItem[];
}

export interface OptimizedItem {
  productId: number;
  productName: string;
  imageUrl: string | null;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}
