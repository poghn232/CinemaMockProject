package com.example.superapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ContactEmailAttachment {
    private String fileName;
    private String contentType;
    private byte[] data;
}
