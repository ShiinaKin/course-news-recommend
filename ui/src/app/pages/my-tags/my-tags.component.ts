import { NgForOf, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';
import { Tag } from '../../models/tag.model';
import { extractErrorMessage } from '../../utils/error-utils';
import { firstValueFrom } from 'rxjs';

@Component({
  standalone: true,
  selector: 'app-my-tags',
  imports: [NgIf, NgForOf, FormsModule],
  templateUrl: './my-tags.component.html',
  styleUrl: './my-tags.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyTagsComponent {
  private readonly userService = inject(UserService);
  private readonly authService = inject(AuthService);

  readonly tags = signal<Tag[]>([]);
  readonly selectedIds = signal<Set<number>>(new Set());
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly message = signal<string | null>(null);

  constructor() {
    this.load();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.message.set(null);
    try {
      const [tags, profile] = await Promise.all([
        firstValueFrom(this.userService.listTags()),
        firstValueFrom(this.userService.getProfile()),
      ]);
      this.tags.set(tags);
      this.selectedIds.set(new Set(profile.tags.map(tag => tag.id)));
    } catch (error) {
      this.message.set(extractErrorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  toggleSelection(tagId: number, checked: boolean): void {
    const next = new Set(this.selectedIds());
    if (checked) {
      next.add(tagId);
    } else {
      next.delete(tagId);
    }
    this.selectedIds.set(next);
  }

  isChecked(tagId: number): boolean {
    return this.selectedIds().has(tagId);
  }

  async submit(): Promise<void> {
    this.saving.set(true);
    this.message.set(null);
    try {
      const tagIds = Array.from(this.selectedIds());
      const response = await firstValueFrom(this.userService.saveTags(tagIds));
      this.message.set(response.message || '标签已保存。');
      await this.authService.refreshProfile().catch(() => undefined);
    } catch (error) {
      this.message.set(extractErrorMessage(error));
    } finally {
      this.saving.set(false);
    }
  }
}
