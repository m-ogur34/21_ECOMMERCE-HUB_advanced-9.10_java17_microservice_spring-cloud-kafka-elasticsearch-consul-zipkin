import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';

// Global hata yakalama interceptor'u
// Tüm HTTP hatalarını yakalar, kullanıcıya snackbar gösterir
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const snackBar = inject(MatSnackBar);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // Auth isteklerinde zaten login sayfasında hata gösterilir
      if (req.url.includes('/auth/login') || req.url.includes('/auth/register')) {
        return throwError(() => error);
      }

      let message = 'Bir hata oluştu';

      if (error.status === 0) {
        // Network hatası: sunucuya ulaşılamıyor
        message = 'Sunucuya bağlanılamıyor. İnternet bağlantınızı kontrol edin.';
      } else if (error.status === 403) {
        message = 'Bu işlem için yetkiniz yok';
      } else if (error.status === 404) {
        message = 'İstenen kaynak bulunamadı';
      } else if (error.status === 409) {
        message = error.error?.message || 'Çakışma hatası';
      } else if (error.status >= 500) {
        message = 'Sunucu hatası. Lütfen daha sonra tekrar deneyin.';
      } else if (error.error?.message) {
        message = error.error.message;
      }

      snackBar.open(message, 'Kapat', { panelClass: ['error-snackbar'] });
      return throwError(() => error);
    })
  );
};
