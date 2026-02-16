import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AdminService } from '../../../core/services/admin.service';

@Component({
    selector: 'app-create-user-dialog',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        MatDialogModule,
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        MatButtonModule,
        MatProgressSpinnerModule
    ],
    template: `
        <h2 mat-dialog-title>Crear Usuario</h2>
        <mat-dialog-content>
            <form [formGroup]="form" class="form-container">
                <mat-form-field appearance="outline" class="full-width">
                    <mat-label>Username</mat-label>
                    <input matInput formControlName="username" placeholder="Nombre de usuario">
                    @if (form.get('username')?.hasError('required') && form.get('username')?.touched) {
                        <mat-error>El username es obligatorio</mat-error>
                    }
                    @if (form.get('username')?.hasError('minlength')) {
                        <mat-error>Mínimo 3 caracteres</mat-error>
                    }
                </mat-form-field>

                <mat-form-field appearance="outline" class="full-width">
                    <mat-label>Email</mat-label>
                    <input matInput formControlName="email" type="email" placeholder="correo@ejemplo.com">
                    @if (form.get('email')?.hasError('required') && form.get('email')?.touched) {
                        <mat-error>El email es obligatorio</mat-error>
                    }
                    @if (form.get('email')?.hasError('email')) {
                        <mat-error>Email no válido</mat-error>
                    }
                </mat-form-field>

                <mat-form-field appearance="outline" class="full-width">
                    <mat-label>Contraseña</mat-label>
                    <input matInput formControlName="password" type="password" placeholder="Mínimo 6 caracteres">
                    @if (form.get('password')?.hasError('required') && form.get('password')?.touched) {
                        <mat-error>La contraseña es obligatoria</mat-error>
                    }
                    @if (form.get('password')?.hasError('minlength')) {
                        <mat-error>Mínimo 6 caracteres</mat-error>
                    }
                </mat-form-field>

                <mat-form-field appearance="outline" class="full-width">
                    <mat-label>Rol</mat-label>
                    <mat-select formControlName="roleName">
                        <mat-option value="USER">USER</mat-option>
                        <mat-option value="ADMIN">ADMIN</mat-option>
                    </mat-select>
                </mat-form-field>

                @if (errorMessage) {
                    <p class="error-text">{{ errorMessage }}</p>
                }
            </form>
        </mat-dialog-content>
        <mat-dialog-actions align="end">
            <button mat-button mat-dialog-close [disabled]="saving">Cancelar</button>
            <button mat-flat-button class="primary-btn"
                    (click)="submit()"
                    [disabled]="form.invalid || saving">
                @if (saving) {
                    <mat-spinner diameter="18" class="btn-spinner"></mat-spinner>
                }
                Crear
            </button>
        </mat-dialog-actions>
    `,
    styles: [`
        .form-container {
            display: flex;
            flex-direction: column;
            gap: 4px;
            padding-top: 8px;
            min-width: 380px;
        }
        .full-width {
            width: 100%;
        }
        .error-text {
            color: #ef4444;
            font-size: 13px;
            margin: 0;
        }
        .btn-spinner {
            display: inline-block;
            margin-right: 8px;
            vertical-align: middle;
        }
    `]
})
export class CreateUserDialogComponent
{
    form: FormGroup;
    saving = false;
    errorMessage = '';

    constructor(
        private fb: FormBuilder,
        private adminService: AdminService,
        private dialogRef: MatDialogRef<CreateUserDialogComponent>
    ) {
        this.form = this.fb.group({
            username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
            email: ['', [Validators.required, Validators.email]],
            password: ['', [Validators.required, Validators.minLength(6)]],
            roleName: ['USER', Validators.required]
        });
    }

    submit(): void
    {
        if (this.form.invalid || this.saving) return;

        this.saving = true;
        this.errorMessage = '';

        this.adminService.createUser(this.form.value).subscribe({
            next: () => {
                this.dialogRef.close(true);
            },
            error: (err) => {
                this.saving = false;
                this.errorMessage = err.error?.message || 'Error al crear el usuario';
            }
        });
    }
}
