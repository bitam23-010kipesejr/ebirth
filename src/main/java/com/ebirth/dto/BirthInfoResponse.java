package com.ebirth.dto;

import java.time.LocalDate;

public class BirthInfoResponse {

    public Long id;
    public String fullName;
    public LocalDate dateOfBirth;
    public String placeOfBirth;
    public String fatherName;
    public String motherName;
    public String address;
    public String status;
    public String comment;
    public Long submittedById;
    public String submittedByName;
    public boolean hasUploadedCertificate;
    public boolean hasGeneratedCertificate;
    public String uploadedCertificateName;
}
