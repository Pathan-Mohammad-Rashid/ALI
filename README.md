# ALI -• Advanced Learning Interface

**ALI -• Advanced Learning Interface** is a 100% free, unlimited, privacy-first Native Android AI companion built with **Kotlin, Jetpack Compose, Room SQLite, Android System Assistant & Accessibility APIs, and C++ `llama.cpp` JNI**.

Unlike robotic chatbots or cloud wrappers that harvest your personal conversations, **ALI lives strictly on your phone**:
- **100% Offline Brain**: Works in airplane mode with zero subscriptions, zero API keys, and zero token limits using a hybrid architecture:
  1. **Autonomous Cognitive & Human Persona Engine**: Works immediately out-of-the-box with zero download required—featuring emotional tone perception, inner-thought reasoning traces, episodic personal memory extraction, local TF-IDF cosine-similarity semantic retrieval, and deterministic math/unit logic.
  2. **On-Device GGUF LLM Engine (`llama.cpp` JNI)**: Download free open-weights quantized GGUF models (`SmolLM2-360M`, `Llama-3.2-1B`, `Qwen2.5-1.5B`) directly inside the **Models** tab for local ARM64 tensor inference.
- **Zero-Data-Leak Private Online Curiosity Engine**:
  - Controlled by a hardware-style **Privacy Firewall switch** (`100% Offline` vs `Curiosity ON`).
  - While **Offline**, any unfamiliar topic you ask about is stripped of personal phrasing and placed into your **Offline Curiosity Queue**.
  - When you allow **Internet Access (`Curiosity ON`)**, ALI's `PrivacyFirewall` strips all personal identifiers (names, phone numbers, emails, first-person pronouns, and conversational framing) and sends **only the anonymous encyclopedic topic** over a cookie-free connection to Wikipedia / DuckDuckGo Instant Graph, indexing the learned summary permanently into your phone's local SQLite database.
  - Every query and blocked event is recorded in the **Transparency & Privacy Audit Log** so you can verify that zero personal data ever leaves your device.
- **Human Voice & Prosody**:
  - Hands-free voice conversation via Android `SpeechRecognizer` (with offline recognition preference) paired with emotion-adaptive `TextToSpeech` pitch and cadence.
- **Day-to-Day Mobile Interactions**:
  - Set alarms & timers (`AlarmClock`), schedule calendar events (`CalendarContract`), draft SMS messages, launch installed apps by name, and manage persistent **Smart Tasks** and **Quick Notes**.
  - Registers for `android.intent.action.ASSIST` and `VOICE_COMMAND` so you can set ALI as your phone's default digital assistant.

---

## Project Structure

- [`app/src/main/AndroidManifest.xml`](app/src/main/AndroidManifest.xml) — System Assistant (`ACTION_ASSIST`), AccessibilityService, and strict `allowBackup="false"` privacy configuration.
- [`app/src/main/cpp/`](app/src/main/cpp/) — Native C++17 JNI bridge (`llama_jni.cpp` & `CMakeLists.txt`) for GGUF model inspection and `llama.cpp` inference.
- [`app/src/main/java/com/ali/agent/ai/`](app/src/main/java/com/ali/agent/ai/)
  - `CognitiveOrchestrator.kt` — Central reasoning pipeline coordinating memory extraction, mobile actions, math/logic, private online curiosity, and GGUF/Autonomous synthesis.
  - `AutonomousCognitiveEngine.kt` — Multi-stage human response synthesizer with visible inner-thought traces.
  - `HumanPersonaEngine.kt` — Emotional subtext detector, time-of-day awareness, and GGUF system prompt builder.
  - `SemanticMemoryEngine.kt` — Automatic personal fact & offline teaching extractor + TF-IDF cosine-similarity vector retrieval.
  - `MathAndLogicEngine.kt` — Offline recursive-descent arithmetic calculator, percentage solver, and unit converter.
- [`app/src/main/java/com/ali/agent/privacy/`](app/src/main/java/com/ali/agent/privacy/)
  - `PrivacyFirewall.kt` — PII & personal context sanitizer + cookie-less allow-listed HTTP client.
  - `PrivateCuriosityEngine.kt` — Anonymous Wikipedia & DuckDuckGo knowledge synthesis + Offline Curiosity Queue auto-sync.
- [`app/src/main/java/com/ali/agent/system/`](app/src/main/java/com/ali/agent/system/)
  - `MobileActionExecutor.kt` — Executes alarms, timers, tasks, notes, SMS drafts, calendar events, and app launches.
  - `AliVoiceManager.kt` — Offline-preferred Speech-to-Text + emotion-modulated Text-to-Speech.
  - `AliAccessibilityService.kt` — Optional on-device foreground app context helper.
- [`app/src/main/java/com/ali/agent/ui/`](app/src/main/java/com/ali/agent/ui/) — Jetpack Compose Cyber-Neural UI across 5 tabs (`Talk`, `Brain`, `Privacy`, `Day Hub`, `Models`).
- [`simulator/index.html`](simulator/index.html) — Instant interactive Web/Mobile Companion Preview of ALI that runs right in your browser with voice, local memory, PII sanitizer, and Wikipedia curiosity learning.

---

## How to Run & Build

### Option 1: Try the Live Interactive Mobile Simulator Right Now
Open [`simulator/index.html`](simulator/index.html) in any desktop or mobile browser (Chrome/Edge recommended for Web Speech voice input/output).

### Option 2: Build the Native Android APK in Android Studio
1. Open this folder (`ALI`) in **Android Studio Ladybug / Koala or newer** (with Android SDK 34 & NDK installed).
2. *(Optional for full C++ GGUF tensor execution)* Clone `llama.cpp` into `app/src/main/cpp/llama.cpp`:
   ```bash
   git clone --depth 1 https://github.com/ggerganov/llama.cpp app/src/main/cpp/llama.cpp
   ```
   *(Note: The project compiles and runs out-of-the-box even without cloning `llama.cpp` thanks to the hybrid CMake configuration.)*
3. Click **Run** (`Shift + F10`) to install on your Android phone or emulator, or run:
   ```bash
   ./gradlew assembleDebug
   ```

### Option 3: Zero-Setup Cloud APK Build via GitHub Actions
Push this repository to GitHub. The included workflow [`.github/workflows/android-apk.yml`](.github/workflows/android-apk.yml) automatically compiles the debug APK and uploads `ALI-Advanced-Learning-Interface-APK` under the **Actions** tab.
