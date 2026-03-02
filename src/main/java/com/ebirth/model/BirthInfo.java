package com.ebirth.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "birth_infos")   // ✅ epuka reserved names + best practice
public class BirthInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;

    private LocalDate dateOfBirth;

    private String placeOfBirth;

    private String fatherName;

    private String motherName;

    private String address;

    private String status = "PENDING";

    private String comment;

    @Lob
    @Column(name = "uploaded_certificate_data")
    private byte[] uploadedCertificateData;

    private String uploadedCertificateName;

    private String uploadedCertificateContentType;

    @Lob
    @Column(name = "generated_certificate_data")
    private byte[] generatedCertificateData;

    private String generatedCertificateName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")   // foreign key clear
    private User submittedBy;

}
