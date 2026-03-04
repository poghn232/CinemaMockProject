package com.example.superapp.dto;

import java.util.List;

public class MoviePageResponse {

    private List<MovieItemDto> items;
    private int page;
    private int totalPages;

    public MoviePageResponse() {
    }

    public MoviePageResponse(List<MovieItemDto> items, int page, int totalPages) {
        this.items = items;
        this.page = page;
        this.totalPages = totalPages;
    }

    public List<MovieItemDto> getItems() {
        return items;
    }

    public void setItems(List<MovieItemDto> items) {
        this.items = items;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}

