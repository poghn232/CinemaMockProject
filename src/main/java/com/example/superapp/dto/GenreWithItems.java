package com.example.superapp.dto;

import java.util.ArrayList;
import java.util.List;

public class GenreWithItems {
    private Long id;
    private String name;
    private List<MovieItemDto> items = new ArrayList<>();

    public GenreWithItems() {
    }

    public GenreWithItems(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<MovieItemDto> getItems() {
        return items;
    }

    public void setItems(List<MovieItemDto> items) {
        this.items = items;
    }

    public void addItem(MovieItemDto item) {
        if (item != null) this.items.add(item);
    }
}
