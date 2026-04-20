package com.nmcnpm.Homestay.service;

import com.nmcnpm.Homestay.dto.request.CancelBookingRequest;
import com.nmcnpm.Homestay.dto.request.CreateBookingRequest;
import com.nmcnpm.Homestay.dto.request.UpdateBookingPaymentStatusRequest;
import com.nmcnpm.Homestay.dto.request.UpdateBookingRequest;
import com.nmcnpm.Homestay.dto.request.UpdateBookingRefundStatusRequest;
import com.nmcnpm.Homestay.dto.request.UpdateBookingStatusRequest;
import com.nmcnpm.Homestay.dto.response.BookingResponse;
import com.nmcnpm.Homestay.dto.response.PagedResponse;
import com.nmcnpm.Homestay.dto.response.PaymentQrResponse;
import com.nmcnpm.Homestay.entity.Booking;
import com.nmcnpm.Homestay.entity.Customer;
import com.nmcnpm.Homestay.entity.Room;
import com.nmcnpm.Homestay.enums.BookingStatus;
import com.nmcnpm.Homestay.enums.BookingType;
import com.nmcnpm.Homestay.enums.PaymentMethod;
import com.nmcnpm.Homestay.enums.PaymentStatus;
import com.nmcnpm.Homestay.enums.RefundStatus;
import com.nmcnpm.Homestay.enums.RoomStatus;
import com.nmcnpm.Homestay.exception.AppException;
import com.nmcnpm.Homestay.exception.ErrorCode;
import com.nmcnpm.Homestay.mapper.BookingMapper;
import com.nmcnpm.Homestay.repository.BookingRepository;
import com.nmcnpm.Homestay.repository.CustomerRepository;
import com.nmcnpm.Homestay.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final String REFUND_STATUS_PREFIX = "REFUND_STATUS=";

    private final BookingRepository  bookingRepository;
    private final RoomRepository     roomRepository;
    private final CustomerRepository customerRepository;
    private final BookingMapper      bookingMapper;
    private final PaymentQrService   paymentQrService;

    // -------------------------------------------------------------------------
    // GET /api/bookings
    // -------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public PagedResponse<BookingResponse> getAllBookings(
            String userId, String status,
            String roomId, String checkInFrom, String checkInTo,
            int page, int size) {

        String currentEmail = currentEmail();
        String currentRole  = currentRole();

        String effectiveUserId;
        boolean onlyConfirmedPayment;
        if ("CUSTOMER".equalsIgnoreCase(currentRole)) {
            effectiveUserId = currentEmail;
            onlyConfirmedPayment = true;
        } else {
            effectiveUserId = (userId != null && !userId.isBlank()) ? userId : null;
            onlyConfirmedPayment = false;
        }

        BookingStatus bookingStatus = parseStatusOrNull(status);
        UUID          roomUUID      = parseUuidOrNull(roomId);
        LocalDate     from          = parseDateOrNull(checkInFrom);
        LocalDate     to            = parseDateOrNull(checkInTo);

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Booking> bPage = bookingRepository.findAllByFilter(
                effectiveUserId, onlyConfirmedPayment, bookingStatus, roomUUID, from, to, pageable);

        List<BookingResponse> content = bPage.getContent()
                .stream().map(bookingMapper::toResponse).collect(Collectors.toList());

        return PagedResponse.of(bPage, content);
    }

    // -------------------------------------------------------------------------
    // GET /api/bookings/{id}
    // -------------------------------------------------------------------------
    public BookingResponse getBookingById(String id) {
        Booking booking = findById(id);
        assertOwnerOrStaff(booking);
        return bookingMapper.toResponse(booking);
    }

    // -------------------------------------------------------------------------
    // POST /api/bookings  — tạo booking + sinh QR nếu paymentMethod = bank
    // -------------------------------------------------------------------------
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest req) {

        // 1. Validate room
        UUID roomUUID = parseUuidOrThrow(req.getRoomId(), ErrorCode.ROOM_NOT_FOUND);
        Room room = roomRepository.findById(roomUUID)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new AppException(
                    ErrorCode.ROOM_NOT_AVAILABLE,
                    "Phòng đang bảo trì, tạm thời không thể đặt phòng"
            );
        }

        // 2. Parse ngày
        LocalDate checkIn  = parseDateOrThrow(req.getCheckIn(),  "checkIn");
        LocalDate checkOut = parseDateOrThrow(req.getCheckOut(), "checkOut");
        BookingType bookingType = parseBookingTypeOrDefault(req.getBookingType());

        LocalTime checkInTime = bookingType == BookingType.HOUR
                ? parseTimeOrThrow(req.getCheckInTime(), "checkInTime")
                : null;
        LocalTime checkOutTime = bookingType == BookingType.HOUR
                ? parseTimeOrThrow(req.getCheckOutTime(), "checkOutTime")
                : null;

        validateBookingTiming(checkIn, checkOut, checkInTime, checkOutTime, bookingType);

        // 3. Check conflict
        if (bookingRepository.hasConflict(roomUUID, checkIn, checkOut)) {
            throw new AppException(ErrorCode.BOOKING_CONFLICT);
        }

        // 4. Tìm customer
        Customer customer = resolveCustomer(req);

        // 5. Xây dựng entity

        Integer requestedPaymentPercent = null;
        if (req.getPaymentAmount() != null) {
            try {
                int p = Integer.parseInt(req.getPaymentAmount());
                if (p == 30 || p == 100) requestedPaymentPercent = p;
            } catch (NumberFormatException ignored) {}
        }

        // 6. Tính QR payment fields nếu là bank
        boolean isBank = paymentQrService.requiresQr(req.getPaymentMethod());
        BigDecimal payAmountVnd = null;
        String transferContent  = null;
        OffsetDateTime expiresAt = null;

        if (isBank && requestedPaymentPercent != null) {
            payAmountVnd   = paymentQrService.calcPayAmountVnd(req.getTotal(), requestedPaymentPercent);
            transferContent = paymentQrService.buildTransferContent(
                    req.getRoomName() != null ? req.getRoomName() : room.getRoomName());
            expiresAt      = paymentQrService.calcExpiresAt();
        }

        Booking booking = Booking.builder()
                .room(room)
                .customer(customer)
                .userIdentifier(resolveUserIdentifier(req))
                .roomNameSnapshot(req.getRoomName() != null ? req.getRoomName() : room.getRoomName())
                .roomImageSnapshot(req.getImage() != null ? req.getImage() : room.getCoverImageUrl())
                .customerNameSnapshot(customer != null ? customer.getFullName() : req.getCustomerName())
                .customerPhone(req.getCustomerPhone())
                .customerIdNumber(req.getCustomerIdNumber())
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .checkInTime(checkInTime)
                .checkOutTime(checkOutTime)
                .bookingType(bookingType)
                .totalAmount(req.getTotal())
                .status(BookingStatus.UPCOMING)
                .paymentMethod(parsePaymentMethodOrNull(req.getPaymentMethod()))
                .paymentPercent(0)
                .payAmountVnd(payAmountVnd)
                .transferContent(transferContent)
                .paymentExpiresAt(expiresAt)
                .note(req.getNote())
                .build();

        booking = bookingRepository.save(booking);

        // 7. Cập nhật booking_count
        if (customer != null) {
            customer.setBookingCount((int) bookingRepository.countByCustomerId(customer.getId()));
            customerRepository.save(customer);
        }

        return bookingMapper.toResponse(booking);
    }

    // -------------------------------------------------------------------------
    // PATCH /api/bookings/{id}  — cập nhật chi tiết booking
    // -------------------------------------------------------------------------
    @Transactional
    public BookingResponse updateBooking(String id, UpdateBookingRequest req) {
        Booking booking = findById(id);

        UUID roomUUID = parseUuidOrThrow(req.getRoomId(), ErrorCode.ROOM_NOT_FOUND);
        Room room = roomRepository.findById(roomUUID)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new AppException(
                    ErrorCode.ROOM_NOT_AVAILABLE,
                    "Phòng đang bảo trì, tạm thời không thể đặt phòng"
            );
        }

        LocalDate checkIn = parseDateOrThrow(req.getCheckIn(), "checkIn");
        LocalDate checkOut = parseDateOrThrow(req.getCheckOut(), "checkOut");
        BookingType bookingType = parseBookingTypeOrDefault(req.getBookingType());

        LocalTime checkInTime = bookingType == BookingType.HOUR
                ? parseTimeOrThrow(req.getCheckInTime(), "checkInTime")
                : null;
        LocalTime checkOutTime = bookingType == BookingType.HOUR
                ? parseTimeOrThrow(req.getCheckOutTime(), "checkOutTime")
                : null;

        validateBookingTiming(checkIn, checkOut, checkInTime, checkOutTime, bookingType);

        UUID bookingUUID = parseUuidOrThrow(id, ErrorCode.BOOKING_NOT_FOUND);
        if (bookingRepository.hasConflictExcludingBooking(bookingUUID, roomUUID, checkIn, checkOut)) {
            throw new AppException(ErrorCode.BOOKING_CONFLICT);
        }

        Customer customer = resolveCustomer(
                req.getCustomerPhone(),
                req.getCustomerIdNumber(),
                req.getCustomerName(),
                req.getCustomerEmail(),
                req.getUserId()
        );

        booking.setRoom(room);
        booking.setRoomNameSnapshot(req.getRoomName() != null ? req.getRoomName() : room.getRoomName());
        booking.setCustomer(customer);
        booking.setCustomerNameSnapshot(customer != null ? customer.getFullName() : req.getCustomerName());
        booking.setUserIdentifier(resolveUserIdentifier(req.getCustomerEmail(), req.getUserId()));
        booking.setCustomerPhone(req.getCustomerPhone());
        booking.setCustomerIdNumber(req.getCustomerIdNumber());
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);
        booking.setCheckInTime(checkInTime);
        booking.setCheckOutTime(checkOutTime);
        booking.setBookingType(bookingType);
        booking.setTotalAmount(req.getTotal());
        booking.setNote(req.getNote());

        Booking saved = bookingRepository.save(booking);
        return bookingMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // GET /api/bookings/{id}/payment-qr
    // -------------------------------------------------------------------------
    @Transactional
    public PaymentQrResponse getPaymentQr(String id) {
        Booking booking = findById(id);
        assertOwnerOrStaff(booking);

        if (booking.getPaymentMethod() != PaymentMethod.BANK) {
            throw new AppException(ErrorCode.INTERNAL_ERROR,
                    "Payment QR is only available for bank transfer bookings");
        }

        PaymentQrResponse qrResponse = paymentQrService.buildQrResponse(booking);

        // Cập nhật expiresAt vào DB nếu QR đã hết hạn và được gia hạn
        if (booking.getPaymentExpiresAt() == null
                || booking.getPaymentExpiresAt().isBefore(OffsetDateTime.now())) {
            booking.setPaymentExpiresAt(OffsetDateTime.now().plusMinutes(5));
            bookingRepository.save(booking);
        }

        return qrResponse;
    }

    // -------------------------------------------------------------------------
    // PATCH /api/bookings/{id}/status
    // -------------------------------------------------------------------------
    @Transactional
    public BookingResponse updateStatus(String id, UpdateBookingStatusRequest req) {
        Booking booking = findById(id);
        BookingStatus newStatus = parseStatusOrThrow(req.getStatus());
        validateStatusTransition(booking.getStatus(), newStatus);
        booking.setStatus(newStatus);
        Booking savedBooking = bookingRepository.save(booking);
        syncRoomStatusFromBooking(savedBooking, newStatus);
        return bookingMapper.toResponse(savedBooking);
    }

    // -------------------------------------------------------------------------
    // PATCH /api/bookings/{id}/payment-status
    // -------------------------------------------------------------------------
    @Transactional
    public BookingResponse updatePaymentStatus(String id, UpdateBookingPaymentStatusRequest req) {
        Booking booking = findById(id);

        if (normalizeBookingStatus(booking.getStatus()) == BookingStatus.CANCELLED) {
            throw new AppException(
                    ErrorCode.INVALID_PAYMENT_STATUS,
                    "Không thể cập nhật trạng thái thanh toán khi booking đã hủy"
            );
        }

        PaymentStatus paymentStatus = parsePaymentStatusOrThrow(req.getPaymentStatus());

        switch (paymentStatus) {
            case UNPAID -> {
                booking.setPaymentPercent(0);
                booking.setPayAmountVnd(BigDecimal.ZERO);
            }
            case DEPOSITED -> {
                booking.setPaymentPercent(30);
                if (booking.getTotalAmount() != null) {
                    booking.setPayAmountVnd(paymentQrService.calcPayAmountVnd(booking.getTotalAmount(), 30));
                }
            }
            case PAID -> {
                booking.setPaymentPercent(100);
                if (booking.getTotalAmount() != null) {
                    booking.setPayAmountVnd(paymentQrService.calcPayAmountVnd(booking.getTotalAmount(), 100));
                }
                booking.setPaymentExpiresAt(null);
            }
        }

        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/bookings/{id}/refund-status
    // -------------------------------------------------------------------------
    @Transactional
    public BookingResponse updateRefundStatus(String id, UpdateBookingRefundStatusRequest req) {
        Booking booking = findById(id);
        RefundStatus refundStatus = parseRefundStatusOrThrow(req.getRefundStatus());

        if (booking.getStatus() != BookingStatus.CANCELLED && refundStatus != RefundStatus.NONE) {
            throw new AppException(ErrorCode.INVALID_REFUND_STATUS,
                    "Chỉ được cập nhật hoàn tiền khi booking đã hủy");
        }

        if (refundStatus == RefundStatus.REFUNDED) {
            RefundStatus currentRefundStatus = extractRefundStatus(booking.getNote());
            if (currentRefundStatus != RefundStatus.ELIGIBLE) {
                throw new AppException(ErrorCode.REFUND_NOT_ELIGIBLE);
            }
        }

        booking.setNote(upsertRefundStatusInNote(booking.getNote(), refundStatus));
        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/bookings/{id}/cancel
    // -------------------------------------------------------------------------
    @Transactional
    public BookingResponse cancelBooking(String id, CancelBookingRequest req) {
        Booking booking = findById(id);

        String currentRole = currentRole();
        if ("CUSTOMER".equalsIgnoreCase(currentRole)) {
            if (!currentEmail().equals(booking.getUserIdentifier())) {
                throw new AppException(ErrorCode.FORBIDDEN);
            }
        }

        validateStatusTransition(booking.getStatus(), BookingStatus.CANCELLED);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelReason(req.getReason());

        PaymentStatus paymentStatus = resolvePaymentStatus(booking);
        RefundStatus refundStatus;
        if (paymentStatus == PaymentStatus.UNPAID) {
            refundStatus = RefundStatus.NONE;
        } else {
            refundStatus = isWithinRefundWindow(booking) ? RefundStatus.ELIGIBLE : RefundStatus.INELIGIBLE;
        }
        booking.setNote(upsertRefundStatusInNote(booking.getNote(), refundStatus));

        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/bookings/{id}  (soft delete)
    // -------------------------------------------------------------------------
    @Transactional
    public void deleteBooking(String id) {
        Booking booking = findById(id);
        booking.setDeletedAt(OffsetDateTime.now());
        bookingRepository.save(booking);
    }

    // -------------------------------------------------------------------------
    // Export: lấy danh sách booking theo filter (cho ExportController)
    // -------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<Booking> getBookingsForExport(String status, String from, String to) {
        BookingStatus bookingStatus = parseStatusOrNull(status);
        LocalDate     fromDate      = parseDateOrNull(from);
        LocalDate     toDate        = parseDateOrNull(to);

        Pageable all = PageRequest.of(0, Integer.MAX_VALUE,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return bookingRepository.findAllByFilter(
                null, false, bookingStatus, null, fromDate, toDate, all
        ).getContent();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Booking findById(String id) {
        UUID uuid = parseUuidOrThrow(id, ErrorCode.BOOKING_NOT_FOUND);
        return bookingRepository.findById(uuid)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
    }

    private void assertOwnerOrStaff(Booking booking) {
        String currentRole = currentRole();
        if ("CUSTOMER".equalsIgnoreCase(currentRole)) {
            if (!currentEmail().equals(booking.getUserIdentifier())) {
                throw new AppException(ErrorCode.FORBIDDEN);
            }
        }
    }

    private Customer resolveCustomer(CreateBookingRequest req) {
        return resolveCustomer(
                req.getCustomerPhone(),
                req.getCustomerIdNumber(),
                req.getCustomerName(),
                req.getCustomerEmail(),
                req.getUserId()
        );
    }

    private Customer resolveCustomer(
            String rawPhone,
            String rawCccd,
            String rawName,
            String rawEmail,
            String fallbackUserId
    ) {
        String phone = rawPhone != null ? rawPhone.trim() : "";
        String cccd = rawCccd != null ? rawCccd.trim() : "";
        String name = rawName != null ? rawName.trim() : "";
        String email = rawEmail != null ? rawEmail.trim() : "";

        if (email.isBlank() && fallbackUserId != null && !fallbackUserId.isBlank()) {
            email = fallbackUserId.trim();
        }
        if (email.isBlank() && "CUSTOMER".equalsIgnoreCase(currentRole()) && !currentEmail().isBlank()) {
            email = currentEmail().trim();
        }

        if (phone.isBlank() || cccd.isBlank()) {
            return null;
        }

        Customer byPhone = customerRepository.findByPhone(phone).orElse(null);
        Customer byCccd = customerRepository.findByCccd(cccd).orElse(null);

        if (byPhone != null && byCccd != null && !byPhone.getId().equals(byCccd.getId())) {
            throw new AppException(ErrorCode.DUPLICATE_CCCD,
                    "Thông tin khách hàng không khớp giữa số điện thoại và CCCD");
        }

        Customer customer = byCccd != null ? byCccd : byPhone;

        if (customer == null) {
            customer = Customer.builder()
                    .fullName(!name.isBlank() ? name : "Khach hang")
                    .phone(phone)
                    .cccd(cccd)
                    .email(!email.isBlank() ? email : null)
                    .build();
            return customerRepository.save(customer);
        }

        if (!name.isBlank()) {
            customer.setFullName(name);
        }
        if (!email.isBlank()) {
            customer.setEmail(email);
        }
        if (!phone.isBlank()) {
            customer.setPhone(phone);
        }
        if (!cccd.isBlank()) {
            customer.setCccd(cccd);
        }

        return customerRepository.save(customer);
    }

    private String resolveUserIdentifier(CreateBookingRequest req) {
        return resolveUserIdentifier(req.getCustomerEmail(), req.getUserId());
    }

    private String resolveUserIdentifier(String customerEmail, String userId) {
        if ("CUSTOMER".equalsIgnoreCase(currentRole()) && !currentEmail().isBlank()) {
            return currentEmail();
        }
        if (customerEmail != null && !customerEmail.isBlank()) {
            return customerEmail.trim();
        }
        if (userId != null && !userId.isBlank()) {
            return userId.trim();
        }
        return currentEmail();
    }

    private void validateStatusTransition(BookingStatus current, BookingStatus next) {
        BookingStatus normalizedCurrent = normalizeBookingStatus(current);
        BookingStatus normalizedNext = normalizeBookingStatus(next);

        if (normalizedCurrent == normalizedNext) {
            return;
        }

        boolean valid = switch (current) {
            case UPCOMING  -> normalizedNext == BookingStatus.CHECKED_IN
                    || normalizedNext == BookingStatus.IN_STAY
                    || normalizedNext == BookingStatus.CHECKED_OUT
                    || normalizedNext == BookingStatus.CANCELLED;
            case CHECKED_IN -> normalizedNext == BookingStatus.IN_STAY
                    || normalizedNext == BookingStatus.CHECKED_OUT
                    || normalizedNext == BookingStatus.CANCELLED;
            case IN_STAY -> normalizedNext == BookingStatus.CHECKED_OUT
                    || normalizedNext == BookingStatus.CANCELLED;
            case CHECKED_OUT -> false;
            case CANCELLED -> false;
            case ACTIVE -> normalizedNext == BookingStatus.CHECKED_IN
                    || normalizedNext == BookingStatus.IN_STAY
                    || normalizedNext == BookingStatus.CHECKED_OUT
                    || normalizedNext == BookingStatus.CANCELLED;
            case COMPLETED -> normalizedNext == BookingStatus.CHECKED_OUT
                    || normalizedNext == BookingStatus.CANCELLED;
        };
        if (!valid) throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION,
                "Cannot transition from " + current + " to " + next);
    }

    private void syncRoomStatusFromBooking(Booking booking, BookingStatus nextStatus) {
        Room room = booking.getRoom();
        if (room == null || nextStatus == null) {
            return;
        }

        RoomStatus currentRoomStatus = room.getStatus();
        BookingStatus normalizedNext = normalizeBookingStatus(nextStatus);

        if (currentRoomStatus == RoomStatus.MAINTENANCE) {
            if (normalizedNext == BookingStatus.CHECKED_IN || normalizedNext == BookingStatus.IN_STAY) {
                throw new AppException(
                        ErrorCode.ROOM_NOT_AVAILABLE,
                        "Phòng đang bảo trì, không thể nhận phòng"
                );
            }
            return;
        }

        RoomStatus targetRoomStatus = switch (normalizedNext) {
            case CHECKED_IN, IN_STAY -> RoomStatus.IN_USE;
            case CHECKED_OUT -> RoomStatus.PENDING_CLEANING;
            default -> null;
        };

        if (targetRoomStatus != null && currentRoomStatus != targetRoomStatus) {
            room.setStatus(targetRoomStatus);
            roomRepository.save(room);
        }
    }

    private BookingStatus parseStatusOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        String normalized = s.trim().toUpperCase();
        try {
            BookingStatus status = switch (normalized) {
                case "ACTIVE" -> BookingStatus.CHECKED_IN;
                case "COMPLETED" -> BookingStatus.CHECKED_OUT;
                default -> BookingStatus.valueOf(normalized);
            };
            return normalizeBookingStatus(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private BookingStatus parseStatusOrThrow(String s) {
        BookingStatus status = parseStatusOrNull(s);
        if (status == null) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Invalid booking status: " + s
                            + ". Valid values: upcoming, checked_in, in_stay, checked_out, cancelled");
        }
        return status;
    }

    private BookingStatus normalizeBookingStatus(BookingStatus status) {
        if (status == null) {
            return BookingStatus.UPCOMING;
        }
        return switch (status) {
            case ACTIVE -> BookingStatus.CHECKED_IN;
            case COMPLETED -> BookingStatus.CHECKED_OUT;
            default -> status;
        };
    }

    private PaymentStatus parsePaymentStatusOrThrow(String s) {
        try {
            return PaymentStatus.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_STATUS,
                    "Invalid payment status: " + s);
        }
    }

    private RefundStatus parseRefundStatusOrThrow(String s) {
        try {
            return RefundStatus.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new AppException(ErrorCode.INVALID_REFUND_STATUS,
                    "Invalid refund status: " + s);
        }
    }

    private PaymentStatus resolvePaymentStatus(Booking booking) {
        Integer paymentPercent = booking.getPaymentPercent();
        if (paymentPercent == null || paymentPercent <= 0) {
            return PaymentStatus.UNPAID;
        }
        if (paymentPercent >= 100) {
            return PaymentStatus.PAID;
        }
        return PaymentStatus.DEPOSITED;
    }

    private boolean isWithinRefundWindow(Booking booking) {
        OffsetDateTime createdAt = booking.getCreatedAt();
        if (createdAt == null) {
            return false;
        }
        long minutes = ChronoUnit.MINUTES.between(createdAt, OffsetDateTime.now());
        return minutes >= 0 && minutes <= 30;
    }

    private RefundStatus extractRefundStatus(String note) {
        if (note == null || note.isBlank()) {
            return RefundStatus.NONE;
        }
        int startIdx = note.indexOf(REFUND_STATUS_PREFIX);
        if (startIdx < 0) {
            return RefundStatus.NONE;
        }
        int valueStart = startIdx + REFUND_STATUS_PREFIX.length();
        int valueEnd = note.indexOf(";", valueStart);
        String value = valueEnd >= 0 ? note.substring(valueStart, valueEnd) : note.substring(valueStart);

        try {
            return RefundStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return RefundStatus.NONE;
        }
    }

    private String upsertRefundStatusInNote(String note, RefundStatus refundStatus) {
        String marker = REFUND_STATUS_PREFIX + refundStatus.name();
        if (note == null || note.isBlank()) {
            return marker;
        }

        int startIdx = note.indexOf(REFUND_STATUS_PREFIX);
        if (startIdx < 0) {
            return note + ";" + marker;
        }

        int valueStart = startIdx + REFUND_STATUS_PREFIX.length();
        int valueEnd = note.indexOf(";", valueStart);
        if (valueEnd < 0) {
            return note.substring(0, startIdx) + marker;
        }
        return note.substring(0, startIdx) + marker + note.substring(valueEnd);
    }

    private BookingType parseBookingTypeOrDefault(String s) {
        if (s == null || s.isBlank()) return BookingType.DAY;
        try { return BookingType.valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return BookingType.DAY; }
    }

    private void validateBookingTiming(
            LocalDate checkIn,
            LocalDate checkOut,
            LocalTime checkInTime,
            LocalTime checkOutTime,
            BookingType bookingType
    ) {
        LocalDate today = LocalDate.now();

        if (checkIn.isBefore(today)) {
            throw new AppException(ErrorCode.BOOKING_START_IN_PAST);
        }

        if (bookingType == BookingType.HOUR) {
            LocalDateTime start = LocalDateTime.of(checkIn, checkInTime);
            LocalDateTime end = LocalDateTime.of(checkOut, checkOutTime);

            if (!end.isAfter(start)) {
                throw new AppException(ErrorCode.INVALID_DATE_RANGE);
            }

            if (start.isBefore(LocalDateTime.now())) {
                throw new AppException(ErrorCode.BOOKING_START_IN_PAST);
            }
            return;
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }
    }

    private LocalTime parseTimeOrThrow(String s, String fieldName) {
        if (s == null || s.isBlank()) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE, fieldName + " is required");
        }

        try {
            return LocalTime.parse(s);
        } catch (DateTimeParseException e) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE, fieldName + " must be HH:mm");
        }
    }

    private PaymentMethod parsePaymentMethodOrNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }

        String normalized = s.trim().toUpperCase();
        if ("BANK".equals(normalized) || "TRANSFER".equals(normalized)) {
            return PaymentMethod.BANK;
        }

        throw new AppException(ErrorCode.INVALID_PAYMENT_STATUS,
                "Only bank transfer payment method is supported");
    }

    private UUID parseUuidOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try { return UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
    }

    private UUID parseUuidOrThrow(String s, ErrorCode code) {
        try { return UUID.fromString(s); }
        catch (IllegalArgumentException e) { throw new AppException(code); }
    }

    private LocalDate parseDateOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s); } catch (DateTimeParseException e) { return null; }
    }

    private LocalDate parseDateOrThrow(String s, String fieldName) {
        if (s == null || s.isBlank())
            throw new AppException(ErrorCode.INVALID_DATE_RANGE, fieldName + " is required");
        try { return LocalDate.parse(s); }
        catch (DateTimeParseException e) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE,
                    fieldName + " must be yyyy-MM-dd");
        }
    }

    private String currentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "";
    }

    private String currentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return "";
        return auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");
    }
}