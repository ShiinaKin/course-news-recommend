import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { KnowledgeEntityType, KnowledgeGraphResponse } from '../models/knowledge-graph.model';

export type GraphFocus = Extract<KnowledgeEntityType, 'USER' | 'ARTICLE' | 'TAG'>;

@Injectable({ providedIn: 'root' })
export class KnowledgeGraphService {
  private readonly http = inject(HttpClient);

  fetchGraph(focusType?: GraphFocus): Observable<KnowledgeGraphResponse> {
    let params = new HttpParams();
    if (focusType) {
      params = params.set('focusType', focusType);
    }
    return this.http.get<KnowledgeGraphResponse>('/api/graph', {
      params,
      withCredentials: true,
    });
  }
}
