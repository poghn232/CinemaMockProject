package com.example.superapp.service;

import com.example.superapp.dto.ContactEmailAttachment;
import com.example.superapp.dto.ContactRequest;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailContactService {

    private static final Logger log = LoggerFactory.getLogger(EmailContactService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Async
    public void sendContactMail(ContactRequest request,
                                List<ContactEmailAttachment> attachments) throws Exception {

        validateImages(attachments);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper =
                new MimeMessageHelper(message, true, "UTF-8");

        // Đổi sang mail admin ở đây
        helper.setTo("manh12345678n@gmail.com");
        helper.setSubject("📩 New Contact From MovieZone");

        String html = """
                <div style="font-family:Arial;padding:24px;background:#0b1220;color:#e5e7eb">
                    <h2 style="color:#f97316">📩 New Contact Message</h2>
                
                    <p><b>Name:</b> %s</p>
                    <p><b>Email:</b> %s</p>
                    <p><b>Phone:</b> %s</p>
                
                    <hr>
                
                    <p><b>Message:</b></p>
                    <div style="background:#0f172a;padding:12px;border-radius:8px">
                        %s
                    </div>
                </div>
                """.formatted(
                request.getName(),
                request.getEmail(),
                request.getPhone(),
                request.getContent()
        );

        helper.setText(html, true);

        if (attachments != null) {
            for (ContactEmailAttachment file : attachments) {

                if (file.getData() != null && file.getData().length > 0) {

                    log.info("Attach file: {}", file.getFileName());

                    helper.addAttachment(
                            file.getFileName(),
                            new ByteArrayResource(file.getData()),
                            file.getContentType()
                    );
                }
            }
        }

        mailSender.send(message);

        sendConfirmationToUser(request.getEmail(), request.getName());
    }

    private void validateImages(List<ContactEmailAttachment> attachments) {

        if (attachments == null) return;

        for (ContactEmailAttachment file : attachments) {

            if (file.getData() == null || file.getData().length == 0) {
                continue;
            }

            if (file.getContentType() == null ||
                    !file.getContentType().startsWith("image/")) {
                throw new RuntimeException("Chỉ được upload file ảnh!");
            }

            if (file.getData().length > 5 * 1024 * 1024) {
                throw new RuntimeException("Ảnh vượt quá 5MB!");
            }
        }
    }

    private void sendConfirmationToUser(String email, String name) throws Exception {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper =
                new MimeMessageHelper(message, false, "UTF-8");

        helper.setTo(email);
        helper.setSubject("🎬 MovieZone - Chúng tôi đã nhận được liên hệ");

        String html = """
                <div style="font-family:Arial;padding:24px;background:#0b1220;color:#e5e7eb">
                    <h2 style="color:#f97316">Xin chào %s 👋</h2>
                    <p>Cảm ơn bạn đã liên hệ với MovieZone.</p>
                    <p>Chúng tôi sẽ phản hồi trong vòng 24 giờ.</p>
                    <br> <p style="font-size:13px;color:#94a3b8"> 
                    Đây là email tự động, vui lòng không trả lời trực tiếp. 
                    </p>
                </div>
                """.formatted(name);

        helper.setText(html, true);

        mailSender.send(message);
    }
}


