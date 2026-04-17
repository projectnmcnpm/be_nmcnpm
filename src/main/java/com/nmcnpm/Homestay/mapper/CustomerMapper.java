package com.nmcnpm.Homestay.mapper;

import com.nmcnpm.Homestay.dto.response.CustomerResponse;
import com.nmcnpm.Homestay.entity.Customer;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Mapper thủ công Customer entity -> CustomerResponse DTO.
 *
 * Mapping spec:
 *   customers.id            -> customer.id   (UUID -> String)
 *   customers.full_name     -> customer.name
 *   customers.phone         -> customer.phone
 *   customers.cccd          -> customer.cccd
 *   customers.email         -> customer.email
 *   customers.booking_count -> customer.bookings
 *   customers.joined_label hoặc format(created_at) -> customer.created ("dd/MM/yyyy")
 *   customers.last_visit_at -> customer.lastVisit   (ISO-8601)
 *   customers.color_tag     -> customer.color
 */
@Component
public class CustomerMapper {

    private static final DateTimeFormatter LABEL_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public CustomerResponse toResponse(Customer customer) {
        if (customer == null) return null;

        // Ưu tiên joined_label, fallback format created_at
        String created = customer.getJoinedLabel();
        if ((created == null || created.isBlank()) && customer.getCreatedAt() != null) {
            created = customer.getCreatedAt().format(LABEL_FMT);
        }

        // last_visit_at -> ISO-8601 string
        String lastVisit = customer.getLastVisitAt() != null
                ? customer.getLastVisitAt().toString()
                : null;

        return CustomerResponse.builder()
                .id(customer.getId() != null ? customer.getId().toString() : null)
                .name(customer.getFullName())
                .phone(customer.getPhone())
                .cccd(customer.getCccd())
                .email(customer.getEmail())
                .bookings(customer.getBookingCount())
                .created(created)
                .lastVisit(lastVisit)
                .color(customer.getColorTag())
                .build();
    }
}