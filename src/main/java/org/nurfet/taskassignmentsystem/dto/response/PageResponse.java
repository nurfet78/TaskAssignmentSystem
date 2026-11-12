package org.nurfet.taskassignmentsystem.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Страница результатов")
public record PageResponse<T>(

        @Schema(description = "Элементы текущей страницы")
        List<T> content,

        @Schema(description = "Номер страницы, начиная с 0", example = "0")
        int page,

        @Schema(description = "Размер страницы", example = "20")
        int size,

        @Schema(description = "Всего элементов", example = "125")
        long totalElements,

        @Schema(description = "Всего страниц", example = "7")
        int totalPages,

        @Schema(description = "Признак последней страницы", example = "false")
        boolean last
) {

    public static <T> PageResponse<T> of(List<T> content, Page<?> page) {
        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }
}
