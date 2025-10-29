package com.example.finsight_backend.controller;

import com.example.finsight_backend.dto.AlertDto;
import com.example.finsight_backend.dto.CreateAlertRequest;
import com.example.finsight_backend.service.AlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AlertDto> createAlert(@Valid @RequestBody CreateAlertRequest request) {
        return new ResponseEntity<>(alertService.createAlert(request), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<AlertDto> getAlerts() {
        return alertService.getAlertsForCurrentUser();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        alertService.deleteAlert(id);
        return ResponseEntity.noContent().build();
    }
}