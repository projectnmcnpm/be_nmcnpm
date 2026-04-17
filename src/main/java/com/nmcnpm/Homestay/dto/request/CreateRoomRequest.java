package com.nmcnpm.Homestay.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

/**
 * Body cho POST /api/rooms  (multipart/form-data hoặc JSON)
 */
@Getter
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class CreateRoomRequest {

    @NotBlank(message = "Room name is required")
    String name;

    @NotBlank(message = "Room type is required")
    String type;   // Single Room / Twin Room / Double Room / VIP Room

    Integer capacity;

    @NotNull(message = "Price per night is required")
    @DecimalMin(value = "0", message = "Price must be >= 0")
    BigDecimal pricePerNight;

    @DecimalMin(value = "0", message = "Price per hour must be >= 0")
    BigDecimal pricePerHour;

    // status mặc định "available" nếu không truyền
    String status;

    // URL ảnh cover — có thể truyền URL sẵn hoặc để trống rồi upload sau
    String coverImageUrl;

    List<String> galleryUrls;

    List<String> amenities;

    String description;
}