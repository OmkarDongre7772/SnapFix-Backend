package com.snapfix.common;

import org.springframework.mock.web.MockMultipartFile;

public class TestFixtures {

    public static MockMultipartFile createMockImage() {
        return new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "dummy-image-content".getBytes()
        );
    }
}
