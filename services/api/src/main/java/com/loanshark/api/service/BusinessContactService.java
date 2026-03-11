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
            return new ApiDtos.LenderContactResponse("", "", "", "");
        }

        return new ApiDtos.LenderContactResponse(
            contact.getBusinessName(),
            contact.getPhone(),
            contact.getEmail(),
            contact.getAddress()
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

        contact = businessContactRepository.save(contact);

        return new ApiDtos.LenderContactResponse(
            contact.getBusinessName(),
            contact.getPhone(),
            contact.getEmail(),
            contact.getAddress()
        );
    }
}
