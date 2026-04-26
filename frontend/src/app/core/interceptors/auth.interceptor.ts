import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

// Angular 17 functional interceptor — class tabanlı değil, fonksiyon tabanlı.
// HttpInterceptorFn: (req, next) => Observable<HttpEvent<unknown>>
export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const token = authService.getAccessToken();

  // Token yoksa isteği olduğu gibi gönder (public endpoint'ler için)
  if (!token) {
    return next(req);
  }

  // Authorization header eklenmiş yeni request oluştur
  // HttpRequest immutable — clone() ile değiştirilir
  const authReq = req.clone({
    setHeaders: { Authorization: `Bearer ${token}` }
  });

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // 401: Token süresi dolmuş → refresh token ile yenile
      if (error.status === 401 && !req.url.includes('/auth/')) {
        return authService.refreshToken().pipe(
          switchMap(response => {
            // Yeni token ile orijinal isteği tekrarla
            const retryReq = req.clone({
              setHeaders: { Authorization: `Bearer ${response.accessToken}` }
            });
            return next(retryReq);
          }),
          catchError(refreshError => {
            // Refresh token da geçersiz → logout
            authService.logout();
            return throwError(() => refreshError);
          })
        );
      }
      return throwError(() => error);
    })
  );
};
