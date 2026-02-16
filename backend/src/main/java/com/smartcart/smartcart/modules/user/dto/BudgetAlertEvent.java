package com.smartcart.smartcart.modules.user.dto;

import java.math.BigDecimal;

import com.smartcart.smartcart.modules.user.entity.LimitType;

public record BudgetAlertEvent(Long userId, Double currentTotal, BigDecimal limitAmount, LimitType type) {}
