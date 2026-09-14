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

Actual unit artwork is excluded from the captured layers and rendered separately by the GPU for smooth movement. Sprites lie **flat in the hex plane**, including in isometric view; they rotate around the board's vertical axis. The same original sprites are used. Real unit elevation is retained, with only a small depth offset to avoid flickering against the ground.

Hex elevations and exposed cliff faces are mesh geometry. Buildings, woods, and bridge artwork remain projected tile artwork; buildings and bridge decks are not separately extruded objects. This matters for the appearance of units at roof or bridge elevation.

Elevated hexes cast GPU shadow maps onto lower terrain in both camera views. The depth pass reuses the ground meshes, including off-screen casters, and runs only when terrain geometry or the light changes. Surface normals light the cliff faces, and a receiver-depth bias prevents flat terrain from shadowing itself. The light direction and shadow preference come from the existing terrain-shadow helper. Captured artwork retains its woods/building/bridge shadows but omits baked elevation shadows, preventing duplicate hill shadows. The map covers the whole board at 2048 or 4096 pixels; very large boards have less shadow detail.

## One source of truth

- `GpuBoardSource` reads the selected `BoardView`, game, visibility helpers, tileset, phase controls, and planned movement on the Swing event thread. It atomically publishes immutable presentation frames. No game objects are read by the rendering thread.
- `BoardView.capturePlanarHexes` temporarily uses a planar presentation, restores the classic projection and sprite preparation afterward, and bounds each capture canvas. Visible hexes refresh at approximately 10 Hz. Discrete resolution levels keep texture slot dimensions stable while panning.
- `BoardGeometry` supplies terrain coordinates, cliff edges, and picking for both cameras. `UnitMotion` interpolates copied event paths using elapsed time; it never changes the authoritative entity position or movement path.
- `GpuTextures` owns the shared atlas implementation for terrain, tactical layers, units, and the HUD. Pixel-only changes update existing texture slots. `GpuTerrain` remeshes only when geometry or atlas layout changes and culls terrain chunks outside the camera.
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

## Remaining validation

The shared painter path covers the existing board layers; it does not constitute a human playthrough of every aerospace, artillery, transport, multi-map, bridge, and special-equipment workflow. The native view remains experimental while those combinations are exercised. Screen widgets are shared, but chat text entry and detailed dialogs still use the Swing client. Some disabled legacy commands supply only a generic availability reason. Hover previews and an editable per-order ledger are not implemented.

The original client stays alive while hidden. Closing the source board disposes the GPU window and its resources without bringing the old interface back.
