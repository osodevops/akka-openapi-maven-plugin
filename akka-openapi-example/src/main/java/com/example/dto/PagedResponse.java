package com.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * A paginated response wrapper for list operations.
 *
 * <p>Provides pagination metadata alongside the requested data.</p>
 *
 * @param <T> the type of items in the page
 */
public class PagedResponse<T> {

    /**
     * The list of items for this page.
     */
    private List<T> content;

    /**
     * The current page number (0-indexed).
     */
    private int page;

    /**
     * The number of items per page.
     */
    private int size;

    /**
     * The total number of items across all pages.
     */
    @JsonProperty("totalElements")
    private long totalElements;

    /**
     * The total number of pages.
     */
    @JsonProperty("totalPages")
    private int totalPages;

    /**
     * Whether this is the first page.
     */
    private boolean first;

    /**
     * Whether this is the last page.
     */
    private boolean last;

    public PagedResponse() {
    }

    public PagedResponse(List<T> content, int page, int size, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        this.first = page == 0;
        this.last = page >= totalPages - 1;
    }

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }
}
