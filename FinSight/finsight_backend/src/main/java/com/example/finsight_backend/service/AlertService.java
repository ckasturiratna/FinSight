package com.example.finsight_backend.service;

import com.example.finsight_backend.dto.AlertDto;
import com.example.finsight_backend.dto.CreateAlertRequest;
import com.example.finsight_backend.entity.Alert;
import com.example.finsight_backend.entity.Company;
import com.example.finsight_backend.entity.User;
import com.example.finsight_backend.repository.AlertRepository;
import com.example.finsight_backend.repository.CompanyRepository;
import com.example.finsight_backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Transactional
    public AlertDto createAlert(CreateAlertRequest request) {
        User user = getCurrentUser();
        Company company = companyRepository.findById(request.getTicker().toUpperCase())
                .orElseThrow(() -> new EntityNotFoundException("Company with ticker " + request.getTicker() + " not found."));

        Alert alert = new Alert();
        alert.setUser(user);
        alert.setCompany(company);
        alert.setConditionType(request.getConditionType());
        alert.setThreshold(request.getThreshold());
        alert.setActive(true);

        Alert savedAlert = alertRepository.save(alert);
        return toDto(savedAlert);
    }

    public List<AlertDto> getAlertsForCurrentUser() {
        User user = getCurrentUser();
        return alertRepository.findByUser(user).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteAlert(Long alertId) {
        User user = getCurrentUser();
        Alert alert = alertRepository.findByIdAndUser_Id(alertId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Alert not found or you do not have permission to delete it."));
        alertRepository.delete(alert);
    }

    private AlertDto toDto(Alert alert) {
        AlertDto dto = new AlertDto();
        dto.setId(alert.getId());
        dto.setTicker(alert.getCompany().getTicker());
        dto.setConditionType(alert.getConditionType());
        dto.setThreshold(alert.getThreshold());
        dto.setActive(alert.isActive());
        dto.setCreatedAt(alert.getCreatedAt());
        dto.setUpdatedAt(alert.getUpdatedAt());
        return dto;
    }
}