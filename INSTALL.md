# EasyHideAndSeek - インストールとトラブルシューティング

## インストール方法

### 1. 必要な環境
- **Paper 1.21.5** サーバー
- **Java 21** 以上
- **Vault** プラグイン（オプション - 将来の経済機能用）

### 2. インストール手順

```bash
# 1. プラグインをコピー
cp build/libs/EasyHideAndSeek-1.0-SNAPSHOT.jar /path/to/server/plugins/

# 2. サーバーを起動
cd /path/to/server
java -jar paper.jar
```

### 3. 起動ログの確認

サーバー起動時に以下のログが表示されることを確認してください：

```
[HideAndSeek] HideAndSeek plugin enabling...
[HideAndSeek] Registering commands...
[HideAndSeek] Command 'hideandseek' (alias: 'hs') registered successfully
[HideAndSeek] Available subcommands: join, leave, shop, admin
[HideAndSeek] Listeners registered
[HideAndSeek] HideAndSeek plugin enabled!
```

## コマンドが表示されない場合のトラブルシューティング

### 問題1: `/hs` コマンドが存在しない

**確認事項:**
1. プラグインが正しく読み込まれているか
   ```
   /plugins
   ```
   リストに「HideAndSeek」が緑色で表示されることを確認

2. サーバーログを確認
   ```
   logs/latest.log
   ```
   エラーメッセージがないか確認

**解決方法:**
- プラグインが赤色の場合、依存関係の問題です
- Vaultプラグインは不要ですが、ある場合は最新版を使用してください

### 問題2: コマンドは存在するが実行できない

**確認事項:**
```
/minecraft:help hideandseek
```
で情報を確認

**解決方法:**
- 権限を確認: `hideandseek.use` (デフォルトで全員に付与)
- OPの場合: `/op <プレイヤー名>` で管理者権限を付与

### 問題3: コマンドのタブ補完が効かない

これは正常です。以下のコマンドを直接入力してください：
- `/hs join`
- `/hs leave`
- `/hs shop`
- `/hs admin setpos1`

## 初回セットアップ

### アリーナの作成

```bash
# 1. 境界の設定
/hs admin setpos1          # 角1の地点に立って実行
/hs admin setpos2          # 角2の地点（対角）に立って実行

# 2. スポーン地点の設定
/hs admin setspawn seeker  # シーカーのスポーン地点に立って実行
/hs admin setspawn hider   # ハイダーのスポーン地点に立って実行

# 3. アリーナの作成
/hs admin create arena1

# 4. 確認
/hs admin list
```

### ゲームの開始

```bash
# プレイヤーが参加
/hs join

# 管理者がゲームを開始（2人以上必要）
/hs admin start arena1
```

## デバッグモード

起動時のログに以下が表示されない場合、JARファイルが破損している可能性があります：

```
[HideAndSeek] Registering commands...
```

この場合：
1. `./gradlew clean build` で再ビルド
2. `build/libs/` フォルダから新しいJARをコピー

## サポート

ログファイル全体を確認してください：
```bash
cat logs/latest.log | grep -i hideandseek
```

これにより、プラグインの読み込み状況とエラーがわかります。
