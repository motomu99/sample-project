# 統合テスト

## 概要

統合テストでは実際のBox APIを使用して動作を確認します。

### 方法1: Developer Token設定

```bash
export BOX_DEVELOPER_TOKEN="your-developer-token-here"
./gradlew test --tests "*IntegrationTest"
```

### 方法2: JWT設定

`src/test/resources/box-config.json`にJWT認証設定を配置してください。

## 注意事項

統合テストを実行する前に`@Disabled`アノテーションを削除してください。
テストを実行しない場合は`@Disabled`アノテーションを追加してください。

## 実行

```bash
# 全ての統合テストを実行
./gradlew test --tests "*IntegrationTest"

# 特定のテストを実行
./gradlew test --tests "*BoxFileServiceIntegrationTest"

# 統合テストのみ実行（ユニットテストをスキップ）
./gradlew test --tests "*IntegrationTest" -x test --tests "*UnitTest"
```

## 要件

1. **認証**: 実際のBox APIを使用するためAPIキーの設定が必要です。
2. **ネットワーク**: インターネット接続が必要です。
3. **レート制限**: 実際のBox APIレート制限が適用されます。
4. **リソース**: 実際のBoxアカウントにリソースが作成されます。

## 実行方法

環境変数`test`プロファイルを使用してテストを実行します。
- 認証: Developer Token
- 認証: developer-token
