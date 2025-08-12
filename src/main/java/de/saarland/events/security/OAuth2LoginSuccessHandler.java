package de.saarland.events.security;

import de.saarland.events.model.ERole;
import de.saarland.events.model.User;
import de.saarland.events.repository.UserRepository;
import de.saarland.events.security.jwt.JwtUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    @Value("${app.oauth2.redirectUri}")
    private String redirectUri;

    public OAuth2LoginSuccessHandler(UserRepository userRepository, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        DefaultOAuth2User oauthUser = (DefaultOAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oauthUser.getAttributes();
        String email = (String) attributes.get("email");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);


                    String username = attributes.get("name") != null ? (String) attributes.get("name") : email.split("@")[0];
                    newUser.setUsername(username);

                    newUser.setRole(ERole.ROLE_USER);

                    newUser.setPassword(UUID.randomUUID().toString());
                    return userRepository.save(newUser);
                });


        String jwt = jwtUtils.generateTokenFromUsername(user.getUsername());


        response.sendRedirect(redirectUri + "?token=" + jwt);
    }
}