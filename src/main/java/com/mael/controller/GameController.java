package com.mael.controller;

import com.mael.dto.game.*;
import com.mael.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Bản đồ ải, đi ải, nhận thưởng, thú đồng hành và màn chào mừng của học sinh.
 */
@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    /** Bản đồ ải theo khối lớp của học sinh */
    @GetMapping("/map")
    public MapResponse map() {
        return gameService.myMap();
    }

    @GetMapping("/profile")
    public GameProfile profile() {
        return gameService.myProfile();
    }

    /** Vào ải thường: lý thuyết + 3 câu thử thách */
    @GetMapping("/stages/{lessonId}")
    public StageDetail lessonStage(@PathVariable String lessonId) {
        return gameService.lessonStage(lessonId);
    }

    @PostMapping("/stages/{lessonId}/submit")
    public StageResultView submitLesson(@PathVariable String lessonId, @Valid @RequestBody SubmitStageRequest req) {
        return gameService.submitLesson(lessonId, req);
    }

    /** Vào ải trùm cuối chương: 5 câu ngẫu nhiên */
    @GetMapping("/bosses/{topicId}")
    public StageDetail bossStage(@PathVariable String topicId) {
        return gameService.bossStage(topicId);
    }

    @PostMapping("/bosses/{topicId}/submit")
    public StageResultView submitBoss(@PathVariable String topicId, @Valid @RequestBody SubmitStageRequest req) {
        return gameService.submitBoss(topicId, req);
    }

    /** Bộ sưu tập thú (đã có / chưa có / giá xu) */
    @GetMapping("/pets")
    public List<PetView> pets() {
        return gameService.myPets();
    }

    /** 3 thú khởi đầu cho màn chào mừng */
    @GetMapping("/pets/starters")
    public List<PetView> starters() {
        return gameService.starterPets();
    }

    @PostMapping("/pets/{petCode}/buy")
    public List<PetView> buy(@PathVariable String petCode) {
        return gameService.buyPet(petCode);
    }

    @PutMapping("/pets/{petCode}/active")
    public GameProfile setActive(@PathVariable String petCode) {
        return gameService.setActivePet(petCode);
    }

    /** Hoàn tất màn chào mừng 3 bước */
    @PostMapping("/onboarding")
    public GameProfile onboarding(@Valid @RequestBody OnboardingRequest req) {
        return gameService.completeOnboarding(req);
    }
}
