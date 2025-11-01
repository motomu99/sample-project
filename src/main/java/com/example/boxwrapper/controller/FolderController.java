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
 * フォルダ操作コントローラー.
 *
 * <p>Box フォルダに関するREST APIエンドポイントを提供します。
 * 作成、一覧取得、情報取得、削除の操作をサポートします。</p>
 *
 * <p>全てのエンドポイントはAPIキー認証（X-API-Keyヘッダー）が必要です。</p>
 *
 * @since 1.0.0
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

    /**
     * 新しいフォルダを作成します.
     *
     * @param request フォルダ作成リクエスト（親フォルダID、フォルダ名）
     * @param httpRequest HTTPリクエスト（APIキーの取得に使用）
     * @return 作成されたフォルダ情報
     */
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

    /**
     * フォルダのメタデータ情報を取得します.
     *
     * @param folderId 対象フォルダのID
     * @param request HTTPリクエスト（APIキーの取得に使用）
     * @return フォルダ情報（名前、親フォルダID、アイテム数など）
     */
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

    /**
     * フォルダ内のアイテム一覧を取得します.
     *
     * @param folderId 対象フォルダのID
     * @param request HTTPリクエスト（APIキーの取得に使用）
     * @return フォルダ内アイテムの名前リスト
     */
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

    /**
     * フォルダを削除します.
     *
     * @param folderId 削除するフォルダのID
     * @param recursive trueの場合、フォルダ内のアイテムも全て削除
     * @param request HTTPリクエスト（APIキーの取得に使用）
     * @return 削除成功時は204 No Content
     */
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
