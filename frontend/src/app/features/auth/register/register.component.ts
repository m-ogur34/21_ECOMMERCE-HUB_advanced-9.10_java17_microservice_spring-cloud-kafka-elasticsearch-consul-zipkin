import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../../../core/services/auth.service';

// Özel validator: iki şifre alanının eşleştiğini kontrol eder
function passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;
  return password === confirmPassword ? null : { passwordMismatch: true };
}

@Component({
  selector: 'app-register',
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
          <mat-card-title>Kayıt Ol</mat-card-title>
          <mat-card-subtitle>Yeni hesap oluşturun</mat-card-subtitle>
        </mat-card-header>

        <mat-card-content>
          <form [formGroup]="registerForm" (ngSubmit)="onSubmit()">

            <div class="name-row">
              <mat-form-field appearance="outline">
                <mat-label>Ad</mat-label>
                <input matInput formControlName="firstName" autocomplete="given-name">
                @if (getError('firstName', 'required')) {
                  <mat-error>Ad zorunludur</mat-error>
                }
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Soyad</mat-label>
                <input matInput formControlName="lastName" autocomplete="family-name">
                @if (getError('lastName', 'required')) {
                  <mat-error>Soyad zorunludur</mat-error>
                }
              </mat-form-field>
            </div>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>E-posta</mat-label>
              <input matInput type="email" formControlName="email" autocomplete="email">
              <mat-icon matSuffix>email</mat-icon>
              @if (getError('email', 'required')) {
                <mat-error>E-posta zorunludur</mat-error>
              }
              @if (getError('email', 'email')) {
                <mat-error>Geçerli bir e-posta girin</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Şifre</mat-label>
              <input matInput
                     [type]="showPassword() ? 'text' : 'password'"
                     formControlName="password"
                     autocomplete="new-password">
              <button mat-icon-button matSuffix type="button"
                      (click)="showPassword.set(!showPassword())">
                <mat-icon>{{ showPassword() ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
              @if (getError('password', 'required')) {
                <mat-error>Şifre zorunludur</mat-error>
              }
              @if (getError('password', 'minlength')) {
                <mat-error>Şifre en az 8 karakter olmalıdır</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Şifre Tekrar</mat-label>
              <input matInput
                     [type]="showPassword() ? 'text' : 'password'"
                     formControlName="confirmPassword"
                     autocomplete="new-password">
              <!-- FormGroup level error: passwordMismatch -->
              @if (registerForm.hasError('passwordMismatch') && registerForm.get('confirmPassword')?.touched) {
                <mat-error>Şifreler eşleşmiyor</mat-error>
              }
            </mat-form-field>

            @if (errorMessage()) {
              <div class="error-text" style="margin-bottom: 16px;">{{ errorMessage() }}</div>
            }

            <button mat-raised-button color="primary" type="submit"
                    class="full-width submit-btn"
                    [disabled]="registerForm.invalid || isLoading()">
              @if (isLoading()) {
                <mat-spinner diameter="20" />
              } @else {
                Kayıt Ol
              }
            </button>
          </form>
        </mat-card-content>

        <mat-card-actions>
          <p>Zaten hesabınız var mı? <a routerLink="/auth/login">Giriş yapın</a></p>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .auth-container { display: flex; justify-content: center; align-items: center; min-height: calc(100vh - 64px); }
    .auth-card { width: 100%; max-width: 480px; padding: 16px; }
    .name-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
    .submit-btn { margin-top: 8px; height: 48px; }
    mat-card-actions { justify-content: center; text-align: center; }
  `]
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  readonly showPassword = signal(false);
  readonly isLoading = signal(false);
  readonly errorMessage = signal('');

  readonly registerForm = this.fb.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required]
  }, { validators: passwordMatchValidator });

  // Kısa helper — template'de uzun ifade yerine
  getError(field: string, error: string): boolean {
    const control = this.registerForm.get(field);
    return !!(control?.hasError(error) && control.touched);
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');

    const { firstName, lastName, email, password } = this.registerForm.getRawValue();

    this.authService.register({
      firstName: firstName!,
      lastName: lastName!,
      email: email!,
      password: password!
    }).subscribe({
      next: () => {
        this.snackBar.open('Kayıt başarılı! Hoş geldiniz.', '', { panelClass: ['success-snackbar'] });
        this.router.navigate(['/products']);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Kayıt sırasında hata oluştu');
      }
    });
  }
}
