package ru.basted.ithelpdesk.departments.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.basted.ithelpdesk.departments.entity.Departments;

public interface DepartmentsRepository extends JpaRepository<Departments, Long> {
    List<Departments> findAllByOrderByIdAsc();

    Boolean existsByName(String name);
}
