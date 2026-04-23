# UXSniffer

![Build](https://github.com/NouniTheodora/ux-sniffer/workflows/Build/badge.svg)

UXSniffer is a WebStorm / IntelliJ IDEA plugin that detects UX-related code smells in Vue.js 3 (Composition API) applications. It performs static analysis directly inside the IDE, presenting findings as inspections with clear explanations to help developers reduce UX debt.

Thresholds are based on empirical research (95th percentile metrics from ReactSniffer) and are fully configurable per project through the IDE's inspection settings.

<!-- Plugin description -->
UXSniffer detects UX-related code smells in Vue.js 3 (Composition API) projects and surfaces them as IDE inspections with research-based thresholds.

**Currently detected smells:**

- **Large Vue.js file** — flags `.vue` files that exceed configurable limits on lines of code (default: 218) or number of import statements (default: 20).
- **Large Vue.js component** — flags components whose `<script setup>` block exceeds configurable limits on lines of code (default: 128) or number of functions (default: 4). Suggests extracting logic into composables.
- **Too many props** — flags components that define more than 13 props via `defineProps()`. Supports object, array, and TypeScript generic syntax.

Thresholds for all smells can be adjusted in _Settings → Editor → Inspections → Vue.js UX Smells_.
<!-- Plugin description end -->

## Detected UX Smells

| # | Smell | What it checks | Status |
|---|---|---|---|
| 1 | **Large File** | `.vue` file LOC > 218 or imports > 20 | ✅ Implemented |
| 2 | **Large Component** | Script block LOC > 128 or functions > 4 | ✅ Implemented |
| 3 | **Too Many Props** | `defineProps()` with > 13 props | ✅ Implemented |
| 4 | Direct DOM Manipulation | `document.*` calls instead of template refs | 🔲 Planned |
| 5 | Force Update | `$forceUpdate()` or `location.reload()` | 🔲 Planned |
| 6 | Props in Initial State | `ref(props.x)` instead of `computed()` | 🔲 Planned |
| 7 | Uncontrolled Component | `<input ref="x">` without `v-model` or `:value` | 🔲 Planned |
| 8 | Inheritance Instead of Composition | `extends:` in component options | 🔲 Planned |

All thresholds are configurable via **Settings → Editor → Inspections → Vue.js UX Smells**.

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "UXSniffer"</kbd> >
  <kbd>Install</kbd>

- Manually:

  Download the [latest release](https://github.com/NouniTheodora/ux-sniffer/releases/latest) and install it using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Development Requirements

- IntelliJ IDEA (Community or Ultimate)
- Java Development Kit (JDK) **21**
- IntelliJ Platform SDK (via Gradle)
- Target IDE: **WebStorm 2025.2+**

## Running Tests

### Unit tests

```bash
./gradlew test
```

Runs all JUnit tests and produces a report at `build/reports/tests/test/index.html`.

> **First run:** Gradle downloads IntelliJ IDEA 2025.2.5 as a compile dependency (~800 MB). This is cached after the first run.

If the build fails with a message about a missing `Core` plugin or an invalid IDE path, the cached download is corrupt. Fix it by deleting the bad cache entry and retrying:

```bash
rm -rf ~/.gradle/caches/9.2.1/transforms
rm -rf .gradle/configuration-cache
./gradlew test
```

### Run a single test class

```bash
./gradlew test --tests "com.github.nounitheodora.uxsniffer.inspections.LargeFileInspectionTest"
```

### Run the plugin in a sandbox IDE

```bash
./gradlew runIde
```

Launches a sandboxed IntelliJ IDEA instance with the plugin loaded. Open any `.vue` file to trigger the UX smell inspections live in the editor. Sample test files are available under `src/test/testData/vue/`.

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
