package de.saarland.events.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
public class RecaptchaService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.recaptcha.secret}")
    private String recaptchaSecret;

    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    private static final double RECAPTCHA_SCORE_THRESHOLD = 0.5;

    public boolean verify(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        String url = String.format("%s?secret=%s&response=%s", RECAPTCHA_VERIFY_URL, recaptchaSecret, token);

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) {
                return false;
            }

            boolean success = (boolean) response.get("success");
            double score = (double) response.getOrDefault("score", 0.0);

            return success && score >= RECAPTCHA_SCORE_THRESHOLD;
        } catch (Exception e) {
            // Логирование ошибки
            return false;
        }
    }
}