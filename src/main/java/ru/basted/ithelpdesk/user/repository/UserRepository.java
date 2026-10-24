package ru.basted.ithelpdesk.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import ru.basted.ithelpdesk.user.entity.User;
import ru.basted.ithelpdesk.user.entity.UserRole;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "department")
    List<User> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = "department")
    List<User> findAllByRoleOrderByFullNameAsc(UserRole role);

    @Override
    @EntityGraph(attributePaths = "department")
    Optional<User> findById(Long id);
}
