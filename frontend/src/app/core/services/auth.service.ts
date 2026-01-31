import { Injectable } from "@angular/core";
import { environment } from "../../../environments/environment";
import { BehaviorSubject, Observable, tap, catchError, throwError } from "rxjs";
import { AuthResponse, LoginRequest, RegisterRequest, RefreshTokenRequest } from "../models/user.model";
import { Router } from "@angular/router";
import { HttpClient, HttpErrorResponse } from "@angular/common/http";

@Injectable({
    providedIn: "root"
})
export class AuthService
{
    private apiUrl = environment.apiUrl;
    private currentUserSubject = new BehaviorSubject<AuthResponse | null>(null);
    public currentUser$ = this.currentUserSubject.asObservable();

    constructor(private http: HttpClient, private router: Router)
    {
        const storedUser = localStorage.getItem("currentUser");
        if (storedUser) {
            this.currentUserSubject.next(JSON.parse(storedUser));
        }
    }

    login(credentials: LoginRequest): Observable<AuthResponse>
    {
        return this.http.post<AuthResponse>(`${this.apiUrl}/auth/login`, credentials)
            .pipe(
                tap(response => {
                    this.saveTokens(response);
                }),
                catchError(this.handleError)
            );
    }

    register(data: RegisterRequest): Observable<AuthResponse>
    {
        return this.http.post<AuthResponse>(`${this.apiUrl}/auth/register`, data)
        .pipe(
            tap(response => {
                this.saveTokens(response);
            }),
            catchError(this.handleError)
        );
    }

    refreshToken(): Observable<AuthResponse>
    {
        const refreshToken = this.getRefreshToken();
        if (!refreshToken) {
            return throwError(() => ({ error: { message: 'No refresh token available' } }));
        }

        const request: RefreshTokenRequest = { refreshToken };

        return this.http.post<AuthResponse>(`${this.apiUrl}/auth/refresh`, request)
            .pipe(
                tap(response => {
                    this.saveTokens(response);
                }),
                catchError(error => {
                    this.logout();
                    return throwError(() => error);
                })
            );
    }

    private saveTokens(response: AuthResponse): void
    {
        localStorage.setItem('access_token', response.access_token);
        localStorage.setItem('refresh_token', response.refresh_token);
        localStorage.setItem('currentUser', JSON.stringify(response));
        this.currentUserSubject.next(response);
    }

    private handleError(error: HttpErrorResponse) {
        let errorMessage = 'Ha ocurrido un error';

        if (error.error instanceof ErrorEvent) {
            errorMessage = error.error.message;
        } else if (error.error?.message) {
            errorMessage = error.error.message;
        } else if (error.status === 401) {
            errorMessage = 'Email o contraseña incorrectos';
        } else if (error.status === 0) {
            errorMessage = 'No se pudo conectar con el servidor';
        }

        return throwError(() => ({ error: { message: errorMessage } }));
    }

    logout(): void
    {
        const accessToken = this.getAccessToken();

        // Llamar al backend para invalidar el token
        if (accessToken) {
            this.http.post(`${this.apiUrl}/auth/logout`, {}).subscribe({
                error: () => {} // Ignorar errores del logout
            });
        }

        localStorage.removeItem('access_token');
        localStorage.removeItem('refresh_token');
        localStorage.removeItem('currentUser');
        this.currentUserSubject.next(null);
        this.router.navigate(['/login']);
    }

    getAccessToken(): string | null
    {
        return localStorage.getItem('access_token');
    }

    getRefreshToken(): string | null
    {
        return localStorage.getItem('refresh_token');
    }

    // Mantener compatibilidad con código existente
    getToken(): string | null
    {
        return this.getAccessToken();
    }

    isAuthenticated(): boolean
    {
        return this.getAccessToken() !== null;
    }

    getCurrentUser(): AuthResponse | null
    {
        return this.currentUserSubject.value;
    }
}
