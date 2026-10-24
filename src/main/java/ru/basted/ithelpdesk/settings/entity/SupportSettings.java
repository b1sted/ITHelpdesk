package ru.basted.ithelpdesk.settings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import ru.basted.ithelpdesk.core.model.BaseEntity;

@Entity
@Table(name = "support_settings")
public class SupportSettings extends BaseEntity {
    @Column(name = "auto_close_resolved_hours", nullable = false)
    private Integer autoCloseResolvedHours = 0;

    public Integer getAutoCloseResolvedHours() {
        return autoCloseResolvedHours;
    }

    public void setAutoCloseResolvedHours(Integer autoCloseResolvedHours) {
        this.autoCloseResolvedHours = autoCloseResolvedHours;
    }
}
