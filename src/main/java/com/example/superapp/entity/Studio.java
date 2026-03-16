package com.example.superapp.entity;

import com.example.superapp.repository.AdminLogsRepository;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "studios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Studio {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    private String logoPath;
    private String originCountry;

    @PostPersist
    private void addLog() {
        AdminLogsRepository.class.getMethod("save", AdminLogs.class).invoke(new AdminLogs(""));
    }

    @Override
    public String toString() {
        return "Studio " + name;
    }
}
