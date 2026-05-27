package com.dentistrybot.shared.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.GetFile;
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
        return Paths.get(materialsPath, relativePath).toString();
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
        Path dir = Paths.get(materialsPath, unitName, String.valueOf(lessonNum));
        Files.createDirectories(dir);
    }

    public String saveFile(String unitName, int lessonNum, String filename, byte[] data) throws IOException {
        ensureMaterialsDir(unitName, lessonNum);
        String relativePath = Paths.get(unitName, String.valueOf(lessonNum), filename).toString();
        Path fullPath = Paths.get(materialsPath, relativePath);
        Files.write(fullPath, data);
        return relativePath;
    }

    public void deleteFile(String relativePath) throws IOException {
        Path fullPath = Paths.get(materialsPath, relativePath);
        Files.deleteIfExists(fullPath);
    }
}
