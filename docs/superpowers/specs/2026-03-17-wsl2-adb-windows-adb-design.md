# WSL2 から Windows の ADB を使う運用（自動接続寄り）

## 1. 概要
WSL2 で USB パススルーを使わず、Windows 側の `adb.exe` を WSL2 から呼び出して端末を即時認識できるようにする。端末接続時に追加の手動操作を不要にすることを目標とする。

## 2. 目的・要件
- 端末を接続したら、WSL2 で `adb devices` がすぐ使えること。
- USB パススルー（usbipd）や WSL2 側の udev 設定に依存しない運用。
- 既存の WSL2 作業フローをできるだけ崩さない。

## 3. 前提
- Windows に Android Studio が入っており、`adb.exe` が存在する。
- `adb.exe` の場所:
  - `C:\Users\yuich\AppData\Local\Android\Sdk\platform-tools\adb.exe`

## 4. 推奨アプローチ
### 4.1. 方式
- Windows のユーザー `PATH` に `platform-tools` を追加。
- WSL2 側の `PATH` に `/mnt/c/Users/yuich/AppData/Local/Android/Sdk/platform-tools` を追加。
- WSL2 で `adb` を実行すると Windows の `adb.exe` が起動する。

### 4.2. 期待される動作
- 端末接続 → Windows が USB を認識 → WSL2 で `adb devices` が即利用可能。
- USB パススルーや WSL2 の udev ルールは不要。

## 5. 設定手順（設計）
### 5.1. Windows 側
- ユーザー環境変数 `PATH` に以下を追加:
  - `C:\Users\yuich\AppData\Local\Android\Sdk\platform-tools`

### 5.2. WSL2 側
- `~/.bashrc` などに以下を追加:
  - `export PATH="$PATH:/mnt/c/Users/yuich/AppData/Local/Android/Sdk/platform-tools"`

## 6. 検証
- WSL2 で `which adb` が `/mnt/c/.../platform-tools/adb.exe` を指すこと。
- WSL2 で `adb devices` が端末を `device` として表示すること。

## 7. リバート方針（USB パススルー運用の撤去）
- Windows:
  - `usbipd detach --busid <BUSID>` で分離
  - 不要なら `usbipd-win` をアンインストール
- WSL2:
  - `/etc/udev/rules.d/51-android.rules` を削除
  - `plugdev` グループを削除

## 8. リスクと対策
- **WSL2 側の PATH が反映されない**
  - シェル再起動や `source ~/.bashrc` を実施
- **`adb.exe` の場所変更**
  - Android SDK の場所が変わった場合は PATH を更新
