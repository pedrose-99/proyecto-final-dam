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

import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, debounceTime, filter } from 'rxjs/operators';
import { ShoppingListService } from '../../shared/services/shopping-list.service';
import { ProductService } from '../../shared/services/product.service';
import { ExpenseService } from '../../shared/services/expense.service';
import { SimulatePurchaseDialogComponent, SimulatePurchaseDialogResult } from '../../shared/components/simulate-purchase-dialog/simulate-purchase-dialog.component';
import { ShoppingList, ListItem, OptimizedList, OptimizedStore, OptimizedItem, SublistInput } from '../../core/models/shopping-list.model';
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

  byStoreResult: OptimizedStore[] | null = null;
  isLoadingByStore = false;

  showAlternatives = false;
  alternativesStoreIdx = -1;
  alternativesItemIdx = -1;
  alternativesStoreId = 0;
  alternativesStoreName = '';
  alternativesSearchTerm = '';
  alternativesResults: ProductSearchResult[] = [];
  isLoadingAlternatives = false;
  alternativesMode: 'optimize' | 'byStore' = 'optimize';
  alternativesSearchControl = new FormControl<string>('');

  isEditingName = false;
  editNameControl = new FormControl<string>('');

  private destroy$ = new Subject<void>();

  constructor(
    private shoppingListService: ShoppingListService,
    private productService: ProductService,
    private expenseService: ExpenseService,
    private dialog: MatDialog,
    private router: Router
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
          if (!list.items) list.items = [];
          this.lists.unshift(list);
          this.selectedList = list;
          this.newListNameControl.reset();
        },
        error: () => {

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
          this.loadLists();
        },
        error: () => {

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
    this.byStoreResult = null;
  }

  isStoreSelected(storeId: number): boolean {
    return this.selectedStoreIds.has(storeId);
  }

  private getStoreIdsForOptimization(): number[] {
    if (this.selectedStoreIds.size > 0) {
      return Array.from(this.selectedStoreIds);
    }
    return this.stores.map(s => s.id);
  }

  optimize(): void {
    if (!this.selectedList) {
      return;
    }

    if (!this.selectedList.items || this.selectedList.items.length === 0) {
      return;
    }

    this.isOptimizing = true;
    this.byStoreResult = null;
    const storeIds = this.getStoreIdsForOptimization();

    this.shoppingListService.optimize(this.selectedList.listId, storeIds)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => {
          this.optimizedResult = result;
          this.editableResult = JSON.parse(JSON.stringify(result));
          this.isOptimizing = false;
        },
        error: () => {

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
          this.selectedList = null;
          this.optimizedResult = null;
          this.editableResult = null;
          this.selectedStoreIds.clear();
          this.loadLists();
        },
        error: () => {
          this.isCreatingSublists = false;

        }
      });
  }

  simulatePurchase(): void {
    if (!this.selectedList) return;

    const dialogRef = this.dialog.open(SimulatePurchaseDialogComponent, {
      width: '400px',
      data: { listId: this.selectedList.listId, listName: this.selectedList.name }
    });

    dialogRef.afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((result: SimulatePurchaseDialogResult | undefined) => {
        if (!result || !this.selectedList) return;

        this.expenseService.createBillFromList(this.selectedList.listId, result.billName)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: () => {
              this.router.navigate(['/mis-gastos']);
            },
            error: () => {

            }
          });
      });
  }

  acceptStoreList(storeIdx: number): void {
    if (!this.byStoreResult || !this.selectedList) return;
    const store = this.byStoreResult[storeIdx];
    if (!store || store.items.length === 0) return;

    this.isCreatingSublists = true;

    const sublists: SublistInput[] = [{
      storeName: store.storeName,
      items: store.items.map(item => ({
        productId: item.productId,
        quantity: item.quantity
      }))
    }];

    this.shoppingListService.createSublists(this.selectedList.name, sublists)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.isCreatingSublists = false;
          this.selectedList = null;
          this.optimizedResult = null;
          this.editableResult = null;
          this.byStoreResult = null;
          this.selectedStoreIds.clear();
          this.loadLists();
        },
        error: () => {
          this.isCreatingSublists = false;

        }
      });
  }

  optimizeByStore(): void {
    if (!this.selectedList) return;
    if (!this.selectedList.items || this.selectedList.items.length === 0) {
      return;
    }

    this.isLoadingByStore = true;
    this.byStoreResult = null;
    this.optimizedResult = null;
    this.editableResult = null;
    const storeIds = this.getStoreIdsForOptimization();

    this.shoppingListService.optimizeByStore(this.selectedList.listId, storeIds)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result) => {
          this.byStoreResult = JSON.parse(JSON.stringify(result));
          this.isLoadingByStore = false;
        },
        error: () => {

          this.isLoadingByStore = false;
        }
      });
  }

  getCheapestStoreId(): number | null {
    if (!this.byStoreResult || this.byStoreResult.length === 0) return null;
    let cheapest = this.byStoreResult[0];
    for (const store of this.byStoreResult) {
      if (store.items.length > 0 && store.subtotal < cheapest.subtotal) {
        cheapest = store;
      }
    }
    return cheapest.storeId;
  }

  openAlternatives(storeId: number, searchTerm: string, storeIdx: number, itemIdx: number, mode: 'optimize' | 'byStore'): void {
    this.showAlternatives = true;
    this.alternativesStoreIdx = storeIdx;
    this.alternativesItemIdx = itemIdx;
    this.alternativesStoreId = storeId;
    this.alternativesMode = mode;

    const store = mode === 'byStore'
      ? this.byStoreResult?.[storeIdx]
      : this.editableResult?.storeGroups[storeIdx];
    this.alternativesStoreName = store?.storeName || '';

    this.alternativesSearchTerm = searchTerm;
    this.alternativesSearchControl.setValue(searchTerm);

    this.searchAlternatives(searchTerm, storeId);
  }

  searchAlternatives(query?: string, storeId?: number): void {
    const term = (query || this.alternativesSearchControl.value || '').trim();
    if (term.length < 2) return;

    this.alternativesSearchTerm = term;
    this.isLoadingAlternatives = true;
    this.alternativesResults = [];

    this.productService.searchProductsByStore(term, storeId || this.alternativesStoreId, 15)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (results) => {
          this.alternativesResults = results;
          this.isLoadingAlternatives = false;
        },
        error: () => {
          this.alternativesResults = [];
          this.isLoadingAlternatives = false;
        }
      });
  }

  closeAlternatives(): void {
    this.showAlternatives = false;
    this.alternativesResults = [];
    this.alternativesSearchControl.setValue('');
  }

  swapProduct(newProduct: ProductSearchResult): void {
    if (this.alternativesMode === 'optimize' && this.editableResult) {
      const group = this.editableResult.storeGroups[this.alternativesStoreIdx];
      if (!group) return;
      const item = group.items[this.alternativesItemIdx];
      if (!item) return;

      item.productId = newProduct.id;
      item.productName = newProduct.name;
      item.imageUrl = newProduct.imageUrl;
      if (newProduct.currentPrice != null) {
        item.unitPrice = newProduct.currentPrice;
        item.lineTotal = newProduct.currentPrice * item.quantity;
      }

      group.subtotal = group.items.reduce((sum, i) => sum + i.lineTotal, 0);
      this.editableResult.totalCost = this.editableResult.storeGroups
        .reduce((sum, g) => sum + g.subtotal, 0);
    } else if (this.alternativesMode === 'byStore' && this.byStoreResult) {
      const store = this.byStoreResult[this.alternativesStoreIdx];
      if (!store) return;
      const item = store.items[this.alternativesItemIdx];
      if (!item) return;

      item.productId = newProduct.id;
      item.productName = newProduct.name;
      item.imageUrl = newProduct.imageUrl;
      if (newProduct.currentPrice != null) {
        item.unitPrice = newProduct.currentPrice;
        item.lineTotal = newProduct.currentPrice * item.quantity;
      }

      store.subtotal = store.items.reduce((sum, i) => sum + i.lineTotal, 0);
    }

    this.closeAlternatives();

  }

  getStoreLogo(logoUrl: string | undefined | null, storeName: string): string {
    const slug = storeName?.toLowerCase().replace(/\s+/g, '');
    const localLogos: Record<string, string> = {
      'mercadona': '/assets/images/stores/mercadona.svg',
      'dia': '/assets/images/stores/dia.svg',
      'carrefour': '/assets/images/stores/carrefour.svg',
      'alcampo': '/assets/images/stores/alcampo.svg',
      'ahorramas': '/assets/images/stores/ahorramas.svg',
    };
    return localLogos[slug] || logoUrl || '/assets/images/stores/placeholder.svg';
  }

  displayProduct(product: ProductSearchResult): string {
    return product ? product.name : '';
  }

  hasGenericItems(): boolean {
    return this.selectedList?.items?.some(i => i.isGeneric) ?? false;
  }

  getStoreFromListName(name: string): string | null {
    const knownStores = ['mercadona', 'dia', 'carrefour', 'alcampo', 'ahorramas'];
    const lower = name?.toLowerCase() || '';
    for (const store of knownStores) {
      if (lower.startsWith(store + ' ')) {
        return store;
      }
    }
    return null;
  }

  startEditName(): void {
    if (!this.selectedList) return;
    this.editNameControl.setValue(this.selectedList.name);
    this.isEditingName = true;
  }

  saveListName(): void {
    const newName = (this.editNameControl.value || '').trim();
    if (!newName || !this.selectedList) {
      this.cancelEditName();
      return;
    }

    this.shoppingListService.renameList(this.selectedList.listId, newName)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updatedList) => {
          this.selectedList!.name = updatedList.name;
          const idx = this.lists.findIndex(l => l.listId === updatedList.listId);
          if (idx >= 0) {
            this.lists[idx].name = updatedList.name;
          }
          this.isEditingName = false;

        },
        error: () => {

        }
      });
  }

  cancelEditName(): void {
    this.isEditingName = false;
  }

  goToExpenses(): void {
    this.router.navigate(['/mis-gastos']);
  }

  backToLists(): void {
    this.selectedList = null;
    this.optimizedResult = null;
    this.editableResult = null;
    this.byStoreResult = null;
    this.selectedStoreIds.clear();
    this.searchControl.reset();
    this.searchResults = [];
    this.closeAlternatives();
  }
}
