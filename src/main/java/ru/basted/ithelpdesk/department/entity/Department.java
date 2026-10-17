package ru.basted.ithelpdesk.department.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import ru.basted.ithelpdesk.core.model.BaseEntity;

@Entity
@Table(name = "departments")
public class Department extends BaseEntity {
    @Column(name = "name", unique = true, nullable = false)
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}