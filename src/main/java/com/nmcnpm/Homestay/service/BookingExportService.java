package com.nmcnpm.Homestay.service;

import com.nmcnpm.Homestay.entity.Booking;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service xuất danh sách booking ra file CSV.
 * Endpoint: GET /api/bookings/export?status=&from=&to=
 */
@Service
public class BookingExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Xuất danh sách booking ra CSV và ghi vào HttpServletResponse.
     */
    public void exportToCsv(List<Booking> bookings, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"bookings_export.csv\"");

        try (
                OutputStreamWriter osw = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
                BufferedWriter bw = new BufferedWriter(osw);
                PrintWriter writer = new PrintWriter(bw)
        ) {
            // BOM để Excel đọc đúng UTF-8
            writer.write('\uFEFF');

            // Header
            writer.println(csvRow(
                    "Booking ID",
                    "Room Name",
                    "Customer Name",
                    "Customer Phone",
                    "Customer CCCD",
                    "Check In",
                    "Check Out",
                    "Check In Time",
                    "Check Out Time",
                    "Booking Type",
                    "Total Amount (VND)",
                    "Pay Amount VND",
                    "Payment Percent",
                    "Payment Method",
                    "Transfer Content",
                    "Status",
                    "Cancel Reason",
                    "Created At"
            ));

            for (Booking b : bookings) {
                writer.println(csvRow(
                        safeStr(b.getId()),
                        safeStr(b.getRoomNameSnapshot()),
                        safeStr(b.getCustomerNameSnapshot()),
                        safeStr(b.getCustomerPhone()),
                        safeStr(b.getCustomerIdNumber()),
                        b.getCheckInDate() != null ? b.getCheckInDate().format(DATE_FMT) : "",
                        b.getCheckOutDate() != null ? b.getCheckOutDate().format(DATE_FMT) : "",
                        b.getCheckInTime() != null ? b.getCheckInTime().format(TIME_FMT) : "",
                        b.getCheckOutTime() != null ? b.getCheckOutTime().format(TIME_FMT) : "",
                        b.getBookingType() != null ? b.getBookingType().name().toLowerCase() : "",
                        b.getTotalAmount() != null ? b.getTotalAmount().toPlainString() : "0",
                        b.getPayAmountVnd() != null ? b.getPayAmountVnd().toPlainString() : "",
                        b.getPaymentPercent() != null ? String.valueOf(b.getPaymentPercent()) : "",
                        b.getPaymentMethod() != null ? b.getPaymentMethod().name().toLowerCase() : "",
                        safeStr(b.getTransferContent()),
                        b.getStatus() != null ? b.getStatus().name().toLowerCase() : "",
                        safeStr(b.getCancelReason()),
                        b.getCreatedAt() != null ? b.getCreatedAt().toString() : ""
                ));
            }

            writer.flush();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Escape CSV: bao quanh bằng dấu nháy kép, escape nháy kép trong nội dung. */
    private String escapeCsv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String csvRow(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(escapeCsv(fields[i]));
        }
        return sb.toString();
    }

    private String safeStr(Object obj) {
        return obj != null ? obj.toString() : "";
    }
}