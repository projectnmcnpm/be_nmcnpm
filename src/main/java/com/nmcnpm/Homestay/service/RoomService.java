package com.nmcnpm.Homestay.service;

import com.nmcnpm.Homestay.dto.request.UpdateRoomStatusRequest;
import com.nmcnpm.Homestay.dto.response.PagedResponse;
import com.nmcnpm.Homestay.dto.response.RoomResponse;
import com.nmcnpm.Homestay.entity.Room;
import com.nmcnpm.Homestay.enums.RoomStatus;
import com.nmcnpm.Homestay.exception.AppException;
import com.nmcnpm.Homestay.exception.ErrorCode;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE     = 100;

    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;
    private final CloudinaryService cloudinaryService;

    // -------------------------------------------------------------------------
    // GET /api/rooms?status=&search=&page=0&size=20&sort=roomName,asc
    // -------------------------------------------------------------------------
    public PagedResponse<RoomResponse> getAllRooms(
            String status, String search, int page, int size, String sort) {

        Pageable pageable = buildPageable(page, size, sort);
        RoomStatus roomStatus = parseStatusOrNull(status);
        String keyword = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<Room> roomPage = roomRepository.findAllByFilter(roomStatus, keyword, pageable);

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
                            + ". Valid values: available, few_left, full, cleaning");
        }
    }


    // -------------------------------------------------------------------------
// POST /api/rooms  — tạo phòng mới
// -------------------------------------------------------------------------
    @Transactional
    public RoomResponse createRoom(CreateRoomRequest req, MultipartFile coverImage) {
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
}