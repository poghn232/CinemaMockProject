package com.example.superapp.service;

import com.example.superapp.entity.AdminLogs;
import com.example.superapp.repository.AdminLogsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminLogsService {
    private final AdminLogsRepository adminLogsRepository;

    public List<AdminLogs> getAllLogs() {
        return adminLogsRepository.findAll();
    }

    public void saveLog(AdminLogs log) {
        adminLogsRepository.save(log);
    }
}
