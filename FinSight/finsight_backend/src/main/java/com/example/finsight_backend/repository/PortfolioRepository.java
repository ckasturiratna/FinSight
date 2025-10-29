package com.example.finsight_backend.repository;

import com.example.finsight_backend.entity.Portfolio;
import com.example.finsight_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    Page<Portfolio> findAllByUser(User user, Pageable pageable);
    Optional<Portfolio> findByIdAndUser(Long id, User user);
}

