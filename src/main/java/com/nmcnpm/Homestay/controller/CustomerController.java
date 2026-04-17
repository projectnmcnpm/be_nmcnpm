package com.nmcnpm.Homestay.controller;

import com.nmcnpm.Homestay.dto.request.CreateCustomerRequest;
import com.nmcnpm.Homestay.dto.request.UpdateCustomerRequest;
import com.nmcnpm.Homestay.dto.response.ApiResponse;
import com.nmcnpm.Homestay.dto.response.CustomerResponse;
import com.nmcnpm.Homestay.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Customer endpoints:
 *
 *   GET    /api/customers          manager, receptionist
 *   POST   /api/customers          manager, receptionist
 *   PATCH  /api/customers/{id}     manager, receptionist
 *   DELETE /api/customers/{id}     manager
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    // ------------------------------------------------------------------
    // GET /api/customers
    // ------------------------------------------------------------------
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'RECEPTIONIST')")
    public ApiResponse<List<CustomerResponse>> getAllCustomers() {
        return ApiResponse.success(customerService.getAllCustomers());
    }

    // ------------------------------------------------------------------
    // POST /api/customers
    // ------------------------------------------------------------------
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MANAGER', 'RECEPTIONIST')")
    public ApiResponse<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request
    ) {
        return ApiResponse.success(
                customerService.createCustomer(request),
                "Customer created successfully"
        );
    }

    // ------------------------------------------------------------------
    // PATCH /api/customers/{id}
    // ------------------------------------------------------------------
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'RECEPTIONIST')")
    public ApiResponse<CustomerResponse> updateCustomer(
            @PathVariable String id,
            @Valid @RequestBody UpdateCustomerRequest request
    ) {
        return ApiResponse.success(customerService.updateCustomer(id, request));
    }

    // ------------------------------------------------------------------
    // DELETE /api/customers/{id}  -> 204 No Content
    // ------------------------------------------------------------------
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('MANAGER')")
    public void deleteCustomer(@PathVariable String id) {
        customerService.deleteCustomer(id);
    }
}