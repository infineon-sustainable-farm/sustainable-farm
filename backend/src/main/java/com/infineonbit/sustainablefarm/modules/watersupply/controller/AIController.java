package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.service.AIService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controleur IA - mince, delegue la logique metier a AIService.
 */
@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/recommendations")
    public List<Map<String, Object>> recommendations() {
        return aiService.recommendations();
    }

    @GetMapping("/drought-prediction")
    public Map<String, Object> droughtPrediction() {
        return aiService.droughtPrediction();
    }

    @PostMapping("/analyze")
    public Map<String, Object> analyze() {
        return aiService.analyze();
    }
}
