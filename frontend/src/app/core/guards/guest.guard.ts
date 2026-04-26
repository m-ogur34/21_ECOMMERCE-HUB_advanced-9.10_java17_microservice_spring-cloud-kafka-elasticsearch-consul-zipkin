import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// Giriş yapmış kullanıcıyı auth sayfalarından (login/register) uzak tut
export const guestGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return true;
  }

  // Zaten giriş yapılmış → ürün listesine yönlendir
  return router.createUrlTree(['/products']);
};
