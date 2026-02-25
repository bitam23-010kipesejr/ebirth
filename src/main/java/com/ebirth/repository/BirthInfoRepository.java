package com.ebirth.repository;

import com.ebirth.model.BirthInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BirthInfoRepository extends JpaRepository<BirthInfo, Long> {
    List<BirthInfo> findBySubmittedById(Long userId);
}
