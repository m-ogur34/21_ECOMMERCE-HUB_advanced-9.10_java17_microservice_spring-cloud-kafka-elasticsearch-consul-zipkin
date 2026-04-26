import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
import { MatMenuModule } from '@angular/material/menu';
import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';

// OnPush: yalnızca Input değişince veya signal güncellenince re-render
@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    RouterLink, RouterLinkActive,
    MatToolbarModule, MatButtonModule, MatIconModule,
    MatBadgeModule, MatMenuModule
  ],
  template: `
    <mat-toolbar color="primary">
      <a routerLink="/products" class="brand">
        <mat-icon>shopping_bag</mat-icon>
        ECommerceHub
      </a>

      <span class="spacer"></span>

      <!-- Ürün arama linki -->
      <a mat-button routerLink="/products" routerLinkActive="active-link">
        <mat-icon>inventory_2</mat-icon> Ürünler
      </a>

      @if (authService.isAuthenticated()) {
        <!-- Sepet — badge ile ürün sayısı göster -->
        <a mat-icon-button routerLink="/cart"
           [matBadge]="cartService.totalItems()"
           [matBadgeHidden]="cartService.isEmpty()"
           matBadgeColor="accent">
          <mat-icon>shopping_cart</mat-icon>
        </a>

        <!-- Sipariş geçmişi -->
        <a mat-button routerLink="/orders" routerLinkActive="active-link">
          <mat-icon>receipt_long</mat-icon> Siparişlerim
        </a>

        <!-- Kullanıcı menüsü -->
        <button mat-icon-button [matMenuTriggerFor]="userMenu">
          <mat-icon>account_circle</mat-icon>
        </button>
        <mat-menu #userMenu="matMenu">
          <span mat-menu-item disabled>{{ authService.currentUser()?.sub }}</span>
          <a mat-menu-item routerLink="/profile">
            <mat-icon>person</mat-icon> Profil
          </a>
          @if (authService.isAdmin()) {
            <a mat-menu-item routerLink="/admin">
              <mat-icon>admin_panel_settings</mat-icon> Admin Panel
            </a>
          }
          <button mat-menu-item (click)="logout()">
            <mat-icon>logout</mat-icon> Çıkış Yap
          </button>
        </mat-menu>
      } @else {
        <a mat-button routerLink="/auth/login">
          <mat-icon>login</mat-icon> Giriş Yap
        </a>
        <a mat-raised-button color="accent" routerLink="/auth/register">
          Kayıt Ol
        </a>
      }
    </mat-toolbar>
  `,
  styles: [`
    mat-toolbar { position: sticky; top: 0; z-index: 100; }
    .brand { display: flex; align-items: center; gap: 8px; color: white; text-decoration: none; font-size: 1.2rem; font-weight: 500; }
    .spacer { flex: 1 1 auto; }
    .active-link { background: rgba(255,255,255,0.15); border-radius: 4px; }
  `]
})
export class NavbarComponent {
  // inject() — Angular 17 functional injection (constructor'a gerek yok)
  readonly authService = inject(AuthService);
  readonly cartService = inject(CartService);

  logout(): void {
    this.authService.logout();
  }
}
