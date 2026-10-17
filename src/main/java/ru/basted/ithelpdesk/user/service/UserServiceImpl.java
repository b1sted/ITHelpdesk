package ru.basted.ithelpdesk.user.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            user.setFullName(updateRequest.fullName());
        }

        if (updateRequest.email() != null) {
            String email = updateRequest.email();
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