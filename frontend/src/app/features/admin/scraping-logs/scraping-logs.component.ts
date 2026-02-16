import { Component, OnInit, ViewChild, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatExpansionModule } from '@angular/material/expansion';
import { AdminService } from '../../../core/services/admin.service';
import { ScrapeLogEntry, ScrapeErrorEntry } from '../../../core/models/admin.model';

@Component({
    selector: 'app-scraping-logs',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        MatCardModule,
        MatTableModule,
        MatPaginatorModule,
        MatSelectModule,
        MatFormFieldModule,
        MatIconModule,
        MatButtonModule,
        MatProgressSpinnerModule,
        MatExpansionModule
    ],
    templateUrl: './scraping-logs.component.html',
    styleUrls: ['./scraping-logs.component.css']
})
export class ScrapingLogsComponent implements OnInit
{
    @ViewChild(MatPaginator) paginator!: MatPaginator;

    logs: ScrapeLogEntry[] = [];
    totalElements = 0;
    pageSize = 10;
    pageIndex = 0;
    loading = true;

    storeFilter = '';
    statusFilter = '';

    displayedColumns = ['startTime', 'storeName', 'durationSeconds', 'productsFound', 'productsCreated', 'productsUpdated', 'errorCount', 'status'];

    expandedLog: ScrapeLogEntry | null = null;
    errors: ScrapeErrorEntry[] = [];
    errorsLoading = false;

    storeOptions = [
        { value: '', label: 'Todas' },
        { value: 'mercadona', label: 'Mercadona' },
        { value: 'alcampo', label: 'Alcampo' },
        { value: 'dia', label: 'Dia' },
        { value: 'carrefour', label: 'Carrefour' }
    ];

    statusOptions = [
        { value: '', label: 'Todos' },
        { value: 'COMPLETED', label: 'Completado' },
        { value: 'FAILED', label: 'Error' },
        { value: 'RUNNING', label: 'En curso' }
    ];

    constructor(private adminService: AdminService, private cdr: ChangeDetectorRef) {}

    ngOnInit(): void
    {
        this.loadLogs();
    }

    loadLogs(): void
    {
        this.loading = true;
        this.adminService.getScrapeLogs(
            this.pageIndex,
            this.pageSize,
            this.storeFilter || undefined,
            this.statusFilter || undefined
        ).subscribe({
            next: (page) => {
                this.logs = page.content;
                this.totalElements = page.totalElements;
                this.loading = false;
                this.cdr.detectChanges();
            },
            error: () => { this.loading = false; this.cdr.detectChanges(); }
        });
    }

    onPageChange(event: PageEvent): void
    {
        this.pageIndex = event.pageIndex;
        this.pageSize = event.pageSize;
        this.loadLogs();
    }

    onFilterChange(): void
    {
        this.pageIndex = 0;
        if (this.paginator) {
            this.paginator.firstPage();
        }
        this.loadLogs();
    }

    toggleErrors(log: ScrapeLogEntry): void
    {
        if (this.expandedLog?.id === log.id) {
            this.expandedLog = null;
            this.errors = [];
            return;
        }

        this.expandedLog = log;
        this.errorsLoading = true;
        this.adminService.getScrapeErrors(log.id).subscribe({
            next: (errors) => {
                this.errors = errors;
                this.errorsLoading = false;
                this.cdr.detectChanges();
            },
            error: () => {
                this.errors = [];
                this.errorsLoading = false;
                this.cdr.detectChanges();
            }
        });
    }

    getStatusColor(status: string): string
    {
        switch (status) {
            case 'COMPLETED': return 'completed';
            case 'FAILED': return 'failed';
            case 'RUNNING': return 'running';
            default: return '';
        }
    }

    getStatusLabel(status: string): string
    {
        switch (status) {
            case 'COMPLETED': return 'Completado';
            case 'FAILED': return 'Error';
            case 'RUNNING': return 'En curso';
            default: return status;
        }
    }

    formatDate(dateStr: string): string
    {
        if (!dateStr) return '-';
        const d = new Date(dateStr);
        return d.toLocaleDateString('es-ES', {
            day: '2-digit', month: '2-digit', year: '2-digit',
            hour: '2-digit', minute: '2-digit'
        });
    }

    formatDuration(seconds: number | null): string
    {
        if (seconds == null) return '-';
        if (seconds < 60) return `${seconds}s`;
        const m = Math.floor(seconds / 60);
        const s = seconds % 60;
        return `${m}m ${s}s`;
    }
}
