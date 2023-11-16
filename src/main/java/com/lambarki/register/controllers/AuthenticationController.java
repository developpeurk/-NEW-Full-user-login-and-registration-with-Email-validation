package com.lambarki.register.controllers;

import com.lambarki.register.dtos.RegistrationDto;
import com.lambarki.register.services.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/authentication")
@RequiredArgsConstructor
public class AuthenticationController {
    private final RegistrationService service;

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @RequestBody RegistrationDto dto
            ) {
        return ResponseEntity.ok(service.register(dto));

    }

    @GetMapping("/confirm")
    public ResponseEntity<String> confirm(
            @RequestParam String token
    ) {
        return ResponseEntity.ok(service.confirm(token));
    }
}
