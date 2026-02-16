package com.smartcart.smartcart.modules.product.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.smartcart.smartcart.modules.product.dto.ProductAlertDTO;
import com.smartcart.smartcart.modules.product.entity.Product;
import com.smartcart.smartcart.modules.product.entity.ProductAlert;
import com.smartcart.smartcart.modules.product.entity.ProductStore;
import com.smartcart.smartcart.modules.product.repository.ProductAlertRepository;
import com.smartcart.smartcart.modules.product.repository.ProductRepository;
import com.smartcart.smartcart.modules.product.repository.ProductStoreRepository;

@Service
public class ProductAlertService {

    private final ProductAlertRepository productAlertRepository;
    private final ProductRepository productRepository;
    private final ProductStoreRepository productStoreRepository;

    public ProductAlertService(ProductAlertRepository productAlertRepository,
                               ProductRepository productRepository,
                               ProductStoreRepository productStoreRepository) {
        this.productAlertRepository = productAlertRepository;
        this.productRepository = productRepository;
        this.productStoreRepository = productStoreRepository;
    }

    public ProductAlertDTO createAlert(Integer productId, Double targetPrice) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        ProductAlert alert = new ProductAlert(product, targetPrice);
        alert = productAlertRepository.save(alert);

        return toDTO(alert);
    }

    public ProductAlertDTO deactivateAlert(Integer alertId) {
        ProductAlert alert = productAlertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alerta no encontrada"));
        alert.setActive(false);
        return toDTO(productAlertRepository.save(alert));
    }

    public List<ProductAlertDTO> getActiveAlerts() {
        return productAlertRepository.findByActiveTrue().stream()
                .map(this::toDTO)
                .toList();
    }

    public List<ProductAlertDTO> getAlertsByProduct(Integer productId) {
        return productAlertRepository.findByProduct_ProductId(productId).stream()
                .map(this::toDTO)
                .toList();
    }

    private ProductAlertDTO toDTO(ProductAlert entity) {
        List<ProductStore> stores = productStoreRepository
                .findByProductId_ProductId(entity.getProduct().getProductId());
        Double bestPrice = stores.stream()
                .filter(ps -> Boolean.TRUE.equals(ps.getAvailable()) && ps.getCurrentPrice() != null)
                .map(ProductStore::getCurrentPrice)
                .min(Comparator.naturalOrder())
                .orElse(null);

        return new ProductAlertDTO(
            entity.getAlertId(),
            entity.getProduct().getProductId(),
            entity.getProduct().getName(),
            entity.getProduct().getEan(),
            entity.getTargetPrice(),
            bestPrice,
            entity.getActive(),
            entity.getTriggered(),
            entity.getCreatedAt()
        );
    }
}
