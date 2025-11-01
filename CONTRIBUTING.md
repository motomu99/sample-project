# Contributing to Box SDK Wrapper REST API

このプロジェクトへのcontributionを歓迎します！

## 開発の流れ

### 1. Issue作成

新機能の提案やバグ報告は、まずIssueを作成してください。

### 2. Fork & Clone

```bash
# Fork後、クローン
git clone https://github.com/your-username/box-sdk-wrapper.git
cd box-sdk-wrapper

# 開発ブランチを作成
git checkout -b feature/your-feature-name
```

### 3. TDD (Test-Driven Development)

本プロジェクトはTDDを採用しています。

**重要**: コードを書く前にテストを書いてください！

```bash
# 1. テストを書く (Red)
# src/test/java/... にテストを作成

# 2. テストを実行して失敗を確認
./mvnw test

# 3. 実装を書く (Green)
# src/main/java/... に実装を作成

# 4. テストが通ることを確認
./mvnw test

# 5. リファクタリング (Refactor)
```

### 4. コーディング規約

- Java 21の機能を活用
- Lombokを積極的に使用
- 適切なログレベルを選択
  - DEBUG: 詳細情報
  - INFO: 一般的な情報
  - WARN: 警告
  - ERROR: エラー
- 例外は適切にハンドリング
- すべてのpublicメソッドにJavaDocを記述

### 5. テストカバレッジ

カバレッジ要件:
- Line Coverage: 90%以上
- Branch Coverage: 85%以上

```bash
# カバレッジレポート生成
./mvnw test jacoco:report

# レポート確認
open target/site/jacoco/index.html
```

### 6. コミットメッセージ

Conventional Commitsに従ってください:

```
feat: 新機能追加
fix: バグ修正
docs: ドキュメント変更
test: テスト追加・修正
refactor: リファクタリング
perf: パフォーマンス改善
style: コードスタイル修正
chore: ビルド・設定変更
```

例:
```bash
git commit -m "feat: ファイル一括ダウンロード機能を追加"
git commit -m "fix: レート制限エラーハンドリングを修正"
git commit -m "test: BoxFileServiceの単体テストを追加"
```

### 7. Pull Request

```bash
# 変更をプッシュ
git push origin feature/your-feature-name

# GitHubでPull Requestを作成
```

**Pull Requestに含めるべき内容**:
- 変更の概要
- Issueへのリンク (closes #123)
- テストの説明
- スクリーンショット (UI変更の場合)

### 8. レビュー

- 最低1人のレビュー承認が必要
- CIが全て通過していること
- コンフリクトが解決されていること

## テストの書き方

### 単体テスト

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("BoxFileService - Unit Tests")
class BoxFileServiceTest {

    @Mock
    private BoxClientManager clientManager;

    @InjectMocks
    private BoxFileService fileService;

    @Test
    @DisplayName("ファイルアップロード - 正常系")
    void uploadFile_Success() {
        // Given (準備)
        // When (実行)
        // Then (検証)
    }
}
```

### 統合テスト

```java
@SpringBootTest
@AutoConfigureMockMvc
class FileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BoxFileService fileService;

    @Test
    void uploadFileApi_Success() throws Exception {
        // Given
        // When & Then
        mockMvc.perform(...)
            .andExpect(status().isOk());
    }
}
```

## よくある質問

### Q: Box Developer Tokenの有効期限は?

A: Developer Tokenは60分で期限切れになります。本番環境ではJWT認証を使用してください。

### Q: テストでBox APIを実際に呼び出す必要はありますか?

A: いいえ。単体テスト・統合テストではモックを使用します。E2Eテストのみ実際のBox APIを使用します。

### Q: 新しいBox SDK機能を追加するには?

A: 以下の手順に従ってください:
1. Issueを作成
2. テストを書く
3. Serviceレイヤーに実装
4. Controllerに公開
5. Swaggerドキュメント更新
6. README更新

## お問い合わせ

質問がある場合は、Issueを作成してください。
