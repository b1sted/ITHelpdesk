package ru.basted.ithelpdesk.user.service;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import ru.basted.ithelpdesk.core.exception.ConflictException;
import ru.basted.ithelpdesk.core.exception.NotFoundException;
import ru.basted.ithelpdesk.department.entity.Department;
import ru.basted.ithelpdesk.department.repository.DepartmentRepository;
import ru.basted.ithelpdesk.user.dto.UserCreateRequest;
import ru.basted.ithelpdesk.user.dto.UserResponse;
import ru.basted.ithelpdesk.user.dto.UserUpdateRequest;
import ru.basted.ithelpdesk.user.entity.User;
import ru.basted.ithelpdesk.user.mapper.UserMapper;
import ru.basted.ithelpdesk.user.repository.UserRepository;

@Service
public class UserServiceImpl implements UserService {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final int FULL_NAME_MAX_LENGTH = 150;
    private static final int EMAIL_MAX_LENGTH = 100;

    private final UserRepository repository;
    private final UserMapper mapper;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
            UserRepository repository,
            UserMapper mapper,
            DepartmentRepository departmentRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return repository.findAllByOrderByIdAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Не найден пользователь с ID " + id));

        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse createUser(UserCreateRequest createRequest) {
        if (repository.existsByEmail(createRequest.email())) {
            throw new ConflictException(
                    "Пользователь с электронной почтой " + createRequest.email() + " уже существует"
            );
        }
        validateDepartment(createRequest.departmentId());

        User user = mapper.toEntity(createRequest);
        user.setPassword(passwordEncoder.encode(createRequest.password()));
        User savedUser = repository.save(user);

        return toResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest updateRequest) {
        User user = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Не найден пользователь с ID " + id));

        if (updateRequest.fullName() != null) {
            user.setFullName(validateFullName(updateRequest.fullName()));
        }

        if (updateRequest.email() != null) {
            String email = validateEmail(updateRequest.email());
            if (!email.equals(user.getEmail()) && repository.existsByEmail(email)) {
                throw new ConflictException(
                        "Пользователь с электронной почтой " + email + " уже существует"
                );
            }
            user.setEmail(email);
        }

        if (updateRequest.role() != null) {
            user.setRole(updateRequest.role());
        }

        if (updateRequest.departmentId() != null) {
            validateDepartment(updateRequest.departmentId());
            user.setDepartmentId(updateRequest.departmentId());
        }

        return toResponse(repository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Не найден пользователь с ID " + id);
        }

        repository.deleteById(id);
    }

    private String validateFullName(String fullName) {
        if (!StringUtils.hasText(fullName)) {
            throw new IllegalArgumentException("Полное имя не может быть пустым");
        }
        if (fullName.length() > FULL_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("Полное имя не может быть длиннее " + FULL_NAME_MAX_LENGTH + " символов");
        }
        return fullName;
    }

    private String validateEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Электронная почта не может быть пустой");
        }
        if (email.length() > EMAIL_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Электронная почта не может быть длиннее " + EMAIL_MAX_LENGTH + " символов"
            );
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Некорректный формат электронной почты");
        }
        return email;
    }

    private void validateDepartment(Long departmentId) {
        if (!departmentRepository.existsById(departmentId)) {
            throw new NotFoundException("Не найден департамент с ID " + departmentId);
        }
    }

    private UserResponse toResponse(User user) {
        String departmentName = departmentRepository.findById(user.getDepartmentId())
                .map(Department::getName)
                .orElse(null);

        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                departmentName
        );
    }
}