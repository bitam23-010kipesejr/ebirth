package com.ebirth.controller;

import com.ebirth.dto.BirthInfoResponse;
import com.ebirth.model.BirthInfo;
import com.ebirth.model.User;
import com.ebirth.repository.BirthInfoRepository;
import com.ebirth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/birth")
@CrossOrigin(
        origins = {"http://localhost:5173", "http://localhost:3000"},
        allowCredentials = "true"
)
public class BirthInfoController {

    @Autowired
    private BirthInfoRepository birthInfoRepo;

    @Autowired
    private UserRepository userRepo;

    // ✅ Client submits birth info
    @PostMapping("/submit/{clientId}")
    public String submit(@PathVariable Long clientId, @RequestBody BirthInfo info) {
        User client = userRepo.findById(clientId).orElse(null);
        if (client == null) return "Client not found";

        info.setSubmittedBy(client);
        info.setStatus("PENDING");
        birthInfoRepo.save(info);
        return "Birth info submitted successfully";
    }

    // ✅ Client views their submitted info
    @GetMapping("/client/{clientId}")
    public List<BirthInfoResponse> getByClient(@PathVariable Long clientId) {
        return birthInfoRepo.findBySubmittedById(clientId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // BirthInfoController.java
    @PutMapping("/update-status/{id}")
    public String updateStatus(@PathVariable Long id, @RequestBody BirthInfo updated) {
        BirthInfo birthInfo = birthInfoRepo.findById(id).orElse(null);
        if (birthInfo == null) return "Birth info not found";

        birthInfo.setStatus(updated.getStatus());
        birthInfo.setComment(updated.getComment());
        birthInfoRepo.save(birthInfo);

        return "Status updated successfully!";
    }
    // Officer can view all submissions
    @GetMapping("/all")
    public List<BirthInfoResponse> getAllSubmissions() {
        return birthInfoRepo.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
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
