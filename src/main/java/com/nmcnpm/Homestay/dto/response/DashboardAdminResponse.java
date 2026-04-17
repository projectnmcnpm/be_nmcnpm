package com.nmcnpm.Homestay.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO trả về cho GET /api/dashboard/admin
 * {
 *   "totalRevenue": 123000000,
 *   "totalBookings": 523,
 *   "availableRooms": 21,
 *   "activeRooms": 14,
 *   "cleaningRooms": 3,
 *   "occupancyRate": 37,
 *   "newCustomers": 12,
 *   "recentBookings": [...]
 * }
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardAdminResponse {
    BigDecimal totalRevenue;
    long totalBookings;
    long availableRooms;
    long activeRooms;      // few_left + full
    long cleaningRooms;
    int occupancyRate;     // phần trăm phòng đang dùng (active / total * 100)
    long newCustomers;     // tạo trong 30 ngày gần nhất
    List<BookingResponse> recentBookings;
}