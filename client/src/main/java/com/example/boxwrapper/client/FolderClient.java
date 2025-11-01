package com.example.boxwrapper.client;

import com.example.boxwrapper.model.request.FolderCreateRequest;
import com.example.boxwrapper.model.response.FolderInfoResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * フォルダ操作クライアント.
 *
 * <p>Box SDK Wrapper REST APIのフォルダ関連エンドポイントにアクセスします。</p>
 *
 * <p>使用例：
 * <pre>{@code
 * FolderClient folderClient = client.folders();
 *
 * // フォルダ作成
 * FolderInfoResponse folder = folderClient.createFolder("0", "New Folder");
 *
 * // フォルダ情報取得
 * FolderInfoResponse info = folderClient.getFolderInfo("123456");
 *
 * // フォルダ内アイテム一覧
 * List<String> items = folderClient.listFolderItems("123456");
 *
 * // フォルダ削除
 * folderClient.deleteFolder("123456", true);
 * }</pre>
 * </p>
 *
 * @since 1.0.0
 */
@Slf4j
public class FolderClient extends BaseClient {

    public FolderClient(BoxWrapperClient mainClient) {
        super(mainClient);
    }

    /**
     * フォルダを作成します.
     *
     * @param parentFolderId 親フォルダID
     * @param folderName フォルダ名
     * @return 作成されたフォルダ情報
     * @throws BoxWrapperClientException 作成に失敗した場合
     */
    public FolderInfoResponse createFolder(String parentFolderId, String folderName) {
        FolderCreateRequest request = new FolderCreateRequest();
        request.setParentFolderId(parentFolderId);
        request.setFolderName(folderName);

        return post("/api/v1/folders", request, FolderInfoResponse.class);
    }

    /**
     * フォルダ情報を取得します.
     *
     * @param folderId フォルダID
     * @return フォルダ情報
     * @throws BoxWrapperClientException 取得に失敗した場合
     */
    public FolderInfoResponse getFolderInfo(String folderId) {
        return get("/api/v1/folders/" + folderId, FolderInfoResponse.class);
    }

    /**
     * フォルダ内のアイテム一覧を取得します.
     *
     * @param folderId フォルダID
     * @return アイテム名のリスト
     * @throws BoxWrapperClientException 取得に失敗した場合
     */
    public List<String> listFolderItems(String folderId) {
        String url = "/api/v1/folders/" + folderId + "/items";
        return get(url, new TypeReference<List<String>>() {});
    }

    /**
     * フォルダを削除します.
     *
     * @param folderId フォルダID
     * @param recursive 再帰的に削除するかどうか
     * @throws BoxWrapperClientException 削除に失敗した場合
     */
    public void deleteFolder(String folderId, boolean recursive) {
        delete("/api/v1/folders/" + folderId + "?recursive=" + recursive);
    }
}
