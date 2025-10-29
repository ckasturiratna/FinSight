package com.example.finsight_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ipo_calendar")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IpoCalendar {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "symbol", unique = true, nullable = false, length = 50)
    private String symbol;
    
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    
    @Column(name = "date", nullable = false)
    private LocalDate date;
    
    @Column(name = "exchange", length = 100)
    private String exchange;
    
    @Column(name = "number_of_shares")
    private Long numberOfShares;
    
    @Column(name = "price", length = 50)
    private String price;
    
    @Column(name = "status", length = 50)
    private String status;
    
    @Column(name = "total_shares_value", precision = 20, scale = 2)
    private BigDecimal totalSharesValue;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
