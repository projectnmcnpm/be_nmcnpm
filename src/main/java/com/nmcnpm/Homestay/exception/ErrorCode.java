package com.nmcnpm.Homestay.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // Auth
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "Email or password is incorrect", HttpStatus.UNAUTHORIZED),
    EMAIL_ALREADY_EXISTS("EMAIL_ALREADY_EXISTS", "Email already exists", HttpStatus.CONFLICT),
    ACCOUNT_DISABLED("ACCOUNT_DISABLED", "Account is disabled", HttpStatus.FORBIDDEN),

    // Resource
    ACCOUNT_NOT_FOUND("ACCOUNT_NOT_FOUND", "Account not found", HttpStatus.NOT_FOUND),
    ROOM_NOT_FOUND("ROOM_NOT_FOUND", "Room not found", HttpStatus.NOT_FOUND),
    BOOKING_NOT_FOUND("BOOKING_NOT_FOUND", "Booking not found", HttpStatus.NOT_FOUND),
    CUSTOMER_NOT_FOUND("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND),

    // Booking
    ROOM_NOT_AVAILABLE("ROOM_NOT_AVAILABLE", "Room is not available for the selected dates", HttpStatus.CONFLICT),
    BOOKING_CONFLICT("BOOKING_CONFLICT", "Room already booked for this period", HttpStatus.CONFLICT),
    INVALID_DATE_RANGE("INVALID_DATE_RANGE", "Check-out must be after check-in", HttpStatus.BAD_REQUEST),
    INVALID_STATUS_TRANSITION("INVALID_STATUS_TRANSITION", "Invalid status transition", HttpStatus.UNPROCESSABLE_ENTITY),

    // Payment QR
    PAYMENT_QR_EXPIRED("PAYMENT_QR_EXPIRED",
            "Payment QR has expired. Please refresh to get a new QR code.",
            HttpStatus.GONE),
    PAYMENT_QR_NOT_SUPPORTED("PAYMENT_QR_NOT_SUPPORTED",
            "Payment QR is only available for bank transfer method",
            HttpStatus.BAD_REQUEST),
    TOO_MANY_PENDING_BOOKINGS("TOO_MANY_PENDING_BOOKINGS",
            "You have too many pending unpaid bookings. Please complete or cancel existing bookings first.",
            HttpStatus.TOO_MANY_REQUESTS),

    // Customer
    DUPLICATE_PHONE("DUPLICATE_PHONE", "Phone number already exists", HttpStatus.CONFLICT),
    DUPLICATE_CCCD("DUPLICATE_CCCD", "CCCD already exists", HttpStatus.CONFLICT),

    // General
    FORBIDDEN("FORBIDDEN", "You don't have permission", HttpStatus.FORBIDDEN),
    INTERNAL_ERROR("INTERNAL_ERROR", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}