# Hide and Seek（かくれんぼ）

**企画者**: しもん

## ゲーム概要
**Hide and Seek**は、Minecraft内で楽しめるシンプルなかくれんぼゲームです。隠れる側（ハイダー）は鬼（シーカー）から逃れ、鬼は制限時間内にすべてのハイダーを見つけ出すことを目指します。攻撃によって捕獲し、経済システムと連動した報酬やアイテムショップ機能を搭載しています。

### 基本情報
- **ゲーム名**: Hide and Seek（かくれんぼ）
- **ジャンル**: 非対称型対戦ミニゲーム
- **プレイ人数**: 2-30人
  - 鬼（シーカー）: 人数の約23%（自動計算）
  - 隠れる側（ハイダー）: 人数の約77%（自動計算）
- **推奨人数**: 13人（鬼3人、ハイダー10人）
- **ゲーム時間**: 10分（推奨）
- **対象年齢**: 全年齢

## ゲーム設定

### 基本ルール
1. **役割分担**
   - ゲーム開始時に自動的に鬼とハイダーをランダムに決定
   - 推奨比率：鬼23%、ハイダー77%（13人なら鬼3人、ハイダー10人）

2. **準備フェーズ**（30秒）
   - ハイダーが隠れる場所を探す
   - シーカーは待機エリアで待機（盲目エフェクト付与）
   - この間シーカーは動けない・見えない状態

3. **探索フェーズ**（10分）
   - シーカーがハイダーを探して攻撃で捕獲
   - ハイダーは自由に動き回ることが可能
   - 捕獲されたハイダーは即座に観戦モードへ

4. **終了条件**
   - 全ハイダーが捕獲される → シーカーの勝利
   - 制限時間終了時に1人でも残存 → ハイダーの勝利
   
5. **リザルト表示**
   - ゲーム終了時に結果画面表示
   - 全プレイヤーが攻撃不可状態になる
   - 報酬の配布

### 設定可能パラメータ
```yaml
game-settings:
  # ゲーム時間設定
  preparation-time: 30  # 準備時間（秒）
  seek-time: 600        # 探索時間（秒）- デフォルト10分
  
  # プレイヤー設定
  min-players: 2        # 最小プレイヤー数
  max-players: 30       # 最大プレイヤー数
  seeker-ratio: 0.23    # シーカーの割合（23%）
  
  # 捕獲設定
  capture-method: "attack"     # 捕獲方法（攻撃）
  capture-damage: false         # 実際のダメージを与えるか
  
  # 経済設定
  economy-enabled: true         # 経済システム連携
  winner-reward: 1000          # 勝利報酬
  participation-reward: 100    # 参加報酬
  per-capture-reward: 200      # 捕獲ごとの報酬（シーカー）
  
  # ショップ設定
  shop-enabled: true           # ショップ機能
  escape-item-price: 500       # 見逃しアイテムの価格
  escape-item-limit: 1         # 見逃しアイテムの購入制限
```

## ゲームメカニクス

### 1. 偽装システム
- **偽装方法**:
  - ショップGUIからブロックを選択
  - プレイヤーが立っている位置（足元）にブロックが設置される
  - プレイヤーは完全に透明化（不可視状態）
  - 元の場所は空気か液体なので復元不要
  
- **偽装解除条件**:
  - 一歩でも移動すると即座に解除
  - ブロックが空気に戻りプレイヤーが可視化
  - 再偽装するには再度ショップから選択

- **偽装中の仕様**:
  - プレイヤーは透明だが当たり判定は存在
  - 設置されたブロックも当たり判定あり
  - ブロックが攻撃されると捕獲
  - 同じ位置には1人のみ偽装可能

### 2. 捕獲システム
- **捕獲方法**: 
  - シーカーが偽装ブロックを攻撃（左クリック）
  - または可視状態のハイダーを直接攻撃
  - ダメージは与えない

- **捕獲後の処理**:
  - 捕獲されたハイダーは即座に観戦モードへ
  - シーカーに捕獲報酬を付与
  - 全体にアナウンス表示

### 3. 移動システム
- **ハイダーの移動**:
  - 偽装していない時は自由移動可能
  - 偽装中は完全に静止する必要あり
  - 一歩でも動くと偽装解除

- **シーカーの移動**:
  - 準備フェーズ中は移動不可（盲目状態）
  - 探索フェーズ中は自由移動
  - 移動速度ボーナスなし（デフォルト）

### 4. 経済システム連携
- **Vault連携**:
  - 必須依存プラグイン
  - ゲーム参加時の参加費設定可能
  - 勝利/敗北時の報酬自動付与
  
- **報酬体系**:
  - ハイダー勝利：1000コイン（デフォルト）
  - シーカー勝利：1000コイン + 捕獲数×200コイン
  - 参加報酬：100コイン（全員）

### 5. ショップシステム
- **アクセス制限**:
  - ゲーム参加者のみ使用可能
  - ゲーム中のみ開ける（/hs shop）
  - 役割によって表示内容が異なる

- **ハイダー用ショップ**:
  - **偽装ブロック選択**（無料）
    - 各種ブロックをGUIから選択
    - 選択すると即座に偽装
    - 石、木、土、砂など基本ブロック
  - **見逃しアイテム**（500コイン、1個限定）
    - 攻撃された時に自動発動
    - 5秒間の無敵時間
  - **追加アイテム**（今後実装予定）
    - スピードポーション（300コイン）
    - 一時的な透明化（800コイン）

- **シーカー用ショップ**:
  - **コンパス**（200コイン）
    - 最も近いハイダーの方向を示す
    - 使用後30秒間有効
  - **スピードブースト**（300コイン）
    - 30秒間移動速度上昇
  - **エリアスキャン**（500コイン）
    - 10ブロック範囲内のハイダー数を表示

### 6. マップシステム
- **マップタイプ**:
  - 既存マップ使用：現在のワールドをそのまま利用
  - エリア指定：座標で範囲を指定
  - マップ改変なし：ブロックは一時的な設置のみ

- **マップ要素**:
  - 境界設定（WorldBorder使用または座標チェック）
  - スポーン地点（シーカー待機所、ハイダー開始地点）
  - ゲーム中のブロック設置/破壊は禁止

## UI/UX

### スコアボード表示
```
===[ Hide and Seek ]===
時間: 08:45 / 10:00
----------------
シーカー: 3人
ハイダー: 7人 (残り)
捕獲済み: 3人
----------------
あなた: ハイダー
所持金: 2,500コイン
見逃し: 購入済み
```

### アクションバー
- ハイダー：「残り時間: [時間] | 偽装: [ブロック名/なし] | 見逃し: [有/無]」
- シーカー：「残り: [人数] | 捕獲数: [数]」

### サウンド
- ゲーム開始：NOTE_PLING
- 偽装成功：ITEM_PICKUP
- 偽装解除：BLOCK_BREAK
- ハイダー捕獲：LEVEL_UP
- 見逃しアイテム発動：ENDERMAN_TELEPORT
- ゲーム終了：FIREWORK_LAUNCH
- 残り1分警告：NOTE_BASS

### パーティクル
- 偽装時：SPELL_MOB
- 偽装解除：BLOCK_DUST
- 捕獲時：EXPLOSION_LARGE
- 見逃しアイテム発動：SPELL_MOB_AMBIENT
- リザルト表示：FIREWORKS_SPARK

## 技術仕様

### 必要な依存関係
- Paper API 1.21.x
- Vault（経済連携、必須）
- PlaceholderAPI（プレースホルダー対応、オプション）

### データ保存
```yaml
# プレイヤー統計（stats.yml）
players:
  UUID:
    games-played: 100
    games-won-seeker: 30
    games-won-hider: 45
    total-captures: 250
    total-escapes: 15
    total-earnings: 150000
    escape-items-used: 23
    achievements: []
```

### イベント
```kotlin
// カスタムイベント
class HideAndSeekStartEvent(val game: Game) : Event()
class PlayerDisguiseEvent(val player: Player, val block: Material) : Event()
class PlayerUnDisguiseEvent(val player: Player, val reason: String) : Event()
class PlayerCapturedEvent(val seeker: Player, val hider: Player) : Event()
class EscapeItemUsedEvent(val player: Player) : Event()
class GameEndEvent(val winners: List<Player>, val reason: EndReason) : Event()
class ShopPurchaseEvent(val player: Player, val item: ShopItem, val price: Int) : Event()
```

### パフォーマンス最適化
- 偽装ブロック：実ブロック設置（プレイヤー足元に直接設置）
- 透明化：ポーション効果で実装
- 移動検出：PlayerMoveEventで監視（0.1ブロック以上の移動で解除）
- 攻撃検出：BlockDamageEventで偽装ブロック判定
- ブロック管理：HashMap<Location, UUID>で位置とプレイヤー紐付け
- 経済処理：非同期で実行
- ゲーム終了時：全偽装ブロックを空気に戻す

## 実装優先順位

### フェーズ1（MVP）
1. 基本的なゲームループ（開始、役割分担、終了）
2. 攻撃による捕獲システム
3. 基本的なUI（スコアボード、チャット通知）
4. Vault連携による報酬システム
5. 1つのプリセットマップ

### フェーズ2（経済機能）
1. ショップGUIシステム
2. 見逃しアイテムの実装
3. 統計システム
4. 複数マップ対応
5. 言語ファイル（日本語・英語）

### フェーズ3（拡張機能）
1. 追加ショップアイテム
2. カスタムマップ作成ツール
3. 観戦モード
4. ランキングシステム
5. 実績システム

### フェーズ4（最適化）
1. パフォーマンス最適化
2. トーナメントモード
3. API公開
4. 他プラグイン連携拡張

## テスト項目

### 機能テスト
- [ ] ゲーム開始・終了の正常動作
- [ ] 役割分担のランダム性と比率
- [ ] 偽装システム（ブロック設置、透明化）
- [ ] 移動による偽装解除
- [ ] ブロック攻撃による捕獲
- [ ] 見逃しアイテムの動作
- [ ] 報酬計算の正確性
- [ ] 役割別ショップ表示

### 負荷テスト
- [ ] 30人同時プレイでのTPS維持
- [ ] 経済処理の非同期実行
- [ ] 大規模マップでのメモリ使用量
- [ ] 10分間ゲームの安定性

### 互換性テスト
- [ ] Paper 1.21.x での動作
- [ ] Vault最新版との互換性
- [ ] 他のミニゲームプラグインとの共存

## 今後の拡張案

### ゲームモード追加
1. **Team Mode**: チーム対抗戦
2. **Infection Mode**: 捕獲されたらシーカーになる
3. **Survival Mode**: 最後の1人になるまで継続
4. **Speed Round**: 3分ごとに役割交代
5. **Hardcore Mode**: 見逃しアイテムなし、報酬2倍

### 新機能
1. **追加ショップアイテム**: 
   - スピードブースト（30秒）
   - 透明化（10秒）
   - テレポート（ランダム位置）
2. **パワーアップシステム**: 連続勝利でボーナス
3. **デイリーボーナス**: ログインボーナス
4. **スペクテイターツール**: 配信者向け機能
5. **季節イベント**: 期間限定の特殊報酬

### 統合機能
1. **ギルドシステム**: チーム永続化
2. **シーズンパス**: 段階的報酬システム
3. **マッチメイキング**: スキルベースのマッチング
4. **ソーシャル機能**: フレンドシステム、招待機能

## 設定ファイル例

### config.yml
```yaml
# Hide and Seek Configuration
general:
  language: "ja_JP"
  auto-start: true
  min-players: 2
  max-players: 30
  lobby-countdown: 30

game:
  preparation-time: 30      # 準備時間（秒）
  seek-time: 600           # 探索時間（秒）
  seeker-ratio: 0.23       # シーカーの割合
  
capture:
  method: "ATTACK"         # 捕獲方法
  damage: false            # 実ダメージ
  
economy:
  enabled: true
  currency-symbol: "コイン"
  
  rewards:
    hider-win: 1000
    seeker-win: 1000
    per-capture: 200
    participation: 100
    
shop:
  enabled: true
  
  # ハイダー用アイテム
  hider-items:
    disguise-blocks:      # 偽装ブロック（無料）
      - STONE
      - DIRT
      - GRASS_BLOCK
      - OAK_LOG
      - COBBLESTONE
      - SAND
      - GRAVEL
    escape-pass:
      price: 500
      limit: 1
      duration: 5         # 無敵時間（秒）
      
  # シーカー用アイテム  
  seeker-items:
    compass:
      price: 200
      duration: 30       # 効果時間（秒）
    speed-boost:
      price: 300
      duration: 30
    area-scan:
      price: 500
      radius: 10         # スキャン範囲（ブロック）
```

### arenas.yml
```yaml
arenas:
  default:
    display-name: "メインワールド"
    world: "world"  # 既存ワールド名
    spawn-seeker:
      x: 100
      y: 64
      z: 100
    spawn-hider:
      x: 100
      y: 64
      z: 100
    boundaries:
      center-x: 100
      center-z: 100
      radius: 50  # 半径50ブロックの円形エリア
    # 既存マップを使用するため、破壊/設置保護を有効化
    protection:
      prevent-block-break: true
      prevent-block-place: true
      allowed-interactions:
        - DOOR
        - GATE
        - BUTTON
```

## 権限ノード
```yaml
hideandseek.play          # ゲーム参加
hideandseek.admin         # 管理コマンド
hideandseek.vip           # VIP機能（優先参加、特殊能力）
hideandseek.spectate      # 観戦
hideandseek.bypass        # 制限回避
hideandseek.arena.create  # アリーナ作成
hideandseek.arena.edit    # アリーナ編集
hideandseek.stats.view    # 統計閲覧
hideandseek.stats.reset   # 統計リセット
```

## コマンド一覧
```
/hideandseek (hs)
├── join [arena]        - ゲームに参加
├── leave               - ゲームから退出
├── shop                - ショップを開く
├── spectate [arena]    - 観戦モード
├── stats [player]      - 統計表示
├── top [category]      - ランキング表示
├── balance             - 所持金確認
└── admin
    ├── start [arena]   - ゲーム強制開始
    ├── stop [arena]    - ゲーム強制終了
    ├── setup [arena]   - アリーナセットアップ
    ├── give [player] [amount] - お金を付与
    ├── reload          - 設定リロード
    └── debug           - デバッグモード
```

## Active Technologies
- Kotlin 2.2.21 / Paper API 1.21.5 + Paper API、Vault API、Kotlin stdlib-jdk8 (003-seeker-shop)
- YAMLファイル（設定）、メモリ内管理（アクティブ効果） (003-seeker-shop)
- Kotlin 2.2.21 / JVM 21 + Paper API 1.21.5, Vault API 1.7, Kotlin stdlib-jdk8 (004-shop-items-review)
- YAML configuration files (config.yml, shop.yml), in-memory state management during games (004-shop-items-review)

## Recent Changes
- 003-seeker-shop: Added Kotlin 2.2.21 / Paper API 1.21.5 + Paper API、Vault API、Kotlin stdlib-jdk8
