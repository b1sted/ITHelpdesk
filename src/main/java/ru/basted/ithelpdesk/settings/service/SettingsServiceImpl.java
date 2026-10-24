package ru.basted.ithelpdesk.settings.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.basted.ithelpdesk.core.exception.NotFoundException;
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
    @Transactional(readOnly = true)
    public SupportSettingsResponse getSupportSettings() {
        return toResponse(findSettings());
    }

    @Override
    @Transactional
    public SupportSettingsResponse updateSupportSettings(SupportSettingsUpdateRequest request) {
        SupportSettings settings = findSettings();
        settings.setAutoCloseResolvedHours(request.autoCloseResolvedHours());
        return toResponse(settings);
    }

    @Override
    @Transactional(readOnly = true)
    public int getAutoCloseResolvedHours() {
        return findSettings().getAutoCloseResolvedHours();
    }

    private SupportSettings findSettings() {
        return repository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new NotFoundException("Не найдены настройки поддержки"));
    }

    private SupportSettingsResponse toResponse(SupportSettings settings) {
        return new SupportSettingsResponse(settings.getAutoCloseResolvedHours());
    }
}
