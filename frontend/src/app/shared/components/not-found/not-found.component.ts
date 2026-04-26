import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule],
  template: `
    <div style="text-align: center; padding: 80px 16px;">
      <mat-icon style="font-size: 96px; width: 96px; height: 96px; color: #9e9e9e;">
        search_off
      </mat-icon>
      <h1 style="font-size: 48px; color: #616161; margin: 16px 0 8px;">404</h1>
      <p style="color: #757575; margin-bottom: 32px;">
        Aradığınız sayfa bulunamadı.
      </p>
      <a mat-raised-button color="primary" routerLink="/products">
        <mat-icon>home</mat-icon> Ana Sayfaya Dön
      </a>
    </div>
  `
})
export class NotFoundComponent {}
