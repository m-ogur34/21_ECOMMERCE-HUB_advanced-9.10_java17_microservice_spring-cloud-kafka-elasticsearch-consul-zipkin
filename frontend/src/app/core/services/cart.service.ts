import { Injectable, signal, computed } from '@angular/core';
import { CartItem } from '../models/order.model';

// Sepet state'i — backend'e kaydedilmez, localStorage'da tutulur.
// Sipariş oluşturulunca OrderService'e gönderilir.
@Injectable({ providedIn: 'root' })
export class CartService {

  private readonly CART_KEY = 'cart_items';

  // Signal: sepet değişince bağımlı component'ler otomatik güncellenir
  private readonly _items = signal<CartItem[]>(this.loadFromStorage());

  readonly items = this._items.asReadonly();

  // computed: items değişince otomatik hesaplanır — gereksiz subscribe yok
  readonly totalItems = computed(() =>
    this._items().reduce((sum, item) => sum + item.quantity, 0)
  );

  readonly totalAmount = computed(() =>
    this._items().reduce((sum, item) => sum + item.unitPrice * item.quantity, 0)
  );

  readonly isEmpty = computed(() => this._items().length === 0);

  addItem(item: CartItem): void {
    const current = this._items();
    // Aynı ürün + varyant varsa miktarı artır
    const existingIndex = current.findIndex(
      i => i.productId === item.productId && i.variantId === item.variantId
    );

    let updated: CartItem[];
    if (existingIndex >= 0) {
      updated = current.map((i, idx) =>
        idx === existingIndex ? { ...i, quantity: i.quantity + item.quantity } : i
      );
    } else {
      updated = [...current, item];
    }

    this._items.set(updated);
    this.saveToStorage(updated);
  }

  updateQuantity(productId: number, variantId: number | undefined, quantity: number): void {
    if (quantity <= 0) {
      this.removeItem(productId, variantId);
      return;
    }

    const updated = this._items().map(item =>
      item.productId === productId && item.variantId === variantId
        ? { ...item, quantity }
        : item
    );
    this._items.set(updated);
    this.saveToStorage(updated);
  }

  removeItem(productId: number, variantId: number | undefined): void {
    const updated = this._items().filter(
      item => !(item.productId === productId && item.variantId === variantId)
    );
    this._items.set(updated);
    this.saveToStorage(updated);
  }

  clear(): void {
    this._items.set([]);
    localStorage.removeItem(this.CART_KEY);
  }

  private saveToStorage(items: CartItem[]): void {
    localStorage.setItem(this.CART_KEY, JSON.stringify(items));
  }

  private loadFromStorage(): CartItem[] {
    try {
      const stored = localStorage.getItem(this.CART_KEY);
      return stored ? JSON.parse(stored) : [];
    } catch {
      return [];
    }
  }
}
