package com.loanshark.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.type.SqlTypes;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "borrower_verifications")
public class BorrowerVerification {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_id", nullable = false)
    private Borrower borrower;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus status = VerificationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_document_id")
    @org.hibernate.annotations.NotFound(action = NotFoundAction.IGNORE)
    private BorrowerDocument idDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selfie_document_id")
    @org.hibernate.annotations.NotFound(action = NotFoundAction.IGNORE)
    private BorrowerDocument selfieDocument;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "location_captured_at")
    private Instant locationCapturedAt;

    @Column(name = "location_name", length = 500)
    private String locationName;

    @Column(name = "sa_id_valid", nullable = false)
    private boolean saIdValid;

    @Column(name = "ocr_confidence", precision = 5, scale = 2)
    private BigDecimal ocrConfidence;

    @Column(name = "extracted_first_name")
    private String extractedFirstName;

    @Column(name = "extracted_last_name")
    private String extractedLastName;

    @Column(name = "extracted_id_number")
    private String extractedIdNumber;

    @Column(name = "details_matched", nullable = false)
    private boolean detailsMatched;

    @Column(name = "face_match_score", precision = 5, scale = 2)
    private BigDecimal faceMatchScore;

    @Column(name = "face_matched", nullable = false)
    private boolean faceMatched;

    @Column(name = "review_notes", length = 2000)
    private String reviewNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    @PrePersist
    void onPrePersist() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }
}
