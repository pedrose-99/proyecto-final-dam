import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './product-detail.html',
  styleUrls: ['./product-detail.css']
})
export class ProductDetailComponent implements OnInit {
  
  // 1. Definimos el objeto product que el HTML está buscando
  product = {
    name: 'Smartphone ProX 256GB Midnight Blue',
    ean: '8410010002341',
    description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.',
    rating: 4.5,
    reviews: 185,
    imageUrl: 'https://via.placeholder.com/400' // Puedes cambiar esto por una URL real
  };

  // 2. Definimos el array de tiendas (stores)
  stores = [
    { name: 'SMARBET', price: 999.99, stock: 'Disponible', url: 'smarbet.es', isBest: true },
    { name: 'CARS', price: 1049.00, stock: 'Disponible', url: 'cars.es', isBest: false },
    { name: 'ADX', price: 1099.99, stock: 'Sin Stock', url: 'adx.es', isBest: false }
  ];

  constructor() {}

  ngOnInit(): void {
    console.log('Product Detail Initialized');
  }

  // 3. Definimos la función para el botón de favoritos
  toggleFavorite() {
    alert('¡Añadido a favoritos!');
  }
}