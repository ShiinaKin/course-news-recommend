import { HttpInterceptorFn } from '@angular/common/http';
import { InjectionToken, inject } from '@angular/core';

export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL');

const ABSOLUTE_URL_REGEX = /^https?:\/\//i;

export const apiBaseUrlInterceptor: HttpInterceptorFn = (req, next) => {
  const baseUrl = inject(API_BASE_URL, { optional: true });
  if (!baseUrl || baseUrl.trim().length === 0) {
    return next(req);
  }
  if (ABSOLUTE_URL_REGEX.test(req.url)) {
    return next(req);
  }
  const normalizedBase = baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl;
  const normalizedPath = req.url.startsWith('/') ? req.url : `/${req.url}`;
  return next(req.clone({ url: `${normalizedBase}${normalizedPath}` }));
};

