import { HttpErrorResponse, HttpInterceptorFn } from "@angular/common/http";
import { inject } from "@angular/core";
import { AuthService } from "../services/auth.service";
import { catchError, switchMap, throwError } from "rxjs";

export const authInterceptor: HttpInterceptorFn = (req, next) =>
{
    const authService = inject(AuthService);
    const accessToken = localStorage.getItem('access_token');

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
                if (error.status === 401 && !req.url.includes('/auth/refresh')) {
                    return authService.refreshToken().pipe(
                        switchMap(response => {
                            const retryReq = req.clone({
                                setHeaders: {
                                    Authorization: `Bearer ${response.access_token}`
                                }
                            });
                            return next(retryReq);
                        }),
                        catchError(refreshError => {
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
