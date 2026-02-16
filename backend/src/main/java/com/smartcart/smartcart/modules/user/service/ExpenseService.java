package com.smartcart.smartcart.modules.user.service;


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
import java.util.List;


@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final SpendingLimitRepository limitRepository;
    private final BillsHistoryRepository historyRepository;
    private final BudgetProducerService budgetProducer;
    
    // Simulación: Inyectaríamos el ShoppingListService real aquí
    // private final ShoppingListService shoppingListService;

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

    // --- QUERY: getBillsHistory ---
    public List<BillsHistory> getHistory(Long userId, String filter, Integer month, Integer year) {
        return historyRepository.findAdvancedHistory(userId, filter, month, year);
    }
}
