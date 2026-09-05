# TacOps Climbing — Behavior Matrix

Reference for what the Climb Mode button (`Climbing` / `Move Thru`) does in
combination with the **TacOps Climbing** and **TacOps Leaping** game options.

Source-of-truth files for each behavior described below are cited inline so this
doc can be re-verified after future merges.

---

## Variables

- **TacOps Climbing** — game option `ADVANCED_GROUND_MOVEMENT_TAC_OPS_CLIMBING`
  (`OptionsConstants`). Enables the per-level climb PSR system from TO:AR p.20.
- **TacOps Leaping** — game option `ADVANCED_GROUND_MOVEMENT_TAC_OPS_LEAPING`.
  Enables the leap-from-edge mechanic (4 MP, leg + fall PSRs).
- **Climb Mode** — per-unit toggle (button `MOVE_CLIMB_MODE`). Two states:
  - **Climbing** (mode ON) — prefer to climb / go on top.
  - **Move Thru** (mode OFF) — prefer to walk through / go under.

---

## Action vocabulary

| Term | Meaning |
|---|---|
| **Climbing UP** | Stepping into an adjacent hex 3+ levels higher (cliff face or building roof). |
| **Edge step-off** | Stepping into an adjacent hex 3+ levels lower from a cliff top or building roof. |
| **Walking 1-2 levels** | Normal terrain step within `entity.getMaxElevationChange()`. Not affected by these rules. |
| **Jumping** | `MOVE_JUMP` path. Never engages the climbing system. |

---

## Configuration A — Climbing **ON** + Leaping **ON** (recommended for full TacOps play)

| Scenario | Climb Mode ON ("Climbing") | Climb Mode OFF ("Move Thru") |
|---|---|---|
| **Climbing UP** a 3+ cliff/building | Climb dialog opens: `Climb Up N levels` (PSR per level, multi-turn supported). | Step is illegal. Path planner routes around. *(`MoveStep:3091` — climbing requires both option AND `climbMode`.)* |
| **Edge step-off** to 3+ lower hex | Edge dialog opens: **Dangle / Climb Down / Drop / Cancel**. | **Edge dialog also opens.** Same options. *(`MovementDisplay:1124` — gate uses `canClimb`, not `climbMode`. Per TO:AR, players shouldn't need to toggle climb mode to descend.)* |
| **Walking onto a low building (≤2 floors)** | Goes on top of the building (roof). | Goes through walls (takes interior CF damage). |
| **Crossing a bridge** | Goes on top of the bridge. | Goes under the bridge (height permitting). |
| **Jumping** (any direction) | Normal jump. Climb mode ignored. *(`MovementDisplay:1106` — `isWalkingNotJumping`, and `MovePathHandler:213` — server-side `isJumpPath` exclusion.)* | Same — normal jump. |

---

## Configuration B — Climbing **ON** + Leaping **OFF**

Same as A *except*:

| Scenario | Difference |
|---|---|
| **Edge step-off** | Edge dialog shows **Dangle / Climb Down / Cancel** only — no Drop. *(`MovementDisplay:1170` — Drop option is gated on TacOps Leaping.)* |
| Internally | The leap-MP cost branch (`MoveStep:3069`, mp=4) is skipped. Climbing handles descents. |

---

## Configuration C — Climbing **OFF** + Leaping **ON**

| Scenario | Climb Mode ON ("Climbing") | Climb Mode OFF ("Move Thru") |
|---|---|---|
| **Climbing UP** a 3+ cliff/building | Step is illegal. **No dialog.** *(TacOps Climbing option off → `canTacOpsClimb` false at all gates.)* | Same — illegal, no dialog. |
| **Edge step-off** to 3+ lower hex | Treated as a leap. **Leap warning** confirms PSR risk. *(`MovementDisplay:1269` — leap warning gated on TacOps Leaping.)* | Same — leap warning. |
| **Walking onto a low building** | Roof. | Through walls. |
| **Crossing a bridge** | On top. | Under. |
| **Jumping** | Normal. | Normal. |

The TacOps Climbing dialog never appears. Climb mode reverts to its pre-TacOps role: building/bridge interaction toggle.

---

## Configuration D — Climbing **OFF** + Leaping **OFF** (vanilla BattleTech)

| Scenario | Climb Mode ON | Climb Mode OFF |
|---|---|---|
| **Climbing UP** a 3+ cliff/building | Illegal. Path routes around. | Same. |
| **Edge step-off** to 3+ lower hex | **Illegal.** No leap rule, no climb rule. Path routes around. **No dialog of any kind.** | Same. |
| **Walking onto a low building** | Roof. | Through walls. |
| **Crossing a bridge** | On top. | Under. |
| **Jumping** | Normal. | Normal. |

Player must use jump, route around, or stay put for cliffs/tall buildings. Climb mode is purely the legacy building/bridge toggle.

---

## Climb Mode button label

Post fix, the button suffix updates based on what the unit is facing:

| Adjacent terrain | Suffix when ON | Suffix when OFF |
|---|---|---|
| 3+ level cliff up | `Climbing — Up Cliff` | `Move Thru — Up Cliff` |
| 3+ level building | `Climbing — Up Building` | `Move Thru — Up Building` |
| 3+ level drop ahead (cliff edge) | `Climbing — At Edge` | `Move Thru — At Edge` |
| Small building adjacent or here | `Climbing — Building` | `Move Thru — Building` |
| Bridge in current/adjacent hex | `Climbing — Bridge` | `Move Thru — Bridge` |
| None of the above | `Climbing` | `Move Thru` |

Tooltip on hover explains the active effect (e.g., *"At Edge — Climb Mode toggle has no effect for descent — Dangle/Drop/Cancel dialog appears either way."*).

---

## Single-line summary

| Climbing | Leaping | Behavior |
|---|---|---|
| ON | ON | Full TacOps. All descent options available (Dangle / Climb Down / Drop). |
| ON | OFF | Climbing works. Descent is Dangle / Climb Down only (no Drop). |
| OFF | ON | No climbing rule. Cliff edges become standard leaps with the leap warning. |
| OFF | OFF | Vanilla BattleTech. Cliffs and tall buildings impassable except via jump or routing around. |

---

## Edge dialog option gating (Configuration A or B)

Within the edge dialog, individual options are conditioned on:

| Option | Visible when… |
|---|---|
| **Dangle Down** | Unit has 2 functional climbing arms (`canDangle`). *(`MovementDisplay:1137`)* |
| **Climb Down N** | Unit has ≥1 functional climbing arm (`canClimb`) and walk MP ≥ N × cost-per-level. *(`MovementDisplay:1158`)* |
| **Drop** | TacOps Leaping enabled AND walk MP ≥ 4 (`DROP_MP_COST`). *(`MovementDisplay:1170`)* |
| **Cancel** | Always. |

Climb cost per level: **2 MP** with two functional climbing arms, **3 MP** with one *(`ClimbingHelper.MP_COST_TWO_HANDS` / `MP_COST_ONE_HAND`)*.

A "functional climbing arm" requires all four actuators intact (shoulder, upper arm, lower arm, hand), free hand (no carried object), and **no club/hatchet/sword** mounted in that arm. A Mek variant with a sword loses one arm's climbability.

---

## PSR targets

For climbing UP and Climb Down:

```
PSR target = base piloting + 1 (climbing modifier)
                          + 2 (only if one functional arm)
```

Drop (leap from edge):

```
Leg damage PSR  = base + 2 × dropHeight
Fall PSR        = base +     dropHeight
```

Drop from a dangle position reduces both modifiers by `DANGLE_LEVELS_PER_TURN` (= 2), reflecting the lower effective drop height.

---

## What to test if these checks change

If anything in `MoveStep.compileIllegal`, `MoveStep.isMovementPossible`, or
`MovePathHandler.processMovement` is touched, re-verify:

1. `TacOpsClimbingMovementTest` — the regression suite.
2. Manual smoke tests:
   - Walk-only Mek (no jump jets) at the base of a 4+ level cliff with all 4 option combinations.
   - Same Mek standing on top of a 5+ level building, edge step-off with all 4 combinations.
   - Jump unit jumping off a cliff top with both climbing/leaping options (must NOT trigger climbing dialog).
3. Toast / leap-warning behavior — picking Climb Down or Dangle should not surface the leap warning afterward.
