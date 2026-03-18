package com.loanshark.api.repository;

import com.loanshark.api.entity.BorrowerDocument;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.loanshark.api.entity.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BorrowerDocumentRepository extends JpaRepository<BorrowerDocument, UUID> {
    // BorrowerDocumentRepository
    List<BorrowerDocument> findByBorrowerIdAndDocumentTypeIn(UUID borrowerId, List<DocumentType> types);
    List<BorrowerDocument> findByBorrowerId(UUID borrowerId);
    Optional<BorrowerDocument> findByIdAndBorrowerId(UUID documentId, UUID borrowerId);

     void deleteByBorrowerId(UUID borrowerId);
}