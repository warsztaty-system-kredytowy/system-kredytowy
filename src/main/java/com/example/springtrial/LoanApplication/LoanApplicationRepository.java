package com.example.springtrial.LoanApplication;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanApplicationRepository
        extends JpaRepository<LoanApplication, Long> {
    // That's it! You get save(), findById(), findAll() etc. for free.
}