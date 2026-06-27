package com.dentistrybot.shared.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final String materialsPath;

    public FileService(String materialsPath) {
        this.materialsPath = materialsPath;
    }

    public String getFilePath(String relativePath) {
        Path root = Paths.get(materialsPath).toAbsolutePath().normalize();
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Invalid file path");
        }
        return resolved.toString();
    }

    public boolean fileExists(String relativePath) {
        return new File(getFilePath(relativePath)).exists();
    }

    public void sendDocument(TelegramClient client, long chatId, String relativePath, String caption) throws Exception {
        String fullPath = getFilePath(relativePath);
        File file = new File(fullPath);
        if (!file.exists()) throw new IOException("File not found: " + fullPath);

        SendDocument msg = SendDocument.builder()
            .chatId(chatId)
            .document(new InputFile(file))
            .caption(caption)
            .build();

        client.execute(msg);
    }

    public void ensureMaterialsDir(String unitName, int lessonNum) throws IOException {
        Path dir = Paths.get(getFilePath(Paths.get(sanitizePathSegment(unitName), String.valueOf(lessonNum)).toString()));
        Files.createDirectories(dir);
    }

    public String saveFile(String unitName, int lessonNum, String filename, byte[] data) throws IOException {
        String safeUnitName = sanitizePathSegment(unitName);
        ensureMaterialsDir(safeUnitName, lessonNum);
        String safeName = sanitizeFilename(filename);
        String relativePath = Paths.get(safeUnitName, String.valueOf(lessonNum), safeName).toString();
        Path fullPath = Paths.get(getFilePath(relativePath));
        Files.write(fullPath, data);
        return relativePath;
    }

    public void deleteFile(String relativePath) throws IOException {
        Path fullPath = Paths.get(getFilePath(relativePath));
        Files.deleteIfExists(fullPath);
    }

    private String sanitizeFilename(String filename) {
        String name = Paths.get(filename == null || filename.isBlank() ? "file" : filename)
            .getFileName()
            .toString()
            .replace('\\', '_')
            .replace('/', '_');
        return name.isBlank() ? "file" : name;
    }

    private String sanitizePathSegment(String value) {
        String segment = Paths.get(value == null || value.isBlank() ? "unknown" : value)
            .getFileName()
            .toString()
            .replace('\\', '_')
            .replace('/', '_');
        return segment.isBlank() ? "unknown" : segment;
    }
}
