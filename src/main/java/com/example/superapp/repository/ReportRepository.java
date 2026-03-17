package com.example.superapp.repository;

import com.example.superapp.entity.Report;
import com.example.superapp.entity.Report.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByStatus(Status status);
}
