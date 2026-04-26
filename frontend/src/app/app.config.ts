import { ApplicationConfig, importProvidersFrom } from '@angular/core';
import { provideRouter, withComponentInputBinding, withViewTransitions } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideClientHydration } from '@angular/platform-browser';
import { MAT_SNACK_BAR_DEFAULT_OPTIONS } from '@angular/material/snack-bar';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';

// Angular 17 standalone: tüm provider'lar burada tanımlanır, AppModule yok.
export const appConfig: ApplicationConfig = {
  providers: [
    // Router: withComponentInputBinding → route parametrelerini @Input() ile al
    //         withViewTransitions → sayfa geçiş animasyonları (Angular 17+)
    provideRouter(routes, withComponentInputBinding(), withViewTransitions()),

    // HttpClient: withInterceptors → functional interceptor API (Angular 15+)
    // Interceptor sırası önemli: önce auth, sonra error
    provideHttpClient(
      withInterceptors([authInterceptor, errorInterceptor])
    ),

    // Angular Material animasyonları
    provideAnimations(),

    // Snackbar varsayılan ayarları — tüm app'te geçerli
    {
      provide: MAT_SNACK_BAR_DEFAULT_OPTIONS,
      useValue: { duration: 3000, horizontalPosition: 'end', verticalPosition: 'top' }
    }
  ]
};
