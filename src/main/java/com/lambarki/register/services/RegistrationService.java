package com.lambarki.register.services;

import com.lambarki.register.dtos.RegistrationDto;
import com.lambarki.register.models.ApplicationUser;
import com.lambarki.register.models.Token;
import com.lambarki.register.repositories.TokenRepository;
import com.lambarki.register.repositories.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.lambarki.register.models.UserRole.ROLE_USER;
import static java.time.LocalDateTime.*;
import static java.util.UUID.randomUUID;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private static final String CONFIRMATION_URL = "http://localhost:8080/api/v1/authentication/confirm?token=%s";
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;


    @Transactional
    public String register(RegistrationDto registrationDto) {
        // check if the user already exists
        boolean userExists = userRepository.findByEmail(registrationDto.getEmail()).isPresent();
        if (userExists) {
            throw new IllegalStateException("A user already exists with the same email");
        }

        // encode the password
        String encodedPassword = passwordEncoder.encode(registrationDto.getPassword());

        // transform - map the registrationDto to ApplicationDto
        ApplicationUser applicationUser = ApplicationUser.builder()
                .firstname(registrationDto.getFirstname())
                .lastname(registrationDto.getLastname())
                .email(registrationDto.getEmail())
                .password(encodedPassword)
                .role(ROLE_USER)
                .build();

        //save the user
        ApplicationUser savedUser = userRepository.save(applicationUser);

        // Generate a token
        String generatedToken = randomUUID().toString();
        Token token = Token.builder()
                .token(generatedToken)
                .createdAt(now())
                .expiredAt(now().plusMinutes(10))
                .user(savedUser)
                .build();
        tokenRepository.save(token);

        // send the confirmation email
        try {
            emailService.send(
                    registrationDto.getEmail(),
                    registrationDto.getFirstname(),
                    null,
                    String.format(CONFIRMATION_URL, generatedToken)
            );
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }


        // return success message
        return generatedToken;
    }

    public String confirm(String token) {
        // get token
        Token savedToken = tokenRepository.findByToken(token).orElseThrow(() -> new IllegalStateException("Token not found"));
        if (LocalDateTime.now().isAfter(savedToken.getExpiredAt())) {
            String generatedToken = randomUUID().toString();
            Token newToken = Token.builder()
                    .token(generatedToken)
                    .createdAt(now())
                    .expiredAt(now().plusMinutes(10))
                    .user(savedToken.getUser())
                    .build();
            tokenRepository.save(newToken);


            // send the confirmation email
            try {
                emailService.send(
                        savedToken.getUser().getEmail(),
                        savedToken.getUser().getFirstname(),
                        null,
                        String.format(CONFIRMATION_URL, generatedToken)
                );
            } catch (MessagingException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            return "Token expired, a new token has been sent to your email";
        }
        ApplicationUser user = userRepository.findById(savedToken.getUser().getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        user.setEnabled(true);
        userRepository.save(user);

        savedToken.setValidateAt(now());
        tokenRepository.save(savedToken);
        return "<h1>Your account has been successfully activated</h1>";
    }
}
