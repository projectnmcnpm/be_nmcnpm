package com.nmcnpm.Homestay.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

/**
 * Body cho PUT /api/rooms/{id}  (multipart/form-data)
 * Dùng để cập nhật thông tin phòng hiện có
 */
@Getter
@Setter
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class UpdateRoomRequest {

    @NotBlank(message = "Room name is required")
    String name;

    @NotBlank(message = "Room type is required")
    String type;

    Integer capacity;

    @NotNull(message = "Price per night is required")
    @DecimalMin(value = "0", message = "Price must be >= 0")
    BigDecimal pricePerNight;

    @DecimalMin(value = "0", message = "Price per hour must be >= 0")
    BigDecimal pricePerHour;

    String status;

    List<String> amenities;

    String description;
}
