# The BattleTech Aerospace Tactics Primer

### A practical guide for MegaMek players — and an audit specification for Princess

**Version 0.2 — draft**
**Accurate as of MegaMek 0.51.x**

> **Changes in v0.2.** Five claims in v0.1 were checked against the source and four of them were wrong or
> too broad. The dead zone's ground-mapsheet distances are `16n + 1`, not `16(n + 1)` — 17 hexes at one
> altitude of separation, not 32 (8.3, Appendix A). The Above/Below hit column **is** implemented, so
> PC-08-04 is retired (8.5). Dive bombing **is** implemented, so PC-16-01 narrows to altitude bombing and
> strafing (16, 21.2). The terrain dead zone applies only at Nap-of-the-Earth, not to strafing generally
> (8.7). And PC-21-01 is answered: atmospheric aerospace was falling through to the ground-unit ranker
> (21.5). Added PC-10-04 on move order and reacting to opponents who have already committed (10.4). The
> `Compute.inDeadZone` predicate is now stated exactly and pinned by a test (8.10).
**Rules basis:** *Total Warfare* (errata v11.01), *TechManual*, *Tactical Operations*, *Strategic Operations: Advanced Aerospace Rules* (errata v5.0, 2024-11-15), *Campaign Operations*

---

## How to use this document

This primer has two audiences and serves them in the same pass.

**If you are a player**, read the prose. It tells you how aerospace combat actually works in MegaMek, what the rules say, where the community's received wisdom is wrong, and what to do about it. Skip the boxed blocks.

**If you are Claude Code auditing Princess**, the boxed **PRINCESS CHECK** blocks are the specification. Each one states a behaviour the bot should exhibit, where in the codebase that behaviour would live, what a failure looks like from outside, and how confident this document is that the underlying claim is true. The prose around each block is the justification — read it when a check needs interpretation.

### Confidence tags

Every substantive claim carries a tag. **This matters more than anything else in the document.** Aerospace lore is full of confident, wrong assertions; several of them are corrected in these pages. The tags exist so that an automated comparison does not file bugs against reasoning that was never verified in the first place.

| Tag | Meaning | Safe to file a bug against? |
|---|---|---|
| **[VERIFIED]** | Traced to a rulebook page, official errata, or a moderator/errata-staff ruling | **Yes** |
| **[MEGAMEK]** | Confirmed by reading MegaMek source; describes what the client does, not necessarily what the book says | **Yes**, but the bug may be "code disagrees with book," not "Princess disagrees with code" |
| **[DERIVED]** | Reasoning from verified premises. Sound, but not stated anywhere authoritative | **No.** File as an RFE or a discussion item, never as a defect |
| **[UNVERIFIED]** | Believed true, but the verification attempt failed or was incomplete. Usually needs a local `rg` to settle | **No.** Resolve the underlying question first |

Where a claim is tagged **[UNVERIFIED]**, the document says explicitly what would settle it. Those are listed together in Appendix C.

### PRINCESS CHECK block format

```
PRINCESS CHECK PC-NN-NN — severity: high — confidence: VERIFIED
Expect:            what correct behaviour looks like
Look in:           likely file(s) and method(s)
Failure looks like: the observable symptom in a game
Notes:             anything that makes the check ambiguous
```

Check IDs are stable: `PC-<chapter>-<sequence>`. If a check is retired, its ID is not reused.

**Severity scale:**

- **critical** — the bot plays an illegal move, or crashes, or the behaviour makes aerospace unusable
- **high** — the bot systematically loses units or wastes turns; a human would immediately notice
- **medium** — measurably suboptimal play a good human would exploit
- **low** — polish; the bot is fine but could be better

### A standing caveat about Princess

MegaMek's own wiki states plainly that **aerospace fighters under Princess "won't altitude bomb or strafe"** and notes "ineptitude with manually guided ordnance in space." **[MEGAMEK]** Several checks in this document will therefore fail by design rather than by defect. Where that is expected, the block says so. The purpose of checking anyway is to distinguish *known-unimplemented* from *implemented-and-wrong* — those need very different fixes.

---

# PART I — Fundamentals

## Chapter 1. What kind of game are you in?

Everything downstream depends on this, and MegaMek does not make it obvious.

There is **no aerospace "movement mode" picker**. What a fighter can do is determined by the **board type** it is on, plus two game options.

### 1.1 Board types

**[MEGAMEK]** `megamek/src/megamek/common/board/BoardType.java` defines five:

| BoardType | What it is |
|---|---|
| `GROUND` | Ground map |
| `SKY` | Low-altitude / atmospheric, no terrain |
| `SKY_WITH_TERRAIN` | Low-altitude with ground terrain beneath |
| `NEAR_SPACE` | High altitude — space close to a planet, with some atmospheric hexes |
| `FAR_SPACE` | Deep space |

The lobby exposes three of these as **Ground / Atmosphere / Space** (`MapSettings.MEDIUM_GROUND / MEDIUM_ATMOSPHERE / MEDIUM_SPACE`). **[MEGAMEK]** High-altitude appears to be an engine-level board type that is not surfaced as a fourth lobby choice; it is reachable through scenario files. **[UNVERIFIED]**

**[MEGAMEK]** `BoardHelper.java` carries the high-altitude geometry: `isGroundRowHex()`, `isAtmosphericRow()` (rows 1–4, or 1–7 under very high pressure), `isSpaceAtmosphereInterface()`, `isTrueSpaceHex()`, `crossesSpaceAtmosphereInterface()`. The space/atmosphere interface sits at **−1 for vacuum, 5 for standard pressure, 8 for very high pressure**.

### 1.2 Multi-board play

**[MEGAMEK]** `Board.java` supports a hierarchy: `enclosingBoard` and `embeddedBoards` let a ground board sit inside one hex of a low-altitude board, which can itself sit in a ground-row hex of a high-altitude board.

A shipping example lives at `megamek/testresources/data/scenarios/test_setups/multiboard_A2G.mms` — a 50×50 airbase ground board embedded at `[24,15]` of a 35×20 sky board, with a Cheetah IIC deployed at altitude 5 on the sky board and an Atlas on the ground board. Note that this scenario turns `aero_ground_move` **off**, so the fighter fights from the sky board rather than being placed on the ground map.

Simultaneous ground and low-altitude play is real, but it is a **scenario-file feature**, not a lobby feature.

### 1.3 The ×16 scale shift — read this before anything else

**[VERIFIED]** On ground mapsheets, aerospace ranges are multiplied by **16**. A short-range bracket of 0–6 becomes 0–96 ground hexes.

**[VERIFIED]** This multiplier also applies to the air-to-air **dead zone** (Chapter 8). Confirmed by Xotl (BattleTech forums, topic 44725, 24 May 2015): *"Yes. ASFs dueling each other using those movement rules are almost fighting in a phone booth, there's gonna be a lot of dead zone interaction there."*

This single fact reshapes atmospheric air-to-air combat more than any other rule in the game, and Chapter 8 is devoted to its consequences.

> **PRINCESS CHECK PC-01-01** — severity: **high** — confidence: **VERIFIED**
> **Expect:** Princess's range and threat evaluation for aerospace units on a ground board applies the ×16 scale consistently — to weapon brackets *and* to any minimum-range or dead-zone reasoning.
> **Look in:** `megamek/src/megamek/common/compute/Compute.java` → `effectiveDistance()`, `getRangeMods()`; Princess path rankers that call them.
> **Failure looks like:** the bot approaches to a distance that looks correct at low-altitude scale but is far outside or inside the real bracket; or it queues shots the server then rejects.
> **Notes:** the engine-side scaling is believed correct; the risk is a Princess heuristic that computes distance independently rather than deferring to `Compute`.

---

## Chapter 2. The options that define your game

Aerospace behaviour in MegaMek is governed by a large option set, most of it off by default. A bot that ignores these will play the same way regardless of the ruleset in force, which is itself a defect.

### 2.1 The Advanced Aerospace Rules group

**[MEGAMEK]** Exact keys from `OptionsConstants.java`, labels from `megamek/resources/megamek/common/options/messages.properties`, defaults from `GameOptions.java`:

| Key | Lobby label | Default |
|---|---|---|
| `aero_ground_move` | Allow aerospace units on ground maps | **true** |
| `stratops_capital_fighter` | StratOps Capital Fighters — **required for squadrons** | false |
| `fuel_consumption` | StratOps Fuel Consumption | false |
| `advanced_movement` | **StratOps Advanced Movement** ("Newtonian physics-style movement") | false |
| `atmospheric_control` | StratOps Advanced Atmospheric Control Rolls | false |
| `variable_damage_thresh` | StratOps variable damage thresholds | false |
| `stratops_space_bomb` | StratOps Space Bombing | false |
| `stratops_advanced_sensors` | StratOps Advanced Sensors | false |
| `stratops_grav_effects` | StratOps Gravitational Effects | false |
| `heat_by_bay` | StratOps Advanced Heat | false |
| `return_flyover` | (Unofficial) Allow return flyovers | **true** |
| `climb_out` | (Unofficial) Allow climb out at altitude 10 | **true** |
| `stratops_aa_fire` | (Unofficial) Advanced Anti-Aircraft | false |
| `aa_move_mod` | (Unofficial) Reduced to-hit mods when using StratOps AAA | false |
| `allow_large_squadrons` | (Unofficial) Allow large (up to 10) fighter squadrons | false |
| `aero_sanity` | (Unofficial) Aero Sanity Mod for damage | false |
| `unoff_adv_atmospheric_control` | (Unofficial) Old StratOps Advanced Atmospheric Control Rolls | false |

Two default-on options deserve attention because almost nobody notices them:

**[MEGAMEK]** `return_flyover` — *"Allows Aero units that have flown off the map to return after a number of turns equal to 1 + roundup(velocity/4) (+2 if OOC)."*

**[MEGAMEK]** `climb_out` — *"Allows Aero units at altitude 10 to exit the map vertically and return later. Requires 'Allow return flyovers' to be enabled."*

### 2.2 A naming trap

**[MEGAMEK]** The vector-movement option is labelled **"StratOps Advanced Movement"**, not "Advanced Movement (AT2 Vector)". The latter is a dead name that still circulates in forum posts and older documentation. Anything searching for the old string will find nothing.

> **PRINCESS CHECK PC-02-01** — severity: **medium** — confidence: **MEGAMEK**
> **Expect:** Princess's aerospace behaviour is conditioned on the active option set — at minimum `advanced_movement`, `fuel_consumption`, `atmospheric_control`, and `aero_ground_move`.
> **Look in:** `megamek/src/megamek/client/bot/princess/` — `Princess.java` (ranker selection), `AeroPathUtil.java`, `NewtonianAerospacePathRanker.java`.
> **Failure looks like:** identical bot flight behaviour with `fuel_consumption` on and off; or the bot burning fuel it cannot afford in a campaign scenario.
> **Notes:** ranker selection on `advanced_movement` is confirmed present. Fuel and control-roll options are the likely gaps.

> **PRINCESS CHECK PC-02-02** — severity: **medium** — confidence: **MEGAMEK**
> **Expect:** with `climb_out` and `return_flyover` enabled, Princess treats vertical exit at altitude 10 as an available disengagement option for a damaged or out-of-ammo fighter, and understands the re-entry delay of `1 + roundup(velocity/4)` turns.
> **Look in:** `AeroPathUtil.java` → `generateValidAltitudeChanges()` (caps at 10), `isSafePathOffBoard()`; `Princess.java` retreat/flee logic.
> **Failure looks like:** a crippled fighter with no ammo continues to orbit and die rather than climbing out, despite the option being on by default.

---

## Chapter 3. Reading a fighter

### 3.1 Thrust

Safe Thrust and Overthrust work as they do for ground units, with one consequence that dominates aerospace decision-making:

**[VERIFIED]** Exceeding Safe Thrust gives the unit **+2 to hit on its own attacks** (*TW* p. 236). There is no defensive downside — only an offensive one.

This is the single most important budgeting fact in the game. Every maneuver, every altitude change, every acceleration competes for the same pool, and spending past Safe converts your turn from an attacking turn into a repositioning turn.

### 3.2 Altitude changes cost asymmetrically

**[MEGAMEK]** From `MoveStep.java`:

```java
case UP:
    if (entity.isAirborne()) {
        setAltitude(altitude + 1);
        setMp(2);            // climbing costs 2 thrust per level
    }
    break;

case DOWN:
    if (entity.isAirborne()) {
        setAltitude(altitude - 1);
        setMp(0);            // "it costs nothing (and may increase velocity)"
        setNDown(getNDown() + 1);
    }
    break;
```

**Climbing costs 2 Thrust per level. Diving costs nothing** and may increase velocity. **[MEGAMEK]** The *Total Warfare* page for this is **[UNVERIFIED]** — worth pinning before it is quoted as rule rather than as client behaviour.

This asymmetry drives a great deal of Chapter 8. Going down is free; coming back up is expensive.

> **PRINCESS CHECK PC-03-01** — severity: **medium** — confidence: **MEGAMEK**
> **Expect:** Princess's altitude selection accounts for the 2:0 climb/dive cost asymmetry — it should treat descending as cheap and ascending as a real thrust commitment.
> **Look in:** `AeroPathUtil.java` → `generateValidAltitudeChanges()`, `calculateMaxSafeThrust()`; whichever ranker scores the resulting paths.
> **Failure looks like:** the bot climbing and descending with apparently equal willingness, or running out of thrust after a climb it could not afford.

### 3.3 Structural Integrity

SI is the real health bar. Armor is a buffer; SI loss is what kills the airframe and, ultimately, the pilot.

**[VERIFIED]** An aerospace pilot is killed by a crash only if **all Structural Integrity is destroyed** (*TW* p. 128). Aerospace pilots do not take automatic crash damage the way a falling 'Mech's pilot does.

**[MEGAMEK]** `AeroPathUtil.calculateMaxSafeThrust()` returns *"the lowest of safe thrust and structural integrity"* — SI caps how hard you can maneuver, so a damaged fighter is a less agile fighter.

### 3.4 Damage Threshold

**[VERIFIED]** *TechManual* p. 239: each armour **facing** has a Damage Threshold equal to **10 percent of its full armour value, rounded up**. Computed from starting armour, not current.

**[VERIFIED]** Damage **strictly greater than** the threshold triggers a critical check. Damage exactly equal does not.

**[VERIFIED]** How damage is grouped depends on who is shooting:

- **Aero vs. aero:** cluster weapons do **not** roll on the Cluster Hits Table. They deal a single averaged Damage Value (*TW* p. 236). One number per weapon.
- **Ground vs. aero:** normal cluster rules apply. Damage arrives in standard 5-point groupings, each compared separately.
- **Squadron Strike/Strafe:** grouped in 5-point clusters (*StratOps* p. 29).

**[MEGAMEK]** `Aero.initializeThresh()` computes `(int) Math.ceil(getArmor(loc) / 10.0)`, matching the rule exactly.

**[DERIVED]** The weapon-selection consequence: long-range, small-packet crit-fishing weapons — the LRM-5, the AC/2 — are far weaker against aerospace targets than on the ground, because they cannot beat threshold. Weapons that deliver damage in large single packets are disproportionately valuable. Note also that most fighters have a threshold of only **1 or 2**, so in practice almost any hit triggers a check; the design principle matters most against heavy fighters and large craft.

**[MEGAMEK]** The `variable_damage_thresh` option recomputes thresholds from **current** remaining armour rather than starting armour, so a battered fighter starts taking crits from smaller hits. Whether this corresponds to a named, published *Strategic Operations* optional rule is **[UNVERIFIED]** — no page reference could be located.

> **PRINCESS CHECK PC-03-02** — severity: **medium** — confidence: **VERIFIED**
> **Expect:** Princess's weapon selection against aerospace targets prefers weapons whose per-packet damage exceeds the target's Damage Threshold, rather than maximising raw damage total.
> **Look in:** Princess firing-solution code (`FireControl` and its aerospace-relevant subclasses); anywhere expected-damage is computed for an aero target.
> **Failure looks like:** the bot firing an LRM-20 in preference to a PPC at a fighter, when the PPC would force a critical check and the LRM cluster would not.
> **Notes:** the threshold rule is verified; treating it as a *targeting priority* is **[DERIVED]**. File as an RFE, not a defect.

> **PRINCESS CHECK PC-03-03** — severity: **low** — confidence: **MEGAMEK**
> **Expect:** if `variable_damage_thresh` is enabled, Princess's threat model recognises that a damaged enemy fighter becomes progressively easier to crit, and prioritises finishing it.
> **Look in:** Princess target-selection scoring; `Aero.autoSetThresh()` / `autoSetFatalThresh()`.
> **Failure looks like:** the bot spreading damage evenly across fresh and battered enemy fighters under this option.

### 3.5 Fuel

**[MEGAMEK]** Governed by `fuel_consumption`, off by default. When off, fuel is not tracked and range is effectively unlimited within the scenario.

**[MEGAMEK]** A known defect interacts with this: issue **#5704** reports that a *failed* Hammerhead maneuver allows a fuel-less fighter to remain aloft indefinitely.

---

## Chapter 4. The modifiers you must memorise

More misconceptions live in this table than anywhere else in aerospace play. It is short, and it settles several arguments that have run for over a decade.

### 4.1 The complete list

**[VERIFIED]** *TW* p. 236 and related:

| Modifier | Value | Applies to |
|---|---|---|
| Attacker exceeded Safe Thrust | **+2** | attacker's own attacks |
| Target at velocity 0 | **−2** | attacker's benefit (excludes grounded spheroids) |
| Attacker at NoE | **+2** (**+1** if OmniFighter) | attacker's own attacks |
| Attacker Out of Control | **+2** | attacker's own attacks |
| Target using Evasive Action | target's evasion bonus | applies even against ground units shooting up (*TW* p. 77) |
| Large craft using Evasive Action | **+2** | its own attacks (not against capital missiles) |
| Large craft ECHO maneuver | **+1 / +2** | *StratOps* p. 113 |
| **Target made a ground attack this turn** | **−3** | attacker's benefit |
| Non-aero airborne target (VTOL, WiGE) | **+5** | aero attacker |

**[VERIFIED]** Ground-to-air, under `stratops_aa_fire`: the modifier equals the aero's **current velocity**. **[MEGAMEK]** MegaMek's unofficial `aa_move_mod` softens this to `min(velocity / 2, 4)`.

**[VERIFIED]** Ground attackers add **2 hexes of range per altitude** of the target (*TW* p. 107, errata v11.01).

### 4.2 Three conclusions

**Aerospace units have no target movement modifier.** **[VERIFIED]** There is no TMM in aerospace combat. Speed does not make you harder to hit. This is the load-bearing fact behind the entire light-versus-heavy fighter debate (Chapter 14): if velocity buys no defence, then extra thrust is worth only what it buys you *positionally*.

**Attacking is what kills you, not altitude.** **[VERIFIED]** A unit that made a ground attack this turn is at **−3 to be hit**. That is a larger swing than most terrain or range effects, and it is the real reason ground-attack runs are dangerous. Fighters do not die because they flew low; they die because they attacked.

**Evasive Action is unusually good.** **[VERIFIED]** It works against ground units firing upward (*TW* p. 77), which many players assume it does not.

> **PRINCESS CHECK PC-04-01** — severity: **high** — confidence: **VERIFIED**
> **Expect:** Princess does not attribute any defensive benefit to aerospace velocity. Its threat model should show an enemy fighter as equally hittable at velocity 2 and velocity 9.
> **Look in:** Princess damage/threat estimation for aero targets; anywhere a movement modifier is derived from a target's speed.
> **Failure looks like:** the bot preferring to engage slow aero targets and avoid fast ones for reasons other than closure geometry — i.e. importing ground-unit TMM logic into aero evaluation.

> **PRINCESS CHECK PC-04-02** — severity: **high** — confidence: **VERIFIED**
> **Expect:** Princess applies and exploits the **−3 to-hit bonus against a unit that made a ground attack this turn** — both defensively (it should recognise its own fighters become vulnerable when they attack ground) and offensively (it should prioritise enemy fighters that have just made a ground attack).
> **Look in:** `ComputeTargetToHitMods.java` for the engine-side modifier; Princess target-priority scoring for whether it is consulted.
> **Failure looks like:** the bot ignoring an enemy fighter that just strafed in favour of an untouched one at the same range.
> **Notes:** the engine applies the modifier; the question is whether Princess's *target selection* is aware of it.

> **PRINCESS CHECK PC-04-03** — severity: **medium** — confidence: **VERIFIED**
> **Expect:** Princess uses Evasive Action on aerospace units under fire, including when the threat is ground-based anti-air.
> **Look in:** `MovementDisplay.MoveCommand.MOVE_EVADE` / `MOVE_EVADE_AERO` equivalents in the bot's move generation; `AeroPathUtil.java`.
> **Failure looks like:** the bot never evading with fighters, or evading only against air threats.

# PART II — Atmospheric Combat

## Chapter 5. Altitude bands

Altitude is not a continuous resource. It is a set of gates, and each attack type opens at a different one.

### 5.1 The attack bands

**[VERIFIED]** *TW* p. 243, confirmed against MegaMek's validation code:

| Attack | Altitude required | Altitude lost by the attack |
|---|---|---|
| **Strike** | 1–5 | 1 |
| **Strafe** | 1–3 | 0 |
| **Dive Bomb** | **must start between 3 and 5** | 2 |
| **Altitude (level) Bomb** | no band; the penalty scales with altitude instead | 0 |

**Correction to received wisdom:** the widely-repeated claim that *"to Strike, Strafe, or Dive Bomb you have to be at Altitude 5 or lower"* is wrong. There are three different bands, and **dive bombing has a floor as well as a ceiling** — you cannot dive bomb from altitude 2.

**[MEGAMEK]** `MMConstants.DIVE_BOMB_MIN_ALTITUDE = 3`, `DIVE_BOMB_MAX_ALTITUDE = 5`. Gates live in `ComputeToHitIsImpossible.java`, tagged `// TW p.243`.

**[VERIFIED]** All ground-attack types additionally require the target hex to lie on the fighter's **declared flight path**, nominated at the moment of the fighter's movement. Nose-mounted artillery weapons are the exception.

### 5.2 Nap-of-Earth

**[VERIFIED]** At Altitude 1 (NoE), the attacker takes **+2 on its own attacks**, reduced to **+1** for OmniFighters.

**[MEGAMEK]** NoE strafing has an additional line-of-sight restriction: blocked when `prevElev − currElev − targetHeight > 2`. See Chapter 8.7 — this is a terrain dead zone, and its boundary condition may be off by one.

> **PRINCESS CHECK PC-05-01** — severity: **high** — confidence: **VERIFIED**
> **Expect:** when Princess plans a ground attack, it selects an altitude legal for the attack type it intends — including respecting the **dive bomb floor of 3**, not merely the ceiling of 5.
> **Look in:** `AeroPathUtil.java` → `generateValidAltitudeChanges()`; Princess's ground-attack intent logic, if any exists.
> **Failure looks like:** the bot descending to altitude 1–2 "to attack" and then having no legal attack available.
> **Notes:** the wiki states Princess does not altitude bomb or strafe at all, so this check may fail as *unimplemented*. Distinguish that from *implemented with the wrong band*.

> **PRINCESS CHECK PC-05-02** — severity: **medium** — confidence: **VERIFIED**
> **Expect:** Princess accounts for the altitude *lost* by an attack (Strike −1, Dive Bomb −2) when deciding whether a run is survivable, rather than evaluating only the starting altitude.
> **Look in:** `WeaponAttackAction.getAltitudeLoss()`; Princess path scoring for post-attack state.
> **Failure looks like:** the bot dive bombing from altitude 3, arriving at altitude 1, and then having no thrust budget to climb back out.

---

## Chapter 6. Control rolls and the death spiral

### 6.1 What actually happens when you fail

**[VERIFIED]** Failing a Control Roll makes the unit **Out of Control**. On the low-altitude map, an out-of-control unit **automatically loses 1d6 altitude** — but **only on the transition into out-of-control.** Subsequent failed control rolls while already out of control do not trigger additional 1d6 drops.

**[VERIFIED]** Descending 2 or more altitudes also **gains 1 velocity**, which compounds any eventual crash.

**[VERIFIED]** Reaching the ground is a **crash, not automatic destruction**. Crash damage is **2D6 × 10 × current velocity** (*TW* pp. 81–83). A heavy fighter can survive with armour remaining. The pilot dies only if all SI is destroyed (*TW* p. 128).

**Correction to received wisdom:** the popular framing that a failed control roll at low altitude means "instant death" overstates the case. The *probabilities of losing control and reaching the deck* are correct; the consequence is a survivable crash, not a guaranteed kill.

**[VERIFIED]** A standing **+2** applies to all control rolls in high-altitude, low-altitude, and ground-map play, including at the atmospheric interface and during High-G maneuvers.

### 6.2 The risk math

**[DERIVED]** Combining the 1d6 altitude loss with the attack bands in Chapter 5:

| Situation | Chance a failed control roll puts you on the deck |
|---|---|
| Loitering at Altitude 5, not attacking | ~1 in 3 |
| Striking (Alt ≤5, −1 altitude from the attack) | ~50/50 |
| Dive bombing (start 3–5, −2 altitude from the attack) | ~2 in 3 |

The conclusion a player should draw is not "never fly low." It is **"never fly low while also doing something that adds a control roll or costs altitude, unless the payoff justifies it."**

### 6.3 The Advanced Atmospheric Control Rolls option

**[VERIFIED]** *StratOps* p. 85. **[MEGAMEK]** Option `atmospheric_control`, "StratOps Advanced Atmospheric Control Rolls", default off.

**Correction to received wisdom:** this option does **not** make altitude loss scale with Margin of Failure. Per the **StratOps AAR errata v5.0 (2 Aug 2024)** it changes **when you roll**: control rolls occur when a unit takes an Avionics or Control critical hit *or* **when it sustains a hit exceeding its Damage Threshold**. If a threshold-exceeding hit also causes critical damage, **two control rolls are made**. **[VERIFIED]**

**[MEGAMEK]** MegaMek implements the errata version (PR #5867, Sep 2024), comparing total damage in a round against the unit's highest threshold across locations, and retains the pre-errata behaviour separately as `unoff_adv_atmospheric_control`.

Advice written before August 2024 describes the old rule. Treat it with suspicion.

> **PRINCESS CHECK PC-06-01** — severity: **high** — confidence: **VERIFIED**
> **Expect:** Princess weights the *consequence* of a control-roll failure by current altitude — a +2 maneuver at altitude 8 is nearly free, the same maneuver at altitude 3 risks the airframe.
> **Look in:** `AeroPathUtil.java` → `willCrash()`, `willStall()`; the path ranker's penalty for control-roll-inducing paths.
> **Failure looks like:** the bot taking identical maneuver risks at altitude 2 and altitude 9.
> **Notes:** `AeroPathUtil` is documented as emphasising stall/crash avoidance, so some of this likely exists. The question is whether the penalty is *graded by altitude* or binary.

> **PRINCESS CHECK PC-06-02** — severity: **medium** — confidence: **VERIFIED**
> **Expect:** with `atmospheric_control` enabled, Princess understands that taking a threshold-exceeding hit will force a control roll, and factors that into whether it exposes a damaged fighter.
> **Look in:** Princess threat evaluation; the `atmospheric_control` handling added in PR #5867.
> **Failure looks like:** no behavioural difference in bot flying with the option on versus off.

> **PRINCESS CHECK PC-06-03** — severity: **medium** — confidence: **VERIFIED**
> **Expect:** Princess treats an Out-of-Control state as recoverable and does not write the unit off — but does account for the **+2 to its own attacks** while OOC.
> **Look in:** Princess's handling of the OOC flag in both movement and firing evaluation.
> **Failure looks like:** the bot continuing to plan precision attacks with an OOC fighter as though nothing had changed.

---

## Chapter 7. Special maneuvers

Nothing in the community's tactical literature discusses this table. It rewards study.

### 7.1 The table

**[VERIFIED]** *Total Warfare*, Special Maneuvers Table (Aerospace Movement section):

| Maneuver | Min/Max Velocity | Thrust Cost | Control Mod | Effect |
|---|---|---|---|---|
| **Loop** | Min 4 | 4 | **+1** | Spends its first 4 points of velocity in the loop; actual velocity unchanged. Ends in the same hex it started, then spends the remainder of its velocity normally. |
| **Immelmann** | Min 3 | 4 | **+1** | Gains 2 altitude, ends facing any hexside. Velocity drops by 2. Remainder spent normally. |
| **Split-S** | Any | 2 | **+2** | Loses 2 altitude, ends facing any hexside. Velocity increases by 1. |
| **Hammerhead** | Any | **= Velocity** | **+3** | Remains in the hex it started, changes facing 180°. |
| **Half-roll** | Any | 1 | **−1** | Rolls 180°, reversing left/right sides and up/down facings. |
| **Barrel roll** | Min 2 | 1 | **0** | Rolls 360°, ending with the same facing. Velocity drops by 1. |
| **Side-slip** | Any | 1 | **0** (−1 VSTOL) | Moves into the front-left or front-right hex instead of directly ahead, **without changing facing**. On ground mapsheets: 8 hexes front-left/right, then 8 more directly forward. |
| **VIFF** | VSTOL only | **Velocity + 2** | **+2** | Halts forward momentum and gains one altitude. |

**[MEGAMEK]** Exposed through the `MOVE_MANEUVER` and `MOVE_ROLL` movement commands.

### 7.2 Three structural readings

**The control modifier is the real price, not the thrust.** **[DERIVED]** A failed control roll costs 1d6 altitude (Chapter 6), so a maneuver's cost must be read against your *current altitude*. A +2 maneuver at altitude 8 is nearly free; the same maneuver at altitude 3 is a coin-flip on the airframe. Damage-induced control modifiers stack on top — which is the mechanism by which a shot-up fighter loses agency.

**Thrust spent maneuvering competes with everything else.** **[DERIVED]** A 4-thrust Loop or Immelmann consumes most of a 6/9 fighter's Safe Thrust in a single action, which likely pushes the rest of the turn over Safe and costs **+2 to hit** on its attacks. Loop and Immelmann are commitments for the turn, not free repositioning.

**Hammerhead's cost scales with velocity, which inverts the intuition.** **[DERIVED]** At velocity 8 it costs 8 thrust and is unaffordable. At velocity 2 it costs 2. **Hammerhead is a slow-speed maneuver.** Anyone reading it as the high-speed emergency reverse has it backwards.

### 7.3 When to use each

| Maneuver | Use it when | Avoid it when |
|---|---|---|
| **Half-roll** | **Constantly.** At 1 thrust and **−1** it is the only maneuver that makes your control roll *easier* than not maneuvering. Its job is armour management — swap a stripped wing for a fresh one between passes. | Rarely a bad call. The cost is one thrust point and a favourable roll. |
| **Side-slip** | You want to leave the enemy's firing arc **while keeping your nose on them.** 1 thrust, no control penalty, lateral displacement with facing preserved. Also the flight-path shaping tool on ground mapsheets — the 8-and-8 movement is how you bring a target hex onto your declared path when it is not straight ahead. | You need an actual heading change. It displaces; it does not turn. |
| **Barrel roll** | Rarely. At 1 thrust and 0 modifier it drops 1 velocity — which plain deceleration already does, at the same cost, **without a control roll.** On the table as written it is strictly dominated. | Essentially always. See the caveat below. |
| **Split-S** | You need **altitude down, facing reversed, and speed up** at once. Escaping a fight; dropping from patrol altitude into the strike band; reversing onto a pursuer. 2 thrust is cheap for what it delivers. | **Below altitude 4.** Losing 2 altitude on a +2 roll means a failure at altitude 3 leaves you at 1 and then rolls 1d6. Also when you cannot afford +1 velocity. |
| **Immelmann** | The mirror: **altitude up, facing reversed, speed bled off.** The natural reset after a strike or bombing pass — climb out of the AA envelope, reverse, set up the next run. Also how you reach altitude 10 to use `climb_out`. | When 4 thrust is unaffordable. Requires velocity 3+. |
| **Loop** | You are **going too fast for the fight you are in.** Converts 4 points of velocity into zero displacement — the loiter tool. Stay on station over a CAS target, avoid overflying into a flak nest, wait for slower friendlies, avoid running off the map edge. | Velocity below 4 (illegal), or when 4 thrust is needed elsewhere. |
| **Hammerhead** | **Only at low velocity**, where cost = velocity is affordable. Buys a free 180° with zero displacement. | At any meaningful velocity, and with existing control penalties stacked on the +3. |
| **VIFF** | VSTOL only, and only when stopping dead is worth it — loitering over a target, spotting, or forcing a pursuer to overshoot. | Almost always. Velocity+2 thrust plus +2 control is the most expensive line on the table. |

### 7.4 The derived rules of thumb

**[DERIVED]**

1. **Half-roll and side-slip are the workhorses.** One thrust each, no control penalty (half-roll is a bonus), and they solve the two problems fighters actually have: asymmetric armour damage, and needing to leave an arc without losing your firing solution. Most players never touch either.
2. **Split-S and Immelmann are a matched pair.** Trade altitude for speed or speed for altitude, and get a free facing change either way. Learn these two and you have an attack-run cycle.
3. **Read every maneuver's control modifier as an altitude bet.**
4. **Do not maneuver on the same turn you make a ground attack unless you have thrust to spare** — the attack already gives every shooter −3 to hit you.
5. **Loop is the answer to "I keep overshooting."**

### 7.5 Caveats before acting on this

**[UNVERIFIED]** The "barrel roll is dominated" reading is derived from the table as reproduced here. If *Total Warfare*'s surrounding text confers something the table does not restate, the conclusion changes. Check before publishing to a wide audience.

**[MEGAMEK]** Two open issues suggest the client's maneuver implementation is not perfectly faithful:
- **#4606** — half-roll allows an illegal facing change
- **#5704** — a failed Hammerhead lets a fuel-less fighter stay aloft indefinitely

> **PRINCESS CHECK PC-07-01** — severity: **medium** — confidence: **MEGAMEK**
> **Expect:** Princess's aerospace move generation includes special maneuvers at all — at minimum half-roll, side-slip, Split-S, and Immelmann should appear in generated paths.
> **Look in:** `AeroPathUtil.java` and whatever builds candidate `MovePath`s for aero units; look for `MOVE_MANEUVER` / `ManeuverType` usage.
> **Failure looks like:** the bot's fighters only ever accelerating, decelerating, turning, and changing altitude — never executing a named maneuver.
> **Notes:** if maneuvers are absent from generation entirely, this is a single high-value RFE rather than eight separate defects.

> **PRINCESS CHECK PC-07-02** — severity: **medium** — confidence: **DERIVED**
> **Expect:** if maneuvers are generated, half-roll is preferred when the bot's fighter has asymmetric wing damage, since it is nearly free and swaps the damaged facing.
> **Look in:** maneuver scoring in the aero path rankers.
> **Failure looks like:** the bot flying a fighter with a stripped left wing into repeated left-side engagements without ever rolling.
> **Notes:** **[DERIVED]** — file as an RFE.

> **PRINCESS CHECK PC-07-03** — severity: **low** — confidence: **DERIVED**
> **Expect:** if Hammerhead is generated, it is only considered at low velocity, since thrust cost equals velocity.
> **Look in:** maneuver legality/cost checks in path generation.
> **Failure looks like:** the bot attempting or scoring Hammerhead at velocity 6+, wasting the entire thrust budget.

---

## Chapter 8. Dead zones

This is the most consequential rule in atmospheric aerospace combat, and the least discussed. It silently decides most engagements, it is invisible in the client, and it makes several pieces of standard doctrine actively wrong.

### 8.1 Two rules, one name

**"Dead zone" names two unrelated rules.** Keep them separate:

- **The altitude dead zone** — air-to-air, *TW* p. 241. Pure geometry.
- **The terrain dead zone** — air-to-ground. Terrain masking along the attacker's approach.

They share a name and nothing else.

### 8.2 The altitude dead zone

**[VERIFIED]** *TW* p. 241. Atmospheric combat uses the space rules with three exceptions:

**1. Range adds altitude difference.** Two fighters ten hexes apart, one at Altitude 3 and one at Altitude 5, are at an effective range of **12** hexes (10 + [5 − 3]).

**2. You cannot aim into the column immediately above or below yourself.** Quantified: **minimum horizontal range = altitude difference + 1.** One altitude apart, the target must be at least 2 hexes away. Two altitudes apart, at least 3 hexes. And so on.

**3. Hit location changes.** Within 2 altitudes of each other, use the normal Hit Location Table column based on attack direction. **At 3 or more altitudes difference, use the Above/Below column** (*TW* p. 237).

Geometrically, the dead zone is a **cone** above and below every airborne unit, widening as altitude separation grows — not a fixed bubble.

**[VERIFIED]** No errata entry touches the dead zone or the Air-to-Air rules. The rule as written stands unamended.

### 8.3 The ×16 problem

**[VERIFIED]** On ground mapsheets, the dead zone scales by 16 along with everything else (Xotl, forums topic 44725).

Run the numbers:

| Altitude difference | Minimum horizontal range (low-altitude hexes) | Minimum on a ground mapsheet |
|---|---|---|
| 1 | 2 | **17 hexes** |
| 2 | 3 | **33 hexes** |
| 3 | 4 | **49 hexes** |
| 4 | 5 | **65 hexes** |
| 6 | 7 | **97 hexes** |

**[MEGAMEK] Correction to v0.1**, verified against the engine and pinned by `AerospaceGeometryTest`: the
ground-mapsheet column is *not* a flat ×16 of the low-altitude column. `Compute.effectiveDistance` converts
ground hexes to low-altitude hexes with **`ceil(distance / 16)`** before the dead-zone comparison, so the
first ground distance that clears an altitude difference of *n* is `16n + 1`, not `16(n + 1)`. One altitude
of separation blocks fire within **17** hexes, not 32.

The doctrinal conclusion is unchanged — a 2×2 mapsheet ground map is roughly 32×34 hexes, so a single level
of separation still sterilises half the board and two levels sterilise all of it.

**[DERIVED]** Therefore: **on a typical MegaMek ground map, any altitude difference at all makes air-to-air combat impossible across most or all of the board.**

Two pieces of doctrine follow immediately, and neither is written down anywhere in the community:

**Escorts must fly at their charge's exact altitude, horizontally offset.** **[DERIVED]** Stacking vertically for "top cover" does not merely perform worse — it produces an escort that literally cannot engage anything attacking its charge.

**High CAP is a myth.** **[DERIVED]** A fighter at Altitude 8 cannot touch a strafer at Altitude 2: six altitudes of separation means a 7-hex minimum at low-altitude scale, 112 hexes on a ground map, +6 to range, and the Above/Below hit column. **Altitude is not a free overwatch position.** An interceptor has to come *down* into the anti-air envelope to fight, which walks straight into Chapter 6's risk math.

### 8.4 The dead zone as cover

**[DERIVED]** The rule is symmetric. If you cannot shoot them, they cannot shoot you.

An outgunned fighter can therefore **deliberately nest directly above or below a heavier opponent** at close horizontal range and become untargetable by it. This is a rules-legal survival tactic that costs nothing, and it is a partial answer to a question the community has never resolved — whether a light fighter that closes and then loses initiative can survive the consequence. In atmosphere, it can: it dives into the dead zone.

**[MEGAMEK]** And the economics favour it. Diving costs **0 thrust**; climbing costs **2 per level** (Chapter 3.2). Entering a dead zone is free. Leaving one is the expensive half.

### 8.5 Above/Below is the crit column

**[MEGAMEK]** From `Aero.rollHitLocation(int table, int side)`:

| Roll | Location | Potential critical |
|---|---|---|
| 2 | Nose | Weapon |
| 3, 11 | Random wing (1d6, 4+ = left) | Gear |
| 4 | Nose | **Sensor** |
| 5 | Nose | **Crew** |
| 6, 8 | Random wing | Weapon |
| 7 | Nose | **Avionics** |
| 9 | Aft | **Control** |
| 10 | Aft | **Engine** |
| 12 | Aft | Weapon |

Distribution: **Nose 38.9% / Wings 38.9% / Aft 22.2%.**

Compared against the other columns:

| Column | Nose | Wings | Aft |
|---|---|---|---|
| **Above/Below** | 38.9% | 38.9% (random wing) | 22.2% |
| Front | 61.1% | 38.9% | 0% |
| Left / Right | 22.2% | 55.6% | 22.2% |
| Rear | 0% | 38.9% | 61.1% |

**[DERIVED]** Above/Below is essentially the side column with roll 7 moved from wing to nose and the wing chosen randomly — which makes it **the crit column**. Crew on a 5 is 4/36 (11.1%); avionics on a 7 is 6/36 (16.7%). Unlike a front-arc attack, it still reaches the aft for control and engine crits.

**It is the only geometry in the game that threatens crew, avionics, control, and engine critical hits from a single column.** Against an opponent you cannot out-damage, deliberately separating by 3 or more altitudes trades range for a pilot-killing, mission-killing hit table. Nobody discusses this.

**[MEGAMEK] — resolved, and v0.1 had it wrong.** Altitude difference **is** wired to the Above/Below column.
`ComputeTerrainMods.java` (lines 348–360) does exactly what *TW* p. 237 asks:

```java
if (Compute.isAirToAir(game, attacker, target)) {
    int altitudeDelta = attacker.getAltitude() - target.getAltitude();
    if (altitudeDelta > 2) {
        toHit.setHitTable(HIT_ABOVE);
    } else if (altitudeDelta < -2) {
        toHit.setHitTable(HIT_BELOW);
    } else if ((altitudeDelta > 0) && attacker.isSpheroid()) {
        toHit.setHitTable(HIT_ABOVE);
    } else if ((altitudeDelta < 0) && attacker.isSpheroid()) {
        toHit.setHitTable(HIT_BELOW);
    }
}
```

Threshold and spheroid exception both match the book. The v0.1 search missed it because it looked for the
constants near the called-shot setters rather than in the terrain-modifier stage. **PC-08-04 is retired** and
Appendix C question 2 is closed.

> **PRINCESS CHECK PC-08-01** — severity: **critical** — confidence: **VERIFIED**
> **Expect:** Princess never plans a path that places one of its fighters in a position from which it cannot legally attack its intended target, and never selects a target inside its own dead zone.
> **Look in:** `megamek/src/megamek/common/compute/Compute.java` → `inDeadZone()`, called from `getRangeMods()`; Princess target selection and path scoring for whether either consults it.
> **Failure looks like:** the bot manoeuvring into a "good" position directly above or below an enemy and then taking no shots for several turns.
> **Notes:** the engine blocks the shot with `ToHitData(IMPOSSIBLE, "Target in dead zone")`, so this cannot produce an illegal move — it produces a wasted turn. On a ground map with ×16 scaling, a wasted turn can be the whole engagement.

> **PRINCESS CHECK PC-08-02** — severity: **critical** — confidence: **DERIVED**
> **Expect:** on a **ground** board, Princess treats altitude matching as a precondition for air-to-air engagement. With ×16 scaling, one altitude of separation imposes a 32-hex minimum — larger than many maps.
> **Look in:** Princess aero path ranking on ground boards; any altitude-selection heuristic that does not consult enemy fighter altitude.
> **Failure looks like:** two opposing flights orbiting a ground map at different altitudes and never exchanging fire.
> **Notes:** the ×16 dead-zone scaling is **[VERIFIED]**; the doctrinal conclusion is **[DERIVED]**. This is the single highest-value aerospace behaviour to test, and the easiest to reproduce: set up two bot flights at altitudes 3 and 5 on a ground map and watch.

> **PRINCESS CHECK PC-08-03** — severity: **high** — confidence: **DERIVED**
> **Expect:** Princess recognises the dead zone as **cover** — a damaged or outgunned fighter should be able to dive into an enemy's dead zone as a survival move, exploiting the free descent.
> **Look in:** `AeroPathUtil.java`; Princess's retreat/self-preservation scoring.
> **Failure looks like:** a crippled bot fighter fleeing horizontally under fire when a free 1–2 altitude change would have made it untargetable.
> **Notes:** **[DERIVED]** — file as an RFE. It is also a strong candidate for a genuinely novel bot capability.

> **PRINCESS CHECK PC-08-04** — **RETIRED** (was: medium / UNVERIFIED)
> **Resolution:** implemented correctly in `ComputeTerrainMods.java:348-360`, at the right threshold and with
> the spheroid exception. Engine question closed; no bot work follows from it. ID not reused.

### 8.6 The spheroid exception

**[VERIFIED]** *"Spheroid craft may target units in their 'dead zone' (see Air-To-Air Attacks, p. 241). For targets at a higher altitude, the spheroid may fire its Nose weapons at the target. For targets at a lower altitude, the spheroid may fire its Aft weapons at the target."*

**Spheroid craft are the only units in the game that can shoot straight up and down.** Two consequences:

**[DERIVED]** **A spheroid is the hard counter to dead-zone nesting.** Everything in 8.4 fails against one. Never park in a spheroid's vertical arc.

**[DERIVED]** **And that is a reason to bring one.** A spheroid escort covers the vertical approaches no aerodyne fighter can cover. This gives spheroid small craft and DropShips a genuinely distinctive escort role — one the community's "gunboat small craft" idea has gestured at for years without ever explaining mechanically.

**[MEGAMEK]** Implemented, and correctly. `ComputeToHitIsImpossible.java` excludes spheroids from the blanket block and applies arc restrictions instead:

```java
if (Compute.inDeadZone(game, attacker, target)) {
    // Only nose weapons can fire at targets in the dead zone at higher altitude
    if ((altDif > 0) && (weapon.getLocation() != Aero.LOC_NOSE)) {
        return Messages.getString("WeaponAttackAction.OnlyNoseInDeadZone");
    }
    // and only aft weapons can fire at targets in the dead zone at lower altitude
    if ((altDif < 0) && (weapon.getLocation() != Aero.LOC_AFT)) {
        return Messages.getString("WeaponAttackAction.OnlyAftInDeadZone");
    }
}
```

The same file carries the spheroid arc-versus-altitude restrictions cited to **TW errata 2.1**: `TooLowForNose`, `TooLowForFrontSide`, `TooHighForAft`, `TooHighForAftSide`.

> **PRINCESS CHECK PC-08-05** — severity: **medium** — confidence: **VERIFIED**
> **Expect:** when flying a spheroid, Princess exploits the vertical-arc privilege — it should be willing to engage targets directly above (nose weapons) and below (aft weapons) that an aerodyne could not touch.
> **Look in:** `AeroPathUtil.getSpheroidDir()` (documented as pointing DropShips at the enemy centroid and rotating damaged sections away); Princess weapon selection for spheroids.
> **Failure looks like:** a bot spheroid manoeuvring for a horizontal firing solution it does not need, or declining a vertical shot it is legally entitled to.

> **PRINCESS CHECK PC-08-06** — severity: **medium** — confidence: **DERIVED**
> **Expect:** Princess does not park an aerodyne fighter in an *enemy spheroid's* vertical arc believing it to be safe.
> **Look in:** any dead-zone-as-cover logic added under PC-08-03 — it must exclude spheroid threats.
> **Notes:** this check only becomes relevant if PC-08-03 is implemented. Flagging it now prevents introducing the bug alongside the feature.

### 8.7 The terrain dead zone

**[VERIFIED]** *"If the hex in front of the target is two or more levels higher than the level of the target unit, the target is in a dead zone and cannot be attacked."*

This is terrain masking along the attacker's approach — a different rule that happens to share a name.

**[VERIFIED] — scope correction to v0.1.** This rule is **specific to Nap-of-the-Earth**, not to strafing in
general. *TW* p. 243 places it inside the NoE clause: *"Most strafing attacks must take into account only the
terrain in the target's hex. However, units flying at Altitude 1 (NOE) find it harder to establish a clear
line of sight and so must also take into account the terrain in the hex adjacent to the target..."* MegaMek
matches the book — the check lives in the NoE branch, which is why it looked like an implementation gap. A
strafing run at altitude 2 or 3 is not subject to it.

**[DERIVED]** **It is directional.** "In front of" is relative to the attacker's flight path, so a unit is masked from one approach vector and exposed from another. Two consequences:

- **For the ground player, terrain is an air-defence asset.** Park in hollows and behind ridges relative to the likely threat axis.
- **For the aerospace player, flight-path planning is a terrain problem**, not just a threat-avoidance problem. You route to approach from the open side.

This is the mechanical backbone of the "route so the fewest enemies have line of sight" heuristic, and it explains why aborting an attack run is so often correct: frequently the geometry simply is not there.

**[MEGAMEK]** The NoE strafing check is implemented as `prevElev − currElev − targetHeight > 2` — masked when the difference **exceeds** 2. The rule as quoted says **"two or more levels higher,"** i.e. masked at **≥ 2**.

**[UNVERIFIED]** The `targetHeight` term makes a direct comparison ambiguous, so this may be correct. But the boundary case is worth settling, because a one-level error changes which targets are attackable on every ground map.

> **PRINCESS CHECK PC-08-07** — severity: **high** — confidence: **VERIFIED**
> **Expect:** when planning a ground-attack flight path, Princess checks terrain masking along the approach and selects an approach vector from which the target is not in a terrain dead zone.
> **Look in:** the NoE/strafing LOS check (`prevElev − currElev − targetHeight`); Princess ground-attack path generation.
> **Failure looks like:** the bot committing a strafing run against a target in a hollow and having the attack rejected or produce nothing.
> **Notes:** likely fails as *unimplemented*, since Princess reportedly does not strafe at all.

> **PRINCESS CHECK PC-08-08** — severity: **medium** — confidence: **UNVERIFIED**
> **Expect:** the terrain-masking boundary matches the rule — masked at 2 or more levels, not 3 or more.
> **Look in:** the `> 2` comparison in the NoE strafing check; compare against *TW* text with `targetHeight` accounted for.
> **Notes:** **Engine question, not a Princess question.** Resolve first.

### 8.8 Scope limits

State these plainly, because assuming otherwise causes real errors:

**[MEGAMEK]** **There is no dead zone in space.** `Compute.effectiveDistance()` gates the altitude-to-range addition on `isAirToAir(...) && !attacker.isSpaceborne()`. Space maps have no altitude levels.

**[MEGAMEK]** **There is no dead zone for ground-to-air.** Ground-to-air uses a different range formula — `2 × target.getAltitude()`, or `1 ×` for weapon-bay units on a ground board — and no dead-zone term. Ground units instead have a hard ceiling: targets above altitude 8 are untargetable (`AeroTooHighForGta`).

**[DERIVED]** The tactical consequence of that second point is worth stating: **altitude is not cover from ground fire.** There is no hiding directly above a flak nest. The only ground-fire protections are range, the −3 penalty you *give up* by attacking, and terrain masking.

**[MEGAMEK]** **VTOL handling is flagged broken by the developers.** `effectiveDistance` bumps an airborne VTOL/WiGE target by `tAlt++` with the source comment `//FIXME VTOLs cannot be A2A`. Treat MegaMek's VTOL dead-zone behaviour as unreliable.

### 8.9 What you will actually experience in MegaMek

**[MEGAMEK]** The rule is enforced as a hard block in `Compute.getRangeMods()`:

```java
// Account for "dead zones" between Aeros at different altitudes
if (!Compute.useSpheroidAtmosphere(game, attackingEntity)
        && Compute.inDeadZone(game, attackingEntity, target)) {
    return new ToHitData(TargetRoll.IMPOSSIBLE, "Target in dead zone");
}
```

Two open issues matter tactically:

**[MEGAMEK]** **Issue #332** (open since September 2016) — *"Aero fighters attacking ground targets create deadzone during weapons phase?"* A fighter targeted at Altitude 5 dive-bombed down to Altitude 3; at resolution MegaMek re-evaluated the **new** altitude, found a dead zone, and the incoming attacks silently vanished. The reporter's own words: *"I think this is really cool, and kind of want it to stay in so your fighters can 'hit the deck' to escape an attack, but it might be a bug."*

**This is a live, exploitable behaviour in current MegaMek.** Diving after being targeted can shed incoming fire. It is described here as client behaviour, not as a rule.

**[MEGAMEK]** **Issue #409** (open since November 2016, labelled Outdated) — a request for a **dead-zone overlay** showing which hexes a selected unit cannot attack from at its current altitude, styled like the existing range-band and ECM overlays. **No dead-zone visualisation ships today.** Given the ×16 scaling, players are being silently denied shots by geometry they cannot see.

> **PRINCESS CHECK PC-08-09** — severity: **high** — confidence: **MEGAMEK**
> **Expect:** Princess does not have its attacks silently deleted by issue #332 — i.e. its declared attacks should not evaporate because the *target* changed altitude between declaration and resolution, and it should not be surprised when they do.
> **Look in:** the interaction between the firing-phase dead-zone re-evaluation and Princess's expected-damage bookkeeping.
> **Failure looks like:** the bot repeatedly declaring attacks that resolve to nothing against a diving opponent, with no adaptation.
> **Notes:** this compounds with issue **#7620** (ground attacks declared in the wrong phase). If both are live, bot air-to-air against a ground-attacking opponent may be substantially degraded.

### 8.10 Open engine questions

**[MEGAMEK] — resolved.** `Compute.inDeadZone()` (`Compute.java:7085`) is:

```java
int altDiff = Math.abs(aAlt - tAlt);
return altDiff >= (distance - altDiff);
```

where `distance` is `effectiveDistance`, which has *already* added the altitude difference to the converted
horizontal range. The two cancel, so the predicate is simply:

> **dead zone ⇔ altitude difference ≥ horizontal range in low-altitude hexes**

which is *TW* p. 241 exactly ("one altitude apart, the target must be at least two hexes away"). On a ground
mapsheet the horizontal range is `ceil(groundHexes / 16)` — the rounding that produces the 17-hex and 33-hex
boundaries in 8.3 rather than 32 and 48.

This is now pinned by a test rather than by reading: `AerospaceGeometryTest` sweeps altitude differences 0–6
against distances 0–70 on both venues and asserts the bot's own predicate agrees with `Compute.inDeadZone`
on every combination.

The remaining chapter question is the terrain-masking boundary (8.7), which is still open:

```
rg -n "prevElev" megamek/src/
```

---

## Chapter 9. The dogfight

### 9.1 Why real-world instincts mislead

The best framing anyone in the community has produced belongs to the forum poster Jackmc:

> "Real World dogfighting is focused on getting into a position for [a] single decisive killing/crippling stroke which is possible due to the relative fragility of aircraft compared to weapons systems. BT ASF's do not generally operate under that paradigm as only the lightest fighters can be one-shotted by heavy weapons. Instead, think of it as a high speed version of ships of the line attempting to batter each other into submission."

**[DERIVED]** Two verified rules explain *why* this is true:

**There is no defensive speed modifier** (Chapter 4). You cannot kite your way out of an attrition fight. Disengaging requires actual separation, not just velocity.

**You cannot attack what is directly beneath you** (Chapter 8). The classic diving attack out of the sun is the one attack the rules forbid. Approaches must be shallow.

### 9.2 Arcs are discrete

**[VERIFIED]** Aerospace firing arcs are nose, left wing, right wing, and aft — four discrete arcs, not a continuous field of fire. Positional play is therefore about arc membership, not angle.

**[DERIVED]** The consequence: getting on an opponent's six is good, but firing from an arc in which they have **no weapons that bear** is better. The former is a hit-location advantage; the latter is immunity.

### 9.3 What velocity should you actually run?

This question was asked on the official forums in 2012 and never answered. Here is an answer.

**[DERIVED]** The new player's symptom — *"I start to feel 'out of control' if I'm going more than about two to three times my safe thrust"* — is real, and it is not about velocity itself. Velocity is free to carry. The problem is that **at high velocity, more of your thrust budget is consumed by course correction**, and thrust spent correcting is thrust not spent on maneuvers, altitude, or staying under Safe Thrust.

The budgeting rule that falls out:

1. Your Safe Thrust is the whole budget. Exceeding it costs **+2 to hit** on everything you fire.
2. Turning, altitude gain, and special maneuvers all draw from that budget.
3. Therefore: **run the lowest velocity that still lets you reach the fight and leave it.** Velocity beyond that converts directly into lost agency.
4. Diving is free thrust (Chapter 3.2). If you need speed, buy it with altitude rather than with thrust — that is exactly what Split-S does.

### 9.4 Slash or slug

**[DERIVED]** The community's two competing prescriptions — high-velocity slashing passes versus sustained attrition — are both right, in different matchups. The reconciliation:

- **Slash** when you out-range or out-run the opponent. Deny them firing opportunities in their preferred bracket, accept fewer of your own.
- **Slug** when you out-armour them. Since neither side gets a defensive speed modifier, sustained exchange favours whoever survives more rounds of it.

The error is picking one as a universal doctrine.

### 9.5 Initiative is a primary combat stat

**[DERIVED]** Because most fighter firepower sits in the nose arc, and because facing changes are cheap relative to their effect, **moving second is a large advantage in aerospace combat** — considerably larger than in ground play. A fighter that moves after its opponent can often choose an arc the opponent cannot answer.

This is the mechanism behind the light-versus-heavy debate in Chapter 14, and it is why a wingman matters (Chapter 10).

> **PRINCESS CHECK PC-09-01** — severity: **medium** — confidence: **DERIVED**
> **Expect:** Princess prefers firing positions in arcs where the target has no weapons that bear, not merely rear arcs.
> **Look in:** Princess arc/firing-solution scoring for aero targets; `NewtonianAerospacePathRanker` documents arc coverage as a ranking input.
> **Failure looks like:** the bot manoeuvring for the aft arc of a fighter with heavy aft armament in preference to a wing arc with none.

> **PRINCESS CHECK PC-09-02** — severity: **medium** — confidence: **DERIVED**
> **Expect:** Princess manages velocity as a thrust-budget problem, not as a speed-is-good problem — it should decelerate when high velocity is consuming thrust it needs for positioning.
> **Look in:** `AeroPathUtil.generateValidAccelerations()`, `calculateMaxSafeThrust()`; velocity scoring in the rankers.
> **Failure looks like:** bot fighters accelerating to high velocity and then overshooting the engagement repeatedly.

> **PRINCESS CHECK PC-09-03** — severity: **low** — confidence: **VERIFIED**
> **Expect:** Princess avoids exceeding Safe Thrust on turns when it intends to attack, given the +2 penalty on its own fire.
> **Look in:** the interaction between path cost and expected-damage scoring.
> **Failure looks like:** the bot routinely overthrusting into position and then firing at a penalty it did not need to accept.

---

## Chapter 10. Wingmen and pairs

### 10.1 There is no wingman mechanic

**[VERIFIED]** No rule in *Total Warfare*, *Tactical Operations*, or *Strategic Operations* names or models a wingman relationship. Every benefit of flying in pairs is emergent from other rules.

The game's actual first-party abstraction for flight-level operation is the **Fighter Squadron** (*StratOps* pp. 25–32), covered in Part IV.

### 10.2 The four real mechanisms

**Initiative asymmetry.** **[DERIVED]** A lone fighter that loses initiative may be unable to fire at all at close quarters; a pair loses only one of its two. A commonly-cited estimate puts this at roughly 75% effectiveness solo versus 87.5% paired. Those specific figures are one forum poster's unaudited back-of-envelope model — the *direction* is sound, the numbers should not be treated as derived constants.

**Cover during ground attacks.** **[VERIFIED premise, DERIVED conclusion]** A unit that made a ground attack is at **−3 to be hit** (Chapter 4). That is the strongest argument for pairing in the game, and — notably — nobody in the community connects the two. A fighter committing to a run needs a partner precisely because the run itself is what makes it vulnerable.

**Arc-gap coverage for large craft.** **[DERIVED]** Small Craft and DropShips almost always lose initiative to fighters. Paired manoeuvring — overlapping arcs, rotating so that no approach vector is permanently safe — is how large craft deny fighters a free attack line.

**Campaign attrition.** **[DERIVED]** A damaged fighter disengages under a healthier partner's cover. If shot down, the partner knows *where*, which enables search-and-rescue and salvage. Crew survival drives progression to veteran and elite. This is the payoff that matters in MekHQ campaigns and does not show up in a single tactical game at all.

### 10.3 The constraint Chapter 8 adds

**[DERIVED]** **On a ground map, a wingman at a different altitude is not a wingman.** With ×16 dead-zone scaling, one altitude of separation means a 32-hex minimum engagement range — a pair separated vertically cannot support each other at all.

**Pairs fly matched-altitude, horizontally offset.** This is not a preference; it is a precondition for mutual support existing.

### 10.4 Move order

**[DERIVED]** A practical technique from the forums: move the lead of each element first and the wingmen second. This preserves a reactive element in every pair regardless of how the initiative falls.

There is no bot equivalent of this, which means a human flying paired fighters retains an edge Princess does not currently contest.

> **PRINCESS CHECK PC-10-04** — severity: **high** — confidence: **DERIVED**
> **Expect:** the bot exploits move order. Aerospace units move last and in their own turn class, interleaved
> between players (`TWGameManager`), so partway through that block some enemy fighters have committed to an
> altitude this turn and some have not. The bot should commit the fighters whose geometry is already decided
> and hold back those still waiting, and should tell a committed opponent apart from an unmoved one when
> choosing its own altitude.
> **Look in:** `Princess.calculateMoveIndex`; `BasicPathRanker.evaluateAsMoved`.
> **Failure looks like:** the bot matching an enemy fighter's altitude, then watching it move somewhere else —
> with no sign it knew the reading was provisional.
> **Notes:** the stock `evaluateAsMoved` reports **every** airborne aero on a ground map as already moved,
> whether it has moved or not, so the bot cannot tell certainty from a guess. `NewtonianAerospacePathRanker`
> omits that clause and gets it right; the shortcut is `BasicPathRanker`'s alone.

> **PRINCESS CHECK PC-10-01** — severity: **high** — confidence: **DERIVED**
> **Expect:** when Princess commits a fighter to a ground attack, it recognises the **−3** vulnerability and, if a second fighter is available, positions it to cover.
> **Look in:** Princess formation/mutual-support logic — note that `MutualSupportPathRanker.java` and `FormationGeometry.java` exist in the CASPAR-derived code and may be reusable here.
> **Failure looks like:** bot fighters making unescorted ground attacks while a second fighter loiters elsewhere.
> **Notes:** **[DERIVED]** — RFE, not a defect. But a high-value one, and the supporting machinery may already exist.

> **PRINCESS CHECK PC-10-02** — severity: **high** — confidence: **DERIVED**
> **Expect:** on ground boards, Princess keeps paired or grouped fighters at **matched altitude**, since vertical separation makes mutual support impossible under ×16 dead-zone scaling.
> **Look in:** altitude selection in aero path generation; any per-unit altitude heuristic that does not consider friendly fighters' altitudes.
> **Failure looks like:** a bot flight spreading across altitudes 1–6 and fighting as isolated singletons.
> **Notes:** this is likely the highest-impact single change available to bot aerospace play on ground maps.

> **PRINCESS CHECK PC-10-03** — severity: **medium** — confidence: **DERIVED**
> **Expect:** Princess manoeuvres paired large craft (Small Craft, DropShips) so their firing arcs overlap and no approach vector is permanently safe.
> **Look in:** `AeroPathUtil.getSpheroidDir()`; large-craft path ranking.
> **Failure looks like:** two bot DropShips presenting the same arc gap to an attacking fighter.

---

# PART III — Vector Movement

*This is a movement **system**, switched on with an option. It is distinct from space combat, which is a **venue**. Conflating the two is why community discussion of both goes in circles.*

## Chapter 11. What changes

**[MEGAMEK]** Option `advanced_movement`, labelled **"StratOps Advanced Movement"**, described as *"Newtonian physics-style movement"*. **Default off.** Gated internally by `Game.useVectorMove()`.

**[MEGAMEK]** Princess switches to `NewtonianAerospacePathRanker` when `entity.isAero() && game.useVectorMove()`. This is the only condition under which that ranker runs.

**[VERIFIED]** *StratOps* p. 64 onward. The core change: **facing decouples from course.** Velocity is a vector you carry; where your nose points is a separate quantity you control independently.

## Chapter 12. What reorienting costs

**[VERIFIED]** *StratOps* p. 66. The named reorientation maneuvers:

- **Pitch / "End Over"** — flips nose-to-tail; wings end reversed relative to the nose. **2 Thrust** to complete in one turn.
- **Yaw Over** — flips nose-to-tail preserving wing orientation relative to the nose. **2 Thrust** to complete in one turn.

**[VERIFIED]** **The trap:** under the rotational velocity rules you may instead spend **1 Thrust** to begin a rotation and complete it over two turns — but **you must spend the same amount again to stop the rotation.** The cheap option is not cheaper. This resolves a long-running community disagreement about whether reorientation costs 2 or 3 thrust: it is 2, plus a stopping cost if you take the staged route.

**[UNVERIFIED]** The full facing-change cost table — cost per hexside for partial turns, and whether costs differ between fighters, Small Craft, DropShips, and WarShips — could not be located. Do not publish a cost table you cannot cite.

### 12.1 A correction that matters

**Cut this claim wherever you encounter it:** *"Under vector movement the angle-of-attack modifier is based on course, not facing — flying sideways relative to an opponent gives them +2 to hit you, equivalent to a full range bracket."*

**[VERIFIED]** It is not a rule. *TW* p. 236 states that there is **no attacker movement modifier except the +2 for exceeding Safe Thrust**, and **no target movement modifier except the −2 for targeting a unit at velocity 0.**

**[MEGAMEK]** MegaMek implements no course-based or aspect-based to-hit modifier anywhere. All thirty-four `addModifier()` calls in `ComputeAeroAttackerToHitMods` were enumerated; none exists.

**[VERIFIED]** What *is* true, and is the correct version of the instinct people are reaching for: relative geometry determines **which firing arc must bear** and **which armour facing takes the hit**. That is a bearing-and-hit-location question, not a to-hit modifier.

> **PRINCESS CHECK PC-12-01** — severity: **high** — confidence: **VERIFIED**
> **Expect:** `NewtonianAerospacePathRanker` scores positions by *arc coverage and armour facing exposure*, not by any course-derived to-hit modifier — because no such modifier exists.
> **Look in:** `megamek/src/megamek/client/bot/princess/NewtonianAerospacePathRanker.java`.
> **Failure looks like:** a scoring term that rewards or penalises the angle between the bot's course and the enemy's, as though it changed to-hit numbers.
> **Notes:** if such a term exists it is likely modelling something real (arc bearing) in a wrong way. Read carefully before filing.

> **PRINCESS CHECK PC-12-02** — severity: **medium** — confidence: **VERIFIED**
> **Expect:** Princess accounts for the rotational-velocity stopping cost — a staged 1-thrust rotation is not treated as half-price.
> **Look in:** vector-move path generation and cost assignment; `MoveStep` vector turn legality checks.
> **Failure looks like:** the bot beginning rotations it cannot afford to stop and drifting through subsequent turns misaligned.

## Chapter 13. Flying under vector

**[DERIVED]** The framing that matters, borrowed from a forum poster and consistent with the rules: *velocity is not the problem; stopping is the problem.* Acceleration is comparatively cheap and its effects persist. Deceleration must be paid for in full, and it must be paid before you need to have stopped.

**[MEGAMEK]** Map edges under vector play are governed by `return_flyover` (return after `1 + roundup(velocity/4)` turns, +2 if out of control) and `climb_out` (vertical exit at altitude 10, requires `return_flyover`). Both default on.

**[MEGAMEK]** `NewtonianAerospacePathRanker` ranks damage potential, return threat, board-edge proximity, and arc coverage, and self-documents its shortcuts: placeholder logic for unmoved enemies, sensor shadows checked only in adjacent hexes, and line-of-sight checks the author notes are *"probably unnecessary"* on a space map.

**[MEGAMEK]** `Princess.calculateAdvancedAerospaceDeploymentCoords()` uses `NewtonianAerospacePathRanker.willFlyOffBoard()` to avoid deploying somewhere that immediately exits.

> **PRINCESS CHECK PC-13-01** — severity: **high** — confidence: **DERIVED**
> **Expect:** Princess plans deceleration ahead of need — it should not accelerate to a velocity from which it cannot stop or turn within the engagement area.
> **Look in:** `NewtonianAerospacePathRanker` board-edge and return-threat terms; `AeroPathUtil.generateValidAccelerations()`.
> **Failure looks like:** the bot repeatedly flying off the board edge and returning, or orbiting at a velocity that never allows it to engage.
> **Notes:** issue **#7261** reports Princess failing to make progress across a space map in a DropShip even when set to cowardly, which may be an instance of this.

> **PRINCESS CHECK PC-13-02** — severity: **medium** — confidence: **MEGAMEK**
> **Expect:** the documented shortcuts in `NewtonianAerospacePathRanker` — placeholder handling of unmoved enemies, adjacent-hex-only sensor shadow checks — are either resolved or explicitly bounded.
> **Look in:** the ranker's own source comments; they identify the gaps directly.
> **Failure looks like:** systematically poor evaluation of enemies that have not yet moved.

## Chapter 14. Light versus heavy under vector

A fourteen-year argument, resolvable from verified rules.

**[VERIFIED]** Any fighter, regardless of weight, can bring any enemy into its forward firing arc for **2 Thrust** under Advanced Movement (Chapter 12). Weight does not buy agility in the way players expect.

**[VERIFIED]** Aerospace units have **no defensive speed modifier** (Chapter 4). Maneuverability therefore buys position only — and cheap facing changes erase most of the positional payoff.

**[DERIVED]** Together these mean that under vector movement, **light fighters become dependent on initiative**. A light that loses initiative can be brought into a heavy's forward arc for a trivial thrust expenditure and has no defensive modifier to fall back on.

**[DERIVED]** **The dividing line the community has converged on, and which the rules support: atmosphere favours lights; vector movement favours heavies.** Atmosphere has control rolls, terrain, altitude bands, and — critically — the dead zone, which gives a light somewhere free to hide (Chapter 8.4). Vector movement has none of those.

**[DERIVED]** The counter-argument deserves airtime: high thrust still allows a light to control *engagement range* and to drag a slower opponent out of position. The metaphor a forum poster used — a small boxer dragging a heavy boxer around the ring — is apt. But it requires map space to work, and standard MegaMek map sizes rarely provide it.

**[DERIVED]** The unanswered question, stated honestly: a light fighter typically mounts short-range weapons, so it must close; if it then loses initiative, can it disengage far enough to survive? In atmosphere, yes — it dives into a dead zone. Under vector movement in space, there is no equivalent refuge, and the answer is probably no.

> **PRINCESS CHECK PC-14-01** — severity: **medium** — confidence: **DERIVED**
> **Expect:** Princess's aero evaluation does not assume heavier is always better, nor lighter always more evasive; it should value thrust for what it buys positionally rather than defensively.
> **Look in:** unit-value and threat scoring for aero units.
> **Failure looks like:** the bot systematically disengaging from light fighters it should be closing on, or vice versa.

> **PRINCESS CHECK PC-14-02** — severity: **low** — confidence: **DERIVED**
> **Expect:** a path ranker will exploit the "2 thrust into the forward arc" fact automatically, which means **Princess likely advantages heavy fighters in vector play more than a human opponent would.** This is not a defect, but it is a balance consideration for scenario designers and for anyone tuning the bot.
> **Look in:** `NewtonianAerospacePathRanker` arc scoring.
> **Notes:** informational. Worth measuring before tuning anything.

---

# PART IV — Air-to-Ground

*The most-played aerospace content in MegaMek, and the area where Princess is furthest from parity.*

## Chapter 15. The governing idea

**[DERIVED]** The best summary of aerospace ground-attack doctrine anyone has written:

> "99% of the time, aero units are vultures, not eagles. It's better to keep circling and wait until your ground buddies can isolate something than to try for a group and die."

Aircraft in BattleTech are not a breakthrough weapon. They punish isolation, overextension, and damage already inflicted. Attacking into a concentrated force is how they die.

The second half of the doctrine, equally important:

> "It's always better to abort an attack run and try again next turn than to push your luck and get swatted because the conditions weren't right."

**[DERIVED]** Aborting is free. A failed control roll at low altitude is not.

## Chapter 16. The four attacks

### 16.1 To-hit structure

**[VERIFIED]** *TW* p. 243, confirmed against `ComputeAeroAttackerToHitMods.java`:

| Attack | Base to-hit modifier | Altitude band | Altitude lost |
|---|---|---|---|
| **Strike** | **+2** | 1–5 | 1 |
| **Strafe** | **+4** (**+6** at NoE, plus strafing terrain modifiers) | 1–3 | 0 |
| **Dive Bomb** | **+2** | start 3–5 | 2 |
| **Altitude Bomb** | **+2 + current altitude** | any | 0 |

**Correction to received wisdom:** altitude bombing from Altitude 8 is **+10**, not +8. The +2 bombing base is dropped constantly in community discussion.

**[VERIFIED]** The **Golden Goose** SPA (*Campaign Operations* p. 75) gives **−2** on bombing and **−1** on striking.

### 16.2 Why bombs appear to ignore target movement

**[VERIFIED]** No rule exempts dive bombing from the target's movement modifier. **Bombs target a hex, not a unit**, so there is no entity modifier to apply.

**[DERIVED]** This distinction matters because the popular phrasing — "dive bombing ignores TMM" — invites a reader to generalise it. **Strike and Strafe target units and do take target modifiers.** Same outcome for bombing, wrong reason, and the wrong reason produces errors elsewhere.

### 16.3 Scatter

**[MEGAMEK]** Dive bombing and altitude bombing use **separate dedicated procedures**, both cited to *TW* p. 246:

- `Compute.scatterDiveBombs(Coords, int moF)` — dive-bombing scatter, based on Margin of Failure
- `Compute.scatterAltitudeBombs(Coords, int facing, int moF)` — altitude-bombing scatter; note the **`facing` parameter**, meaning altitude-bomb scatter is oriented to the flight path rather than a free direction
- `Compute.scatter(Coords, int)` — the *generic* routine, which rolls 1d6 for one of six straight-line directions

**Correction to received wisdom:** "roll 1d6 for direction, scatter a number of hexes equal to Margin of Failure" describes the **generic** routine, not what bombs use. Do not cross-contaminate these — `scatterAssaultDrop` uses yet another formula (1d6 for direction, 1d6 per point of MoF).

> **PRINCESS CHECK PC-16-01** — severity: **high** — confidence: **MEGAMEK**
> **Expect:** Princess is capable of executing **altitude** bombing.
> **Look in:** Princess ground-attack intent and weapon selection; `IBomber` constants `ALT_BOMB_ATTACK`, `SPACE_BOMB_ATTACK`.
> **Failure looks like:** bot fighters carrying bombs and never dropping them from altitude.
> **Notes:** **Narrowed from v0.1.** Dive bombing is **implemented and works** — `FireControl.getDiveBombPlan()` builds a full payload plan and `guessFullAirToGroundPlan` picks it whenever it beats the gun plan. The wiki's "won't altitude bomb or strafe" is accurate only for those two. What `getDiveBombPlan` does *not* do is choose intelligently: it drops the entire load in one pass, mixing munition types, under its own `TODO: more intelligent bomb drops`, and it does not exclude friendly units from the blast (see PC-16-03).

> **PRINCESS CHECK PC-16-02** — severity: **medium** — confidence: **VERIFIED**
> **Expect:** if bombing is implemented, Princess weighs the **+2 + altitude** cost of altitude bombing against the **+2** of dive bombing and the control-roll risk each carries, rather than treating them as interchangeable.
> **Look in:** attack-type selection in the ground-attack path.
> **Failure looks like:** the bot altitude bombing from altitude 8 (a +10 shot) when a dive bomb was available.

> **PRINCESS CHECK PC-16-03** — severity: **medium** — confidence: **MEGAMEK**
> **Expect:** Princess does not drop bombs on its own units. **This is a live defect** — issue **#8004** reports exactly this.
> **Look in:** bomb target-hex selection; friendly-unit exclusion in blast radius.
> **Failure looks like:** friendly casualties from bot bombing runs.
> **Notes:** confirmed open issue. Highest-confidence actionable item in this chapter.

## Chapter 17. Terrain, masking, and route planning

**[VERIFIED]** The terrain dead zone (Chapter 8.7): if the hex in front of the target is two or more levels higher than the target's own level, the target cannot be attacked.

**[DERIVED]** The practical skills this creates:

**Read the map before planning the run.** A target in a depression is immune from one approach and exposed from another. Which approach works is a property of the terrain, not of the target.

**Approach vector selection is the attack.** Most of the decision-making in a successful ground attack happens before you commit — choosing a flight path that reaches the target from an open side while crossing the fewest enemy firing arcs.

**"Route so the fewest enemies have line of sight" and "abort the run" are the same skill.** Both are recognitions that the geometry either exists or does not.

**[VERIFIED]** Remember also that ground attackers add **2 hexes of range per altitude** of the target (*TW* p. 107), and that a unit that made a ground attack is at **−3 to be hit** for the rest of the turn (Chapter 4).

> **PRINCESS CHECK PC-17-01** — severity: **high** — confidence: **DERIVED**
> **Expect:** Princess's ground-attack flight path minimises the number of enemy units with line of sight and a firing solution, not merely the distance to the target.
> **Look in:** ground-attack path generation and scoring; `PathRanker.java` contains comments indicating aero-on-ground-map handling is heuristic — *"if we are an aero unit on the ground map, we want to discard paths that keep us at altitude 1 with no bombs"*.
> **Failure looks like:** the bot flying the shortest path to the target straight over a concentration of anti-air.

> **PRINCESS CHECK PC-17-02** — severity: **medium** — confidence: **DERIVED**
> **Expect:** Princess is willing to **abort** an attack run — to fly through without attacking when the geometry or risk is unfavourable — rather than always committing once it has approached.
> **Look in:** whether attack commitment is a scored decision or an unconditional consequence of proximity.
> **Failure looks like:** the bot always attacking whenever it is in range, regardless of accumulated risk.

## Chapter 18. Doing it in MegaMek

The mechanics of actually performing these attacks in the client, because they are not obvious.

**[MEGAMEK]** **Bombs are chosen before deployment**, in the unit customisation dialog (HE, Cluster, Laser-Guided, Inferno, Mine, TAG, Arrow, Rocket, Alamo). **Internal and external bomb points are tracked separately** (`getMaxIntBombPoints()` / `getMaxExtBombPoints()`).

**[MEGAMEK]** **Movement phase:** you must fly over the target. Commands are `MOVE_BOMB`, `MOVE_STRAFE`, `MOVE_DUMP` — menu labels "Bomb", "Strafe", "Dump Bombs".

**[MEGAMEK]** **Strafing, in the firing phase:**
1. The fighter must be at **altitude 1–3**; the Strafe button becomes available.
2. Click **Strafe**.
3. Click **1–5 consecutive hexes in a straight line that the aircraft flew over.**
4. Select weapons — **only energy weapons with no ammunition can strafe.**
5. Restrictions: spheroids cannot strafe; airborne units cannot be strafed; infantry in buildings cannot be strafed. Each hex resolves separately against units, buildings, and terrain.

**[MEGAMEK]** **Bombing, in the firing phase:** bombs behave as pseudo-weapons (`F_SPACE_BOMB`, `F_DIVE_BOMB`, `F_ALT_BOMB`), each opening a payload dialog. Altitude bombing is limited to **2 per attack**; internal bomb bays are capped at **6 per turn**.

**[MEGAMEK]** **The asymmetry that confuses everyone:** VTOLs and LAMs in AirMek mode declare their bomb target hex during the **movement** phase. Aerospace fighters declare during the **firing** phase.

### 18.1 A known divergence from the rules

**[MEGAMEK]** **Issue #7620** (open, severity High) — *"AeroSpace Units declare Ground Attacks in the Wrong Phase."* *TW* p. 242 requires declaration during the aerospace movement phase; MegaMek declares in the firing phase.

Reported consequences:
- visual-range problems
- air-to-air attacks invalidated when the target declares a ground attack
- range miscalculation, because altitude changes after declaration

**[DERIVED]** This compounds with issue #332 (Chapter 8.9). If both are live, bot air-to-air performance against a ground-attacking opponent may be substantially degraded through no fault of the bot's own logic.

**[MEGAMEK]** **Version note:** altitude bombing's to-hit was missing the base +2 until PR #7270 (June 2025), which also added bombing from an atmospheric board onto an embedded ground board. Tactical advice written against older builds is suspect.

> **PRINCESS CHECK PC-18-01** — severity: **high** — confidence: **MEGAMEK**
> **Expect:** Princess's ground-attack declaration works correctly given issue #7620's phase divergence — and does not compound it.
> **Look in:** where the bot commits to a ground attack relative to when it plans its movement.
> **Failure looks like:** the bot planning a run in the movement phase whose preconditions no longer hold when the firing phase arrives.
> **Notes:** the underlying issue is an **engine** defect. Fixing #7620 may change bot behaviour; check both before and after.

> **PRINCESS CHECK PC-18-02** — severity: **medium** — confidence: **MEGAMEK**
> **Expect:** Princess respects the strafing constraints — altitude 1–3, 1–5 consecutive hexes in a straight line along the flown path, energy weapons without ammunition only.
> **Look in:** strafing support in the bot, if any.
> **Failure looks like:** absent entirely. **Known unimplemented.**

> **PRINCESS CHECK PC-18-03** — severity: **low** — confidence: **MEGAMEK**
> **Expect:** Princess can jettison bombs it cannot use, rather than carrying the thrust penalty for the whole game.
> **Look in:** `MOVE_DUMP` availability to the bot.
> **Failure looks like:** a bot fighter flying at reduced thrust all game with an undropped payload. Issue **#6392** is an open RFE for exactly this.

## Chapter 19. High or low?

The community has argued this for years without resolution. It resolves cleanly.

**[DERIVED]** **Low altitude is for killing armour.** Dive bombing and striking put damage where you aim it. The price is the control-roll risk from Chapter 6 and exposure to anti-air. The mitigation is cheap, disposable airframes — a bomber that costs 64 BV can be spent.

**[DERIVED]** **High altitude is for utility ordnance.** Inferno, FASCAM, area denial — payloads where scatter matters less because the effect is area-based anyway. A failed control roll at altitude 8 cannot put you on the deck.

**[DERIVED]** **The number that actually decides it** is neither of those: a unit that made a ground attack is at **−3 to be hit**. Exposure is the price of *attacking*, not of being *low*. Choosing altitude is choosing your accuracy-versus-control-risk trade, not your survivability.

**[DERIVED]** **And whichever altitude you choose, your escort has to be there with you** (Chapter 8.3). An escort three altitudes above your bombers is not escorting anything.

## Chapter 20. Loadouts and survivability

### 20.1 Munitions

**[DERIVED]** Community consensus, uncited but internally consistent:

| Munition | Note |
|---|---|
| **HE** | The default. Meant to be dropped in salvo, not singly — a ten-bomb load delivers roughly 100 damage per sortie |
| **Cluster** | Pairs with high-altitude bombing; scatter hurts less when the payload is area-effect |
| **Inferno** | 10 heat, equivalent to 5 Inferno missiles |
| **FASCAM** | Seeds 7 hexes with 20-point minefields. **Density does not stack** |
| **Fuel-Air (small)** | Extremely effective against assault 'Mechs, and a recurring source of table friction |
| **Laser-guided / homing Arrow IV** | Better precision, reduces the need for a follow-up attack |

**[DERIVED]** **The TAG delivery trick:** load a very cheap conventional fighter with a drone operating system and TAG, and use it as a disposable spotter for air-launched homing Arrow IV.

### 20.2 Thrust under load

**[DERIVED]** The real design constraint on a bomber is not payload capacity but what payload does to thrust. Representative figures from community analysis:

| Unit | Clean | Loaded | Bombs | Verdict |
|---|---|---|---|---|
| Boeing Jump Bomber | — | 8/12 | 4 | Fast enough to penetrate contested airspace; disposable by design |
| Drake (35t) | 6/9 | 4/6 | 7 | Can defend itself after the drop |
| 'Mechbuster | 5/8 | **3/5** | 10 | Lacks the thrust to carry bombs into contested airspace |

**[UNVERIFIED]** A recurring community claim holds that a destroyed bomber still delivers its bombs. **No rule, errata entry, or official ruling was found in either direction.** Do not build tactics on it; it deserves a Rules Questions post.

### 20.3 Surviving anti-air

**[DERIVED]** LB-X autocannon are widely regarded as the best anti-aircraft weapon in the game. Two counters are debated and neither is settled:

- **Ferro-Lamellar** zeroes single pellets, so no damage means no critical roll and no through-armour critical.
- **Heavy Ferro-Fibrous** is the better weight buy on conventional fighters, because they rarely reach even Damage Threshold 3 and 1-point pellets cannot penetrate regardless.

**[DERIVED]** **Build to damage-threshold breakpoints: 2, 3, 5, 7, 8, 10, 15.** This is the most directly actionable design number in the community's literature.

**[VERIFIED]** **Vehicular Stealth Armor** (*TacOps* p. 282 / *TO:AU&E* p. 94) can be mounted on **both conventional and aerospace fighters**. It requires a functioning ECM suite and gives **+1 at medium range, +2 at long and extreme**. It provides **no benefit while expending Thrust above the space/atmosphere interface** — so it works in atmosphere, works coasting in space, and does nothing under thrust in vacuum.

**[DERIVED]** **Chaff Pods** impose a disposable +1 to hit on attackers — effectively free ECM, and underused. **AMS** is near-useless on a fighter, being good only against SRMs when most threats are energy weapons.

**[MEGAMEK]** **Altitude is not cover from ground fire** (Chapter 8.8). Ground units have no dead zone.

> **PRINCESS CHECK PC-20-01** — severity: **medium** — confidence: **DERIVED**
> **Expect:** Princess accounts for the thrust penalty of a bomb load when planning routes and when deciding whether a fighter can escape after its run.
> **Look in:** `IBomber` load state; `calculateMaxSafeThrust()` and path generation.
> **Failure looks like:** the bot routing a loaded 'Mechbuster at 3/5 as though it still had 5/8.

> **PRINCESS CHECK PC-20-02** — severity: **low** — confidence: **VERIFIED**
> **Expect:** Princess does not attribute stealth benefit to a fighter expending thrust above the space/atmosphere interface.
> **Look in:** stealth-armour handling in threat and to-hit estimation for aero units.
> **Failure looks like:** the bot over-valuing a stealth fighter's survivability in space.

---

# PART V — Princess

## Chapter 21. Flying against the bot, and auditing it

### 21.1 What Princess currently does

**[MEGAMEK]** `AeroPathUtil.java` provides the aerospace-specific machinery:

| Method | Purpose |
|---|---|
| `generateValidAccelerations()` | legal velocity changes |
| `calculateMaxSafeThrust()` | *"the lowest of safe thrust and structural integrity"* |
| `willStall()` | detects a path ending at zero velocity |
| `willCrash()` | detects altitude below 1 without landing |
| `isSafePathOffBoard()` | board-exit safety |
| `generateValidAltitudeChanges()` | caps at 10, floors at 1 |
| `getSpheroidDir()` | points DropShips at the enemy centroid, rotates damaged sections away |

**The emphasis throughout is on avoiding stalls, crashes, and control rolls — not on offence.**

**[MEGAMEK] The single most consequential fact about bot aerospace, added in v0.2.** Altitude is never a
*choice*. Every `getFinalAltitude()` reference in `megamek/src/megamek/client/bot/` is a legality filter —
"will I crash", "am I too low to strike", "do I have bombs" — and none of them is a preference. The two
venues then fail in different ways:

| | Path generation | Path ranking |
|---|---|---|
| **Ground map** | `AeroGroundPathFinder` drives *every* generated path to `OPTIMAL_STRIKE_ALTITUDE = 5`. Its author left the intent beside the call: *"repeat with 1, 3, 7 when we settle things down?"* | no altitude term |
| **Low altitude** | `AeroLowAltitudePathFinder` correctly generates altitude variants and dedupes per altitude | no altitude term |

So on a ground map the ranker is never *offered* an alternative altitude, and at low altitude it is offered
one but has nothing to say about it. Combined with the ×16 dead zone (Chapter 8), a bot flight that deploys
at the wrong altitude on a ground map cannot fight and cannot work out why.

**[MEGAMEK]** `NewtonianAerospacePathRanker.java` runs **only** when `entity.isAero() && game.useVectorMove()`. It ranks damage potential, return threat, board-edge proximity, and arc coverage.

**[MEGAMEK]** `PathRanker.java` shows that ground-map aero handling is heuristic, with comments including *"if we are an aero unit on the ground map, we want to discard paths that keep us at altitude 1 with no bombs"*, *"Skip this part if I'm an aero on the ground map, as it's kind of irrelevant"*, and *"Skip airborne aero units as they're further away than they seem and hard to catch."*

### 21.2 What it does not do

**[MEGAMEK]** From MegaMek's own wiki, "What Can Princess (the Bot) Currently Do?":

- **"Aerospace fighters won't altitude bomb or strafe."**
- **"Ineptitude with manually guided ordnance in space."**
- Short-term-only planning is described as *"her fatal flaw."*

**[MEGAMEK] — correction to v0.1.** Read that first line narrowly. **Dive bombing is implemented**
(`FireControl.getDiveBombPlan`). Altitude bombing and strafing are the genuinely missing pair.

**[DERIVED]** **The practical consequence for players:** if you want an air-to-ground threat in a scenario, you must fly it yourself or script it. Princess will manoeuvre and shoot air-to-air; it will not conduct strafing runs or bombing.

### 21.3 Known open aerospace issues

**[MEGAMEK]** Relevant to bot behaviour specifically:

| Issue | Summary |
|---|---|
| **#5952** | "ASF/DS Low Atmo Insanity" |
| **#6334** | Airborne DropShips not engaging ground forces |
| **#7261** | Princess won't cross a space map in a DropShip even when set to cowardly |
| **#8004** | Princess bombing her own units |
| **#7443** | RFE: use both level and dive bombing |
| **#6392** | RFE: let ASF Princess jettison bombs |
| **#5400** | Princess generating ghost targets with naval ECM |

Engine-side issues that shape bot behaviour without being bot defects: **#7620** (ground attacks declared in the wrong phase), **#332** (dive-to-escape deletes incoming attacks), **#2457** (double-blind spotting with ASFs), **#409** (no dead-zone overlay).

### 21.4 The highest-value checks

**[DERIVED]** If the audit has limited time, these are the ones that matter, in order:

1. **PC-08-02** — altitude matching for air-to-air on ground boards. The ×16 dead-zone scaling means a bot flight spread across altitudes cannot fight at all. Easy to reproduce, potentially decisive.
2. **PC-10-02** — matched altitude within a flight, for the same reason.
3. **PC-04-02** — exploiting the −3 modifier against units that made ground attacks.
4. **PC-16-03** — bombing friendly units (#8004), a confirmed live defect.
5. **PC-08-01** — never manoeuvring into a position from which no legal shot exists.
6. **PC-06-01** — grading control-roll risk by current altitude rather than treating it as binary.

### 21.5 A note on CASPAR

**[MEGAMEK]** The CASPAR wiki page sits under deprecated content and states the initiative is *"ON INDEFINITE HOLD"*, with its training dataset scoped to **"ground combat games only."** Meanwhile the code is active in master — headless batch running, mutual-support doctrine, combat posture, position discipline, and CASPAR selectable in the Edit Bots dialog.

**[MEGAMEK] — resolved.** As of v0.1 CASPAR's only ranker override was the `Basic` slot
(`MutualSupportPathRanker`), and `Princess.getPathRanker(Entity)` sent aerospace units to exactly two places:
`NewtonianAerospace` under vector movement, and otherwise the `Basic` fallback. So vector-move aero ran pure
Princess, while **atmospheric aero was being flown by a ground formation doctrine** — units-in-a-lance
cohesion applied to fighters. Not a fallback so much as an accident of dispatch order.

**[MEGAMEK]** That is now addressed: a dedicated `PathRankerType.Aerospace` slot sits ahead of the `Basic`
fallback, Princess registers its existing ranker there (so nothing about Princess changes), and CASPAR
registers an `AerospacePathRanker`. Vector movement and space are untouched.

**[DERIVED]** The original observation still stands for the parts not yet built: `FormationGeometry.java` is
close to what PC-10-01 and PC-10-02 want for escort and matched-altitude pairing.

> **PRINCESS CHECK PC-21-01** — **RESOLVED** (was: medium / UNVERIFIED)
> **Resolution:** CASPAR now handles atmospheric aerospace explicitly through its own ranker, fire control and
> ground-map path finder. Princess is unchanged and remains the control arm. ID not reused.

---

# PART VI — Reference

## Appendix A — Dead zone quick reference

**Air-to-air altitude dead zone** *(TW p. 241)*

| Altitude difference | Minimum horizontal range | On a ground mapsheet | Range penalty | Hit location column |
|---|---|---|---|---|
| 0 | none | none | +0 | directional |
| 1 | 2 hexes | **17 hexes** | +1 | directional |
| 2 | 3 hexes | **33 hexes** | +2 | directional |
| 3 | 4 hexes | **49 hexes** | +3 | **Above/Below** |
| 4 | 5 hexes | **65 hexes** | +4 | **Above/Below** |
| 5 | 6 hexes | **81 hexes** | +5 | **Above/Below** |
| 6 | 7 hexes | **97 hexes** | +6 | **Above/Below** |

The ground-mapsheet column is `16n + 1`, not `16(n + 1)`: the engine rounds the ground distance up to
low-altitude hexes before comparing (Chapter 8.3).

**Exceptions**
- **Spheroids** may fire into their own dead zone: Nose weapons at higher targets, Aft weapons at lower.
- **No dead zone in space.**
- **No dead zone for ground-to-air** — but ground units cannot target above altitude 8.

**Terrain dead zone (air-to-ground)**
If the hex in front of the target is 2 or more levels higher than the target's level, the target cannot be attacked. Directional — relative to the attacker's flight path.

## Appendix B — Modifier quick reference

| Modifier | Value |
|---|---|
| Attacker exceeded Safe Thrust | +2 (own attacks) |
| Attacker at NoE | +2 (+1 OmniFighter) |
| Attacker Out of Control | +2 (own attacks) |
| Target at velocity 0 | −2 |
| **Target made a ground attack this turn** | **−3** |
| Non-aero airborne target | +5 |
| Large craft Evasive Action | +2 (own attacks) |
| ECHO maneuver | +1 / +2 |
| Strike | +2 |
| Strafe | +4 (+6 NoE) |
| Dive Bomb | +2 |
| Altitude Bomb | +2 + altitude |
| Golden Goose SPA | −2 bombing / −1 striking |
| Ground-to-air range | +2 hexes per altitude of target |

**There is no target movement modifier in aerospace combat.**

## Appendix C — Open questions to resolve before acting

These gate specific checks. Resolve them first.

| # | Question | Settles | Status |
|---|---|---|---|
| 1 | The exact predicate in `Compute.inDeadZone()` | PC-08-01, PC-08-02 | **CLOSED.** `altDiff >= horizontal range in low-altitude hexes`; ground hexes converted with `ceil(d/16)`. See 8.10 |
| 2 | Is altitude difference ever wired to `HIT_ABOVE` / `HIT_BELOW`? | PC-08-04 | **CLOSED.** Yes — `ComputeTerrainMods.java:348-360`, correct threshold and spheroid exception. Check retired |
| 3 | Terrain-masking boundary: `> 2` in code vs. "two or more levels" in the rule | PC-08-08 | **OPEN.** `rg -n "prevElev" megamek/src/` and compare against *TW* p. 243. Note the rule is NoE-only (8.7) |
| 4 | *TW* page for the 2-thrust climb / free dive | PC-03-01 | OPEN. Book |
| 5 | Full vector facing-change cost table | PC-12-02 | OPEN. *StratOps* p. 64 ff. |
| 6 | Does a destroyed bomber still deliver its bombs? | Ch. 20.2 | OPEN. Official Rules Questions post |
| 7 | Does `variable_damage_thresh` correspond to a published StratOps rule? | PC-03-03 | OPEN. Book |
| 8 | Does CASPAR handle aerospace at all? | PC-21-01 | **CLOSED.** It did not; atmospheric aero fell through to the ground-unit ranker. It does now. See 21.5 |

## Appendix D — Check index

| ID | Chapter | Severity | Confidence | Subject |
|---|---|---|---|---|
| PC-01-01 | 1 | high | VERIFIED | ×16 range scaling on ground boards |
| PC-02-01 | 2 | medium | MEGAMEK | Behaviour conditioned on game options |
| PC-02-02 | 2 | medium | MEGAMEK | Climb-out as a disengagement option |
| PC-03-01 | 3 | medium | MEGAMEK | Climb/dive cost asymmetry |
| PC-03-02 | 3 | medium | VERIFIED | Weapon selection vs. Damage Threshold |
| PC-03-03 | 3 | low | MEGAMEK | Variable damage threshold awareness |
| PC-04-01 | 4 | high | VERIFIED | No defensive speed modifier |
| PC-04-02 | 4 | high | VERIFIED | Exploiting the −3 ground-attack modifier |
| PC-04-03 | 4 | medium | VERIFIED | Evasive Action usage |
| PC-05-01 | 5 | high | VERIFIED | Legal altitude for intended attack type |
| PC-05-02 | 5 | medium | VERIFIED | Altitude lost by the attack |
| PC-06-01 | 6 | high | VERIFIED | Control-roll risk graded by altitude |
| PC-06-02 | 6 | medium | VERIFIED | `atmospheric_control` awareness |
| PC-06-03 | 6 | medium | VERIFIED | Out-of-Control handling |
| PC-07-01 | 7 | medium | MEGAMEK | Special maneuvers generated at all |
| PC-07-02 | 7 | medium | DERIVED | Half-roll for armour management |
| PC-07-03 | 7 | low | DERIVED | Hammerhead only at low velocity |
| PC-08-01 | 8 | **critical** | VERIFIED | Never manoeuvre into own dead zone |
| PC-08-02 | 8 | **critical** | DERIVED | Altitude matching on ground boards |
| PC-08-03 | 8 | high | DERIVED | Dead zone as cover |
| PC-08-04 | 8 | ~~medium~~ | **RETIRED** | Above/Below hit column - implemented correctly |
| PC-08-05 | 8 | medium | VERIFIED | Spheroid vertical-arc exploitation |
| PC-08-06 | 8 | medium | DERIVED | Do not hide in a spheroid's arc |
| PC-08-07 | 8 | high | VERIFIED | Terrain masking on approach |
| PC-08-08 | 8 | medium | UNVERIFIED | Terrain masking boundary |
| PC-08-09 | 8 | high | MEGAMEK | Issue #332 attack deletion |
| PC-09-01 | 9 | medium | DERIVED | Prefer arcs with no return fire |
| PC-09-02 | 9 | medium | DERIVED | Velocity as thrust budget |
| PC-09-03 | 9 | low | VERIFIED | Avoid overthrust on attack turns |
| PC-10-01 | 10 | high | DERIVED | Cover fighters making ground attacks |
| PC-10-02 | 10 | high | DERIVED | Matched altitude within a flight |
| PC-10-03 | 10 | medium | DERIVED | Large-craft arc overlap |
| PC-10-04 | 10 | high | DERIVED | Move order and reaction to committed enemies |
| PC-12-01 | 12 | high | VERIFIED | No course-based to-hit modifier |
| PC-12-02 | 12 | medium | VERIFIED | Rotational velocity stopping cost |
| PC-13-01 | 13 | high | DERIVED | Plan deceleration ahead of need |
| PC-13-02 | 13 | medium | MEGAMEK | Documented ranker shortcuts |
| PC-14-01 | 14 | medium | DERIVED | Weight-class valuation |
| PC-14-02 | 14 | low | DERIVED | Ranker bias toward heavies |
| PC-16-01 | 16 | high | MEGAMEK | Bombing implemented at all |
| PC-16-02 | 16 | medium | VERIFIED | Attack-type selection by modifier |
| PC-16-03 | 16 | medium | MEGAMEK | **Bombing friendlies (#8004)** |
| PC-17-01 | 17 | high | DERIVED | Route to minimise exposure |
| PC-17-02 | 17 | medium | DERIVED | Willingness to abort a run |
| PC-18-01 | 18 | high | MEGAMEK | Declaration-phase divergence (#7620) |
| PC-18-02 | 18 | medium | MEGAMEK | Strafing constraints |
| PC-18-03 | 18 | low | MEGAMEK | Bomb jettison (#6392) |
| PC-20-01 | 20 | medium | DERIVED | Thrust penalty of bomb load |
| PC-20-02 | 20 | low | VERIFIED | Stealth armour under thrust |
| PC-21-01 | 21 | ~~medium~~ | **RESOLVED** | CASPAR aerospace handling |

**50 checks, of which 48 are live** (PC-08-04 retired, PC-21-01 resolved).
By severity: **2 critical · 16 high · 24 medium · 6 low**
By confidence: **18 VERIFIED · 12 MEGAMEK · 17 DERIVED · 1 UNVERIFIED**

**30 live checks (VERIFIED + MEGAMEK) are safe to file bugs against. 18 (DERIVED + UNVERIFIED) are not.**

**Closed since v0.1:** PC-08-04 (implemented all along), PC-21-01 (CASPAR now handles atmospheric aerospace).
**Added:** PC-10-04 (move order and reaction).

**Reminder:** DERIVED and UNVERIFIED checks are **not** bug reports. File them as RFEs or discussion items, or resolve the underlying question first.

## Appendix E — Things the community gets wrong

1. Strike, Strafe, and Dive Bomb do **not** share one altitude band — and dive bombing has a **floor** of 3
2. Altitude bombing is **+2 + altitude**, not just altitude
3. There is **no course-based to-hit modifier** under vector movement
4. Crashing is **not** automatic destruction
5. Advanced Atmospheric Control Rolls change **when you roll**, not how far you fall
6. Aerospace units have **no** speed-based defensive modifier
7. Rotational velocity costs the same to **stop** as to start
8. Aerospace fighters **can** mount Vehicular Stealth Armor
9. **You cannot attack the unit directly below you** — and on a ground map, "directly below" means anything within 32 hexes at one altitude of separation
10. **Altitude is not cover from ground fire** — ground units have no dead zone
11. Bomb scatter uses **dedicated dive and altitude procedures**, not the generic 1d6 routine

## Appendix F — Chapters not yet written

Reserved from the full outline, with numbering held:

- **Part IV extension:** Space Combat — setting up, fighter combat in space, **Fighter Squadrons** (*StratOps* pp. 25–32), large craft
- **Part VI extension:** Roles that mean something (the can-opener / vulture taxonomy); Doctrine at the operational level; Campaign and MekHQ

The Squadron chapter is the most significant gap. Fighter Squadrons are the game's first-party abstraction for flight-level operation, MegaMek implements them (`FighterSquadron.java`, gated behind `stratops_capital_fighter`), and they carry their own open defects (#3321, #3086, #2942). They warrant their own set of PRINCESS CHECK blocks.

## Appendix G — Sources

**Rules:** *Total Warfare* (errata v11.01, 2023-09-17) · *TechManual* · *Tactical Operations* / *TO:AU&E* · *Strategic Operations: Advanced Aerospace Rules* (errata v5.0, 2024-11-15) · *Campaign Operations*

**Rulings:** Xotl and moderator rulings on the official BattleTech forums, notably topic 44725 (dead zone ×16 scaling), topic 12373 (damage threshold), topic 22006 (criticals and threshold), topic 55183 (clusters vs. aerospace), topic 42955 (control rolls), topic 58857 (aerospace clarifications), topic 83500 (squadrons)

**MegaMek source:** `OptionsConstants.java` · `GameOptions.java` · `messages.properties` · `Compute.java` · `ComputeAeroAttackerToHitMods.java` · `ComputeTargetToHitMods.java` · `ComputeToHitIsImpossible.java` · `MMConstants.java` · `MoveStep.java` · `BoardType.java` · `Board.java` · `BoardHelper.java` · `Aero.java` · `ToHitData.java` · `FighterSquadron.java` · `AeroPathUtil.java` · `NewtonianAerospacePathRanker.java` · `PathRanker.java`

**MegaMek wiki:** Strafing · What Can Princess (the Bot) Currently Do? · CASPAR Tactical Evolution Initiative

**Community:** seven aerospace tactics threads on the official forums (2012–2024), and the *Dicta Coburn* (Trace Coburn, 2005/2011)

---

*End of draft v0.1.*



