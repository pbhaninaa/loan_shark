package com.loanshark.api.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "borrower_documents")
public class BorrowerDocument {

    @Id
    @JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_id", nullable = false)
    private Borrower borrower;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "sha256_checksum", nullable = false, length = 128)
    private String sha256Checksum;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Lob
    @Column(name = "file_data", columnDefinition = "MEDIUMBLOB")
    private byte[] fileData;
    @Override
    public String toString() {
        return "BorrowerDocument{" +
                "id=" + id +
                ", documentType=" + documentType +
                ", fileUrl='" + fileUrl + '\'' +
                ", originalFileName='" + originalFileName + '\'' +
                ", contentType='" + contentType + '\'' +
                ", fileSizeBytes=" + fileSizeBytes +
                ", uploadedAt=" + uploadedAt +
                '}';
    }
    @PrePersist
    void onPrePersist() {
        if (id == null) id = UUID.randomUUID();
        if (uploadedAt == null) uploadedAt = Instant.now();
    }
}