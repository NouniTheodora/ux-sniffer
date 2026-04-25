# UXSniffer — Manual Test Cases

This document describes how to manually verify each UX smell inspection inside the sandbox IDE.

## How to run the sandbox

```bash
./gradlew runIde
```

A sandboxed IntelliJ IDEA instance opens with the plugin already loaded. Create or open any project inside it, then open or paste a `.vue` file to trigger inspections.

To verify a smell is registered: **Settings → Editor → Inspections → Vue.js UX Smells**. All enabled smells appear there and can be toggled individually.

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
