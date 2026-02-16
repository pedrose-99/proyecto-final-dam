import { Component, Inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AdminService } from '../../../core/services/admin.service';
import { ScrapeLogEntry, ScrapeErrorEntry } from '../../../core/models/admin.model';

@Component({
    selector: 'app-log-errors-dialog',
    standalone: true,
    imports: [
        CommonModule,
        MatDialogModule,
        MatIconModule,
        MatButtonModule,
        MatProgressSpinnerModule
    ],
    template: `
        <h2 mat-dialog-title class="dialog-title">
            <mat-icon class="title-icon">error_outline</mat-icon>
            Errores — {{ data.log.storeName }}
        </h2>

        <mat-dialog-content class="dialog-content">
            <div class="log-summary">
                <span><strong>Fecha:</strong> {{ formatDate(data.log.startTime) }}</span>
                <span><strong>Estado:</strong>
                    <span class="status-badge" [class]="getStatusClass(data.log.status)">
                        {{ getStatusLabel(data.log.status) }}
                    </span>
                </span>
                <span><strong>Errores:</strong> {{ data.log.errorCount ?? 0 }}</span>
            </div>

            @if (data.log.errorMessage) {
                <div class="error-message-box">
                    <mat-icon>warning</mat-icon>
                    <span>{{ data.log.errorMessage }}</span>
                </div>
            }

            @if (loading) {
                <div class="loading">
                    <mat-spinner diameter="32"></mat-spinner>
                </div>
            } @else if (errors.length === 0) {
                <p class="no-data">No se encontraron detalles de errores</p>
            } @else {
                <div class="errors-list">
                    @for (error of errors; track error.id) {
                        <div class="error-item">
                            <div class="error-header">
                                <span class="error-type">{{ error.errorType }}</span>
                                <span class="error-time">{{ formatDate(error.occurredAt) }}</span>
                            </div>
                            <p class="error-msg">{{ error.errorMessage }}</p>
                            @if (error.failedUrl) {
                                <p class="error-url">{{ error.failedUrl }}</p>
                            }
                        </div>
                    }
                </div>
            }
        </mat-dialog-content>

        <mat-dialog-actions align="end">
            <button mat-button mat-dialog-close>Cerrar</button>
        </mat-dialog-actions>
    `,
    styles: [`
        .dialog-title {
            display: flex;
            align-items: center;
            gap: 8px;
            color: #991b1b;
            font-size: 18px;
        }
        .title-icon {
            color: #dc2626;
        }
        .dialog-content {
            min-width: 500px;
            max-height: 60vh;
        }
        .log-summary {
            display: flex;
            gap: 20px;
            flex-wrap: wrap;
            padding: 12px 16px;
            background: #f8fafc;
            border-radius: 8px;
            margin-bottom: 16px;
            font-size: 13px;
        }
        .status-badge {
            display: inline-block;
            padding: 2px 8px;
            border-radius: 10px;
            font-size: 11px;
            font-weight: 600;
        }
        .status-badge.completed { background: #dcfce7; color: #166534; }
        .status-badge.failed { background: #fee2e2; color: #991b1b; }
        .status-badge.running { background: #fef3c7; color: #92400e; }
        .error-message-box {
            display: flex;
            align-items: flex-start;
            gap: 8px;
            padding: 12px 16px;
            background: #fef2f2;
            border: 1px solid #fecaca;
            border-radius: 8px;
            margin-bottom: 16px;
            color: #991b1b;
            font-size: 13px;
        }
        .error-message-box mat-icon {
            font-size: 20px;
            width: 20px;
            height: 20px;
            flex-shrink: 0;
            margin-top: 1px;
        }
        .loading {
            display: flex;
            justify-content: center;
            padding: 24px;
        }
        .no-data {
            color: #64748b;
            font-style: italic;
            text-align: center;
            padding: 16px;
        }
        .errors-list {
            display: flex;
            flex-direction: column;
            gap: 8px;
        }
        .error-item {
            background: #fff;
            border: 1px solid #fecaca;
            border-radius: 8px;
            padding: 10px 14px;
        }
        .error-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 4px;
        }
        .error-type {
            font-weight: 600;
            font-size: 12px;
            color: #991b1b;
            background: #fee2e2;
            padding: 2px 8px;
            border-radius: 4px;
        }
        .error-time {
            font-size: 11px;
            color: #64748b;
        }
        .error-msg {
            margin: 4px 0 0;
            font-size: 13px;
            color: #1e293b;
            word-break: break-word;
        }
        .error-url {
            margin: 4px 0 0;
            font-size: 12px;
            color: #64748b;
            word-break: break-all;
        }
    `]
})
export class LogErrorsDialogComponent implements OnInit {
    errors: ScrapeErrorEntry[] = [];
    loading = true;

    constructor(
        @Inject(MAT_DIALOG_DATA) public data: { log: ScrapeLogEntry },
        private adminService: AdminService,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        this.adminService.getScrapeErrors(this.data.log.id).subscribe({
            next: (errors) => {
                this.errors = errors;
                this.loading = false;
                this.cdr.detectChanges();
            },
            error: () => {
                this.loading = false;
                this.cdr.detectChanges();
            }
        });
    }

    formatDate(dateStr: string): string {
        if (!dateStr) return '-';
        const d = new Date(dateStr);
        return d.toLocaleDateString('es-ES', {
            day: '2-digit', month: '2-digit', year: '2-digit',
            hour: '2-digit', minute: '2-digit'
        });
    }

    getStatusClass(status: string): string {
        switch (status) {
            case 'COMPLETED': return 'completed';
            case 'FAILED': return 'failed';
            case 'RUNNING': return 'running';
            default: return '';
        }
    }

    getStatusLabel(status: string): string {
        switch (status) {
            case 'COMPLETED': return 'Completado';
            case 'FAILED': return 'Error';
            case 'RUNNING': return 'En curso';
            default: return status;
        }
    }
}
