package com.smartcart.smartcart.modules.product.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.smartcart.smartcart.modules.product.dto.ProductStoreDTO;
import com.smartcart.smartcart.modules.product.entity.Product;
import com.smartcart.smartcart.modules.product.entity.ProductStore;
import com.smartcart.smartcart.modules.product.mapper.ProductStoreMapper;
import com.smartcart.smartcart.modules.product.repository.ProductRepository;
import com.smartcart.smartcart.modules.product.repository.ProductStoreRepository;
import com.smartcart.smartcart.modules.store.entity.Store;
import com.smartcart.smartcart.modules.store.repository.StoreRepository;

@Service
public class ProductStoreService {

    private final ProductStoreRepository productStoreRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;

    public ProductStoreService(ProductStoreRepository productStoreRepository,
                               ProductRepository productRepository,
                               StoreRepository storeRepository) {
        this.productStoreRepository = productStoreRepository;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
    }

    public ProductStoreDTO link(Integer productId, Integer storeId, String url, Integer stock) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));

        ProductStore ps = new ProductStore();
        ps.setProductId(product);
        ps.setStoreId(store);
        ps.setUrl(url);
        ps.setStock(stock);
        ps.setAvailable(stock != null && stock > 0);

        return ProductStoreMapper.toDTO(productStoreRepository.save(ps));
    }

    public ProductStoreDTO updateProductStore(Integer productId, Integer storeId, Boolean available) {
        ProductStore ps = productStoreRepository
                .findByProductId_ProductIdAndStoreId_StoreId(productId, storeId)
                .orElseThrow(() -> new RuntimeException("Relación producto-tienda no encontrada"));

        if (available != null) ps.setAvailable(available);

        return ProductStoreMapper.toDTO(productStoreRepository.save(ps));
    }

    public List<ProductStoreDTO> findStoresByProductId(Integer productId) {
        List<ProductStore> stores = productStoreRepository.findByProductId_ProductId(productId);
        return stores.stream()
                .map(ProductStoreMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<ProductStore> findUniqueStoreProduct(Integer productId, Integer storeId) {
        return productStoreRepository.findByProductId_ProductIdAndStoreId_StoreId(productId, storeId);
    }
}
