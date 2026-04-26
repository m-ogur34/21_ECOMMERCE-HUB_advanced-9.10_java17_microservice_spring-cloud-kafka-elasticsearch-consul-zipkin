import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// Functional guard — Angular 14+ ile class Guard deprecated
// Giriş yapılmamışsa login sayfasına yönlendir
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  // returnUrl: login'den sonra tekrar bu sayfaya dön
  return router.createUrlTree(['/auth/login'], {
    queryParams: { returnUrl: router.url }
  });
};
