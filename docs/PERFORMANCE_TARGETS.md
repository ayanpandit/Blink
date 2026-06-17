# Blink Performance Targets

To maintain our product philosophy of **"Tap → Open → Read"**, Blink enforces strict performance thresholds. These metrics must be verified on mid-range test devices (e.g., octa-core chipset, 4GB RAM, Android 11+) and flagship devices in release builds (R8 enabled, compilation profile fully compiled).

---

## 1. Startup Performance

### Cold Start (Time to Interactive)
* **Target**: 
  * Mid-range devices: **< 500ms**
  * Flagship devices: **< 250ms**
* **Definition**: The time elapsed from the user tapping the application icon or opening a document via an external intent, to the first interactive frame drawn on screen.
* **Measurement**: Verified using Android Vitals metrics via `logcat` parsing for `ActivityTaskManager: Displayed` time, or Android Macrobenchmark tests.
* **Key Constraints**:
  * No splash screen delay.
  * Manual DI container initialization must take `< 5ms`.
  * Application class initialization must not perform any disk IO or thread blocking operations.

---

## 2. Document Opening Performance

### Time to First Page Render
* **Target**:
  * Common files (< 10MB PDF, < 5MB DOCX/XLSX/TXT): **< 300ms**
  * Large files (10MB - 50MB PDF, 5MB - 20MB Office files): **< 800ms**
* **Definition**: Time from the `Activity` receiving the file URI inside the intent, to the first page (or first visible viewport area) being fully drawn.
* **Measurement**: Inside the app using `System.nanoTime()` measured from `onCreate()` to the first Compose layout pass where the document is visible.
* **Key Constraints**:
  * Parsers must load files lazily. They must parse the file structure headers, retrieve page dimensions, and only parse/render page `0` initially.
  * Complete file scanning is prohibited on startup.

---

## 3. Rendering and Scrolling Performance

### Frame Rate Consistency
* **Target**: 
  * Standard screens: **60 FPS** (frame rendering budget **< 16ms**)
  * High-refresh displays: **90 / 120 FPS** (budget **< 11.1ms** / **< 8.3ms**)
  * Frame Drop Rate: **< 1%** (Janky frames) during continuous scrolling.
* **Measurement**: Measured using Android GPU Rendering Profiling (`adb shell dumpsys gfxinfo`) and Android Profile installer benchmark tools.
* **Key Constraints**:
  * Rendering tasks must not touch the main thread.
  * Compose recompositions must be optimized. Compose layouts must skip recomposition for static parameters (e.g., using `@Stable` wrappers for pages).

---

## 4. Memory Allocations

### Heap Footprint limits
* **Target**:
  * Idle state (no document loaded): **< 25MB**
  * Active viewing (rendering common files): **< 80MB** peak heap usage.
  * Large document scrolling: **< 150MB** absolute peak heap usage.
* **Definition**: Memory allocated within the application's JVM/Native heap, measured via Android Profiler.
* **Key Constraints**:
  * Bitmaps must be recycled. Once a rendered page scrolls out of the cache boundary, its backing `Bitmap` must be freed or put back into a `BitmapPool` to prevent memory fragmentation.
  * Memory Cache limits: The document `LruCache` must be set dynamically based on system memory:
    $$\text{Cache Size} = \min(32\text{MB}, \text{Available Heap} \times 0.15)$$
  * GC Activity: Scrolling must trigger zero garbage collection stutters.

---

## 5. Summary Table of Performance Gates

| Metric | Target (Mid-Range) | Target (Flagship) | Measurement Mechanism |
|---|---|---|---|
| **Cold Start** | < 500ms | < 250ms | `ActivityTaskManager: Displayed` log |
| **First Page Render (Standard)** | < 300ms | < 150ms | Internal tracing (`onCreate` -> `fullyDrawn`) |
| **First Page Render (Large)** | < 800ms | < 400ms | Internal tracing (`onCreate` -> `fullyDrawn`) |
| **Frame Render Time** | < 16ms (60 FPS) | < 8.3ms (120 FPS) | `adb shell dumpsys gfxinfo` |
| **Idle Heap Usage** | < 25MB | < 25MB | Android Profiler Memory Heap |
| **Active Heap Usage** | < 80MB | < 80MB | Android Profiler Memory Heap |
