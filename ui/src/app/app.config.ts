import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { API_BASE_URL, apiBaseUrlInterceptor } from './config/api.config';
import { authErrorInterceptor } from './config/auth-error.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    { provide: API_BASE_URL, useValue: 'http://localhost:8080' },
    provideHttpClient(
      withFetch(),
      withInterceptors([apiBaseUrlInterceptor, authErrorInterceptor]),
    )
  ]
};
