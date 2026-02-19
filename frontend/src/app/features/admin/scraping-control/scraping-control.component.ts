import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Subscription, firstValueFrom } from 'rxjs';
import { AdminService } from '../../../core/services/admin.service';
import { StoreAdmin, ScrapingResponse } from '../../../core/models/admin.model';
import { ScrapingStateService, StoreScrapingState } from '../../../core/services/scraping-state.service';

interface StoreCard extends StoreAdmin {
    result: ScrapingResponse | null;
}

@Component({
    selector: 'app-scraping-control',
    standalone: true,
    imports: [
        CommonModule,
        DatePipe,
        MatCardModule,
        MatButtonModule,
        MatIconModule,
        MatProgressSpinnerModule,
        MatSnackBarModule
    ],
    templateUrl: './scraping-control.component.html',
    styleUrls: ['./scraping-control.component.css']
})
export class ScrapingControlComponent implements OnInit, OnDestroy
{
    stores: StoreCard[] = [];
    stateMap = new Map<string, StoreScrapingState>();
    loading = true;
    scrapingAll = false;
    exportingAll = false;
    private cancelledAll = false;
    private stateSub: Subscription | null = null;

    constructor(
        private adminService: AdminService,
        private scrapingState: ScrapingStateService,
        private snackBar: MatSnackBar,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void
    {
        this.loadStores();
    }

    ngOnDestroy(): void
    {
        this.stateSub?.unsubscribe();
    }

    loadStores(): void
    {
        this.adminService.getStores().subscribe({
            next: (stores) => {
                this.stores = stores.map(s => ({ ...s, result: null }));
                this.loading = false;

                const slugs = stores.map(s => s.slug);
                this.scrapingState.initStores(slugs);
                this.scrapingState.refreshAllStatuses();

                if (!this.stateSub) {
                    this.stateSub = this.scrapingState.allStates$.subscribe(map => {
                        this.stateMap = map;
                        this.cdr.detectChanges();
                    });
                }

                this.cdr.detectChanges();
            },
            error: () => {
                this.loading = false;
                this.cdr.detectChanges();
                this.snackBar.open('Error al cargar tiendas', 'Cerrar', { duration: 3000 });
            }
        });
    }

    isScraping(store: StoreCard): boolean
    {
        return this.stateMap.get(store.slug)?.isRunning ?? false;
    }

    isAnyScraping(): boolean
    {
        return this.scrapingState.isAnyRunning();
    }

    triggerScraping(store: StoreCard): void
    {
        if (this.isScraping(store)) return;
        this.scrapingState.markRunning(store.slug);
        store.result = null;

        this.adminService.triggerScraping(store.slug).subscribe({
            next: (result) => {
                this.scrapingState.markFinished(store.slug, 'COMPLETED', new Date().toISOString());
                store.result = result;
                this.cdr.detectChanges();
                this.snackBar.open(
                    `${store.name}: ${result.created} creados, ${result.updated} actualizados, ${result.scrapingErrors + result.syncErrors} errores`,
                    'OK',
                    { duration: 5000 }
                );
                this.loadStores();
            },
            error: (err) => {
                if (err.status === 409) {
                    this.snackBar.open(
                        `${store.name}: El scraping ya esta en curso`,
                        'Cerrar',
                        { duration: 4000 }
                    );
                    this.scrapingState.refreshAllStatuses();
                } else {
                    this.scrapingState.markFinished(store.slug, 'FAILED', new Date().toISOString());
                    this.snackBar.open(
                        `Error al scrapear ${store.name}: ${err.error?.message || 'Error desconocido'}`,
                        'Cerrar',
                        { duration: 5000 }
                    );
                }
                this.cdr.detectChanges();
            }
        });
    }

    cancelScraping(store: StoreCard): void
    {
        this.scrapingState.markFinished(store.slug, 'CANCELLED', new Date().toISOString());
        this.cdr.detectChanges();

        this.adminService.cancelScraping(store.slug).subscribe({
            next: () => {
                this.snackBar.open(`Scraping de ${store.name} cancelado`, 'OK', { duration: 3000 });
                this.scrapingState.refreshAllStatuses();
                this.loadStores();
            },
            error: () => {
                this.snackBar.open(`Error al cancelar ${store.name}`, 'Cerrar', { duration: 3000 });
            }
        });
    }

    async triggerAll(): Promise<void>
    {
        this.scrapingAll = true;
        this.cancelledAll = false;
        const activeStores = this.stores.filter(s => s.active);

        for (const store of activeStores) {
            if (this.cancelledAll) break;
            try {
                this.scrapingState.markRunning(store.slug);
                this.cdr.detectChanges();
                const result = await firstValueFrom(this.adminService.triggerScraping(store.slug));
                store.result = result ?? null;
                this.scrapingState.markFinished(store.slug, 'COMPLETED', new Date().toISOString());
                this.cdr.detectChanges();
            } catch (err: any) {
                if (err.status === 409) {
                    this.scrapingState.refreshAllStatuses();
                } else {
                    this.scrapingState.markFinished(store.slug, 'FAILED', new Date().toISOString());
                }
                this.cdr.detectChanges();
            }
        }

        this.scrapingAll = false;
        if (!this.cancelledAll) {
            this.snackBar.open('Scraping completo para todas las tiendas', 'OK', { duration: 4000 });
        }
        this.cdr.detectChanges();
        this.loadStores();
    }

    cancelAll(): void
    {
        this.cancelledAll = true;
        const scrapingStores = this.stores.filter(s => this.isScraping(s));
        scrapingStores.forEach(store => this.cancelScraping(store));
        this.scrapingAll = false;
        this.cdr.detectChanges();
        this.snackBar.open('Todos los scrapings cancelados', 'OK', { duration: 3000 });
    }

    exportCsv(store: StoreCard): void
    {
        this.adminService.exportCsv(store.slug).subscribe({
            next: (blob) => {
                this.downloadBlob(blob, `products_${store.slug}.csv`);
                this.snackBar.open(`CSV de ${store.name} descargado`, 'OK', { duration: 3000 });
            },
            error: () => {
                this.snackBar.open(`Error al exportar CSV de ${store.name}`, 'Cerrar', { duration: 3000 });
            }
        });
    }

    exportAllCsv(): void
    {
        this.exportingAll = true;
        this.adminService.exportAllCsv().subscribe({
            next: (blob) => {
                this.exportingAll = false;
                this.downloadBlob(blob, 'products_all.zip');
                this.snackBar.open('ZIP con todos los CSVs descargado', 'OK', { duration: 3000 });
            },
            error: () => {
                this.exportingAll = false;
                this.snackBar.open('Error al exportar CSVs', 'Cerrar', { duration: 3000 });
            }
        });
    }

    private downloadBlob(blob: Blob, filename: string): void
    {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        a.click();
        window.URL.revokeObjectURL(url);
    }

    getStatusLabel(store: StoreCard): string
    {
        const state = this.stateMap.get(store.slug);
        if (state?.isRunning) return 'En curso';
        const status = state?.lastScrapeStatus ?? store.lastScrapeStatus;
        switch (status) {
            case 'COMPLETED': return 'Completado';
            case 'FAILED': return 'Error';
            case 'RUNNING': return 'En curso';
            case 'CANCELLED': return 'Cancelado';
            default: return 'Sin datos';
        }
    }

    getStatusClass(store: StoreCard): string
    {
        const state = this.stateMap.get(store.slug);
        if (state?.isRunning) return 'status-running';
        const status = state?.lastScrapeStatus ?? store.lastScrapeStatus;
        switch (status) {
            case 'COMPLETED': return 'status-completed';
            case 'FAILED': return 'status-failed';
            case 'RUNNING': return 'status-running';
            case 'CANCELLED': return 'status-cancelled';
            default: return 'status-none';
        }
    }

    getLastScrapeDate(store: StoreCard): string | null
    {
        return this.stateMap.get(store.slug)?.lastScrapeTime ?? store.lastScrapeDate;
    }
}
