package com.example.superapp.repository;

import com.example.superapp.entity.AdminLogs;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminLogsRepository extends JpaRepository<AdminLogs, Integer> {
}
