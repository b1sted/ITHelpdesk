package ru.basted.ithelpdesk.department.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.basted.ithelpdesk.department.entity.Department;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findAllByOrderByIdAsc();

    boolean existsByName(String name);

    Optional<Department> findByName(String name);
}