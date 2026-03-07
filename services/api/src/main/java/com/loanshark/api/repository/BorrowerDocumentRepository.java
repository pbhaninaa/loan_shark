package com.loanshark.api.repository;

import com.loanshark.api.entity.BorrowerDocument;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BorrowerDocumentRepository extends JpaRepository<BorrowerDocument, UUID> {

    List<BorrowerDocument> findByBorrowerId(UUID borrowerId);
}
