package com.smartcart.smartcart.modules.user.dto;

import java.io.Serializable;

public record BillItem(String productName, Double price, Integer quantity) implements Serializable {}
