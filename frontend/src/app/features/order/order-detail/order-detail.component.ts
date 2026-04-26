import { Component, OnInit, inject, signal, Input } from '@angular/core';
import { Router } from '@angular/router';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { OrderService } from '../../../core/services/order.service';
import { Order, OrderStatus } from '../../../core/models/order.model';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [
    DatePipe, CurrencyPipe,
    MatCardModule, MatButtonModule, MatIconModule,
    MatChipsModule, MatDividerModule, MatProgressSpinnerModule
  ],
  template: `
    @if (isLoading()) {
      <div style="display:flex; justify-content:center; padding:80px">
        <mat-spinner />
      </div>
    } @else if (order()) {
      <div class="detail-page">

        <!-- Başlık -->
        <div class="page-header">
          <div>
            <h1>Sipariş #{{ order()!.orderNumber }}</h1>
            <p class="order-date">{{ order()!.createdAt | date:'dd MMMM yyyy, HH:mm':'':'tr' }}</p>
          </div>
          <mat-chip [color]="getStatusColor(order()!.status)" selected style="font-size: 16px">
            {{ getStatusLabel(order()!.status) }}
          </mat-chip>
        </div>

        <div class="detail-grid">

          <!-- Sol: Sipariş Kalemleri -->
          <mat-card>
            <mat-card-header>
              <mat-card-title>Ürünler</mat-card-title>
            </mat-card-header>
            <mat-card-content>
              @for (item of order()!.items; track item.productId; let last = $last) {
                <div class="order-item">
                  <div class="item-details">
                    <strong>{{ item.productName }}</strong>
                    <span class="item-sku">SKU: {{ item.productSku }}</span>
                    <span class="item-qty">{{ item.quantity }} adet × {{ item.unitPrice | currency:'TRY':'symbol':'1.2-2':'tr' }}</span>
                  </div>
                  <strong class="item-total">
                    {{ item.totalPrice | currency:'TRY':'symbol':'1.2-2':'tr' }}
                  </strong>
                </div>
                @if (!last) {
                  <mat-divider />
                }
              }
            </mat-card-content>
          </mat-card>

          <!-- Sağ: Özet + İşlemler -->
          <div>
            <mat-card style="margin-bottom: 16px;">
              <mat-card-header>
                <mat-card-title>Sipariş Özeti</mat-card-title>
              </mat-card-header>
              <mat-card-content>
                <div class="summary-row">
                  <span>Ara Toplam</span>
                  <span>{{ order()!.totalAmount | currency:'TRY':'symbol':'1.2-2':'tr' }}</span>
                </div>
                <div class="summary-row">
                  <span>Kargo</span>
                  <span>Ücretsiz</span>
                </div>
                <mat-divider style="margin: 12px 0" />
                <div class="summary-row" style="font-size: 18px; font-weight: 700;">
                  <span>Toplam</span>
                  <span style="color: #1976d2">{{ order()!.totalAmount | currency:'TRY':'symbol':'1.2-2':'tr' }}</span>
                </div>
              </mat-card-content>
            </mat-card>

            <mat-card>
              <mat-card-header>
                <mat-card-title>Teslimat Adresi</mat-card-title>
              </mat-card-header>
              <mat-card-content>
                <p>{{ order()!.shippingAddress }}</p>
              </mat-card-content>
            </mat-card>

            <!-- İptal butonu — yalnızca PENDING veya CONFIRMED'da göster -->
            @if (order()!.status === 'PENDING' || order()!.status === 'CONFIRMED') {
              <button mat-stroked-button color="warn" class="full-width"
                      style="margin-top: 16px; height: 48px;"
                      [disabled]="isCancelling()"
                      (click)="cancelOrder()">
                @if (isCancelling()) {
                  <mat-spinner diameter="20" />
                } @else {
                  <mat-icon>cancel</mat-icon> Siparişi İptal Et
                }
              </button>
            }
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .detail-page { padding: 16px 0; }
    .page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
    .order-date { color: #757575; margin: 4px 0 0; }
    .detail-grid { display: grid; grid-template-columns: 1fr 360px; gap: 24px; }
    .order-item { display: flex; justify-content: space-between; align-items: center; padding: 16px 0; }
    .item-details { display: flex; flex-direction: column; gap: 4px; }
    .item-sku { color: #9e9e9e; font-size: 12px; }
    .item-qty { color: #616161; font-size: 14px; }
    .item-total { font-size: 16px; color: #1976d2; }
    .summary-row { display: flex; justify-content: space-between; padding: 8px 0; }
    @media (max-width: 768px) { .detail-grid { grid-template-columns: 1fr; } }
  `]
})
export class OrderDetailComponent implements OnInit {
  @Input() id!: string;

  private readonly orderService = inject(OrderService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  readonly order = signal<Order | null>(null);
  readonly isLoading = signal(true);
  readonly isCancelling = signal(false);

  ngOnInit(): void {
    this.orderService.getOrderById(Number(this.id)).subscribe({
      next: (o) => {
        this.order.set(o);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }

  cancelOrder(): void {
    this.isCancelling.set(true);
    this.orderService.cancelOrder(Number(this.id)).subscribe({
      next: (updated) => {
        this.order.set(updated);
        this.isCancelling.set(false);
        this.snackBar.open('Sipariş iptal edildi', '', { panelClass: ['success-snackbar'] });
      },
      error: () => this.isCancelling.set(false)
    });
  }

  getStatusColor(status: OrderStatus): 'primary' | 'accent' | 'warn' {
    const map: Record<OrderStatus, 'primary' | 'accent' | 'warn'> = {
      PENDING: 'accent',
      CONFIRMED: 'primary',
      SHIPPED: 'primary',
      DELIVERED: 'primary',
      CANCELLED: 'warn'
    };
    return map[status];
  }

  getStatusLabel(status: OrderStatus): string {
    const labels: Record<OrderStatus, string> = {
      PENDING: 'Beklemede',
      CONFIRMED: 'Onaylandı',
      SHIPPED: 'Kargoya Verildi',
      DELIVERED: 'Teslim Edildi',
      CANCELLED: 'İptal Edildi'
    };
    return labels[status];
  }
}
