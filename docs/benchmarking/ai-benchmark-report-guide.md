# AI Benchmark Report — format guide

Companion to `ai-benchmark-report-template.html`.

This describes how we report headless bot-vs-bot benchmark results, and *why* the format is shaped
the way it is. It came out of the Princess Mutual Support evaluation (2026-08-02, 197 games), where
three separate measurement traps each nearly produced a confidently wrong answer. The structure
below exists to make those traps visible rather than to look tidy.

Use it for any AI behaviour change measured with `AIMatchRunner`.

---

## The one rule

**A benchmark report's job is to be believable, not to be impressive.** A reader should be able to
tell, without re-running anything, whether the numbers mean what the headline claims. Every section
below serves that.

Corollary: report the metrics that did *not* move as prominently as the ones that did. The Mutual
Support run improved four measures and failed two; burying the two would have produced a "success"
that fell apart the first time someone played against it.

---

## Required sections

### 1. Summary in one page

The verdict, up top, in plain words — including what failed. A table of *what we hoped for* against
*what happened*, then a one-sentence explanation of why, then the recommendation.

If a reader stops after this section they should still have the correct impression.

### 2. Scorecard

One row per goal the change was meant to achieve, each with a status chip — **Confirmed / Partial /
Not met** — the headline figure, and one line of interpretation.

Status is a judgement, so make it a visible one. Encoding it as a chip stops a reader from having to
infer success from a number whose good direction they may not know.

### 3. Head to head

Win rate with **confidence intervals**, for the change arm *and the control arm*, pooled across
side-swapped variants.

Never report a win rate without its interval. In the Mutual Support run a single variant showed a
76/24 split from *identical code on both sides* — pure map bias. The interval and the control are
what stop that becoming a claimed result.

### 4. Behaviour metrics

The measures that describe *how* the bot fought, before and after, with the control bot's numbers in
the same table.

**Precede this with plain-language definitions.** Metric names like `arrival_stagger` are invented
jargon; the definition should say what it measures, *what question it is asking*, and which
direction is good. Put the definitions next to the numbers, not in an appendix.

### 5. What it looks like — the battle map

A replay of one representative game per arm, side by side, scrubbable by round, drawn on the real
board terrain.

Percentages tell you a formation loosened; the map shows you what that means. Pick the game whose
key metric is closest to the arm's **median** — a representative battle, not a flattering one — and
say in the caption that you did.

What the map draws, and why each part earns its place:

- **Real terrain from the `.board` file.** `extract_board.py` parses the board MegaMek actually
  played on, so woods, water and elevation are the genuine article. This matters more than it
  sounds: a move that looks irrational on a blank grid usually turns out to be a company filtering
  around a wood line or refusing a water crossing. Without terrain you will misread the behaviour.
- **True flat-top hexes in offset columns**, matching MegaMek's own layout — odd columns sit half a
  hex lower. Units land in the hex they actually occupied rather than on an approximate grid.
- **A line from each unit to its nearest team-mate.** This is the centrepiece: it draws cohesion
  instead of tabulating it. Short tangled lines are a blob; longer even lines are a formation. It
  makes the headline metric visible without the reader having to trust a number.
- **Red north / blue south, pinned to team id**, so a side keeps its colour across every arm and
  every map. Wrecks keep their own side's colour, so you can see who is losing units.
- **Dot opacity tracks remaining armour**, and labels show the model code (AS7, BLR, TDR) — what a
  player reads off a record sheet, not the chassis name. Labels are toggleable, because two dozen
  of them at once is unreadable.

Terrain is deliberately desaturated and elevation is a lightness ramp rather than a second hue: the
map has to stay legible *underneath* the unit markers, since the formation is the thing being
measured. If the terrain competes for attention, the visualisation has failed.

**Wrecks have to be inferred.** A destroyed unit does not get a "destroyed" row — it simply stops
appearing in the log, and removed units report position −1,−1. `extract_battle_trace.py` therefore
carries a unit forward as a wreck once it stops appearing. Without that, losses vanish silently and
the map implies the formation thinned by choice rather than by casualties.

### 6. Recommendation

What to do next, with the specific changes named, and for each one a **success test and a failure
sign** stated in advance. "Raise X to Y; success is metric Z below N; if metric W collapses instead,
the change has backfired and should be halved."

### 7. Methodology notes

The traps you hit and how you handled them. This is the section that makes the rest credible.

---

## Methodology rules these reports assume

Violating any of these invalidates the numbers, so state explicitly that you followed them.

**Run a control arm.** Two arms of the *same* code against each other. Its win rate is the yardstick
for what counts as a real effect. Without it you cannot distinguish skill from map, dice or seating.

**Always pool side-swapped variants.** Deployment position is worth far more than most behaviour
changes. Run variant A and variant B with the sides exchanged, and only ever report the pooled
figure. Measured bias on `32x34 Requiem for a Blue Star`: **76/24 from identical code**.

**Measure the shape, not just the nearest neighbour.** A metric built on distance to the *nearest*
friendly unit cannot tell a tight blob from a company strung right across the board — in both, every
unit has a close neighbour. Measured on the Requiem board: nearest-friend read 2.6 hexes ("tight")
while the same company was **22–27 hexes wide**, which is a picket line, not a formation. Always
report **force diameter** (furthest pair) and **radius from the centroid** alongside it. Getting this
wrong does not just mislabel the behaviour; it hides the fact that the starting position, not the
movement code, is setting the outcome.

**Filter degenerate games.** Roughly 4% of games have a side that fails to deploy — zero units, zero
Battle Value. Unfiltered they hand the opponent a free win and produce NaN in percentage maths.
Exclude them and *report the count*.

**End on attrition, not a turn cap.** A binding round cap makes "rounds to decision" a constant and
destroys the most useful pace metric. End when one side loses half its units. Two traps here:
`activeunits atMost N` fires before deployment finishes (count is 0) and ends the game on round 1;
and `killedunits` scoped only by `player:` counts ejected crew, so it fires early. Scope it to an
explicit unit ID list.

**Search rolled logs.** `princess.log` rotates at 5 MB, which a company-scale game exceeds in about
a minute, so any startup-time line is already compressed away. Grepping only the plain log gives
false negatives — this made a working feature look unwired twice in one evening. Search the `.gz`
archives too.

**Write predictions before reading results.** State what each outcome would mean, and what you would
change in response, *before* the data lands. It is the difference between a conclusion and a
rationalisation. In the Mutual Support run this correctly predicted both failures from the constants
alone.

**Prove the code under test actually ran.** Have the feature log a receipt naming itself at wiring
time, and confirm it is present in the change arm and absent in the control.

---

## Producing a report

1. Run the arms (`run-ai-benchmark.ps1`, or `gradlew aiMatch` for a single batch).
2. Win rates: `analyze_ai_benchmark.py --head-to-head <A> --head-to-head <B>`.
3. Behaviour metrics: `analyze_doctrine_metrics.py --run <dir> --teams "2=BotA,3=BotB"`.
   The `--teams` map matters — the side-swapped variant puts each bot on the opposite team, so
   pooling by raw team number silently mixes them.
4. Battle map data, one per arm:
   `extract_battle_trace.py --run <dir> --name "<label>" --out trace-base.json`
5. Map terrain, once per board:
   `extract_board.py --board "<path to .board>" --out board.json`
6. Copy `ai-benchmark-report-template.html`, replace the `[bracketed placeholders]`, and paste the
   three JSON blobs where the script says to. Set the round slider's `max` to the round count of
   the shorter trace.

Example for a scenario on the Requiem board:

```powershell
$tools = "<python tooling dir>\ai-benchmark"
$runs  = "D:\MegaMek Projects\ai-benchmark-runs"

py "$tools\analyze_ai_benchmark.py"    --head-to-head "$runs\<armA>\results.csv" `
                                       --head-to-head "$runs\<armB>\results.csv"
py "$tools\analyze_doctrine_metrics.py" --run "$runs\<armA>" --label "A" --teams "2=Princess,3=CASPAR" `
                                        --run "$runs\<armB>" --label "B" --teams "2=CASPAR,3=Princess"
py "$tools\extract_battle_trace.py"     --run "$runs\<armA>" --name "Stock"  --out trace-base.json
py "$tools\extract_battle_trace.py"     --run "$runs\<armB>" --name "Change" --out trace-after.json
py "$tools\extract_board.py" --board "megamek\data\boards\<board>.board" --out board.json
```

Those five scripts currently live outside the repo in the local Python tooling directory alongside
the other MegaMek scripts:

| Script | Produces |
|---|---|
| `run-ai-benchmark.ps1` | shards a batch across worker JVMs, merges `results.csv` |
| `chain-mutual-support-arms.ps1` | runs several arms back to back, unattended |
| `analyze_ai_benchmark.py` | win rates with confidence intervals; filters degenerate games |
| `analyze_doctrine_metrics.py` | behaviour metrics mined from the game TSVs |
| `extract_battle_trace.py` | per-round unit positions for the map |
| `force_width.py` | force diameter, radius from centroid and nearest-friend, per round |
| `extract_board.py` | board terrain for the map |

If benchmarking becomes routine they are worth moving into the repo so the report and its inputs
version together.

### Notes on the template

- It is a **complete standalone HTML file** — open it in a browser directly. No build step, no
  network access, nothing external to fetch. (When published as an Artifact instead, drop the
  `<!DOCTYPE>`, `<html>`, `<head>` and `<body>` wrapper — the host supplies it.)
- All three JSON blobs are **inlined**, not fetched. A `file://` page cannot fetch a sibling file,
  and the Artifact host blocks external requests, so pasting is the only thing that works in both.
- It is **theme-aware**: light and dark are both defined through CSS custom properties, and the
  canvas re-reads its colours on theme change rather than baking them in at load.
- The map **degrades gracefully**: if `BOARD` is left `null` the units still render on a blank
  field, so a report can ship before the board has been extracted.
- Page weight is small — the Requiem board is 6 KB of JSON and an 18-round trace about 22 KB, so a
  full report with two arms and terrain lands near 110 KB with no external requests.
