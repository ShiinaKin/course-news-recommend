import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MediaJob } from '../models/media-job.model';

@Injectable({ providedIn: 'root' })
export class MediaJobService {
  private readonly http = inject(HttpClient);

  uploadAudio(file: File): Observable<MediaJob> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<MediaJob>('/api/upload/audio', formData, { withCredentials: true });
  }

  uploadImage(file: File): Observable<MediaJob> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<MediaJob>('/api/upload/image', formData, { withCredentials: true });
  }

  getJob(id: number): Observable<MediaJob> {
    return this.http.get<MediaJob>(`/api/jobs/${id}`, { withCredentials: true });
  }
}

