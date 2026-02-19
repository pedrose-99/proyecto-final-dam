import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';

import { ShoppingListService } from '../../../../shared/services/shopping-list.service';
import { forkJoin, of } from 'rxjs';

@Component({
  selector: 'app-select-shopping-list-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatCheckboxModule,
    MatIconModule,
  ],
  template: `
    <div class="dialog-container">
      <h2 mat-dialog-title>Selecciona listas de compra</h2>
      
      <mat-dialog-content>
        <div class="product-info">
          <p><strong>Producto:</strong> {{ data.productName }}</p>
        </div>

        <div class="lists-list">
          <div *ngFor="let list of validLists" 
               class="list-item"
               [class.selected]="isListSelected(list.listId)">
            <mat-checkbox 
              [checked]="isListSelected(list.listId)"
              (change)="toggleList(list)">
              <div class="list-info">
                <div class="list-name">{{ list.name }}</div>
                <div class="items-count">{{ list.items?.length || 0 }} artículos</div>
              </div>
            </mat-checkbox>
          </div>
        </div>

        <div class="no-lists" *ngIf="validLists.length === 0">
          <p>No tienes listas de compra. Por favor crea una primero.</p>
        </div>

        <div class="selected-count" *ngIf="selectedLists.length > 0">
          <p>{{ selectedLists.length }} lista(s) seleccionada(s)</p>
        </div>
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-button (click)="onCancel()">Cancelar</button>
        <button mat-raised-button color="primary" 
                [disabled]="selectedLists.length === 0 || isAdding"
                (click)="onConfirm()">
          <span *ngIf="!isAdding">Añadir a {{ selectedLists.length }} lista(s)</span>
          <span *ngIf="isAdding">Añadiendo...</span>
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .dialog-container {
      min-width: 350px;
      padding: 10px;
    }

    .product-info {
      background-color: #f5f5f5;
      padding: 12px;
      border-radius: 4px;
      margin-bottom: 16px;
    }

    .product-info p {
      margin: 0;
      font-size: 14px;
    }

    .lists-list {
      border: 1px solid #ddd;
      border-radius: 4px;
      max-height: 300px;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
    }

    .list-item {
      padding: 12px 16px;
      border-bottom: 1px solid #eee;
      transition: background-color 0.2s ease;
    }

    .list-item:hover {
      background-color: #f9f9f9;
    }

    .list-item.selected {
      background-color: #e3f2fd;
    }

    .list-info {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .list-name {
      font-weight: 500;
      font-size: 14px;
    }

    .items-count {
      font-size: 12px;
      color: #999;
    }

    .no-lists {
      text-align: center;
      padding: 20px;
      color: #999;
    }

    .selected-count {
      margin-top: 12px;
      padding: 8px 12px;
      background-color: #f5f5f5;
      border-radius: 4px;
      font-size: 13px;
      color: #666;
    }

    .selected-count p {
      margin: 0;
    }

    mat-dialog-actions {
      padding: 16px 0 0 0;
    }
  `]
})
export class SelectShoppingListDialogComponent {
  selectedLists: any[] = [];
  isAdding = false;
  validLists: any[] = [];

  constructor(
    public dialogRef: MatDialogRef<SelectShoppingListDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private shoppingListService: ShoppingListService
  ) {
    // Validar y filtrar listas al inicializar
    this.validLists = (data.lists || []).filter((list: any) => 
      list && list.listId && list.name && Array.isArray(list.items)
    );
  }

  isListSelected(listId: number): boolean {
    return this.selectedLists.some(list => list.listId === listId);
  }

  toggleList(list: any): void {
    const index = this.selectedLists.findIndex(l => l.listId === list.listId);
    if (index > -1) {
      this.selectedLists.splice(index, 1);
    } else {
      this.selectedLists.push(list);
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onConfirm(): void {
    if (this.selectedLists.length === 0) return;

    this.isAdding = true;

    // Refrescar las listas desde el servidor para tener datos actuales
    this.shoppingListService.getMyLists().subscribe({
      next: (freshLists: any[]) => {
        console.log('Listas frescas obtenidas:', freshLists);
        
        // Crear observables para cada lista USANDO LOS DATOS FRESCOS
        const operations = this.selectedLists.map(selectedList => {
          // Encontrar la versión fresca de la lista seleccionada
          const freshList = freshLists.find(l => l.listId === selectedList.listId);
          
          if (!freshList) {
            console.error(`Lista ${selectedList.listId} no encontrada en datos frescos`);
            return of(null);
          }

          // Buscar si el producto ya está en la lista fresca (convertir a número para comparar)
          const productId = Number(this.data.productId);
          const existingItem = freshList.items?.find((item: any) => {
            const itemProdId = Number(item.productId);
            console.log(`Comparando: ${itemProdId} === ${productId}?`, itemProdId === productId);
            return itemProdId === productId;
          });
          
          if (existingItem) {
            // Si ya existe, actualizar la cantidad (incrementar en 1)
            console.log(`✅ EXISTE: Actualizando ${existingItem.displayName} de ${existingItem.quantity} a ${existingItem.quantity + 1}`);
            return this.shoppingListService.updateItem(
              freshList.listId,
              existingItem.itemId,
              existingItem.quantity + 1
            );
          } else {
            // Si no existe, añadir nuevo
            console.log(`❌ NO EXISTE: Añadiendo nuevo producto a lista ${freshList.name}`);
            return this.shoppingListService.addItem(
              freshList.listId,
              productId,
              null,
              1
            );
          }
        });

        // Ejecutar todas las operaciones en paralelo
        forkJoin(operations).subscribe({
          next: () => {
            this.dialogRef.close({
              success: true, 
              addedCount: this.selectedLists.length 
            });
          },
          error: (err) => {
            console.error('Error al añadir producto a listas:', err);
            this.isAdding = false;
          }
        });
      },
      error: (err) => {
        console.error('Error al refrescar listas:', err);
        this.isAdding = false;
      }
    });
  }
}
