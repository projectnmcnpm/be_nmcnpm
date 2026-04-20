package com.nmcnpm.Homestay.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // Auth
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "Email hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED),
    EMAIL_ALREADY_EXISTS("EMAIL_ALREADY_EXISTS", "Email đã tồn tại", HttpStatus.CONFLICT),
    ACCOUNT_DISABLED("ACCOUNT_DISABLED", "Tài khoản đã bị vô hiệu hóa", HttpStatus.FORBIDDEN),

    // Resource
    ACCOUNT_NOT_FOUND("ACCOUNT_NOT_FOUND", "Không tìm thấy tài khoản", HttpStatus.NOT_FOUND),
    ROOM_NOT_FOUND("ROOM_NOT_FOUND", "Không tìm thấy phòng", HttpStatus.NOT_FOUND),
    BOOKING_NOT_FOUND("BOOKING_NOT_FOUND", "Không tìm thấy đơn đặt phòng", HttpStatus.NOT_FOUND),
    CUSTOMER_NOT_FOUND("CUSTOMER_NOT_FOUND", "Không tìm thấy khách hàng", HttpStatus.NOT_FOUND),

    // Booking
    ROOM_NOT_AVAILABLE("ROOM_NOT_AVAILABLE", "Phòng không khả dụng trong khoảng thời gian đã chọn", HttpStatus.CONFLICT),
    BOOKING_CONFLICT("BOOKING_CONFLICT", "Phòng đã có người đặt trong khoảng thời gian này", HttpStatus.CONFLICT),
    INVALID_DATE_RANGE("INVALID_DATE_RANGE", "Ngày trả phòng phải sau ngày nhận phòng", HttpStatus.BAD_REQUEST),
    BOOKING_START_IN_PAST("BOOKING_START_IN_PAST", "Ngày/giờ nhận phòng không được ở trong quá khứ", HttpStatus.BAD_REQUEST),
    INVALID_STATUS_TRANSITION("INVALID_STATUS_TRANSITION", "Chuyển trạng thái không hợp lệ", HttpStatus.UNPROCESSABLE_ENTITY),
    INVALID_ROOM_CAPACITY("INVALID_ROOM_CAPACITY", "Sức chứa không hợp lệ với loại phòng", HttpStatus.BAD_REQUEST),
    INVALID_PAYMENT_STATUS("INVALID_PAYMENT_STATUS", "Trạng thái thanh toán không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_REFUND_STATUS("INVALID_REFUND_STATUS", "Trạng thái hoàn tiền không hợp lệ", HttpStatus.BAD_REQUEST),
    REFUND_NOT_ELIGIBLE("REFUND_NOT_ELIGIBLE", "Đơn đặt phòng không đủ điều kiện hoàn tiền theo chính sách", HttpStatus.BAD_REQUEST),

    // Payment QR
    PAYMENT_QR_EXPIRED("PAYMENT_QR_EXPIRED",
            "Mã QR đã hết hạn. Vui lòng làm mới để nhận mã mới.",
            HttpStatus.GONE),
    PAYMENT_QR_NOT_SUPPORTED("PAYMENT_QR_NOT_SUPPORTED",
            "Mã QR chỉ hỗ trợ cho phương thức chuyển khoản ngân hàng",
            HttpStatus.BAD_REQUEST),
    TOO_MANY_PENDING_BOOKINGS("TOO_MANY_PENDING_BOOKINGS",
            "Bạn có quá nhiều đơn chưa thanh toán. Vui lòng hoàn tất hoặc hủy bớt đơn hiện tại.",
            HttpStatus.TOO_MANY_REQUESTS),

    // Customer
    DUPLICATE_PHONE("DUPLICATE_PHONE", "Số điện thoại đã tồn tại", HttpStatus.CONFLICT),
    DUPLICATE_CCCD("DUPLICATE_CCCD", "CCCD đã tồn tại", HttpStatus.CONFLICT),

    // General
    FORBIDDEN("FORBIDDEN", "Bạn không có quyền thực hiện thao tác này", HttpStatus.FORBIDDEN),
    INTERNAL_ERROR("INTERNAL_ERROR", "Lỗi hệ thống", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}