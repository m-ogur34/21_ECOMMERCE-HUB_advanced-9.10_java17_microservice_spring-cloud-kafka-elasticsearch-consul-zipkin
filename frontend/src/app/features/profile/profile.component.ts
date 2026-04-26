import { Component, inject } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatIconModule, MatChipsModule, RouterLink, DatePipe],
  template: `
    <h1 style="margin-bottom: 24px">Profilim</h1>
    <mat-card style="max-width: 480px;">
      <mat-card-header>
        <div mat-card-avatar style="background: #1976d2; display:flex; align-items:center; justify-content:center; border-radius:50%">
          <mat-icon style="color:white">account_circle</mat-icon>
        </div>
        <mat-card-title>{{ authService.currentUser()?.sub }}</mat-card-title>
        <mat-card-subtitle>
          Token süresi: {{ tokenExpiry() }}
        </mat-card-subtitle>
      </mat-card-header>

      <mat-card-content style="padding-top: 16px;">
        <div style="margin-bottom: 16px;">
          <p style="color: #757575; font-size: 13px; margin-bottom: 8px;">ROLLER</p>
          <div style="display: flex; gap: 8px; flex-wrap: wrap;">
            @for (role of authService.currentUser()?.roles ?? []; track role) {
              <mat-chip [color]="role === 'ROLE_ADMIN' ? 'warn' : 'primary'" selected>
                {{ role.replace('ROLE_', '') }}
              </mat-chip>
            }
          </div>
        </div>
      </mat-card-content>

      <mat-card-actions>
        <a mat-button routerLink="/orders">
          <mat-icon>receipt_long</mat-icon> Siparişlerim
        </a>
        <button mat-button color="warn" (click)="authService.logout()">
          <mat-icon>logout</mat-icon> Çıkış Yap
        </button>
      </mat-card-actions>
    </mat-card>
  `
})
export class ProfileComponent {
  readonly authService = inject(AuthService);

  tokenExpiry(): string {
    const exp = this.authService.currentUser()?.exp;
    if (!exp) return '';
    return new Date(exp * 1000).toLocaleString('tr-TR');
  }
}
