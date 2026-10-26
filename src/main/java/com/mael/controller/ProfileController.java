package com.mael.controller;

import com.mael.dto.profile.ChangePasswordRequest;
import com.mael.dto.profile.ProfileView;
import com.mael.dto.profile.UpdateProfileRequest;
import com.mael.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Hồ sơ học sinh */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/profile")
    public ProfileView profile() {
        return profileService.myProfile();
    }

    @PutMapping("/profile")
    public ProfileView update(@Valid @RequestBody UpdateProfileRequest req) {
        return profileService.update(req);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        profileService.changePassword(req);
    }
}
