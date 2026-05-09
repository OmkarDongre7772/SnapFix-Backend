package com.snapfix.storage.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.snapfix.storage.service.StorageService;

@RestController
@RequestMapping("/test/storage")
public class StorageTestController {
    
    private final StorageService storageService;
    public StorageTestController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/upload")
    public String upload(@RequestParam MultipartFile file) {
        return storageService.uploadImage(file);
    }

}
