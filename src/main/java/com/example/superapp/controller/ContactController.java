package com.example.superapp.controller;

import com.example.superapp.dto.ContactEmailAttachment;
import com.example.superapp.dto.ContactRequest;
import com.example.superapp.service.EmailService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ContactController {

    private final EmailService emailService;

    public ContactController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping(value = "/contact", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> sendContact(
            @ModelAttribute ContactRequest request,
            @RequestParam(value = "images[]", required = false) MultipartFile[] images
    ) throws Exception {

        List<ContactEmailAttachment> attachments = new ArrayList<>();

        if (images != null) {
            for (MultipartFile file : images) {

                if (!file.isEmpty()) {

                    attachments.add(
                            new ContactEmailAttachment(
                                    file.getOriginalFilename(),
                                    file.getContentType(),
                                    file.getBytes()
                            )
                    );
                }
            }
        }

        emailService.sendContactMail(request, attachments);

        return ResponseEntity.ok("Gửi liên hệ thành công!");
    }
}

