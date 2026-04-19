package com.nmcnpm.Homestay.controller;

import com.nmcnpm.Homestay.dto.request.CreateRoomRequest;
import com.nmcnpm.Homestay.dto.request.UpdateRoomRequest;
import com.nmcnpm.Homestay.dto.request.UpdateRoomStatusRequest;
import com.nmcnpm.Homestay.dto.response.ApiResponse;
import com.nmcnpm.Homestay.dto.response.PagedResponse;
import com.nmcnpm.Homestay.dto.response.RoomAvailabilityDayResponse;
import com.nmcnpm.Homestay.dto.response.RoomResponse;
import com.nmcnpm.Homestay.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Room endpoints:
 *
 *   GET    /api/rooms                 public  — phân trang, filter
 *   GET    /api/rooms/{id}            public
 *   PATCH  /api/rooms/{id}/status     MANAGER / RECEPTIONIST / CLEANER
 *   DELETE /api/rooms/{id}            MANAGER
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    // ------------------------------------------------------------------
    // GET /api/rooms
    //
    // Query params:
    //   status  — available | in_use | pending_cleaning | cleaning_in_progress | cleaned | maintenance (optional)
    //   search  — keyword tìm theo tên/loại phòng         (optional)
    //   page    — số trang, bắt đầu từ 0                  (default 0)
    //   size    — số item mỗi trang, tối đa 100           (default 10)
    //   sort    — "fieldName,direction" vd: "roomName,asc" (default createdAt,desc)
    //
    // Response: ApiResponse<PagedResponse<RoomResponse>>
    // {
    //   "data": {
    //     "content": [...],
    //     "page": 0, "size": 10,
    //     "totalElements": 87, "totalPages": 5, "last": false
    //   }
    // }
    // ------------------------------------------------------------------
    @GetMapping
    public ApiResponse<PagedResponse<RoomResponse>> getAllRooms(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false)    String sort
    ) {
        return ApiResponse.success(roomService.getAllRooms(status, search, page, size, sort));
    }

    // ------------------------------------------------------------------
    // GET /api/rooms/{id}
    // ------------------------------------------------------------------
    @GetMapping("/{id}")
    public ApiResponse<RoomResponse> getRoomById(@PathVariable String id) {
        return ApiResponse.success(roomService.getRoomById(id));
    }

    @GetMapping("/{id}/availability")
    public ApiResponse<List<RoomAvailabilityDayResponse>> getRoomAvailability(
            @PathVariable String id,
            @RequestParam(defaultValue = "6") Integer days
    ) {
        return ApiResponse.success(roomService.getRoomAvailability(id, days));
    }

    // ------------------------------------------------------------------
    // PATCH /api/rooms/{id}/status
    // ------------------------------------------------------------------
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('MANAGER', 'RECEPTIONIST', 'CLEANER')")
    public ApiResponse<RoomResponse> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateRoomStatusRequest request
    ) {
        return ApiResponse.success(roomService.updateStatus(id, request));
    }

    // ------------------------------------------------------------------
    // PUT /api/rooms/{id}   — update phòng hiện có
    // ------------------------------------------------------------------
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ApiResponse<RoomResponse> updateRoom(
            @PathVariable String id,
            @Valid @ModelAttribute UpdateRoomRequest request
    ) {
        return ApiResponse.success(roomService.updateRoom(id, request));
    }

    // ------------------------------------------------------------------
    // DELETE /api/rooms/{id}   -> 204 No Content
    // ------------------------------------------------------------------
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('MANAGER')")
    public void deleteRoom(@PathVariable String id) {
        roomService.deleteRoom(id);
    }

    // ------------------------------------------------------------------
// POST /api/rooms  — tạo phòng mới (multipart/form-data)
//
// Form fields:
//   name, type, capacity, pricePerNight, pricePerHour,
//   status, coverImageUrl, amenities (JSON array string), description
// File field:
//   coverImage (optional — nếu có sẽ upload lên Cloudinary)
// ------------------------------------------------------------------
    @PostMapping(consumes = {
            org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE,
            org.springframework.http.MediaType.APPLICATION_JSON_VALUE
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MANAGER')")
    public ApiResponse<RoomResponse> createRoom(
            @Valid @ModelAttribute CreateRoomRequest request,
            @RequestPart(value = "coverImage", required = false) MultipartFile coverImage
    ) {
        return ApiResponse.success(
                roomService.createRoom(request, coverImage),
                "Room created successfully"
        );
    }

    // ------------------------------------------------------------------
// POST /api/rooms/{id}/images  — upload/thay thế cover image
//
// Content-Type: multipart/form-data
// File field: file
// ------------------------------------------------------------------
    @PostMapping("/{id}/images")
    @PreAuthorize("hasRole('MANAGER')")
    public ApiResponse<RoomResponse> uploadCoverImage(
            @PathVariable String id,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.success(
                roomService.uploadCoverImage(id, file),
                "Cover image updated successfully"
        );
    }

    // ------------------------------------------------------------------
// POST /api/rooms/{id}/gallery  — thêm ảnh vào gallery
//
// Content-Type: multipart/form-data
// File field: file
// ------------------------------------------------------------------
    @PostMapping("/{id}/gallery")
    @PreAuthorize("hasRole('MANAGER')")
    public ApiResponse<RoomResponse> uploadGalleryImage(
            @PathVariable String id,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.success(
                roomService.uploadGalleryImage(id, file),
                "Gallery image added successfully"
        );
    }

    // ------------------------------------------------------------------
// DELETE /api/rooms/{id}/gallery  — xóa 1 ảnh khỏi gallery
//
// Body: { "imageUrl": "https://res.cloudinary.com/..." }
// ------------------------------------------------------------------
    @DeleteMapping("/{id}/gallery")
    @PreAuthorize("hasRole('MANAGER')")
    public ApiResponse<RoomResponse> deleteGalleryImage(
            @PathVariable String id,
            @RequestBody java.util.Map<String, String> body
    ) {
        String imageUrl = body.get("imageUrl");
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new com.nmcnpm.Homestay.exception.AppException(
                    com.nmcnpm.Homestay.exception.ErrorCode.INTERNAL_ERROR,
                    "imageUrl is required"
            );
        }
        return ApiResponse.success(
                roomService.deleteGalleryImage(id, imageUrl),
                "Gallery image removed successfully"
        );
    }
}