import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { User } from '../../../core/models/user.model';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatInputModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.css']
})
export class SettingsComponent implements OnInit {

  settingsForm: FormGroup;
  hidePassword = true;
  hideNewPassword = true;
  isLoading = false;
  isSaving = false;
  errorMessage = '';
  currentUsername = '';
  currentUser: User | null = null;
  editUsername = false;
  editPassword = false;

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private authService: AuthService,
    public router: Router
  ) {
    this.settingsForm = this.fb.group({
      username: [{
        value: '',
        disabled: true
      }, [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(50),
        Validators.pattern(/^(?!.*@).+$/)
      ]],
      newPassword: [{
        value: '',
        disabled: true
      }, [Validators.minLength(6)]]
    });
  }

  ngOnInit(): void {
    this.loadCurrentUser();
  }

  loadCurrentUser(): void {
    this.isLoading = true;
    this.userService.getProfile()
      .pipe(finalize(() => this.isLoading = false))
      .subscribe({
        next: (user) => {
          this.currentUser = user;
          this.currentUsername = user.username;
          this.settingsForm.patchValue({
            username: user.username
          });
          this.settingsForm.get('username')?.disable();
          this.settingsForm.get('newPassword')?.disable();
        },
        error: (err) => {
          this.errorMessage = 'Error al cargar el perfil';
          console.error(err);
        }
      });
  }

  toggleUsernameEdit(): void {
    this.editUsername = !this.editUsername;
    const control = this.settingsForm.get('username');

    if (this.editUsername) {
      control?.enable();
      control?.setValue(this.currentUser?.username ?? '');
      control?.markAsTouched();
    } else {
      control?.disable();
      control?.setValue(this.currentUser?.username ?? '');
      control?.markAsUntouched();
    }
  }

  togglePasswordEdit(): void {
    this.editPassword = !this.editPassword;
    const control = this.settingsForm.get('newPassword');

    if (this.editPassword) {
      control?.setValidators([Validators.required, Validators.minLength(6)]);
      control?.enable();
      control?.setValue('');
      control?.markAsTouched();
    } else {
      control?.setValidators([Validators.minLength(6)]);
      control?.disable();
      control?.setValue('');
      control?.markAsUntouched();
    }

    control?.updateValueAndValidity();
  }

  onSubmit(): void {
    if (this.settingsForm.invalid) return;
    if (!this.editUsername && !this.editPassword) return;

    this.isSaving = true;
    this.errorMessage = '';

    const updateData: { username?: string; newPassword?: string } = {};
    
    const username = this.settingsForm.get('username')?.value;
    const newPassword = this.settingsForm.get('newPassword')?.value;

    if (this.editUsername && username) {
      updateData.username = username;
    }
    if (this.editPassword && newPassword) {
      updateData.newPassword = newPassword;
    }

    this.userService.updateProfile(updateData)
      .pipe(finalize(() => this.isSaving = false))
      .subscribe({
        next: (user) => {
          this.currentUser = user;
          this.currentUsername = user.username;
          this.settingsForm.patchValue({
            username: user.username,
            newPassword: ''
          });
          if (this.editUsername) {
            this.toggleUsernameEdit();
          }
          if (this.editPassword) {
            this.togglePasswordEdit();
          }
          this.authService.updateProfileData({
            username: user.username,
            email: user.email,
            role: user.role
          });
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Error al actualizar el perfil';
        }
      });
  }
}

