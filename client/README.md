# Box Wrapper Client Library

Box SDK Wrapper REST APIにアクセスするためのJavaクライアントライブラリです。

## 依存関係の追加

### Maven

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>box-wrapper-client</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### Gradle

```gradle
implementation 'com.example:box-wrapper-client:0.0.1-SNAPSHOT'
```

## 使用方法

### クライアントの初期化

```java
BoxWrapperClient client = BoxWrapperClient.builder()
    .baseUrl("http://localhost:8080")
    .apiKey("your-api-key-here")
    .build();
```

### ファイル操作

```java
// ファイルアップロード
FileUploadResponse uploadResponse = client.files()
    .uploadFile("0", new File("document.pdf"));
System.out.println("Uploaded file ID: " + uploadResponse.getFileId());

// ファイル情報取得
FileInfoResponse fileInfo = client.files()
    .getFileInfo("123456");
System.out.println("File name: " + fileInfo.getFileName());

// ファイルダウンロード
byte[] content = client.files()
    .downloadFile("123456");
Files.write(Paths.get("downloaded.pdf"), content);

// ファイル削除
client.files().deleteFile("123456");
```

### フォルダ操作

```java
// フォルダ作成
FolderInfoResponse folder = client.folders()
    .createFolder("0", "New Folder");
System.out.println("Created folder ID: " + folder.getFolderId());

// フォルダ情報取得
FolderInfoResponse folderInfo = client.folders()
    .getFolderInfo("123456");

// フォルダ内アイテム一覧
List<String> items = client.folders()
    .listFolderItems("123456");
items.forEach(System.out::println);

// フォルダ削除（再帰的）
client.folders().deleteFolder("123456", true);
```

### 検索

```java
// シンプルな検索
List<String> results = client.search()
    .search("annual report");

// 詳細検索
SearchRequest searchRequest = SearchRequest.builder()
    .query("contract")
    .type("file")
    .fileExtension("pdf")
    .offset(0)
    .limit(20)
    .build();

List<String> pdfResults = client.search()
    .search(searchRequest);
```

### ジョブステータス確認

```java
// ジョブステータス取得
JobStatusResponse status = client.jobs()
    .getJobStatus("job-abc-123");

System.out.println("Status: " + status.getStatus());
System.out.println("Progress: " + status.getCompleted() + "/" + status.getTotal());

// ジョブ削除
client.jobs().deleteJob("job-abc-123");
```

## カスタムHTTPクライアントの使用

```java
// タイムアウトをカスタマイズ
OkHttpClient customHttpClient = new OkHttpClient.Builder()
    .connectTimeout(60, TimeUnit.SECONDS)
    .readTimeout(120, TimeUnit.SECONDS)
    .writeTimeout(120, TimeUnit.SECONDS)
    .build();

BoxWrapperClient client = BoxWrapperClient.builder()
    .baseUrl("http://localhost:8080")
    .apiKey("your-api-key")
    .httpClient(customHttpClient)
    .build();
```

## エラーハンドリング

```java
try {
    FileInfoResponse info = client.files().getFileInfo("invalid-id");
} catch (BoxWrapperClientException e) {
    System.err.println("API call failed: " + e.getMessage());
    e.printStackTrace();
}
```

## スレッドセーフティ

`BoxWrapperClient`インスタンスはスレッドセーフです。複数のスレッドから安全に使用できます。

```java
// シングルトンパターンで使用することを推奨
private static final BoxWrapperClient CLIENT = BoxWrapperClient.builder()
    .baseUrl("http://localhost:8080")
    .apiKey(System.getenv("BOX_API_KEY"))
    .build();
```

## 必要な環境

- Java 21以上
- Box SDK Wrapper REST APIサーバーが起動していること
