import { HttpErrorResponse, HttpInterceptorFn } from "@angular/common/http";
import { inject } from "@angular/core";
import { AuthService } from "../services/auth.service";
import { catchError, switchMap, throwError } from "rxjs";

export const authInterceptor: HttpInterceptorFn = (req, next) =>
{
    const authService = inject(AuthService);
    const accessToken = localStorage.getItem('access_token');

    // No añadir token a las rutas de auth (excepto logout)
    if (req.url.includes('/auth/') && !req.url.includes('/auth/logout')) {
        return next(req);
    }

    if (accessToken) {
        const cloned = req.clone({
            setHeaders: {
                Authorization: `Bearer ${accessToken}`
            }
        });

        return next(cloned).pipe(
            catchError((error: HttpErrorResponse) => {
                // Si el token expiró (401), intentar refresh
                if (error.status === 401 && !req.url.includes('/auth/refresh')) {
                    return authService.refreshToken().pipe(
                        switchMap(response => {
                            // Reintentar la petición original con el nuevo token
                            const retryReq = req.clone({
                                setHeaders: {
                                    Authorization: `Bearer ${response.access_token}`
                                }
                            });
                            return next(retryReq);
                        }),
                        catchError(refreshError => {
                            // Si el refresh falla, hacer logout
                            authService.logout();
                            return throwError(() => refreshError);
                        })
                    );
                }
                return throwError(() => error);
            })
        );
    }

    return next(req);
};
