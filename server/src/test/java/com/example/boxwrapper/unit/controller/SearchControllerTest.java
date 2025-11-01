package com.example.boxwrapper.unit.controller;

import com.example.boxwrapper.controller.SearchController;
import com.example.boxwrapper.model.request.SearchRequest;
import com.example.boxwrapper.service.BoxSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SearchControllerのユニットテスト.
 */
@WebMvcTest(SearchController.class)
@DisplayName("SearchController Unit Tests")
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BoxSearchService searchService;

    private static final String API_KEY = "test-api-key-123";

    @Test
    @DisplayName("GET /api/v1/search - 検索が成功すること（最小限のパラメータ）")
    void testSearch_Success_MinimalParams() throws Exception {
        // Given
        List<String> results = Arrays.asList("document1.pdf", "report.docx", "data.xlsx");
        when(searchService.search(anyString(), any(SearchRequest.class)))
            .thenReturn(results);

        // When & Then
        mockMvc.perform(get("/api/v1/search")
                .header("X-API-Key", API_KEY)
                .param("query", "report"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0]").value("document1.pdf"))
            .andExpect(jsonPath("$[1]").value("report.docx"))
            .andExpect(jsonPath("$[2]").value("data.xlsx"));

        verify(searchService, times(1)).search(anyString(), any(SearchRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/search - ファイルタイプフィルタ付き検索が成功すること")
    void testSearch_WithFileTypeFilter() throws Exception {
        // Given
        List<String> results = Arrays.asList("document1.pdf", "document2.pdf");
        when(searchService.search(anyString(), any(SearchRequest.class)))
            .thenReturn(results);

        // When & Then
        mockMvc.perform(get("/api/v1/search")
                .header("X-API-Key", API_KEY)
                .param("query", "document")
                .param("type", "file"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2));

        verify(searchService, times(1)).search(anyString(), any(SearchRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/search - フォルダタイプフィルタ付き検索が成功すること")
    void testSearch_WithFolderTypeFilter() throws Exception {
        // Given
        List<String> results = Arrays.asList("Projects", "Documents", "Archive");
        when(searchService.search(anyString(), any(SearchRequest.class)))
            .thenReturn(results);

        // When & Then
        mockMvc.perform(get("/api/v1/search")
                .header("X-API-Key", API_KEY)
                .param("query", "folder")
                .param("type", "folder"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0]").value("Projects"))
            .andExpect(jsonPath("$[1]").value("Documents"))
            .andExpect(jsonPath("$[2]").value("Archive"));

        verify(searchService, times(1)).search(anyString(), any(SearchRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/search - ファイル拡張子フィルタ付き検索が成功すること")
    void testSearch_WithFileExtensionFilter() throws Exception {
        // Given
        List<String> results = Arrays.asList("report.pdf", "invoice.pdf");
        when(searchService.search(anyString(), any(SearchRequest.class)))
            .thenReturn(results);

        // When & Then
        mockMvc.perform(get("/api/v1/search")
                .header("X-API-Key", API_KEY)
                .param("query", "document")
                .param("fileExtension", "pdf"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2));

        verify(searchService, times(1)).search(anyString(), any(SearchRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/search - ページネーション付き検索が成功すること")
    void testSearch_WithPagination() throws Exception {
        // Given
        List<String> results = Arrays.asList("item21", "item22", "item23", "item24", "item25");
        when(searchService.search(anyString(), any(SearchRequest.class)))
            .thenReturn(results);

        // When & Then
        mockMvc.perform(get("/api/v1/search")
                .header("X-API-Key", API_KEY)
                .param("query", "item")
                .param("offset", "20")
                .param("limit", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(5));

        verify(searchService, times(1)).search(anyString(), any(SearchRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/search - 全パラメータ指定の検索が成功すること")
    void testSearch_WithAllParameters() throws Exception {
        // Given
        List<String> results = Arrays.asList("report.xlsx", "data.xlsx");
        when(searchService.search(anyString(), any(SearchRequest.class)))
            .thenReturn(results);

        // When & Then
        mockMvc.perform(get("/api/v1/search")
                .header("X-API-Key", API_KEY)
                .param("query", "annual report")
                .param("type", "file")
                .param("fileExtension", "xlsx")
                .param("offset", "0")
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2));

        verify(searchService, times(1)).search(anyString(), any(SearchRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/search - 検索結果が空の場合、空配列が返ること")
    void testSearch_EmptyResults() throws Exception {
        // Given
        when(searchService.search(anyString(), any(SearchRequest.class)))
            .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/search")
                .header("X-API-Key", API_KEY)
                .param("query", "nonexistent"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));

        verify(searchService, times(1)).search(anyString(), any(SearchRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/search - APIキーがない場合、401エラーが返ること")
    void testSearch_MissingApiKey() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/search")
                .param("query", "test"))
            .andExpect(status().isUnauthorized());

        verify(searchService, never()).search(anyString(), any(SearchRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/search - queryパラメータがない場合、400エラーが返ること")
    void testSearch_MissingQueryParameter() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/search")
                .header("X-API-Key", API_KEY))
            .andExpect(status().isBadRequest());

        verify(searchService, never()).search(anyString(), any(SearchRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/search - offsetのみ指定の検索が成功すること")
    void testSearch_WithOffsetOnly() throws Exception {
        // Given
        List<String> results = Arrays.asList("item11", "item12");
        when(searchService.search(anyString(), any(SearchRequest.class)))
            .thenReturn(results);

        // When & Then
        mockMvc.perform(get("/api/v1/search")
                .header("X-API-Key", API_KEY)
                .param("query", "item")
                .param("offset", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2));

        verify(searchService, times(1)).search(anyString(), any(SearchRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/search - limitのみ指定の検索が成功すること")
    void testSearch_WithLimitOnly() throws Exception {
        // Given
        List<String> results = Arrays.asList("item1", "item2", "item3");
        when(searchService.search(anyString(), any(SearchRequest.class)))
            .thenReturn(results);

        // When & Then
        mockMvc.perform(get("/api/v1/search")
                .header("X-API-Key", API_KEY)
                .param("query", "item")
                .param("limit", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(3));

        verify(searchService, times(1)).search(anyString(), any(SearchRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/search - 複数の検索クエリが独立して処理されること")
    void testSearch_MultipleQueries() throws Exception {
        // Given
        List<String> results1 = Arrays.asList("doc1.pdf", "doc2.pdf");
        List<String> results2 = Arrays.asList("report1.xlsx");

        when(searchService.search(eq(API_KEY), any(SearchRequest.class)))
            .thenReturn(results1)
            .thenReturn(results2);

        // When & Then - First query
        mockMvc.perform(get("/api/v1/search")
                .header("X-API-Key", API_KEY)
                .param("query", "document"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));

        // When & Then - Second query
        mockMvc.perform(get("/api/v1/search")
                .header("X-API-Key", API_KEY)
                .param("query", "report"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(searchService, times(2)).search(anyString(), any(SearchRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/search - 日本語クエリが正しく処理されること")
    void testSearch_JapaneseQuery() throws Exception {
        // Given
        List<String> results = Arrays.asList("年次報告書.pdf", "四半期報告.xlsx");
        when(searchService.search(anyString(), any(SearchRequest.class)))
            .thenReturn(results);

        // When & Then
        mockMvc.perform(get("/api/v1/search")
                .header("X-API-Key", API_KEY)
                .param("query", "報告書"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2));

        verify(searchService, times(1)).search(anyString(), any(SearchRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/search - スペースを含むクエリが正しく処理されること")
    void testSearch_QueryWithSpaces() throws Exception {
        // Given
        List<String> results = Arrays.asList("Annual Report 2023.pdf");
        when(searchService.search(anyString(), any(SearchRequest.class)))
            .thenReturn(results);

        // When & Then
        mockMvc.perform(get("/api/v1/search")
                .header("X-API-Key", API_KEY)
                .param("query", "annual report"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1));

        verify(searchService, times(1)).search(anyString(), any(SearchRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/search - 数値のoffsetとlimitが正しく処理されること")
    void testSearch_NumericPaginationParameters() throws Exception {
        // Given
        List<String> results = Arrays.asList("item1", "item2");
        when(searchService.search(anyString(), any(SearchRequest.class)))
            .thenReturn(results);

        // When & Then
        mockMvc.perform(get("/api/v1/search")
                .header("X-API-Key", API_KEY)
                .param("query", "test")
                .param("offset", "100")
                .param("limit", "50"))
            .andExpect(status().isOk());

        verify(searchService, times(1)).search(anyString(), any(SearchRequest.class));
    }
}
