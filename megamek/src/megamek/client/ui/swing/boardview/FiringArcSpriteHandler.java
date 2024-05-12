/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.boardview;

import megamek.client.ui.swing.*;
import megamek.common.*;
import megamek.common.equipment.WeaponMounted;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.weapons.infantry.InfantryWeapon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This BoardViewSpriteHandler handles the sprites for the firing arcs (field of fire) that can be shown for
 * individual weapons or bays. The field of fire depends on a handful of variables (position, unit facing
 * and twists, weapon arc and range). These variables can and must be set individually and are cached.
 */
public class FiringArcSpriteHandler extends BoardViewSpriteHandler implements IPreferenceChangeListener {

    private static final String[] rangeTexts = {"min", "S", "M", "L", "E"};

    private final Game game;
    private final ClientGUI clientGUI;

    // In this handler, the values are cached because only some of the values are updated by method calls at a time
    private Entity firingEntity;
    private Coords firingPosition;
    private int facing;
    private int arc;
    private boolean isUnderWater = false;
    private final int[][] ranges = new int[2][5];

    public FiringArcSpriteHandler(BoardView boardView, ClientGUI clientGUI) {
        super(boardView);
        this.clientGUI = clientGUI;
        game = clientGUI.getClient().getGame();
    }

    /**
     * Updates the facing that is used for aligning the field of fire.
     * Does not change the assumed unit, position weapon and arc.
     *
     * @param facing The unit's facing
     */
    public void updateFacing(int facing) {
        this.facing = facing;
        renewSprites();
    }

    /**
     * Updates the position that is used for centering the field of fire to the given Coords.
     * Does not change the assumed unit, weapon and arc, nor the facing.
     *
     * @param firingPosition The position to center the field of fire on
     */
    public void updatePosition(Coords firingPosition) {
        this.firingPosition = firingPosition;
        renewSprites();
    }

    /**
     * Updates the position and facing that is used for centering the field of fire to the end
     * position and facing of the given movement path. Does not change the assumed unit, weapon and arc.
     *
     * @param movePath The considered movement path
     */
    public void updatePosition(MovePath movePath) {
        firingPosition = movePath.getFinalCoords();
        facing = movePath.getFinalFacing();
        isUnderWater=testUnderWater(movePath);
        renewSprites();
    }

    /**
     * Sets the selected unit and weapon. This will recalculate ranges, the facing including possible
     * torso/turret twists and the weapon arc. When no firing position is currently stored, the unit's
     * position will be used. This method does not check if the weapon is valid or, actually, on the unit.
     *
     * @param firingEntity the unit carrying the weapon to consider
     * @param weapon the weapon to consider
     */
    public void updateSelectedWeapon(Entity firingEntity, WeaponMounted weapon) {
        this.firingEntity = firingEntity;
        arc = firingEntity.getWeaponArc(clientGUI.getSelectedWeaponId());
        findRanges(weapon);
        updateFacing(weapon);
        if (firingPosition == null) {
            firingPosition = firingEntity.getPosition();
        }
        renewSprites();
    }

    /**
     * Clears the sprites and resets the cached values so no new sprites are drawn at this time. As this
     * handler requires cached values for weapon, arc etc. to draw the sprites, the {@link #clear()} method
     * may not be overridden to reset those fields as is done in other handlers.
     */
    public void clearValues() {
        clear();
        firingEntity = null;
        firingPosition = null;
        isUnderWater = false;
    }

    /**
     * Draw the sprites for the currently stored values for position, unit, arc etc. Does not draw sprites
     * if field of fire is deactivated.
     */
    public void renewSprites() {
        clear();
        if (!GUIP.getShowFieldOfFire() || (firingEntity == null) || (firingPosition == null)
                || firingEntity.isOffBoard() || !clientGUI.hasSelectedWeapon()) {
            return;
        }

        // check if extreme range is used
        int maxrange = 4;
        if (!game.getBoard().onGround() || game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE)) {
            maxrange = 5;
        }

        // create the lists of hexes
        List<Set<Coords>> fieldFire = new ArrayList<>(5);
        int range = 1;
        // for all available range brackets Min/S/M/L/E ...
        for (int bracket = 0; bracket < maxrange; bracket++) {
            fieldFire.add(new HashSet<>());
            // Add all hexes up to the weapon range to separate lists
            while (range <= ranges[underWaterIndex()][bracket]) {
                fieldFire.get(bracket).addAll(firingPosition.allAtDistance(range));
                range++;
                if (range > 100) {
                    break; // only to avoid hangs
                }
            }

            // Remove hexes that are not on the board or not in the arc
            fieldFire.get(bracket).removeIf(h -> !game.getBoard().contains(h) || !Compute.isInArc(firingPosition, facing, h, arc));
        }

        // for all available range brackets Min/S/M/L/E ...
        for (int bracket = 0; bracket < fieldFire.size(); bracket++) {
            if (fieldFire.get(bracket) == null) {
                continue;
            }
            for (Coords loc : fieldFire.get(bracket)) {
                // check surrounding hexes
                int edgesToPaint = 0;
                for (int dir = 0; dir < 6; dir++) {
                    Coords adjacentHex = loc.translated(dir);
                    if (!fieldFire.get(bracket).contains(adjacentHex)) {
                        edgesToPaint += (1 << dir);
                    }
                }
                // create sprite if there's a border to paint
                if (edgesToPaint > 0) {
                    FieldofFireSprite ffSprite = new FieldofFireSprite(boardView, bracket, loc, edgesToPaint);
                    currentSprites.add(ffSprite);
                }
            }
            // Add range markers (m, S, M, L, E)
            // this looks for a hex in the middle of the range bracket;
            // if outside the board, nearer hexes will be tried until
            // the inner edge of the range bracket is reached
            // the directions tested are those that fall between the
            // hex facings because this makes for a better placement
            // ... most of the time...

            // The directions[][] is used to make the marker placement
            // fairly symmetrical to the unit facing which a simple for
            // loop over the hex facings doesn't do
            int[][] directions = {{0, 1}, {0, 5}, {3, 2}, {3, 4}, {1, 2}, {5, 4}};
            // don't paint too many "min" markers
            int numMinMarkers = 0;
            for (int[] dir : directions) {
                // find the middle of the range bracket
                int rangeend = Math.max(ranges[underWaterIndex()][bracket], 0);
                int rangebegin = 1;
                if (bracket > 0) {
                    rangebegin = Math.max(ranges[underWaterIndex()][bracket - 1] + 1, 1);
                }
                int dist = (rangeend + rangebegin) / 2;
                // translate to the middle of the range bracket
                Coords mark = firingPosition.translated((dir[0] + facing) % 6, (dist + 1) / 2)
                        .translated((dir[1] + facing) % 6, dist / 2);
                // traverse back to the unit until a hex is onboard
                while (!game.getBoard().contains(mark)) {
                    mark = Coords.nextHex(mark, firingPosition);
                }

                // add a text range marker if the found position is good
                if (game.getBoard().contains(mark) && fieldFire.get(bracket).contains(mark)
                        && ((bracket > 0) || (numMinMarkers < 2))) {
                    TextMarkerSprite tS = new TextMarkerSprite(boardView, mark,
                            rangeTexts[bracket], FieldofFireSprite.getFieldOfFireColor(bracket));
                    currentSprites.add(tS);
                    if (bracket == 0) {
                        numMinMarkers++;
                    }
                }
            }
        }

        boardView.addSprites(currentSprites);
    }


    @Override
    public void initialize() {
        GUIP.addPreferenceChangeListener(this);
    }

    @Override
    public void dispose() {
        clear();
        GUIP.removePreferenceChangeListener(this);
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent evt) {
        switch (evt.getName()) {
            case GUIPreferences.SHOW_FIELD_OF_FIRE:
            case GUIPreferences.BOARD_FIELD_OF_FIRE_EXTREME_COLOR:
            case GUIPreferences.BOARD_FIELD_OF_FIRE_LONG_COLOR:
            case GUIPreferences.BOARD_FIELD_OF_FIRE_MEDIUM_COLOR:
            case GUIPreferences.BOARD_FIELD_OF_FIRE_SHORT_COLOR:
            case GUIPreferences.BOARD_FIELD_OF_FIRE_MIN_COLOR:
                renewSprites();
                break;
        }
    }

    /**
     * @return The ranges lookup index depending on whether the weapon is underWater
     */
    private int underWaterIndex() {
        return isUnderWater ? 1 : 0;
    }

    private void updateFacing(WeaponMounted weapon) {
        facing = firingEntity.getFacing();
        if (game.getPhase().isFiring()) {
            if (firingEntity.isSecondaryArcWeapon(firingEntity.getEquipmentNum(weapon))) {
                facing = firingEntity.getSecondaryFacing();
            }
            // If this is mech with turrets, check to see if the weapon is on a turret.
            if ((firingEntity instanceof Mech) && (weapon.isMechTurretMounted())) {
                // facing is currently adjusted for mek torso twist and facing, adjust for turret facing.
                facing = (weapon.getFacing() + facing) % 6;
            }
            // If this is a tank with dual turrets, check to see if the weapon is a second turret.
            if ((firingEntity instanceof Tank) && (weapon.getLocation() == ((Tank) firingEntity).getLocTurret2())) {
                facing = ((Tank) firingEntity).getDualTurretFacing();
            }
        } else if (game.getPhase().isTargeting()) {
            if (firingEntity.isSecondaryArcWeapon(firingEntity.getEquipmentNum(weapon))) {
                facing = firingEntity.getSecondaryFacing();
            }
        }
    }

    private void findRanges(WeaponMounted weapon) {
        WeaponType wtype = weapon.getType();
        ranges[0] = wtype.getRanges(weapon);

        AmmoType atype = null;
        if ((weapon.getLinked() != null) && (weapon.getLinked().getType() instanceof AmmoType)) {
            atype = (AmmoType) weapon.getLinked().getType();
        }

        // gather underwater ranges
        ranges[1] = wtype.getWRanges();
        if (atype != null) {
            if ((wtype.getAmmoType() == AmmoType.T_SRM)
                    || (wtype.getAmmoType() == AmmoType.T_SRM_IMP)
                    || (wtype.getAmmoType() == AmmoType.T_MRM)
                    || (wtype.getAmmoType() == AmmoType.T_LRM)
                    || (wtype.getAmmoType() == AmmoType.T_LRM_IMP)
                    || (wtype.getAmmoType() == AmmoType.T_MML)) {
                if (atype.getMunitionType().contains(AmmoType.Munitions.M_TORPEDO)) {
                    ranges[1] = wtype.getRanges(weapon);
                } else if (atype.getMunitionType().contains(AmmoType.Munitions.M_MULTI_PURPOSE)) {
                    ranges[1] = wtype.getRanges(weapon);
                }
            }
        }

        // Infantry range types 4+ are simplified to
        // the usual range types as displaying 5 range circles
        // would be visual overkill (and besides this makes
        // things easier)
        if (wtype instanceof InfantryWeapon) {
            InfantryWeapon inftype = (InfantryWeapon) wtype;
            int iR = inftype.getInfantryRange();
            ranges[0] = new int[] { 0, iR, iR * 2, iR * 3, 0 };
            ranges[1] = new int[] { 0, iR / 2, (iR / 2) * 2, (iR / 2) * 3, 0 };
        }

        // Artillery gets fixed ranges, 100 as an arbitrary
        // large range for the targeting phase and
        // 6 to 17 in the other phases as it will be
        // direct fire then
        if (wtype.hasFlag(WeaponType.F_ARTILLERY)) {
            boolean isADA = (weapon.getLinked() != null
                    && ((AmmoType) weapon.getLinked().getType()).getMunitionType().contains(AmmoType.Munitions.M_ADA));
            if (game.getPhase().isTargeting()) {
                ranges[0] = (!isADA? new int[] { 0, 0, 0, 100, 0 } : new int[] { 0, 0, 0, 51, 0 });
            } else {
                ranges[0] = (!isADA? new int[] { 6, 0, 0, 17, 0 } : wtype.getRanges(weapon));
            }
            ranges[1] = new int[] { 0, 0, 0, 0, 0 };
        }

        // Override for the MML ammos
        if (atype != null) {
            if (atype.getAmmoType() == AmmoType.T_MML) {
                if (atype.hasFlag(AmmoType.F_MML_LRM)) {
                    if (atype.getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE)) {
                        ranges[0] = new int[]{4, 5, 10, 15, 20};
                    } else {
                        ranges[0] = new int[]{6, 7, 14, 21, 28};
                    }
                } else {
                    if (atype.getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE)) {
                        ranges[0] = new int[]{0, 2, 4, 6, 8};
                    } else {
                        ranges[0] = new int[]{0, 3, 6, 9, 12};
                    }
                }
            }
        }

        // No minimum range for hotload
        if ((weapon.getLinked() != null) && weapon.getLinked().isHotLoaded()) {
            ranges[0][0] = 0;
        }

        // Aero
        if (firingEntity.isAirborne()) {

            // keep original ranges ranges, no underwater
            ranges[1] = new int[] { 0, 0, 0, 0, 0 };
            int maxr;

            // In the WeaponPanel, when the weapon is out of ammo
            // or otherwise nonfunctional, SHORT range will be listed;
            // the field of fire is instead disabled
            // Here as well as in WeaponPanel, choosing a specific ammo
            // only works for the current player's units
            if (!weapon.isBreached() && !weapon.isMissing()
                    && !weapon.isDestroyed() && !weapon.isJammed()
                    && ((weapon.getLinked() == null)
                    || (weapon.getLinked().getUsableShotsLeft() > 0))) {
                maxr = wtype.getMaxRange(weapon);

                // set the standard ranges, depending on capital or no
                // boolean isCap = wtype.isCapital();
                int rangeMultiplier = wtype.isCapital() ? 2 : 1;
                if (game.getBoard().onGround()) {
                    rangeMultiplier *= 8;
                }

                for (int rangeIndex = RangeType.RANGE_MINIMUM; rangeIndex <= RangeType.RANGE_EXTREME; rangeIndex++) {
                    if (maxr >= rangeIndex) {
                        ranges[0][rangeIndex] = WeaponType.AIRBORNE_WEAPON_RANGES[rangeIndex] * rangeMultiplier;
                    }
                }
            }
        }
    }

    /**
     * @return True when, for the given movement path, the currently selected weapon ends up being underwater.
     * @param movePath The movement path that is considered for the selected unit
     */
    private boolean testUnderWater(MovePath movePath) {
        if ((firingEntity == null) || (movePath == null) || !clientGUI.hasSelectedWeapon()) {
            return false;
        }

        int location = clientGUI.getSelectedWeapon().getLocation();
        Hex hex = game.getBoard().getHex(movePath.getFinalCoords());
        int waterDepth = hex.terrainLevel(Terrains.WATER);

        if ((waterDepth > 0) && !movePath.isJumping() && (movePath.getFinalElevation() < 0)) {
            if ((firingEntity instanceof Mech) && !firingEntity.isProne() && (waterDepth == 1)) {
                return firingEntity.locationIsLeg(location);
            } else {
                return true;
            }
        }
        return false;
    }
}
