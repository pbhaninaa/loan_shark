package com.loanshark.api.service;

import com.loanshark.api.dto.ApiDtos;
import com.loanshark.api.entity.BusinessContact;
import com.loanshark.api.repository.BusinessContactRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessContactService {

    private final BusinessContactRepository businessContactRepository;

    public BusinessContactService(BusinessContactRepository businessContactRepository) {
        this.businessContactRepository = businessContactRepository;
    }

    public ApiDtos.LenderContactResponse get() {
        BusinessContact contact = businessContactRepository.findFirstByOrderByCreatedAtAsc()
            .orElse(null);

        if (contact == null) {
            return new ApiDtos.LenderContactResponse(
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "");
        }

        return new ApiDtos.LenderContactResponse(
                contact.getBusinessName(),
                contact.getPhone(),
                contact.getEmail(),
                contact.getAddress(),
                contact.getAccountNumber(),
                contact.getBankName(),
                contact.getAccountHolderName(),
                contact.getAccountType(),
                contact.getBranchCode(),
                contact.getPaymentReference()
        );
    }

    @Transactional
    public ApiDtos.LenderContactResponse createOrUpdate(ApiDtos.BusinessContactUpdateRequest request) {
        BusinessContact contact = businessContactRepository.findFirstByOrderByCreatedAtAsc()
            .orElse(new BusinessContact());

        contact.setBusinessName(request.businessName());
        contact.setPhone(request.phone());
        contact.setEmail(request.email());
        contact.setAddress(request.address());
        contact.setAccountNumber(request.accountNumber());
        contact.setBankName(request.bankName());
        contact.setAccountHolderName(request.accountHolderName());
        contact.setAccountType(request.accountType());
        contact.setBranchCode(request.branchCode());
        contact.setPaymentReference(request.paymentReference());
        contact = businessContactRepository.save(contact);

        return new ApiDtos.LenderContactResponse(
                contact.getBusinessName(),
                contact.getPhone(),
                contact.getEmail(),
                contact.getAddress(),
                contact.getAccountNumber()     ,
                contact.getBankName(),
                contact.getAccountHolderName(),
                contact.getAccountType(),
                contact.getBranchCode(),
                contact.getPaymentReference()

        );
    }
}
