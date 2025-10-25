import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Tag } from '../models/tag.model';
import { SimpleMessageResponse, UserProfile } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);

  getProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>('/api/user/profile', { withCredentials: true });
  }

  listTags(): Observable<Tag[]> {
    return this.http.get<Tag[]>('/api/tags', { withCredentials: true });
  }

  saveTags(tagIds: number[]): Observable<SimpleMessageResponse> {
    return this.http.post<SimpleMessageResponse>(
      '/api/user/tags',
      { tagIds },
      { withCredentials: true },
    );
  }
}

