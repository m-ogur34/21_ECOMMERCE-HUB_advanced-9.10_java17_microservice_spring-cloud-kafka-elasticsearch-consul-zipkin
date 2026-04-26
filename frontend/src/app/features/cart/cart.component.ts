import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CurrencyPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CartService } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import { CartItem } from '../../core/models/order.model';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [
    RouterLink, ReactiveFormsModule, CurrencyPipe,
    MatCardModule, MatButtonModule, MatIconModule, MatFormFieldModule,
    MatInputModule, MatDividerModule, MatProgressSpinnerModule
  ],
  template: `
    <h1 style="margin-bottom: 24px;">Sepetim</h1>

    @if (cartService.isEmpty()) {
      <div class="empty-cart">
        <mat-icon>shopping_cart</mat-icon>
        <h3>Sepetiniz boş</h3>
        <p>Ürün eklemek için alışverişe devam edin.</p>
        <a mat-raised-button color="primary" routerLink="/products">
          Alışverişe Başla
        </a>
      </div>
    } @else {
      <div class="cart-layout">

        <!-- Sol: Sepet Öğeleri -->
        <div class="cart-items">
          @for (item of cartService.items(); track item.productId + '-' + item.variantId) {
            <mat-card class="cart-item-card">
              <div class="cart-item">
                <div class="item-info">
                  <h3>{{ item.productName }}</h3>
                  @if (item.variantName) {
                    <p class="variant-name">Varyant: {{ item.variantName }}</p>
                  }
                  <p class="item-price">{{ item.unitPrice | currency:'TRY':'symbol':'1.2-2':'tr' }} / adet</p>
                </div>

                <!-- Miktar kontrolü -->
                <div class="quantity-control">
                  <button mat-icon-button
                          (click)="cartService.updateQuantity(item.productId, item.variantId, item.quantity - 1)">
                    <mat-icon>remove</mat-icon>
                  </button>
                  <span class="qty-display">{{ item.quantity }}</span>
                  <button mat-icon-button
                          (click)="cartService.updateQuantity(item.productId, item.variantId, item.quantity + 1)">
                    <mat-icon>add</mat-icon>
                  </button>
                </div>

                <div class="item-total">
                  {{ (item.unitPrice * item.quantity) | currency:'TRY':'symbol':'1.2-2':'tr' }}
                </div>

                <button mat-icon-button color="warn"
                        (click)="cartService.removeItem(item.productId, item.variantId)">
                  <mat-icon>delete</mat-icon>
                </button>
              </div>
            </mat-card>
          }

          <button mat-button color="warn" (click)="cartService.clear()">
            <mat-icon>delete_sweep</mat-icon> Sepeti Temizle
          </button>
        </div>

        <!-- Sağ: Sipariş Özeti + Adres Formu -->
        <div class="order-summary">
          <mat-card>
            <mat-card-header>
              <mat-card-title>Sipariş Özeti</mat-card-title>
            </mat-card-header>
            <mat-card-content>

              <!-- Fiyat özeti -->
              <div class="summary-row">
                <span>Ara Toplam ({{ cartService.totalItems() }} ürün)</span>
                <span>{{ cartService.totalAmount() | currency:'TRY':'symbol':'1.2-2':'tr' }}</span>
              </div>
              <div class="summary-row">
                <span>Kargo</span>
                <span>Ücretsiz</span>
              </div>
              <mat-divider style="margin: 12px 0;" />
              <div class="summary-row total">
                <strong>Toplam</strong>
                <strong>{{ cartService.totalAmount() | currency:'TRY':'symbol':'1.2-2':'tr' }}</strong>
              </div>

              <!-- Teslimat adresi -->
              <form [formGroup]="addressForm" style="margin-top: 24px;">
                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>Teslimat Adresi</mat-label>
                  <textarea matInput formControlName="shippingAddress"
                            rows="3" placeholder="Açık adresinizi girin..."></textarea>
                  @if (addressForm.get('shippingAddress')?.hasError('required') && addressForm.get('shippingAddress')?.touched) {
                    <mat-error>Teslimat adresi zorunludur</mat-error>
                  }
                </mat-form-field>
              </form>

            </mat-card-content>
            <mat-card-actions>
              <button mat-raised-button color="primary" class="full-width checkout-btn"
                      [disabled]="isOrdering() || addressForm.invalid"
                      (click)="placeOrder()">
                @if (isOrdering()) {
                  <mat-spinner diameter="20" />
                } @else {
                  <mat-icon>payment</mat-icon>
                  Siparişi Tamamla
                }
              </button>
            </mat-card-actions>
          </mat-card>
        </div>

      </div>
    }
  `,
  styles: [`
    .empty-cart { text-align: center; padding: 80px 16px; }
    .empty-cart mat-icon { font-size: 80px; width: 80px; height: 80px; color: #bdbdbd; }
    .empty-cart h3 { margin: 16px 0 8px; color: #616161; }
    .empty-cart p { color: #9e9e9e; margin-bottom: 24px; }
    .cart-layout { display: grid; grid-template-columns: 1fr 380px; gap: 24px; }
    .cart-items { display: flex; flex-direction: column; gap: 12px; }
    .cart-item-card { padding: 16px; }
    .cart-item { display: flex; align-items: center; gap: 16px; }
    .item-info { flex: 1; }
    .item-info h3 { margin: 0 0 4px; }
    .variant-name { color: #757575; font-size: 13px; margin: 0 0 4px; }
    .item-price { color: #1976d2; margin: 0; }
    .quantity-control { display: flex; align-items: center; gap: 4px; }
    .qty-display { min-width: 32px; text-align: center; font-weight: 600; }
    .item-total { font-size: 16px; font-weight: 700; min-width: 80px; text-align: right; }
    .summary-row { display: flex; justify-content: space-between; padding: 8px 0; }
    .summary-row.total { font-size: 18px; padding-top: 12px; }
    .checkout-btn { height: 48px; }
    @media (max-width: 768px) { .cart-layout { grid-template-columns: 1fr; } }
  `]
})
export class CartComponent {
  readonly cartService = inject(CartService);
  private readonly orderService = inject(OrderService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  readonly isOrdering = signal(false);

  readonly addressForm = this.fb.group({
    shippingAddress: ['', [Validators.required, Validators.minLength(20)]]
  });

  placeOrder(): void {
    if (this.addressForm.invalid) {
      this.addressForm.markAllAsTouched();
      return;
    }

    this.isOrdering.set(true);

    // Sepet öğelerini OrderRequest formatına çevir
    const orderRequest = {
      shippingAddress: this.addressForm.value.shippingAddress!,
      items: this.cartService.items().map((item: CartItem) => ({
        productId: item.productId,
        variantId: item.variantId,
        quantity: item.quantity
      }))
    };

    this.orderService.createOrder(orderRequest).subscribe({
      next: (order) => {
        this.cartService.clear();  // Başarılı sipariş → sepeti temizle
        this.snackBar.open(
          `Siparişiniz alındı! Sipariş No: ${order.orderNumber}`,
          '',
          { panelClass: ['success-snackbar'], duration: 5000 }
        );
        this.router.navigate(['/orders', order.id]);
      },
      error: () => this.isOrdering.set(false)
    });
  }
}
