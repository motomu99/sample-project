package com.example.boxwrapper.client;

import com.example.boxwrapper.model.response.JobStatusResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * ジョブ管理クライアント.
 *
 * <p>Box SDK Wrapper REST APIのジョブ管理エンドポイントにアクセスします。</p>
 *
 * <p>使用例：
 * <pre>{@code
 * JobClient jobClient = client.jobs();
 *
 * // ジョブステータス取得
 * JobStatusResponse status = jobClient.getJobStatus("job-123");
 *
 * // ジョブ削除
 * jobClient.deleteJob("job-123");
 * }</pre>
 * </p>
 *
 * @since 1.0.0
 */
@Slf4j
public class JobClient extends BaseClient {

    public JobClient(BoxWrapperClient mainClient) {
        super(mainClient);
    }

    /**
     * ジョブのステータスを取得します.
     *
     * @param jobId ジョブID
     * @return ジョブステータス
     * @throws BoxWrapperClientException 取得に失敗した場合
     */
    public JobStatusResponse getJobStatus(String jobId) {
        return get("/api/v1/jobs/" + jobId + "/status", JobStatusResponse.class);
    }

    /**
     * ジョブを削除します.
     *
     * @param jobId ジョブID
     * @throws BoxWrapperClientException 削除に失敗した場合
     */
    public void deleteJob(String jobId) {
        delete("/api/v1/jobs/" + jobId);
    }
}
