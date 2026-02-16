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
import { ShoppingList, ListItem, OptimizedList } from '../../core/models/shopping-list.model';
import { Store } from '../../core/models/store.model';
import { Product } from '../../core/models/product.model';

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
  stores: Store[] = [];
  selectedStoreIds: Set<number> = new Set();
  
  searchControl = new FormControl<string>('');
  searchResults: Product[] = [];
  
  newListNameControl = new FormControl<string>('');
  
  isLoading = false;
  isOptimizing = false;
  
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
          this.lists = lists;
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
      return;
    }

    this.shoppingListService.createList(name)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (list) => {
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
          this.lists = this.lists.filter(l => l.listId !== list.listId);
          if (this.selectedList?.listId === list.listId) {
            this.selectedList = null;
          }
          this.snackBar.open('Lista eliminada', 'Cerrar', { duration: 3000 });
        },
        error: () => {
          this.snackBar.open('Error al eliminar la lista', 'Cerrar', { duration: 3000 });
        }
      });
  }

  onProductSelected(event: any): void {
    const product = event.option.value as Product;
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
          this.isOptimizing = false;
        },
        error: () => {
          this.snackBar.open('Error al optimizar la compra', 'Cerrar', { duration: 3000 });
          this.isOptimizing = false;
        }
      });
  }

  displayProduct(product: Product): string {
    return product ? product.name : '';
  }

  backToLists(): void {
    this.selectedList = null;
    this.optimizedResult = null;
    this.selectedStoreIds.clear();
    this.searchControl.reset();
    this.searchResults = [];
  }
}
