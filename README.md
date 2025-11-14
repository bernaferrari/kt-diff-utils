# KT Diff Utils

Lightweight Kotlin diff utilities optimized for Android and Kotlin Multiplatform projects. KT Diff Utils started as the change engine inside [ChangeDetection](https://github.com/bernaferrari/ChangeDetection), where a Java port of java-diff-utils kept app size down. This codebase grew from that foundation into a full Kotlin-first successor to java-diff-utils: same API surface, but backed by profiling-driven Myers tweaks and modern build tooling.

## Highlights

- **Kotlin-first**: idiomatic APIs, null-safety, and multiplatform-friendly collections. All blocking `java.io` usage is isolated to optional modules, so the core works directly in Android apps and Kotlin Multiplatform targets.
- **Performance obsessed**: profiled and benchmarked; shared-prefix trimming, patience-style anchors, and cached delta ordering keep costs proportional to real edits.
- **Modular**: pick the pieces you need.
  - `kt-diff-core`: Myers diff, patch apply/restore, diff-row generation – 100% Kotlin.
  - `kt-diff-jvm-io`: optional unified diff parser/writer plus legacy Java serialization helpers.
  - `kt-diff-jgit`: histogram diff adapter backed by Eclipse JGit.
- **Benchmarked**: run `./gradlew :kt-diff-core:jmh` to reproduce the numbers below (see `kt-diff-core/src/jmh/...` for workloads).

| Scenario (JDK 22) | java-diff-utils | KT Diff Utils | Notes |
| --- | --- | --- | --- |
| `DiffUtils.diff` – 2 000 lines, 10 % edits | 2.73 ms/op | **0.24 ms/op** | Random-access normalization + anchors (~11× faster) |
| `diffWithEquals` – same workload | 2.83 ms/op | **0.32 ms/op** | Inline diff remains ~8.8× faster |
| `DiffUtils.diff` – 45 % unchanged edges | 2.73 ms/op | **0.59 ms/op** | Prefix/suffix trimming keeps work bounded |
| Applying prepared patch | 0.05 ms/op | **0.028 ms/op** | Smarter patch data structures |
| Repeated `patch.deltas` access (500×) | 0.67 ms/op | **~0.001 ms/op** | Cached ordering eliminates re-sorts |

### Head-to-head vs kotlin-multiplatform-diff

We cloned [petertrr/kotlin-multiplatform-diff](https://github.com/petertrr/kotlin-multiplatform-diff) at `79b80e1` and reused our `DiffBenchmark` dataset to compare JVM performance. Both libraries were run on JDK 22, identical inputs.

| Workload | KT Diff Utils | kotlin-multiplatform-diff | Notes |
| --- | --- | --- | --- |
| `diffDefault`, 2 000 lines, 10 % edits | **0.306 ms/op** | 3.044 ms/op | ~10× faster on noisy edits |
| `diffDefault`, 2 000 lines, 10 % edits, 45 % stable edges | **0.625 ms/op** | 0.701 ms/op | Comparable when edits cluster |
| `diffWithEquals`, 2 000 lines, 10 % edits | **0.339 ms/op** | 3.737 ms/op | Inline mode stays 11× ahead |
| `applyPreparedPatch`, 2 000 lines, 10 % edits | **0.046 ms/op** | 0.056 ms/op | Similar patch-apply performance |
| `repeatedDeltaAccess`, 2 000 lines, 10 % edits | **≈0.001 ms/op** | 1.209 ms/op | Cached ordering vs repeated sorts |

Smaller workloads (500 lines / 3 % edits) show similar results within noise; KT Diff Utils pulls ahead as inputs get larger or more chaotic. To reproduce, temporarily add the competitor’s `src/commonMain/kotlin` directory to our JMH source set and run `./gradlew :kt-diff-core:jmh`.

Benchmark reports land in `kt-diff-core/build/results/jmh/results.txt`. Adjust workloads via `kt-diff-core/src/jmh/kotlin/com/bernaferrari/difflib/benchmark/DiffBenchmark.kt`.

## Related Kotlin ports

- [petertrr/kotlin-multiplatform-diff](https://github.com/petertrr/kotlin-multiplatform-diff) focuses on sharing a java-diff-utils port across JVM/JS/Native. KT Diff Utils narrows in on JVM/Android today, trading breadth for optimized Myers and patience-style heuristics.
- [GitLiveApp/kotlin-diff-utils](https://github.com/GitLiveApp/kotlin-diff-utils) offers another JVM-first port. KT Diff Utils targets the same API compatibility but adds benchmark-driven optimizations and optional modules for unified diff IO or JGit histogram support.

If you already depend on one of those ports, swapping to KT Diff Utils should only require updating package names to `com.bernaferrari.difflib.*`.

## Installation

There’s no published artifact yet—grab the source from GitHub (fork or clone) and include it directly:

1. Keep this repo next to your app/library and wire it via a composite build:

   ```kotlin
   // settings.gradle.kts of your consumer project
   includeBuild("../kt-diff-utils")
   ```

2. Declare the module dependency:

   ```kotlin
   dependencies {
       implementation(project(":kt-diff-core"))
       // Optional JVM helpers:
       // implementation(project(":kt-diff-jvm-io"))
       // Histogram diff support:
       // implementation(project(":kt-diff-jgit"))
   }
   ```

If you need a Maven artifact internally, run `./gradlew publishToMavenLocal` and reference `com.bernaferrari.difflib:kt-diff-*`. Public artifacts are not published yet.

## Usage

KT Diff Utils mirrors the java-diff-utils API. Existing examples on the [original wiki](https://github.com/java-diff-utils/java-diff-utils/wiki) compile unchanged; simply update imports to `com.bernaferrari.difflib.*` and, if needed, swap coordinates to the modules above.

Key entry points:

```kotlin
val patch = DiffUtils.diff(originalList, revisedList)
val result = patch.applyTo(originalList)
val diffRows = DiffRowGenerator.create()
    .ignoreWhiteSpaces(true)
    .showInlineDiffs(true)
    .build()
    .generateDiffRows(originalLines, revisedLines)
```

Unified diff parsing/writing lives in `kt-diff-jvm-io` (`UnifiedDiffReader`, `UnifiedDiffWriter`). Histogram diff support is exposed via `kt-diff-jgit`.

## Multiplatform status

- `kt-diff-core` is free of `java.io` and ready for Android / Kotlin Multiplatform (JVM target today).
- `kt-diff-jvm-io` intentionally uses `BufferedReader`, `InputStream`, and `Writer` for GNU diff compatibility.

## Contributing

- Build & test: `./gradlew test`
- Benchmarks: `./gradlew :kt-diff-core:jmh`
- Formatting: `./gradlew spotlessCheck`

Issues and pull requests are welcome.

## License

MIT License – see [LICENSE](LICENSE).
