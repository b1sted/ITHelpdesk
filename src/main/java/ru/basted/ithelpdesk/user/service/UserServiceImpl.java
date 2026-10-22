package ru.basted.ithelpdesk.user.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.basted.ithelpdesk.core.exception.ConflictException;
import ru.basted.ithelpdesk.core.exception.NotFoundException;
import ru.basted.ithelpdesk.department.entity.Department;
import ru.basted.ithelpdesk.department.repository.DepartmentRepository;
import ru.basted.ithelpdesk.user.dto.UserCreateRequest;
import ru.basted.ithelpdesk.user.dto.UserResponse;
import ru.basted.ithelpdesk.user.dto.UserLimitsRequest;
import ru.basted.ithelpdesk.user.dto.UserUpdateRequest;
import ru.basted.ithelpdesk.user.entity.User;
import ru.basted.ithelpdesk.user.entity.UserRole;
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
    public List<UserResponse> getAgents() {
        return repository.findAllByRoleOrderByFullNameAsc(UserRole.AGENT)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return toResponse(findUser(id));
    }

    @Override
    @Transactional
    public UserResponse createUser(UserCreateRequest createRequest) {
        if (repository.existsByEmail(createRequest.email())) {
            throw new ConflictException(
                    "Пользователь с электронной почтой %s уже существует"
                            .formatted(createRequest.email())
            );
        }
        Department department = findDepartment(createRequest.departmentId());

        User user = mapper.toEntity(createRequest);
        user.setPassword(passwordEncoder.encode(createRequest.password()));
        user.setDepartment(department);
        user.setTicketLimit(10);

        return toResponse(repository.save(user));
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest updateRequest) {
        User user = findUser(id);

        if (updateRequest.fullName() != null) {
            user.setFullName(updateRequest.fullName());
        }

        if (updateRequest.email() != null) {
            String email = updateRequest.email();
            if (!email.equals(user.getEmail()) && repository.existsByEmail(email)) {
                throw new ConflictException(
                        "Пользователь с электронной почтой %s уже существует".formatted(email)
                );
            }
            user.setEmail(email);
        }

        if (updateRequest.role() != null) {
            user.setRole(updateRequest.role());
        }

        if (updateRequest.departmentId() != null) {
            user.setDepartment(findDepartment(updateRequest.departmentId()));
        }

        return toResponse(repository.save(user));
    }

    @Override
    @Transactional
    public UserResponse updateLimits(Long id, UserLimitsRequest request) {
        User user = findUser(id);

        if (user.getRole() != UserRole.AGENT) {
            throw new ConflictException("Лимиты применяются только к специалистам поддержки");
        }

        user.setTicketLimit(request.ticketLimit());
        user.setTimeLimitHours(request.timeLimitHours());
        return toResponse(repository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = findUser(id);

        try {
            repository.delete(user);
            repository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(
                    "Нельзя удалить пользователя, у которого есть заявки или сообщения"
            );
        }
    }

    private User findUser(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Не найден пользователь с ID %d".formatted(id)
                ));
    }

    private Department findDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new NotFoundException(
                        "Не найден департамент с ID %d".formatted(departmentId)
                ));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getDepartment().getName(),
                user.getTicketLimit(),
                user.getTimeLimitHours()
        );
    }
}
