import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ArticleDetail, ArticleSummary, PagedArticleResponse } from '../models/article.model';

interface RecommendResponse {
  items: ArticleSummary[];
}

@Injectable({ providedIn: 'root' })
export class ArticleService {
  private readonly http = inject(HttpClient);

  listArticles(params: { page?: number; size?: number; orderBy?: string }): Observable<PagedArticleResponse> {
    let httpParams = new HttpParams();
    if (params.page !== undefined) {
      httpParams = httpParams.set('page', params.page.toString());
    }
    if (params.size !== undefined) {
      httpParams = httpParams.set('size', params.size.toString());
    }
    if (params.orderBy) {
      httpParams = httpParams.set('orderBy', params.orderBy);
    }
    return this.http.get<PagedArticleResponse>('/api/articles', {
      params: httpParams,
      withCredentials: true,
    });
  }

  listLatest(size = 10): Observable<PagedArticleResponse> {
    return this.listArticles({ page: 0, size, orderBy: 'time' });
  }

  getArticle(id: number): Observable<ArticleDetail> {
    return this.http.get<ArticleDetail>(`/api/articles/${id}`, { withCredentials: true });
  }

  recommend(topK = 10): Observable<ArticleSummary[]> {
    const params = new HttpParams().set('topK', topK.toString());
    return this.http.get<RecommendResponse>('/api/recommend', {
      params,
      withCredentials: true,
    }).pipe(map(response => response.items));
  }
}
