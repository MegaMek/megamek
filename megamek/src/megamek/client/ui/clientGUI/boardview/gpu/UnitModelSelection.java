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
        int count = 1;
        String variant = entity instanceof Mek ? UnitModelKey.forEntity(entity) : entity.getShortNameRaw();
        if (entity instanceof BattleArmor armor) {
            int survivors = 0;
            for (int location = 1; location < armor.locations(); location++) {
                if (armor.getInternal(location) > 0) {
                    survivors++;
                }
            }
            count = figures(survivors, 4);
        } else if (entity instanceof Infantry infantry) {
            count = figures(infantry.getActiveTroopers(), 6);
            // Formation artwork follows the unit's motive type, independently of its name or sprite.
            variant = infantry.getMovementMode().name();
        }
        return new BoardScene.UnitModel(asset, tileset.genericModelFor(entity, part),
              variant, count, twist, damage(entity));
    }

    /**
     * A Mek's lost locations, split by how a model shows them: an arm that is gone is taken off, anything else is
     * left in place and burnt out, because the rest of the Mek stands on it or hangs from it. A side torso takes its
     * arm with it in the game, so that arm arrives here as lost too.
     *
     * @return the locations to show as lost; {@link BoardScene.LocationDamage#NONE} for anything but a Mek
     */
    static BoardScene.LocationDamage damage(Entity entity) {
        if (!(entity instanceof Mek mek)) {
            return BoardScene.LocationDamage.NONE;
        }
        Set<String> removed = new TreeSet<>();
        Set<String> wrecked = new TreeSet<>();
        for (int location = 0; location < mek.locations(); location++) {
            if (isLost(mek, location)) {
                (mek.isArm(location) ? removed : wrecked).add(mek.getLocationAbbr(location));
            }
        }
        return (removed.isEmpty() && wrecked.isEmpty()) ? BoardScene.LocationDamage.NONE
              : new BoardScene.LocationDamage(removed, wrecked);
    }

    /**
     * Physically gone, as opposed to {@code isLocationBad}, which also counts a flooded leg that is still attached.
     * A limb blown off this phase stays on until the phase ends, as its damage does everywhere else.
     */
    private static boolean isLost(Mek mek, int location) {
        boolean isDestroyed = mek.isLocationTrulyDestroyed(location);
        boolean isBlownOff = mek.isLocationBlownOff(location) && !mek.isLocationBlownOffThisPhase(location);
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

    /** Square-root compression: 28 soldiers become six figures; five armored troopers become three. */
    static int figures(int survivors, int limit) {
        return Math.min(limit, (int) Math.ceil(Math.sqrt(Math.max(0, survivors))));
    }
}
