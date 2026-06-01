package com.example.springtrial.LoanApplication;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanApplicationRepository
        extends JpaRepository<LoanApplication, Long> {
    List<LoanApplication> findByOwner(String owner);
    Optional<LoanApplication> findByIdAndOwner(Long id, String owner);
}