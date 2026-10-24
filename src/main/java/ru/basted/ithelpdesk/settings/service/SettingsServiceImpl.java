package ru.basted.ithelpdesk.settings.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.basted.ithelpdesk.settings.dto.SupportSettingsResponse;
import ru.basted.ithelpdesk.settings.dto.SupportSettingsUpdateRequest;
import ru.basted.ithelpdesk.settings.entity.SupportSettings;
import ru.basted.ithelpdesk.settings.repository.SupportSettingsRepository;

@Service
public class SettingsServiceImpl implements SettingsService {
    private final SupportSettingsRepository repository;

    public SettingsServiceImpl(SupportSettingsRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SupportSettingsResponse getSupportSettings() {
        return toResponse(loadOrCreate());
    }

    @Override
    @Transactional
    public SupportSettingsResponse updateSupportSettings(SupportSettingsUpdateRequest request) {
        SupportSettings settings = loadOrCreate();
        settings.setAutoCloseResolvedHours(request.autoCloseResolvedHours());
        return toResponse(repository.save(settings));
    }

    @Override
    @Transactional(readOnly = true)
    public int getAutoCloseResolvedHours() {
        return repository.findFirstByOrderByIdAsc()
                .map(SupportSettings::getAutoCloseResolvedHours)
                .orElse(0);
    }

    private SupportSettings loadOrCreate() {
        return repository.findFirstByOrderByIdAsc()
                .orElseGet(() -> {
                    SupportSettings settings = new SupportSettings();
                    settings.setAutoCloseResolvedHours(0);
                    return repository.save(settings);
                });
    }

    private SupportSettingsResponse toResponse(SupportSettings settings) {
        return new SupportSettingsResponse(settings.getAutoCloseResolvedHours());
    }
}
