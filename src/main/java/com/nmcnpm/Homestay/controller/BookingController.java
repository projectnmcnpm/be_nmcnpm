package com.nmcnpm.Homestay.controller;

import com.nmcnpm.Homestay.dto.request.CancelBookingRequest;
import com.nmcnpm.Homestay.dto.request.CreateBookingRequest;
import com.nmcnpm.Homestay.dto.request.UpdateBookingPaymentStatusRequest;
import com.nmcnpm.Homestay.dto.request.UpdateBookingRequest;
import com.nmcnpm.Homestay.dto.request.UpdateBookingRefundStatusRequest;
import com.nmcnpm.Homestay.dto.request.UpdateBookingStatusRequest;
import com.nmcnpm.Homestay.dto.response.ApiResponse;
import com.nmcnpm.Homestay.dto.response.BookingResponse;
import com.nmcnpm.Homestay.dto.response.PagedResponse;
import com.nmcnpm.Homestay.dto.response.PaymentQrResponse;
import com.nmcnpm.Homestay.entity.Booking;
import com.nmcnpm.Homestay.service.BookingExportService;
import com.nmcnpm.Homestay.service.BookingService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * Booking endpoints:
 *
 *   GET    /api/bookings                             manager/receptionist/customer
 *   GET    /api/bookings/{id}                        manager/receptionist/customer(owner)
 *   GET    /api/bookings/{id}/payment-qr             manager/receptionist/customer(owner)
 *   GET    /api/bookings/export                      manager/receptionist
 *   POST   /api/bookings                             manager/receptionist/customer
 *   PATCH  /api/bookings/{id}/status                 manager/receptionist
 *   PATCH  /api/bookings/{id}/cancel                 manager/receptionist/customer(owner)
 *   DELETE /api/bookings/{id}                        manager/receptionist
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService       bookingService;
    private final BookingExportService bookingExportService;

    // ------------------------------------------------------------------
    // GET /api/bookings
    // ------------------------------------------------------------------
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','RECEPTIONIST','CUSTOMER')")
    public ApiResponse<PagedResponse<BookingResponse>> getAllBookings(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String roomId,
            @RequestParam(required = false) String checkInFrom,
            @RequestParam(required = false) String checkInTo,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(
                bookingService.getAllBookings(userId, status, roomId, checkInFrom, checkInTo, page, size)
        );
    }

    // ------------------------------------------------------------------
    // GET /api/bookings/export  — trả về file CSV
    // QUAN TRỌNG: phải khai báo TRƯỚC /{id} để Spring không bắt nhầm
    // ------------------------------------------------------------------
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('MANAGER','RECEPTIONIST')")
    public void exportBookings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            HttpServletResponse response
    ) throws IOException {
        List<Booking> bookings = bookingService.getBookingsForExport(status, from, to);
        bookingExportService.exportToCsv(bookings, response);
    }

    // ------------------------------------------------------------------
    // GET /api/bookings/{id}
    // ------------------------------------------------------------------
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','RECEPTIONIST','CUSTOMER')")
    public ApiResponse<BookingResponse> getBookingById(@PathVariable String id) {
        return ApiResponse.success(bookingService.getBookingById(id));
    }

    // ------------------------------------------------------------------
    // GET /api/bookings/{id}/payment-qr
    // Trả về thông tin QR payment (semi-dynamic) cho booking.
    // Chỉ hỗ trợ booking có paymentMethod = bank.
    // Nếu QR hết hạn (> 5 phút), tự động gia hạn thêm 5 phút.
    // ------------------------------------------------------------------
    @GetMapping("/{id}/payment-qr")
    @PreAuthorize("hasAnyRole('MANAGER','RECEPTIONIST','CUSTOMER')")
    public ApiResponse<PaymentQrResponse> getPaymentQr(@PathVariable String id) {
        return ApiResponse.success(bookingService.getPaymentQr(id));
    }

    // ------------------------------------------------------------------
    // POST /api/bookings
    // ------------------------------------------------------------------
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MANAGER','RECEPTIONIST','CUSTOMER')")
    public ApiResponse<BookingResponse> createBooking(
            @Valid @RequestBody CreateBookingRequest request
    ) {
        return ApiResponse.success(bookingService.createBooking(request), "Booking created successfully");
    }

    // ------------------------------------------------------------------
    // PATCH /api/bookings/{id}
    // ------------------------------------------------------------------
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','RECEPTIONIST')")
    public ApiResponse<BookingResponse> updateBooking(
            @PathVariable String id,
            @Valid @RequestBody UpdateBookingRequest request
    ) {
        return ApiResponse.success(bookingService.updateBooking(id, request));
    }

    // ------------------------------------------------------------------
    // PATCH /api/bookings/{id}/status
    // ------------------------------------------------------------------
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('MANAGER','RECEPTIONIST')")
    public ApiResponse<BookingResponse> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateBookingStatusRequest request
    ) {
        return ApiResponse.success(bookingService.updateStatus(id, request));
    }

    // ------------------------------------------------------------------
    // PATCH /api/bookings/{id}/payment-status
    // ------------------------------------------------------------------
    @PatchMapping("/{id}/payment-status")
    @PreAuthorize("hasAnyRole('MANAGER','RECEPTIONIST')")
    public ApiResponse<BookingResponse> updatePaymentStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateBookingPaymentStatusRequest request
    ) {
        return ApiResponse.success(bookingService.updatePaymentStatus(id, request));
    }

    // ------------------------------------------------------------------
    // PATCH /api/bookings/{id}/refund-status
    // ------------------------------------------------------------------
    @PatchMapping("/{id}/refund-status")
    @PreAuthorize("hasAnyRole('MANAGER','RECEPTIONIST')")
    public ApiResponse<BookingResponse> updateRefundStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateBookingRefundStatusRequest request
    ) {
        return ApiResponse.success(bookingService.updateRefundStatus(id, request));
    }

    // ------------------------------------------------------------------
    // PATCH /api/bookings/{id}/cancel
    // ------------------------------------------------------------------
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('MANAGER','RECEPTIONIST','CUSTOMER')")
    public ApiResponse<BookingResponse> cancelBooking(
            @PathVariable String id,
            @RequestBody(required = false) CancelBookingRequest request
    ) {
        return ApiResponse.success(
                bookingService.cancelBooking(id, request != null ? request : new CancelBookingRequest())
        );
    }

    // ------------------------------------------------------------------
    // DELETE /api/bookings/{id}  -> 204 No Content
    // ------------------------------------------------------------------
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('MANAGER','RECEPTIONIST')")
    public void deleteBooking(@PathVariable String id) {
        bookingService.deleteBooking(id);
    }
}