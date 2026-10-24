package ru.basted.ithelpdesk.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ru.basted.ithelpdesk.category.entity.Category;
import ru.basted.ithelpdesk.category.repository.CategoryRepository;
import ru.basted.ithelpdesk.department.entity.Department;
import ru.basted.ithelpdesk.department.repository.DepartmentRepository;
import ru.basted.ithelpdesk.user.entity.User;
import ru.basted.ithelpdesk.user.entity.UserRole;
import ru.basted.ithelpdesk.user.repository.UserRepository;

@Component
public class DBInitializer implements ApplicationRunner {
    private static final String DEFAULT_DEPARTMENT = "IT Service Desk";
    private static final String DEFAULT_ADMIN_FULL_NAME = "Главный администратор";

    private static final String[] DEFAULT_CATEGORIES = {
            "Оборудование",
            "Программное обеспечение",
            "Сеть и доступ",
            "Учётные записи и доступы"
    };

    private static final String TICKET_STATUS_CONSTRAINT = """
            ALTER TABLE tickets ADD CONSTRAINT tickets_status_check
            CHECK (status IN ('NEW', 'IN_PROGRESS', 'WAITING_USER', 'ANSWERED', 'RESOLVED', 'CLOSED'))
            """;

    private static final String USER_ROLE_CONSTRAINT = """
            ALTER TABLE users ADD CONSTRAINT users_role_check
            CHECK (role IN ('EMPLOYEE', 'AGENT', 'MANAGER', 'ADMIN'))
            """;

    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminFullName;

    public DBInitializer(
            DepartmentRepository departmentRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.admin-email:admin@company.ru}") String adminEmail,
            @Value("${app.seed.admin-password:Admin2024!}") String adminPassword,
            @Value("${app.seed.admin-full-name:}") String adminFullName
    ) {
        this.departmentRepository = departmentRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminFullName = adminFullName.isBlank() ? DEFAULT_ADMIN_FULL_NAME : adminFullName;
    }

    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments args) {
        refreshTicketStatusConstraint();
        refreshUserRoleConstraint();
        migrateAgentLimits();
        migrateTicketExtensions();
        migrateTicketLifecycleTimestamps();
        seedSupportSettings();
        Department department = seedDepartment();
        seedCategories();
        seedAdmin(department);
    }

    private void refreshTicketStatusConstraint() {
        jdbcTemplate.execute("ALTER TABLE tickets DROP CONSTRAINT IF EXISTS tickets_status_check");
        jdbcTemplate.execute(TICKET_STATUS_CONSTRAINT);
    }

    private void refreshUserRoleConstraint() {
        jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
        jdbcTemplate.execute(USER_ROLE_CONSTRAINT);
    }

    private void migrateAgentLimits() {
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS ticket_limit INTEGER");
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS time_limit_hours INTEGER");
        jdbcTemplate.execute("UPDATE users SET ticket_limit = 10 WHERE ticket_limit IS NULL");
        jdbcTemplate.execute(
                "UPDATE users SET time_limit_hours = 24 WHERE time_limit_hours IS NULL AND role = 'AGENT'"
        );
    }

    private void migrateTicketExtensions() {
        jdbcTemplate.execute("ALTER TABLE tickets ADD COLUMN IF NOT EXISTS extension_hours INTEGER");
        jdbcTemplate.execute("UPDATE tickets SET extension_hours = 0 WHERE extension_hours IS NULL");
    }

    private void migrateTicketLifecycleTimestamps() {
        jdbcTemplate.execute("""
                UPDATE tickets t
                SET resolved_at = COALESCE(
                    (SELECT MAX(m.created_at) FROM messages m WHERE m.ticket_id = t.id),
                    t.created_at
                )
                WHERE t.status IN ('RESOLVED', 'CLOSED') AND t.resolved_at IS NULL
                """);
        jdbcTemplate.execute("""
                UPDATE tickets t
                SET closed_at = COALESCE(t.resolved_at, t.created_at)
                WHERE t.status = 'CLOSED' AND t.closed_at IS NULL
                """);
    }

    private void seedSupportSettings() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM support_settings",
                Integer.class
        );
        if (count == null || count == 0) {
            jdbcTemplate.update(
                    "INSERT INTO support_settings (auto_close_resolved_hours) VALUES (0)"
            );
        }
    }

    private Department seedDepartment() {
        return departmentRepository.findByName(DEFAULT_DEPARTMENT)
                .orElseGet(() -> {
                    Department department = new Department();
                    department.setName(DEFAULT_DEPARTMENT);
                    return departmentRepository.save(department);
                });
    }

    private void seedCategories() {
        for (String name : DEFAULT_CATEGORIES) {
            if (!categoryRepository.existsByName(name)) {
                Category category = new Category();
                category.setName(name);
                categoryRepository.save(category);
            }
        }
    }

    private void seedAdmin(Department department) {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        User admin = new User();
        admin.setFullName(adminFullName);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(UserRole.ADMIN);
        admin.setDepartment(department);
        userRepository.save(admin);
    }
}