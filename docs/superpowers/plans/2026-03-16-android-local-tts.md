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

## Chunk 2: Model Management & Testing

### Task 2.1: Implement Robust ModelManager
**Files:**
- Create/Modify: `app/src/main/java/com/example/androidlocaltts/utils/ModelManager.kt`
- Test: `app/src/test/java/com/example/androidlocaltts/utils/ModelManagerTest.kt`

- [ ] **Step 1: Write failing test for recursive asset checking**
    - `isModelPrepared` should check for `model.onnx`, `tokens.txt`, `voices.bin`, and `espeak-ng-data/` directory.
- [ ] **Step 2: Update `isModelPrepared` to include all required files/directories**
- [ ] **Step 3: Write failing test (using Robolectric if possible, or Mockito) for recursive asset copying**
    - `copyAssets` should correctly copy nested directories like `espeak-ng-data/`.
- [ ] **Step 4: Update `copyAssets` to be recursive**
- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/example/androidlocaltts/utils/ModelManager.kt app/src/test/java/com/example/androidlocaltts/utils/ModelManagerTest.kt
git commit -m "feat: implement recursive asset copying and comprehensive model check"
```

---

## Chunk 3: TTS Engine Implementation & Testing

### Task 3.1: Implement Robust SherpaOnnxTtsEngine
**Files:**
- Modify: `app/src/main/java/com/example/androidlocaltts/engine/SherpaOnnxTtsEngine.kt`
- Test: `app/src/test/java/com/example/androidlocaltts/engine/SherpaOnnxTtsEngineTest.kt`

- [ ] **Step 1: Write failing test for synthesis lifecycle**
    - Verify `callback.start()` and `callback.done()` are called even for empty strings or errors.
- [ ] **Step 2: Update `synthesize` to guarantee `done()` or `error()` call**
- [ ] **Step 3: Write failing test for thread-safe release**
    - Verify `release()` waits for ongoing `synthesize()` via Mutex.
- [ ] **Step 4: Update `release()` and `synthesize()` to use Mutex consistently**
- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/example/androidlocaltts/engine/SherpaOnnxTtsEngine.kt app/src/test/java/com/example/androidlocaltts/engine/SherpaOnnxTtsEngineTest.kt
git commit -m "feat: guarantee synthesis lifecycle and thread-safe release"
```

### Task 3.2: Implement Robust KokoroTtsService
**Files:**
- Modify: `app/src/main/java/com/example/androidlocaltts/service/KokoroTtsService.kt`

- [ ] **Step 1: Update `isEngineReady` to use `@Volatile` or `AtomicBoolean`**
- [ ] **Step 2: Implement cancellation logic in `onStop()`**
- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/example/androidlocaltts/service/KokoroTtsService.kt
git commit -m "feat: improve service thread safety and cancellation"
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

## Chunk 6: Final Robustness Refinements

### Task 6.1: Strict Model Verification
**Files:**
- Modify: `app/src/main/java/com/example/androidlocaltts/utils/ModelManager.kt`

- [x] **Step 1: Update `isModelPrepared` to check `isDirectory()` for `espeak-ng-data`**
- [x] **Step 2: Commit**
```bash
git add app/src/main/java/com/example/androidlocaltts/utils/ModelManager.kt
git commit -m "fix: enforce directory check for espeak-ng-data in ModelManager"
```

### Task 6.2: Responsive Cancellation and Callback Consistency
**Files:**
- Modify: `app/src/main/java/com/example/androidlocaltts/engine/SherpaOnnxTtsEngine.kt`

- [x] **Step 1: Add `kotlinx.coroutines.yield()` or `isActive` check inside synthesis loop**
    - Ensure `synthesize` respects coroutine cancellation from `KokoroTtsService`.
- [x] **Step 2: Refactor error handling to ensure consistent `start/error` or `start/done` sequence**
    - Avoid cases where only `error()` is called after `start()`.
- [x] **Step 3: Commit**
```bash
git add app/src/main/java/com/example/androidlocaltts/engine/SherpaOnnxTtsEngine.kt
git commit -m "fix: implement responsive cancellation and consistent synthesis callbacks"
```

### Task 6.3: Enhanced Service Reliability
**Files:**
- Modify: `app/src/main/java/com/example/androidlocaltts/service/KokoroTtsService.kt`

- [x] **Step 1: Add simple retry logic or improved error reporting when engine is initializing**
- [x] **Step 2: Commit**
```bash
git add app/src/main/java/com/example/androidlocaltts/service/KokoroTtsService.kt
git commit -m "fix: improve service reliability during initialization"
```
