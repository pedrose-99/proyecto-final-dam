package com.smartcart.smartcart.modules.user.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.smartcart.smartcart.modules.user.dto.BillItem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "bills_history")
@Data
public class BillsHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long billsHistoryId;

    private Long idUser;
    
    @Column(unique = true)
    private String name;

    private LocalDateTime recordedAt;
    private Double totalAmount;
    private Boolean exceededLimit;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<BillItem> itemsSummary;
}
