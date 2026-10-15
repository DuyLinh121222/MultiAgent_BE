package com.mael.controller;

import com.mael.dto.photo.PhotoProblemView;
import com.mael.service.PhotoProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Giải toán bằng ảnh.
 */
@RestController
@RequestMapping("/api/photo-problems")
@RequiredArgsConstructor
public class PhotoProblemController {

    private final PhotoProblemService photoProblemService;

    /** multipart/form-data: image (file), note (text, không bắt buộc - đề bài học sinh gõ thêm) */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PhotoProblemView upload(@RequestPart("image") MultipartFile image,
                                   @RequestParam(value = "note", required = false) String note) {
        return photoProblemService.upload(image, note);
    }

    @GetMapping
    public List<PhotoProblemView> mine() {
        return photoProblemService.myProblems();
    }

    @GetMapping("/{id}")
    public PhotoProblemView get(@PathVariable String id) {
        return photoProblemService.get(id);
    }
}
