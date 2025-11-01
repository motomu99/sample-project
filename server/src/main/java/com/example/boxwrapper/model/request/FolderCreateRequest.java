package com.example.boxwrapper.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * フォルダ作成リクエスト
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderCreateRequest {

    @NotBlank(message = "親フォルダIDは必須です")
    private String parentFolderId;

    @NotBlank(message = "フォルダ名は必須です")
    private String folderName;
}
