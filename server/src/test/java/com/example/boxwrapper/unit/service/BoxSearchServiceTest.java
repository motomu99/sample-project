package com.example.boxwrapper.unit.service;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxItem;
import com.box.sdk.BoxSearch;
import com.box.sdk.BoxSearchParameters;
import com.box.sdk.PartialCollection;
import com.example.boxwrapper.client.BoxClientManager;
import com.example.boxwrapper.exception.BoxApiException;
import com.example.boxwrapper.model.request.SearchRequest;
import com.example.boxwrapper.service.BoxSearchService;
import com.example.boxwrapper.utils.RateLimiterManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BoxSearchService Unit Tests")
class BoxSearchServiceTest {

    private static final String API_KEY = "test-api-key";

    @Mock
    private BoxClientManager clientManager;

    @Mock
    private RateLimiterManager rateLimiterManager;

    @Mock
    private BoxAPIConnection apiConnection;

    private BoxSearchService searchService;

    @BeforeEach
    void setUp() {
        searchService = new BoxSearchService(clientManager, rateLimiterManager);
        lenient().when(clientManager.getConnection(API_KEY)).thenReturn(apiConnection);
    }

    @Test
    @DisplayName("search - 全てのパラメータを指定して検索できること")
    void search_SuccessWithAllParameters() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(true);

        SearchRequest request = SearchRequest.builder()
            .query("annual report")
            .type("file")
            .fileExtension("xlsx")
            .offset(50)
            .limit(25)
            .build();

        BoxItem.Info item1 = mock(BoxItem.Info.class);
        BoxItem.Info item2 = mock(BoxItem.Info.class);
        when(item1.getName()).thenReturn("report-2023.xlsx");
        when(item2.getName()).thenReturn("report-2024.xlsx");

        AtomicLong capturedOffset = new AtomicLong();
        AtomicLong capturedLimit = new AtomicLong();
        AtomicReference<BoxSearchParameters> capturedParams = new AtomicReference<>();

        try (MockedConstruction<BoxSearch> mockedSearch = mockConstruction(
            BoxSearch.class,
            (mock, context) -> when(mock.searchRange(anyLong(), anyLong(), any(BoxSearchParameters.class)))
                .thenAnswer(invocation -> {
                    capturedOffset.set(invocation.getArgument(0, Long.class));
                    capturedLimit.set(invocation.getArgument(1, Long.class));
                    BoxSearchParameters params = invocation.getArgument(2);
                    capturedParams.set(params);

                    @SuppressWarnings("unchecked")
                    PartialCollection<BoxItem.Info> results = mock(PartialCollection.class);
                    when(results.iterator()).thenReturn(List.of(item1, item2).iterator());
                    return results;
                })
        )) {
            List<String> names = searchService.search(API_KEY, request);

            assertThat(names).containsExactly("report-2023.xlsx", "report-2024.xlsx");
            assertThat(capturedOffset.get()).isEqualTo(50L);
            assertThat(capturedLimit.get()).isEqualTo(25L);
            assertThat(capturedParams.get().getQuery()).isEqualTo("annual report");
            assertThat(capturedParams.get().getType()).isEqualTo("file");
            assertThat(capturedParams.get().getFileExtensions()).containsExactly("xlsx");

            verify(rateLimiterManager).handleSuccess(API_KEY);
            assertThat(mockedSearch.constructed()).hasSize(1);
        }
    }

    @Test
    @DisplayName("search - ページネーション未指定時はデフォルト値を使うこと")
    void search_DefaultPagination() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(true);

        SearchRequest request = SearchRequest.builder()
            .query("documents")
            .build();

        AtomicLong capturedOffset = new AtomicLong(-1L);
        AtomicLong capturedLimit = new AtomicLong(-1L);

        try (MockedConstruction<BoxSearch> mockedSearch = mockConstruction(
            BoxSearch.class,
            (mock, context) -> when(mock.searchRange(anyLong(), anyLong(), any(BoxSearchParameters.class)))
                .thenAnswer(invocation -> {
                    capturedOffset.set(invocation.getArgument(0, Long.class));
                    capturedLimit.set(invocation.getArgument(1, Long.class));
                    @SuppressWarnings("unchecked")
                    PartialCollection<BoxItem.Info> results = mock(PartialCollection.class);
                    when(results.iterator()).thenReturn(List.<BoxItem.Info>of().iterator());
                    return results;
                })
        )) {
            List<String> names = searchService.search(API_KEY, request);

            assertThat(names).isEmpty();
            assertThat(capturedOffset.get()).isEqualTo(0L);
            assertThat(capturedLimit.get()).isEqualTo(100L);
        }
    }

    @Test
    @DisplayName("search - レートリミットゲートで拒否された場合に例外を送出すること")
    void search_RateLimitGateRejected() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(false);

        SearchRequest request = SearchRequest.builder()
            .query("test")
            .build();

        assertThatThrownBy(() -> searchService.search(API_KEY, request))
            .isInstanceOf(BoxApiException.class)
            .hasMessageContaining("レート制限");

        verify(clientManager, never()).getConnection(API_KEY);
    }

    @Test
    @DisplayName("search - Box APIが429を返した場合にレートリミットハンドラが呼ばれること")
    void search_RateLimitExceededFromApi() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(true);

        SearchRequest request = SearchRequest.builder()
            .query("test")
            .build();

        BoxAPIException rateLimit = mock(BoxAPIException.class);
        when(rateLimit.getResponseCode()).thenReturn(429);

        try (MockedConstruction<BoxSearch> mockedSearch = mockConstruction(
            BoxSearch.class,
            (mock, context) -> when(mock.searchRange(anyLong(), anyLong(), any(BoxSearchParameters.class)))
                .thenThrow(rateLimit)
        )) {
            assertThatThrownBy(() -> searchService.search(API_KEY, request))
                .isInstanceOf(BoxApiException.class)
                .hasMessageContaining("検索に失敗しました");

            verify(rateLimiterManager).handleRateLimitExceeded(API_KEY);
            verify(rateLimiterManager, never()).handleSuccess(API_KEY);
        }
    }

    @Test
    @DisplayName("search - Box APIのその他の例外をBoxApiExceptionにラップすること")
    void search_UnexpectedError() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(true);

        SearchRequest request = SearchRequest.builder()
            .query("test")
            .build();

        RuntimeException unexpected = new RuntimeException("boom");

        try (MockedConstruction<BoxSearch> mockedSearch = mockConstruction(
            BoxSearch.class,
            (mock, context) -> when(mock.searchRange(anyLong(), anyLong(), any(BoxSearchParameters.class)))
                .thenThrow(unexpected)
        )) {
            assertThatThrownBy(() -> searchService.search(API_KEY, request))
                .isInstanceOf(BoxApiException.class)
                .hasMessageContaining("検索に失敗しました");

            verify(rateLimiterManager, never()).handleSuccess(API_KEY);
        }
    }
}
