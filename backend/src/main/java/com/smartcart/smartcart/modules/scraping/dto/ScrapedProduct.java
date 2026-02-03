package com.smartcart.smartcart.modules.scraping.dto;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * DTO que representa un producto extraído del scraping.
 * Inmutable para seguridad en concurrencia.
 */
@Builder
public record ScrapedProduct(
    String externalId,        // ID en la tienda original
    String name,              // Nombre del producto
    String brand,             // Marca (puede ser null)
    String description,       // Descripcion
    BigDecimal price,         // Precio actual
    BigDecimal originalPrice, // Precio sin descuento (si aplica)
    boolean onSale,           // Esta en oferta
    String pricePerUnit,      // "2.50€/kg"
    String unit,              // "kg", "L", "unidad"
    String imageUrl,          // URL de imagen
    String productUrl,        // URL del producto en la tienda
    String categoryName,      // Categoria en la tienda
    String categoryId         // ID de categoria en la tienda
) {}
