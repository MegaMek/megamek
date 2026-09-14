# Contextual battle UI

Implemented in the experimental libGDX battle window. Tactical commands are accessed through a labeled menu beside a clicked hex or unit and through searchable **All actions**. The GPU window has no paged action sidebar. The Swing client's original controls remain available for detailed dialogs and as a fallback.

## Interaction

Clicking or inspecting a location opens choices without changing the acting unit, selecting a firing target, or submitting orders. The menu uses the current phase and the existing `MapMenu` entries. Multiple units in one hex retain distinct target choices. Selecting a target is explicit; firing and physical attacks use the existing handlers and confirmation behavior.

Movement is a persistent tool: choose **Plot movement here** or an existing movement mode, then continue plotting on the board. Escape leaves the tool and right-click opens contextual choices. Weapon, ammunition, and target menus stay open while preparing a salvo. **Done/Skip** remains separate and explicit; clicking a target never commits the turn.

The bottom strip keeps the acting unit, All actions, Orders, Clear orders, and original completion actions accessible. Orders displays the existing completion-control summaries and movement destination. It is not a new attack model or an editable per-order ledger. Clear orders invokes the phase's original clear operation; there is no promise of universal undo for immediate actions.

Both interfaces expose the existing File, Game, Board, View, and Help headings in a visible menu bar. The GPU interface adds Maps. **View > GPU Battle View (Experimental)** hides the classic interface after the first 3D frame is ready. **View > Classic Board**, or closing the native window, disposes the GPU renderer and restores the original interface. Only one board window is visible at a time; both use the same game and original commands. Camera menu entries act on the active renderer.

Commands without a map target remain in All actions or their global menu. Global commands recheck the live menu's availability without tying switching or settings to an old acting unit or phase. The target's existing tooltip content is available through **Details**. Hover previews of prospective attacks or movement are not added; existing plotted paths and tactical overlays provide current game feedback.

## Presentation and discoverability

Menus use a compact labeled list with expandable groups, a search field, scrolling, disabled-state details, and a position clamped inside the viewport. Search traverses the same command tree. Disabled legacy controls retain their supplied explanation; when none exists, the UI shows a generic availability message rather than inventing a rule or cost.

Interaction hints have three persisted modes:

- **Smart:** outlines the acting and hovered visible units; the hovered hex is outlined for inspection.
- **Hold:** reveals visible-unit outlines and inspectable hexes while the bound key is held. The default is Space; the Hints menu remaps it.
- **Always:** keeps the same reveal overlay on without holding a key.

These marks identify locations that can be inspected. They do not assert that every highlighted hex is a legal destination or attack target; the existing movement envelopes, ranges, and phase controls retain that meaning. Hidden units are excluded and sensor contacts retain anonymous presentation. The held state and repeated keys reset when the GPU window loses focus.

Tab cycles visible units on the board and Enter opens their context. Menus support up/down navigation and Enter activation, action search, and Escape dismissal. F10 opens All actions. Scene2D uses the existing Noto Sans font at a higher raster resolution and automatically scales for the current monitor's DPI and window size, with the configured GUI scale as an additional adjustment. Layout limits keep menus within the window; shared HUD drawing and input use the same scale. Instant movement playback is available as an alternative to animation.

## Integration without duplicate logic

The Camera menu provides rotation and tilt steps plus an isometric reset. Shift + right/middle-drag adjusts both angles directly, right/middle-drag pans, and the wheel zooms. These gestures retain the same contextual picking and tactical overlays at every angle. The footer and Camera tooltip show the controls; short unmodified right-clicks still open choices. See [gpu-board.md](gpu-board.md) for camera limits and verification.

`StatusBarPhaseDisplay.getActionButtons()` exposes the existing button list across all pages, excluding the More paging command. The adapter also reads visible controls from other phase panels and the original Done/Skip controls. It never constructs a parallel set of phase buttons.

`MapMenu` construction is now read-only. Finding a contextual target no longer replaces the acting entity or calls a phase target method. The classic popup still explicitly selects its target when shown and then rebuilds its choices. GPU target and physical-attack actions select the intended target only when activated. Contextual fire availability checks that the current firing target belongs to the inspected hex.

Weapon and ammunition choices adapt the actual WeaponPanel list and combo-box models. To-hit information comes from the existing target summary. Commands use the original action IDs and listeners. A callback verifies its phase panel, turn index, acting unit, and live control/model; contextual callbacks rebuild the current menu before lookup so a newly hidden or unavailable target cannot execute an old choice.

Snapshots cross from Swing to the render thread atomically. Rendering and navigation happen in Scene2D; callbacks return to Swing. Board input uses the picked board and hex, and consumes complete overlay gestures to prevent a UI click from also plotting an order. No second rules engine, command registry, asset catalog, or game model is introduced.

## Review and limits

Regression tests cover read-only inspection, acting versus inspected units, target-before-fire behavior, stale actor and target choices, stacked units, commands on hidden pages, and original weapon/ammunition selection. The native test exercises menu input without board-click leakage, search, keyboard activation, and actual board-tool dispatch. Renderer validation and measured workloads are documented in [gpu-board.md](gpu-board.md).

Full gameplay testing across all phases and usability sessions with new and experienced players remain necessary before replacing the classic view by default. The current implementation does not add a separately pinned weapon panel, new predictive hover cards, individually editable pending orders, or detailed explanations that the existing controls do not already provide. Chat typing and detailed dialogs still use the Swing client.
