package com.example.superapp.controller;

import com.example.superapp.entity.AdminLogs;
import com.example.superapp.service.AdminLogsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
public class AdminLogsController {
    private final AdminLogsService logsService;

    @GetMapping("/all")
    public ResponseEntity<List<AdminLogs>> getLogs() {
        List<AdminLogs> logs = logsService.getAllLogs();
        return ResponseEntity.ok(logs);
    }
}
