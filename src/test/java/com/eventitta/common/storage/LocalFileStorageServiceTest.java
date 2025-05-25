package com.eventitta.common.storage;

import com.eventitta.file.exception.FileStorageException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageServiceTest {

    private LocalFileStorageService service;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException, NoSuchFieldException, IllegalAccessException {
        tempDir = Files.createTempDirectory("test-uploads");
        service = new LocalFileStorageService();
        var field = LocalFileStorageService.class.getDeclaredField("storageLocation");
        field.setAccessible(true);
        field.set(service, tempDir.toString());
        service.init();
    }

    @AfterEach
    void tearDown() throws IOException {
        try (var paths = Files.walk(tempDir)) {
            paths
                .map(Path::toFile)
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(file -> {
                    if (!file.delete()) file.deleteOnExit();
                });
        }
    }

    @DisplayName("유효한 파일을 저장하고 업로드 URL을 반환한다")
    @Test
    void givenValidFile_whenStore_thenSavesAndReturnsUrl() {
        // given
        var file = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());

        // when
        String url = service.store(file);

        // then
        assertThat(url).startsWith("/uploads/");
        Path saved = tempDir.resolve(url.replace("/uploads/", ""));
        assertThat(Files.exists(saved)).isTrue();
    }

    @DisplayName("업로드된 파일 경로를 통해 파일을 정상적으로 삭제한다")
    @Test
    void givenFileUrl_whenDelete_thenRemovesFile() throws IOException {
        // given
        Path saved = tempDir.resolve("delete-me.txt");
        Files.writeString(saved, "dummy");
        String url = "/uploads/delete-me.txt";

        // when
        service.delete(url);

        // then
        assertThat(Files.notExists(saved)).isTrue();
    }

    @DisplayName("존재하는 파일명을 통해 Resource 객체를 반환한다")
    @Test
    void givenExistingFile_whenLoadAsResource_thenReturnsResource() throws IOException {
        // given
        String filename = "readme.txt";
        Path file = tempDir.resolve(filename);
        Files.writeString(file, "content");

        // when
        Resource resource = service.loadAsResource(filename);

        // then
        assertThat(resource.exists()).isTrue();
        assertThat(resource.isReadable()).isTrue();
        assertThat(resource.getFilename()).isEqualTo(filename);
    }

    @DisplayName("존재하지 않는 파일명을 요청하면 예외가 발생한다")
    @Test
    void givenMissingFile_whenLoadAsResource_thenThrowsException() {
        // expect
        assertThatThrownBy(() -> service.loadAsResource("nope.txt"))
            .isInstanceOf(FileStorageException.class)
            .hasMessageContaining("요청한 파일이 존재하지 않습니다.");
    }
}
