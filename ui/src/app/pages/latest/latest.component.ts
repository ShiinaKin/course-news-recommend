import { DatePipe, DecimalPipe, NgForOf, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ArticleSummary } from '../../models/article.model';
import { ArticleService } from '../../services/article.service';
import { extractErrorMessage } from '../../utils/error-utils';

@Component({
  standalone: true,
  selector: 'app-latest',
  imports: [NgIf, NgForOf, RouterLink, DatePipe, DecimalPipe],
  templateUrl: './latest.component.html',
  styleUrl: './latest.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LatestComponent implements OnInit {
  private readonly articleService = inject(ArticleService);

  readonly latestArticles = signal<ArticleSummary[]>([]);
  readonly latestLoading = signal(false);
  readonly latestError = signal<string | null>(null);

  ngOnInit(): void {
    this.loadLatest();
  }

  async loadLatest(): Promise<void> {
    this.latestLoading.set(true);
    this.latestError.set(null);
    try {
      const response = await firstValueFrom(this.articleService.listLatest(10));
      this.latestArticles.set(response.items);
    } catch (error) {
      this.latestError.set(extractErrorMessage(error));
    } finally {
      this.latestLoading.set(false);
    }
  }
}
