// --- Dashboard ---

export interface AdminStats {
    totalProducts: number;
    totalUsers: number;
    totalStores: number;
    totalCategories: number;
    productsByStore: StoreProductCount[];
    usersByRole: Record<string, number>;
    recentScrapeLogs: ScrapeLogEntry[];
}

export interface ServiceHealth {
    backend: string;
    database: string;
    pythonScraper: string;
}

export interface StoreProductCount {
    storeName: string;
    storeSlug: string;
    count: number;
}

// --- Scrape Logs ---

export interface ScrapeLogEntry {
    id: number;
    storeName: string;
    storeSlug: string;
    startTime: string;
    endTime: string | null;
    productsFound: number | null;
    productsCreated: number | null;
    productsUpdated: number | null;
    productsUnchanged: number | null;
    errorCount: number | null;
    status: 'RUNNING' | 'COMPLETED' | 'FAILED';
    errorMessage: string | null;
    durationSeconds: number | null;
}

export interface ScrapeErrorEntry {
    id: number;
    errorType: string;
    errorMessage: string;
    failedUrl: string | null;
    occurredAt: string;
}

// --- Users ---

export interface UserAdmin {
    id: number;
    username: string;
    email: string;
    roleName: string;
    createdAt: string;
}

export interface CreateUserRequest {
    username: string;
    email: string;
    password: string;
    roleName: string;
}

export interface ChangeRoleRequest {
    roleName: string;
}

// --- Stores ---

export interface StoreAdmin {
    id: number;
    name: string;
    slug: string;
    logo: string | null;
    website: string | null;
    active: boolean;
    scrapingUrl: string | null;
    productCount: number;
    lastScrapeDate: string | null;
    lastScrapeStatus: string | null;
}

export interface UpdateStoreRequest {
    active?: boolean;
    scrapingUrl?: string;
}

// --- Scraping trigger response ---

export interface ScrapingResponse {
    scraped: number;
    scrapingErrors: number;
    created: number;
    updated: number;
    unchanged: number;
    syncErrors: number;
    durationSeconds: number;
}

// --- Paginación genérica ---

export interface PageResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
    first: boolean;
    last: boolean;
}
