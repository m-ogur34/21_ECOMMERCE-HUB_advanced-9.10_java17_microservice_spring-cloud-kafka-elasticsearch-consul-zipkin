import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';

// Lazy loading: her route kendi chunk'ına derlenir — başlangıç bundle küçük kalır
export const routes: Routes = [
  {
    path: '',
    redirectTo: 'products',
    pathMatch: 'full'
  },
  {
    path: 'auth',
    // Guest guard: giriş yapmış kullanıcıyı login sayfasına yönlendirme
    canActivate: [guestGuard],
    loadChildren: () =>
      import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES)
  },
  {
    path: 'products',
    loadChildren: () =>
      import('./features/product/product.routes').then(m => m.PRODUCT_ROUTES)
  },
  {
    path: 'orders',
    // Auth guard: giriş yapılmamışsa login'e yönlendir
    canActivate: [authGuard],
    loadChildren: () =>
      import('./features/order/order.routes').then(m => m.ORDER_ROUTES)
  },
  {
    path: 'cart',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/cart/cart.component').then(m => m.CartComponent)
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/profile/profile.component').then(m => m.ProfileComponent)
  },
  {
    path: '**',
    loadComponent: () =>
      import('./shared/components/not-found/not-found.component').then(m => m.NotFoundComponent)
  }
];
