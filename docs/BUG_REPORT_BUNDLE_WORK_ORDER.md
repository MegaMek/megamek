# Work Order — One-Click Bug Report Bundle + Prefilled Issue Form (MegaMek)

**Audience:** a Claude Code CLI session working in the `MegaMek/megamek` repo (IntelliJ IDEA). Everything here is
self-contained; you do not need any prior conversation. Line numbers are against `main` at commit `5e808c6754`
("Ready to Develop 0.51.01"). **Re-read a file before editing — line numbers shift as you insert.**

**Origin:** this is the MegaMek counterpart of MekHQ PR
[#8433 "Feature: Added 'Report a Bug' Helper"](https://github.com/MegaMek/mekhq/pull/8433) (Illiani, merged
2025-12-07). Read that PR's diff before starting — but read §2 first, because roughly half of it is already done in
MegaMek and another chunk of it must **not** be copied.

---

## 1. Goal

Give the player a button that produces **one zip file they can drag onto a GitHub issue**, and a repo button that
opens the issue form **already filled in** with the version/OS/Java details the template currently asks them to dig
out of a log file by hand.

Concretely, after this work order:

- `Help > Report a Bug` gains a **"Package Bug Report"** button. Pressing it saves the game (if one is running),
  collects the right log files, and writes `MegaMek-BugReport-<timestamp>.zip` where the player chooses.
- The four repo buttons (MegaMek / MegaMekLab / MekHQ / Data) open the issue form with **MegaMek Suite Version**,
  **Operating System** and **Java Version** pre-populated.
- Nothing requires the player to log in, and nothing requires a GitHub token, OAuth app, or any change on the
  MegaMek org's side.

---

## 2. Facts already verified — rely on these, do not re-derive

### 2.1 Over half of MekHQ #8433 already exists in MegaMek

Someone already ported the dialog. **Do not create a new dialog class.**

| Piece | Where it already is |
|---|---|
| The dialog itself | `megamek/src/megamek/client/ui/clientGUI/BugReportDialog.java` |
| Menu action | `megamek/src/megamek/client/ui/ShowBugReportDialogAction.java` |
| Menu wiring | `CommonMenuBar.java:425` — `menu.add(new ShowBugReportDialogAction(this, new CopySystemDataAction()));` |
| Four repo links + Discord | `BugReportDialog.java:58-61`, `MMConstants.DISCORD_LINK` (`MMConstants.java:50`) |
| i18n text ("Which Bug Goes Where?", the 4 steps) | `megamek/resources/megamek/client/BugReport.properties` (+ `_de`, `_en`) |
| i18n accessor | `megamek/src/megamek/client/ui/BugReportMessages.java` — use `I18N.get(key)` |
| "Copy System Data" button | `megamek/src/megamek/client/ui/CopySystemDataAction.java` (MekHQ has no equivalent) |
| Browser launch helper | `UIUtil.browse(String address)` — already used by `BugReportDialog.UrlButton` |

`CommonMenuBar.getMenuBarForMainMenu()` (`:234`) builds the same menu bar for the **main menu**, and the bug-report
item is added with `menu.add(action)` rather than `initMenuItem(...)`, so it is **not** part of the enable/disable
machinery and is always clickable. The dialog is therefore reachable both with and without a running game — §6.3
depends on this.

### 2.2 The invasive half of MekHQ #8433 has NO MegaMek counterpart — do not port it

MekHQ threaded an `isBugReportPrep` flag through `Campaign.writeToXML()`, which forced signature changes in
`CampaignGUI`, `AutosaveService` and `writeCustoms`, purely to bake custom units into the save so players do not have
to attach their `.mtf`/`.blk` files.

MegaMek does not need any of that. `GameManagerSaveHelper.java:85` serializes the **entire `Game` object** with
XStream:

```java
SerializationHelper.getSaveGameXStream().toXML(gameManager.getGame(), writer);
```

and `Server.loadGame` (`Server.java:949-951`) reads it straight back with `xStream.fromXML(gzi)` and
`newGame.initializeAfterLoad()`. It never consults `MekSummaryCache` and never re-reads a board file. Entities,
their damage state, equipment, and the board hexes are all already inside the `.sav.gz`.

**Consequence: a MegaMek save is self-contained. Write no customs-export code.**

### 2.3 Saving in MegaMek is asynchronous and server-mediated — this is the real work

MekHQ's `saveCampaign(...)` writes on the calling thread and returns a `boolean`, so `EasyBugReport` can save → zip →
delete in one method. **MegaMek cannot do this.** The actual sequence when a player saves:

1. `ClientGUI.saveGame()` (`ClientGUI.java:2791-2811`) sends the chat command
   `/localsave <fileName> <path>` and calls `client.setAwaitingSave(true)`.
2. The **server** serializes to its own `savegames/` directory (`GameManagerSaveHelper.saveGame`).
3. The server reads that file back byte-by-byte and pushes it to the client as a `SEND_SAVEGAME` packet
   (`GameManagerSaveHelper.java:122-129`).
4. `Client.java:1334-1363` handles `SEND_SAVEGAME`, writes the file to the player's disk, and calls
   `setAwaitingSave(false)`.

So the archiver **cannot zip the save immediately after requesting it** — the file does not exist yet. §5.2 adds a
completion callback. There is precedent for the crude alternative: `ClientGUI.die()` (`:1692-1696`) re-posts itself
with `SwingUtilities.invokeLater` while `client.isAwaitingSave()`. Do not copy that; use the callback.

### 2.4 There is a save path that never completes (pre-existing bug — you must defend against it)

`LocalSaveGameCommand.run()` refuses to save when a game is double-blind **and**
`OptionsConstants.BASE_DISABLE_LOCAL_SAVE` is set. It prints "Local Save only outside double blind games." to chat
and returns **without ever sending a `SEND_SAVEGAME` packet**. The client is then stuck with `awaitingSave == true`
forever.

This is not introduced by this work order, but the packager walks straight into it and would hang. §6.2 requires a
timeout with a logs-only fallback. Do not attempt to fix `LocalSaveGameCommand` here — it is out of scope; note it
in the PR body as a follow-up.

### 2.5 The `logs/` directory is a dumping ground — a `*.log` glob is wrong in both directions

MekHQ's `createBugReportArchive` globs the logs folder for `*.log` and `*.log.gz`. Measured against a real working
`megamek/logs/` on this machine:

- total directory size: **437 MB**
- what MekHQ's filter would collect: **27 files, 0.2 MB**
- largest files present: `gamelog_2026-07-21_11-16-49.html` and siblings, **~3 MB each**, hundreds of them
- also present: several hundred `Bot_*.mul` files that MegaMek writes at game end

Two problems with copying the filter verbatim:

1. The **most useful artifact for a MegaMek bug** — the round-by-round combat report, `gamelog*.html` — does **not**
   match `*.log` and would be skipped.
2. `princess.log` and `bot_path_ranker.log` grow without bound in long bot games and can blow past GitHub's
   attachment cap on their own.

MegaMek therefore needs an explicit **manifest**, not a glob. See §5.1.

Log destinations are declared in `megamek/mmconf/log4j2.xml`:

| Appender | File | Rolled pattern |
|---|---|---|
| `UnifiedLog` | `logs/unified_log.log` | `logs/unified_log_%i.log.gz` |
| `BotLog` | `logs/bot_path_ranker.log` | `logs/bot_path_ranker_%i.log.gz` |
| `PrincessLog` | `logs/princess.log` | `logs/princess_%i.log.gz` |
| `MegaMekLog` | `logs/megamek.log` | (none, `append="false"`) |

**Do not hardcode `"logs"`.** MekHQ did (`MHQConstants.LOGS_PATH`); MegaMek's log directory is user-configurable and
the rest of the codebase reads it from `PreferenceManager.getClientPreferences().getLogDirectory()` (see
`Client.java:1039`, `ClientGUI.java:1624`, `ClientGUI.java:3368`). Use that. The game-log filename likewise comes
from `ClientPreferences.getGameLogFilename()` (default `gamelog.html`, `ClientPreferences.java:144`), and
`ClientPreferences.stampFilenames()` controls whether a timestamp is appended.

### 2.6 GitHub attachment size cap

25 MB for `.zip` (10 MB for images/GIFs, 100 MB for video on paid plans). Source:
[Attaching files](https://docs.github.com/en/get-started/writing-on-github/working-with-advanced-formatting/attaching-files).
This is the number to enforce in §5.1.

---

## 3. The GitHub tracker question, answered

The brief asked whether this can "write the bug report, send the file, and choose the labels." Three separate
answers, and they are not the same answer.

### 3.1 Write the bug report — YES, and it is the best part of this work order

All three suite repos use GitHub **issue forms** (`.github/ISSUE_TEMPLATE/bug_report.yml`), and issue-form fields can
be prefilled by URL query parameter keyed on the field's `id`
([docs](https://docs.github.com/en/communities/using-templates-to-encourage-useful-issues-and-pull-requests/syntax-for-issue-forms)).

I diffed the three templates. **MegaMek, MegaMekLab and MekHQ use byte-identical field ids**, so one URL builder
serves all three buttons with only the repo slug changing:

| Field `id` | Template label | Can MegaMek fill it? |
|---|---|---|
| `brief-description` | Brief Description * | No — the player writes this |
| `steps-to-reproduce` | 3. Steps to Reproduce | No |
| `attached-files` | Attach Files | Yes — a pointer to the zip filename |
| `severity` | Severity * (dropdown) | No — leave for the player |
| `custom-megamek-version` | MegaMek Suite Version * | **Yes** — `SuiteConstants.VERSION` |
| `operating-system` | Operating System * | **Yes** — `os.name` + `os.version` + `os.arch` |
| `java-version` | Java Version * | **Yes** — `java.vendor` + `java.version` |
| `final-checks` | Final Verification (checkboxes) | No — the player must actually confirm these |

This matters more than it looks. `bug_report.yml:57-66` currently tells the player:

> "For the next sections, go to the 'logs' folder. Find the MegaMek.log file and open it with a text editor. The
> information in the header will be needed."

...followed by an imgur screenshot showing them what to copy. Those three fields are all marked `required: true`.
We already have every one of those values in-process — `MegaMek.getUnderlyingInformation()` (`MegaMek.java:372-400`)
assembles them from `SuiteConstants.VERSION` and `System.getProperty(...)`. **Read the values from those sources
directly; do not parse `getUnderlyingInformation()`'s formatted output.**

`mm-data` is the exception — its button points at `issues/new` with no template, so leave that one unchanged.

### 3.2 Choose the labels — ALREADY DONE, and doing it ourselves would break the button

`bug_report.yml:7-8` declares:

```yaml
labels:
  - bug
```

The template applies that label **server-side, for every reporter, regardless of permission**. Nothing to build.

Do **not** add `&labels=...` to the URL. GitHub's docs are explicit that query parameters require the corresponding
permission — you must be able to add a label to use the `labels` parameter — and that **an invalid or
under-permissioned query parameter returns a 404 Not Found page**
([docs](https://docs.github.com/en/free-pro-team@latest/github/managing-your-work-on-github/about-automation-for-issues-and-pull-requests-with-query-parameters)).
Ordinary players have no triage permission on `MegaMek/megamek`. Adding a `labels` parameter would therefore replace
the issue form with a 404 for **exactly the people this feature exists to help**, while working fine for every
maintainer who tests it. This is a trap; the doc exists partly to record it.

Severity is captured by the template's `severity` dropdown, not by a label, so there is nothing else to choose.

### 3.3 Send the file — NO. Do not attempt this in this PR.

There is no supported way to do it, and the unsupported way costs more than it is worth.

- **There is no official REST API for issue attachments.** This is a long-standing gap; see GitHub community
  discussions [#46951](https://github.com/orgs/community/discussions/46951) and
  [#28219](https://github.com/orgs/community/discussions/28219).
- An **undocumented** endpoint, `https://uploads.github.com/user-attachments/assets`, became publicly known around
  August 2026 ([write-up](https://island94.org/2026/08/programmatically-upload-attachments-to-github-issues-pull-requests-comments)).
  It takes a bearer token and mimics drag-and-drop. The author of that write-up describes it as unofficial and
  undocumented and says outright that they do not know whether the token behaviour is new or merely unnoticed. There
  is no stability guarantee, no documented size limit, and no deprecation policy.
- Using it — or the REST issue-creation API — requires the **player** to authenticate MegaMek against their own
  GitHub account. A desktop open-source app cannot ship a client secret, so the only correct flow is OAuth Device
  Flow, which needs an OAuth App **registered under the MegaMek organisation**. That is an org/admin decision by the
  maintainers, not something a code PR can settle.
- It also turns MegaMek into a potential issue-spam vector and puts volunteer maintainers on the hook for token
  handling and revocation.

**Recommendation:** ship §3.1 + §3.2 + the local zip now. They need no auth, no secret, no org decision, and deliver
most of the value. If the maintainers later decide they want true one-click filing, that is a separate PR that starts
with an org-level OAuth App registration, not with code.

The practical substitute is in §6.4: put the finished zip **on the system clipboard as a file**, so the player pastes
it directly into the GitHub upload box rather than hunting for it.

---

## 4. Hard constraints

1. **Do not create a new dialog.** Extend the existing `BugReportDialog` the way `copySystemDataAction` already is —
   an optional `Action` passed in by the caller. This keeps `BugReportDialog` free of any `Client` dependency, which
   is what lets the main-menu (no game) case work.
2. **No new logic in `ClientGUI` or `CommonMenuBar` beyond wiring.** Both are large. The archiver is a new class.
3. **The archiver must be Swing-free and headless-testable.** It goes in `megamek.common.util` so a JUnit test can
   drive it with a temp directory. All Swing lives in the action class.
4. **Never write a save the player did not ask for into their `savegames/` folder permanently.** Follow MekHQ's
   pattern: save to a temp name next to the chosen archive, zip it, delete the temp file — but on archive failure,
   leave it so the player still has something to attach.
5. **Every user-facing string goes in `BugReport.properties`** via `BugReportMessages`. No hardcoded English.
6. **ASCII only in code and log messages** (`[OK]`, `[ERROR]` — not check marks). Windows console encoding.
7. **Tag all diagnostic logging `[BugReport]`** so a playtest log can be grepped for the whole flow, per
   `CLAUDE.md`'s diagnostic-logging rule.
8. The zip must be **capped at 25 MB** (§2.6) and must report what it dropped rather than silently truncating.

---

## 5. Task checklist (single PR)

- [x] 1. New `megamek/common/util/BugReportBundle.java` — manifest + zip writer, Swing-free.
- [x] 2. New `megamek/common/util/IssueReportUrl.java` — prefilled issue-form URL builder.
- [x] 3. `AbstractClient` / `Client` — one-shot save-completion callback.
- [x] 4. New `megamek/client/ui/PackageBugReportAction.java` — the Swing action (chooser, timeout, result dialog).
- [x] 5. `BugReportDialog.java` — accept and show the new action; use `IssueReportUrl` for the three form repos.
- [x] 6. `CommonMenuBar.java` — pass the action in.
- [x] 7. `BugReport.properties` — new keys; rewrite step 1 of `mainText` (it currently says "zip it yourself").
- [x] 8. New `megamek/unittests/megamek/common/util/BugReportBundleTest.java` (plus `IssueReportUrlTest.java`).
- [x] 9. `./gradlew :megamek:compileJava :megamek:test` — done. **The manual smoke test in §8 is still outstanding.**

> **Progress, 2026-08-14.** All code steps are done on branch `Implement-Bug-Report-Bundle` (not committed, not
> pushed). `:megamek:compileJava` and `:megamek:checkstyleMain` are clean, and the full suite is green at 15,072
> tests / 0 failures / 0 errors.
>
> **The feature has not been run once.** Everything in §7 "What is NOT proven yet" still stands in full, and §8's
> eight manual steps are all outstanding. Treat this as compiled, not working.
>
> Resolved decisions: §6.6 uses a `Supplier<Client>` held by `CommonMenuBar` and set from `ClientGUI`
> (`setClientSupplier`). §6.5 passes `null` for the attachment note, so `attached-files` is left empty for now.

---

## 6. Implementation

### 6.1 New: `megamek/src/megamek/common/util/BugReportBundle.java`

Swing-free. Full MegaMek license header, `Copyright (C) 2026`.

Responsibilities: decide **which** files go in (the manifest), enforce the size cap, write the zip, and report back
what was included and what was dropped.

```java
package megamek.common.util;

/**
 * Collects the files a MegaMek bug report needs and writes them to a single zip archive.
 *
 * <p>The file list is a deliberate manifest rather than a directory scan. The log directory also accumulates
 * per-game {@code gamelog*.html} reports and {@code Bot_*.mul} unit lists, and can reach hundreds of megabytes;
 * sweeping it wholesale would produce an archive far too large to attach to an issue.</p>
 *
 * <p>The archive is capped at {@link #MAX_ARCHIVE_BYTES}. Files that would exceed the cap are skipped and named
 * in the returned {@link BundleResult} so the caller can tell the player what was left out.</p>
 */
public class BugReportBundle {

    private static final MMLogger LOGGER = MMLogger.create(BugReportBundle.class);

    /** GitHub refuses issue attachments over 25 MB. */
    public static final long MAX_ARCHIVE_BYTES = 25L * 1024L * 1024L;

    /** Log basenames worth collecting, in priority order - the first is the most important. */
    private static final List<String> LOG_BASE_NAMES =
          List.of("megamek", "unified_log", "princess", "bot_path_ranker");

    /**
     * The outcome of writing an archive.
     *
     * @param archiveFile  the archive that was written
     * @param includedEntries archive-relative paths that made it in
     * @param skippedEntries names of files skipped because of the size cap, may be empty
     * @param totalBytes    the uncompressed total of everything included
     */
    public record BundleResult(File archiveFile, List<String> includedEntries,
          List<String> skippedEntries, long totalBytes) {}

    // constructor takes: File logDirectory, @Nullable File saveGameFile,
    //                    String systemInformation, @Nullable String gameLogFileName
}
```

**Manifest, in this order** (order matters — the size cap drops from the bottom):

1. `system-info.txt` — the `systemInformation` string, written as a zip entry from an in-memory byte array. This is
   the same text `CopySystemDataAction` puts on the clipboard: `MegaMek.getUnderlyingInformation(originProject,
   currentProject)`. Tiny and always first.
2. The save game at the archive root, if non-`null`.
3. `logs/megamek.log`, `logs/unified_log.log` — always, if they exist.
4. `logs/princess.log`, `logs/bot_path_ranker.log` — only if they exist and are non-empty.
5. The **single newest** `gamelog*.html` in the log directory. Match on
   `ClientPreferences.getGameLogFilename()`'s basename, because `stampFilenames()` turns `gamelog.html` into
   `gamelog_2026-07-21_11-16-49.html`. **Newest only — never all of them.**
6. Rolled archives `<base>_<n>.log.gz` for the basenames in (3) and (4), newest first, as space allows.

Implementation notes:

- Track a running total; before adding an entry, if `total + file.length() > MAX_ARCHIVE_BYTES`, add the name to
  `skippedEntries` and continue to the next candidate — do **not** abort the archive.
- Log a single summary line **after** the loop, never inside it (`CLAUDE.md` logging rules):
  `LOGGER.info("[BugReport] Archive written: {} entries, {} skipped, {} bytes", ...)`.
- The byte-copy helper is a straight port of `EasyBugReport.addFileToZip` (8 KB buffer, `putNextEntry` /
  `closeEntry`); that part of the MekHQ PR is fine to copy.
- Name the parameter `zipOutputStream`, not `zos`. Reviewers flag abbreviations.

### 6.2 New: `megamek/src/megamek/common/util/IssueReportUrl.java`

Builds the prefilled issue-form URL. Swing-free so it can be unit-tested.

```java
/**
 * Builds a GitHub issue-form URL with the environment fields pre-populated.
 *
 * <p>MegaMek, MegaMekLab and MekHQ all use issue forms whose field ids are identical, so the same query string
 * serves all three; only the repository slug differs. The field ids below must stay in sync with
 * {@code .github/ISSUE_TEMPLATE/bug_report.yml} in each repository.</p>
 *
 * <p>Note that the {@code labels} query parameter is deliberately NOT set. The template already applies the
 * {@code bug} label server-side, and GitHub returns a 404 page when a query parameter names a label the visitor
 * lacks permission to apply - which would break the link for every non-maintainer.</p>
 */
public final class IssueReportUrl {

    private static final String FIELD_VERSION = "custom-megamek-version";
    private static final String FIELD_OPERATING_SYSTEM = "operating-system";
    private static final String FIELD_JAVA_VERSION = "java-version";
    private static final String FIELD_ATTACHED_FILES = "attached-files";

    /** Keep the whole URL well inside what browsers and GitHub accept. */
    private static final int MAX_URL_LENGTH = 6000;

    public static String forIssueForm(String repositoryUrl, @Nullable String attachmentNote) { ... }
}
```

- Values come straight from source, **not** from parsing `getUnderlyingInformation()`:
  - version → `SuiteConstants.VERSION`
  - operating system → `"%s %s (%s)".formatted(os.name, os.version, os.arch)`
  - java version → `"%s %s".formatted(java.vendor, java.version)`
- Encode with `URLEncoder.encode(value, StandardCharsets.UTF_8)`.
- Base URL keeps `?template=bug_report.yml` — the repo sets `blank_issues_enabled: false`, so the template must be
  named explicitly rather than relying on `/choose`.
- If the assembled URL exceeds `MAX_URL_LENGTH`, drop `attached-files` first, then fall back to the plain
  `issues/new/choose` link. Never emit a truncated URL.
- `attachmentNote` is the zip's filename once one has been built (e.g. "Please attach
  MegaMek-BugReport-2026-08-14_17-48-02.zip, created by the in-game Package Bug Report button."), or `null`.

### 6.3 `AbstractClient` / `Client` — save-completion callback

The one piece of engine plumbing this needs. Keep it minimal.

In `megamek/src/megamek/client/AbstractClient.java`, beside the existing `awaitingSave` field (`:87`) and its
accessors (`:603-609`):

```java
private @Nullable Consumer<File> saveCompletionCallback;

/**
 * Registers a one-shot callback fired once a requested local save has actually landed on disk. Saving is
 * asynchronous - the request goes to the server, which serializes the game and streams the file back - so callers
 * that need the finished file (such as the bug report packager) cannot simply read it after calling save.
 *
 * @param saveCompletionCallback invoked with the saved file, or {@code null} to clear a pending registration
 */
public void setSaveCompletionCallback(@Nullable Consumer<File> saveCompletionCallback) {
    this.saveCompletionCallback = saveCompletionCallback;
}

/** Fires and clears any registered save-completion callback. Safe to call when none is registered. */
protected void fireSaveCompleted(@Nullable File savedFile) {
    Consumer<File> callback = saveCompletionCallback;
    saveCompletionCallback = null;
    if (callback != null) {
        callback.accept(savedFile);
    }
}
```

In `megamek/src/megamek/client/Client.java`, in the `SEND_SAVEGAME` branch (`:1334-1363`): after **both**
`setAwaitingSave(false)` sites (`:1348` in the `finally`, `:1362` at the end of the branch), call
`fireSaveCompleted(new File(localFile))`. `localFile` is already computed at `:1337`. Because `fireSaveCompleted`
clears the field first, calling it twice is harmless.

> Do **not** touch `Precognition.java:338` — it lists `SEND_SAVEGAME` among packets the bot deliberately ignores,
> which stays correct.

### 6.4 New: `megamek/src/megamek/client/ui/PackageBugReportAction.java`

The Swing side. Model it on `CopySystemDataAction` (same package, same `AbstractAction` shape) so it slots into
`BugReportDialog` identically.

Constructor: `PackageBugReportAction(@Nullable Window parent, @Nullable Client client)`. A `null` client means
"no game running" — the action stays enabled and produces a logs-only bundle.

`actionPerformed` flow:

1. **Choose the destination.** `JFileChooser` defaulting to `MMConstants.SAVEGAME_DIR`, with a
   `FileNameExtensionFilter` for `zip`, pre-named `MegaMek-BugReport-<timestamp>.zip`. Append `.zip` if the player
   removed it (`toLowerCase(Locale.ROOT).endsWith(".zip")`, as MekHQ does). Cancel → log at DEBUG and return.
2. **No client, or client not connected** → skip to step 5 with a `null` save file.
3. **Request the save.** Register `client.setSaveCompletionCallback(...)`, then send the same command
   `ClientGUI.saveGame()` uses:
   ```java
   client.sendChat(ClientGUI.CG_CHAT_COMMAND_LOCAL_SAVE + " " + tempSaveName + " " + escapedParentPath);
   client.setAwaitingSave(true);
   ```
   The parent path must have spaces replaced with `|` — see `ClientGUI.java:2807`; the server undoes this in
   `GameManagerSaveHelper.java:119`. Getting this wrong silently writes the save to the wrong directory.
4. **Guard the hang from §2.4.** Start a non-repeating `javax.swing.Timer` (30 s). Whichever of callback or timer
   fires first wins; the loser must be disarmed. On timeout: clear the callback, `setAwaitingSave(false)`, log
   `LOGGER.warn("[BugReport] Save request timed out after {} ms - packaging logs only", ...)`, and continue to
   step 5 with a `null` save. **The player must still get a zip.**
5. **Build the archive** off the EDT (`SwingWorker`) — zipping tens of megabytes will freeze the UI otherwise.
   Feed `BugReportBundle` the log directory from
   `PreferenceManager.getClientPreferences().getLogDirectory()`, the system-info string from
   `MegaMek.getUnderlyingInformation(MegaMek.getOriginProject(), MMConstants.PROJECT_NAME)`, and the game-log
   basename from `ClientPreferences.getGameLogFilename()`.
6. **Delete the temp save** on success; on `IOException`, leave it (MekHQ's reasoning applies verbatim: the player
   still has something to attach) and log at ERROR.
7. **Report back** with a `JOptionPane` listing what went in, what was skipped, and the archive path. Give it two
   extra buttons:
   - **Open folder** — `Desktop.getDesktop().open(archiveFile.getParentFile())`. **Do not use
     `Desktop.browseFileDirectory(File)`**; it throws `UnsupportedOperationException` on Windows, which is this
     project's primary platform.
   - **Copy file to clipboard** — put the archive on the clipboard as a file list so the player can paste it
     straight into GitHub's upload box:
     ```java
     Transferable fileTransferable = new Transferable() {
         @Override public DataFlavor[] getTransferDataFlavors() {
             return new DataFlavor[] { DataFlavor.javaFileListFlavor };
         }
         @Override public boolean isDataFlavorSupported(DataFlavor flavor) {
             return DataFlavor.javaFileListFlavor.equals(flavor);
         }
         @Override public Object getTransferData(DataFlavor flavor) {
             return List.of(archiveFile);
         }
     };
     Toolkit.getDefaultToolkit().getSystemClipboard().setContents(fileTransferable, null);
     ```
     **Verify this during the smoke test** (§8 step 7). Pasting a file into a browser upload target is reliable on
     Windows and macOS but varies by desktop environment on Linux. If it does not work, keep the button but reword
     it, or drop it — the Open-folder button is the guaranteed path.

Log every gate and transition with the `[BugReport]` prefix, at DEBUG for per-click detail and INFO for the
one-shot completion, per `CLAUDE.md`.

### 6.5 `BugReportDialog.java` — show the new button, prefill the repo links

Two small changes. Do not restructure the class.

1. **Second optional action.** The class already takes `@Nullable Action copySystemDataAction` (`:68`) and guards it
   at `:97`. Add `@Nullable Action packageBugReportAction` the same way, and add it to `row3` in `buttonPanel()`
   (`:96-99`) next to the copy-system-data button. `row3` is already a plain `JPanel` with `FlowLayout`, so a second
   button needs no layout change; the `GridLayout(3, 1, 0, 8)` root is unaffected.

2. **Prefilled URLs.** In `buttonPanel()` (`:91-94`), replace the three form-repo constants with
   `IssueReportUrl.forIssueForm(...)`:

   ```java
   row2.add(new UrlButton(I18N.get("mm.text"), IssueReportUrl.forIssueForm(REPORT_LINK_MM, attachmentNote)));
   row2.add(new UrlButton(I18N.get("mml.text"), IssueReportUrl.forIssueForm(REPORT_LINK_MML, attachmentNote)));
   row2.add(new UrlButton(I18N.get("mhq.text"), IssueReportUrl.forIssueForm(REPORT_LINK_MHQ, attachmentNote)));
   row2.add(new UrlButton(I18N.get("mmData.text"), REPORT_LINK_MM_DATA));   // unchanged - no template
   ```

   `REPORT_LINK_*` currently end in `/issues/new/choose`; `IssueReportUrl` rewrites that to
   `/issues/new?template=bug_report.yml&...`. Leave the constants as the canonical repo links.

   `UrlButton` sets its tooltip to the raw address (`:110`). A prefilled URL is long and ugly in a tooltip — give
   `UrlButton` an optional display tooltip so it can keep showing the plain repo URL.

   > **Ordering note:** `attachmentNote` is only known *after* a zip has been built, but the buttons are created in
   > the constructor. For v1 pass `null` and let `attached-files` stay empty. Making the repo buttons rebuild their
   > URL after a successful package is a nice-to-have; if you do it, hold the `UrlButton`s in fields and add a
   > `setAddress(String)`, do not rebuild the panel.

### 6.6 `CommonMenuBar.java` — pass the action in

At `:425`:

```java
menu.add(new ShowBugReportDialogAction(this, new CopySystemDataAction()));
```

`CommonMenuBar` already knows its context through the `isGame` field (`:241-244`). Extend
`ShowBugReportDialogAction`'s constructor with a third `@Nullable Action` parameter and forward it into
`BugReportDialog` at `:74`. Keep the existing two-argument constructor delegating to the new one so nothing else
breaks.

`CommonMenuBar` has no `Client` reference, so getting one to the action needs a small amount of care — the
`ClientGUI` that owns the menu bar does. Simplest correct approach: give `ShowBugReportDialogAction` a
`Supplier<Client>` (or have `ClientGUI` set the action's client after construction) rather than resolving it at menu
build time, when no game exists yet. **A `null` client is a supported state, not an error** — that is the whole
reason `PackageBugReportAction` accepts one.

### 6.7 `BugReport.properties` — new keys and one correction

File: `megamek/resources/megamek/client/BugReport.properties`. Add:

```properties
package.text=Package Bug Report
package.tooltip=Saves the current game (if any) and collects the logs into a single zip you can attach to an issue.
package.result.title=Bug Report Packaged
package.result.included=Included in {0}:
package.result.skipped=Left out to stay under GitHub's 25 MB attachment limit:
package.result.openFolder=Open Folder
package.result.copyFile=Copy File to Clipboard
package.noGame=No game is running, so the archive contains logs and system information only.
package.saveTimedOut=The server did not return a save in time, so the archive contains logs only.
package.failed=Could not write the bug report archive. See megamek.log for details.
```

**Also fix `mainText` step 1.** It currently reads "Save your game, campaign or unit. Best make a ZIP of it so you
can upload it with the issue report." — that instruction is obsolete the moment the button exists. Reword it to
point at the Package Bug Report button, and keep the manual route as the fallback for anyone whose save fails.

Per `CLAUDE.md`: do **not** copy the new English strings into `BugReport_de.properties`. Missing keys fall back to
English automatically. `BugReport_en.properties` follows the existing convention in that file — check what it
currently does before deciding whether to add keys there.

> **Editing trap** (recorded from prior sessions): `\ ` escapes in `.properties` files do not survive the Edit tool
> cleanly, and PowerShell `Set-Content` adds a BOM that corrupts the file. Use the Write tool, and keep the new
> values free of leading/trailing-space escapes.

### 6.8 New: `megamek/unittests/megamek/common/util/BugReportBundleTest.java`

`BugReportBundle` is deliberately Swing-free so this is a plain JUnit test with a `@TempDir`. Cover:

1. **Empty log directory** — archive is still produced and contains `system-info.txt`. (The main-menu / fresh-install
   case; MekHQ's version returns early here, and getting it wrong means the button appears to do nothing.)
2. **Manifest selection** — given `megamek.log`, `princess.log`, three `gamelog_*.html` of different mtimes, and
   forty `Bot_*.mul`, the archive contains both logs, exactly **one** gamelog (the newest), and **zero** `.mul`
   files.
3. **Size cap** — a log file larger than `MAX_ARCHIVE_BYTES` is skipped, named in `skippedEntries`, and the archive
   still writes successfully with the smaller entries present.
4. **Save file placement** — a supplied save lands at the archive **root**, not under `logs/`.
5. **Entry paths** — log entries are prefixed `logs/`, matching MekHQ's layout so both suites' bundles look alike.

Assert on archive **contents** via `ZipFile`, not on log text or file names in messages — behaviour, not
implementation, per the testing guidance.

A second small test for `IssueReportUrl` is worth having: assert the URL contains the three field ids, contains
`template=bug_report.yml`, and — the regression that matters — **does not contain `labels=`** (§3.2).

---

## 7. Testing status

### Confirmed in game (2026-08-15)

- The **crash dialog path**. A deliberate `NullPointerException` was triggered from a temporary Commands-menu item,
  on both the event dispatch thread and a background thread. The "Uncaught Exception" dialog shows the **Report a
  Bug** button alongside OK, and it opens the helper. The temporary trigger has been removed and was never committed.
- Related fact worth keeping: an EDT exception **does** reach `Thread.setDefaultUncaughtExceptionHandler` on Java 21
  (verified by probe against `jdk-21.0.8.9-hotspot`). Older Java swallowed these in `EventDispatchThread`, printing
  the stack trace instead, so do not assume this from memory if the handler is ever revisited.
- `megamek/sentry.properties` has `enabled=false`, so `Sentry.captureException` on the `errorDialog` path is inert
  and deliberate test crashes are not reported anywhere. Re-check this before enabling Sentry locally.

- The **archive** was produced in game and its contents were complete. That exercises the whole chain that was
  hardest to get right: the asynchronous `/localsave` round trip through the server, the save-completion callback,
  the `SwingWorker` build, and the manifest running against a real log directory rather than a synthetic one.
- **Copy File to Clipboard works on Windows 11.** The `javaFileListFlavor` transferable pastes as a file where it
  needs to. Untested on macOS and Linux; on a desktop environment that does not support it the button simply does
  nothing, and Open Folder remains the guaranteed route, so it is safe to ship as is.
- **The archived save loads back into MegaMek.** Routing the save through the archive does not damage it. Whether the
  test game contained a custom unit is unrecorded, so §2.2's specific claim that customs ride along inside the save
  may still rest on reading `Server.loadGame` rather than on a demonstration.

### What is NOT proven yet

Carry this into the PR body verbatim, per house style:
- The **file-to-clipboard paste into GitHub** (§6.4) is unverified on Linux desktops.
- The **save-timeout path** (§2.4) needs a deliberate double-blind + `BASE_DISABLE_LOCAL_SAVE` game to exercise; it
  is the one branch most likely to be wrong, because the underlying save request never completes by design.
- The **prefilled issue URL must be opened once against the real repository** and visually confirmed to land on a
  filled-in form and not a 404. Test it **while signed out of GitHub, or as a non-maintainer account** — a
  maintainer account has triage permission and will not reproduce the §3.2 failure mode.
- Archive size against a real long bot game is unmeasured; the 25 MB cap is enforced but the manifest priority order
  may need tuning once someone sees a genuine 400 MB log directory.

---

## 8. Compile and verify

1. `./gradlew :megamek:compileJava` — fix signature mismatches, especially `SuiteConstants.VERSION`'s import and
   `ClientPreferences.getGameLogFilename()`.
2. `./gradlew :megamek:test --tests "*BugReportBundleTest*" --tests "*IssueReportUrlTest*"`.
   > **Trap:** do not run Gradle concurrently with another build in this repo — concurrent builds corrupt the
   > checkstyle report.
3. Launch MegaMek. From the **main menu**, `Help > Report a Bug` → Package Bug Report. Expect a zip containing
   `system-info.txt` and the logs, no save, and the "no game is running" note.
4. Host a game with two Meks and a Princess bot, play a round so `gamelog*.html` and `princess.log` exist, then
   package. Expect the save at the root, `logs/megamek.log`, `logs/princess.log`, and exactly one gamelog.
5. Open the zip and confirm the save actually loads — `File > Load Game` on the extracted `.sav.gz`. This is the
   check that proves §2.2's claim that customs need no special handling; **use a custom unit in the test game.**
6. Click each of the MegaMek / MegaMekLab / MekHQ buttons and confirm the browser lands on a filled-in issue form
   with version, OS and Java populated — **not** a 404 (see §7).
7. Test Open Folder and Copy File to Clipboard; paste into a GitHub comment box to confirm the upload starts.
8. Double-blind game with `BASE_DISABLE_LOCAL_SAVE` enabled → confirm the 30 s timeout fires, a logs-only zip is
   still written, and the UI never freezes.

---

## 9. Risks and watch-list

- **`labels` query parameter → 404 for ordinary players** (§3.2). The single most likely way to ship a feature that
  works for every maintainer who tests it and fails for every player who uses it.
- **The save that never completes** (§2.4). Without the timeout the packager hangs with no feedback.
- **`Desktop.browseFileDirectory` is unsupported on Windows.** Use `Desktop.open(parentDirectory)`.
- **Zipping on the EDT** will freeze the client for seconds on a large log directory. Use a `SwingWorker`.
- **Log directory is user-configurable.** Read it from `ClientPreferences`; do not hardcode `"logs"` the way MekHQ
  did.
- **`stampFilenames()` changes the gamelog filename.** Match on the basename, not on an exact `gamelog.html`.
- **Field ids are a contract with three separate repos.** If anyone edits `bug_report.yml` in MegaMek, MegaMekLab or
  MekHQ, the prefill silently stops filling that field (it does not error). Note this in `IssueReportUrl`'s Javadoc —
  §6.2 already does.
- **Do not expand `TWGameManager` or `ClientGUI`.** Nothing in this work order needs to.

---

## 10. Suggested commit sequence (single PR)

1. `BugReportBundle` + `BugReportBundleTest` (self-contained, testable, no UI).
2. `IssueReportUrl` + its test.
3. `AbstractClient`/`Client` save-completion callback (engine plumbing, no behaviour change on its own).
4. `PackageBugReportAction`.
5. `BugReportDialog` + `ShowBugReportDialogAction` + `CommonMenuBar` wiring.
6. `BugReport.properties` (new keys + the `mainText` step-1 correction).
7. Compile fixes and smoke-test notes.

**PR title:** `Add one-click bug report bundle and prefilled issue forms`

**PR label:** add `AI Assisted Development` (standing authorisation). Do **not** use `AI Generated Fix`, and do
**not** append any tool-branding footer to the PR body.

**Pre-PR self-review** (required): `git fetch origin main && git diff main...HEAD --stat`, then read every changed
file as a reviewer would. Specifically check for abbreviated names (`zos`, `fc`, `e` in catch blocks), bare
`true`/`false`/`null` in Javadoc instead of `{@code ...}`, files whose only change is whitespace, and any missing
import that got written as an inline fully-qualified name.

Commit message trailer to use:
```
Co-Authored-By: Claude <noreply@anthropic.com>
```
