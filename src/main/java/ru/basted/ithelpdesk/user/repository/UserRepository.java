package ru.basted.ithelpdesk.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import ru.basted.ithelpdesk.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = "department")
    List<User> findAllByOrderByIdAsc();

    @Override
    @EntityGraph(attributePaths = "department")
    Optional<User> findById(Long id);
}
