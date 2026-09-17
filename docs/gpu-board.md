# Experimental GPU battle view

The Java client has a libGDX/LWJGL3 battle window with shared top and isometric views, smooth movement, and contextual Scene2D controls. From deployment onward, choose **View > GPU Battle View (Experimental)**. Both interfaces have a visible menu bar with the existing File, Game, Board, View, and Help menus. The GPU interface also exposes map selection through Maps.

The two board interfaces are exclusive: the classic window hides once the first 3D frame is ready. Choose **View > Classic Board** in the GPU window to return, or close the GPU window. Its native window and renderer are disposed before the original client is shown again. Switching preserves the existing game, phase controls, orders, and classic window; reopening 3D creates a fresh renderer. Startup failure leaves the classic interface available, and shutting down the game never restores a disposed client.

Launch from this checkout with `./gradlew :megamek:run` (`.\gradlew.bat :megamek:run` on Windows). The build includes the GPU and FreeType native dependencies. One GPU window can be open at a time; it follows the client's selected map.

## Controls

| Control | Action |
| --- | --- |
| View > GPU Battle View / Classic Board | Switch interfaces; only the active board window remains visible |
| Top view / Isometric | Restore the preset angle and bearing while retaining focus and zoom |
| Fit board | Frame the entire board, including cliffs, at the current angle |
| +/-; mouse wheel | Zoom; the wheel anchors zoom to the pointer on the camera's focus plane |
| Right or middle drag | Pan along the board plane |
| Shift + right/middle drag | Rotate through 360 degrees and adjust tilt |
| Camera | Step rotation/tilt, or reset to the isometric preset and fit the board |
| Click a hex or unit; right-click | Open choices anchored beside that location |
| Plot movement here, or a movement mode in All actions | Enter the existing board tool; subsequent clicks plot directly |
| Escape | Close choices or leave the active board tool |
| Ctrl-click, or Measure line of sight | Use the existing two-hex LOS tool |
| All actions / F10 | Search the current phase's commands across every former button page |
| Tab on the board; Enter | Cycle visible units and inspect the focused hex |
| Up/down in a menu; Enter | Navigate choices and activate the focused row |
| Orders; Clear orders; Done/Skip | Inspect the existing order summary, clear through the phase handler, or explicitly commit |
| Hints | Choose Smart, Hold, or Always; remap the hold key (default Space) |
| Speed | Cycle 1x, 0.5x, 2x, and instant movement playback |
| Space during playback, outside menus | Finish all active and queued unit animations without issuing game orders |

Existing scroll, zoom, and overview key bindings control the GPU camera. Other game shortcuts dispatch through the existing client controller. Detailed dialogs remain Swing dialogs. See [contextual-ui.md](contextual-ui.md) for interaction behavior and limits.

The GPU interface reads the current monitor's DPI and window dimensions automatically, including when moved between monitors. Larger windows scale up controls; smaller windows limit scaling so menus remain usable. The existing GUI-scale preference remains an additional adjustment. Text uses a higher-resolution font atlas, and the shared HUD uses the same display scale for drawing and mouse input. Fit board continues fitting during resize; after panning, zooming, or centering a unit, resizing preserves that camera focus and adjusts zoom for DPI changes.

Rotation and tilt use one orthographic orbit camera. Tilt ranges from directly overhead to 15 degrees above the board, keeping the horizon and picking stable. Panning retains the pivot's elevation. Modifier keys choose the gesture when dragging starts; releasing Shift during an orbit does not unexpectedly switch to pan. A short unmodified right-click still opens the hex's contextual menu, while dragging away and back never issues a context click. Losing window focus cancels the gesture. Camera controls do not issue game orders or rebuild terrain/shadow meshes.

## Board capability migration

Both renderers now call the same terrain and tactical painters. The GPU adapter captures their output on the Swing event thread and projects it onto the corresponding hex surfaces. This preserves existing visibility, preferences, sprite handlers, and calculations without reimplementing them.

| Existing source | Content carried into the GPU board |
| --- | --- |
| `BoardView.drawHexes` | Terrain/feature artwork, shadows, field-of-view shading, hex text, wrecks, special hex artwork, deployment markings, and the behind-terrain sprite collection |
| Existing behind/over-terrain sprite collections | Movement envelopes, firing arcs and solutions, sensor ranges, ECM and other area highlights, objectives, collapse warnings, and other registered sprite handlers |
| Shared `drawTacticalLayers` | Minefields, demolition charges, artillery/orbital markers, selection and LOS cursors, deployment areas, C3 links, flyover/VTOL paths, ghost sprites, attack arrows, artillery drift, vector movement, strafing, movement paths, flight indicators, and ruler |
| Existing `IDisplayable` overlays | Screen-anchored board widgets and their mouse hit/drag/release handlers |
| Existing tooltip provider | Hex/unit details, available from the contextual Details action |
| Existing phase displays and `MapMenu` | Deployment, movement, targeting, firing, physical attacks, and special contextual operations through their original handlers |

Actual unit artwork and annotations are excluded from captured tile layers. Units are flat, alpha-shaped extruded tokens: the supplied sprite textures the top, and the sides use its alpha-weighted average visible color. Generated unit drop shadows and smoke are excluded from these textures, preserving the unit silhouette. Camouflage and damage marks remain. Surface normals light the top and sides.

Token thickness is `(entity.height() + 1) * BoardGeometry.LEVEL`, so one occupied height level equals one terrain elevation level. This uses the existing zero-based LOS height convention, including current stance. The present game implementation reports prone superheavy Meks as one occupied level, not two; the renderer does not override that rule. Sensor contacts always use a generic one-level token and reveal no actual height or unit status.

Labels, armor/internal bars, and status indicators reuse the existing entity painter but float above the animated token in screen space. Text is rasterized at four times its logical resolution, independently of classic-board zoom, and stays at a minimum size equivalent to a 14-pixel label font before display scaling. Overlapping labels move to nearby free screen space, with selected and hovered units placed first. `SPREAD_UNIT_ANNOTATIONS` in `GpuBattleView` enables this layout by default; disabling it keeps labels at their anchors without suppressing overlaps. Labels stay within the viewport and remain drawn even when it is too crowded to separate every label; selected and hovered labels are drawn on top. Both views render to the native framebuffer; texture sampling is linear. Source artwork remains raster art, so extreme close-ups cannot reveal detail absent from the shipped images.

Known units outside the camera view retain their annotations, clamped to the left, right, top, or bottom edge of the board viewport. `UNIT_ANNOTATION_MAX_OFFSCREEN_DISTANCE` in `GpuBattleView` controls when to drop them: `-1` (default) disables dropping, `0` drops annotations as soon as their projected anchor leaves the viewport, and positive values allow that many logical screen pixels beyond the viewport, multiplied by display scaling. Distance is measured to the nearest viewport point before clamping or spreading, including diagonally at corners. Model culling does not remove labels; normal game visibility still controls which units are available to the renderer.

Hex coordinates and level/depth/building-height/foliage labels are not baked into tile artwork. The shared board code publishes their text, color, font size, and baseline; the GPU draws them on the hex surface using the high-resolution native font atlas. This keeps those labels sharp at maximum zoom and prevents them from being stretched onto cliff walls. `hex-text-closeup.png` records the native close-up check. Other legacy tactical symbols and the invalid-hex marker still use captured pixels.

`UnitMotion` allocates a fixed playback budget per event path: `WALK_SECONDS = 1.2`, `RUN_SECONDS = 0.8`, and `JUMP_SECONDS = 1.0`. Longer walking/running paths traverse more steps within the same budget and therefore move faster. Speed controls scale elapsed playback time. Jumping follows a single parabolic takeoff-to-landing arc, with clearance derived from the copied path and `JUMP_MIN_LEVELS` / `JUMP_LEVELS_PER_HEX`. Positions, facing, labels, and real shadows follow that same timeline. Playback and skipping never modify authoritative entity state.

Hex elevations and exposed cliff faces are mesh geometry. Buildings, woods, and bridge artwork remain projected tile artwork; buildings and bridge decks are not separately extruded objects and cannot cast volumetric shadows. This matters for the appearance of units at roof or bridge elevation. Their former generated 2D shadows are no longer baked into GPU captures. Shading already painted into source assets cannot be removed automatically.

Terrain and unit tokens cast and receive the same GPU shadow map in both camera views, including off-screen casters. The depth pass updates when terrain, lighting, visible unit geometry, or animated transforms change; camera-only changes reuse it. Surface normals provide directional lighting and a receiver-depth bias limits self-shadowing artifacts. Light direction and the shadow preference come from the existing terrain-shadow helper. GPU captures omit classic generated drop shadows and hex ambient-occlusion shading. The map covers the whole board at 2048 or 4096 pixels; very large boards have less shadow detail.

## One source of truth

- `GpuBoardSource` reads the selected `BoardView`, game, visibility helpers, tileset, phase controls, and planned movement on the Swing event thread. It atomically publishes immutable presentation frames. No game objects are read by the rendering thread.
- `BoardView.capturePlanarHexes` captures native-resolution artwork in bounded 16 x 16-hex chunks, independent of classic zoom. It restores classic scale, zoom index, dimensions, caches, shadows, fonts, and sprite preparation afterward. Visible hexes refresh at approximately 10 Hz; large boards are no longer downsampled to a single 2048-pixel canvas.
- `BoardGeometry` supplies terrain coordinates, cliff edges, and picking for both cameras. `UnitMotion` interpolates copied event paths using elapsed time; it never changes the authoritative entity position or movement path.
- `GpuTextures` owns the shared atlas implementation for terrain, tactical layers, units, and the HUD. Pixel-only changes update existing texture slots. `GpuTerrain` remeshes only when geometry or atlas layout changes and culls terrain chunks outside the camera. `GpuMeeple` caches alpha-shaped meshes per artwork, while each displayed unit part owns its transform; stance changes scale those meshes without rebuilding them.
- `GpuBoardActions` describes the actual phase buttons, menu entries, weapon list, and ammunition models. It creates no duplicate buttons or legality rules. Callbacks recheck the owning panel, phase, turn, actor, and live target/control availability.
- `GpuBoardUi` presents those descriptions through Scene2D. The global and contextual menus, search, explicit completion controls, and hints have no independent game state.

This is a hybrid renderer: Java2D still produces shared layer pixels; OpenGL handles projection, batched drawing, camera changes, and unit interpolation. Layer refresh cost and initial image loading can still stall presentation. It is not a native GPU rewrite of every annotation primitive. Development rules are in [AGENTS.md](../AGENTS.md).

## Critical review and verification

Review fixes included read-only menu construction, acting-versus-inspected unit confusion, stale targets, wrong-board input dispatch, incorrect drag/click constants, menu clicks leaking to the board, keyboard/search navigation, focus-loss cleanup, sensor-contact disclosure, classic isometric state restoration, atomic frame publication, atlas churn, and duplicated movement-path drawing.

The resize failure was an atlas packing boundary error: libGDX's guillotine packer requires three padding margins beyond the largest image dimension. The shared atlas now reserves that space for all users, including large HUD images. Resize handling ignores zero-size windows and updates the UI, camera, HUD capture dimensions, and input transform together.

Run the focused checks from the checkout root:

```text
./gradlew :megamek:test --tests 'megamek.client.ui.clientGUI.boardview.gpu.*' --tests 'megamek.client.ui.clientGUI.boardview.LOS*' :megamek:checkstyleMain :megamek:checkstyleTest :megamek:spotlessCheck
./gradlew :megamek:gpuBoardSmoke
```

The regular tests cover motion and picking through a full rotation and the tilt limits, grounded panning, pointer-anchored zoom, fitting cliff geometry at low angles, camera reset, DPI/resolution scaling, responsive camera fitting and focus preservation, visibility and sensor contacts, immutable image reuse, layer appearance and clearing, classic projection restoration, overlay input consumption, command expiry, stacked targets, off-page commands, and the original weapon/ammunition models. Existing LOS calculation regressions are included in the review command.

The native smoke tests require desktop OpenGL. They render shipped terrain and unit sprites, exercise actual Scene2D input and keyboard search, verify movement while the game already holds the final position, and check OpenGL errors. They also render deployment/range markings and a 48 x 51 board with 36 units, ranges, an attack line, and cursor updates. Window lifecycle checks cover repeated resizing from 900 x 600 through 3840 x 2160, scaled HUD input, exclusive visibility, shared menu switching, GPU menu zoom, reopening, native close, and client disposal. Atlas tests cross the original 2043-pixel failure boundary in both axes and upload a 3840 x 2160 image. Shadow tests sample rendered pixels to verify that reversing the light moves the shadow, lowering the caster removes it, and disabling shadows clears the map. Screenshots and timing files are saved to `megamek/build/gpu-board-review/`.

On Windows with Intel Iris Xe at 1280 x 800, the large workload with hex shadows averaged approximately **60 FPS after warm-up** at the current 60 FPS/vsync cap. The recorded 95th-percentile frame time was **20.0 ms**. Initial loading and resize transitions are excluded. There is no classic-renderer baseline, so this does not establish a speedup. Linux, macOS, and physical transitions between monitors with different DPI have not been tested; the scale calculation has regression coverage for those pixel ratios.

The camera interaction test drives Shift-drag and plain drag through the native input processor, selects a raised hex after orbiting, exercises the Camera menu and reset, and verifies that a drag returning to its start or losing focus does not issue orders. `orbit.png`, `orbit-context.png`, and `camera-menu.png` show those checks.

Additional checks cover shadow-free GPU texture capture, zoom-independent high-resolution labels and terrain, current entity heights, alpha-weighted side colors, fixed path budgets, queued movement, jump clearance and exact landing, and Space-to-skip through native input. Native pixel tests verify that a meeple casts a shadow, reversing the light moves it and changes side brightness, and moving the token clears its old shadow. `meeple-shadow.png` records the controlled lighting scene.

## Remaining validation

The shared painter path covers the existing board layers; it does not constitute a human playthrough of every aerospace, artillery, transport, multi-map, bridge, and special-equipment workflow. The native view remains experimental while those combinations are exercised. Screen widgets are shared, but chat text entry and detailed dialogs still use the Swing client. Some disabled legacy commands supply only a generic availability reason. Hover previews and an editable per-order ledger are not implemented.

The original client stays alive while hidden. Closing the source board disposes the GPU window and its resources without bringing the old interface back.
