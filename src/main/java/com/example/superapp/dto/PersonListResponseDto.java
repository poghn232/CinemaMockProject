package com.example.superapp.dto;

import java.util.List;

public class PersonListResponseDto {
    private List<PersonDto> items;
    private int page;
    private int totalPages;
    private long totalElements;

    public List<PersonDto> getItems() { return items; }
    public void setItems(List<PersonDto> items) { this.items = items; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
}
