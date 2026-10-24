package ru.basted.ithelpdesk.settings.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.basted.ithelpdesk.settings.dto.SupportSettingsResponse;
import ru.basted.ithelpdesk.settings.dto.SupportSettingsUpdateRequest;
import ru.basted.ithelpdesk.settings.service.SettingsService;

@RestController
@RequestMapping("/api/v1")
public class SettingsController implements SettingsAPI {
    private final SettingsService service;

    public SettingsController(SettingsService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<SupportSettingsResponse> getSupportSettings() {
        return ResponseEntity.ok(service.getSupportSettings());
    }

    @Override
    public ResponseEntity<SupportSettingsResponse> updateSupportSettings(
            SupportSettingsUpdateRequest request
    ) {
        return ResponseEntity.ok(service.updateSupportSettings(request));
    }
}
