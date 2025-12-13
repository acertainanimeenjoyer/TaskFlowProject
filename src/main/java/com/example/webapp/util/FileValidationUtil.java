package com.example.webapp.util;

import org.springframework.web.multipart.MultipartFile;

/**
 * Utility for file upload validation
 */
public class FileValidationUtil {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_MIME_TYPES = {
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    };

    /**
     * Validate the uploaded file
     * @param file the multipart file
     * @return validation error message or null if valid
     */
    public static String validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            return "File is empty";
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return "File size exceeds maximum allowed size of 5MB";
        }

        String contentType = file.getContentType();
        if (contentType == null || !isAllowedMimeType(contentType)) {
            return "File type not allowed. Allowed types: JPEG, PNG, GIF, WebP";
        }

        return null; // Valid
    }

    /**
     * Check if the MIME type is allowed
     * @param mimeType the MIME type to check
     * @return true if allowed, false otherwise
     */
    private static boolean isAllowedMimeType(String mimeType) {
        for (String allowed : ALLOWED_MIME_TYPES) {
            if (mimeType.equals(allowed)) {
                return true;
            }
        }
        return false;
    }
}
