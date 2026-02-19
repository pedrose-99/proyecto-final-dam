package com.smartcart.smartcart.modules.user.service;


import com.smartcart.smartcart.common.enums.NotificationType;
import com.smartcart.smartcart.modules.notification.entity.Notification;
import com.smartcart.smartcart.modules.notification.repository.NotificationRepository;
import com.smartcart.smartcart.modules.product.entity.ProductStore;
import com.smartcart.smartcart.modules.product.repository.ProductStoreRepository;
import com.smartcart.smartcart.modules.shoppinglist.entity.ListItem;
import com.smartcart.smartcart.modules.shoppinglist.entity.ShoppingList;
import com.smartcart.smartcart.modules.shoppinglist.repository.ShoppingListRepository;
import com.smartcart.smartcart.modules.user.dto.BillItem;
import com.smartcart.smartcart.modules.user.dto.MonthlyExpenseSummaryDTO;
import com.smartcart.smartcart.modules.user.entity.BillsHistory;
import com.smartcart.smartcart.modules.user.entity.LimitType;
import com.smartcart.smartcart.modules.user.entity.SpendingLimit;
import com.smartcart.smartcart.modules.user.entity.User;
import com.smartcart.smartcart.modules.user.repository.BillsHistoryRepository;
import com.smartcart.smartcart.modules.user.repository.SpendingLimitRepository;
import com.smartcart.smartcart.modules.user.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final SpendingLimitRepository limitRepository;
    private final BillsHistoryRepository historyRepository;
    private final BudgetProducerService budgetProducer;
    private final ShoppingListRepository shoppingListRepository;
    private final ProductStoreRepository productStoreRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

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
            new BillItem("Aceite Oliva", 5.50, 1, null),
            new BillItem("Leche Entera", 1.20, 6, null)
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
    public BillsHistory createBillFromList(Long userId, Integer listId, String billName, String purchaseDate) {
        // 1. Buscar la lista de la compra
        ShoppingList shoppingList = shoppingListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("Lista no encontrada con id: " + listId));

        // 2. Convertir cada ListItem a BillItem
        List<BillItem> billItems = new ArrayList<>();
        for (ListItem item : shoppingList.getItems()) {
            String productName;
            Double price = 0.0;

            String storeName = null;

            if (item.getProduct() != null) {
                productName = item.getProduct().getName();
                // Buscar el precio más barato disponible y capturar la tienda
                List<ProductStore> stores = productStoreRepository
                        .findByProductId_ProductId(item.getProduct().getProductId());
                Optional<ProductStore> cheapest = stores.stream()
                        .filter(ps -> ps.getCurrentPrice() != null)
                        .min(Comparator.comparingDouble(ProductStore::getCurrentPrice));
                if (cheapest.isPresent()) {
                    price = cheapest.get().getCurrentPrice();
                    storeName = cheapest.get().getStoreId().getName();
                }
            } else {
                productName = item.getGenericName() != null ? item.getGenericName() : "Producto genérico";
            }

            billItems.add(new BillItem(productName, price, item.getQuantity(), storeName));
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
        LocalDateTime recordDate = LocalDateTime.now();
        if (purchaseDate != null && !purchaseDate.isBlank())
        {
            try
            {
                // Extraer solo la parte de fecha (YYYY-MM-DD) del ISO string
                LocalDate date = LocalDate.parse(purchaseDate.substring(0, 10));
                recordDate = date.atStartOfDay();
            }
            catch (Exception e)
            {
                log.warn("No se pudo parsear purchaseDate '{}', usando fecha actual", purchaseDate, e);
            }
        }
        history.setRecordedAt(recordDate);
        history.setTotalAmount(totalAmount);
        history.setExceededLimit(exceeded);
        history.setItemsSummary(billItems);

        // 6. Disparar evento Kafka (no bloqueante)
        try
        {
            budgetProducer.checkLimitsAndNotify(userId, totalAmount);
        }
        catch (Exception e)
        {
            log.warn("Error al enviar alerta Kafka para usuario {}: {}", userId, e.getMessage());
        }

        BillsHistory saved = historyRepository.save(history);

        // 7. Crear notificación de compra
        try
        {
            User user = userRepository.findById(Math.toIntExact(userId)).orElse(null);
            if (user != null)
            {
                Notification notification = new Notification();
                notification.setRecipient(user);
                notification.setMessage("Compra '" + billName + "' registrada por " + String.format("%.2f", totalAmount) + "€");
                notification.setType(NotificationType.PURCHASE);
                notificationRepository.save(notification);
            }
        }
        catch (Exception e)
        {
            log.warn("Error al crear notificación de compra para usuario {}: {}", userId, e.getMessage());
        }

        return saved;
    }

    // --- QUERY: getBillsHistory ---
    public List<BillsHistory> getHistory(Long userId, String filter, Integer month, Integer year) {
        return historyRepository.findAdvancedHistory(userId, filter, month, year);
    }

    // --- QUERY: getActiveLimits ---
    public List<SpendingLimit> getActiveLimits(Long userId)
    {
        return limitRepository.findByIdUserAndIsActiveTrue(userId);
    }

    // --- QUERY: getExpenseSummary ---
    private static final String[] MONTH_NAMES = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
    private static final int WEEKLY_WINDOW = 8;
    private static final int MONTHLY_WINDOW = 6;
    private static final int YEARLY_WINDOW = 5;

    public List<MonthlyExpenseSummaryDTO> getExpenseSummary(Long userId, String period, int offset)
    {
        switch (period.toUpperCase())
        {
            case "WEEKLY":
            {
                LocalDateTime until = LocalDateTime.now().minusWeeks((long) offset * WEEKLY_WINDOW);
                LocalDateTime since = until.minusWeeks(WEEKLY_WINDOW);
                return historyRepository.findWeeklySummaryRaw(userId, since, until).stream()
                        .map(row -> new MonthlyExpenseSummaryDTO(
                                "Sem " + ((Number) row[0]).intValue(),
                                ((Number) row[2]).doubleValue(),
                                ((Number) row[3]).intValue(),
                                ((Number) row[4]).intValue()
                        ))
                        .toList();
            }
            case "YEARLY":
            {
                LocalDateTime until = LocalDateTime.now().minusYears((long) offset * YEARLY_WINDOW);
                LocalDateTime since = until.minusYears(YEARLY_WINDOW);
                return historyRepository.findYearlySummaryRaw(userId, since, until).stream()
                        .map(row -> new MonthlyExpenseSummaryDTO(
                                String.valueOf(((Number) row[0]).intValue()),
                                ((Number) row[1]).doubleValue(),
                                ((Number) row[2]).intValue(),
                                ((Number) row[3]).intValue()
                        ))
                        .toList();
            }
            default: // MONTHLY
            {
                LocalDateTime until = LocalDateTime.now().minusMonths((long) offset * MONTHLY_WINDOW);
                LocalDateTime since = until.minusMonths(MONTHLY_WINDOW);
                return historyRepository.findMonthlySummaryRaw(userId, since, until).stream()
                        .map(row -> new MonthlyExpenseSummaryDTO(
                                MONTH_NAMES[((Number) row[0]).intValue() - 1] + " " + ((Number) row[1]).intValue(),
                                ((Number) row[2]).doubleValue(),
                                ((Number) row[3]).intValue(),
                                ((Number) row[4]).intValue()
                        ))
                        .toList();
            }
        }
    }
}
