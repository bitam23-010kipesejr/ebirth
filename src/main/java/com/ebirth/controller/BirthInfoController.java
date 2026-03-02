package com.ebirth.controller;

import com.ebirth.dto.BirthInfoResponse;
import com.ebirth.model.BirthInfo;
import com.ebirth.model.User;
import com.ebirth.repository.BirthInfoRepository;
import com.ebirth.repository.UserRepository;
import com.ebirth.service.CertificatePdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/birth")
@CrossOrigin(
        originPatterns = {"http://localhost:5173", "http://localhost:3000", "https://*.onrender.com"},
        allowCredentials = "true"
)
public class BirthInfoController {

    @Autowired
    private BirthInfoRepository birthInfoRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private CertificatePdfService certificatePdfService;

    @PostMapping(value = "/submit/{clientId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String submit(@PathVariable Long clientId, @RequestBody BirthInfo info) {
        User client = userRepo.findById(clientId).orElse(null);
        if (client == null) return "Client not found";

        info.setSubmittedBy(client);
        info.setStatus("PENDING");
        info.setGeneratedCertificateData(null);
        info.setGeneratedCertificateName(null);
        birthInfoRepo.save(info);
        return "Birth info submitted successfully";
    }

    @PostMapping(value = "/submit-with-certificate/{clientId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String submitWithCertificate(
            @PathVariable Long clientId,
            @RequestPart("birthInfo") BirthInfo info,
            @RequestPart("certificate") MultipartFile certificate
    ) {
        User client = userRepo.findById(clientId).orElse(null);
        if (client == null) return "Client not found";
        if (certificate == null || certificate.isEmpty()) return "Certificate file is required";
        if (!isAllowedCertificateType(certificate.getContentType())) return "Unsupported certificate file type";

        try {
            info.setUploadedCertificateData(certificate.getBytes());
            info.setUploadedCertificateName(certificate.getOriginalFilename());
            info.setUploadedCertificateContentType(certificate.getContentType());
            info.setSubmittedBy(client);
            info.setStatus("PENDING");
            info.setGeneratedCertificateData(null);
            info.setGeneratedCertificateName(null);
            birthInfoRepo.save(info);
            return "Birth info and certificate submitted successfully";
        } catch (IOException e) {
            return "Failed to store uploaded certificate";
        }
    }

    @GetMapping("/client/{clientId}")
    public List<BirthInfoResponse> getByClient(@PathVariable Long clientId) {
        return birthInfoRepo.findBySubmittedById(clientId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @PutMapping("/update-status/{id}")
    public String updateStatus(@PathVariable Long id, @RequestBody BirthInfo updated) {
        BirthInfo birthInfo = birthInfoRepo.findById(id).orElse(null);
        if (birthInfo == null) return "Birth info not found";

        String newStatus = updated.getStatus() == null ? "PENDING" : updated.getStatus().toUpperCase();
        birthInfo.setStatus(newStatus);
        birthInfo.setComment(updated.getComment());

        if ("CERTIFIED".equals(newStatus)) {
            if (birthInfo.getUploadedCertificateData() == null) {
                return "Cannot certify before applicant uploads birth certificate";
            }
            try {
                byte[] generated = certificatePdfService.generateCertifiedCertificate(birthInfo);
                birthInfo.setGeneratedCertificateData(generated);
                birthInfo.setGeneratedCertificateName("certified_birth_certificate_" + birthInfo.getId() + ".pdf");
            } catch (IOException e) {
                return "Failed to generate certified certificate";
            }
        } else {
            birthInfo.setGeneratedCertificateData(null);
            birthInfo.setGeneratedCertificateName(null);
        }

        birthInfoRepo.save(birthInfo);
        return "Status updated successfully!";
    }

    @GetMapping("/all")
    public List<BirthInfoResponse> getAllSubmissions() {
        return birthInfoRepo.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<?> downloadCertifiedCertificate(@PathVariable Long id, @RequestParam Long clientId) {
        BirthInfo info = birthInfoRepo.findById(id).orElse(null);
        if (info == null) return ResponseEntity.notFound().build();
        if (info.getSubmittedBy() == null || !clientId.equals(info.getSubmittedBy().getId())) {
            return ResponseEntity.status(403).body("You are not allowed to download this certificate");
        }
        if (!"CERTIFIED".equalsIgnoreCase(info.getStatus())) {
            return ResponseEntity.badRequest().body("Certificate is not yet certified");
        }
        if (info.getGeneratedCertificateData() == null) {
            try {
                byte[] generated = certificatePdfService.generateCertifiedCertificate(info);
                info.setGeneratedCertificateData(generated);
                info.setGeneratedCertificateName("certified_birth_certificate_" + info.getId() + ".pdf");
                birthInfoRepo.save(info);
            } catch (IOException e) {
                return ResponseEntity.internalServerError().body("Failed to generate certified certificate");
            }
        }

        String filename = info.getGeneratedCertificateName() != null
                ? info.getGeneratedCertificateName()
                : "certified_birth_certificate_" + id + ".pdf";

        ByteArrayResource resource = new ByteArrayResource(info.getGeneratedCertificateData());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentLength(info.getGeneratedCertificateData().length)
                .body(resource);
    }

    @GetMapping("/uploaded-certificate/{id}")
    public ResponseEntity<?> viewUploadedCertificate(@PathVariable Long id) {
        BirthInfo info = birthInfoRepo.findById(id).orElse(null);
        if (info == null) return ResponseEntity.notFound().build();
        if (info.getUploadedCertificateData() == null) {
            return ResponseEntity.badRequest().body("No uploaded certificate for this request");
        }

        String filename = info.getUploadedCertificateName() != null
                ? info.getUploadedCertificateName()
                : "uploaded_certificate_" + id;
        String contentType = info.getUploadedCertificateContentType() != null
                ? info.getUploadedCertificateContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        ByteArrayResource resource = new ByteArrayResource(info.getUploadedCertificateData());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentLength(info.getUploadedCertificateData().length)
                .body(resource);
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

    private boolean isAllowedCertificateType(String contentType) {
        if (contentType == null) return false;
        return contentType.equalsIgnoreCase("application/pdf")
                || contentType.equalsIgnoreCase("image/png")
                || contentType.equalsIgnoreCase("image/jpeg");
    }
}
