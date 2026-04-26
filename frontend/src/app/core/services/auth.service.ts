import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap, catchError, throwError } from 'rxjs';
import { environment } from '@env/environment';
import {
  LoginRequest, RegisterRequest, AuthResponse,
  TokenRefreshRequest, JwtPayload
} from '../models/auth.model';

// Injection token'sız — Angular 17'de injectable sınıflar için
// providedIn: 'root' singleton oluşturur (tüm app paylaşır)
@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly TOKEN_KEY = 'access_token';
  private readonly REFRESH_KEY = 'refresh_token';
  private readonly apiUrl = `${environment.apiUrl}/auth`;

  // Angular 17 Signals — reaktif state yönetimi
  // signal() ile değer set edildiğinde bağımlı component'ler otomatik güncellenir
  private readonly _currentUser = signal<JwtPayload | null>(this.decodeStoredToken());
  private readonly _isLoading = signal<boolean>(false);

  // computed: _currentUser değişince otomatik hesaplanır
  readonly isAuthenticated = computed(() => {
    const user = this._currentUser();
    if (!user) return false;
    // Token süresi dolmuş mu? exp: Unix timestamp (saniye cinsinden)
    return user.exp * 1000 > Date.now();
  });

  readonly currentUser = this._currentUser.asReadonly();
  readonly isAdmin = computed(() =>
    this._currentUser()?.roles?.includes('ROLE_ADMIN') ?? false
  );
  readonly isLoading = this._isLoading.asReadonly();

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router
  ) {}

  login(request: LoginRequest) {
    this._isLoading.set(true);
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, request).pipe(
      tap(response => {
        this.storeTokens(response);
        this._currentUser.set(this.decodeToken(response.accessToken));
      }),
      catchError(err => {
        this._isLoading.set(false);
        return throwError(() => err);
      }),
      tap(() => this._isLoading.set(false))
    );
  }

  register(request: RegisterRequest) {
    this._isLoading.set(true);
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, request).pipe(
      tap(response => {
        this.storeTokens(response);
        this._currentUser.set(this.decodeToken(response.accessToken));
      }),
      catchError(err => {
        this._isLoading.set(false);
        return throwError(() => err);
      }),
      tap(() => this._isLoading.set(false))
    );
  }

  refreshToken() {
    const refreshToken = localStorage.getItem(this.REFRESH_KEY);
    if (!refreshToken) return throwError(() => new Error('Refresh token yok'));

    const body: TokenRefreshRequest = { refreshToken };
    return this.http.post<AuthResponse>(`${this.apiUrl}/refresh-token`, body).pipe(
      tap(response => {
        this.storeTokens(response);
        this._currentUser.set(this.decodeToken(response.accessToken));
      })
    );
  }

  logout() {
    const refreshToken = localStorage.getItem(this.REFRESH_KEY);
    if (refreshToken) {
      // Backend'e logout isteği gönder (refresh token'ı DB'den sil)
      this.http.post(`${this.apiUrl}/logout`, { refreshToken }).subscribe({
        error: () => {} // Logout başarısız olsa bile local state temizle
      });
    }
    this.clearTokens();
    this._currentUser.set(null);
    this.router.navigate(['/auth/login']);
  }

  getAccessToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  isTokenExpired(): boolean {
    const user = this._currentUser();
    if (!user) return true;
    return user.exp * 1000 <= Date.now();
  }

  private storeTokens(response: AuthResponse): void {
    localStorage.setItem(this.TOKEN_KEY, response.accessToken);
    localStorage.setItem(this.REFRESH_KEY, response.refreshToken);
  }

  private clearTokens(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_KEY);
  }

  // JWT'nin payload kısmını decode et — imza doğrulaması yapılmaz (backend yapar)
  private decodeToken(token: string): JwtPayload | null {
    try {
      const payload = token.split('.')[1];
      // Base64URL → Base64 dönüşümü
      const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
      return JSON.parse(decoded) as JwtPayload;
    } catch {
      return null;
    }
  }

  private decodeStoredToken(): JwtPayload | null {
    const token = localStorage.getItem(this.TOKEN_KEY);
    return token ? this.decodeToken(token) : null;
  }
}
