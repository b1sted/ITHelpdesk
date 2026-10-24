package ru.basted.ithelpdesk.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import ru.basted.ithelpdesk.department.entity.Department;
import ru.basted.ithelpdesk.department.repository.DepartmentRepository;
import ru.basted.ithelpdesk.user.entity.User;
import ru.basted.ithelpdesk.user.entity.UserRole;
import ru.basted.ithelpdesk.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DBInitializerTest {
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ApplicationArguments applicationArguments;

    private DBInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new DBInitializer(
                departmentRepository,
                userRepository,
                passwordEncoder,
                "admin@company.ru",
                "Admin2024!",
                ""
        );
    }

    @Test
    void createsOnlyAdminAccountWhenItIsMissing() {
        Department department = new Department();
        department.setName("IT Service Desk");
        when(userRepository.existsByEmail("admin@company.ru")).thenReturn(false);
        when(departmentRepository.findByName("IT Service Desk"))
                .thenReturn(Optional.of(department));
        when(passwordEncoder.encode("Admin2024!")).thenReturn("encoded");

        initializer.run(applicationArguments);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User admin = captor.getValue();
        assertEquals("admin@company.ru", admin.getEmail());
        assertEquals("Главный администратор", admin.getFullName());
        assertEquals(UserRole.ADMIN, admin.getRole());
        assertEquals("encoded", admin.getPassword());
        assertEquals(department, admin.getDepartment());
    }

    @Test
    void doesNothingWhenAdminAlreadyExists() {
        when(userRepository.existsByEmail("admin@company.ru")).thenReturn(true);

        initializer.run(applicationArguments);

        verify(userRepository, never()).save(any(User.class));
        verify(departmentRepository, never()).findByName(any());
    }
}
