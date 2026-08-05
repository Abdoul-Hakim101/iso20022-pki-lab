package so.cb.pki.shared.dto;

import java.util.List;

public record PaginatedResponse<T>(
        List<T> items,
        int totalItems,
        int pageNumber,
        int pageSize
) {}