import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './shared/components/navbar/navbar.component';

// Angular 17 standalone component — NgModule gerektirmez
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent],
  template: `
    <app-navbar />
    <main class="container page-content">
      <!-- Router outlet: aktif route'un component'ini render eder -->
      <router-outlet />
    </main>
  `
})
export class AppComponent {}
