package com.example.finsight_backend.config;

import com.example.finsight_backend.entity.Company;
import com.example.finsight_backend.entity.Role;
import com.example.finsight_backend.entity.User;
import com.example.finsight_backend.repository.CompanyRepository;
import com.example.finsight_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed-demo:false}")
    private boolean seedDemo;

    @Override
    public void run(String... args) throws Exception {
        if (!seedDemo) return;

        // Seed a couple of companies if missing
        seedCompany("AAPL", "Apple Inc.");
        seedCompany("MSFT", "Microsoft Corp.");

        // Seed a demo user for local development
        String email = "demo@finsight.local";
        if (userRepository.findByEmail(email).isEmpty()) {
            User u = User.builder()
                    .firstName("Demo")
                    .lastName("User")
                    .email(email)
                    .password(passwordEncoder.encode("demo123"))
                    .role(Role.USER)
                    .build();
            userRepository.save(u);
            log.info("Seeded demo user: {} (password: demo123)", email);
        }
    }

    private void seedCompany(String ticker, String name) {
        companyRepository.findById(ticker).orElseGet(() -> {
            Company c = new Company();
            c.setTicker(ticker);
            c.setName(name);
            c.setSector("Technology");
            c.setCountry("US");
            c.setMarketCap(1_000_000_000_000L);
            log.info("Seeding company {}", ticker);
            return companyRepository.save(c);
        });
    }
}

