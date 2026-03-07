package com.loanshark.api.repository;

import com.loanshark.api.entity.BorrowerDocument;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BorrowerDocumentRepository extends JpaRepository<BorrowerDocument, Long> {

    List<BorrowerDocument> findByBorrowerId(Long borrowerId);
}
