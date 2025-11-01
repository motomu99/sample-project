package com.example.boxwrapper.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * フォルダ情報レスポンス
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderInfoResponse {

    private String folderId;

    private String folderName;

    private String parentFolderId;

    private Integer itemCount;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime modifiedAt;
}
