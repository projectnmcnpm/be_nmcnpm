package com.nmcnpm.Homestay.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * DTO trả về cho GET /api/dashboard/staff
 * Dành cho receptionist + manager ở màn hình staff dashboard.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardStaffResponse {
    long upcomingCount;
    long activeCount;
    long availableRooms;
    long cleaningRooms;

    // Danh sách ngắn upcoming + active (tối đa 10 bản ghi mỗi loại)
    List<BookingResponse> upcomingBookings;
    List<BookingResponse> activeBookings;
}