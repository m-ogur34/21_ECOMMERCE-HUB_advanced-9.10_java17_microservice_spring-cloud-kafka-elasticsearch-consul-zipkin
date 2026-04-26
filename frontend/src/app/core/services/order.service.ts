import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Order, OrderRequest, PageResponse } from '../models/order.model';

@Injectable({ providedIn: 'root' })
export class OrderService {

  private readonly apiUrl = `${environment.apiUrl}/orders`;

  constructor(private readonly http: HttpClient) {}

  createOrder(request: OrderRequest): Observable<Order> {
    // Saga başlatır: CreateOrderStep → ReserveStockStep
    return this.http.post<Order>(this.apiUrl, request);
  }

  getMyOrders(page = 0, size = 10): Observable<PageResponse<Order>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);
    return this.http.get<PageResponse<Order>>(`${this.apiUrl}/my-orders`, { params });
  }

  getOrderById(id: number): Observable<Order> {
    return this.http.get<Order>(`${this.apiUrl}/${id}`);
  }

  cancelOrder(id: number): Observable<Order> {
    return this.http.patch<Order>(`${this.apiUrl}/${id}/cancel`, {});
  }

  // Admin: tüm siparişler
  getAllOrders(page = 0, size = 20, status?: string): Observable<PageResponse<Order>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<PageResponse<Order>>(this.apiUrl, { params });
  }
}
