import { NgFor, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subscription, firstValueFrom, timer } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { MediaJob } from '../../models/media-job.model';
import { MediaJobService } from '../../services/media-job.service';
import { extractErrorMessage } from '../../utils/error-utils';

interface RssImportResponse {
  importedCount: number;
}

@Component({
  standalone: true,
  selector: 'app-upload',
  imports: [NgIf, NgFor],
  templateUrl: './upload.component.html',
  styleUrl: './upload.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UploadComponent {
  private readonly mediaJobService = inject(MediaJobService);
  private readonly httpClient = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  private readonly allowedAudioExtensions = ['.mp3', '.wav'];
  private readonly allowedAudioMimeTypes = ['audio/mpeg', 'audio/mp3', 'audio/wav', 'audio/x-wav', 'audio/wave', 'audio/x-pn-wav'];

  readonly audioFile = signal<File | null>(null);
  readonly imageFile = signal<File | null>(null);
  readonly uploading = signal(false);
  readonly message = signal<string | null>(null);
  readonly currentJob = signal<MediaJob | null>(null);
  readonly rssUrl = signal('');
  readonly rssLoading = signal(false);
  readonly rssMessage = signal<string | null>(null);
  readonly rssPresets = signal([
    { label: '少数派', url: 'https://sspai.com/feed' },
    { label: '南方周末', url: 'https://rsshub.app/infzm/2' },
    { label: '联合早报', url: 'https://plink.anyfeeder.com/zaobao/realtime/china' },
  ]);

  private pollSubscription: Subscription | null = null;

  constructor() {
    this.destroyRef.onDestroy(() => this.stopPolling());
  }

  onAudioFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.item(0) ?? null;
    if (!file) {
      this.audioFile.set(null);
      return;
    }
    const name = file.name.toLowerCase();
    const type = file.type.toLowerCase();
    const validByExt = this.allowedAudioExtensions.some(ext => name.endsWith(ext));
    const validByMime = this.allowedAudioMimeTypes.includes(type);
    if (!validByExt && !validByMime) {
      this.audioFile.set(null);
      this.message.set('仅支持上传 mp3 或 wav 音频文件');
      input.value = '';
      return;
    }
    this.message.set(null);
    this.audioFile.set(file);
  }

  onImageFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.imageFile.set(input.files?.item(0) ?? null);
  }

  async submitAudio(): Promise<void> {
    const file = this.audioFile();
    if (!file) {
      this.message.set('请选择要上传的音频文件');
      return;
    }
    await this.submit(this.mediaJobService.uploadAudio(file));
  }

  async submitImage(): Promise<void> {
    const file = this.imageFile();
    if (!file) {
      this.message.set('请选择要上传的图片文件');
      return;
    }
    await this.submit(this.mediaJobService.uploadImage(file));
  }

  private async submit(request$: Observable<MediaJob>): Promise<void> {
    this.uploading.set(true);
    this.message.set(null);
    try {
      const job = await firstValueFrom(request$);
      this.currentJob.set(job);
      this.startPolling(job.id);
    } catch (error) {
      this.message.set(extractErrorMessage(error));
    } finally {
      this.uploading.set(false);
    }
  }

  private startPolling(jobId: number): void {
    this.stopPolling();
    this.pollSubscription = timer(1500, 1500)
      .pipe(switchMap(() => this.mediaJobService.getJob(jobId)))
      .subscribe({
        next: job => {
          this.currentJob.set(job);
          if (job.status === 'DONE' || job.status === 'FAILED') {
            this.stopPolling();
          }
        },
        error: error => {
          this.message.set(extractErrorMessage(error));
          this.stopPolling();
        },
      });
  }

  private stopPolling(): void {
    this.pollSubscription?.unsubscribe();
    this.pollSubscription = null;
  }

  onRssUrlChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.rssUrl.set(input.value);
  }

  useRssSource(url: string): void {
    this.rssUrl.set(url);
    void this.fetchRss();
  }

  async fetchRss(): Promise<void> {
    const url = this.rssUrl().trim();
    if (!url) {
      this.rssMessage.set('请输入 RSS 源地址');
      return;
    }
    this.rssLoading.set(true);
    this.rssMessage.set(null);
    try {
      const response = await firstValueFrom(
        this.httpClient.post<RssImportResponse>(
          '/api/rss/import',
          { rssLink: url },
          {
            withCredentials: true,
          },
        ),
      );
      const count = response.importedCount ?? 0;
      this.rssMessage.set(`已成功获取文章${count}条`);
    } catch (error) {
      console.error('Failed to fetch RSS', error);
      this.rssMessage.set('获取 RSS 失败，请稍后重试');
    } finally {
      this.rssLoading.set(false);
    }
  }
}
