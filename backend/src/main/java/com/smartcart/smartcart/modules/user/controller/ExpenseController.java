package com.smartcart.smartcart.modules.user.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.smartcart.smartcart.modules.user.entity.BillsHistory;
import com.smartcart.smartcart.modules.user.entity.SpendingLimit;
import com.smartcart.smartcart.modules.user.service.ExpenseService;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    // Helper para obtener usuario actual (Simulado)
    private Long getCurrentUserId() {
        return 1L; // Hardcodeado por simplicidad, aquí iría SecurityContext
    }

    @MutationMapping
    public SpendingLimit saveSpendingLimit(@Argument BigDecimal amount, @Argument String type) {
        return expenseService.saveSpendingLimit(getCurrentUserId(), amount, type);
    }

    @MutationMapping
    public BillsHistory registerBill(@Argument String name) {
        return expenseService.registerBill(getCurrentUserId(), name);
    }

    @QueryMapping
    public List<BillsHistory> getBillsHistory(@Argument String filter, @Argument Integer month, @Argument Integer year) {
        return expenseService.getHistory(getCurrentUserId(), filter, month, year);
    }
}
