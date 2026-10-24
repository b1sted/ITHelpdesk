package ru.basted.ithelpdesk.settings.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.basted.ithelpdesk.settings.entity.SupportSettings;

public interface SupportSettingsRepository extends JpaRepository<SupportSettings, Long> {
    Optional<SupportSettings> findFirstByOrderByIdAsc();
}
