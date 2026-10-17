package ru.basted.ithelpdesk.category.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import ru.basted.ithelpdesk.core.model.BaseEntity;

@Entity
@Table(name = "categories")
public class Category extends BaseEntity {
    @Column(name = "name", nullable = false)
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}