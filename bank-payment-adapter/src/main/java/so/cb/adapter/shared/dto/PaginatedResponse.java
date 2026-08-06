package so.cb.adapter.shared.dto;

import java.util.List;

public record PaginatedResponse<T>(
        List<T> items,
        int totalItems,
        int pageNumber,
        int pageSize
) {}