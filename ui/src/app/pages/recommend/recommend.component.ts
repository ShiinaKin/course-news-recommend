import { DatePipe, DecimalPipe, NgForOf, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ArticleSummary } from '../../models/article.model';
import { ArticleService } from '../../services/article.service';
import { AuthService } from '../../services/auth.service';
import { extractErrorMessage } from '../../utils/error-utils';

@Component({
  standalone: true,
  selector: 'app-recommend',
  imports: [NgIf, NgForOf, RouterLink, DatePipe, DecimalPipe],
  templateUrl: './recommend.component.html',
  styleUrl: './recommend.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RecommendComponent implements OnInit {
  private readonly articleService = inject(ArticleService);
  private readonly authService = inject(AuthService);

  readonly articles = signal<ArticleSummary[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly isAuthenticated = computed(() => this.authService.isAuthenticated());

  async ngOnInit(): Promise<void> {
    await this.authService.ensureProfileLoaded().catch(() => undefined);
    if (this.isAuthenticated()) {
      await this.loadRecommendations();
    }
  }

  async loadRecommendations(): Promise<void> {
    if (!this.isAuthenticated()) {
      this.error.set('请先登录以查看推荐。');
      this.articles.set([]);
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    try {
      const items = await firstValueFrom(this.articleService.recommend(10));
      this.articles.set(items);
    } catch (error) {
      this.error.set(extractErrorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }
}
