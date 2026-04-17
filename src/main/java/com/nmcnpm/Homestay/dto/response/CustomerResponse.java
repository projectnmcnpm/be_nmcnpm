package com.nmcnpm.Homestay.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO trả về cho frontend — tuân theo Customer DTO contract:
 * {
 *   "id": "CS-001",
 *   "name": "Nguyen Van A",
 *   "phone": "0901234567",
 *   "cccd": "001090123456",
 *   "email": "a@gmail.com",
 *   "bookings": 5,
 *   "created": "09/04/2026",
 *   "lastVisit": "2026-04-01T10:00:00Z",
 *   "color": "bg-blue-500"
 * }
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerResponse {
    String id;
    String name;
    String phone;
    String cccd;
    String email;
    Integer bookings;   // booking_count
    String created;     // "dd/MM/yyyy"
    String lastVisit;   // ISO-8601 UTC
    String color;       // Tailwind class
}