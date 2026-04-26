import { Component, OnInit, inject, signal, Input } from '@angular/core';
import { Router } from '@angular/router';
import { CurrencyPipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../../core/services/product.service';
import { CartService } from '../../../core/services/cart.service';
import { AuthService } from '../../../core/services/auth.service';
import { Product, ProductVariant } from '../../../core/models/product.model';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [
    FormsModule, CurrencyPipe,
    MatCardModule, MatButtonModule, MatIconModule,
    MatChipsModule, MatSelectModule, MatFormFieldModule,
    MatProgressSpinnerModule
  ],
  template: `
    @if (isLoading()) {
      <div style="display:flex; justify-content:center; padding:80px">
        <mat-spinner />
      </div>
    } @else if (product()) {
      <div class="detail-layout">

        <!-- Sol: Ürün Görseli -->
        <div class="image-section">
          @if (product()!.imageUrl) {
            <img [src]="product()!.imageUrl" [alt]="product()!.name" class="product-image">
          } @else {
            <div class="image-placeholder">
              <mat-icon>inventory_2</mat-icon>
            </div>
          }
        </div>

        <!-- Sağ: Ürün Bilgileri -->
        <div class="info-section">
          <mat-chip color="primary" selected>{{ product()!.categoryName }}</mat-chip>
          <h1 class="product-title">{{ product()!.name }}</h1>
          <p class="product-sku">SKU: {{ product()!.sku }}</p>
          <p class="product-description">{{ product()!.description }}</p>

          <!-- Varyant seçimi (renk, beden vb.) -->
          @if (product()!.variants && product()!.variants!.length > 0) {
            <mat-form-field appearance="outline" style="width: 100%; margin-top: 16px;">
              <mat-label>Varyant Seçin</mat-label>
              <mat-select [(ngModel)]="selectedVariant">
                <mat-option [value]="null">Standart</mat-option>
                @for (variant of product()!.variants; track variant.id) {
                  <mat-option [value]="variant">
                    {{ variant.name }}
                    @if (variant.additionalPrice > 0) {
                      (+{{ variant.additionalPrice | currency:'TRY':'symbol':'1.2-2':'tr' }})
                    }
                  </mat-option>
                }
              </mat-select>
            </mat-form-field>
          }

          <!-- Fiyat -->
          <div class="price-section">
            <span class="price">
              {{ effectivePrice() | currency:'TRY':'symbol':'1.2-2':'tr' }}
            </span>
            @if (selectedVariant?.additionalPrice) {
              <span class="price-detail">(Baz + {{ selectedVariant!.additionalPrice | currency:'TRY':'symbol':'1.2-2':'tr' }})</span>
            }
          </div>

          <!-- Stok durumu -->
          <div class="stock-info">
            @if (effectiveStock() > 10) {
              <mat-chip color="primary" selected>
                <mat-icon matChipAvatar>check_circle</mat-icon>
                Stokta ({{ effectiveStock() }} adet)
              </mat-chip>
            } @else if (effectiveStock() > 0) {
              <mat-chip color="warn" selected>
                Son {{ effectiveStock() }} adet!
              </mat-chip>
            } @else {
              <mat-chip color="warn" selected>Tükendi</mat-chip>
            }
          </div>

          <!-- Miktar + Sepete Ekle -->
          <div class="actions">
            <div class="quantity-selector">
              <button mat-icon-button (click)="decreaseQty()" [disabled]="quantity() <= 1">
                <mat-icon>remove</mat-icon>
              </button>
              <span class="quantity">{{ quantity() }}</span>
              <button mat-icon-button (click)="increaseQty()" [disabled]="quantity() >= effectiveStock()">
                <mat-icon>add</mat-icon>
              </button>
            </div>

            @if (authService.isAuthenticated()) {
              <button mat-raised-button color="primary"
                      [disabled]="effectiveStock() === 0"
                      (click)="addToCart()">
                <mat-icon>add_shopping_cart</mat-icon>
                Sepete Ekle
              </button>
            } @else {
              <button mat-raised-button color="primary"
                      (click)="goToLogin()">
                <mat-icon>login</mat-icon>
                Satın Almak için Giriş Yap
              </button>
            }
          </div>
        </div>
      </div>
    } @else {
      <div style="text-align:center; padding:48px; color:#9e9e9e">
        <mat-icon style="font-size:64px; width:64px; height:64px">error_outline</mat-icon>
        <p>Ürün bulunamadı</p>
      </div>
    }
  `,
  styles: [`
    .detail-layout { display: grid; grid-template-columns: 1fr 1fr; gap: 48px; padding: 24px 0; }
    .image-section { position: sticky; top: 80px; }
    .product-image { width: 100%; border-radius: 12px; object-fit: cover; max-height: 480px; }
    .image-placeholder { height: 400px; background: #f5f5f5; border-radius: 12px; display: flex; align-items: center; justify-content: center; }
    .image-placeholder mat-icon { font-size: 96px; width: 96px; height: 96px; color: #bdbdbd; }
    .product-title { font-size: 28px; font-weight: 700; margin: 12px 0 4px; }
    .product-sku { color: #9e9e9e; font-size: 13px; margin-bottom: 16px; }
    .product-description { color: #424242; line-height: 1.7; margin-bottom: 24px; }
    .price-section { margin: 16px 0; display: flex; align-items: baseline; gap: 8px; }
    .price { font-size: 32px; font-weight: 700; color: #1976d2; }
    .price-detail { color: #757575; font-size: 14px; }
    .stock-info { margin: 16px 0; }
    .actions { display: flex; align-items: center; gap: 24px; margin-top: 24px; }
    .quantity-selector { display: flex; align-items: center; gap: 8px; border: 1px solid #e0e0e0; border-radius: 8px; padding: 4px; }
    .quantity { font-size: 18px; font-weight: 600; min-width: 32px; text-align: center; }
    @media (max-width: 768px) {
      .detail-layout { grid-template-columns: 1fr; }
      .image-section { position: static; }
    }
  `]
})
export class ProductDetailComponent implements OnInit {
  // Angular 17 withComponentInputBinding: route :id parametresi @Input() ile alınır
  @Input() id!: string;

  private readonly productService = inject(ProductService);
  private readonly cartService = inject(CartService);
  readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  readonly product = signal<Product | null>(null);
  readonly isLoading = signal(true);
  readonly quantity = signal(1);
  selectedVariant: ProductVariant | null = null;

  // computed yerine getter — template'de daha basit kullanım
  effectivePrice(): number {
    const base = this.product()?.price ?? 0;
    return base + (this.selectedVariant?.additionalPrice ?? 0);
  }

  effectiveStock(): number {
    return this.selectedVariant?.stockQuantity ?? (this.product()?.stockQuantity ?? 0);
  }

  ngOnInit(): void {
    this.productService.getProductById(Number(this.id)).subscribe({
      next: (p) => {
        this.product.set(p);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }

  increaseQty(): void {
    if (this.quantity() < this.effectiveStock()) {
      this.quantity.update(q => q + 1);
    }
  }

  decreaseQty(): void {
    if (this.quantity() > 1) {
      this.quantity.update(q => q - 1);
    }
  }

  addToCart(): void {
    const p = this.product()!;
    this.cartService.addItem({
      productId: p.id,
      productName: p.name,
      productSku: this.selectedVariant?.sku ?? p.sku,
      variantId: this.selectedVariant?.id,
      variantName: this.selectedVariant?.name,
      quantity: this.quantity(),
      unitPrice: this.effectivePrice()
    });
    this.snackBar.open(`Sepete eklendi (${this.quantity()} adet)`, '', {
      panelClass: ['success-snackbar']
    });
  }

  goToLogin(): void {
    this.router.navigate(['/auth/login'], {
      queryParams: { returnUrl: `/products/${this.id}` }
    });
  }
}
