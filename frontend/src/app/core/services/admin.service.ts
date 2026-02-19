import { Injectable } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Observable } from "rxjs";
import { environment } from "../../../environments/environment";
import {
    AdminStats,
    ServiceHealth,
    UserAdmin,
    CreateUserRequest,
    ChangeRoleRequest,
    ScrapeLogEntry,
    ScrapeErrorEntry,
    StoreAdmin,
    UpdateStoreRequest,
    ScrapingResponse,
    StoreScrapingStatus,
    PageResponse
} from "../models/admin.model";

@Injectable({
    providedIn: "root"
})
export class AdminService
{
    private apiUrl = environment.apiUrl;

    constructor(private http: HttpClient) {}

    // --- Dashboard ---

    getStats(): Observable<AdminStats>
    {
        return this.http.get<AdminStats>(`${this.apiUrl}/admin/dashboard/stats`);
    }

    getServiceHealth(): Observable<ServiceHealth>
    {
        return this.http.get<ServiceHealth>(`${this.apiUrl}/admin/dashboard/health`);
    }

    // --- User Management ---

    getUsers(page: number, size: number, role?: string, search?: string): Observable<PageResponse<UserAdmin>>
    {
        let params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());

        if (role) {
            params = params.set('role', role);
        }
        if (search) {
            params = params.set('search', search);
        }

        return this.http.get<PageResponse<UserAdmin>>(`${this.apiUrl}/admin/users`, { params });
    }

    createUser(data: CreateUserRequest): Observable<UserAdmin>
    {
        return this.http.post<UserAdmin>(`${this.apiUrl}/admin/users`, data);
    }

    changeUserRole(userId: number, roleName: string): Observable<UserAdmin>
    {
        const body: ChangeRoleRequest = { roleName };
        return this.http.put<UserAdmin>(`${this.apiUrl}/admin/users/${userId}/role`, body);
    }

    deleteUser(userId: number): Observable<void>
    {
        return this.http.delete<void>(`${this.apiUrl}/admin/users/${userId}`);
    }

    // --- Scrape Logs ---

    getScrapeLogs(page: number, size: number, store?: string, status?: string): Observable<PageResponse<ScrapeLogEntry>>
    {
        let params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());

        if (store) {
            params = params.set('store', store);
        }
        if (status) {
            params = params.set('status', status);
        }

        return this.http.get<PageResponse<ScrapeLogEntry>>(`${this.apiUrl}/admin/scrape-logs`, { params });
    }

    getScrapeLog(logId: number): Observable<ScrapeLogEntry>
    {
        return this.http.get<ScrapeLogEntry>(`${this.apiUrl}/admin/scrape-logs/${logId}`);
    }

    getScrapeErrors(logId: number): Observable<ScrapeErrorEntry[]>
    {
        return this.http.get<ScrapeErrorEntry[]>(`${this.apiUrl}/admin/scrape-logs/${logId}/errors`);
    }

    // --- Scraping Control ---

    triggerScraping(storeSlug: string): Observable<ScrapingResponse>
    {
        return this.http.post<ScrapingResponse>(
            `${this.apiUrl}/admin/scraping/${storeSlug}/sync/all`, {});
    }

    cancelScraping(storeSlug: string): Observable<{ store: string; cancelled: boolean }>
    {
        return this.http.post<{ store: string; cancelled: boolean }>(
            `${this.apiUrl}/admin/scraping/${storeSlug}/cancel`, {});
    }

    getStoreScrapingStatus(storeSlug: string): Observable<StoreScrapingStatus>
    {
        return this.http.get<StoreScrapingStatus>(`${this.apiUrl}/admin/scraping/${storeSlug}/status`);
    }

    // --- CSV Export ---

    exportCsv(storeSlug: string): Observable<Blob>
    {
        return this.http.get(`${this.apiUrl}/admin/export/csv/${storeSlug}`, { responseType: 'blob' });
    }

    exportAllCsv(): Observable<Blob>
    {
        return this.http.get(`${this.apiUrl}/admin/export/csv/all`, { responseType: 'blob' });
    }

    // --- Store Management ---

    getStores(): Observable<StoreAdmin[]>
    {
        return this.http.get<StoreAdmin[]>(`${this.apiUrl}/admin/stores`);
    }

    updateStore(storeId: number, data: UpdateStoreRequest): Observable<StoreAdmin>
    {
        return this.http.put<StoreAdmin>(`${this.apiUrl}/admin/stores/${storeId}`, data);
    }
}
