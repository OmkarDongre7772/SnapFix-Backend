package com.snapfix.storage.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String uploadImage(MultipartFile file);

}