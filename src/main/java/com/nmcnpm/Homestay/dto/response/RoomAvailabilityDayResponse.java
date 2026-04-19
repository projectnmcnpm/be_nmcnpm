package com.nmcnpm.Homestay.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomAvailabilityDayResponse {
    String date;                // yyyy-MM-dd
    boolean booked;             // true nếu có lịch đặt trong ngày
    List<String> bookedRanges;  // Ví dụ: ["10:00 - 10:20", "14:00 - 15:30"]
    String availableFrom;       // Ví dụ: "10:40" (đã cộng 20 phút dọn dẹp)
    String note;                // "Trống cả ngày" | "Đã đặt"
}
