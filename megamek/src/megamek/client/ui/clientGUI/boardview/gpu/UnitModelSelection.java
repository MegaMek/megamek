/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.Set;
import java.util.TreeSet;

import megamek.client.ui.tileset.MekTileset;
import megamek.client.ui.tileset.UnitModelKey;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;

/** Presentation only: the tileset owns identity and the unit owns its surviving personnel. */
final class UnitModelSelection {
    private UnitModelSelection() { }

    /** Reuse the tileset and captured loadout/camo for a historical form; no alternate assembly or Entity copy. */
    static BoardScene.UnitModel inForm(BoardScene.UnitModel model, Entity entity, int part, MekTileset tileset,
          megamek.common.units.UnitLocation.Form form) {
        if (model == null || model.state() == null || form == null) { return model; }
        var state = model.state();
        var structure = state.structure();
        var body = structure.bodyForm();
        var pose = state.pose();
        var movement = entity instanceof megamek.common.units.QuadVee ? megamek.common.units.EntityMovementMode.QUAD : form.movement();
        var captured = new UnitModelState(new UnitModelState.Structure(movement, structure.equipment(),
              structure.members(), structure.activeTroopers(), structure.externalSearchlight(), structure.anatomy(),
              body == null ? null : new UnitModelState.BodyForm(movement.name(), body.size(), body.turrets(), body.fighters())),
              state.appearance(), new UnitModelState.Pose(pose.proneCause(), pose.facing(), pose.secondaryFacing(), form,
                    pose.dead(), pose.armsFlipped(), pose.hullDown()));
        return new BoardScene.UnitModel(tileset.modelFor(entity, part, form), tileset.genericModelFor(entity, part, form),
              model.variant(), model.figures(), model.twist(), model.damage(), captured);
    }

    static BoardScene.UnitModel capture(Entity entity, int part, boolean sensor, MekTileset tileset) {
        return capture(entity, part, sensor, tileset, 0);
    }

    /**
     * @param twist hexsides the displayed facing is turned clockwise from the unit's own facing, see
     *              {@link #twist(int, int)}
     */
    static BoardScene.UnitModel capture(Entity entity, int part, boolean sensor, MekTileset tileset, int twist) {
        if (sensor) {
            return null;
        }
        String asset = tileset.modelFor(entity, part);
        if (asset == null) {
            return null;
        }
        // Support assets may reuse an infantry tileset entry without exposing a personnel count.
        UnitModelState state = UnitModelState.capture(entity);
        int count = 1;
        String variant = entity instanceof Mek ? UnitModelKey.forEntity(entity) : entity.getShortNameRaw();
        if (entity instanceof BattleArmor) {
            count = BattleArmorVisual.figures(state.structure().activeTroopers());
        } else if (entity instanceof Infantry infantry) {
            count = figures(state.structure().activeTroopers(), 6);
            // Formation artwork follows the unit's motive type, independently of its name or sprite.
            variant = infantry.getMovementMode().name();
        }
        return new BoardScene.UnitModel(asset, tileset.genericModelFor(entity, part),
              variant, count, twist, damage(entity), state);
    }

    /**
     * A Mek's lost locations: blown-off parts and destroyed arms disappear; other destroyed locations remain
     * burnt out in place. A lost side torso
     * takes its arm with it. Combat damage records that arm as destroyed too, but the damage editor can zero a torso
     * and leave the arm's own numbers alone, so the arm is worked out here instead of trusted to be recorded.
     *
     * @return the locations to show as lost; {@link BoardScene.LocationDamage#NONE} for anything but a Mek
     */
    static BoardScene.LocationDamage damage(Entity entity) {
        if (entity instanceof Infantry) { return BoardScene.LocationDamage.NONE; }
        if (!(entity instanceof Mek mek)) {
            float original = 0, remaining = 0;
            for (int location = 0; location < entity.locations(); location++) {
                original += armor(entity, location, true) + Math.max(0, entity.getOInternal(location));
                remaining += armor(entity, location, false) + Math.max(0, entity.getInternal(location));
            }
            var stage = UnitDamageDisplay.bodyStage(loss(remaining, original));
            return stage == null ? BoardScene.LocationDamage.NONE
                  : new BoardScene.LocationDamage(Set.of(), Set.of(), java.util.Map.of("*", stage));
        }
        Set<String> removed = new TreeSet<>();
        Set<String> wrecked = new TreeSet<>();
        var stages = new java.util.HashMap<String, UnitDamageDisplay.Stage>();
        for (int location = 0; location < mek.locations(); location++) {
            if (isLost(mek, location)) {
                addLost(mek, location, removed, wrecked);
                int dependent = mek.getDependentLocation(location);
                if (dependent != Entity.LOC_NONE) {
                    addLost(mek, dependent, removed, wrecked);
                }
            }
            var stage = UnitDamageDisplay.locationStage(loss(armor(mek, location, false), armor(mek, location, true)),
                  loss(mek.getInternal(location), mek.getOInternal(location)));
            if (stage != null) { stages.put(mek.getLocationAbbr(location), stage); }
        }
        return removed.isEmpty() && wrecked.isEmpty() && stages.isEmpty() ? BoardScene.LocationDamage.NONE
              : new BoardScene.LocationDamage(removed, wrecked, stages);
    }

    private static float armor(Entity entity, int location, boolean original) {
        float value = Math.max(0, original ? entity.getOArmor(location) : entity.getArmor(location));
        if (entity.hasRearArmor(location)) {
            value += Math.max(0, original ? entity.getOArmor(location, true) : entity.getArmor(location, true));
        }
        return value;
    }

    private static float loss(float remaining, float original) {
        return original <= 0 ? 0 : 1 - Math.clamp(remaining / original, 0, 1);
    }

    private static void addLost(Mek mek, int location, Set<String> removed, Set<String> wrecked) {
        (mek.isLocationBlownOff(location) || mek.isArm(location) ? removed : wrecked)
              .add(mek.getLocationAbbr(location));
    }

    /**
     * Physically gone, as opposed to {@code isLocationBad}, which also counts a flooded leg that is still attached.
     * Show a confirmed detachment immediately, matching the game's ground remains. This changes no phase rules.
     */
    private static boolean isLost(Mek mek, int location) {
        boolean isDestroyed = mek.isLocationTrulyDestroyed(location);
        boolean isBlownOff = mek.isLocationBlownOff(location);
        return isDestroyed || isBlownOff;
    }

    /**
     * The classic sprite shows a Mek at its torso facing, so a twisted Mek appears to turn its legs as well. A model
     * with a separate upper body can keep the legs where they are, which needs the difference between the two.
     *
     * @param facing          the unit's own facing (a Mek's legs), in hexsides
     * @param displayedFacing the facing the unit is shown at (a Mek's torso), in hexsides
     *
     * @return the shortest turn from {@code facing} to {@code displayedFacing} in hexsides, clockwise positive, from
     *       {@code -2} to {@code 3}; {@code 0} when either facing is not set
     */
    static int twist(int facing, int displayedFacing) {
        if ((facing < 0) || (displayedFacing < 0)) {
            return 0;
        }
        int clockwise = Math.floorMod(displayedFacing - facing, 6);
        return (clockwise > 3) ? clockwise - 6 : clockwise;
    }

    /** Conventional-infantry compression: 28 soldiers become six figures. BA owns its independent policy. */
    static int figures(int survivors, int limit) {
        return Math.min(limit, (int) Math.ceil(Math.sqrt(Math.max(0, survivors))));
    }
}
