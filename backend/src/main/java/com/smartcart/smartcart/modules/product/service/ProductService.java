package com.smartcart.smartcart.modules.product.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import com.smartcart.smartcart.modules.category.entity.Category;
import com.smartcart.smartcart.modules.category.repository.CategoryRepository;
import com.smartcart.smartcart.modules.product.dto.ProductDTO;
import com.smartcart.smartcart.modules.product.entity.Product;
import com.smartcart.smartcart.modules.product.mapper.ProductMapper;
import com.smartcart.smartcart.modules.product.repository.ProductRepository;




@Service
public class ProductService {
    
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<ProductDTO> findAll() { 
        return productRepository.findAll().stream()
                .map(ProductMapper::toDTO)
                .toList(); 
    }


    public ProductDTO findByEan(String ean) {
        Product p = productRepository.findByEan(ean)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return ProductMapper.toDTO(p);
    }
    
    public Product create(String name, String ean, String brand, Integer categoryId) {
        Category cat = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new RuntimeException("Category not found"));
        
        Product p = new Product();
        p.setName(name);
        p.setEan(ean);
        p.setBrand(brand);
        p.setCategoryId(cat); 
        return productRepository.save(p);
    }

    public Product update(Integer id, String name, String brand, String imageUrl) {
        Product p = productRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        if (name != null) p.setName(name);
        if (brand != null) p.setBrand(brand);
        if (imageUrl != null) p.setImageUrl(imageUrl);
        return productRepository.save(p);
    }

    public Boolean delete(Integer id) {
        productRepository.deleteById(id);
        return true;
    }
}