package com.ebirth.controller;

import com.ebirth.dto.BirthInfoResponse;
import com.ebirth.model.BirthInfo;
import com.ebirth.repository.BirthInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    // ✅ View all submitted birth info
    @GetMapping("/birth-requests")
    public List<BirthInfoResponse> getAll() {
        return birthInfoRepo.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ✅ Certify or Reject info
    @PutMapping("/certify/{id}")
    public String certify(@PathVariable Long id, @RequestParam String status, @RequestParam(required = false) String comment) {
        BirthInfo info = birthInfoRepo.findById(id).orElse(null);
        if (info == null) return "Record not found";

        info.setStatus(status.toUpperCase()); // CERTIFIED or REJECTED
        if (comment != null) info.setComment(comment);
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
        return response;
    }
}
