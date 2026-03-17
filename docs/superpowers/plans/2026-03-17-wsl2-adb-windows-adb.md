# WSL2 Windows ADB Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** USB パススルー無しで、WSL2 から Windows の `adb.exe` を使って端末接続後すぐに `adb devices` が使えるようにする。

**Architecture:** Windows の Android SDK `platform-tools` を PATH に追加し、WSL2 側の PATH に `/mnt/c/.../platform-tools` を追加して `adb.exe` を呼び出す。WSL2 の USB パススルー関連設定は撤去する。

**Tech Stack:** Windows PowerShell, WSL2 (bash), Android SDK Platform Tools (`adb.exe`)

---

## Chunk 1: Windows 側の ADB 利用準備

### Task 1: Windows PATH に platform-tools を追加

**Files:**
- Modify: Windows ユーザー環境変数 `PATH`

- [ ] **Step 1: 現在の PATH を確認**

Run (PowerShell):
```powershell
[Environment]::GetEnvironmentVariable("Path", "User")
```
Expected: 既存の PATH が表示される。

- [ ] **Step 2: platform-tools を PATH に追加**

Run (PowerShell):
```powershell
$pt = "C:\Users\yuich\AppData\Local\Android\Sdk\platform-tools"
$path = [Environment]::GetEnvironmentVariable("Path", "User")
if (-not ($path -split ';' | Where-Object { $_ -eq $pt })) {
  [Environment]::SetEnvironmentVariable("Path", "$path;$pt", "User")
}
```
Expected: エラーが出ない。

- [ ] **Step 3: PowerShell を再起動して反映確認**

Run (PowerShell, 新しいウィンドウ):
```powershell
where adb
```
Expected: `C:\Users\yuich\AppData\Local\Android\Sdk\platform-tools\adb.exe` が表示される。

- [ ] **Step 4: Windows で adb が動くことを確認**

Run (PowerShell):
```powershell
adb version
adb devices
```
Expected: `Android Debug Bridge version ...` と、接続端末が `device` 表示。

- [ ] **Step 5: Commit**

```bash
git status --short
# (ドキュメント以外の変更がないことを確認)
```
Expected: 変更なし。

## Chunk 2: WSL2 から Windows ADB を利用

### Task 2: WSL2 の PATH に Windows platform-tools を追加

**Files:**
- Modify: `~/.bashrc`

- [ ] **Step 1: 追加済みか確認**

Run (WSL2):
```bash
grep -n "platform-tools" ~/.bashrc || true
```
Expected: 何も出ない or 既存行が表示。

- [ ] **Step 2: PATH 追記**

Run (WSL2):
```bash
echo 'export PATH="$PATH:/mnt/c/Users/yuich/AppData/Local/Android/Sdk/platform-tools"' >> ~/.bashrc
```
Expected: エラーなし。

- [ ] **Step 3: 反映**

Run (WSL2):
```bash
source ~/.bashrc
```
Expected: エラーなし。

- [ ] **Step 4: WSL2 で adb の参照先確認**

Run (WSL2):
```bash
which adb
```
Expected: `/mnt/c/Users/yuich/AppData/Local/Android/Sdk/platform-tools/adb.exe`

- [ ] **Step 5: WSL2 から adb が動くことを確認**

Run (WSL2):
```bash
adb version
adb devices
```
Expected: 端末が `device` 表示。

- [ ] **Step 6: Commit**

```bash
git status --short
# (ドキュメント以外の変更がないことを確認)
```
Expected: 変更なし。

## Chunk 3: USB パススルー設定のリバート

### Task 3: Windows 側の usbipd 設定撤去

**Files:**
- Modify: Windows の USB アタッチ状態

- [ ] **Step 1: デバイス分離（必要なら）**

Run (PowerShell):
```powershell
usbipd list
usbipd detach --busid <BUSID>
```
Expected: 対象 BUSID が Detach される。

- [ ] **Step 2: usbipd-win をアンインストール（任意）**

Run (PowerShell):
```powershell
winget uninstall --id dorssel.usbipd-win -e
```
Expected: アンインストール完了。

- [ ] **Step 3: Commit**

```bash
git status --short
# (ドキュメント以外の変更がないことを確認)
```
Expected: 変更なし。

### Task 4: WSL2 側の udev 設定撤去

**Files:**
- Modify: `/etc/udev/rules.d/51-android.rules`

- [ ] **Step 1: ルール削除**

Run (WSL2):
```bash
sudo rm -f /etc/udev/rules.d/51-android.rules
```
Expected: エラーなし。

- [ ] **Step 2: udev 反映**

Run (WSL2):
```bash
sudo udevadm control --reload-rules
sudo udevadm trigger
```
Expected: エラーなし。

- [ ] **Step 3: plugdev グループ削除**

Run (WSL2):
```bash
sudo groupdel plugdev
```
Expected: グループが存在しない場合はエラーになる可能性あり。

- [ ] **Step 4: 動作確認**

Run (WSL2):
```bash
adb devices
```
Expected: 端末が `device` 表示（Windows adb 経由）。

- [ ] **Step 5: Commit**

```bash
git status --short
# (ドキュメント以外の変更がないことを確認)
```
Expected: 変更なし。
