package com.example.superapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_logs")
@Getter
@Setter
@NoArgsConstructor
public class AdminLogs {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private int logId;

    @CreationTimestamp
    private LocalDateTime at;

    private String description;

    public AdminLogs(String description) {
        this.description = description;
    }
}
