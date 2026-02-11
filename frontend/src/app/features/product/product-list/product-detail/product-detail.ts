import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ProductService } from '../../../../core/services/product.service';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './product-detail.html',
  styleUrls: ['./product-detail.css']
})
export class ProductDetailComponent implements OnInit {
  loading = true;
  stores: any[] = [];
  bestPrice: number | null = null;
  
  product: any = {
    name: 'Cargando...',
    ean: '',
    description: '',
    imageUrl: '',
    rating: 4.5,
    reviews: 185
  };

  constructor(
    private route: ActivatedRoute,
    private productService: ProductService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.productService.getComparison(id).subscribe({
        next: (data: any) => {
          if (data) {
            // Mapeamos los datos reales del DTO de Java
            this.product = {
              name: data.name,
              ean: data.ean,
              brand: data.brand,
              description: data.description || 'Sin descripción disponible para este producto.',
              imageUrl: data.imageUrl,
              rating: 4.5, // Dato estático por ahora
              reviews: 185  // Dato estático por ahora
            };
            this.stores = data.storePrices || [];
            this.bestPrice = data.bestPrice?.currentPrice || null;
          }
          
          this.loading = false;
          this.cdr.detectChanges();
        },
        error: (err: any) => {
          console.error('Error cargando producto:', err);
          this.loading = false;
          this.cdr.detectChanges()
        }
      });
    }
  }

  toggleFavorite(): void {
    alert('¡Añadido a favoritos!');
  }
}