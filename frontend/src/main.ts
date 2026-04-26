// Angular 17 standalone bootstrap — NgModule yok, AppModule yok.
// bootstrapApplication: module'suz uygulama başlatır.
import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';

bootstrapApplication(AppComponent, appConfig)
  .catch(err => console.error('Uygulama başlatma hatası:', err));
