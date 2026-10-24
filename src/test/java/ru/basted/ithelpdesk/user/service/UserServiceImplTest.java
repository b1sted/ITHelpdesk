package ru.basted.ithelpdesk.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import ru.basted.ithelpdesk.core.exception.ConflictException;
import ru.basted.ithelpdesk.core.model.BaseEntity;
import ru.basted.ithelpdesk.department.entity.Department;
import ru.basted.ithelpdesk.department.repository.DepartmentRepository;
import ru.basted.ithelpdesk.user.dto.UserCreateRequest;
import ru.basted.ithelpdesk.user.dto.UserLimitsRequest;
import ru.basted.ithelpdesk.user.dto.UserUpdateRequest;
import ru.basted.ithelpdesk.user.entity.User;
import ru.basted.ithelpdesk.user.entity.UserRole;
import ru.basted.ithelpdesk.user.mapper.UserMapper;
import ru.basted.ithelpdesk.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    private UserRepository repository;
    @Mock
    private UserMapper mapper;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(repository, mapper, departmentRepository, passwordEncoder);
    }

    @Test
    void partialUpdateChangesOnlyProvidedFields() {
        User user = user();
        when(repository.findById(1L)).thenReturn(Optional.of(user));

        var response = service.updateUser(
                1L, new UserUpdateRequest(null, null, UserRole.AGENT, null)
        );

        assertEquals(UserRole.AGENT, response.role());
        assertEquals("Иванов Иван", response.fullName());
        assertEquals("ivanov@company.ru", response.email());
    }

    @Test
    void updateLimitsChangesTicketAndTimeLimitsOfAgent() {
        User user = user();
        user.setRole(UserRole.AGENT);
        when(repository.findById(1L)).thenReturn(Optional.of(user));

        var response = service.updateLimits(1L, new UserLimitsRequest(7, 12));

        assertEquals(7, response.ticketLimit());
        assertEquals(12, response.timeLimitHours());
    }

    @Test
    void updateLimitsRejectsNonAgent() {
        User user = user();
        when(repository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(
                ConflictException.class,
                () -> service.updateLimits(1L, new UserLimitsRequest(7, 12))
        );
    }

    @Test
    void createUserWithDuplicateEmailThrowsConflict() {
        when(repository.existsByEmailIgnoreCase("ivanov@company.ru")).thenReturn(true);

        UserCreateRequest request = new UserCreateRequest(
                "Иванов Иван", "ivanov@company.ru", "password123", UserRole.EMPLOYEE, 1L
        );

        assertThrows(ConflictException.class, () -> service.createUser(request));
    }

    @Test
    void createUserStoresNormalizedEmail() {
        User user = user();
        when(repository.existsByEmailIgnoreCase("new@company.ru")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department()));
        when(mapper.toEntity(any(UserCreateRequest.class))).thenReturn(user);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(repository.save(user)).thenReturn(user);

        var response = service.createUser(new UserCreateRequest(
                "Иванов Иван", "  New@Company.RU  ", "password123", UserRole.EMPLOYEE, 1L
        ));

        assertEquals("new@company.ru", response.email());
    }

    @Test
    void updateUserStoresNormalizedEmail() {
        User user = user();
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(repository.existsByEmailIgnoreCase("new@company.ru")).thenReturn(false);

        var response = service.updateUser(
                1L, new UserUpdateRequest(null, " NEW@Company.RU ", null, null)
        );

        assertEquals("new@company.ru", response.email());
    }

    private User user() {
        User user = new User();
        setId(user, 1L);
        user.setFullName("Иванов Иван");
        user.setEmail("ivanov@company.ru");
        user.setPassword("encoded");
        user.setRole(UserRole.EMPLOYEE);
        user.setDepartment(department());
        return user;
    }

    private Department department() {
        Department department = new Department();
        setId(department, 1L);
        department.setName("IT Service Desk");
        return department;
    }

    private void setId(BaseEntity entity, Long id) {
        try {
            Field field = BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
