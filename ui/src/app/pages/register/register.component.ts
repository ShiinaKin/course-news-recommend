import { NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { extractErrorMessage } from '../../utils/error-utils';

@Component({
  standalone: true,
  selector: 'app-register',
  imports: [ReactiveFormsModule, NgIf],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);

  readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(32)]],
    nickname: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(32)]],
    password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(64)]],
  });

  readonly message = signal<string | null>(null);
  readonly loading = signal(false);

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.message.set(null);
    const { username, nickname, password } = this.form.getRawValue();
    try {
      const message = await this.authService.register(username, password, nickname);
      this.message.set(message || '注册成功，请前往登录。');
    } catch (error) {
      this.message.set(extractErrorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }
}
