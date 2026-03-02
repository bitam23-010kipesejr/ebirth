package com.ebirth.controller;

import com.ebirth.dto.BirthInfoResponse;
import com.ebirth.model.BirthInfo;
import com.ebirth.repository.BirthInfoRepository;
import com.ebirth.service.CertificatePdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/officer")
@CrossOrigin(
        origins = {"http://localhost:5173", "http://localhost:3000"},
        allowCredentials = "true"
)
public class OfficerController {

    @Autowired
    private BirthInfoRepository birthInfoRepo;

    @Autowired
    private CertificatePdfService certificatePdfService;

    @GetMapping("/birth-requests")
    public List<BirthInfoResponse> getAll() {
        return birthInfoRepo.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @PutMapping("/certify/{id}")
    public String certify(@PathVariable Long id, @RequestParam String status, @RequestParam(required = false) String comment) {
        BirthInfo info = birthInfoRepo.findById(id).orElse(null);
        if (info == null) return "Record not found";

        String normalizedStatus = status.toUpperCase();
        info.setStatus(normalizedStatus);
        if (comment != null) info.setComment(comment);

        if ("CERTIFIED".equals(normalizedStatus)) {
            if (info.getUploadedCertificateData() == null) {
                return "Cannot certify before applicant uploads birth certificate";
            }
            try {
                byte[] generated = certificatePdfService.generateCertifiedCertificate(info);
                info.setGeneratedCertificateData(generated);
                info.setGeneratedCertificateName("certified_birth_certificate_" + info.getId() + ".pdf");
            } catch (IOException e) {
                return "Failed to generate certified certificate";
            }
        } else {
            info.setGeneratedCertificateData(null);
            info.setGeneratedCertificateName(null);
        }

        birthInfoRepo.save(info);
        return "Updated successfully";
    }

    private BirthInfoResponse toResponse(BirthInfo info) {
        BirthInfoResponse response = new BirthInfoResponse();
        response.id = info.getId();
        response.fullName = info.getFullName();
        response.dateOfBirth = info.getDateOfBirth();
        response.placeOfBirth = info.getPlaceOfBirth();
        response.fatherName = info.getFatherName();
        response.motherName = info.getMotherName();
        response.address = info.getAddress();
        response.status = info.getStatus();
        response.comment = info.getComment();
        if (info.getSubmittedBy() != null) {
            response.submittedById = info.getSubmittedBy().getId();
            response.submittedByName = info.getSubmittedBy().getFullName();
        }
        response.hasUploadedCertificate = info.getUploadedCertificateData() != null;
        response.hasGeneratedCertificate = info.getGeneratedCertificateData() != null;
        response.uploadedCertificateName = info.getUploadedCertificateName();
        return response;
    }
}
