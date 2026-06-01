package com.example.springtrial.LoanApplication;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class ApplicationStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_application_id")
    @JsonBackReference
    private LoanApplication application;

    @Enumerated(EnumType.STRING)
    private LoanStatus status;

    private LocalDateTime timestamp;

    @Column(length = 2000)
    private String note;

    private String evaluatedBy;
}
