# Redstone Essentials — Ideas

## Mod Philosophy

**Redstone Essentials** is a vanilla-style / vanilla-friendly redstone mod.

- **Goal:** Make redstone more convenient, appealing, and user-friendly without removing the problem-solving and circuit-building core of vanilla redstone.
- **What fits:** Replacing tedious workarounds (such as filling hoppers, chests, or composters just to obtain a specific comparator signal strength) with clean, purpose-built components that still behave like natural vanilla redstone parts.
- **Explicit non-goals:** All-in-one "magic box" automation blocks (e.g., a complete timer/clock circuit condensed into a single block) that eliminate the need to wire up vanilla redstone logic.

### Shared Conventions

- **Namespace & Registry IDs:**
  - `redstone_essentials:tuned_redstone_crystal`
  - `redstone_essentials:redstone_variode`
  - `redstone_essentials:redstone_capacitor`
- **Creative Inventory:** All mod blocks appear in **both** the vanilla **Redstone Blocks** creative tab and a dedicated **Redstone Essentials** creative tab.
- **Vanilla `26.3` Model Wiring Pattern** *(verified against `repeater`, `comparator`, and `observer` in Minecraft `26.3`)*:
  - Java block classes only declare `BlockState` properties (never hardcode model paths); `assets/redstone_essentials/blockstates/<id>.json` maps property combinations to block models plus `x`/`y` rotations, so facing directions never require extra model files.
  - Vanilla `repeater` uses **16** block models (`4 delays × 2 powered × 2 locked`), `1` blockstate, `1` 2D item model (`models/item/repeater.json`), and `1` MC `26.3` item definition (`items/repeater.json`).
  - Vanilla `observer` uses **2** block models, `1` blockstate (`x`/`y` rotations for all 6 facings), `0` `models/item/` files, and `1` item definition (`items/observer.json`) pointing directly to `block/observer`.
  - **Parent-model tip:** When multiple state models share identical 3D geometry and only swap textures (e.g., `tuned_redstone_crystal` or `redstone_capacitor`), each state model can inherit a single shared `*_base.json` parent model so geometry is never duplicated.

---

## 1. Tuned Redstone Crystal (`tuned_redstone_crystal`)

A purely in-world **analog T-flip-flop / signal-strength latch** driven entirely by redstone or Amethyst vibration resonance—no GUI and no manual right-click tuning or toggling.

### Form Factor & Orientation

- **Shape:** Full solid block, placeable in **all 6 directions** (Observer-style).
- **Faces:**
  - **Front face (Output):** Emits power only out of this face so neighboring redstone lines are not accidentally powered.
  - **Back face (Input):** Sole input face for both direct redstone signals and Amethyst vibration resonance.

### Input & Latching Behavior

- **Purely Redstone / Amethyst Driven:** Right-clicking by hand does not toggle or tune the block.
- **Standard Mode (Non-I/O Sides Unpowered — Edge-Triggered Toggle, Copper Bulb-style):**
  - When **OFF**, a rising edge (`0` → `1–15`) on the back input face toggles the block **ON** and **latches** the incoming signal strength (`1–15`).
  - When **ON**, the next rising edge on the back input face toggles the block **OFF** (emitting `0`).
- **Side-Powered Retune Mode (Any Non-I/O Side Powered — Live Analog Latch):**
  - The 4 non-I/O side faces accept any redstone signal (lever, torch, repeater, comparator, strongly powered block, etc.), while redstone dust does not auto-bend into the side faces.
  - While any non-I/O side is powered, the block stays **ON** (never toggled OFF by back inputs or Amethyst resonance): any back signal `> 0` or Amethyst resonance live-updates the stored strength (`1–15`), and dropping the back input to `0` holds the last latched strength.
- **Two In-World Tuning Inputs (Back Face Only):**
  1. **Direct Redstone Signal:** Incoming redstone strength (`1–15`) at the back face.
  2. **Amethyst Vibration Resonance:** A Block of Amethyst placed directly against the back input face relaying a Sculk Sensor vibration (`1–15` by sound event frequency) triggers/tunes the latched strength to that vibration frequency.

### Output Behavior

- **Strong Power (Front Face):** Emits strong redstone power at the latched strength (`1–15`) when **ON**, or `0` when **OFF** (powers adjacent dust directly and can power through a solid block).
- **Comparator-Readable:** Supports Comparator output matching its current emitted strength.

### Visual Design

- **4-Bit Binary Crystal Pips:** Four glowing amethyst/redstone nodes representing `1 / 2 / 4 / 8` on the block faces for a compact, vanilla-friendly exact readout, paired with distinct Observer-like front output and back input faces.

### Technical Approach & Piston Behavior

- **BlockState-Only + Vibration Mixin:** Stores state directly in `BlockState` (`FACING`, `POWERED`, `POWER` `0–15`) with a lightweight Mixin hooking Amethyst/Sculk vibration resonance.
- **Piston-Pushable:** Natively pushable by pistons (retaining its latched strength when moved, like an Observer or Copper Bulb) with zero `BlockEntity` overhead.

### Crafting Recipe (Mid-Early Tier, Yields 1)

| | | |
|---|---|---|
| Copper Ingot | Redstone Dust | Copper Ingot |
| Redstone Dust | Amethyst Shard | Redstone Dust |
| Stone | Nether Quartz | Stone |

### Required JSON Models & Suggested File Names

- **BlockState properties:** `facing` (6 directions via `x`/`y` rotation), `power` (`0–15`), `powered` (`true`/`false`, internal edge-detection state; no separate visual unless a back-face indicator is later added).
- **Block models (`assets/redstone_essentials/models/block/`) — 16 files** *(+ 1 optional shared geometry parent `tuned_redstone_crystal_base.json`)*:
  - `tuned_redstone_crystal_0.json` *(OFF, all 4 binary pips dark; also used for inventory item)*
  - `tuned_redstone_crystal_1.json` through `tuned_redstone_crystal_15.json` *(4-bit binary pip combinations `0001`–`1111`)*
- **Blockstate (`assets/redstone_essentials/blockstates/`) — 1 file:**
  - `tuned_redstone_crystal.json` *(96 variant entries = `6 facings × 16 power`, mapping both `powered` values to the same model)*
- **Item definition (`assets/redstone_essentials/items/`) — 1 file:**
  - `tuned_redstone_crystal.json` *(points directly to `redstone_essentials:block/tuned_redstone_crystal_0`, matching vanilla `observer`; **0** `models/item/` files required)*
- **Total:** `16` block models + `1` blockstate + `1` item definition (**18 JSON files**, or 19 with shared base parent).
  - *Note:* If a back-face `powered` indicator is ever added later, switch `blockstates/tuned_redstone_crystal.json` to `multipart` using ~6–7 modular part models instead of 32 variant models.

---

## 2. Redstone Variode (`redstone_variode`)

A compact, **0-tick, one-way diode and 4-notch binary attenuator** (`out = max(0, in - R)`).

### Form Factor & Orientation

- **Shape:** Flat Repeater/Comparator-style component, placeable in **4 horizontal directions**.
- **Faces:** Back input face and front output face; **no side inputs**.
- **Output:** Emits **strong power** forward.

### Mechanics & Attenuation

- **0-Tick One-Way Propagation:** Passes signals from back to front with **0 redstone ticks of delay** while isolating the input from back-feeding.
- **Loop-Safe by Design:** Because every pass subtracts at least `1` signal strength, 0-tick feedback loops naturally terminate in at most 15 steps—preserving a clear role for Repeaters (boosting back to `15`) and Comparators (lossless comparison).
- **4 Right-Click Binary Notches:** Cycling the component selects an attenuation step `R` of **`-1`**, **`-2`**, **`-4`**, or **`-8`**:
  - At notch 1 (`-1`), it acts as a pure 0-tick isolating diode.
  - Chaining units in series composes any drop from `1` to `15` without needing to snake redstone dust back and forth.

### Visual Design

- **Resistor Bands + Repeater-Style Slab:** Visually reminiscent of a Redstone Repeater overall—a stone slab base featuring inlaid redstone resistor-band notches with sliding selectors indicating the active binary notch (`-1`, `-2`, `-4`, `-8`), plus a forward diode chevron on the slab base.

### Crafting Recipe (Overworld Mid-Tier — Semiconductor Composite Triad, Yields 1)

| | | |
|---|---|---|
| — | Copper Torch | — |
| Copper Ingot | Diamond | Copper Ingot |
| Stone | Redstone Dust | Stone |

### Implementation Notes

- Verify clean synchronous 0-tick neighbor-update ordering and monotonic loop termination when multiple Redstone Variodes are chained or placed in loops alongside redstone dust.
- **Verified Emergent Behavior:** Placing a Redstone Variode inline (e.g., `dust(14) → variode(-1) → dust(13)`) acts as a 0-tick directional signal extender without resetting strength to `15`, which feels natural in-game and is balanced by the mid-tier crafting cost.

### Required JSON Models & Suggested File Names

- **BlockState properties:** `facing` (4 horizontal directions via `y` rotation), `attenuation` (`1`, `2`, `4`, `8`), `powered` (`false`/`true`).
  - *Why 8 block models instead of Repeater's 16:* Vanilla `repeater` doubles from 8 to 16 solely for `locked=true` side-lock variants; `redstone_variode` has no side inputs or lock state.
- **Block models (`assets/redstone_essentials/models/block/`) — 8 files:**
  - `redstone_variode_1.json`, `redstone_variode_1_on.json`
  - `redstone_variode_2.json`, `redstone_variode_2_on.json`
  - `redstone_variode_4.json`, `redstone_variode_4_on.json`
  - `redstone_variode_8.json`, `redstone_variode_8_on.json`
- **Item model (`assets/redstone_essentials/models/item/`) — 1 file:**
  - `redstone_variode.json` *(flat 2D item icon via `minecraft:item/generated`, matching vanilla `repeater`)*
- **Blockstate (`assets/redstone_essentials/blockstates/`) — 1 file:**
  - `redstone_variode.json` *(32 variant entries = `4 facings × 4 notches × 2 powered`)*
- **Item definition (`assets/redstone_essentials/items/`) — 1 file:**
  - `redstone_variode.json` *(points to `redstone_essentials:item/redstone_variode`)*
- **Total:** `8` block models + `1` item model + `1` blockstate + `1` item definition (**11 JSON files**).

---

## 3. Redstone Capacitor (`redstone_capacitor`)

A flat, directional **analog charge-storage and RC-curve signal-shaping component** using pure exponential RC decay.

### Form Factor & Orientation

- **Shape:** Flat Repeater/Comparator-style component, placeable in **4 horizontal directions**.
- **Faces:** Back input face and front output face; **no side inputs** and **not specially Comparator-readable on the sides**.
- **Output:** Emits **weak power** forward (like a Redstone Block / Target) equal to its currently stored charge `C` (`0–15`).

### Charging & Exponential Discharge Mechanics

- **Progressive Delta-Based Charging (`I > C`):**
  - When incoming input `I` is greater than stored charge `C`, the capacitor charges up toward `I` progressively along a delta-dependent curve (faster when `|I - C|` is large, slower as `C` approaches `I`).
- **Pure Exponential RC Discharge ($V(t) = V_0 e^{-t/\tau}$):**
  - Requires zero extra configuration on the block—whenever `I < C`, the capacitor always discharges regardless of what is in front of it, outputting its current stored charge `C` (`0–15`) while each step down takes progressively longer.
  - Naturally composes with Comparators and the **Redstone Variode** to tap specific strength thresholds and durations.
- **Charge Retention Rule (Backpressure):**
  - **Backpressure (`O >= C`):** Retains its stored charge `C` if an external signal on the output side has strength `O >= C`; only discharges while `O < C`.
  - *(Future idea: a specific dedicated "hold-charge" block placed in front of the output could pause discharge if ever desired later.)*

### Initial Timing Tables (Tunable Constants)

- **Delta-Based Charging (`delta = I - C` → game ticks per `+1` step):**
  - `10–15`: `2` ticks
  - `7–9`: `3` ticks
  - `4–6`: `4` ticks
  - `2–3`: `6` ticks
  - `1`: `8` ticks
  - *(Total `0 → 15` charge time: `53` game ticks ≈ `2.65s`)*
- **Exponential RC Discharge (game ticks per `-1` step):**
  - `15→14`: `4t` · `14→13`: `5t` · `13→12`: `6t` · `12→11`: `7t` · `11→10`: `9t`
  - `10→9`: `11t` · `9→8`: `13t` · `8→7`: `16t` · `7→6`: `19t` · `6→5`: `23t`
  - `5→4`: `28t` · `4→3`: `34t` · `3→2`: `41t` · `2→1`: `49t` · `1→0`: `58t`
  - *(Total `15 → 0` discharge tail: `323` game ticks ≈ `16.15s`)*

### Visual Design

- **Glowing Progress Groove:** Flat stone slab with two small copper terminals at back and front, connected by an inlaid redstone strip that fills progressively with light from back to front (`0` = dark, `8` = half-lit, `15` = fully lit).

### Crafting Recipe (Overworld Mid-Early Tier — Waxed-Paper Capacitor Theme, Yields 1)

| | | |
|---|---|---|
| Copper Ingot | Honeycomb | Copper Ingot |
| Copper Ingot | Paper | Copper Ingot |
| Stone | Redstone Dust | Stone |

### Implementation Notes

- Ensure backpressure detection reads *external* redstone signal `O` at the output position while excluding the Capacitor's own emitted power.

### Required JSON Models & Suggested File Names

- **BlockState properties:** `facing` (4 horizontal directions via `y` rotation), `charge` (`0–15`).
- **Block models (`assets/redstone_essentials/models/block/`) — 16 files** *(+ 1 optional shared geometry parent `redstone_capacitor_base.json` so all 16 files only override the top progress-groove texture)*:
  - `redstone_capacitor_0.json` *(empty — groove dark)*
  - `redstone_capacitor_1.json` through `redstone_capacitor_15.json` *(progressive fill levels `1`–`15`)*
- **Item model (`assets/redstone_essentials/models/item/`) — 1 file:**
  - `redstone_capacitor.json` *(flat 2D item icon via `minecraft:item/generated`, matching vanilla `repeater`/`comparator`)*
- **Blockstate (`assets/redstone_essentials/blockstates/`) — 1 file:**
  - `redstone_capacitor.json` *(64 variant entries = `4 facings × 16 charge levels`)*
- **Item definition (`assets/redstone_essentials/items/`) — 1 file:**
  - `redstone_capacitor.json` *(points to `redstone_essentials:item/redstone_capacitor`)*
- **Total:** `16` block models + `1` item model + `1` blockstate + `1` item definition (**19 JSON files**, or 20 with shared base parent).

---

## Deferred / Shelved Ideas

- **Quartz / Crystal Resonator Torch:** A floor/wall torch that emits a clickable/set signal strength (or acts as an analog NOT gate). Shelved due to high functional overlap with the **Tuned Redstone Crystal**, which already pairs naturally with a vanilla Redstone Torch. Revisit only if a distinct quartz niche emerges after playing with the Tuned Redstone Crystal in-game.

---

## Future Ideas

*(Add additional Vanilla+ redstone components and conveniences here as they are designed.)*
