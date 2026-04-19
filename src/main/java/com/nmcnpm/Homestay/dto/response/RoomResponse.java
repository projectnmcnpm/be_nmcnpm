package com.nmcnpm.Homestay.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO trả về cho frontend — tuân theo Room DTO contract trong spec.
 * {
 *   "id": "RM-101", "name": "...", "type": "...",
 *   "price": 650000, "pricePerHour": 150000,
 *   "status": "available", "image": "...",
 *   "gallery": [...], "amenities": [...], "description": "..."
 * }
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomResponse {

    String id;
    String name;
    String type;
    Integer capacity;
    BigDecimal price;
    BigDecimal pricePerHour;
    String status;       // lowercase: available / few_left / full / cleaning
    String image;
    List<String> gallery;
    List<String> amenities;
    String description;
}