package com.nmcnpm.Homestay.service;

import com.nmcnpm.Homestay.dto.request.CreateCustomerRequest;
import com.nmcnpm.Homestay.dto.request.UpdateCustomerRequest;
import com.nmcnpm.Homestay.dto.response.CustomerResponse;
import com.nmcnpm.Homestay.entity.Customer;
import com.nmcnpm.Homestay.exception.AppException;
import com.nmcnpm.Homestay.exception.ErrorCode;
import com.nmcnpm.Homestay.mapper.CustomerMapper;
import com.nmcnpm.Homestay.repository.BookingRepository;
import com.nmcnpm.Homestay.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final BookingRepository  bookingRepository;
    private final CustomerMapper     customerMapper;

    private static final DateTimeFormatter LABEL_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // -------------------------------------------------------------------------
    // GET /api/customers
    // -------------------------------------------------------------------------
    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll()
                .stream()
                .map(customer -> {
                    if (customer.getEmail() == null || customer.getEmail().isBlank()) {
                        String fallbackEmail = bookingRepository
                                .findRecentUserIdentifiersByCustomerId(
                                        customer.getId(),
                                        PageRequest.of(0, 1)
                                )
                                .stream()
                                .findFirst()
                                .orElse(null);

                        if (fallbackEmail != null && fallbackEmail.contains("@")) {
                            customer.setEmail(fallbackEmail);
                        }
                    }
                    return customerMapper.toResponse(customer);
                })
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // POST /api/customers
    // -------------------------------------------------------------------------
    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest req) {
        // Kiểm tra unique phone / cccd / email
        if (customerRepository.existsByPhone(req.getPhone())) {
            throw new AppException(ErrorCode.DUPLICATE_PHONE);
        }
        if (customerRepository.existsByCccd(req.getCccd())) {
            throw new AppException(ErrorCode.DUPLICATE_CCCD);
        }
        if (req.getEmail() != null && !req.getEmail().isBlank()
                && customerRepository.existsByEmail(req.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        OffsetDateTime lastVisit = parseOffsetDateTimeOrNull(req.getLastVisit());

        Customer customer = Customer.builder()
                .fullName(req.getName())
                .phone(req.getPhone())
                .cccd(req.getCccd())
                .email(req.getEmail())
                .lastVisitAt(lastVisit)
                .colorTag(req.getColor())
                .build();

        customer = customerRepository.save(customer);

        // Gán joined_label sau khi có created_at
        if (customer.getCreatedAt() != null) {
            customer.setJoinedLabel(customer.getCreatedAt().format(LABEL_FMT));
            customer = customerRepository.save(customer);
        }

        return customerMapper.toResponse(customer);
    }

    // -------------------------------------------------------------------------
    // PATCH /api/customers/{id}  — partial update
    // -------------------------------------------------------------------------
    @Transactional
    public CustomerResponse updateCustomer(String id, UpdateCustomerRequest req) {
        Customer customer = findById(id);

        if (req.getName() != null && !req.getName().isBlank()) {
            customer.setFullName(req.getName());
        }

        if (req.getPhone() != null && !req.getPhone().isBlank()) {
            if (!req.getPhone().equals(customer.getPhone())
                    && customerRepository.existsByPhone(req.getPhone())) {
                throw new AppException(ErrorCode.DUPLICATE_PHONE);
            }
            customer.setPhone(req.getPhone());
        }

        if (req.getCccd() != null && !req.getCccd().isBlank()) {
            if (!req.getCccd().equals(customer.getCccd())
                    && customerRepository.existsByCccd(req.getCccd())) {
                throw new AppException(ErrorCode.DUPLICATE_CCCD);
            }
            customer.setCccd(req.getCccd());
        }

        if (req.getEmail() != null) {
            if (!req.getEmail().isBlank()
                    && !req.getEmail().equals(customer.getEmail())
                    && customerRepository.existsByEmail(req.getEmail())) {
                throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            customer.setEmail(req.getEmail().isBlank() ? null : req.getEmail());
        }

        if (req.getLastVisit() != null) {
            customer.setLastVisitAt(parseOffsetDateTimeOrNull(req.getLastVisit()));
        }

        if (req.getColor() != null) {
            customer.setColorTag(req.getColor());
        }

        return customerMapper.toResponse(customerRepository.save(customer));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/customers/{id}  — soft delete
    // -------------------------------------------------------------------------
    @Transactional
    public void deleteCustomer(String id) {
        Customer customer = findById(id);
        customer.setDeletedAt(OffsetDateTime.now());
        customerRepository.save(customer);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Customer findById(String id) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.CUSTOMER_NOT_FOUND);
        }
        return customerRepository.findById(uuid)
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));
    }

    private OffsetDateTime parseOffsetDateTimeOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return OffsetDateTime.parse(s);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}