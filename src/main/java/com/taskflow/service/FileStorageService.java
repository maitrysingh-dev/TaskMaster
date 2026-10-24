package com.taskflow.service;

import com.taskflow.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Stores uploads on the local disk. Files are saved under a random name so the client-supplied
 * file name never touches the file system (no path traversal, no collisions).
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final Path root;

    public FileStorageService(@Value("${app.storage.upload-dir}") String uploadDir) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload directory " + root, e);
        }
    }

    /** Saves the file and returns the generated storage name. */
    public String store(MultipartFile file) {
        String storedName = UUID.randomUUID().toString();
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, root.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store uploaded file", e);
        }
        return storedName;
    }

    public Resource load(String storedName) {
        Resource resource = new FileSystemResource(root.resolve(storedName));
        if (!resource.exists()) {
            throw new ResourceNotFoundException("The file is no longer available on the server");
        }
        return resource;
    }

    public void delete(String storedName) {
        try {
            Files.deleteIfExists(root.resolve(storedName));
        } catch (IOException e) {
            log.warn("Could not delete stored file {}: {}", storedName, e.getMessage());
        }
    }
}
