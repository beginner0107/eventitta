package com.eventitta.file.controller;

import com.eventitta.ControllerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FileUploadControllerTest extends ControllerTestSupport {

    @DisplayName("파일 업로드 요청 시 파일 저장 후 URL 목록을 반환한다")
    @Test
    void givenMultipartFiles_whenUpload_thenReturnsFileUrls() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "files", "test.txt", MediaType.TEXT_PLAIN_VALUE, "hello".getBytes()
        );
        when(storageService.store(any())).thenReturn("/uploads/test.txt");

        // when & then
        mockMvc.perform(multipart("/api/v1/uploads").file(file))
            .andExpect(status().isOk())
            .andExpect(content().json("[\"/uploads/test.txt\"]"));
    }

    @DisplayName("파일 이름으로 조회 요청 시 파일을 리소스로 반환한다")
    @Test
    void givenFilename_whenGet_thenReturnsFileResource() throws Exception {
        // given
        Resource resource = new ByteArrayResource("file-content".getBytes()) {
            @Override
            public String getFilename() {
                return "test.txt";
            }
        };
        when(storageService.loadAsResource("test.txt")).thenReturn(resource);

        // when & then
        mockMvc.perform(get("/api/v1/uploads/test.txt"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "inline; filename=\"test.txt\""))
            .andExpect(content().bytes("file-content".getBytes()));
    }
}
