import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged, switchMap, startWith } from 'rxjs/operators';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CurrencyPipe } from '@angular/common';
import { ProductService } from '../../../core/services/product.service';
import { CartService } from '../../../core/services/cart.service';
import { AuthService } from '../../../core/services/auth.service';
import { Product, PageResponse } from '../../../core/models/product.model';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink, CurrencyPipe,
    MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatPaginatorModule,
    MatProgressSpinnerModule, MatChipsModule
  ],
  template: `
    <div class="product-list-page">

      <!-- Arama ve Filtreler -->
      <mat-card class="filter-card">
        <form [formGroup]="filterForm" class="filter-form">

          <!-- Arama kutusu: debounce ile Elasticsearch çağrısını optimize et -->
          <mat-form-field appearance="outline">
            <mat-label>Ürün Ara</mat-label>
            <input matInput formControlName="query" placeholder="Ürün adı veya açıklama...">
            <mat-icon matSuffix>search</mat-icon>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Min Fiyat (₺)</mat-label>
            <input matInput type="number" formControlName="minPrice" min="0">
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Max Fiyat (₺)</mat-label>
            <input matInput type="number" formControlName="maxPrice" min="0">
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Sıralama</mat-label>
            <mat-select formControlName="sortBy">
              <mat-option value="name">İsim</mat-option>
              <mat-option value="price">Fiyat (Düşük → Yüksek)</mat-option>
              <mat-option value="price-desc">Fiyat (Yüksek → Düşük)</mat-option>
              <mat-option value="createdAt">Yeni Eklenenler</mat-option>
            </mat-select>
          </mat-form-field>

        </form>
      </mat-card>

      <!-- Sonuç bilgisi -->
      <div class="results-header">
        <span class="results-count">
          {{ totalElements() }} ürün bulundu
        </span>
      </div>

      <!-- Yükleme durumu -->
      @if (isLoading()) {
        <div class="loading-center">
          <mat-spinner diameter="48" />
        </div>
      } @else if (products().length === 0) {
        <div class="empty-state">
          <mat-icon>inventory_2</mat-icon>
          <p>Ürün bulunamadı</p>
        </div>
      } @else {
        <!-- Ürün kartları — CSS Grid ile responsive -->
        <div class="product-grid">
          @for (product of products(); track product.id) {
            <mat-card class="product-card" [routerLink]="['/products', product.id]">
              <!-- Ürün görseli -->
              <div class="product-image">
                @if (product.imageUrl) {
                  <img [src]="product.imageUrl" [alt]="product.name">
                } @else {
                  <mat-icon class="placeholder-icon">image_not_supported</mat-icon>
                }
              </div>

              <mat-card-header>
                <mat-card-title>{{ product.name }}</mat-card-title>
                <mat-card-subtitle>{{ product.categoryName }}</mat-card-subtitle>
              </mat-card-header>

              <mat-card-content>
                <p class="description">{{ product.description | slice:0:80 }}...</p>
                <div class="price-stock">
                  <span class="price">{{ product.price | currency:'TRY':'symbol':'1.2-2':'tr' }}</span>
                  <mat-chip [color]="product.stockQuantity > 0 ? 'primary' : 'warn'" selected>
                    {{ product.stockQuantity > 0 ? 'Stokta' : 'Tükendi' }}
                  </mat-chip>
                </div>
              </mat-card-content>

              <mat-card-actions>
                <button mat-button color="primary" [routerLink]="['/products', product.id]"
                        (click)="$event.stopPropagation()">
                  <mat-icon>visibility</mat-icon> Detay
                </button>
                @if (authService.isAuthenticated()) {
                  <button mat-raised-button color="accent"
                          [disabled]="product.stockQuantity === 0"
                          (click)="addToCart($event, product)">
                    <mat-icon>add_shopping_cart</mat-icon> Sepete Ekle
                  </button>
                }
              </mat-card-actions>
            </mat-card>
          }
        </div>

        <!-- Sayfalama -->
        <mat-paginator
          [length]="totalElements()"
          [pageSize]="pageSize()"
          [pageSizeOptions]="[12, 24, 48]"
          [pageIndex]="currentPage()"
          (page)="onPageChange($event)"
          showFirstLastButtons />
      }
    </div>
  `,
  styles: [`
    .product-list-page { padding: 16px 0; }
    .filter-card { margin-bottom: 24px; padding: 16px; }
    .filter-form { display: grid; grid-template-columns: 2fr 1fr 1fr 1fr; gap: 16px; align-items: center; }
    .results-header { margin-bottom: 16px; }
    .results-count { color: #616161; font-size: 14px; }
    .loading-center { display: flex; justify-content: center; padding: 48px; }
    .empty-state { text-align: center; padding: 48px; color: #9e9e9e; }
    .empty-state mat-icon { font-size: 64px; width: 64px; height: 64px; }
    .product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; margin-bottom: 24px; }
    .product-card { cursor: pointer; transition: transform 0.2s, box-shadow 0.2s; }
    .product-card:hover { transform: translateY(-4px); box-shadow: 0 8px 16px rgba(0,0,0,0.15); }
    .product-image { height: 200px; background: #f5f5f5; display: flex; align-items: center; justify-content: center; overflow: hidden; }
    .product-image img { width: 100%; height: 100%; object-fit: cover; }
    .placeholder-icon { font-size: 64px; width: 64px; height: 64px; color: #bdbdbd; }
    .description { color: #616161; font-size: 14px; line-height: 1.5; margin-bottom: 12px; }
    .price-stock { display: flex; align-items: center; justify-content: space-between; }
    .price { font-size: 20px; font-weight: 700; color: #1976d2; }
    @media (max-width: 768px) {
      .filter-form { grid-template-columns: 1fr; }
    }
  `]
})
export class ProductListComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly cartService = inject(CartService);
  readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  // Sayfa state'i — signal ile reaktif
  readonly products = signal<Product[]>([]);
  readonly totalElements = signal(0);
  readonly currentPage = signal(0);
  readonly pageSize = signal(12);
  readonly isLoading = signal(false);

  readonly filterForm = this.fb.group({
    query: [''],
    minPrice: [null as number | null],
    maxPrice: [null as number | null],
    sortBy: ['name']
  });

  ngOnInit(): void {
    // Form değişikliklerini dinle — debounce ile gereksiz API çağrısını önle
    this.filterForm.valueChanges.pipe(
      debounceTime(400),      // 400ms bekle (kullanıcı yazmayı bırakıncaya)
      distinctUntilChanged(), // Aynı değer tekrar gelirse ignore et
      startWith(this.filterForm.value)
    ).subscribe(() => {
      this.currentPage.set(0);  // Filtre değişince ilk sayfaya dön
      this.loadProducts();
    });
  }

  loadProducts(): void {
    this.isLoading.set(true);
    const { query, minPrice, maxPrice, sortBy } = this.filterForm.getRawValue();

    const [sortField, sortDir] = sortBy === 'price-desc'
      ? ['price', 'DESC']
      : sortBy === 'price'
      ? ['price', 'ASC']
      : [sortBy || 'name', 'ASC'];

    const params = {
      page: this.currentPage(),
      size: this.pageSize(),
      sortBy: sortField,
      sortDir: sortDir as 'ASC' | 'DESC',
      ...(minPrice && { minPrice }),
      ...(maxPrice && { maxPrice })
    };

    // query varsa Elasticsearch search, yoksa normal listing
    const request$ = query?.trim()
      ? this.productService.searchProducts(query.trim(), params)
      : this.productService.getProducts(params);

    request$.subscribe({
      next: (page) => {
        this.products.set(page.content);
        this.totalElements.set(page.totalElements);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }

  onPageChange(event: PageEvent): void {
    this.currentPage.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadProducts();
  }

  addToCart(event: MouseEvent, product: Product): void {
    event.stopPropagation(); // Kart tıklamasını engelle (detail sayfasına gitme)
    this.cartService.addItem({
      productId: product.id,
      productName: product.name,
      productSku: product.sku,
      quantity: 1,
      unitPrice: product.price
    });
    this.snackBar.open(`"${product.name}" sepete eklendi`, 'Sepete Git', {
      panelClass: ['success-snackbar']
    });
  }
}
