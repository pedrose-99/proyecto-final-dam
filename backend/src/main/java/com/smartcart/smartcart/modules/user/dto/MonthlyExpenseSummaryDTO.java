package com.smartcart.smartcart.modules.user.dto;

public record MonthlyExpenseSummaryDTO(String periodLabel, double totalAmount, int billCount, int exceededCount)
{
}
