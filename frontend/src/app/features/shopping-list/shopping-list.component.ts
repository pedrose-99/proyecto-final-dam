import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject } from 'rxjs';
import { takeUntil, debounceTime, filter } from 'rxjs/operators';
import { ShoppingListService } from '../../shared/services/shopping-list.service';
import { ProductService } from '../../shared/services/product.service';
import { ShoppingList, ListItem, OptimizedList, OptimizedStore, SublistInput } from '../../core/models/shopping-list.model';
import { Store } from '../../core/models/store.model';
import { ProductSearchResult } from '../../core/models/product.model';

@Component({
  selector: 'app-shopping-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatCheckboxModule,
    MatChipsModule,
    MatAutocompleteModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatDialogModule,
    MatTooltipModule
  ],
  templateUrl: './shopping-list.component.html',
  styleUrls: ['./shopping-list.component.css']
})
export class ShoppingListComponent implements OnInit, OnDestroy {
  lists: ShoppingList[] = [];
  selectedList: ShoppingList | null = null;
  optimizedResult: OptimizedList | null = null;
  editableResult: OptimizedList | null = null;
  stores: Store[] = [];
  selectedStoreIds: Set<number> = new Set();

  searchControl = new FormControl<string>('');
  searchResults: ProductSearchResult[] = [];

  newListNameControl = new FormControl<string>('');

  isLoading = false;
  isOptimizing = false;
  isCreatingSublists = false;
  
  private destroy$ = new Subject<void>();

  constructor(
    private shoppingListService: ShoppingListService,
    private productService: ProductService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadLists();
    this.loadStores();
    this.setupProductSearch();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadLists(): void {
    this.isLoading = true;
    this.shoppingListService.getMyLists()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (lists) => {
          this.lists = (lists || []).map(l => ({ ...l, items: l.items || [] }));
          this.isLoading = false;
        },
        error: () => {
          this.snackBar.open('Error al cargar las listas', 'Cerrar', { duration: 3000 });
          this.isLoading = false;
        }
      });
  }

  loadStores(): void {
    this.productService.getStores()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (stores) => {
          this.stores = stores.filter(s => s.productCount && s.productCount > 0);
        },
        error: () => {
          this.snackBar.open('Error al cargar las tiendas', 'Cerrar', { duration: 3000 });
        }
      });
  }

  setupProductSearch(): void {
    this.searchControl.valueChanges
      .pipe(
        debounceTime(300),
        takeUntil(this.destroy$)
      )
      .subscribe(query => {
        if (!query || query.length < 2) {
          this.searchResults = [];
          return;
        }
        
        this.productService.searchProducts(query, 8)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: (results) => {
              this.searchResults = results;
            },
            error: () => {
              this.searchResults = [];
            }
          });
      });
  }

  createList(): void {
    const name = (this.newListNameControl.value || '').trim();
    if (!name) {
      this.snackBar.open('Escribe un nombre para la lista', 'Cerrar', { duration: 3000 });
      return;
    }

    this.shoppingListService.createList(name)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (list) => {
          if (!list.items) list.items = [];
          this.lists.unshift(list);
          this.selectedList = list;
          this.newListNameControl.reset();
          this.snackBar.open('Lista creada', 'Cerrar', { duration: 3000 });
        },
        error: () => {
          this.snackBar.open('Error al crear la lista', 'Cerrar', { duration: 3000 });
        }
      });
  }

  selectList(list: ShoppingList): void {
    this.optimizedResult = null;
    this.editableResult = null;
    this.shoppingListService.getListById(list.listId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (fullList) => {
          this.selectedList = fullList;
        },
        error: () => {
          this.snackBar.open('Error al cargar la lista', 'Cerrar', { duration: 3000 });
        }
      });
  }

  deleteList(list: ShoppingList): void {
    this.shoppingListService.deleteList(list.listId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          if (this.selectedList?.listId === list.listId) {
            this.selectedList = null;
          }
          // La lista se actualizará automáticamente con refetchQueries
          this.loadLists();
        },
        error: () => {
          this.snackBar.open('Error al eliminar la lista', 'Cerrar', { duration: 3000 });
        }
      });
  }

  onProductSelected(event: any): void {
    const product = event.option.value as ProductSearchResult;
    if (!this.selectedList) return;

    this.shoppingListService.addItem(this.selectedList.listId, product.id, null, 1)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updatedList) => {
          this.selectedList = updatedList;
          this.searchControl.reset();
          this.searchResults = [];
        },
        error: () => {
          this.snackBar.open('Error al añadir el producto', 'Cerrar', { duration: 3000 });
        }
      });
  }

  addGenericItem(): void {
    const text = (this.searchControl.value || '').trim();
    if (!text || !this.selectedList) {
      return;
    }

    this.shoppingListService.addItem(this.selectedList.listId, null, text, 1)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updatedList) => {
          this.selectedList = updatedList;
          this.searchControl.reset();
          this.searchResults = [];
        },
        error: () => {
          this.snackBar.open('Error al añadir el producto', 'Cerrar', { duration: 3000 });
        }
      });
  }

  changeQuantity(item: ListItem, delta: number): void {
    if (!this.selectedList) return;
    
    const newQuantity = Math.max(1, item.quantity + delta);
    this.shoppingListService.updateItem(this.selectedList.listId, item.itemId, newQuantity)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updatedList) => {
          this.selectedList = updatedList;
        },
        error: () => {
          this.snackBar.open('Error al actualizar la cantidad', 'Cerrar', { duration: 3000 });
        }
      });
  }

  toggleChecked(item: ListItem): void {
    if (!this.selectedList) return;

    this.shoppingListService.updateItem(this.selectedList.listId, item.itemId, undefined, !item.checked)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updatedList) => {
          this.selectedList = updatedList;
        },
        error: () => {
          this.snackBar.open('Error al actualizar el estado', 'Cerrar', { duration: 3000 });
        }
      });
  }

  removeItem(item: ListItem): void {
    if (!this.selectedList) return;

    this.shoppingListService.removeItem(this.selectedList.listId, item.itemId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updatedList) => {
          this.selectedList = updatedList;
        },
        error: () => {
          this.snackBar.open('Error al eliminar el producto', 'Cerrar', { duration: 3000 });
        }
      });
  }

  toggleStore(storeId: number): void {
    if (this.selectedStoreIds.has(storeId)) {
      this.selectedStoreIds.delete(storeId);
    } else {
      this.selectedStoreIds.add(storeId);
    }
    this.optimizedResult = null;
    this.editableResult = null;
  }

  isStoreSelected(storeId: number): boolean {
    return this.selectedStoreIds.has(storeId);
  }

  optimize(): void {
    if (!this.selectedList) {
      this.snackBar.open('Selecciona una lista', 'Cerrar', { duration: 3000 });
      return;
    }

    if (this.selectedStoreIds.size === 0) {
      this.snackBar.open('Selecciona al menos una tienda', 'Cerrar', { duration: 3000 });
      return;
    }

    if (!this.selectedList.items || this.selectedList.items.length === 0) {
      this.snackBar.open('La lista está vacía', 'Cerrar', { duration: 3000 });
      return;
    }

    this.isOptimizing = true;
    const storeIds = Array.from(this.selectedStoreIds);
    
    this.shoppingListService.optimize(this.selectedList.listId, storeIds)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => {
          this.optimizedResult = result;
          this.editableResult = JSON.parse(JSON.stringify(result));
          this.isOptimizing = false;
        },
        error: () => {
          this.snackBar.open('Error al optimizar la compra', 'Cerrar', { duration: 3000 });
          this.isOptimizing = false;
        }
      });
  }

  removeOptimizedItem(storeIdx: number, itemIdx: number): void {
    if (!this.editableResult) return;

    const group = this.editableResult.storeGroups[storeIdx];
    group.items.splice(itemIdx, 1);

    if (group.items.length === 0) {
      this.editableResult.storeGroups.splice(storeIdx, 1);
    } else {
      group.subtotal = group.items.reduce((sum, i) => sum + i.lineTotal, 0);
    }

    this.editableResult.totalCost = this.editableResult.storeGroups
      .reduce((sum, g) => sum + g.subtotal, 0);
  }

  changeOptimizedQuantity(storeIdx: number, itemIdx: number, delta: number): void {
    if (!this.editableResult) return;

    const item = this.editableResult.storeGroups[storeIdx].items[itemIdx];
    item.quantity = Math.max(1, item.quantity + delta);
    item.lineTotal = item.unitPrice * item.quantity;

    const group = this.editableResult.storeGroups[storeIdx];
    group.subtotal = group.items.reduce((sum, i) => sum + i.lineTotal, 0);

    this.editableResult.totalCost = this.editableResult.storeGroups
      .reduce((sum, g) => sum + g.subtotal, 0);
  }

  acceptOptimization(): void {
    if (!this.editableResult || !this.selectedList) return;
    if (this.editableResult.storeGroups.length === 0) return;

    this.isCreatingSublists = true;

    const sublists: SublistInput[] = this.editableResult.storeGroups.map(group => ({
      storeName: group.storeName,
      items: group.items.map(item => ({
        productId: item.productId,
        quantity: item.quantity
      }))
    }));

    this.shoppingListService.createSublists(this.selectedList.name, sublists)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (newLists) => {
          this.isCreatingSublists = false;
          this.snackBar.open(
            `Se crearon ${newLists.length} sublista${newLists.length !== 1 ? 's' : ''}`,
            'Cerrar',
            { duration: 3000 }
          );
          this.selectedList = null;
          this.optimizedResult = null;
          this.editableResult = null;
          this.selectedStoreIds.clear();
          this.loadLists();
        },
        error: () => {
          this.isCreatingSublists = false;
          this.snackBar.open('Error al crear las sublistas', 'Cerrar', { duration: 3000 });
        }
      });
  }

  displayProduct(product: ProductSearchResult): string {
    return product ? product.name : '';
  }

  backToLists(): void {
    this.selectedList = null;
    this.optimizedResult = null;
    this.editableResult = null;
    this.selectedStoreIds.clear();
    this.searchControl.reset();
    this.searchResults = [];
  }
}
