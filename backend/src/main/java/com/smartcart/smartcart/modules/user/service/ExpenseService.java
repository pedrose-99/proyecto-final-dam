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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


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

    private static final String[] MONTH_NAMES = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
    private static final String[] DAY_NAMES = {"Lun", "Mar", "Mie", "Jue", "Vie", "Sab", "Dom"};

    @Transactional
    public SpendingLimit saveSpendingLimit(Long userId, BigDecimal amount, String typeStr) {
        LimitType type = LimitType.valueOf(typeStr.toUpperCase());

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

    @Transactional
    public BillsHistory registerBill(Long userId, String name) {
        List<BillItem> mockItems = List.of(
            new BillItem("Aceite Oliva", 5.50, 1, null),
            new BillItem("Leche Entera", 1.20, 6, null)
        );

        Double totalAmount = mockItems.stream()
            .mapToDouble(i -> i.price() * i.quantity())
            .sum();

        boolean exceeded = limitRepository.findByIdUserAndIsActiveTrue(userId).stream()
                .anyMatch(l -> BigDecimal.valueOf(totalAmount).compareTo(l.getAmount()) > 0);

        BillsHistory history = new BillsHistory();
        history.setIdUser(userId);
        history.setName(name);
        history.setRecordedAt(LocalDateTime.now());
        history.setTotalAmount(totalAmount);
        history.setExceededLimit(exceeded);
        history.setItemsSummary(mockItems);

        budgetProducer.checkLimitsAndNotify(userId, totalAmount);

        return historyRepository.save(history);
    }

    @Transactional
    public BillsHistory createBillFromList(Long userId, Integer listId, String billName, String purchaseDate) {
        ShoppingList shoppingList = shoppingListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("Lista no encontrada con id: " + listId));

        List<BillItem> billItems = new ArrayList<>();
        for (ListItem item : shoppingList.getItems()) {
            String productName;
            Double price = 0.0;

            String storeName = null;

            if (item.getProduct() != null) {
                productName = item.getProduct().getName();
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
                productName = item.getGenericName() != null ? item.getGenericName() : "Producto generico";
            }

            billItems.add(new BillItem(productName, price, item.getQuantity(), storeName));
        }

        Double totalAmount = billItems.stream()
                .mapToDouble(i -> i.price() * i.quantity())
                .sum();

        boolean exceeded = limitRepository.findByIdUserAndIsActiveTrue(userId).stream()
                .anyMatch(l -> BigDecimal.valueOf(totalAmount).compareTo(l.getAmount()) > 0);

        BillsHistory history = new BillsHistory();
        history.setIdUser(userId);
        history.setName(billName);
        LocalDateTime recordDate = LocalDateTime.now();
        if (purchaseDate != null && !purchaseDate.isBlank())
        {
            try
            {
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

        try
        {
            budgetProducer.checkLimitsAndNotify(userId, totalAmount);
        }
        catch (Exception e)
        {
            log.warn("Error al enviar alerta Kafka para usuario {}: {}", userId, e.getMessage());
        }

        BillsHistory saved = historyRepository.save(history);

        try
        {
            User user = userRepository.findById(Math.toIntExact(userId)).orElse(null);
            if (user != null)
            {
                Notification notification = new Notification();
                notification.setRecipient(user);
                notification.setMessage("Compra '" + billName + "' registrada por " + String.format("%.2f", totalAmount) + "\u20AC");
                notification.setType(NotificationType.PURCHASE);
                notificationRepository.save(notification);
            }
        }
        catch (Exception e)
        {
            log.warn("Error al crear notificacion de compra para usuario {}: {}", userId, e.getMessage());
        }

        return saved;
    }

    public List<BillsHistory> getHistory(Long userId, String filter, Integer month, Integer year) {
        return historyRepository.findAdvancedHistory(userId, filter, month, year);
    }

    public List<SpendingLimit> getActiveLimits(Long userId)
    {
        return limitRepository.findByIdUserAndIsActiveTrue(userId);
    }

    /**
     * Returns expense summary for the given period:
     * - WEEKLY:  7 entries (Lun-Dom) for one specific week. offset=0 is current week.
     * - MONTHLY: 4-5 entries (Sem 1-5) for one specific month. offset=0 is current month.
     * - YEARLY:  12 entries (Ene-Dic) for one specific year. offset=0 is current year.
     */
    public List<MonthlyExpenseSummaryDTO> getExpenseSummary(Long userId, String period, int offset)
    {
        switch (period.toUpperCase())
        {
            case "WEEKLY":
                return buildWeeklySummary(userId, offset);
            case "YEARLY":
                return buildYearlySummary(userId, offset);
            default:
                return buildMonthlySummary(userId, offset);
        }
    }

    /**
     * WEEKLY: Shows the 7 days (Lun-Dom) of a single week.
     * offset=0 → current week, offset=1 → previous week, etc.
     */
    private List<MonthlyExpenseSummaryDTO> buildWeeklySummary(Long userId, int offset)
    {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(offset);
        LocalDateTime since = monday.atStartOfDay();
        LocalDateTime until = monday.plusDays(7).atStartOfDay();

        List<Object[]> rows = historyRepository.findDailySummaryRaw(userId, since, until);

        // Build a map: dayOfWeek (1=Mon..7=Sun) -> row data
        Map<Integer, Object[]> dayMap = rows.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(),
                        row -> row,
                        (a, b) -> a
                ));

        List<MonthlyExpenseSummaryDTO> result = new ArrayList<>();
        for (int dow = 1; dow <= 7; dow++)
        {
            LocalDate dayDate = monday.plusDays(dow - 1);
            String label = DAY_NAMES[dow - 1] + " " + dayDate.getDayOfMonth();
            Object[] row = dayMap.get(dow);
            if (row != null)
            {
                result.add(new MonthlyExpenseSummaryDTO(
                        label,
                        ((Number) row[3]).doubleValue(),
                        ((Number) row[4]).intValue(),
                        ((Number) row[5]).intValue()
                ));
            }
            else
            {
                result.add(new MonthlyExpenseSummaryDTO(label, 0.0, 0, 0));
            }
        }
        return result;
    }

    /**
     * MONTHLY: Shows weeks (Sem 1-5) of a single month.
     * offset=0 → current month, offset=1 → previous month, etc.
     */
    private List<MonthlyExpenseSummaryDTO> buildMonthlySummary(Long userId, int offset)
    {
        YearMonth ym = YearMonth.now().minusMonths(offset);
        LocalDateTime since = ym.atDay(1).atStartOfDay();
        LocalDateTime until = ym.plusMonths(1).atDay(1).atStartOfDay();

        List<Object[]> rows = historyRepository.findWeeklyInMonthSummaryRaw(userId, since, until);

        Map<Integer, Object[]> weekMap = rows.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(),
                        row -> row,
                        (a, b) -> a
                ));

        int totalWeeks = (int) Math.ceil(ym.lengthOfMonth() / 7.0);
        List<MonthlyExpenseSummaryDTO> result = new ArrayList<>();
        for (int w = 1; w <= totalWeeks; w++)
        {
            String label = "Sem " + w;
            Object[] row = weekMap.get(w);
            if (row != null)
            {
                result.add(new MonthlyExpenseSummaryDTO(
                        label,
                        ((Number) row[1]).doubleValue(),
                        ((Number) row[2]).intValue(),
                        ((Number) row[3]).intValue()
                ));
            }
            else
            {
                result.add(new MonthlyExpenseSummaryDTO(label, 0.0, 0, 0));
            }
        }
        return result;
    }

    /**
     * YEARLY: Shows 12 months (Ene-Dic) of a single year.
     * offset=0 → current year, offset=1 → previous year, etc.
     */
    private List<MonthlyExpenseSummaryDTO> buildYearlySummary(Long userId, int offset)
    {
        int year = LocalDate.now().getYear() - offset;
        LocalDateTime since = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime until = LocalDate.of(year + 1, 1, 1).atStartOfDay();

        List<Object[]> rows = historyRepository.findMonthlyInYearSummaryRaw(userId, since, until);

        Map<Integer, Object[]> monthMap = rows.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(),
                        row -> row,
                        (a, b) -> a
                ));

        List<MonthlyExpenseSummaryDTO> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++)
        {
            String label = MONTH_NAMES[m - 1];
            Object[] row = monthMap.get(m);
            if (row != null)
            {
                result.add(new MonthlyExpenseSummaryDTO(
                        label,
                        ((Number) row[1]).doubleValue(),
                        ((Number) row[2]).intValue(),
                        ((Number) row[3]).intValue()
                ));
            }
            else
            {
                result.add(new MonthlyExpenseSummaryDTO(label, 0.0, 0, 0));
            }
        }
        return result;
    }
}
