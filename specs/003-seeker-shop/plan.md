# Implementation Plan: Seeker Shop System

**Branch**: `003-seeker-shop` | **Date**: 2025-11-03 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/003-seeker-shop/spec.md`

## Summary

シーカー専用のショップシステムを実装し、4種類の特殊アイテム（視界拡大、偽装ブロック発光、スピードブースト、攻撃範囲拡大）を提供します。既存のショップGUIフレームワーク（ShopManager、ShopCategory、ShopItem）を拡張し、役割（ハイダー/シーカー）に応じた表示切り替え機能を追加します。アイテム使用時の効果管理システム（ActiveEffect）を新規実装し、時限式効果の適用・解除・状態表示を行います。

## Technical Context

**Language/Version**: Kotlin 2.2.21 / Paper API 1.21.5
**Primary Dependencies**: Paper API、Vault API、Kotlin stdlib-jdk8
**Storage**: YAMLファイル（設定）、メモリ内管理（アクティブ効果）
**Testing**: JUnit 5、MockBukkit
**Target Platform**: Paper 1.21.5（Minecraft Server）
**Project Type**: 単一プロジェクト（Minecraftプラグイン）
**Performance Goals**: TPS 18以上維持、効果適用500ms以内、30人同時プレイ対応
**Constraints**: メモリ使用量500KB/プレイヤー、購入処理1秒以内、効果解除200ms以内
**Scale/Scope**: 4種類のアイテム、30人同時プレイ、複数の同時効果管理

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Note**: プロジェクト憲法（`.specify/memory/constitution.md`）はテンプレート状態であり、具体的なプロジェクト原則が定義されていません。そのため、以下の一般的なベストプラクティスを適用します：

### General Best Practices Applied

1. **既存コード基盤の再利用**
   - ✅ 既存のShopManagerを拡張（新規実装ではなく拡張）
   - ✅ ShopCategoryパターンを踏襲
   - ✅ 設定ファイル構造（shop.yml）を維持

2. **段階的な実装**
   - ✅ ユーザーストーリー優先度に基づくフェーズ分け
   - ✅ P1機能を優先実装（視界拡大、発光検出、ショップ統合）
   - ✅ P2機能を後続実装（スピード、攻撃範囲）

3. **テスト容易性**
   - ✅ 各ユーザーストーリーは独立してテスト可能
   - ✅ エフェクト管理システムを分離（単体テスト可能）
   - ✅ 設定駆動型設計（テスト時に値を変更可能）

4. **パフォーマンス考慮**
   - ✅ 非同期経済処理（Vault API）
   - ✅ メモリ効率的なエフェクト管理（HashMap使用）
   - ✅ タスクスケジューラによる効果時間管理

**Status**: ✅ **PASS** - 既存アーキテクチャとの整合性を保ちながら、段階的かつテスト可能な設計

## Project Structure

### Documentation (this feature)

```text
specs/003-seeker-shop/
├── plan.md              # This file
├── research.md          # Phase 0 output - 技術調査結果
├── data-model.md        # Phase 1 output - データモデル定義
├── quickstart.md        # Phase 1 output - 開発者向けクイックスタート
├── contracts/           # Phase 1 output - アイテム効果仕様
└── tasks.md             # Phase 2 output (will be created by /speckit.tasks)
```

### Source Code (repository root)

```text
src/main/kotlin/com/hideandseek/
├── shop/
│   ├── ShopManager.kt           # [EXTEND] 役割ベース表示追加
│   ├── ShopCategory.kt          # [EXISTING] カテゴリ管理
│   ├── ShopItem.kt              # [EXISTING] アイテムデータ
│   └── ShopAction.kt            # [EXISTING] アクション定義
├── effects/                     # [NEW] エフェクト管理システム
│   ├── EffectManager.kt         # アクティブ効果の管理
│   ├── EffectType.kt            # 効果タイプ定義
│   ├── ActiveEffect.kt          # 効果データクラス
│   └── ItemEffectHandler.kt     # アイテム使用時の効果適用
├── items/                       # [NEW] シーカーアイテム実装
│   ├── SeekerItemHandler.kt     # アイテム使用ハンドラー
│   ├── VisionEnhancerItem.kt    # 視界拡大アイテム
│   ├── GlowDetectorItem.kt      # 偽装ブロック発光アイテム
│   ├── SpeedBoostItem.kt        # スピードブーストアイテム
│   └── ReachExtenderItem.kt     # 攻撃範囲拡大アイテム
├── listeners/
│   ├── ShopListener.kt          # [EXTEND] シーカーアイテム購入処理
│   ├── PlayerInteractListener.kt # [EXTEND] アイテム使用検出
│   └── EffectCleanupListener.kt # [NEW] ゲーム終了時のクリーンアップ
├── config/
│   └── ShopConfig.kt            # [EXTEND] シーカーアイテム設定読み込み
├── scoreboard/
│   └── ScoreboardManager.kt     # [EXTEND] 効果残り時間表示
└── utils/
    └── EffectScheduler.kt       # [NEW] 効果時間管理タスク

src/main/resources/
├── shop.yml                     # [EXTEND] シーカーアイテム設定追加
└── config.yml                   # [EXTEND] エフェクト関連設定追加

tests/
└── kotlin/com/hideandseek/
    ├── effects/
    │   ├── EffectManagerTest.kt
    │   └── ItemEffectHandlerTest.kt
    └── items/
        ├── VisionEnhancerItemTest.kt
        ├── GlowDetectorItemTest.kt
        ├── SpeedBoostItemTest.kt
        └── ReachExtenderItemTest.kt
```

**Structure Decision**: 既存の単一プロジェクト構造を維持し、新規パッケージ`effects/`と`items/`を追加します。既存のショップシステム（`shop/`）は拡張のみで、アーキテクチャの大幅な変更は行いません。これにより、既存のハイダー用ショップ機能との共存が容易になります。

## Complexity Tracking

> **憲法違反がある場合のみ記入**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| N/A       | N/A        | N/A                                 |

**Note**: 憲法が未定義のため、違反は存在しません。既存コードベースとの整合性を最優先します。

## Phase 0: Research & Design Decisions

### Research Areas

1. **Paper API エフェクト実装パターン**
   - PotionEffect APIの使用方法
   - カスタムエフェクトの実装パターン
   - エフェクト時間管理のベストプラクティス

2. **視界拡大の実装方法**
   - 描画距離（View Distance）の動的変更
   - 暗視効果（Night Vision）の適用
   - クライアント設定への影響分析

3. **発光エフェクトのプレイヤー限定表示**
   - パーティクルのプレイヤー限定送信
   - Glowing効果の選択的表示
   - パフォーマンスへの影響評価

4. **攻撃範囲拡大の実装**
   - レイキャストによる距離判定
   - 攻撃イベントのインターセプト
   - 既存の捕獲システムとの統合

5. **効果の重複処理**
   - 同一効果の上書き vs 延長
   - 異なる効果の並行適用
   - 効果終了時の正確なクリーンアップ

### Expected Outputs

- `research.md`: 各調査項目の結果、選択した実装方法、根拠
- 実装方針の確定（視界拡大はNight Vision + 描画距離変更など）
- パフォーマンスベンチマーク基準の設定

## Phase 1: Design & Contracts

### Data Model

**新規エンティティ**:

1. **ActiveEffect（アクティブ効果）**
   - プレイヤーUUID
   - 効果タイプ（VISION、GLOW、SPEED、REACH）
   - 開始時刻（Instant）
   - 終了時刻（Instant）
   - 効果強度（倍率、秒数など）
   - タスクID（BukkitTask）

2. **SeekerItem（シーカーアイテム定義）**
   - アイテムID（文字列）
   - 表示名
   - アイコン（Material）
   - 価格
   - 効果タイプ
   - 効果時間
   - クールダウン時間
   - 購入制限

**既存エンティティの拡張**:

1. **ShopCategory**
   - 役割フィルター（HIDER、SEEKER、ALL）を追加

2. **ShopItem**
   - 効果タイプ（EffectType）を追加
   - 使用可能条件（役割、フェーズ）を追加

### API Contracts

#### EffectManager API

```kotlin
interface EffectManager {
    // 効果の適用
    fun applyEffect(player: Player, effectType: EffectType, duration: Int): Boolean

    // 効果の解除
    fun removeEffect(player: Player, effectType: EffectType)

    // 全効果の解除
    fun removeAllEffects(player: Player)

    // 効果の確認
    fun hasEffect(player: Player, effectType: EffectType): Boolean

    // 効果の取得
    fun getActiveEffect(player: Player, effectType: EffectType): ActiveEffect?

    // 残り時間の取得
    fun getRemainingTime(player: Player, effectType: EffectType): Long
}
```

#### ItemEffectHandler API

```kotlin
interface ItemEffectHandler {
    // アイテム使用処理
    fun handleItemUse(player: Player, item: ItemStack): Boolean

    // 効果の検証（購入制限、クールダウン）
    fun canUseItem(player: Player, itemId: String): Boolean

    // クールダウン設定
    fun setCooldown(player: Player, itemId: String, seconds: Int)
}
```

#### Shop Extension API

```kotlin
// ShopManagerの拡張
fun ShopManager.getItemsForRole(role: PlayerRole): List<ShopItem>
fun ShopManager.purchaseItem(player: Player, itemId: String): PurchaseResult
```

### Configuration Contracts

#### shop.yml 拡張

```yaml
categories:
  seeker-items:
    display-name: "&cSeeker Items"
    icon: ENDER_EYE
    slot: 11
    role-filter: SEEKER  # 新規フィールド
    description:
      - "&7Special items for seekers"

    items:
      vision-enhancer:
        material: ENDER_PEARL
        display-name: "&bVision Enhancer"
        price: 300
        effect-type: VISION
        effect-duration: 30
        slot: 10
        lore:
          - "&7Enhances your vision for 30 seconds"
          - "&e&lPrice: 300 coins"

      glow-detector:
        material: GLOWSTONE_DUST
        display-name: "&eGlow Detector"
        price: 500
        effect-type: GLOW
        effect-duration: 15
        slot: 11
        lore:
          - "&7Makes disguised blocks glow for 15 seconds"
          - "&e&lPrice: 500 coins"

      speed-boost:
        material: SUGAR
        display-name: "&aSpeed Boost"
        price: 200
        effect-type: SPEED
        effect-duration: 30
        slot: 12
        lore:
          - "&7Increases movement speed for 30 seconds"
          - "&e&lPrice: 200 coins"

      reach-extender:
        material: STICK
        display-name: "&6Reach Extender"
        price: 250
        effect-type: REACH
        effect-duration: 30
        slot: 13
        lore:
          - "&7Extends attack range for 30 seconds"
          - "&e&lPrice: 250 coins"
```

### Expected Outputs

- `data-model.md`: エンティティ定義、リレーションシップ図
- `contracts/effect-manager.md`: EffectManager APIの詳細仕様
- `contracts/item-effects.md`: 各アイテム効果の詳細仕様
- `contracts/shop-extension.md`: ショップ拡張APIの詳細仕様
- `quickstart.md`: 開発者向けセットアップガイド

## Phase 2: Implementation Planning

*この計画は `/speckit.tasks` コマンドで `tasks.md` に展開されます*

### Implementation Phases (User Story Order)

#### User Story 5: Shop Access and Economy Integration (P1) - 基盤
- ショップカテゴリに役割フィルター追加
- ShopManagerにシーカーアイテム読み込み機能追加
- 購入処理にVault連携追加
- エラーハンドリング（所持金不足、インベントリ満杯）

#### User Story 1: Enhanced Vision Item (P1)
- EffectManagerの実装
- VisionEnhancerItemの実装
- Night Vision + View Distance変更
- 効果時間管理タスク
- アクションバーへの残り時間表示

#### User Story 2: Disguise Block Detection Item (P1)
- GlowDetectorItemの実装
- 偽装ブロック位置取得（DisguiseManager連携）
- プレイヤー限定パーティクル送信
- 偽装解除時のエフェクト即座削除

#### User Story 3: Speed Boost Item (P2)
- SpeedBoostItemの実装
- Speed PotionEffectの適用
- 効果終了時のクリーンアップ

#### User Story 4: Extended Reach Item (P2)
- ReachExtenderItemの実装
- レイキャストによる距離判定拡張
- BlockDamageListenerの拡張

### Testing Strategy

- 単体テスト: 各ItemHandlerとEffectManager
- 統合テスト: ショップ購入 → アイテム使用 → 効果適用の全フロー
- パフォーマンステスト: 30人同時、複数効果、TPS測定
- エッジケーステスト: 効果重複、ゲーム終了時、role変更時

### Performance Considerations

1. **メモリ管理**
   - ActiveEffectはHashMap<UUID, Map<EffectType, ActiveEffect>>で管理
   - ゲーム終了時に全クリア
   - 効果終了時に即座に削除

2. **タスクスケジューリング**
   - BukkitSchedulerで1秒ごとに残り時間更新
   - 効果終了時にタスクキャンセル
   - 非同期処理は使用しない（同期優先）

3. **パーティクル送信**
   - プレイヤー限定送信でネットワーク負荷軽減
   - 発光エフェクトは毎tick送信ではなく間隔を開ける

## Integration Points

### 既存システムとの連携

1. **DisguiseManager**
   - 偽装ブロック位置の取得
   - 偽装解除イベントの監視

2. **GameManager**
   - ゲーム終了イベントでの効果全解除
   - フェーズ変更時の制御

3. **ShopManager**
   - カテゴリ表示の役割フィルタリング
   - アイテム購入処理

4. **Vault API**
   - 経済処理（非同期）
   - 所持金チェック・引き落とし

5. **ScoreboardManager**
   - アクティブ効果の表示
   - 残り時間のリアルタイム更新

## Migration Strategy

既存機能への影響を最小化するため、以下の戦略を採用：

1. **後方互換性の維持**
   - 既存のハイダー用ショップは変更なし
   - ShopCategoryにrole-filterフィールドを追加（オプション、デフォルトはALL）

2. **段階的なロールアウト**
   - Phase 1: 基盤システム（EffectManager）のみ実装
   - Phase 2: 1つずつアイテムを追加しテスト
   - Phase 3: 全アイテム統合テスト

3. **設定の分離**
   - shop.ymlに新規カテゴリ追加（既存カテゴリは変更なし）
   - 設定ミスによる既存機能への影響を防止

## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| 視界拡大がクライアント設定に依存 | Medium | Research phase で実装方法を確定、フォールバックを用意 |
| 発光エフェクトのパフォーマンス低下 | High | 送信間隔を調整、プレイヤー限定送信を徹底 |
| 効果の重複による不具合 | Medium | 明確な上書きルールを実装、テストケースを網羅 |
| 既存ショップシステムへの影響 | Low | 役割フィルターをオプションにし、既存カテゴリは変更なし |
| Vault API の非同期処理 | Low | 既存実装を参考に、エラーハンドリングを徹底 |

## Success Metrics

実装完了時に以下を満たすこと：

- ✅ 全20の機能要件が実装されている
- ✅ 各ユーザーストーリーが独立してテスト可能
- ✅ TPS 18以上を維持（30人同時プレイ）
- ✅ 購入処理1秒以内、効果適用500ms以内
- ✅ メモリ使用量500KB/プレイヤー以下
- ✅ 既存ハイダー用ショップに影響なし
- ✅ 全エッジケースがハンドリングされている

## Next Steps

1. Run `/speckit.tasks` to generate detailed task breakdown
2. Execute Phase 0 research (視界拡大、発光エフェクト実装方法の確定)
3. Create data model and contracts (Phase 1)
4. Begin implementation following tasks.md
