import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';
import { NgIf } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NgIf],
  templateUrl: './app.html',
  styleUrl: './app.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App implements OnInit {
  private readonly authService = inject(AuthService);

  readonly user = computed(() => this.authService.profile());
  readonly isAuthenticated = computed(() => this.authService.isAuthenticated());
  readonly currentYear = new Date().getFullYear();

  async ngOnInit(): Promise<void> {
    try {
      await this.authService.ensureProfileLoaded();
    } catch (error) {
      console.warn('无法加载用户信息', error);
    }
  }

  async logout(): Promise<void> {
    await this.authService.logout();
  }
}
