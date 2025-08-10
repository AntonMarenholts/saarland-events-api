package de.saarland.events.controller;

import de.saarland.events.service.TranslationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/translate")
public class TranslateController {

    private final TranslationService translationService;

    public TranslateController(TranslationService translationService) {
        this.translationService = translationService;
    }


    public static class TranslateRequest {
        private String text;
        private String targetLang;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getTargetLang() { return targetLang; }
        public void setTargetLang(String targetLang) { this.targetLang = targetLang; }
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> performTranslate(@RequestBody TranslateRequest request) {
        String translatedText = translationService.translate(request.getText(), request.getTargetLang());

        Map<String, String> response = Map.of("translatedText", translatedText);
        return ResponseEntity.ok(response);
    }
}