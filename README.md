# Box SDK Wrapper REST API

Box Java SDKをラップしたREST APIサービス。複数のアプリケーションから利用可能で、アプリケーション毎に認証キーを管理します。

## 技術スタック

- **Java 21**
- **Spring Boot 3.2.1**
- **Maven / Gradle**
- **Box Java SDK 4.8.0**
- **Resilience4j** (リトライ・レート制限)
- **Bucket4j** (レート制限)
- **Caffeine** (キャッシュ)
- **OpenAPI/Swagger** (API ドキュメント)

## 主要機能

### ファイル操作
- ファイルアップロード (マルチパート対応)
- ファイルダウンロード (ストリーム対応)
- ファイル情報取得
- ファイル削除

### フォルダ操作
- フォルダ作成
- フォルダ内アイテム一覧取得
- フォルダ情報取得
- フォルダ削除 (再帰的削除対応)

### 検索機能
- キーワード検索
- ファイルタイプフィルタ
- ページネーション対応

### 非機能要件
- **APIキー認証**: `X-API-Key` ヘッダーによる認証
- **レート制限**: Bucket4jによるアダプティブレート制限
- **リトライ処理**: Resilience4jによる自動リトライ (最大5回、Exponential Backoff)
- **キャッシング**: Caffeineによるメタデータキャッシュ (TTL: 5分)
- **非同期処理**: CompletableFutureによる並列アップロード/ダウンロード
- **エラーハンドリング**: 統一されたエラーレスポンス形式
- **ロギング**: SLF4J + Logback、リクエストIDトレーシング
- **API ドキュメント**: Swagger UI

## セットアップ

### 前提条件

- JDK 21+
- Maven 3.8+ または Gradle 8.5+
- Box Developer Account

### Box認証設定

#### 1. JWT認証 (本番環境推奨)

1. Box Developer Consoleで新しいアプリを作成
2. 認証方法として「OAuth 2.0 with JWT」を選択
3. アプリの設定から構成ファイル (JSON) をダウンロード
4. `src/main/resources/box-config.json` として配置

#### 2. Developer Token (開発・テスト環境)

1. Box Developer Consoleでアプリを作成
2. Developer Tokenを生成
3. 環境変数 `BOX_DEVELOPER_TOKEN` に設定

### アプリケーション設定

`src/main/resources/application.yml` を編集:

```yaml
box:
  auth:
    type: jwt  # jwt or developer-token
    config-file: classpath:box-config.json
    developer-token: ${BOX_DEVELOPER_TOKEN:}

api:
  keys:
    - key: ${API_KEY_APP1:your-api-key-here}
      box-configs:
        - classpath:box-app1-account1.json
```

### ビルド & 実行

#### Maven使用

```bash
# ビルド
./mvnw clean package

# 実行
./mvnw spring-boot:run

# または
java -jar target/box-sdk-wrapper-0.0.1-SNAPSHOT.jar
```

#### Gradle使用

```bash
# ビルド
./gradlew clean build

# 実行
./gradlew bootRun
```

### テスト実行

```bash
# Maven
./mvnw test

# Gradle
./gradlew test

# カバレッジレポート生成
./mvnw test jacoco:report
# レポート: target/site/jacoco/index.html
```

## API使用方法

### 認証

全てのAPIリクエストに `X-API-Key` ヘッダーが必要です:

```bash
curl -H "X-API-Key: your-api-key-here" \
  http://localhost:8080/api/v1/files/{fileId}
```

### Swagger UI

アプリケーション起動後、以下のURLでAPI ドキュメントにアクセス:

```
http://localhost:8080/swagger-ui.html
```

### API エンドポイント

#### ファイル操作

```bash
# ファイルアップロード
curl -X POST \
  -H "X-API-Key: your-api-key" \
  -F "folderId=0" \
  -F "file=@/path/to/file.txt" \
  http://localhost:8080/api/v1/files/upload

# ファイル情報取得
curl -H "X-API-Key: your-api-key" \
  http://localhost:8080/api/v1/files/{fileId}

# ファイルダウンロード
curl -H "X-API-Key: your-api-key" \
  -o downloaded-file.txt \
  http://localhost:8080/api/v1/files/{fileId}/download

# ファイル削除
curl -X DELETE \
  -H "X-API-Key: your-api-key" \
  http://localhost:8080/api/v1/files/{fileId}
```

#### フォルダ操作

```bash
# フォルダ作成
curl -X POST \
  -H "X-API-Key: your-api-key" \
  -H "Content-Type: application/json" \
  -d '{"parentFolderId":"0","folderName":"NewFolder"}' \
  http://localhost:8080/api/v1/folders

# フォルダ情報取得
curl -H "X-API-Key: your-api-key" \
  http://localhost:8080/api/v1/folders/{folderId}

# フォルダ内アイテム一覧
curl -H "X-API-Key: your-api-key" \
  http://localhost:8080/api/v1/folders/{folderId}/items

# フォルダ削除
curl -X DELETE \
  -H "X-API-Key: your-api-key" \
  http://localhost:8080/api/v1/folders/{folderId}?recursive=true
```

#### 検索

```bash
# ファイル検索
curl -H "X-API-Key: your-api-key" \
  "http://localhost:8080/api/v1/search?query=report&type=file&limit=10"
```

#### ジョブ管理

```bash
# ジョブステータス取得
curl -H "X-API-Key: your-api-key" \
  http://localhost:8080/api/v1/jobs/{jobId}/status
```

## エラーレスポンス形式

全てのエラーは統一されたJSON形式で返却されます:

```json
{
  "timestamp": "2025-11-01T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "File (ID: 12345) が見つかりません",
  "path": "/api/v1/files/12345",
  "requestId": "uuid-here"
}
```

## レート制限

- デフォルト: 10リクエスト/秒
- アダプティブ制御: 有効
- 429エラー発生時、自動的にレートを調整
- `Retry-After` ヘッダーを尊重

## ロギング

リクエストIDでトレーシング可能:

```
2025-11-01 10:00:00 [http-nio-8080-exec-1] [uuid-here] INFO  c.e.b.controller.FileController - Request: GET /api/v1/files/12345
```

## 設定項目

主要な設定項目（`application.yml`）:

```yaml
# Box SDK設定
box:
  auth:
    type: jwt
  rate-limit:
    enabled: true
    requests-per-second: 10
    adaptive: true
  retry:
    max-attempts: 5

# 非同期処理
async:
  parallel:
    max-concurrent-uploads: 5
    max-concurrent-downloads: 5
  thread-pool:
    core-size: 10
    max-size: 20

# キャッシュ
spring:
  cache:
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=300s
```

## 開発ガイド

### TDD (Test-Driven Development)

本プロジェクトはTDDで開発されています:

1. テストを先に書く (Red)
2. 最小限のコードでテストを通す (Green)
3. リファクタリング (Refactor)

### テストカバレッジ目標

- Line Coverage: 90%以上
- Branch Coverage: 85%以上

### コード品質

- Lombok使用で冗長なコードを削減
- SLF4Jによる統一されたロギング
- Spring AOPで横断的関心事を分離

## トラブルシューティング

### Box API 認証エラー

```
AuthenticationException: Invalid API key or no Box connections configured
```

**解決方法**:
1. `box-config.json` が正しく配置されているか確認
2. APIキーが `application.yml` で正しく設定されているか確認
3. Box Developer Consoleでアプリが有効化されているか確認

### レート制限エラー

```
BoxApiException: レート制限に達しました (429)
```

**解決方法**:
1. アダプティブレート制限が有効化されていることを確認
2. 必要に応じて `box.rate-limit.requests-per-second` を調整
3. 複数のBox認証情報を設定してロードバランシング

## ライセンス

MIT License

## 貢献

プルリクエスト歓迎！

1. Fork the repository
2. Create your feature branch
3. Commit your changes (with tests!)
4. Push to the branch
5. Create a Pull Request

## サポート

Issue: https://github.com/your-repo/box-sdk-wrapper/issues
