package com.snapfix.storage.service;

import com.cloudinary.Cloudinary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CloudinaryStorageService implements StorageService {

    private final Cloudinary cloudinary;

    private static final Logger log =
            LoggerFactory.getLogger(CloudinaryStorageService.class);

    public CloudinaryStorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public String uploadImage(MultipartFile file) {

        log.info("Uploading image to Cloudinary");

        try {

            @SuppressWarnings("rawtypes")
            Map uploadResult = cloudinary.uploader()
                    .upload(file.getBytes(), Map.of());

            log.info("Image uploaded successfully");

            return uploadResult.get("secure_url").toString();

        } catch (Exception e) {
            log.error("Image upload failed", e);
            throw new RuntimeException("Image upload failed", e);
        }
    }
}