package com.example.boxwrapper.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 一括アップロード結果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUploadResult {

    private int total;

    private int successful;

    private int failed;

    @Builder.Default
    private List<FileUploadResponse> successfulFiles = new ArrayList<>();

    @Builder.Default
    private List<FailedFileInfo> failedFiles = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedFileInfo {
        private String fileName;
        private String errorMessage;
    }
}
