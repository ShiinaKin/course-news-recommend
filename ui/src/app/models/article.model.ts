export interface ArticleSummary {
  id: number;
  title: string;
  summary: string;
  publishTime?: string | null;
  tags: string[];
  score?: number | null;
}

export interface PagedArticleResponse {
  page: number;
  size: number;
  total: number;
  items: ArticleSummary[];
}

export interface ArticleDetail {
  id: number;
  title: string;
  content: string;
  source?: string | null;
  publishTime?: string | null;
  tags: string[];
  related: ArticleSummary[];
}

