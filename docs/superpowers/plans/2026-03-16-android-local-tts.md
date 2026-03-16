# Android Local TTS Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a system-wide local Android TTS engine using Sherpa-ONNX and Kokoro-82M Japanese model.

**Architecture:** A `TextToSpeechService` implementation that uses Sherpa-ONNX `OfflineTts` to synthesize speech. Assets are copied to internal storage on first run for fast access.

**Tech Stack:** Kotlin, Android SDK, Sherpa-ONNX (ONNX Runtime), Kokoro-82M v1.0 (Japanese).

---

## Chunk 1: Project Scaffolding & Dependencies

### Task 1.1: Create Android Project Structure
**Files:**
- Create: `app/build.gradle`
- Create: `settings.gradle`
- Create: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Define root project structure**
- [ ] **Step 2: Add Sherpa-ONNX and Coroutine dependencies to `app/build.gradle`**
```gradle
dependencies {
    implementation 'com.k2fsa.sherpa.onnx:sherpa-onnx-android:latest.release'
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
}
```
- [ ] **Step 3: Commit**
```bash
git add app/build.gradle settings.gradle app/src/main/AndroidManifest.xml
git commit -m "chore: initial project scaffolding with sherpa-onnx dependencies"
```

### Task 1.2: Prepare Assets Directory
**Files:**
- Create: `app/src/main/assets/kokoro/`

- [ ] **Step 1: Create the kokoro directory in assets**
- [ ] **Step 2: Note: Actual model files will be added later or assumed to be provided**
- [ ] **Step 3: Commit**
```bash
git add app/src/main/assets/kokoro/
git commit -m "chore: create assets directory for kokoro model"
```

---

## Chunk 2: Model Management

### Task 2.1: Implement Asset Manager
**Files:**
- Create: `app/src/main/java/com/example/androidlocaltts/utils/ModelManager.kt`

- [ ] **Step 1: Implement `copyAssets` function to copy models to `context.filesDir`**
- [ ] **Step 2: Implement check for already copied files**
- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/example/androidlocaltts/utils/ModelManager.kt
git commit -m "feat: add ModelManager to handle asset extraction"
```

---

## Chunk 3: TTS Engine Implementation

### Task 3.1: Implement SherpaOnnxTtsEngine
**Files:**
- Create: `app/src/main/java/com/example/androidlocaltts/engine/SherpaOnnxTtsEngine.kt`

- [ ] **Step 1: Create `SherpaOnnxTtsEngine` with Mutex for thread safety**
- [ ] **Step 2: Implement `synthesize(text, speed, callback)`**
    - Use `kotlinx.coroutines.sync.Mutex` to protect `OfflineTts.generate`.
    - Handle text chunking (split by punctuation) to reduce latency.
- [ ] **Step 3: Implement chunked output to `SynthesisCallback`**
- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/example/androidlocaltts/engine/SherpaOnnxTtsEngine.kt
git commit -m "feat: implement robust SherpaOnnxTtsEngine with Mutex and chunking"
```

### Task 3.2: Implement KokoroTtsService
**Files:**
- Create: `app/src/main/java/com/example/androidlocaltts/service/KokoroTtsService.kt`
- Create: `app/src/main/res/xml/tts_engine.xml`

- [ ] **Step 1: Extend `TextToSpeechService` with non-blocking initialization**
- [ ] **Step 2: Implement `onSynthesizeText` with state checking (Model Ready?)**
- [ ] **Step 3: Implement `onStop()` to cancel ongoing synthesis tasks**
- [ ] **Step 4: Register service in `AndroidManifest.xml`**
- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/example/androidlocaltts/service/KokoroTtsService.kt app/src/main/res/xml/tts_engine.xml app/src/main/AndroidManifest.xml
git commit -m "feat: implement lifecycle-aware KokoroTtsService"
```

---

## Chunk 4: UI & Settings

### Task 4.1: Implement Settings UI
**Files:**
- Create: `app/src/main/java/com/example/androidlocaltts/ui/MainActivity.kt`
- Create: `app/src/main/java/com/example/androidlocaltts/ui/SettingsActivity.kt`

- [ ] **Step 1: Create `MainActivity` to show model status and "Open System TTS Settings" button**
- [ ] **Step 2: Create `SettingsActivity` for future configuration (optional for now)**
- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/example/androidlocaltts/ui/MainActivity.kt app/src/main/java/com/example/androidlocaltts/ui/SettingsActivity.kt
git commit -m "feat: add basic UI and settings activity"
```

---

## Chunk 5: Verification

### Task 5.1: Build and Verify
**Files:**
- None (Command-line validation)

- [ ] **Step 1: Run `./gradlew assembleDebug` to verify build**
- [ ] **Step 2: Verify that the service is visible in Android TTS settings (Manual verification required)**
