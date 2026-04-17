package com.nmcnpm.Homestay.service;

import com.nmcnpm.Homestay.dto.request.CancelBookingRequest;
import com.nmcnpm.Homestay.dto.request.CreateBookingRequest;
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
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository  bookingRepository;
    private final RoomRepository     roomRepository;
    private final CustomerRepository customerRepository;
    private final BookingMapper      bookingMapper;
    private final PaymentQrService   paymentQrService;

    // -------------------------------------------------------------------------
    // GET /api/bookings
    // -------------------------------------------------------------------------
    public PagedResponse<BookingResponse> getAllBookings(
            String userId, String status,
            String roomId, String checkInFrom, String checkInTo,
            int page, int size) {

        String currentEmail = currentEmail();
        String currentRole  = currentRole();

        String effectiveUserId;
        if ("CUSTOMER".equalsIgnoreCase(currentRole)) {
            effectiveUserId = currentEmail;
        } else {
            effectiveUserId = (userId != null && !userId.isBlank()) ? userId : null;
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
                effectiveUserId, bookingStatus, roomUUID, from, to, pageable);

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

        // 2. Parse ngày
        LocalDate checkIn  = parseDateOrThrow(req.getCheckIn(),  "checkIn");
        LocalDate checkOut = parseDateOrThrow(req.getCheckOut(), "checkOut");

        if (checkOut.isBefore(checkIn)) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        // 3. Check conflict
        if (bookingRepository.hasConflict(roomUUID, checkIn, checkOut)) {
            throw new AppException(ErrorCode.BOOKING_CONFLICT);
        }

        // 4. Tìm customer
        Customer customer = resolveCustomer(req);

        // 5. Xây dựng entity
        BookingType bookingType = parseBookingTypeOrDefault(req.getBookingType());

        Integer paymentPercent = null;
        if (req.getPaymentAmount() != null) {
            try {
                int p = Integer.parseInt(req.getPaymentAmount());
                if (p == 30 || p == 100) paymentPercent = p;
            } catch (NumberFormatException ignored) {}
        }

        // 6. Tính QR payment fields nếu là bank
        boolean isBank = paymentQrService.requiresQr(req.getPaymentMethod());
        BigDecimal payAmountVnd = null;
        String transferContent  = null;
        OffsetDateTime expiresAt = null;

        if (isBank && paymentPercent != null) {
            payAmountVnd   = paymentQrService.calcPayAmountVnd(req.getTotal(), paymentPercent);
            transferContent = paymentQrService.buildTransferContent(
                    req.getRoomName() != null ? req.getRoomName() : room.getRoomName());
            expiresAt      = paymentQrService.calcExpiresAt();
        }

        Booking booking = Booking.builder()
                .room(room)
                .customer(customer)
                .userIdentifier(req.getUserId() != null ? req.getUserId() : currentEmail())
                .roomNameSnapshot(req.getRoomName() != null ? req.getRoomName() : room.getRoomName())
                .roomImageSnapshot(req.getImage() != null ? req.getImage() : room.getCoverImageUrl())
                .customerNameSnapshot(customer != null ? customer.getFullName() : null)
                .customerPhone(req.getCustomerPhone())
                .customerIdNumber(req.getCustomerIdNumber())
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .checkInTime(req.getCheckInTime() != null
                        ? java.time.LocalTime.parse(req.getCheckInTime()) : null)
                .checkOutTime(req.getCheckOutTime() != null
                        ? java.time.LocalTime.parse(req.getCheckOutTime()) : null)
                .bookingType(bookingType)
                .totalAmount(req.getTotal())
                .status(BookingStatus.UPCOMING)
                .paymentMethod(parsePaymentMethodOrNull(req.getPaymentMethod()))
                .paymentPercent(paymentPercent)
                .payAmountVnd(payAmountVnd)
                .transferContent(transferContent)
                .paymentExpiresAt(expiresAt)
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
                null, bookingStatus, null, fromDate, toDate, all
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
        if (req.getCustomerPhone() == null || req.getCustomerPhone().isBlank()) return null;
        return customerRepository.findByPhone(req.getCustomerPhone()).orElse(null);
    }

    private void validateStatusTransition(BookingStatus current, BookingStatus next) {
        boolean valid = switch (current) {
            case UPCOMING  -> next == BookingStatus.ACTIVE
                    || next == BookingStatus.COMPLETED
                    || next == BookingStatus.CANCELLED;
            case ACTIVE    -> next == BookingStatus.COMPLETED
                    || next == BookingStatus.CANCELLED;
            case COMPLETED -> false;
            case CANCELLED -> false;
        };
        if (!valid) throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION,
                "Cannot transition from " + current + " to " + next);
    }

    private BookingStatus parseStatusOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try { return BookingStatus.valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private BookingStatus parseStatusOrThrow(String s) {
        try { return BookingStatus.valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Invalid booking status: " + s);
        }
    }

    private BookingType parseBookingTypeOrDefault(String s) {
        if (s == null || s.isBlank()) return BookingType.DAY;
        try { return BookingType.valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return BookingType.DAY; }
    }

    private PaymentMethod parsePaymentMethodOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try { return PaymentMethod.valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
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