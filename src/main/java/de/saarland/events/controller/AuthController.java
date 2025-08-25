package de.saarland.events.controller;

import de.saarland.events.dto.auth.JwtResponse;
import de.saarland.events.dto.auth.LoginRequest;
import de.saarland.events.dto.auth.MessageResponse;
import de.saarland.events.dto.auth.SignupRequest;
import de.saarland.events.model.ERole;
import de.saarland.events.model.User;
import de.saarland.events.repository.UserRepository;
import de.saarland.events.security.jwt.JwtUtils;
import de.saarland.events.security.services.UserDetailsImpl;
import de.saarland.events.service.EmailService;
import de.saarland.events.service.RecaptchaService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RecaptchaService recaptchaService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    private final EmailService emailService;

    public AuthController(AuthenticationManager authenticationManager, UserRepository userRepository, PasswordEncoder encoder, JwtUtils jwtUtils, EmailService emailService, RecaptchaService recaptchaService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
        this.emailService = emailService;
        this.recaptchaService = recaptchaService;
    }

    @PostMapping("/signin")
    @RateLimiter(name = "loginLimiter")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        if (!recaptchaService.verify(loginRequest.getRecaptchaToken())) {
            return ResponseEntity
                    .status(401)
                    .body(new MessageResponse("Error: reCAPTCHA validation failed."));
        }
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (!recaptchaService.verify(signUpRequest.getRecaptchaToken())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: reCAPTCHA validation failed."));
        }

        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }
        User user = new User(signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()),
                ERole.ROLE_USER);

        userRepository.save(user);
        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<JwtResponse> getUserProfile(Authentication authentication, HttpServletRequest request) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        String token = parseJwt(request);

        return ResponseEntity.ok(new JwtResponse(
                token,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles
        ));
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String recaptchaToken = request.get("recaptchaToken");
        if (!recaptchaService.verify(recaptchaToken)) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: reCAPTCHA validation failed."));
        }
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            String token = UUID.randomUUID().toString();
            user.setResetPasswordToken(token);
            user.setTokenCreationDate(LocalDateTime.now());
            userRepository.save(user);

            String resetLink = "https://saarland-event-front-hq61.vercel.app/reset-password?token=" + token;
            emailService.sendPasswordResetEmail(user, resetLink);
        }

        return ResponseEntity.ok(new MessageResponse("If your email is in our system, you will receive a password reset link."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam("token") String token, @RequestBody Map<String, String> request) {
        String newPassword = request.get("password");
        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Password must be at least 6 characters long!"));
        }

        Optional<User> userOptional = userRepository.findByResetPasswordToken(token);

        if (userOptional.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Invalid token!"));
        }

        User user = userOptional.get();

        if (user.getTokenCreationDate() != null && user.getTokenCreationDate().plusHours(24).isBefore(LocalDateTime.now())) {
            user.setResetPasswordToken(null);
            user.setTokenCreationDate(null);
            userRepository.save(user);
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Token expired!"));
        }

        user.setPassword(encoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setTokenCreationDate(null);
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("Password has been reset successfully."));
    }
}
