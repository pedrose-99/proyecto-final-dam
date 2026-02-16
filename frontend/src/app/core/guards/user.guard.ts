import { CanActivateFn, Router } from "@angular/router";
import { inject } from "@angular/core";
import { AuthService } from "../services/auth.service";

export const userGuard: CanActivateFn = (route, state) =>
{
    const authService = inject(AuthService);
    const router = inject(Router);

    const user = authService.getCurrentUser();

    if (user?.role === 'ADMIN')
    {
        router.navigate(['/admin/dashboard']);
        return false;
    }

    return true;
};
