package com.example.finsight_backend.repository;

import com.example.finsight_backend.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CompanyRepository extends JpaRepository<Company, String>, JpaSpecificationExecutor<Company> {
    // JpaSpecificationExecutor for dynamic queries
}
