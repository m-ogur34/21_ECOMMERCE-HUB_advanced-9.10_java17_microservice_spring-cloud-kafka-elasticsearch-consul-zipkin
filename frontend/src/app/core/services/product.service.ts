import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import {
  Product, ProductRequest, ProductSearchParams, PageResponse
} from '../models/product.model';

@Injectable({ providedIn: 'root' })
export class ProductService {

  private readonly apiUrl = `${environment.apiUrl}/products`;

  // Signal ile yükleme durumu — component'ler inject edip okuyabilir
  readonly isLoading = signal(false);

  constructor(private readonly http: HttpClient) {}

  getProducts(params: ProductSearchParams = {}): Observable<PageResponse<Product>> {
    const httpParams = this.buildParams(params);
    return this.http.get<PageResponse<Product>>(this.apiUrl, { params: httpParams });
  }

  getProductById(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.apiUrl}/${id}`);
  }

  searchProducts(query: string, params: ProductSearchParams = {}): Observable<PageResponse<Product>> {
    const httpParams = this.buildParams({ ...params, query });
    // Elasticsearch full-text search endpoint
    return this.http.get<PageResponse<Product>>(`${this.apiUrl}/search`, { params: httpParams });
  }

  getProductsByCategory(categoryId: number, params: ProductSearchParams = {}): Observable<PageResponse<Product>> {
    const httpParams = this.buildParams(params);
    return this.http.get<PageResponse<Product>>(
      `${this.apiUrl}/category/${categoryId}`, { params: httpParams }
    );
  }

  createProduct(request: ProductRequest): Observable<Product> {
    return this.http.post<Product>(this.apiUrl, request);
  }

  updateProduct(id: number, request: ProductRequest): Observable<Product> {
    return this.http.put<Product>(`${this.apiUrl}/${id}`, request);
  }

  deleteProduct(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  private buildParams(params: ProductSearchParams): HttpParams {
    let httpParams = new HttpParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    });
    return httpParams;
  }
}
