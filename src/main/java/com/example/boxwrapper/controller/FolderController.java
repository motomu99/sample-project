package com.example.boxwrapper.controller;

import com.example.boxwrapper.model.request.FolderCreateRequest;
import com.example.boxwrapper.model.response.FolderInfoResponse;
import com.example.boxwrapper.service.BoxFolderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * フォルダ操作コントローラー
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/folders")
@RequiredArgsConstructor
@Validated
@Tag(name = "Folder Operations", description = "Box フォルダ操作 API")
@SecurityRequirement(name = "API Key")
public class FolderController {

    private final BoxFolderService folderService;

    @PostMapping
    @Operation(summary = "フォルダ作成", description = "新しいフォルダを作成")
    public ResponseEntity<FolderInfoResponse> createFolder(
            @Valid @RequestBody FolderCreateRequest request,
            HttpServletRequest httpRequest) {

        String apiKey = (String) httpRequest.getAttribute("apiKey");
        FolderInfoResponse response = folderService.createFolder(
            apiKey,
            request.getParentFolderId(),
            request.getFolderName()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{folderId}")
    @Operation(summary = "フォルダ情報取得", description = "フォルダのメタデータを取得")
    public ResponseEntity<FolderInfoResponse> getFolderInfo(
            @Parameter(description = "フォルダID", required = true)
            @PathVariable String folderId,
            HttpServletRequest request) {

        String apiKey = (String) request.getAttribute("apiKey");
        FolderInfoResponse response = folderService.getFolderInfo(apiKey, folderId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{folderId}/items")
    @Operation(summary = "フォルダ内アイテム一覧", description = "フォルダ内のアイテム一覧を取得")
    public ResponseEntity<List<String>> listFolderItems(
            @Parameter(description = "フォルダID", required = true)
            @PathVariable String folderId,
            HttpServletRequest request) {

        String apiKey = (String) request.getAttribute("apiKey");
        List<String> items = folderService.listFolderItems(apiKey, folderId);

        return ResponseEntity.ok(items);
    }

    @DeleteMapping("/{folderId}")
    @Operation(summary = "フォルダ削除", description = "指定されたフォルダを削除")
    public ResponseEntity<Void> deleteFolder(
            @Parameter(description = "フォルダID", required = true)
            @PathVariable String folderId,

            @Parameter(description = "再帰的削除")
            @RequestParam(defaultValue = "false") boolean recursive,

            HttpServletRequest request) {

        String apiKey = (String) request.getAttribute("apiKey");
        folderService.deleteFolder(apiKey, folderId, recursive);

        return ResponseEntity.noContent().build();
    }
}
