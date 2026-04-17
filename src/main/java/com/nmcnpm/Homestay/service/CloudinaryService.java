package com.nmcnpm.Homestay.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.nmcnpm.Homestay.exception.AppException;
import com.nmcnpm.Homestay.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.folder:homestay/rooms}")
    private String folder;

    /**
     * Upload 1 file ảnh lên Cloudinary.
     * @param file  MultipartFile từ request
     * @param publicId  tên file muốn đặt (optional, null = Cloudinary tự generate)
     * @return URL ảnh secure (https)
     */
    public String upload(MultipartFile file, String publicId) {
        validateImageFile(file);
        try {
            Map<?, ?> options = publicId != null
                    ? ObjectUtils.asMap(
                    "folder",    folder,
                    "public_id", publicId,
                    "overwrite", true,
                    "resource_type", "image"
            )
                    : ObjectUtils.asMap(
                    "folder",    folder,
                    "resource_type", "image"
            );

            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), options);
            String url = (String) result.get("secure_url");
            log.info("Cloudinary upload success: {}", url);
            return url;

        } catch (IOException e) {
            log.error("Cloudinary upload failed", e);
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Image upload failed: " + e.getMessage());
        }
    }

    /**
     * Xóa ảnh khỏi Cloudinary theo URL.
     * Trích xuất public_id từ URL rồi gọi destroy.
     */
    public void deleteByUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        try {
            String publicId = extractPublicId(imageUrl);
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Cloudinary delete success: {}", publicId);
        } catch (IOException e) {
            // Không throw — ảnh trên cloud xóa lỗi không nên block luồng chính
            log.warn("Cloudinary delete failed for url: {}", imageUrl, e);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "File is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new AppException(ErrorCode.INTERNAL_ERROR,
                    "Only image files are allowed (jpg, png, webp...)");
        }
        // Giới hạn 10MB
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new AppException(ErrorCode.INTERNAL_ERROR,
                    "File size must not exceed 10MB");
        }
    }

    /**
     * Trích public_id từ Cloudinary URL.
     * VD: https://res.cloudinary.com/demo/image/upload/v123/homestay/rooms/abc.jpg
     *  -> homestay/rooms/abc
     */
    private String extractPublicId(String url) {
        // Lấy phần sau "/upload/"
        int uploadIdx = url.indexOf("/upload/");
        if (uploadIdx == -1) return url;
        String afterUpload = url.substring(uploadIdx + 8); // bỏ "/upload/"
        // Bỏ version prefix vd: v1234567890/
        if (afterUpload.startsWith("v") && afterUpload.contains("/")) {
            afterUpload = afterUpload.substring(afterUpload.indexOf("/") + 1);
        }
        // Bỏ extension
        int dotIdx = afterUpload.lastIndexOf(".");
        return dotIdx != -1 ? afterUpload.substring(0, dotIdx) : afterUpload;
    }
}