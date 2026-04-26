import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule
  ],
  template: `
    <div class="auth-container">
      <mat-card class="auth-card">
        <mat-card-header>
          <mat-card-title>Giriş Yap</mat-card-title>
          <mat-card-subtitle>Hesabınıza erişin</mat-card-subtitle>
        </mat-card-header>

        <mat-card-content>
          <!-- Reactive form — tek yönlü değil iki yönlü veri bağlama -->
          <form [formGroup]="loginForm" (ngSubmit)="onSubmit()">

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>E-posta</mat-label>
              <input matInput type="email" formControlName="email"
                     placeholder="ornek@email.com" autocomplete="email">
              <mat-icon matSuffix>email</mat-icon>
              <!-- Hata mesajları: @if ile Angular 17 control flow -->
              @if (loginForm.get('email')?.hasError('required') && loginForm.get('email')?.touched) {
                <mat-error>E-posta zorunludur</mat-error>
              }
              @if (loginForm.get('email')?.hasError('email')) {
                <mat-error>Geçerli bir e-posta girin</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Şifre</mat-label>
              <input matInput
                     [type]="showPassword() ? 'text' : 'password'"
                     formControlName="password"
                     autocomplete="current-password">
              <!-- Şifre göster/gizle toggle -->
              <button mat-icon-button matSuffix type="button"
                      (click)="showPassword.set(!showPassword())">
                <mat-icon>{{ showPassword() ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
              @if (loginForm.get('password')?.hasError('required') && loginForm.get('password')?.touched) {
                <mat-error>Şifre zorunludur</mat-error>
              }
            </mat-form-field>

            <!-- Hata mesajı signal'dan okunur -->
            @if (errorMessage()) {
              <div class="error-text" style="margin-bottom: 16px;">
                {{ errorMessage() }}
              </div>
            }

            <button mat-raised-button color="primary" type="submit"
                    class="full-width submit-btn"
                    [disabled]="loginForm.invalid || isLoading()">
              @if (isLoading()) {
                <mat-spinner diameter="20" />
              } @else {
                Giriş Yap
              }
            </button>
          </form>
        </mat-card-content>

        <mat-card-actions>
          <p>Hesabınız yok mu? <a routerLink="/auth/register">Kayıt olun</a></p>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .auth-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: calc(100vh - 64px);
    }
    .auth-card { width: 100%; max-width: 420px; padding: 16px; }
    .submit-btn { margin-top: 8px; height: 48px; }
    mat-card-actions { justify-content: center; text-align: center; }
  `]
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly snackBar = inject(MatSnackBar);

  // Signal: template'de () ile okunur, reactive
  readonly showPassword = signal(false);
  readonly isLoading = signal(false);
  readonly errorMessage = signal('');

  // Reactive form — FormBuilder ile kısa sözdizimi
  readonly loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');

    const { email, password } = this.loginForm.getRawValue();

    this.authService.login({ email: email!, password: password! }).subscribe({
      next: () => {
        this.snackBar.open('Giriş başarılı!', '', { panelClass: ['success-snackbar'] });
        // returnUrl: güvenli yönlendirme — sadece local path'lere izin ver
        const returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/products';
        this.router.navigateByUrl(returnUrl.startsWith('/') ? returnUrl : '/products');
      },
      error: (err) => {
        this.isLoading.set(false);
        const msg = err.error?.message || 'E-posta veya şifre hatalı';
        this.errorMessage.set(msg);
      }
    });
  }
}
