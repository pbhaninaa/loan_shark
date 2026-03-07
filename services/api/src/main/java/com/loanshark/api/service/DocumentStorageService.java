package com.loanshark.api.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Service
public class DocumentStorageService {

    private static final Set<String> PDF_TYPES = Set.of("application/pdf");
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/jpg", "image/png");

    private final Path storageDirectory;

    public DocumentStorageService(@Value("${app.storage.kyc-dir}") String storageDirectory) {
        this.storageDirectory = Paths.get(storageDirectory).toAbsolutePath().normalize();
    }

    public StoredFile storePdf(MultipartFile file) {
        validateFile(file, PDF_TYPES, ".pdf");
        return store(file, ".pdf");
    }

    public StoredFile storeImage(MultipartFile file) {
        validateFile(file, IMAGE_TYPES, null);
        String extension = extension(file.getOriginalFilename());
        if (extension.isBlank()) {
            extension = ".jpg";
        }
        return store(file, extension);
    }

    private void validateFile(MultipartFile file, Set<String> allowedTypes, String requiredExtension) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Required file is missing");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!allowedTypes.contains(contentType)) {
            throw new ResponseStatusException(BAD_REQUEST, "Unsupported file type: " + contentType);
        }
        if (requiredExtension != null && !extension(file.getOriginalFilename()).equalsIgnoreCase(requiredExtension)) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid file extension");
        }
    }

    private StoredFile store(MultipartFile file, String extension) {
        try {
            Files.createDirectories(storageDirectory);
            String fileName = UUID.randomUUID() + extension;
            Path target = storageDirectory.resolve(fileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredFile(
                target.toString(),
                file.getOriginalFilename() == null ? fileName : file.getOriginalFilename(),
                file.getContentType() == null ? "application/octet-stream" : file.getContentType(),
                file.getSize(),
                sha256(target)
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Could not store uploaded file");
        }
    }

    private String sha256(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Files.readAllBytes(path));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException exception) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Could not hash uploaded file");
        }
    }

    private String extension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.')).toLowerCase();
    }

    public record StoredFile(
        String path,
        String originalFileName,
        String contentType,
        long size,
        String sha256
    ) {
    }
}
