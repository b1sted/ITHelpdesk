package ru.basted.ithelpdesk.user.service;

import java.util.List;

import ru.basted.ithelpdesk.user.dto.UserCreateRequest;
import ru.basted.ithelpdesk.user.dto.UserResponse;
import ru.basted.ithelpdesk.user.dto.UserLimitsRequest;
import ru.basted.ithelpdesk.user.dto.UserUpdateRequest;

public interface UserService {
    List<UserResponse> getAllUsers();

    List<UserResponse> getAgents();

    UserResponse getUserById(Long id);

    UserResponse createUser(UserCreateRequest createRequest);

    UserResponse updateUser(Long id, UserUpdateRequest updateRequest);

    UserResponse updateLimits(Long id, UserLimitsRequest request);

    void deleteUser(Long id);
}
