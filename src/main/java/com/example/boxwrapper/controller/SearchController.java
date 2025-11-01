package com.example.boxwrapper.controller;

import com.example.boxwrapper.model.request.SearchRequest;
import com.example.boxwrapper.service.BoxSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 検索コントローラー.
 *
 * <p>Box のファイル・フォルダ検索機能を提供するREST APIエンドポイントです。
 * キーワード検索、タイプフィルタ、ページネーションに対応しています。</p>
 *
 * <p>全てのエンドポイントはAPIキー認証（X-API-Keyヘッダー）が必要です。</p>
 *
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Validated
@Tag(name = "Search", description = "Box 検索 API")
@SecurityRequirement(name = "API Key")
public class SearchController {

    private final BoxSearchService searchService;

    /**
     * ファイルおよびフォルダを検索します.
     *
     * <p>クエリパラメータで検索条件を指定します。</p>
     *
     * @param query 検索キーワード（必須）
     * @param type フィルタタイプ（"file"または"folder"、オプション）
     * @param offset ページネーションのオフセット（オプション）
     * @param limit 取得件数の上限（オプション）
     * @param fileExtension ファイル拡張子フィルタ（オプション）
     * @param request HTTPリクエスト（APIキーの取得に使用）
     * @return 検索結果のアイテム名リスト
     */
    @GetMapping
    @Operation(summary = "ファイル/フォルダ検索", description = "キーワードでファイル・フォルダを検索")
    public ResponseEntity<List<String>> search(
            @RequestParam String query,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String fileExtension,
            HttpServletRequest request) {

        String apiKey = (String) request.getAttribute("apiKey");

        SearchRequest searchRequest = SearchRequest.builder()
            .query(query)
            .type(type)
            .offset(offset)
            .limit(limit)
            .fileExtension(fileExtension)
            .build();

        List<String> results = searchService.search(apiKey, searchRequest);

        return ResponseEntity.ok(results);
    }
}
