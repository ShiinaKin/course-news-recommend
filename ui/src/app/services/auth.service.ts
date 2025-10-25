import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { SimpleMessageResponse, UserProfile } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly profileSignal = signal<UserProfile | null>(null);
  private readonly loadingSignal = signal(false);
  private initialized = false;

  readonly profile = computed(() => this.profileSignal());
  readonly isAuthenticated = computed(() => this.profileSignal() !== null);
  readonly loading = computed(() => this.loadingSignal());

  async ensureProfileLoaded(): Promise<void> {
    if (this.initialized) {
      return;
    }
    this.initialized = true;
    await this.refreshProfile().catch(() => undefined);
  }

  async refreshProfile(): Promise<void> {
    this.loadingSignal.set(true);
    try {
      const profile = await firstValueFrom(
        this.http.get<UserProfile>('/api/user/profile', { withCredentials: true }),
      );
      this.profileSignal.set(profile);
    } catch (error) {
      this.profileSignal.set(null);
      if (error instanceof HttpErrorResponse && error.status === 401) {
        return;
      }
      throw error;
    } finally {
      this.loadingSignal.set(false);
    }
  }

  async login(username: string, password: string): Promise<void> {
    await firstValueFrom(
      this.http.post<SimpleMessageResponse>(
        '/api/auth/login',
        { username, password },
        { withCredentials: true },
      ),
    );
    await this.refreshProfile().catch(() => undefined);
  }

  async logout(): Promise<void> {
    try {
      await firstValueFrom(
        this.http.post<SimpleMessageResponse>(
          '/api/auth/logout',
          {},
          { withCredentials: true },
        ),
      );
    } finally {
      this.profileSignal.set(null);
    }
  }

  async register(username: string, password: string, nickname: string): Promise<string> {
    const response = await firstValueFrom(
      this.http.post<SimpleMessageResponse>(
        '/api/auth/register',
        { username, password, nickname },
        { withCredentials: true },
      ),
    );
    return response.message;
  }
}
