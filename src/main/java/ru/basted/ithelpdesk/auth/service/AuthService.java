package ru.basted.ithelpdesk.auth.service;

import ru.basted.ithelpdesk.auth.dto.AuthResponse;
import ru.basted.ithelpdesk.auth.dto.LoginRequest;

public interface AuthService {
    AuthResponse login(LoginRequest loginRequest);
}