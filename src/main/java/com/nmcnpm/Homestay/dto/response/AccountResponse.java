package com.nmcnpm.Homestay.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO trả về cho frontend — tuân theo Account DTO contract:
 * {
 *   "id": "USR-001",
 *   "email": "manager@genz.com",
 *   "name": "Quan Ly",
 *   "role": "manager",
 *   "created": "09/04/2026",
 *   "status": "active"
 * }
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountResponse {
    String id;
    String email;
    String name;
    String role;     // lowercase: manager / receptionist / cleaner / customer
    String created;  // "dd/MM/yyyy"
    String status;   // lowercase: active / disabled
}