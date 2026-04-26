import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { OrderService } from '../../../core/services/order.service';
import { Order, OrderStatus } from '../../../core/models/order.model';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [
    RouterLink, DatePipe, CurrencyPipe,
    MatCardModule, MatButtonModule, MatIconModule,
    MatChipsModule, MatPaginatorModule, MatProgressSpinnerModule
  ],
  template: `
    <h1 style="margin-bottom: 24px;">Siparişlerim</h1>

    @if (isLoading()) {
      <div style="display:flex; justify-content:center; padding:48px">
        <mat-spinner />
      </div>
    } @else if (orders().length === 0) {
      <div style="text-align:center; padding:80px; color:#9e9e9e">
        <mat-icon style="font-size:64px; width:64px; height:64px">receipt_long</mat-icon>
        <h3>Henüz sipariş vermediniz</h3>
        <a mat-raised-button color="primary" routerLink="/products">Alışverişe Başla</a>
      </div>
    } @else {
      <div class="orders-list">
        @for (order of orders(); track order.id) {
          <mat-card class="order-card">
            <div class="order-header">
              <div>
                <strong>Sipariş #{{ order.orderNumber }}</strong>
                <span class="order-date">{{ order.createdAt | date:'dd MMM yyyy HH:mm':'':'tr' }}</span>
              </div>
              <!-- Sipariş durumu renkli chip ile göster -->
              <mat-chip [color]="getStatusColor(order.status)" selected>
                {{ getStatusLabel(order.status) }}
              </mat-chip>
            </div>

            <div class="order-items-summary">
              @for (item of order.items.slice(0, 3); track item.productId) {
                <span class="item-pill">{{ item.productName }} × {{ item.quantity }}</span>
              }
              @if (order.items.length > 3) {
                <span class="item-pill more">+{{ order.items.length - 3 }} daha</span>
              }
            </div>

            <div class="order-footer">
              <strong class="order-total">
                {{ order.totalAmount | currency:'TRY':'symbol':'1.2-2':'tr' }}
              </strong>
              <a mat-button color="primary" [routerLink]="['/orders', order.id]">
                <mat-icon>visibility</mat-icon> Detay
              </a>
            </div>
          </mat-card>
        }
      </div>

      <mat-paginator
        [length]="totalElements()"
        [pageSize]="10"
        [pageIndex]="currentPage()"
        (page)="onPageChange($event)"
        showFirstLastButtons />
    }
  `,
  styles: [`
    .orders-list { display: flex; flex-direction: column; gap: 12px; margin-bottom: 24px; }
    .order-card { padding: 20px; }
    .order-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
    .order-date { color: #757575; font-size: 13px; margin-left: 12px; }
    .order-items-summary { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 16px; }
    .item-pill { background: #f5f5f5; border-radius: 16px; padding: 4px 12px; font-size: 13px; }
    .item-pill.more { color: #757575; }
    .order-footer { display: flex; align-items: center; justify-content: space-between; }
    .order-total { font-size: 18px; color: #1976d2; }
  `]
})
export class OrderListComponent implements OnInit {
  private readonly orderService = inject(OrderService);

  readonly orders = signal<Order[]>([]);
  readonly totalElements = signal(0);
  readonly currentPage = signal(0);
  readonly isLoading = signal(true);

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.isLoading.set(true);
    this.orderService.getMyOrders(this.currentPage()).subscribe({
      next: (page) => {
        this.orders.set(page.content);
        this.totalElements.set(page.totalElements);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }

  onPageChange(event: PageEvent): void {
    this.currentPage.set(event.pageIndex);
    this.loadOrders();
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
