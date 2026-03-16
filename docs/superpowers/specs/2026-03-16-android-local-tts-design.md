# Android Local TTS Engine with Kokoro-82M (Sherpa-ONNX)

## 1. 概要
Android システム全体で利用可能な、ローカル完結型の日本語テキスト読み上げ（TTS）エンジンを構築する。音声合成には軽量かつ高品質な Kokoro-82M モデルを使用し、Android への統合には Sherpa-ONNX フレームワークを採用する。

## 2. 目的・要件
- **ローカル実行**: インターネット接続なしで音声合成が可能であること。
- **システム統合**: Android の「優先するエンジン」として設定可能であること。
- **日本語対応**: 日本語テキストの解析（G2P）および音声合成に対応すること。
- **デフォルト音声**: 日本語男性ボイス `jm_kumo` をデフォルトとして採用。
- **ターゲットOS**: Android 8.0 (API 26) 以上。

## 3. 技術スタック
- **開発言語**: Kotlin / Java (Android SDK)
- **コアライブラリ**: [Sherpa-ONNX](https://k2-fsa.github.io/sherpa/onnx/index.html)
- **推論エンジン**: ONNX Runtime (Sherpa-ONNX 内蔵)
- **モデル**: Kokoro-82M v1.0 (Japanese version)
- **ビルドツール**: Gradle

## 4. システムアーキテクチャ
### 4.1. 主要コンポーネント
1.  **`KokoroTtsService`**: `TextToSpeechService` を継承し、システムからの合成リクエストを処理する。
2.  **`SherpaOnnxEngine`**: Sherpa-ONNX の `OfflineTts` を管理し、実際の音声合成（推論）を行う。
3.  **`AssetManager`**: APK 内の `assets` に同梱された巨大なモデルファイルを、初回起動時に内部ストレージへ展開する。
4.  **`SettingsActivity`**: 音声のテスト再生や、基本的な設定（速度、ピッチなど）を行う UI。

### 4.2. データフロー
1.  **リクエスト受信**: システムまたは他アプリから `onSynthesizeText` が呼ばれる。
2.  **前処理**: テキストをクレンジングし、Sherpa-ONNX の日本語 G2P モジュールに渡す。
3.  **音声合成**: `jm_kumo` モデルを使用して PCM データを生成。
4.  **音声出力**: `SynthesisCallback` を介してシステムにオーディオデータをストリーミング。

## 5. 実装のポイント
- **モデルの同梱**: 約 100MB の ONNX モデルと辞書ファイルを `assets` に含める。
- **初回起動処理**: モデルの展開状況をチェックし、必要に応じてコピーを行う（`SharedPreferences` で管理）。
- **メモリ最適化**: TTS サービスがアイドル状態になった際の適切なリソース解放。

## 6. テスト計画
- **単体テスト**: テキストから音声データが正しく生成されるかの検証。
- **結合テスト**: Android システム設定からのエンジン登録と切り替えの検証。
- **性能テスト**: 低スペック端末での推論速度とメモリ消費量の確認。
