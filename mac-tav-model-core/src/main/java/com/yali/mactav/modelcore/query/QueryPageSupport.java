package com.yali.mactav.modelcore.query;

import java.util.List;

/**
 * Shared pagination helpers for Model Core read-side queries.
 */
public final class QueryPageSupport {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private QueryPageSupport() {
    }

    public static int page(int page) {
        return page <= 0 ? DEFAULT_PAGE : page;
    }

    public static int size(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    public static int offset(int page, int size) {
        return (page(page) - 1) * size(size);
    }

    public static <T> List<T> slice(List<T> source, int page, int size) {
        int normalizedPage = page(page);
        int normalizedSize = size(size);
        int fromIndex = Math.min((normalizedPage - 1) * normalizedSize, source.size());
        int toIndex = Math.min(fromIndex + normalizedSize, source.size());
        return source.subList(fromIndex, toIndex);
    }
}
