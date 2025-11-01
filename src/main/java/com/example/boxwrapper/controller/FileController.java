package com.example.boxwrapper.controller;

import com.example.boxwrapper.model.response.FileInfoResponse;
import com.example.boxwrapper.model.response.FileUploadResponse;
import com.example.boxwrapper.service.BoxFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * ファイル操作コントローラー
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Validated
@Tag(name = "File Operations", description = "Box ファイル操作 API")
@SecurityRequirement(name = "API Key")
public class FileController {

    private final BoxFileService fileService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "ファイルアップロード", description = "指定されたフォルダにファイルをアップロード")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @Parameter(description = "フォルダID", required = true)
            @RequestParam @NotBlank String folderId,

            @Parameter(description = "アップロードファイル", required = true)
            @RequestParam @NotBlank MultipartFile file,

            HttpServletRequest request) {

        String apiKey = (String) request.getAttribute("apiKey");
        FileUploadResponse response = fileService.uploadFile(apiKey, folderId, file);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{fileId}")
    @Operation(summary = "ファイル情報取得", description = "ファイルのメタデータを取得")
    public ResponseEntity<FileInfoResponse> getFileInfo(
            @Parameter(description = "ファイルID", required = true)
            @PathVariable String fileId,

            HttpServletRequest request) {

        String apiKey = (String) request.getAttribute("apiKey");
        FileInfoResponse response = fileService.getFileInfo(apiKey, fileId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{fileId}/download")
    @Operation(summary = "ファイルダウンロード", description = "ファイルの内容をダウンロード")
    public ResponseEntity<byte[]> downloadFile(
            @Parameter(description = "ファイルID", required = true)
            @PathVariable String fileId,

            HttpServletRequest request) {

        String apiKey = (String) request.getAttribute("apiKey");
        byte[] fileContent = fileService.downloadFile(apiKey, fileId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "file");

        return ResponseEntity.ok()
            .headers(headers)
            .body(fileContent);
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "ファイル削除", description = "指定されたファイルを削除")
    public ResponseEntity<Void> deleteFile(
            @Parameter(description = "ファイルID", required = true)
            @PathVariable String fileId,

            HttpServletRequest request) {

        String apiKey = (String) request.getAttribute("apiKey");
        fileService.deleteFile(apiKey, fileId);

        return ResponseEntity.noContent().build();
    }
}
