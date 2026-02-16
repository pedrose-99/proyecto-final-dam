import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ShoppingListService } from '../../services/shopping-list.service';
import { ShoppingList } from '../../../core/models/shopping-list.model';

export interface AddToListDialogData
{
    productId: number;
    productName: string;
}

export interface AddToListDialogResult
{
    listId: number;
    isNew: boolean;
}

interface ListOption
{
    listId: number;
    name: string;
    groupName: string | null;
    itemCount: number;
}

@Component({
    selector: 'app-add-to-list-dialog',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        MatDialogModule,
        MatButtonModule,
        MatIconModule,
        MatListModule,
        MatFormFieldModule,
        MatInputModule,
        MatDividerModule,
        MatProgressSpinnerModule
    ],
    template: `
        <h2 mat-dialog-title>Añadir a lista</h2>
        <mat-dialog-content>
            <p class="product-name">{{ data.productName }}</p>

            @if (isLoading) {
                <div class="loading">
                    <mat-spinner diameter="32"></mat-spinner>
                </div>
            } @else {
                <!-- Crear nueva lista -->
                @if (showNewListForm) {
                    <div class="new-list-form">
                        <mat-form-field appearance="outline" class="full-width">
                            <mat-label>Nombre de la nueva lista</mat-label>
                            <input matInput [formControl]="newListNameControl"
                                   placeholder="Ej: Compra semanal"
                                   (keyup.enter)="createAndAdd()">
                        </mat-form-field>
                        <div class="form-actions">
                            <button mat-flat-button color="primary" (click)="createAndAdd()"
                                    [disabled]="!newListNameControl.value?.trim() || isCreating">
                                @if (isCreating) {
                                    <mat-spinner diameter="18"></mat-spinner>
                                } @else {
                                    Crear y añadir
                                }
                            </button>
                            <button mat-button (click)="showNewListForm = false">Cancelar</button>
                        </div>
                    </div>
                } @else {
                    <button mat-stroked-button class="full-width new-list-btn" (click)="showNewListForm = true">
                        <mat-icon>add</mat-icon>
                        Crear nueva lista
                    </button>
                }

                <mat-divider class="divider"></mat-divider>

                @if (listOptions.length === 0) {
                    <p class="empty-text">No tienes listas todavia. Crea una para empezar.</p>
                } @else {
                    <div class="lists-container">
                        @for (option of listOptions; track option.listId) {
                            <button mat-button class="list-option" (click)="selectList(option.listId)">
                                <div class="list-option-content">
                                    <mat-icon>{{ option.groupName ? 'groups' : 'list_alt' }}</mat-icon>
                                    <div class="list-option-info">
                                        <span class="list-option-name">{{ option.name }}</span>
                                        @if (option.groupName) {
                                            <span class="list-option-group">{{ option.groupName }}</span>
                                        }
                                    </div>
                                    <span class="list-option-count">{{ option.itemCount }} items</span>
                                </div>
                            </button>
                        }
                    </div>
                }
            }
        </mat-dialog-content>
        <mat-dialog-actions align="end">
            <button mat-button mat-dialog-close>Cancelar</button>
        </mat-dialog-actions>
    `,
    styles: [`
        .product-name {
            font-weight: 500;
            color: var(--smartcart-text);
            margin: 0 0 16px;
            font-size: 14px;
        }
        .loading {
            display: flex;
            justify-content: center;
            padding: 24px;
        }
        .full-width {
            width: 100%;
        }
        .new-list-btn {
            margin-bottom: 8px;
        }
        .new-list-form {
            margin-bottom: 8px;
        }
        .form-actions {
            display: flex;
            gap: 8px;
            margin-top: 4px;
        }
        .divider {
            margin: 12px 0;
        }
        .empty-text {
            color: var(--smartcart-text-light);
            text-align: center;
            padding: 16px 0;
            font-style: italic;
        }
        .lists-container {
            display: flex;
            flex-direction: column;
            max-height: 320px;
            overflow-y: auto;
        }
        .list-option {
            text-align: left;
            width: 100%;
            padding: 8px 12px;
            height: auto;
            line-height: normal;
        }
        .list-option-content {
            display: flex;
            align-items: center;
            gap: 12px;
            width: 100%;
        }
        .list-option-content > mat-icon {
            color: var(--smartcart-accent);
            flex-shrink: 0;
        }
        .list-option-info {
            flex: 1;
            display: flex;
            flex-direction: column;
            min-width: 0;
        }
        .list-option-name {
            font-weight: 500;
            color: var(--smartcart-text);
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }
        .list-option-group {
            font-size: 12px;
            color: var(--smartcart-accent);
        }
        .list-option-count {
            font-size: 12px;
            color: var(--smartcart-text-light);
            flex-shrink: 0;
        }
    `]
})
export class AddToListDialogComponent implements OnInit
{
    listOptions: ListOption[] = [];
    isLoading = true;
    showNewListForm = false;
    isCreating = false;
    newListNameControl = new FormControl('');

    constructor(
        @Inject(MAT_DIALOG_DATA) public data: AddToListDialogData,
        private dialogRef: MatDialogRef<AddToListDialogComponent>,
        private shoppingListService: ShoppingListService
    ) {}

    ngOnInit(): void
    {
        this.loadLists();
    }

    private loadLists(): void
    {
        this.isLoading = true;
        this.shoppingListService.getMyLists().subscribe({
            next: lists =>
            {
                this.listOptions = lists.map(list => ({
                    listId: list.listId,
                    name: list.name,
                    groupName: list.groupName || null,
                    itemCount: list.items?.length || 0
                }));
                this.isLoading = false;
            },
            error: () =>
            {
                this.isLoading = false;
            }
        });
    }

    selectList(listId: number): void
    {
        this.dialogRef.close({ listId, isNew: false } as AddToListDialogResult);
    }

    createAndAdd(): void
    {
        const name = this.newListNameControl.value?.trim();
        if (!name) return;

        this.isCreating = true;
        this.shoppingListService.createList(name).subscribe({
            next: newList =>
            {
                this.dialogRef.close({ listId: newList.listId, isNew: true } as AddToListDialogResult);
            },
            error: () =>
            {
                this.isCreating = false;
            }
        });
    }
}
