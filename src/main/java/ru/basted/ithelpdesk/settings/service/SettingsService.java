package ru.basted.ithelpdesk.settings.service;

import ru.basted.ithelpdesk.settings.dto.SupportSettingsResponse;
import ru.basted.ithelpdesk.settings.dto.SupportSettingsUpdateRequest;

public interface SettingsService {
    SupportSettingsResponse getSupportSettings();

    SupportSettingsResponse updateSupportSettings(SupportSettingsUpdateRequest request);

    int getAutoCloseResolvedHours();
}
