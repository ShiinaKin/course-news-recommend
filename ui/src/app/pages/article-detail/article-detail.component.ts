import { DatePipe, NgForOf, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter, map, switchMap, tap } from 'rxjs/operators';
import { ArticleDetail } from '../../models/article.model';
import { ArticleService } from '../../services/article.service';
import { extractErrorMessage } from '../../utils/error-utils';

@Component({
  standalone: true,
  selector: 'app-article-detail',
  imports: [NgIf, NgForOf, RouterLink, DatePipe],
  templateUrl: './article-detail.component.html',
  styleUrl: './article-detail.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ArticleDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly articleService = inject(ArticleService);

  readonly article = signal<ArticleDetail | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly contentParagraphs = computed(() => {
    const content = this.article()?.content;
    if (!content) {
      return [] as string[];
    }
    return content.split(/\r?\n/).filter(Boolean);
  });

  constructor() {
    this.route.paramMap
      .pipe(
        map(params => Number(params.get('id'))),
        filter(id => !Number.isNaN(id) && id > 0),
        tap(() => {
          this.loading.set(true);
          this.error.set(null);
        }),
        switchMap(id => this.articleService.getArticle(id)),
        takeUntilDestroyed(),
      )
      .subscribe({
        next: article => {
          this.article.set(article);
          this.loading.set(false);
        },
        error: error => {
          this.error.set(extractErrorMessage(error));
          this.loading.set(false);
        },
      });
  }
}
