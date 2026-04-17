package com.nmcnpm.Homestay.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Envelope phân trang dùng chung — bọc ngoài ApiResponse.data.
 *
 * Ví dụ response:
 * {
 *   "data": {
 *     "content": [...],
 *     "page": 0,
 *     "size": 20,
 *     "totalElements": 87,
 *     "totalPages": 5,
 *     "last": false
 *   }
 * }
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PagedResponse<T> {

    List<T> content;
    int page;
    int size;
    long totalElements;
    int totalPages;
    boolean last;

    /** Factory method — tạo từ Spring Page + danh sách content đã map. */
    public static <T> PagedResponse<T> of(Page<?> springPage, List<T> mappedContent) {
        return PagedResponse.<T>builder()
                .content(mappedContent)
                .page(springPage.getNumber())
                .size(springPage.getSize())
                .totalElements(springPage.getTotalElements())
                .totalPages(springPage.getTotalPages())
                .last(springPage.isLast())
                .build();
    }
}