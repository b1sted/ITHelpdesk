package ru.basted.ithelpdesk.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.basted.ithelpdesk.auth.dto.AuthResponse;
import ru.basted.ithelpdesk.auth.dto.LoginRequest;
import ru.basted.ithelpdesk.auth.security.JwtService;
import ru.basted.ithelpdesk.auth.security.LoginRateLimiter;
import ru.basted.ithelpdesk.core.exception.NotFoundException;
import ru.basted.ithelpdesk.user.entity.User;
import ru.basted.ithelpdesk.user.repository.UserRepository;

@Service
public class AuthServiceImpl implements AuthService {
    private static final String INVALID_CREDENTIALS = "Неверные учётные данные";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimiter rateLimiter;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            LoginRateLimiter rateLimiter
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rateLimiter = rateLimiter;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest loginRequest) {
        String key = "login:" + loginRequest.email().toLowerCase().trim();
        rateLimiter.check(key);

        User user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new NotFoundException(INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(loginRequest.password(), user.getPassword())) {
            throw new NotFoundException(INVALID_CREDENTIALS);
        }

        rateLimiter.reset(key);
        String token = jwtService.generateToken(user);

        return new AuthResponse(
                token,
                user.getEmail(),
                user.getFullName(),
                user.getRole().name()
        );
    }
}