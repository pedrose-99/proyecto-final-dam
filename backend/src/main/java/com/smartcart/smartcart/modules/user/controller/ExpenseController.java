package com.smartcart.smartcart.modules.user.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.smartcart.smartcart.modules.user.dto.MonthlyExpenseSummaryDTO;
import com.smartcart.smartcart.modules.user.entity.BillsHistory;
import com.smartcart.smartcart.modules.user.entity.SpendingLimit;
import com.smartcart.smartcart.modules.user.entity.User;
import com.smartcart.smartcart.modules.user.repository.UserRepository;
import com.smartcart.smartcart.modules.user.service.ExpenseService;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ExpenseController
{

    private final ExpenseService expenseService;
    private final UserRepository userRepository;

    private Long getCurrentUserId()
    {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return Long.valueOf(user.getIdUser());
    }

    @MutationMapping
    public SpendingLimit saveSpendingLimit(@Argument BigDecimal amount, @Argument String type)
    {
        return expenseService.saveSpendingLimit(getCurrentUserId(), amount, type);
    }

    @MutationMapping
    public BillsHistory registerBill(@Argument String name)
    {
        return expenseService.registerBill(getCurrentUserId(), name);
    }

    @MutationMapping
    public BillsHistory createBillFromList(@Argument Integer listId, @Argument String billName)
    {
        return expenseService.createBillFromList(getCurrentUserId(), listId, billName);
    }

    @QueryMapping
    public List<BillsHistory> getBillsHistory(@Argument String filter, @Argument Integer month, @Argument Integer year)
    {
        return expenseService.getHistory(getCurrentUserId(), filter, month, year);
    }

    @QueryMapping
    public List<SpendingLimit> getSpendingLimits()
    {
        return expenseService.getActiveLimits(getCurrentUserId());
    }

    @QueryMapping
    public List<MonthlyExpenseSummaryDTO> getExpenseSummary(@Argument String period, @Argument Integer offset)
    {
        return expenseService.getExpenseSummary(
                getCurrentUserId(),
                period != null ? period : "MONTHLY",
                offset != null ? offset : 0
        );
    }
}
