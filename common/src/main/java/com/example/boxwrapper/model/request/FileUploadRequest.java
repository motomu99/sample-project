package com.example.boxwrapper.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * ファイルアップロードリクエスト
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadRequest {

    @NotBlank(message = "フォルダIDは必須です")
    private String folderId;

    @NotBlank(message = "ファイル名は必須です")
    private String fileName;

    @NotNull(message = "ファイルは必須です")
    private MultipartFile file;

    public FileUploadRequest(String folderId, String fileName, byte[] content) {
        this.folderId = folderId;
        this.fileName = fileName;
        // For test purposes
    }
}
