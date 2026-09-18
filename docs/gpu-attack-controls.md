# Attack controls in the GPU board

The GPU board shows an attack console on the right during an acting unit's attack
phase. It uses the same fonts and frame as the rest of the board UI, with flat
weapon rows and an amber firing control. The information area scrolls on small
windows while the firing buttons stay in place. Both camera views use this panel.

For weapon attacks:

1. Click a hex and choose its target from the contextual commands. Stacked units
   remain separate choices. **Next Target** also uses the existing target cycle.
2. Select a weapon in the console. Its statistics, target range, hit chance,
   modifiers, and any reason it cannot fire come from the Swing weapon display.
3. Use the ammunition or bay-weapon selector when available. **Mode**, **Called**,
   **Twist**, and **Flip Arms** are the existing phase commands. Other controls
   are available under **Controls**.
4. **Fire weapon** queues the selected attack and follows the existing automatic
   weapon selection behavior. Review the queued attacks in the console or the
   bottom **Orders** menu. **Clear** cancels pending orders; **Done Firing** submits
   them using the original completion control.

Targeting and offboard weapon phases use the same weapon controls. Physical
phases show their target, pending orders, and existing physical action buttons.
Specialized dialogs, including aiming, turret facing, and physical-attack
confirmations, remain Swing dialogs raised by the existing GPU window bridge.

The Swing event thread owns all game access and attack calculations. It publishes
immutable presentation snapshots to the render thread. Native controls invoke
the existing buttons and selection models on Swing, with live checks for the
acting unit, phase, turn, availability, and weapon/ammunition selection. The
native panel never constructs or submits its own attack actions.

Weapon arcs and range boundaries use translucent, narrow 3D walls with a bright
top edge. They rise two levels above the adjoining hex ridges and extend down
the cliff sides; shared corners meet even where the elevation changes. The
existing field-of-fire handler supplies the exact boundaries and configured
range colours. Sensor ranges, deployment markers and objective zones retain
their ground presentation.

Queued attack arrows connect the attacker and target at their occupied heights.
Direct shots, including direct artillery, are straight, depth-tested 3D lines.
Indirect orders use a curved visual trajectory above intervening terrain and structures. This
curve illustrates the order; it does not simulate projectile physics or change
LOS or attack legality. Multiple weapons share a line, with separate straight
and curved lines when both modes attack the same target. These graphics are
excluded from the per-hex ground textures, so they no longer jump between levels.

`GpuBoardActionsTest` covers presentation ownership and stale selections.
`GpuAttackSmokeTest` exercises the real firing display through native input,
including target selection, firing, clearing, and layouts in both camera views.
Run the latter with `:megamek:gpuBoardSmoke --tests '*GpuAttackSmokeTest'` on a
machine with a desktop OpenGL context; it saves screenshots under
`megamek/build/gpu-board-review`.

`BoardFiringGeometryTest` checks terrain clearance, exact endpoints and ridge
joins. `GpuFiringCaptureTest` checks attack modes, elevation, clearing and planar
layer separation. `GpuFiringSmokeTest` captures the native volumes on a stepped
board in both cameras, and can be run with
`:megamek:gpuBoardSmoke --tests '*GpuFiringSmokeTest'`.
