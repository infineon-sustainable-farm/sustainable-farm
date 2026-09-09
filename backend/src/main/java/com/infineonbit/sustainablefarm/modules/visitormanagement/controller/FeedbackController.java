package com.infineonbit.sustainablefarm.modules.visitormanagement.controller;

import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.FeedbackRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.FeedbackResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.FeedbackSummaryResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.RouteFeedbackRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.SurveyRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.SurveyResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.SurveyStatus;
import com.infineonbit.sustainablefarm.modules.visitormanagement.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping("/feedback")
    public List<FeedbackResponse> list(@RequestParam(required = false) Long visitorId,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return feedbackService.listFeedback(visitorId, from, to);
    }

    @GetMapping("/feedback/summary")
    public FeedbackSummaryResponse summary(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return feedbackService.summary(from, to);
    }

    @PostMapping("/feedback")
    public ResponseEntity<FeedbackResponse> submit(@Valid @RequestBody FeedbackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(feedbackService.submitFeedback(request));
    }

    @PatchMapping("/feedback/{id}/route")
    public FeedbackResponse route(@PathVariable Long id, @Valid @RequestBody RouteFeedbackRequest request) {
        return feedbackService.routeFeedback(id, request);
    }

    @GetMapping("/surveys")
    public List<SurveyResponse> surveys(@RequestParam(required = false) Long visitorId,
                                        @RequestParam(required = false) SurveyStatus status) {
        return feedbackService.listSurveys(visitorId, status);
    }

    @PostMapping("/surveys")
    public ResponseEntity<SurveyResponse> send(@Valid @RequestBody SurveyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(feedbackService.sendSurvey(request));
    }
}