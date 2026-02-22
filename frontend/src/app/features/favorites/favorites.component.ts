import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { MatIconModule } from '@angular/material/icon';
import { Subject, takeUntil } from 'rxjs';
import { FavoriteService } from '../../shared/services/favorite.service';
import { ProductCardComponent } from '../../shared/components/product-card/product-card.component';
import { AddToListDialogComponent, AddToListDialogResult } from '../../shared/components/add-to-list-dialog/add-to-list-dialog.component';
import { ShoppingListService } from '../../shared/services/shopping-list.service';
import { Product } from '../../core/models/product.model';
import { MatDialog } from '@angular/material/dialog';

@Component({
  selector: 'app-favorites',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatProgressSpinnerModule,
    MatIconModule,
    ProductCardComponent
  ],
  templateUrl: './favorites.component.html',
  styleUrls: ['./favorites.component.css']
})
export class FavoritesComponent implements OnInit, OnDestroy {

  favorites: Product[] = [];
  isLoading = true;
  productsInList: Set<number> = new Set();

  private destroy$ = new Subject<void>();

  constructor(
    private favoriteService: FavoriteService,
    private shoppingListService: ShoppingListService,
    private dialog: MatDialog,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadFavorites();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadFavorites(): void {
    this.isLoading = true;
    this.favoriteService.getMyFavorites()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (favorites) => {
          this.favorites = favorites;
          this.isLoading = false;
        },
        error: (err) => {
          console.error('Error al cargar favoritos:', err);
          this.isLoading = false;
        }
      });
  }

  onToggleFavorite(product: Product): void {
    this.favoriteService.removeFromFavorites(product.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.favorites = this.favorites.filter(p => p.id !== product.id);
        },
        error: () => {
        }
      });
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

      this.shoppingListService.addItem(result.listId, product.id, null, 1).subscribe({
        next: () => {
          this.productsInList.add(product.id);
        },
        error: () => {
        }
      });
    });
  }

  onViewProduct(product: Product): void {
    this.router.navigate(['/producto', product.id]);
  }

  isProductInList(productId: number): boolean {
    return this.productsInList.has(productId);
  }
}
