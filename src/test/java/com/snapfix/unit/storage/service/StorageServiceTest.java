package com.snapfix.unit.storage.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.snapfix.common.TestFixtures;
import com.snapfix.storage.service.CloudinaryStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StorageServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @InjectMocks
    private CloudinaryStorageService storageService;

    @Test
    @DisplayName("Upload image returns secure URL when Cloudinary succeeds")
    void uploadImage_success_returnsUrl() throws Exception {
        // Given
        MultipartFile file = TestFixtures.createMockImage();
        String expectedUrl = "https://res.cloudinary.com/demo/image/upload/sample.jpg";
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap()))
                .thenReturn(Map.of("secure_url", expectedUrl));

        // When
        String result = storageService.uploadImage(file);

        // Then
        assertThat(result).isEqualTo(expectedUrl);
    }

    @Test
    @DisplayName("Upload image throws RuntimeException when Cloudinary fails")
    void uploadImage_cloudinaryError_throwsException() throws Exception {
        // Given
        MultipartFile file = TestFixtures.createMockImage();
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap()))
                .thenThrow(new IOException("Cloudinary error"));

        // When / Then
        assertThatThrownBy(() -> storageService.uploadImage(file))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Image upload failed");
    }
}
