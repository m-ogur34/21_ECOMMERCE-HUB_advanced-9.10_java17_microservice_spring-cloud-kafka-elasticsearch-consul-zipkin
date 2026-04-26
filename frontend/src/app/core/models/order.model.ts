export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED';

export interface OrderItem {
  productId: number;
  productName: string;
  productSku: string;
  variantId?: number;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

export interface Order {
  id: number;
  orderNumber: string;
  status: OrderStatus;
  totalAmount: number;
  shippingAddress: string;
  items: OrderItem[];
  createdAt: string;
  updatedAt: string;
}

export interface OrderItemRequest {
  productId: number;
  variantId?: number;
  quantity: number;
}

export interface OrderRequest {
  items: OrderItemRequest[];
  shippingAddress: string;
}

// Sepet (client-side state) — backend'e göndermeden önce tutulur
export interface CartItem {
  productId: number;
  productName: string;
  productSku: string;
  variantId?: number;
  variantName?: string;
  quantity: number;
  unitPrice: number;
}
