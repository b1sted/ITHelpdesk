package ru.basted.ithelpdesk.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
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

    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminFullName;

    public DBInitializer(
            DepartmentRepository departmentRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.admin-email:admin@company.ru}") String adminEmail,
            @Value("${app.seed.admin-password:Admin2024!}") String adminPassword,
            @Value("${app.seed.admin-full-name:}") String adminFullName
    ) {
        this.departmentRepository = departmentRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminFullName = adminFullName.isBlank() ? DEFAULT_ADMIN_FULL_NAME : adminFullName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Department department = seedDepartment();
        seedCategories();
        seedAdmin(department);
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
        admin.setDepartmentId(department.getId());
        userRepository.save(admin);
    }
}