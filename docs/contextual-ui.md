# Contextual battle UI

Implemented in the experimental libGDX battle window. Tactical commands are accessed through a labeled menu beside a clicked hex or unit and through searchable **All actions**. The GPU window has no paged action sidebar. The Swing client's original controls remain available for detailed dialogs and as a fallback.

## Interaction

Clicking or inspecting a location opens choices without changing the acting unit, selecting a firing target, or submitting orders. The menu uses the current phase and the existing `MapMenu` entries. Multiple units in one hex retain distinct target choices. Selecting a target is explicit; firing and physical attacks use the existing handlers and confirmation behavior.

Movement is a persistent tool: choose **Plot movement here** or an existing movement mode, then continue plotting on the board. Escape leaves the tool and right-click opens contextual choices. Weapon, ammunition, and target menus stay open while preparing a salvo. **Done/Skip** remains separate and explicit; clicking a target never commits the turn.

The bottom strip keeps the acting unit, All actions, Orders, Clear orders, and original completion actions accessible. Orders displays the existing completion-control summaries and movement destination. It is not a new attack model or an editable per-order ledger. Clear orders invokes the phase's original clear operation; there is no promise of universal undo for immediate actions.

Clicking a unit portrait in the side overview centers the active board camera through the client's existing unit-location handler, even when the phase cannot select that unit to act. The original selection event still runs. GPU centering preserves zoom and view angle, and pending centering requests survive opening the window or switching boards.

Both interfaces expose the existing File, Game, Board, View, and Help headings in a visible menu bar. The GPU interface adds Maps. **View > GPU Battle View (Experimental)** hides the classic interface after the first 3D frame is ready. **View > Classic Board**, or closing the native window, disposes the GPU renderer and restores the original interface. Only one board window is visible at a time; both use the same game and original commands. Camera menu entries act on the active renderer.

Commands without a map target remain in All actions or their global menu. Global commands recheck the live menu's availability without tying switching or settings to an old acting unit or phase. The target's existing tooltip content is available through **Details**. Hover previews of prospective attacks or movement are not added; existing plotted paths and tactical overlays provide current game feedback.

## Presentation and discoverability

The GPU interface reuses MegaMek's configured skin through `SkinXMLHandler`. Buttons use the same normal and pushed images as `MegaMekButton`, including the Bloodwolf default's brushed metal faces and chrome frames. Nine-patch drawing keeps the frame thickness fixed as command rows grow, with text and icons inset inside the recessed face. Panels tile the existing phase background, and their borders are painted by `MegaMekBorder` from the same skin XML. Noto Sans text, neutral colors, and small silver focus marks keep the extended controls consistent with the classic client. Font textures are rasterized above their display size for DPI scaling.

Button textures are used as authored, including their corner shapes, highlights, and alpha. Changes to the silhouette belong in the texture; the renderers do not apply geometric corner masks. Classic image-skinned buttons suppress the extra look-and-feel border where the skin specifies no border, so it cannot paint into the texture's transparent corners.

Context menus have a hex/unit identity header, the current phase and terrain elevation, a searchable command list, and a keyboard legend. Command rows combine an embossed metal symbol, a wrapped name, an inline summary of the existing explanation, and a submenu arrow or unavailable marker. Intelligence and general controls have their own separator. Full explanations remain available as skinned tooltips. Details expands the existing board tooltip inside an inset intelligence panel. Additional command symbols and matte recessed wells are generated once per window. Search has a thin steel rim, an inset search icon, and an empty-field hint that remains visible during keyboard focus.

The HUD uses the same materials and typography for global menus, camera controls, tuning, and the bottom unit strip. The acting-unit card displays existing visible artwork, name, and hex location when available. Anonymous sensor contacts do not supply a portrait. Done/Skip remain explicit completion controls and use the classic completion-button skin. Map dimensions and the current phase remain visible without placing renderer diagnostics in the command bar. The GPU skin is loaded when the window opens; reopen it after changing the classic skin. Skins without button or panel images use the shipped Bloodwolf artwork for those surfaces.

Expandable groups, search, scrolling, and viewport clamping keep large command trees usable. Search traverses the same command tree; keyboard navigation skips disabled rows and preserves focus across updates when the command remains available. Changing a search or opening another menu resets scrolling. Disabled controls retain their supplied explanation, with a generic explanation when none exists. The UI does not invent a rule or cost.

The acting unit, hovered visible units, and hovered hex retain normal selection outlines. Movement envelopes, ranges, and phase controls show the existing game rules and availability. Hidden units are excluded and sensor contacts retain anonymous presentation. There is no separate interaction-hints mode or reveal key.

Tab cycles visible units on the board and Enter opens their context. Menus support up/down navigation and Enter activation, action search, and Escape dismissal. F10 opens All actions. Scene2D automatically scales for the current monitor's DPI and window size, with the configured GUI scale as an additional adjustment. Layout limits keep menus within the window; shared HUD drawing and input use the same scale. Instant movement playback is available as an alternative to animation.

## Integration without duplicate logic

The Camera menu provides rotation and tilt steps plus an isometric reset. Shift + right/middle-drag adjusts both angles directly, right/middle-drag pans, and the wheel zooms. These gestures retain the same contextual picking and tactical overlays at every angle. The footer and Camera tooltip show the controls; short unmodified right-clicks still open choices. See [gpu-board.md](gpu-board.md) for camera limits and verification.

`StatusBarPhaseDisplay.getActionButtons()` exposes the existing button list across all pages, excluding the More paging command. The adapter also reads visible controls from other phase panels and the original Done/Skip controls. It never constructs a parallel set of phase buttons.

`MapMenu` construction is now read-only. Finding a contextual target no longer replaces the acting entity or calls a phase target method. The classic popup still explicitly selects its target when shown and then rebuilds its choices. GPU target and physical-attack actions select the intended target only when activated. Contextual fire availability checks that the current firing target belongs to the inspected hex.

Weapon and ammunition choices adapt the actual WeaponPanel list and combo-box models. To-hit information comes from the existing target summary. Commands use the original action IDs and listeners. A callback verifies its phase panel, turn index, acting unit, and live control/model; contextual callbacks rebuild the current menu before lookup so a newly hidden or unavailable target cannot execute an old choice.

Snapshots cross from Swing to the render thread atomically. Rendering and navigation happen in Scene2D; callbacks return to Swing. Board input uses the picked board and hex, and consumes complete overlay gestures to prevent a UI click from also plotting an order. Game rules, command availability, and acting-unit state stay in the existing client.

## Review and limits

Regression tests cover read-only inspection, acting versus inspected units, target-before-fire behavior, stale actor and target choices, stacked units, commands on hidden pages, and original weapon/ammunition selection. The native test exercises menu input without board-click leakage, search, keyboard activation, and actual board-tool dispatch. Renderer validation and measured workloads are documented in [gpu-board.md](gpu-board.md).

`GpuBoardUiSmokeTest` uses deterministic command snapshots over the real board renderer to check submenu scrolling, reverse keyboard navigation, search, disabled and newly revoked commands, explicit board-tool activation, separate completion actions, and menu bounds at 900 × 600. It writes `tactical-context.png`, `tactical-context-small.png`, and `tactical-actions-small.png` to `megamek/build/gpu-board-review`. These fixtures validate presentation and input; the adapter tests validate the actual game callbacks.

`MegaMekButtonTest` checks that actual Swing button painting preserves both the texture's transparent outer pixels and its opaque metal corners in enabled, disabled, and pressed states.

Full gameplay testing across all phases and usability sessions with new and experienced players remain necessary before replacing the classic view by default. The current implementation does not add a separately pinned weapon panel, new predictive hover cards, individually editable pending orders, or detailed explanations that the existing controls do not already provide. Chat typing and detailed dialogs still use the Swing client.
