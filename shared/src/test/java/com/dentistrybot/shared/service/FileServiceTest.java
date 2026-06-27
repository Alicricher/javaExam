package com.dentistrybot.shared.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void saveFileStripsPathFromUploadedFilename() throws Exception {
        FileService service = new FileService(tempDir.toString());

        String relativePath = service.saveFile("U1", 2, "../evil.pdf", "data".getBytes());

        assertThat(relativePath).isEqualTo(Path.of("U1", "2", "evil.pdf").toString());
        assertThat(Files.readString(tempDir.resolve(relativePath))).isEqualTo("data");
        assertThat(Files.exists(tempDir.resolve("evil.pdf"))).isFalse();
    }

    @Test
    void getFilePathRejectsTraversalOutsideMaterialsRoot() {
        FileService service = new FileService(tempDir.toString());

        assertThatThrownBy(() -> service.getFilePath("../secret.txt"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid file path");
    }

    @Test
    void deleteFileDoesNotDeleteOutsideRoot() {
        FileService service = new FileService(tempDir.toString());

        assertThatThrownBy(() -> service.deleteFile("../secret.txt"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
