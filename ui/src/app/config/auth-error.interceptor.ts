import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

const AUTH_ENDPOINT_PREFIX = '/api/auth/';

export const authErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  return next(req).pipe(
    catchError(error => {
      if (error instanceof HttpErrorResponse) {
        if ((error.status === 401 || error.status === 403) && !isAuthEndpoint(req.url)) {
          if (router.url !== '/login') {
            const redirectTarget =
              typeof window !== 'undefined'
                ? window.location.pathname + window.location.search
                : router.url;
            void router.navigate(['/login'], {
              queryParams: {
                redirect: redirectTarget,
              },
            });
          }
        }
      }
      return throwError(() => error);
    }),
  );
};

function isAuthEndpoint(url: string): boolean {
  if (!url) {
    return false;
  }
  try {
    const parsed = new URL(url, window.location.origin);
    return parsed.pathname.startsWith(AUTH_ENDPOINT_PREFIX);
  } catch {
    return url.startsWith(AUTH_ENDPOINT_PREFIX);
  }
}
