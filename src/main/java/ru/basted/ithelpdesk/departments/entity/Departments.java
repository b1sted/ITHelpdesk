package ru.basted.ithelpdesk.departments.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import ru.basted.ithelpdesk.core.model.BaseEntity;

@Entity
@Table(name = "departments")
@Getter
@Setter
public class Departments extends BaseEntity {
    @Column(name = "name", unique = true, nullable = false)
    private String name;
}
