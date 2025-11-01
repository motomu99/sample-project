package com.example.boxwrapper.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 検索リクエスト
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

    @NotBlank(message = "検索クエリは必須です")
    private String query;

    private String type;  // file or folder

    private Integer offset;

    private Integer limit;

    private String fileExtension;
}
