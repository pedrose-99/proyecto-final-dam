import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, takeUntil, debounceTime } from 'rxjs';
import { ProductService } from '../../shared/services/product.service';
import { ShoppingListService } from '../../shared/services/shopping-list.service';
import { ProductCardComponent } from '../../shared/components/product-card/product-card.component';
import { AddToListDialogComponent, AddToListDialogResult } from '../../shared/components/add-to-list-dialog/add-to-list-dialog.component';
import { Product, ProductFilters, ProductPage } from '../../core/models/product.model';
import { Category } from '../../core/models/category.model';
import { Store } from '../../core/models/store.model';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,

    MatAutocompleteModule,
    MatChipsModule,
    MatDialogModule,
    MatTooltipModule,
    ProductCardComponent
  ],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit, OnDestroy {

  products: Product[] = [];
  categories: Category[] = [];
  stores: Store[] = [];
  productsInList: Set<number> = new Set();

  totalProducts = 0;
  pageSize = 24;
  pageIndex = 0;
  pageSizeOptions = [12, 24, 48];

  isLoading = false;

  filters: ProductFilters = {};
  activeFilters: { key: string; label: string }[] = [];
  searchTerms: string[] = [];  // Array de términos de búsqueda para OR

  filterForm = new FormGroup({
    sortBy: new FormControl<string>('relevance')
  });

  categorySearchControl = new FormControl<string>('');
  filteredCategories: Category[] = [];
  selectedCategories: Category[] = [];

  storeSearchControl = new FormControl<string>('');
  filteredStores: Store[] = [];
  selectedStores: Store[] = [];

  private destroy$ = new Subject<void>();

  constructor(
    private productService: ProductService,
    private shoppingListService: ShoppingListService,
    private dialog: MatDialog,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.productService.getCategories().subscribe({
      next: (categories) => {
        this.categories = categories;
        this.filteredCategories = categories;
        this.setupCategorySearch();
        this.checkDataLoaded();
      },
      error: (err) => {
        console.error('Error al cargar categorías:', err);
        this.checkDataLoaded();
      }
    });

    this.productService.getStores().subscribe({
      next: (stores) => {
        this.stores = stores.filter(s => s.productCount && s.productCount > 0);
        this.filteredStores = this.stores;
        this.setupStoreSearch();
        this.checkDataLoaded();
      },
      error: (err) => {
        console.error('Error al cargar tiendas:', err);
        this.checkDataLoaded();
      }
    });

    this.setupFilterSubscription();
    this.syncProductsInLists();
  }

  private dataLoadedFlags = { categories: false, stores: false };

  private checkDataLoaded(): void {
    this.dataLoadedFlags.categories = this.categories.length > 0;
    this.dataLoadedFlags.stores = this.stores.length > 0;

    if (this.dataLoadedFlags.categories && this.dataLoadedFlags.stores) {
      this.handleRouteParams();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private parseIdArray(value: any): number[] {
    if (typeof value === 'string') {
      return value.split(',').map(s => {
        const num = Number(s.trim());
        return isNaN(num) ? 0 : num;
      }).filter(n => n > 0);
    } else if (Array.isArray(value)) {
      return value.flatMap(v => {
        if (typeof v === 'string') {
          return v.split(',').map(s => {
            const num = Number(s.trim());
            return isNaN(num) ? 0 : num;
          });
        }
        return isNaN(Number(v)) ? [] : [Number(v)];
      }).filter(n => n > 0);
    } else if (typeof value === 'number') {
      return [value];
    }
    return [];
  }

  private setupCategorySearch(): void {
    this.categorySearchControl.valueChanges.pipe(
      debounceTime(200),
      takeUntil(this.destroy$)
    ).subscribe(searchTerm => {
      this.filterCategories(searchTerm || '');
    });
  }

  private filterCategories(searchTerm: string): void {
    if (!searchTerm) {
      this.filteredCategories = this.categories.filter(
        c => !this.selectedCategories.some(sc => sc.id === c.id)
      );
    } else {
      const term = searchTerm.toLowerCase();
      this.filteredCategories = this.categories.filter(c =>
        c.name.toLowerCase().includes(term) &&
        !this.selectedCategories.some(sc => sc.id === c.id)
      );
    }
  }

  private setupStoreSearch(): void {
    this.storeSearchControl.valueChanges.pipe(
      debounceTime(200),
      takeUntil(this.destroy$)
    ).subscribe(searchTerm => {
      this.filterStores(searchTerm || '');
    });
  }

  private filterStores(searchTerm: string): void {
    if (!searchTerm) {
      this.filteredStores = this.stores.filter(
        s => !this.selectedStores.some(ss => ss.id === s.id)
      );
    } else {
      const term = searchTerm.toLowerCase();
      this.filteredStores = this.stores.filter(s =>
        s.name.toLowerCase().includes(term) &&
        !this.selectedStores.some(ss => ss.id === s.id)
      );
    }
  }

  private setupFilterSubscription(): void {
    this.filterForm.valueChanges.pipe(
      debounceTime(300),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.pageIndex = 0;
      this.applyFilters();
    });
  }

  private handleRouteParams(): void {
    this.route.queryParams.pipe(
      takeUntil(this.destroy$)
    ).subscribe(params => {
      const newSearch = params['search'];
      const newSearchTerms = newSearch 
        ? newSearch.split(',').map((t: string) => t.trim()).filter((t: string) => t)
        : [];
      
      const searchChanged = newSearch !== this.filters.search;
      
      if (newSearchTerms.length > 0) {
        this.searchTerms = newSearchTerms;
        this.filters.search = newSearchTerms[0];
      } else {
        this.searchTerms = [];
        this.filters.search = undefined;
      }
      
      if (searchChanged) {
        this.pageIndex = 0;
      }
      
      if (params['categoryIds']) {
        const categoryIds = this.parseIdArray(params['categoryIds']);
        this.selectedCategories = categoryIds.length > 0 
          ? this.categories.filter(category => categoryIds.includes(category.id))
          : [];
      } else {
        this.selectedCategories = [];
      }
      
      if (params['storeIds']) {
        const storeIds = this.parseIdArray(params['storeIds']);
        this.selectedStores = storeIds.length > 0 
          ? this.stores.filter(store => storeIds.includes(store.id))
          : [];
      } else {
        this.selectedStores = [];
      }
      
      this.loadProducts();
    });
  }

  loadProducts(): void {
    this.isLoading = true;
    this.buildFiltersFromForm();
    console.log('[DEBUG] loadProducts() llamado con filtros:', this.filters, 'página:', this.pageIndex);

    if (this.searchTerms.length > 1) {
      this.loadProductsWithMultipleSearchTerms();
    } else {
      this.loadProductsSingleSearch();
    }
  }

  private loadProductsSingleSearch(): void {
    this.productService.getProducts(this.filters, this.pageIndex, this.pageSize)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (page: ProductPage) => {
          console.log('[DEBUG] Productos recibidos:', page.content.length, 'total:', page.totalElements);
          this.products = page.content;
          this.totalProducts = page.totalElements;
          this.isLoading = false;
          this.updateActiveFilters();
          this.syncProductsInLists();
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error al cargar productos:', err);
          this.isLoading = false;
        }
      });
  }

  private loadProductsWithMultipleSearchTerms(): void {
    const allProducts: Product[] = [];
    const productIds = new Set<number>();
    let completedRequests = 0;

    this.searchTerms.forEach(term => {
      const filtersCopy = { ...this.filters, search: term };
      this.productService.getProducts(filtersCopy, 0, 1000) // Obtener más productos para combinar
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (page: ProductPage) => {
            page.content.forEach(product => {
              if (!productIds.has(product.id)) {
                productIds.add(product.id);
                allProducts.push(product);
              }
            });

            completedRequests++;
            if (completedRequests === this.searchTerms.length) {
              const startIndex = this.pageIndex * this.pageSize;
              const endIndex = startIndex + this.pageSize;
              this.products = allProducts.slice(startIndex, endIndex);
              this.totalProducts = allProducts.length;
              this.isLoading = false;
              this.updateActiveFilters();
              this.syncProductsInLists();
              this.cdr.detectChanges();
            }
          },
          error: (err) => {
            console.error('Error al cargar productos para término:', term, err);
            completedRequests++;
            if (completedRequests === this.searchTerms.length) {
              this.isLoading = false;
            }
          }
        });
    });
  }

  private syncProductsInLists(): void {
    this.shoppingListService.getMyLists().subscribe({
      next: (lists: any[]) => {
        this.productsInList.clear();

        lists.forEach(list => {
          if (list.items && Array.isArray(list.items)) {
            list.items.forEach((item: any) => {
              if (item.productId) {
                this.productsInList.add(Number(item.productId));
              }
            });
          }
        });
        
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error al sincronizar productos en listas:', err);
      }
    });
  }

  private buildFiltersFromForm(): void {
    const formValue = this.filterForm.value;
    this.filters.sortBy = formValue.sortBy as ProductFilters['sortBy'];

    if (this.selectedCategories.length > 0) {
      this.filters.categoryIds = this.getExpandedCategoryIds(this.selectedCategories);
      this.filters.categoryId = undefined;
      this.filters.categoryNames = this.selectedCategories.map(c => c.name);
    } else {
      this.filters.categoryIds = undefined;
      this.filters.categoryId = undefined;
      this.filters.categoryNames = undefined;
    }

    if (this.selectedStores.length > 0) {
      this.filters.storeIds = this.selectedStores.map(s => s.id);
    } else {
      this.filters.storeIds = undefined;
    }
  }

  private getExpandedCategoryIds(selected: Category[]): number[] {
    const selectedIds = selected.map(c => c.id);
    if (this.categories.length === 0) {
      return selectedIds;
    }

    const childrenByParent = new Map<number, number[]>();
    this.categories.forEach(category => {
      if (category.parentId !== null) {
        const parentId = category.parentId;
        const siblings = childrenByParent.get(parentId) || [];
        siblings.push(category.id);
        childrenByParent.set(parentId, siblings);
      }
    });

    const expanded = new Set<number>(selectedIds);
    const queue: number[] = [...selectedIds];

    while (queue.length > 0) {
      const currentId = queue.shift();
      if (currentId === undefined) {
        continue;
      }
      const children = childrenByParent.get(currentId) || [];
      children.forEach(childId => {
        if (!expanded.has(childId)) {
          expanded.add(childId);
          queue.push(childId);
        }
      });
    }

    return Array.from(expanded);
  }

  private updateActiveFilters(): void {
    this.activeFilters = [];

    if (this.searchTerms.length > 0) {
      this.searchTerms.forEach(term => {
        this.activeFilters.push({ key: `search-${term}`, label: `"${term}"` });
      });
    }
  }

  applyFilters(): void {
    this.loadProducts();
  }

  refreshProducts(): void {
    this.productService.clearCache();
    this.loadProducts();
  }

  onCategoryInputFocus(): void {
    this.filterCategories(this.categorySearchControl.value || '');
  }

  onCategorySelected(event: MatAutocompleteSelectedEvent): void {
    const category = event.option.value as Category;

    if (!this.selectedCategories.some(c => c.id === category.id)) {
      this.selectedCategories.push(category);
    }

    this.categorySearchControl.setValue('');
    this.filterCategories('');

    this.pageIndex = 0;
    this.updateQueryParams();
    this.loadProducts();
  }

  removeCategory(category: Category): void {
    this.selectedCategories = this.selectedCategories.filter(c => c.id !== category.id);
    this.filterCategories(this.categorySearchControl.value || '');
    this.pageIndex = 0;
    this.updateQueryParams();
    this.loadProducts();
  }

  onStoreInputFocus(): void {
    this.filterStores(this.storeSearchControl.value || '');
  }

  onStoreSelected(event: MatAutocompleteSelectedEvent): void {
    const store = event.option.value as Store;

    if (!this.selectedStores.some(s => s.id === store.id)) {
      this.selectedStores.push(store);
      
      this.storeSearchControl.setValue('');
      this.filterStores('');

      this.pageIndex = 0;
      this.updateQueryParams();
      this.loadProducts();
    }
  }

  removeStore(store: Store): void {
    this.selectedStores = this.selectedStores.filter(s => s.id !== store.id);
    this.filterStores(this.storeSearchControl.value || '');
    this.pageIndex = 0;
    this.updateQueryParams();
    this.loadProducts();
  }

  private updateQueryParams(): void {
    const queryParams: any = {};

    if (this.searchTerms.length > 0) {
      queryParams['search'] = this.searchTerms.join(',');
    } else {
      queryParams['search'] = null;
    }
    
    if (this.selectedStores.length > 0) {
      queryParams['storeIds'] = this.selectedStores.map(s => s.id).join(',');
    } else {
      queryParams['storeIds'] = null;
    }
    
    if (this.selectedCategories.length > 0) {
      queryParams['categoryIds'] = this.selectedCategories.map(c => c.id).join(',');
    } else {
      queryParams['categoryIds'] = null;
    }
    
    this.router.navigate([], { queryParams, queryParamsHandling: 'merge' });
  }

  removeFilter(filterKey: string): void {
    if (filterKey.startsWith('search-')) {
      const term = filterKey.substring(7); // "search-".length = 7
      
      this.searchTerms = this.searchTerms.filter(t => t !== term);
      
      if (this.searchTerms.length > 0) {
        const newSearchQuery = this.searchTerms.join(',');
        this.filters.search = this.searchTerms[0];
        this.router.navigate([], { queryParams: { search: newSearchQuery }, queryParamsHandling: 'merge' });
      } else {
        this.filters.search = undefined;
        this.router.navigate([], { queryParams: { search: null }, queryParamsHandling: 'merge' });
      }
    }
    this.loadProducts();
  }

  clearAllFilters(): void {
    this.filters = {};
    this.searchTerms = [];
    this.selectedCategories = [];
    this.selectedStores = [];
    this.categorySearchControl.setValue('');
    this.storeSearchControl.setValue('');
    this.filterCategories('');
    this.filterStores('');
    this.filterForm.reset({
      sortBy: 'relevance'
    });
    this.router.navigate([], { queryParams: {} });
    this.loadProducts();
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadProducts();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  onAddToList(product: Product): void {
    const dialogRef = this.dialog.open(AddToListDialogComponent, {
      width: '420px',
      data: { productId: product.id, productName: product.name }
    });

    dialogRef.afterClosed().pipe(
      takeUntil(this.destroy$)
    ).subscribe((result: AddToListDialogResult | undefined) => {
      if (!result) return;

      this.shoppingListService.getMyLists().subscribe({
        next: (lists: any[]) => {
          console.log('🔍 Refrescando listas en HOME:', lists.length);

          const selectedList = lists.find(l => l.listId === result.listId);
          
          if (!selectedList) {
            console.error('❌ Lista no encontrada:', result.listId);
            return;
          }

          const productId = Number(product.id);
          const existingItem = selectedList.items?.find((item: any) => {
            const itemProdId = Number(item.productId);
            return itemProdId === productId;
          });

          if (existingItem) {
            console.log(`✅ Producto YA existe: actualizando cantidad de ${existingItem.quantity} a ${existingItem.quantity + 1}`);
            this.shoppingListService.updateItem(
              selectedList.listId,
              existingItem.itemId,
              existingItem.quantity + 1
            ).subscribe({
              next: () => {
                this.productsInList.add(product.id);
              },
              error: () => {
              }
            });
          } else {
            console.log(`❌ Producto NO existe: añadiendo nuevo`);
            this.shoppingListService.addItem(result.listId, product.id, null, 1).subscribe({
              next: () => {
                this.productsInList.add(product.id);
              },
              error: () => {
              }
            });
          }
        },
        error: (err) => {
          console.error('❌ Error al refrescar listas:', err);
        }
      });
    });
  }

  onToggleFavorite(product: Product): void {
    if (product.isFavorite) {
      this.productService.removeFromFavorites(product.id).subscribe({
        next: () => {
          product.isFavorite = false;
        }
      });
    } else {
      this.productService.addToFavorites(product.id).subscribe({
        next: () => {
          product.isFavorite = true;
        }
      });
    }
  }

  onViewProduct(product: Product): void {
    this.router.navigate(['/producto', product.id], { queryParamsHandling: 'preserve' });
  }

  isProductInList(productId: number): boolean {
    return this.productsInList.has(productId);
  }
}
