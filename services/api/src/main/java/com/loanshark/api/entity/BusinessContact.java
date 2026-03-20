package com.loanshark.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "business_contact")
@Data
public class BusinessContact {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(nullable = false)
    private String businessName;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String email;
    @Column(nullable = false)
    private String accountNumber;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;
    @Column(nullable = false)
    private String bankName;

    @Column(nullable = false)
    private String accountHolderName;

    @Column(nullable = false)
    private String accountType;

    @Column(nullable = false)
    private String branchCode;

    @Column(nullable = false)
    private String paymentReference;
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }


}
