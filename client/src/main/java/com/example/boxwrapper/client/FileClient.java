package com.example.boxwrapper.client;

import com.example.boxwrapper.model.response.FileInfoResponse;
import com.example.boxwrapper.model.response.FileUploadResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.File;
import java.io.IOException;

/**
 * ファイル操作クライアント.
 *
 * <p>Box SDK Wrapper REST APIのファイル関連エンドポイントにアクセスします。</p>
 *
 * <p>使用例：
 * <pre>{@code
 * FileClient fileClient = client.files();
 *
 * // ファイルアップロード
 * FileUploadResponse response = fileClient.uploadFile("0", new File("test.pdf"));
 *
 * // ファイル情報取得
 * FileInfoResponse info = fileClient.getFileInfo("123456");
 *
 * // ファイルダウンロード
 * byte[] content = fileClient.downloadFile("123456");
 *
 * // ファイル削除
 * fileClient.deleteFile("123456");
 * }</pre>
 * </p>
 *
 * @since 1.0.0
 */
@Slf4j
public class FileClient extends BaseClient {

    public FileClient(BoxWrapperClient mainClient) {
        super(mainClient);
    }

    /**
     * ファイルをアップロードします.
     *
     * @param folderId アップロード先のフォルダID
     * @param file アップロードするファイル
     * @return アップロード結果
     * @throws BoxWrapperClientException アップロードに失敗した場合
     */
    public FileUploadResponse uploadFile(String folderId, File file) {
        String url = mainClient.getBaseUrl() + "/api/v1/files/upload";

        RequestBody requestBody = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("folderId", folderId)
            .addFormDataPart("file", file.getName(),
                RequestBody.create(file, MediaType.parse("application/octet-stream")))
            .build();

        Request request = new Request.Builder()
            .url(url)
            .addHeader("X-API-Key", mainClient.getApiKey())
            .post(requestBody)
            .build();

        try (Response response = mainClient.getHttpClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new BoxWrapperClientException(
                    "Upload failed with status: " + response.code()
                );
            }

            String responseBody = response.body() != null ? response.body().string() : "{}";
            return objectMapper.readValue(responseBody, FileUploadResponse.class);
        } catch (IOException e) {
            throw new BoxWrapperClientException("Upload failed", e);
        }
    }

    /**
     * ファイル情報を取得します.
     *
     * @param fileId ファイルID
     * @return ファイル情報
     * @throws BoxWrapperClientException 取得に失敗した場合
     */
    public FileInfoResponse getFileInfo(String fileId) {
        return get("/api/v1/files/" + fileId, FileInfoResponse.class);
    }

    /**
     * ファイルをダウンロードします.
     *
     * @param fileId ファイルID
     * @return ファイル内容のバイト配列
     * @throws BoxWrapperClientException ダウンロードに失敗した場合
     */
    public byte[] downloadFile(String fileId) {
        String url = mainClient.getBaseUrl() + "/api/v1/files/" + fileId + "/download";

        Request request = new Request.Builder()
            .url(url)
            .addHeader("X-API-Key", mainClient.getApiKey())
            .get()
            .build();

        try (Response response = mainClient.getHttpClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new BoxWrapperClientException(
                    "Download failed with status: " + response.code()
                );
            }

            return response.body() != null ? response.body().bytes() : new byte[0];
        } catch (IOException e) {
            throw new BoxWrapperClientException("Download failed", e);
        }
    }

    /**
     * ファイルを削除します.
     *
     * @param fileId ファイルID
     * @throws BoxWrapperClientException 削除に失敗した場合
     */
    public void deleteFile(String fileId) {
        delete("/api/v1/files/" + fileId);
    }
}
