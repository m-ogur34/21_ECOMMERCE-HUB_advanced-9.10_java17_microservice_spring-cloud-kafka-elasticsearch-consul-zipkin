export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  stockQuantity: number;
  sku: string;
  categoryId: number;
  categoryName: string;
  imageUrl?: string;
  variants?: ProductVariant[];
  createdAt: string;
}

export interface ProductVariant {
  id: number;
  name: string;
  sku: string;
  additionalPrice: number;
  stockQuantity: number;
}

export interface ProductRequest {
  name: string;
  description: string;
  price: number;
  stockQuantity: number;
  sku: string;
  categoryId: number;
  imageUrl?: string;
}

export interface ProductSearchParams {
  query?: string;
  categoryId?: number;
  minPrice?: number;
  maxPrice?: number;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'ASC' | 'DESC';
}

// API'dan dönen sayfalı yanıt wrapper'ı — common-lib PageResponse ile eşleşir
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;    // current page
  size: number;
  first: boolean;
  last: boolean;
}
