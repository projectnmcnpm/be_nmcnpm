package com.nmcnpm.Homestay.mapper;

import com.nmcnpm.Homestay.dto.response.RoomResponse;
import com.nmcnpm.Homestay.entity.Room;
import org.springframework.stereotype.Component;

/**
 * Mapper thủ công Room entity -> RoomResponse DTO.
 * Tuân theo mapping spec:
 *   rooms.id              -> room.id        (UUID -> String, frontend dùng string)
 *   rooms.room_name       -> room.name
 *   rooms.room_type       -> room.type
 *   rooms.price_per_night -> room.price
 *   rooms.price_per_hour  -> room.pricePerHour
 *   rooms.status          -> room.status    (lowercase enum name)
 *   rooms.cover_image_url -> room.image
 *   rooms.gallery_urls    -> room.gallery
 *   rooms.amenities       -> room.amenities
 *   rooms.description     -> room.description
 */
@Component
public class RoomMapper {

    public RoomResponse toResponse(Room room) {
        if (room == null) return null;

        return RoomResponse.builder()
                .id(room.getId() != null ? room.getId().toString() : null)
                .name(room.getRoomName())
                .type(room.getRoomType())
                .capacity(room.getCapacity())
                .price(room.getPricePerNight())
                .pricePerHour(room.getPricePerHour())
                .status(room.getStatus() != null
                        ? room.getStatus().name().toLowerCase()
                        : null)
                .image(room.getCoverImageUrl())
                .gallery(room.getGalleryUrls())
                .amenities(room.getAmenities())
                .description(room.getDescription())
                .build();
    }
}