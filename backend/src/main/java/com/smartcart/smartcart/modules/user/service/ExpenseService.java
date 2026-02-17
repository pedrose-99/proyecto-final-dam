package com.smartcart.smartcart.modules.user.service;


import com.smartcart.smartcart.modules.product.entity.ProductStore;
import com.smartcart.smartcart.modules.product.repository.ProductStoreRepository;
import com.smartcart.smartcart.modules.shoppinglist.entity.ListItem;
import com.smartcart.smartcart.modules.shoppinglist.entity.ShoppingList;
import com.smartcart.smartcart.modules.shoppinglist.repository.ShoppingListRepository;
import com.smartcart.smartcart.modules.user.dto.BillItem;
import com.smartcart.smartcart.modules.user.entity.BillsHistory;
import com.smartcart.smartcart.modules.user.entity.LimitType;
import com.smartcart.smartcart.modules.user.entity.SpendingLimit;
import com.smartcart.smartcart.modules.user.repository.BillsHistoryRepository;
import com.smartcart.smartcart.modules.user.repository.SpendingLimitRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final SpendingLimitRepository limitRepository;
    private final BillsHistoryRepository historyRepository;
    private final BudgetProducerService budgetProducer;
    private final ShoppingListRepository shoppingListRepository;
    private final ProductStoreRepository productStoreRepository;

    // --- MUTATION: saveSpendingLimit ---
    @Transactional
    public SpendingLimit saveSpendingLimit(Long userId, BigDecimal amount, String typeStr) {
        LimitType type = LimitType.valueOf(typeStr.toUpperCase());
        
        // Buscamos si ya existe para actualizar, si no creamos
        SpendingLimit limit = limitRepository.findByIdUserAndType(userId, type)
                .orElse(new SpendingLimit());
        
        if (limit.getLimitId() == null) {
            limit.setIdUser(userId);
            limit.setType(type);
        }
        
        limit.setAmount(amount);
        limit.setIsActive(true);
        
        return limitRepository.save(limit);
    }

    // --- MUTATION: registerBill (Simulación) ---
    @Transactional
    public BillsHistory registerBill(Long userId, String name) {
        // 1. Obtener items de la lista activa (Simulado)
        // List<CartItem> cartItems = shoppingListService.getCart(userId);
        
        // MOCK DATA para el ejemplo
        List<BillItem> mockItems = List.of(
            new BillItem("Aceite Oliva", 5.50, 1),
            new BillItem("Leche Entera", 1.20, 6)
        );
        
        Double totalAmount = mockItems.stream()
            .mapToDouble(i -> i.price() * i.quantity())
            .sum();

        // 2. Comprobar si excede algún límite en el momento del registro
        // (Esto es aparte de Kafka, es para persistir el flag en el historial)
        boolean exceeded = limitRepository.findByIdUserAndIsActiveTrue(userId).stream()
                .anyMatch(l -> BigDecimal.valueOf(totalAmount).compareTo(l.getAmount()) > 0);

        // 3. Crear el registro histórico
        BillsHistory history = new BillsHistory();
        history.setIdUser(userId);
        history.setName(name);
        history.setRecordedAt(LocalDateTime.now());
        history.setTotalAmount(totalAmount);
        history.setExceededLimit(exceeded);
        history.setItemsSummary(mockItems); // JPA convierte esto a JSONB automáticamente

        // 4. Disparar evento Kafka (Opcional, ya que se disparó al añadir al carrito, pero útil confirmar)
        budgetProducer.checkLimitsAndNotify(userId, totalAmount);

        return historyRepository.save(history);
    }

    // --- MUTATION: createBillFromList (desde lista real) ---
    @Transactional
    public BillsHistory createBillFromList(Long userId, Integer listId, String billName) {
        // 1. Buscar la lista de la compra
        ShoppingList shoppingList = shoppingListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("Lista no encontrada con id: " + listId));

        // 2. Convertir cada ListItem a BillItem
        List<BillItem> billItems = new ArrayList<>();
        for (ListItem item : shoppingList.getItems()) {
            String productName;
            Double price = 0.0;

            if (item.getProduct() != null) {
                productName = item.getProduct().getName();
                // Buscar el precio más barato disponible
                List<ProductStore> stores = productStoreRepository
                        .findByProductId_ProductId(item.getProduct().getProductId());
                price = stores.stream()
                        .filter(ps -> ps.getCurrentPrice() != null)
                        .mapToDouble(ProductStore::getCurrentPrice)
                        .min()
                        .orElse(0.0);
            } else {
                productName = item.getGenericName() != null ? item.getGenericName() : "Producto genérico";
            }

            billItems.add(new BillItem(productName, price, item.getQuantity()));
        }

        // 3. Calcular total
        Double totalAmount = billItems.stream()
                .mapToDouble(i -> i.price() * i.quantity())
                .sum();

        // 4. Comprobar límites
        boolean exceeded = limitRepository.findByIdUserAndIsActiveTrue(userId).stream()
                .anyMatch(l -> BigDecimal.valueOf(totalAmount).compareTo(l.getAmount()) > 0);

        // 5. Crear registro histórico
        BillsHistory history = new BillsHistory();
        history.setIdUser(userId);
        history.setName(billName);
        history.setRecordedAt(LocalDateTime.now());
        history.setTotalAmount(totalAmount);
        history.setExceededLimit(exceeded);
        history.setItemsSummary(billItems);

        // 6. Disparar evento Kafka
        budgetProducer.checkLimitsAndNotify(userId, totalAmount);

        return historyRepository.save(history);
    }

    // --- QUERY: getBillsHistory ---
    public List<BillsHistory> getHistory(Long userId, String filter, Integer month, Integer year) {
        return historyRepository.findAdvancedHistory(userId, filter, month, year);
    }
}
