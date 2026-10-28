package com.mael.controller;

import com.mael.dto.lead.LeadRequest;
import com.mael.dto.lead.LeadView;
import com.mael.entity.enums.LeadStatus;
import com.mael.service.LeadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    /** Trung tâm / trường học gửi yêu cầu tư vấn (public) */
    @PostMapping("/leads")
    @ResponseStatus(HttpStatus.CREATED)
    public LeadView create(@Valid @RequestBody LeadRequest req, jakarta.servlet.http.HttpServletRequest http) {
        return leadService.create(req, com.mael.service.security.ClientIp.of(http));
    }

    /** Admin xem danh sách yêu cầu tư vấn */
    @GetMapping("/admin/leads")
    public List<LeadView> all() {
        return leadService.all();
    }

    @PatchMapping("/admin/leads/{id}")
    public LeadView updateStatus(@PathVariable String id,
                                 @RequestParam(required = false) LeadStatus status,
                                 @RequestParam(required = false) String adminNote) {
        return leadService.updateStatus(id, status, adminNote);
    }
}
