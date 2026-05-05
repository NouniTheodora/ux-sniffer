# UXSniffer — Manual Test Cases

This document describes how to manually verify each UX smell inspection inside the sandbox IDE.

## How to run the sandbox

```bash
./gradlew runIde
```

A sandboxed IntelliJ IDEA instance opens with the plugin already loaded. Create or open any project inside it, then open or paste a `.vue` file to trigger inspections.

To verify a smell is registered: **Settings → Editor → Inspections → Vue.js UX Smells**. All enabled smells appear there and can be toggled individually.

---

## Tool Window — Project-Wide Scan

**What it does:** The UXSniffer tool window provides a one-click scan of the entire project. It finds all `.vue` files and runs every enabled inspection against them, displaying the results in a sortable table.

### How to open

The tool window appears in the **bottom panel** of the IDE with the tab label **UXSniffer**. If the tab is not visible, open it via `View → Tool Windows → UXSniffer`.

### Test case — basic scan

1. Run `./gradlew runIde`.
2. In the sandbox IDE, open the `src/test/testData/vue/` folder as a project (or create a project and copy the `.vue` files into it).
3. Open the **UXSniffer** tool window from the bottom panel.
4. Expected initial state: summary label reads *"Click 'Scan Project' to analyze all .vue files."* and the table is empty.
5. Click **Scan Project**.
6. Expected: the summary label briefly shows *"Scanning..."*, then updates to show a summary like *"X smell(s) found across Y file(s). Z distinct smell type(s) detected."*
7. The table populates with rows. Each row has three columns: **Smell** (inspection name), **File** (file name), **Message** (the smell description).

### Test case — navigate to file on double-click

1. After a scan completes and the table has results, double-click any row.
2. Expected: the corresponding `.vue` file opens in the editor.

### Test case — column sorting

1. After a scan, click the **Smell** column header.
2. Expected: rows sort alphabetically by smell name. Click again to reverse.
3. Repeat for the **File** and **Message** columns.

### Test case — scan with no Vue files

1. Open or create an empty project (no `.vue` files).
2. Open the **UXSniffer** tool window and click **Scan Project**.
3. Expected: summary reads *"No UX smells found."* and the table remains empty.

### Test case — scan with clean files only

1. Create a project containing only clean test data files (e.g. `LargeFile_clean.vue`, `DirectDom_clean.vue`, etc.).
2. Click **Scan Project**.
3. Expected: summary reads *"No UX smells found."*

### Test case — re-scan after changes

1. Run a scan — note the results.
2. Modify a `.vue` file to introduce or remove a smell (e.g. add `document.getElementById` or remove it).
3. Click **Scan Project** again.
4. Expected: the table clears and repopulates with updated results reflecting the change.

### Test case — button disabled during scan

1. Click **Scan Project**.
2. Immediately try to click it again while the scan is running.
3. Expected: the button is greyed out (disabled) during the scan and re-enables after results appear.

---

## Ignore Files (`.uxsnifferignore`)

**What it does:** Allows users to exclude specific files or directories from the project-wide scan by placing a `.uxsnifferignore` file in the project root. Uses glob pattern syntax (same as `.gitignore`).

### Test case — no ignore file (default behaviour)

1. Run `./gradlew runIde`.
2. Open a project with `.vue` files that trigger smells. Ensure there is **no** `.uxsnifferignore` file in the project root.
3. Open the **UXSniffer** tool window and click **Scan Project**.
4. Expected: all `.vue` files are scanned and findings appear as normal.

### Test case — ignore a specific file by name

1. Create a `.uxsnifferignore` file in the project root with the content:
   ```
   LargeFile_tooManyLines.vue
   ```
2. Click **Scan Project**.
3. Expected: no findings from `LargeFile_tooManyLines.vue` appear in the results. Findings from other files still appear.

### Test case — ignore files by glob pattern

1. Create a `.uxsnifferignore` file with:
   ```
   src/test/**/*.vue
   ```
2. Click **Scan Project**.
3. Expected: no `.vue` files under `src/test/` (or any subdirectory) are scanned. Files outside that path are scanned normally.

### Test case — ignore with wildcard in filename

1. Create a `.uxsnifferignore` file with:
   ```
   *_clean.vue
   ```
2. Click **Scan Project**.
3. Expected: all files ending in `_clean.vue` are skipped. Files like `LargeFile_tooManyLines.vue` still appear.

### Test case — comments and blank lines

1. Create a `.uxsnifferignore` file with:
   ```
   # This is a comment
   
   LargeFile_tooManyLines.vue
   
   # Another comment
   ```
2. Click **Scan Project**.
3. Expected: only `LargeFile_tooManyLines.vue` is ignored. Comments and blank lines are not treated as patterns.

### Test case — changes take effect without restart

1. Run a scan — note the findings.
2. Create or edit `.uxsnifferignore` to add a file that produced findings.
3. Click **Scan Project** again (without restarting the IDE).
4. Expected: the newly ignored file no longer appears in the results.

### Test case — empty ignore file

1. Create an empty `.uxsnifferignore` file (0 bytes).
2. Click **Scan Project**.
3. Expected: all files are scanned normally — same as if the file did not exist.

### Test case — ignore file with only comments

1. Create a `.uxsnifferignore` file with only comments:
   ```
   # Nothing to ignore yet
   ```
2. Click **Scan Project**.
3. Expected: all files are scanned normally.

---

## Findings Detail Panel (Tabbed)

**What it shows:** When you select a finding in the Findings table, a tabbed detail panel appears below the table with two tabs: "Overview & Fix" (smell definition and refactoring) and "Cost Impact" (PAF quality costs triggered by the smell).

### Test case — detail panel appears on row selection

1. Run `./gradlew runIde`.
2. Open a project with `.vue` files that trigger smells.
3. Open the **UXSniffer** tool window and click **Scan Project**.
4. Click on any row in the findings table (single click).
5. Expected: the bottom panel shows two tabs: **Overview & Fix** and **Cost Impact (N)** where N is the number of cost mappings.

### Test case — Overview & Fix tab content

1. Select a finding for "Large Vue.js component" (S17).
2. Expected the **Overview & Fix** tab shows:
   - Header: smell name, smell ID [S17], and severity (e.g., "High")
   - **What is this smell?** — a definition explaining what the smell is
   - **Suggested refactoring** — concrete advice on how to fix the smell
   - **Detected in** — the file name and full path

### Test case — Cost Impact tab content

1. Select a finding for "Large Vue.js component" (S17).
2. Switch to the **Cost Impact** tab.
3. Expected:
   - Introductory text explaining the PAF model
   - **Direct costs** section header (red underline) with Primary cost cards
   - **Indirect costs** section header (blue underline) with Secondary cost cards
   - Each card shows: cost name, cost ID, PAF category, and causation logic

### Test case — cost card color coding

1. Select any finding with both Internal Failure and Appraisal costs.
2. Expected on the Cost Impact tab: cards have colored left borders — **red** for Internal Failure (e.g., C01 Necessary Rework), **blue** for Appraisal (e.g., C11 Design Reviews).

### Test case — deselection clears panel

1. After selecting a row, click on a different area so no row is selected (or clear the selection).
2. Expected: the detail panel returns to showing *"Select a finding above to see details, refactoring advice, and cost impact."*

### Test case — Multiple Booleans smell (S33) has costs

1. Ensure a `.vue` file triggers the "Multiple booleans for state" smell.
2. Run scan, select the finding.
3. Expected: Cost Impact tab shows 5 cost mappings (3 Primary: C01, C11, C17; 2 Secondary: C12, C16).

### Test case — switching between findings

1. Select a "Large File" finding (S25 — 6 costs).
2. Then select a "Non-Null Assertions" finding (S30 — 3 costs).
3. Expected: the detail panel updates immediately. The Cost Impact tab badge shows the correct count for the newly selected smell. No mixing of data.

---

## Statistics Tab (Tabbed Layout)

**What it shows:** After a scan, the Statistics tab displays summary cards at the top (always visible) plus three sub-tabs: Smell Distribution, Quality Costs, and Files.

### Test case — statistics appear after scan

1. Run `./gradlew runIde`.
2. Open a project with multiple `.vue` files that trigger smells.
3. Open the **UXSniffer** tool window, switch to the **Statistics** tab.
4. Expected initial state: message reads *"Run a scan to see statistics."*
5. Click **Scan Project**.
6. Expected: the Statistics tab now shows:
   - **Summary cards** at the top (always visible): Total Smells, Files Affected, Smell Types.
   - Three sub-tabs below: **Smell Distribution**, **Quality Costs**, **Files (N)**.

### Test case — Smell Distribution sub-tab

1. After a scan, switch to the **Smell Distribution** sub-tab.
2. Expected: explanatory text about what the chart shows, followed by a horizontal bar chart with one colored bar per smell type, sorted by count descending. Each bar is labelled with the smell name and count.
3. All label text should be fully visible (no ellipsis truncation).

### Test case — Quality Costs sub-tab

1. After a scan, switch to the **Quality Costs** sub-tab.
2. Expected:
   - Explanatory text about the PAF model
   - Two colored summary cards: **Internal Failure** (red accent, showing "X occurrence(s) require rework") and **Appraisal** (blue accent, showing "X occurrence(s) require reviews/testing")
   - Section header "Cost breakdown by category" with explanatory text
   - Horizontal bar chart with red bars for Internal Failure costs and blue bars for Appraisal costs
   - All cost names fully visible in the chart labels

### Test case — Files sub-tab

1. After a scan, switch to the **Files** sub-tab.
2. Expected:
   - Explanatory text about cost exposure ranking
   - A sortable table with columns: #, File, Smells, Failure Costs, Appraisal Costs, Total Costs
   - Files are ranked by smell count by default
   - Clicking any column header sorts by that column (numeric sorting for number columns)

### Test case — bar chart label visibility

1. After a scan with smells that have long names (e.g., "Cost for Performing Metrics-Based Quality Assurance").
2. Expected: the full name is visible in the chart — no "..." truncation. The chart panel is wide enough (700px preferred width, 380px label area).

### Test case — statistics reset on re-scan

1. Run a scan and verify statistics appear.
2. Click **Scan Project** again.
3. Expected: statistics briefly show *"Run a scan to see statistics."* then update with fresh results.

### Test case — empty project statistics

1. Open an empty project (no `.vue` files).
2. Click **Scan Project**.
3. Expected: Statistics tab shows *"Run a scan to see statistics."* (no charts rendered).

---

## Export Report

**What it does:** Generates a self-contained HTML report with interactive Chart.js charts, PAF cost analysis, smell definitions and refactoring suggestions, cost-per-file ranking, and full findings table. Opens in the default browser after saving.

### Test case — export button disabled before scan

1. Open the UXSniffer tool window.
2. Expected: the **Export Report** button is greyed out (disabled).
3. Run a scan that finds smells.
4. Expected: the **Export Report** button becomes enabled.

### Test case — export generates valid HTML

1. Run a scan with findings.
2. Click **Export Report**.
3. Expected: a save file dialog appears with a default filename `UXSniffer_Report.html`.
4. Choose a location and save.
5. Expected: the file is created and opens automatically in the default browser.
6. Verify the report contains:
   - Title "UXSniffer Report" with the project name and generation timestamp.
   - **Five summary cards**: Total Smells, Files Affected, Smell Types, Internal Failure Costs (red accent), Appraisal Costs (blue accent).
   - **Doughnut chart** showing smell distribution by type (hover shows tooltips, legend on the right).
   - **Horizontal bar chart** showing quality cost breakdown by category (red bars for Internal Failure, blue for Appraisal).
   - **Smell Details & Refactoring Suggestions** section with one card per smell type containing:
     - Smell name, ID badge, occurrence count badge, severity badge (High/Medium/Context-Dependent)
     - "What is this?" definition
     - "Suggested fix" in a green-tinted box
     - "Quality costs triggered" — bulleted list with color-coded cost badges (red "INTERNAL FAILURE" / blue "APPRAISAL")
   - **Files Ranked by Cost Exposure** table with columns: #, File, Smells, Failure Costs, Appraisal Costs, Total Costs.
   - **All Findings** table listing every finding with columns: #, Smell (with ID badge), File, Message.
   - Footer with "Generated by UXSniffer" attribution.

### Test case — export chart interactivity

1. Open the exported HTML report in a browser.
2. Hover over a segment of the doughnut chart.
3. Expected: a tooltip appears showing the smell name and count.
4. Hover over a bar in the cost chart.
5. Expected: a tooltip shows the cost name and number of smell occurrences triggering it.

### Test case — export smell details completeness

1. Export a report from a scan that detected multiple smell types (e.g., Large File, Large Component, Any Type).
2. Expected: the "Smell Details" section contains one card for each distinct smell type. Each card has a definition, refactoring suggestion, and cost list. No cards show "?" for the smell ID or missing definitions.

### Test case — export cost-per-file table

1. Export a report from a scan where one file has multiple smells.
2. Expected: the "Files Ranked by Cost Exposure" table shows that file with higher Total Costs than files with fewer smells. The Failure/Appraisal columns break down the costs correctly.

### Test case — export with many findings

1. Open a project with 10+ `.vue` files triggering various smells.
2. Run scan, then export.
3. Expected: the HTML report renders all findings. Charts are interactive. All tables are complete. The page is scrollable and well-styled.

### Test case — export button disabled when no findings

1. Open an empty project or one with only clean `.vue` files.
2. Run scan — expect "No UX smells found."
3. Expected: the **Export Report** button remains disabled (nothing to export).

---

## Smell 1 — Large File

**What it detects:** A `.vue` file that has too many lines of code or too many imports.

| Metric | Threshold | Condition |
|---|---|---|
| Lines of code | 218 | `LOC > 218` |
| Import statements | 20 | `imports > 20` |

### Test file: `LargeFile_clean.vue`
- **Expected result:** No warnings.
- **Why:** 20 lines, 1 import — well under both thresholds.

### Test file: `LargeFile_tooManyLines.vue`
- **Expected result:** Warning — *"Large file: X lines (threshold: 218). Consider splitting into smaller components."*
- **Why:** File exceeds 218 lines.

### Test file: `LargeFile_tooManyImports.vue`
- **Expected result:** Warning — *"Too many imports: 21 (threshold: 20). This file may be taking on too many responsibilities."*
- **Why:** 21 import statements in the `<script setup>` block.

### Steps
1. Run `./gradlew runIde`.
2. Inside the sandbox, open the folder `src/test/testData/vue/`.
3. Open each file above and confirm the expected result.
4. The warning appears as a yellow underline at the top of the file. Hover to read the message.

### Configuring thresholds

Both thresholds are configurable per IDE profile through the IntelliJ settings UI. No code changes or config files needed.

**How to open the settings panel:**

`Settings → Editor → Inspections → Vue.js UX Smells → Large Vue.js file`

The right-hand panel shows two spinners:

```
Max lines of code:       [ 218 ]
Max import statements:   [  20 ]
```

Change either value and click **OK** or **Apply**. IntelliJ saves the values to the active inspection profile (`.idea/inspectionProfiles/Project_Default.xml`). Committing that file shares the thresholds with the whole team.

**Test case — lowering the LOC threshold:**

1. Open settings and change **Max lines of code** to `10`.
2. Click **Apply**.
3. Open `LargeFile_clean.vue` (20 lines).
4. Expected: warning now appears — *"Large file: 20 lines (threshold: 10)…"*
5. Restore the threshold to `218` and confirm the warning disappears.

**Test case — lowering the imports threshold:**

1. Open settings and change **Max import statements** to `1`.
2. Click **Apply**.
3. Open `LargeFile_clean.vue` (1 import).
4. Expected: no warning — file has exactly 1 import, threshold is `> 1`.
5. Change threshold to `0` — warning should appear.
6. Restore to `20`.

**Test case — multiple violations at once:**

1. Open `LargeFile_bothViolations.vue`.
2. Expected: **two separate warnings** appear, one for each violated threshold:
   - *"Large file: X lines (threshold: 218)…"*
   - *"Too many imports: 21 (threshold: 20)…"*
3. Both appear in the **Problems** panel at the bottom (`View → Tool Windows → Problems`) as two distinct entries for the same file.
4. Hover over the file in the editor — both messages are listed in the tooltip one after the other.

The two checks are independent (`if` / `if`, not `if` / `else if`), so all violated thresholds are always reported simultaneously.

**Test case — disabling the inspection entirely:**

1. In the same settings panel, uncheck the **Large Vue.js file** checkbox.
2. Open `LargeFile_tooManyLines.vue`.
3. Expected: no warning at all, regardless of file size.
4. Re-enable the inspection to restore default behaviour.

---

## Smell 2 — Large Component

**What it detects:** A `.vue` component whose `<script setup>` block is too long or defines too many functions.

| Metric | Threshold | Condition |
|---|---|---|
| Script block LOC | 128 | `scriptLOC > 128` |
| Number of functions | 4 | `functions > 4` |

Counted as functions: `function f()`, `async function f()`, `const f = () => {`, `const f = async () => {`. Vue reactivity primitives (`computed`, `watch`, `watchEffect`) are excluded even if they contain arrow callbacks.

### Test file: `LargeComponent_clean.vue`
- **Expected result:** No warnings.
- **Why:** 2 functions, short script block — well under both thresholds.

### Test file: `LargeComponent_tooManyFunctions.vue`
- **Expected result:** Warning — *"Large component: 5 functions defined (threshold: 4). Consider extracting logic into composables."*
- **Why:** Defines 5 functions (`handleClick`, `handleHover`, `handleFocus`, `handleBlur`, `handleSubmit`).

### Test file: `LargeComponent_tooManyScriptLines.vue`
- **Expected result:** Warning — *"Large component: script block has X lines (threshold: 128). Consider splitting logic into composables."*
- **Why:** Script block contains 130+ `ref()` declarations, exceeding 128 lines.

### Steps
1. Run `./gradlew runIde`.
2. Open the folder `src/test/testData/vue/`.
3. Open each file above and confirm the expected result.
4. The warning appears as a box at the top of the file.

### Configuring thresholds

`Settings → Editor → Inspections → Vue.js UX Smells → Large Vue.js component`

```
Max script block lines:  [ 128 ]
Max functions:           [   4 ]
```

**Test case — lower function threshold to 1:**
1. Set **Max functions** to `1`.
2. Open `LargeComponent_clean.vue` (2 functions).
3. Expected: warning appears — *"Large component: 2 functions defined (threshold: 1)…"*
4. Restore to `4`.

---

## Smell 3 — Too Many Props

**What it detects:** A component that defines more than 13 props via `defineProps()`.

| Metric | Threshold | Condition |
|---|---|---|
| Number of props | 13 | `props > 13` |

Supports all three Vue prop definition styles:
- Object syntax: `defineProps({ propA: String, ... })`
- Array syntax: `defineProps(['propA', ...])`
- TypeScript generic: `defineProps<{ propA: string }>()`

Nested type definitions inside a prop (e.g. `config: { type: Object, required: true }`) are correctly not counted as additional props.

### Test file: `TooManyProps_clean.vue`
- **Expected result:** No warnings.
- **Why:** 5 props — below the threshold of 13.

### Test file: `TooManyProps_trigger.vue`
- **Expected result:** Warning — *"Too many props: 14 defined (threshold: 13). Consider grouping related props into objects or splitting the component."*
- **Why:** 14 props defined in the object syntax `defineProps({...})`.

### Steps
1. Run `./gradlew runIde`.
2. Open `src/test/testData/vue/`.
3. Open each file and confirm the expected result.

### Configuring thresholds

`Settings → Editor → Inspections → Vue.js UX Smells → Too many props`

```
Max props:  [ 13 ]
```

---

## Smell 4 — Direct DOM Manipulation

**What it detects:** Calls to DOM manipulation APIs inside a Vue component's `<script setup>` block. Vue's reactivity system and template refs should be used instead.

Detected APIs:

| Category | APIs |
|---|---|
| Document methods | `getElementById`, `getElementsByTagName`, `getElementsByClassName`, `querySelector`, `querySelectorAll`, `createElement` |
| DOM mutation methods | `appendChild`, `removeChild`, `replaceChild`, `setAttribute` |
| DOM properties | `innerHTML`, `innerText`, `textContent` |

When multiple APIs are found in the same file, they are reported in a single combined warning message. Comments (`//`, `/*`, `*`) are skipped during detection.

### Test file: `DirectDom_clean.vue`
- **Expected result:** No warnings.
- **Why:** Uses Vue `ref()` to access DOM elements — no direct DOM calls.

### Test file: `DirectDom_trigger.vue`
- **Expected result:** Warning — *"Direct DOM manipulation via 'getElementById'. Use Vue template refs instead."*
- **Why:** Calls `document.getElementById('title')` inside a function.

### Test file: `DirectDom_multiple.vue`
- **Expected result:** Warning listing multiple APIs — *"Direct DOM manipulation via 'getElementById', 'createElement', 'textContent' and 'appendChild'. Use Vue template refs instead."*
- **Why:** Uses `document.getElementById`, `document.createElement`, `.textContent` assignment, and `.appendChild` — all in one component.

### Steps
1. Run `./gradlew runIde`.
2. Inside the sandbox, open the folder `src/test/testData/vue/`.
3. Open each file above and confirm the expected result.
4. The warning appears as a box at the top of the file.

### Test case — disabling the inspection:

1. Open `Settings → Editor → Inspections → Vue.js UX Smells → Direct DOM manipulation`.
2. Uncheck the checkbox to disable.
3. Open `DirectDom_trigger.vue`.
4. Expected: no warning.
5. Re-enable to restore default behaviour.

---

## Smell 5 — Force Update

**What it detects:** Calls to `$forceUpdate()` or `location.reload()` inside a Vue component's `<script setup>` block, which bypass Vue's reactivity system.

| Pattern | What it catches |
|---|---|
| `$forceUpdate()` | Forcing a re-render instead of relying on reactive state |
| `location.reload()` | Full page reload instead of reactive updates or router navigation |

Both `location.reload()` and `window.location.reload()` are detected. Comments are skipped. When both patterns appear in the same file, they are reported in a single combined message.

### Test file: `ForceUpdate_clean.vue`
- **Expected result:** No warnings.
- **Why:** Uses `ref()` and `computed()` — proper Vue reactivity.

### Test file: `ForceUpdate_forceUpdate.vue`
- **Expected result:** Warning — *"Avoid '$forceUpdate()'. Redesign state so Vue's reactivity handles updates automatically."*
- **Why:** Calls `instance.proxy.$forceUpdate()`.

### Test file: `ForceUpdate_reload.vue`
- **Expected result:** Warning — *"Avoid 'location.reload()'. Use Vue's reactivity or router navigation instead of full page reloads."*
- **Why:** Calls `location.reload()`.

### Test file: `ForceUpdate_both.vue`
- **Expected result:** Warning — *"Avoid '$forceUpdate()' and 'location.reload()'. Redesign state so Vue's reactivity handles updates automatically."*
- **Why:** Uses both `$forceUpdate()` and `location.reload()` in the same component.

### Steps
1. Run `./gradlew runIde`.
2. Inside the sandbox, open the folder `src/test/testData/vue/`.
3. Open each file above and confirm the expected result.

### Test case — disabling the inspection:

1. Open `Settings → Editor → Inspections → Vue.js UX Smells → Force update / page reload`.
2. Uncheck the checkbox to disable.
3. Open `ForceUpdate_forceUpdate.vue`.
4. Expected: no warning.
5. Re-enable to restore default behaviour.

---

## Smell 6 — Props in Initial State

**What it detects:** Reactive state initialised directly from a prop value, which breaks the one-way data flow and causes the state to go out of sync when the prop changes.

| Pattern | Example |
|---|---|
| `ref(props.x)` | `const name = ref(props.initialName)` |
| `reactive({ ... props.x })` | `const state = reactive({ name: props.userName })` |

Both patterns are detected. Using `computed(() => props.x)` is the correct alternative — it stays in sync when the prop changes. Comments are skipped. When multiple props are copied into state, all are reported in a single combined message.

### Test file: `PropsInInitialState_clean.vue`
- **Expected result:** No warnings.
- **Why:** Uses `computed()` to derive values from props — stays in sync.

### Test file: `PropsInInitialState_trigger.vue`
- **Expected result:** Warning — *"Prop 'initialName' used to initialise reactive state. Use computed(() => props.initialName) to stay in sync."*
- **Why:** Copies `props.initialName` into a `ref()`.

### Test file: `PropsInInitialState_multiple.vue`
- **Expected result:** Warning — *"Props 'initialName' and 'initialCount' used to initialise reactive state. Use computed() to stay in sync with prop changes."*
- **Why:** Two props are copied into separate `ref()` calls.

### Steps
1. Run `./gradlew runIde`.
2. Inside the sandbox, open the folder `src/test/testData/vue/`.
3. Open each file above and confirm the expected result.

### Test case — disabling the inspection:

1. Open `Settings → Editor → Inspections → Vue.js UX Smells → Props used in initial state`.
2. Uncheck the checkbox to disable.
3. Open `PropsInInitialState_trigger.vue`.
4. Expected: no warning.
5. Re-enable to restore default behaviour.

---

## Smell 7 — Uncontrolled Component

**What it detects:** Form elements (`<input>`, `<textarea>`, `<select>`) that use a `ref` attribute but have no `v-model` or `:value` binding, meaning the value is only accessible imperatively.

| Detected elements | Recognised bindings |
|---|---|
| `<input>`, `<textarea>`, `<select>` | `v-model`, `:value`, `v-bind:value` |

An element is only flagged if it has a `ref` **and** lacks any reactive value binding. Elements without a `ref` are not flagged (they may be plain HTML with no JS interaction). When multiple uncontrolled elements are found, they are reported in a single combined message.

### Test file: `UncontrolledComponent_clean.vue`
- **Expected result:** No warnings.
- **Why:** All inputs use `v-model` or `:value`.

### Test file: `UncontrolledComponent_trigger.vue`
- **Expected result:** Warning — *"Uncontrolled &lt;input&gt;: uses a ref but has no v-model or :value binding. Bind the value reactively instead."*
- **Why:** `<input ref="nameInput">` has no value binding.

### Test file: `UncontrolledComponent_multiple.vue`
- **Expected result:** Warning — *"2 uncontrolled form elements use a ref but have no v-model or :value binding. Bind values reactively instead."*
- **Why:** Both `<input>` and `<textarea>` use refs without value bindings.

### Steps
1. Run `./gradlew runIde`.
2. Inside the sandbox, open the folder `src/test/testData/vue/`.
3. Open each file above and confirm the expected result.

### Test case — disabling the inspection:

1. Open `Settings → Editor → Inspections → Vue.js UX Smells → Uncontrolled form component`.
2. Uncheck the checkbox to disable.
3. Open `UncontrolledComponent_trigger.vue`.
4. Expected: no warning.
5. Re-enable to restore default behaviour.

---

## Smell 8 — Inheritance Instead of Composition

**What it detects:** A component that uses `extends: SomeComponent` in its options object, preferring inheritance over Vue's composition model.

The warning message includes the name of the extended component, extracted dynamically from the code.

### Test file: `Inheritance_clean.vue`
- **Expected result:** No warnings.
- **Why:** Uses a composable (`useFormatter`) instead of inheritance.

### Test file: `Inheritance_trigger.vue`
- **Expected result:** Warning — *"Inheritance via 'extends: BaseComponent' detected. Prefer composables or component composition over class-style inheritance."*
- **Why:** Uses `extends: BaseComponent` in the options object.

### Steps
1. Run `./gradlew runIde`.
2. Inside the sandbox, open the folder `src/test/testData/vue/`.
3. Open each file above and confirm the expected result.

### Test case — disabling the inspection:

1. Open `Settings → Editor → Inspections → Vue.js UX Smells → Inheritance instead of composition`.
2. Uncheck the checkbox to disable.
3. Open `Inheritance_trigger.vue`.
4. Expected: no warning.
5. Re-enable to restore default behaviour.

---

## Smell 9 — Any Type

**What it detects:** Usage of the `any` type in TypeScript Vue components (`<script setup lang="ts">`). Applies to type annotations (`: any`), generic parameters (`<any>`), and composite types (`, any`).

### Test file: `AnyType_clean.vue`
- **Expected result:** No warnings.
- **Why:** All types are explicitly defined (`string`, `ref<string>`).

### Test file: `AnyType_trigger.vue`
- **Expected result:** Warning — *"TypeScript 'any' type used 3 times. This disables type checking and may hide errors. Define explicit types instead."*
- **Why:** Uses `ref<any>(null)` and `function process(input: any): any`.

### Steps
1. Run `./gradlew runIde`.
2. Open a `.vue` file with `<script setup lang="ts">` containing `: any`.
3. Confirm warning appears. Files without `lang="ts"` should not trigger.

---

## Smell 10 — Non-Null Assertions

**What it detects:** Usage of the non-null assertion operator (`!.`) in TypeScript Vue components (`<script setup lang="ts">`). This operator tells TypeScript a value is not null/undefined without performing a runtime check.

### Test file: `NonNullAssertion_clean.vue`
- **Expected result:** No warnings.
- **Why:** Uses optional chaining (`?.`) and nullish coalescing (`??`) instead.

### Test file: `NonNullAssertion_trigger.vue`
- **Expected result:** Warning — *"Non-null assertion operator ('!') used. This may hide null/undefined errors at runtime. Use proper null checks instead."*
- **Why:** Uses `found!.value` to access a potentially undefined result.

### Steps
1. Run `./gradlew runIde`.
2. Open a `.vue` file with `<script setup lang="ts">` containing `!.` patterns.
3. Confirm warning appears. Logical NOT (`!isHidden`) and not-equal (`!==`) are not flagged.

---

## Smell 11 — Multiple Booleans for State

**What it detects:** Components that define too many boolean `ref()` values to manage state, making it easy to set contradictory combinations.

| Metric | Threshold | Condition |
|---|---|---|
| Boolean refs | 4 | `booleanRefs > 4` |

### Test file: `MultipleBooleans_clean.vue`
- **Expected result:** No warnings.
- **Why:** Uses a union type (`CountdownState`) instead of multiple booleans.

### Test file: `MultipleBooleans_trigger.vue`
- **Expected result:** Warning — *"Multiple booleans for state: 5 boolean refs defined (threshold: 4). Consider using an enum or union type to manage state transitions instead."*
- **Why:** Defines 5 boolean refs (`isLoading`, `isRunning`, `isPaused`, `isFinished`, `hasError`).

### Configuring thresholds

`Settings → Editor → Inspections → Vue.js UX Smells → Multiple booleans for state`

```
Max boolean refs:  [ 4 ]
```

---

## Smell 12 — Enum Implicit Values

**What it detects:** TypeScript enums whose members do not have explicit values assigned. When members are reordered, all auto-generated values shift, causing silent runtime inconsistencies.

### Test file: `EnumImplicit_clean.vue`
- **Expected result:** No warnings.
- **Why:** Enum `Color` has explicit string values (`Red = 'Red'`, etc.).

### Test file: `EnumImplicit_trigger.vue`
- **Expected result:** Warning — *"Enum 'Color' has implicit values. Assign explicit values to prevent reordering issues."*
- **Why:** Enum `Color` members have no `=` assignments.

### Steps
1. Run `./gradlew runIde`.
2. Open a `.vue` file with `<script setup lang="ts">` containing an enum without explicit values.
3. Confirm warning appears. Enums with all explicit values should not trigger.

---

## Thresholds reference

| Smell | Metric | Threshold | Source |
|---|---|---|---|
| Large File | Lines of code | 218 | Ferreira & Valente, 2022 (95th percentile) |
| Large File | Import count | 20 | Ferreira & Valente, 2022 (95th percentile) |
| Large Component | Script LOC | 128 | Ferreira & Valente, 2022 (95th percentile) |
| Large Component | Function count | 4 | Ferreira & Valente, 2022 (95th percentile) |
| Too Many Props | Prop count | 13 | Ferreira & Valente, 2022 (95th percentile) |
| Direct DOM Manipulation | Any DOM API call | 1 | Any occurrence |
| Force Update | Any forceUpdate/reload call | 1 | Any occurrence |
| Props in Initial State | ref(props.x) pattern | 1 | Any occurrence |
| Uncontrolled Component | input with ref, no v-model | 1 | Any occurrence |
| Inheritance | extends: in options | 1 | Any occurrence |
| Any Type | `: any` annotation | 1 | Nunes et al., 2025 (any occurrence) |
| Non-Null Assertions | `!.` operator | 1 | Nunes et al., 2025 (any occurrence) |
| Multiple Booleans for State | Boolean ref count | 4 | Nunes et al., 2025 |
| Enum Implicit Values | Enum without explicit values | 1 | Nunes et al., 2025 (any occurrence) |
