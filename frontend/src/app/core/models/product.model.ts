export interface Product {
  id: number;
  name: string;
  brand: string | null;
  imageUrl: string | null;
  categoryName: string;
  isFavorite?: boolean;
}

export interface ProductPage {
  content: Product[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface ProductSearchResult {
  id: number;
  name: string;
  brand: string | null;
  imageUrl: string | null;
  categoryName: string;
}

export interface ProductFilters {
  search?: string;
  categoryId?: number;
  categoryIds?: number[];
  categoryNames?: string[];
  storeIds?: number[];
  sortBy?: 'relevance' | 'name_asc' | 'name_desc';
}