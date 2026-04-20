package com.nmcnpm.Homestay.service;

import com.nmcnpm.Homestay.dto.request.UpdateRoomStatusRequest;
import com.nmcnpm.Homestay.dto.response.RoomAvailabilityDayResponse;
import com.nmcnpm.Homestay.dto.request.UpdateRoomRequest;
import com.nmcnpm.Homestay.dto.response.PagedResponse;
import com.nmcnpm.Homestay.dto.response.RoomResponse;
import com.nmcnpm.Homestay.entity.Booking;
import com.nmcnpm.Homestay.entity.Room;
import com.nmcnpm.Homestay.enums.BookingType;
import com.nmcnpm.Homestay.enums.RoomStatus;
import com.nmcnpm.Homestay.exception.AppException;
import com.nmcnpm.Homestay.exception.ErrorCode;
import com.nmcnpm.Homestay.repository.BookingRepository;
import com.nmcnpm.Homestay.mapper.RoomMapper;
import com.nmcnpm.Homestay.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nmcnpm.Homestay.dto.request.CreateRoomRequest;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE     = 100;
    private static final int DEFAULT_AVAILABILITY_DAYS = 6;
    private static final int MAX_AVAILABILITY_DAYS = 14;
    private static final int CLEANUP_MINUTES = 20;
    private static final LocalTime DEFAULT_DAY_CHECKIN_TIME = LocalTime.of(14, 0);
    private static final LocalTime DEFAULT_DAY_CHECKOUT_TIME = LocalTime.of(12, 0);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final RoomMapper roomMapper;
    private final CloudinaryService cloudinaryService;

    // -------------------------------------------------------------------------
    // GET /api/rooms?status=&search=&page=0&size=20&sort=roomName,asc
    // -------------------------------------------------------------------------
    public PagedResponse<RoomResponse> getAllRooms(
            String status, String search, int page, int size, String sort) {

        Pageable pageable = buildPageable(page, size, sort);
        RoomStatus roomStatus = parseStatusOrNull(status);
        String searchPattern = (search != null && !search.isBlank())
                ? "%" + search.trim().toLowerCase() + "%"
                : null;
        Page<Room> roomPage = roomRepository.findAllByFilter(roomStatus, searchPattern, pageable);

        List<RoomResponse> content = roomPage.getContent()
                .stream()
                .map(roomMapper::toResponse)
                .collect(Collectors.toList());

        return PagedResponse.of(roomPage, content);
    }

    // -------------------------------------------------------------------------
    // GET /api/rooms/{id}
    // -------------------------------------------------------------------------
    public RoomResponse getRoomById(String id) {
        return roomMapper.toResponse(findRoomByStringId(id));
    }

    public List<RoomAvailabilityDayResponse> getRoomAvailability(String id, Integer daysParam) {
        Room room = findRoomByStringId(id);
        int days = daysParam == null
                ? DEFAULT_AVAILABILITY_DAYS
                : Math.min(Math.max(daysParam, 1), MAX_AVAILABILITY_DAYS);

        LocalDate fromDate = LocalDate.now();
        LocalDate toDate = fromDate.plusDays(days - 1L);
        List<Booking> bookings = bookingRepository.findForRoomAvailability(room.getId(), fromDate, toDate);

        return fromDate.datesUntil(toDate.plusDays(1L))
                .map(date -> buildAvailabilityForDate(date, bookings))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // PATCH /api/rooms/{id}/status
    // -------------------------------------------------------------------------
    @Transactional
    public RoomResponse updateStatus(String id, UpdateRoomStatusRequest request) {
        Room room = findRoomByStringId(id);
        room.setStatus(parseStatusOrThrow(request.getStatus()));
        return roomMapper.toResponse(roomRepository.save(room));
    }

    // -------------------------------------------------------------------------
    // PUT /api/rooms/{id}   — update thông tin phòng
    // -------------------------------------------------------------------------
    @Transactional
    public RoomResponse updateRoom(String id, UpdateRoomRequest request) {
        Room room = findRoomByStringId(id);

        validateCapacityByRoomType(request.getType(), request.getCapacity());

        room.setRoomName(request.getName());
        room.setRoomType(request.getType());

        if (request.getCapacity() != null) {
            room.setCapacity(request.getCapacity());
        }

        room.setPricePerNight(request.getPricePerNight());

        if (request.getPricePerHour() != null) {
            room.setPricePerHour(request.getPricePerHour());
        }

        if (request.getStatus() != null) {
            RoomStatus status = parseStatusOrNull(request.getStatus());
            if (status != null) {
                room.setStatus(status);
            }
        }

        if (request.getAmenities() != null) {
            room.setAmenities(request.getAmenities());
        }

        if (request.getDescription() != null) {
            room.setDescription(request.getDescription());
        }

        return roomMapper.toResponse(roomRepository.save(room));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/rooms/{id}   (soft delete)
    // -------------------------------------------------------------------------
    @Transactional
    public void deleteRoom(String id) {
        Room room = findRoomByStringId(id);

        if (roomRepository.hasActiveOrUpcomingBookings(room.getId())) {
            throw new AppException(ErrorCode.ROOM_NOT_AVAILABLE,
                    "Cannot delete room with active or upcoming bookings");
        }

        room.setDeletedAt(OffsetDateTime.now());
        roomRepository.save(room);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Room findRoomByStringId(String id) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.ROOM_NOT_FOUND);
        }
        return roomRepository.findById(uuid)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
    }

    /**
     * Xây Pageable từ query params.
     * sort format: "fieldName,direction" — vd: "roomName,asc" hoặc "createdAt,desc"
     * Mặc định sort theo createdAt DESC nếu không truyền.
     */
    private Pageable buildPageable(int page, int size, String sort) {
        // Giới hạn size để tránh bị abuse
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);

        Sort sortObj = Sort.by(Sort.Direction.DESC, "createdAt"); // default

        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String field = parts[0].trim();
            Sort.Direction dir = (parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc"))
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;

            // Whitelist các field được phép sort để tránh injection
            if (isAllowedSortField(field)) {
                sortObj = Sort.by(dir, field);
            }
        }

        return PageRequest.of(safePage, safeSize, sortObj);
    }

    private boolean isAllowedSortField(String field) {
        return switch (field) {
            case "roomName", "roomType", "pricePerNight", "pricePerHour",
                 "status", "createdAt", "updatedAt" -> true;
            default -> false;
        };
    }

    private RoomStatus parseStatusOrNull(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return RoomStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private RoomStatus parseStatusOrThrow(String status) {
        try {
            return RoomStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Invalid room status: " + status
                            + ". Valid values: available, in_use, pending_cleaning, cleaning_in_progress, cleaned, maintenance");
        }
    }

    private RoomAvailabilityDayResponse buildAvailabilityForDate(LocalDate date, List<Booking> bookings) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1L).atStartOfDay();

        List<LocalDateTime[]> segments = bookings.stream()
                .map(this::toDateRange)
                .map(range -> intersect(range[0], range[1], dayStart, dayEnd))
                .filter(segment -> segment != null)
                .collect(Collectors.toList());

        if (segments.isEmpty()) {
            return RoomAvailabilityDayResponse.builder()
                    .date(date.format(DATE_FMT))
                    .booked(false)
                    .bookedRanges(List.of())
                    .availableRanges(List.of("00:00 - 23:59"))
                    .availableFrom("00:00")
                    .note("Trống cả ngày")
                    .build();
        }

        List<String> ranges = segments.stream()
                .map(segment -> formatRange(segment[0], segment[1]))
                .collect(Collectors.toList());

        List<LocalDateTime[]> bufferedSegments = segments.stream()
                .map(segment -> new LocalDateTime[] {
                        segment[0],
                        segment[1].plusMinutes(CLEANUP_MINUTES).isBefore(dayEnd)
                                ? segment[1].plusMinutes(CLEANUP_MINUTES)
                                : dayEnd
                })
                .collect(Collectors.toList());

        List<LocalDateTime[]> mergedBufferedSegments = mergeSegments(bufferedSegments);
        List<String> availableRanges = buildAvailableRanges(
                mergedBufferedSegments,
                dayStart,
                dayEnd
        );

        LocalDateTime latestEnd = segments.stream()
                .map(segment -> segment[1])
                .max(LocalDateTime::compareTo)
                .orElse(dayStart);
        LocalDateTime availableAt = latestEnd.plusMinutes(CLEANUP_MINUTES);

        String availableFrom = availableAt.toLocalDate().isAfter(date)
                ? availableAt.format(TIME_FMT) + " (+1 ngày)"
                : availableAt.format(TIME_FMT);

        return RoomAvailabilityDayResponse.builder()
                .date(date.format(DATE_FMT))
                .booked(true)
                .bookedRanges(ranges)
                .availableRanges(availableRanges)
                .availableFrom(availableFrom)
                .note(availableRanges.isEmpty() ? "Hết giờ trống" : "Còn giờ trống")
                .build();
    }

    private List<LocalDateTime[]> mergeSegments(List<LocalDateTime[]> segments) {
        if (segments.isEmpty()) {
            return List.of();
        }

        List<LocalDateTime[]> sortedSegments = segments.stream()
                .sorted((a, b) -> a[0].compareTo(b[0]))
                .collect(Collectors.toList());

        List<LocalDateTime[]> merged = new ArrayList<>();
        LocalDateTime currentStart = sortedSegments.get(0)[0];
        LocalDateTime currentEnd = sortedSegments.get(0)[1];

        for (int i = 1; i < sortedSegments.size(); i++) {
            LocalDateTime nextStart = sortedSegments.get(i)[0];
            LocalDateTime nextEnd = sortedSegments.get(i)[1];

            if (!nextStart.isAfter(currentEnd)) {
                if (nextEnd.isAfter(currentEnd)) {
                    currentEnd = nextEnd;
                }
                continue;
            }

            merged.add(new LocalDateTime[] { currentStart, currentEnd });
            currentStart = nextStart;
            currentEnd = nextEnd;
        }

        merged.add(new LocalDateTime[] { currentStart, currentEnd });
        return merged;
    }

    private List<String> buildAvailableRanges(
            List<LocalDateTime[]> blockedSegments,
            LocalDateTime dayStart,
            LocalDateTime dayEnd
    ) {
        if (blockedSegments.isEmpty()) {
            return List.of("00:00 - 23:59");
        }

        List<String> ranges = new ArrayList<>();
        LocalDateTime cursor = dayStart;

        for (LocalDateTime[] blocked : blockedSegments) {
            LocalDateTime blockedStart = blocked[0];
            LocalDateTime blockedEnd = blocked[1];

            if (blockedStart.isAfter(cursor)) {
                ranges.add(formatAvailabilityRange(cursor, blockedStart));
            }

            if (blockedEnd.isAfter(cursor)) {
                cursor = blockedEnd;
            }
        }

        if (dayEnd.isAfter(cursor)) {
            ranges.add(formatAvailabilityRange(cursor, dayEnd));
        }

        return ranges;
    }

    private String formatAvailabilityRange(LocalDateTime start, LocalDateTime endExclusive) {
        LocalDateTime inclusiveEnd = endExclusive.minusSeconds(1);
        if (!inclusiveEnd.isAfter(start)) {
            inclusiveEnd = start;
        }
        return start.format(TIME_FMT) + " - " + inclusiveEnd.format(TIME_FMT);
    }

    private LocalDateTime[] toDateRange(Booking booking) {
        boolean isHourType = booking.getBookingType() == BookingType.HOUR
                && booking.getCheckInTime() != null
                && booking.getCheckOutTime() != null;

        LocalDateTime start = LocalDateTime.of(
                booking.getCheckInDate(),
                isHourType ? booking.getCheckInTime() : DEFAULT_DAY_CHECKIN_TIME
        );

        LocalDateTime end = LocalDateTime.of(
                booking.getCheckOutDate(),
                isHourType ? booking.getCheckOutTime() : DEFAULT_DAY_CHECKOUT_TIME
        );

        if (!end.isAfter(start)) {
            end = start.plusMinutes(1);
        }

        return new LocalDateTime[] { start, end };
    }

    private LocalDateTime[] intersect(
            LocalDateTime start,
            LocalDateTime end,
            LocalDateTime dayStart,
            LocalDateTime dayEnd
    ) {
        LocalDateTime maxStart = start.isAfter(dayStart) ? start : dayStart;
        LocalDateTime minEnd = end.isBefore(dayEnd) ? end : dayEnd;
        if (!minEnd.isAfter(maxStart)) {
            return null;
        }
        return new LocalDateTime[] { maxStart, minEnd };
    }

    private String formatRange(LocalDateTime start, LocalDateTime end) {
        LocalDateTime clampedEnd = end.minusSeconds(1);
        if (!clampedEnd.isAfter(start)) {
            clampedEnd = end;
        }
        return start.format(TIME_FMT) + " - " + clampedEnd.format(TIME_FMT);
    }


    // -------------------------------------------------------------------------
// POST /api/rooms  — tạo phòng mới
// -------------------------------------------------------------------------
    @Transactional
    public RoomResponse createRoom(CreateRoomRequest req, MultipartFile coverImage) {
        validateCapacityByRoomType(req.getType(), req.getCapacity());

        // Upload cover image nếu có file
        String coverUrl = req.getCoverImageUrl();
        if (coverImage != null && !coverImage.isEmpty()) {
            String publicId = "cover_" + System.currentTimeMillis();
            coverUrl = cloudinaryService.upload(coverImage, publicId);
        }
        if (coverUrl == null || coverUrl.isBlank()) {
            coverUrl = "https://placehold.co/800x600?text=No+Image";
        }

        RoomStatus status = req.getStatus() != null
                ? parseStatusOrNull(req.getStatus())
                : RoomStatus.AVAILABLE;
        if (status == null) status = RoomStatus.AVAILABLE;

        Room room = Room.builder()
                .roomName(req.getName())
                .roomType(req.getType())
                .capacity(req.getCapacity())
                .pricePerNight(req.getPricePerNight())
                .pricePerHour(req.getPricePerHour())
                .status(status)
                .coverImageUrl(coverUrl)
                .galleryUrls(req.getGalleryUrls() != null ? req.getGalleryUrls() : new ArrayList<>())
                .amenities(req.getAmenities() != null ? req.getAmenities() : new ArrayList<>())
                .description(req.getDescription())
                .build();

        return roomMapper.toResponse(roomRepository.save(room));
    }

    // -------------------------------------------------------------------------
// POST /api/rooms/{id}/images  — upload/thay cover image
// -------------------------------------------------------------------------
    @Transactional
    public RoomResponse uploadCoverImage(String id, MultipartFile file) {
        Room room = findRoomByStringId(id);
        String publicId = "cover_" + room.getId().toString();
        String url = cloudinaryService.upload(file, publicId);
        room.setCoverImageUrl(url);
        return roomMapper.toResponse(roomRepository.save(room));
    }

    // -------------------------------------------------------------------------
// POST /api/rooms/{id}/gallery  — thêm ảnh vào gallery
// -------------------------------------------------------------------------
    @Transactional
    public RoomResponse uploadGalleryImage(String id, MultipartFile file) {
        Room room = findRoomByStringId(id);
        String publicId = "gallery_" + room.getId() + "_" + System.currentTimeMillis();
        String url = cloudinaryService.upload(file, publicId);

        List<String> gallery = new ArrayList<>(
                room.getGalleryUrls() != null ? room.getGalleryUrls() : List.of()
        );
        gallery.add(url);
        room.setGalleryUrls(gallery);
        return roomMapper.toResponse(roomRepository.save(room));
    }

    // -------------------------------------------------------------------------
// DELETE /api/rooms/{id}/gallery  — xóa 1 ảnh khỏi gallery
// Body: { "imageUrl": "https://..." }
// -------------------------------------------------------------------------
    @Transactional
    public RoomResponse deleteGalleryImage(String id, String imageUrl) {
        Room room = findRoomByStringId(id);

        List<String> gallery = new ArrayList<>(
                room.getGalleryUrls() != null ? room.getGalleryUrls() : List.of()
        );
        if (!gallery.remove(imageUrl)) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Image URL not found in gallery");
        }
        room.setGalleryUrls(gallery);
        cloudinaryService.deleteByUrl(imageUrl);
        return roomMapper.toResponse(roomRepository.save(room));
    }

    private void validateCapacityByRoomType(String roomType, Integer capacity) {
        if (capacity == null) {
            return;
        }

        int maxCapacity = switch (roomType) {
            case "Single Room" -> 1;
            case "Twin Room", "Double Room" -> 2;
            case "VIP Room" -> 3;
            default -> 10;
        };

        if (capacity < 1 || capacity > maxCapacity) {
            throw new AppException(
                    ErrorCode.INVALID_ROOM_CAPACITY,
                    "Capacity " + capacity + " is invalid for room type " + roomType
                            + ". Maximum allowed is " + maxCapacity
            );
        }
    }
}