import { CommonModule } from "@angular/common";
import { Component } from "@angular/core";
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { Router, RouterLink } from "@angular/router";
import { AuthService } from "../../../core/services/auth.service";
import { finalize } from "rxjs";

@Component({
    selector: 'app-register',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        RouterLink,
        MatCardModule,
        MatInputModule,
        MatButtonModule,
        MatFormFieldModule,
        MatIconModule,
        MatProgressSpinnerModule
        ],
    templateUrl: './register.component.html',
    styleUrls: ['./register.component.css']
})

export class RegisterComponent
{
	registerForm: FormGroup;
	hidePassword = true;
	hideConfirmPassword = true;
	isLoading = false;
	errorMessage = '';

	constructor(
		private fb: FormBuilder,
		private authService: AuthService,
		private router: Router
	) {
		this.registerForm = this.fb.group({
			username: ['', [Validators.required, Validators.minLength(3)]],
			email: ['', [Validators.required, Validators.email]],
			password: ['', [Validators.required, Validators.minLength(6)]],
			confirmPassword: ['', [Validators.required]]
		}, { validators: this.passwordMatchValidator });
	}

	passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
		const password = control.get('password');
		const confirmPassword = control.get('confirmPassword');

		if (password && confirmPassword && password.value !== confirmPassword.value) {
			confirmPassword.setErrors({ passwordMismatch: true });
			return { passwordMismatch: true };
		}
		return null;
	}

	onSubmit(): void
	{
		if (this.registerForm.invalid) return;

		this.isLoading = true;
		this.errorMessage = '';

		const { confirmPassword, ...registerData } = this.registerForm.value;

		this.authService.register(registerData)
			.pipe(finalize(() => this.isLoading = false))
			.subscribe({
				next: () => {
					this.router.navigate(['/home']);
				},
				error: (err) => {
					this.errorMessage = err.error?.message || 'Error al registrar el usuario';
				}
			});
	}
}

