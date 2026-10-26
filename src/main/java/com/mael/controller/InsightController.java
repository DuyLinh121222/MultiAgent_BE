package com.mael.controller;

import com.mael.dto.insight.InsightView;
import com.mael.service.InsightService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Phân tích cá nhân hoá kết quả học (AI hoặc theo luật) */
@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightController {

    private final InsightService insightService;

    @GetMapping("/me")
    public InsightView me(@RequestParam(defaultValue = "false") boolean refresh) {
        return insightService.myInsight(refresh);
    }
}
