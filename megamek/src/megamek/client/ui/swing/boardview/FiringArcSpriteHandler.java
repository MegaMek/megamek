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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.GUIPreferences;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * This BoardViewSpriteHandler handles the sprites for the firing arcs (field of
 * fire) that can be shown for
 * individual weapons or bays. The field of fire depends on a handful of
 * variables (position, unit facing
 * and twists, weapon arc and range). These variables can and must be set
 * individually and are cached.
 */
public class FiringArcSpriteHandler extends BoardViewSpriteHandler implements IPreferenceChangeListener {

    private static final String[] rangeTexts = { "min", "S", "M", "L", "E" };

    private final Game game;
    private final ClientGUI clientGUI;

    // In this handler, the values are cached because only some of the values are
    // updated by method calls at a time
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
     * Shows the firing arcs for the given weapon on the given unit, centering on
     * the endpoint
     * of the planned movePath if it is not null.
     *
     * @param entity   The unit carrying the weapon
     * @param weapon   the selected weapon
     * @param movePath planned movement in the movement phase
     */
    public void update(@Nullable Entity entity, @Nullable WeaponMounted weapon, @Nullable MovePath movePath) {
        firingEntity = entity;
        if ((entity == null) || (weapon == null)) {
            clearValues();
            return;
        }
        int weaponId = entity.getEquipmentNum(weapon);
        if (weaponId == -1) {
            // entities are replaced all the time by server-sent changes, must always guard
            clearValues();
            return;
        }
        // findRanges must be called before any call to testUnderWater due to usage of
        // global-style variables for some reason
        findRanges(weapon);
        if (movePath != null) {
            firingPosition = movePath.getFinalCoords();
            isUnderWater = testUnderWater(movePath);
            updateFacing(weapon, movePath.getFinalFacing());
        } else {
            firingPosition = entity.getPosition();
            isUnderWater = testUnderWater();
            updateFacing(weapon);
        }
        firingPosition = (movePath != null) ? movePath.getFinalCoords() : entity.getPosition();
        arc = firingEntity.getWeaponArc(weaponId);

        renewSprites();
    }

    /**
     * Shows the firing arcs for the given weapon on the given unit.
     *
     * @param entity The unit carrying the weapon
     * @param weapon the selected weapon
     */
    public void update(Entity entity, WeaponMounted weapon) {
        update(entity, weapon, null);
    }

    /**
     * Clears the sprites and resets the cached values so no new sprites are drawn
     * at this time. As this
     * handler requires cached values for weapon, arc etc. to draw the sprites, the
     * {@link #clear()} method
     * may not be overridden to reset those fields as is done in other handlers.
     */
    public void clearValues() {
        clear();
        firingEntity = null;
        firingPosition = null;
        isUnderWater = false;
    }

    /**
     * For landed DropShips, the effective range is always one more as they can
     * choose any of their
     * secondary positions as the effective origin of weapon fire.
     *
     * @return 1 for a landed DropShip, 0 otherwise
     */
    private int secondaryPositionsRangeBonus() {
        return ((firingEntity instanceof Dropship) && !firingEntity.isAirborne() &&
                !firingEntity.isSpaceborne() && clientGUI.getDisplayedWeapon().isPresent()
                && clientGUI.getDisplayedWeapon().get().getLocation() != Dropship.LOC_NOSE) ? 1 : 0;
    }

    /**
     * Draw the sprites for the currently stored values for position, unit, arc etc.
     * Does not draw sprites
     * if field of fire is deactivated.
     */
    public void renewSprites() {
        clear();
        if (!GUIP.getShowFieldOfFire() || (firingEntity == null) || (firingPosition == null)
                || firingEntity.isOffBoard() || clientGUI.getDisplayedWeapon().isEmpty()) {
            return;
        }

        // check if extreme range is used
        int maxrange = 4;
        if (!game.getBoard().onGround() || game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE)) {
            maxrange = 5;
        }

        // create the lists of hexes
        List<Set<Coords>> fieldFire = new ArrayList<>(5);
        int secondaryPositionRangeBonus = secondaryPositionsRangeBonus();

        // Firing arcs should normally not include the unit's hex(es), therefore start
        // at range 1; 2 for landed DS
        int range = 1 + secondaryPositionRangeBonus;

        // Special treatment for AFT weapons on landed DS; they can fire only into the
        // DS's hexes:
        if ((firingEntity instanceof Dropship) && !firingEntity.isAirborne()
                && !firingEntity.isSpaceborne() && clientGUI.getDisplayedWeapon().isPresent()
                && (clientGUI.getDisplayedWeapon().get().getLocation() == Dropship.LOC_AFT)) {
            // AFT weapons on landed DS can only fire into its own 7 hexes
            range = 0;
        }

        // for all available range brackets Min/S/M/L/E ...
        for (int bracket = 0; bracket < maxrange; bracket++) {
            fieldFire.add(new HashSet<>());
            // Don't add any hexes to the min range bracket when the minimum range is 0,
            // i.e. no minimum range
            if ((bracket != 0) || (ranges[underWaterIndex()][0] > 0)) {
                // Add all hexes up to the weapon range to separate lists
                while (range <= ranges[underWaterIndex()][bracket] + secondaryPositionRangeBonus) {
                    fieldFire.get(bracket).addAll(firingPosition.allAtDistance(range));
                    range++;
                    if (range > 100) {
                        break; // only to avoid hangs
                    }
                }
            }

            // Remove hexes that are not on the board or not in the arc
            fieldFire.get(bracket).removeIf(coords -> !game.getBoard().contains(coords)
                    || !Compute.isInArc(firingPosition, facing, coords, arc));
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
            int[][] directions = { { 0, 1 }, { 0, 5 }, { 3, 2 }, { 3, 4 }, { 1, 2 }, { 5, 4 } };
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
        if (firingEntity != null) {
            updateFacing(weapon, firingEntity.getFacing());
        }
    }

    private void updateFacing(WeaponMounted weapon, int assumedFacing) {
        if (firingEntity == null) {
            return;
        }
        facing = firingEntity.getFacing();
        if (game.getPhase().isFiring()) {
            if (firingEntity.isSecondaryArcWeapon(firingEntity.getEquipmentNum(weapon))) {
                facing = firingEntity.getSecondaryFacing();
            }
            // If this is mek with turrets, check to see if the weapon is on a turret.
            if ((firingEntity instanceof Mek) && (weapon.isMekTurretMounted())) {
                // facing is currently adjusted for mek torso twist and facing, adjust for
                // turret facing.
                facing = (weapon.getFacing() + facing) % 6;
            }
            // If this is a tank with dual turrets, check to see if the weapon is a second
            // turret.
            if ((firingEntity instanceof Tank) && (weapon.getLocation() == ((Tank) firingEntity).getLocTurret2())) {
                facing = ((Tank) firingEntity).getDualTurretFacing();
            }
        } else if (game.getPhase().isTargeting() || game.getPhase().isOffboard()) {
            if (firingEntity.isSecondaryArcWeapon(firingEntity.getEquipmentNum(weapon))) {
                facing = firingEntity.getSecondaryFacing();
            }
        }
        facing = (assumedFacing + facing - firingEntity.getFacing() + 6) % 6;
    }

    private void findRanges(WeaponMounted weapon) {
        WeaponType wtype = weapon.getType();

        // Use the Weapon Panel's selected ammo to determine ranges, or the current
        // linked ammo if not set
        AmmoMounted ammoMounted = (clientGUI.getDisplayedAmmo().isPresent())
                ? clientGUI.getDisplayedAmmo().get()
                : weapon.getLinkedAmmo();

        // Try to get the ammo type from the selected ammo if possible, or the current
        // linked ammo if not
        AmmoType atype = (ammoMounted != null) ? ammoMounted.getType() : null;
        if (atype == null && (weapon.getLinked() != null) && (weapon.getLinked().getType() instanceof AmmoType)) {
            atype = (AmmoType) weapon.getLinked().getType();
        }

        // Ranges set by weapon + ammo combination, but will be updated depending on
        // selected unit
        ranges[0] = wtype.getRanges(weapon, ammoMounted);

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
                    ranges[1] = wtype.getRanges(weapon, ammoMounted);
                } else if (atype.getMunitionType().contains(AmmoType.Munitions.M_MULTI_PURPOSE)) {
                    ranges[1] = wtype.getRanges(weapon, ammoMounted);
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
            boolean isADA = (ammoMounted != null
                    && ((AmmoType) ammoMounted.getType()).getMunitionType().contains(AmmoType.Munitions.M_ADA));
            if (game.getPhase().isTargeting()) {
                ranges[0] = (!isADA ? new int[] { 0, 0, 0, 100, 0 } : new int[] { 0, 0, 0, 51, 0 });
            } else {
                ranges[0] = (!isADA ? new int[] { 6, 0, 0, 17, 0 } : wtype.getRanges(weapon, ammoMounted));
            }
            ranges[1] = new int[] { 0, 0, 0, 0, 0 };
        }

        // Override for the MML ammos
        if (atype != null) {
            if (atype.getAmmoType() == AmmoType.T_MML) {
                if (atype.hasFlag(AmmoType.F_MML_LRM)) {
                    if (atype.getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE)) {
                        ranges[0] = new int[] { 4, 5, 10, 15, 20 };
                    } else {
                        ranges[0] = new int[] { 6, 7, 14, 21, 28 };
                    }
                } else {
                    if (atype.getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE)) {
                        ranges[0] = new int[] { 0, 2, 4, 6, 8 };
                    } else {
                        ranges[0] = new int[] { 0, 3, 6, 9, 12 };
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
                    && ((ammoMounted == null)
                            || (ammoMounted.getUsableShotsLeft() > 0))) {
                maxr = wtype.getMaxRange(weapon, ammoMounted);

                // set the standard ranges, depending on capital or no
                // boolean isCap = wtype.isCapital();
                int rangeMultiplier = wtype.isCapital() ? 2 : 1;
                if (game.getBoard().onGround()) {
                    rangeMultiplier *= 8;
                }

                for (int rangeIndex = RangeType.RANGE_MINIMUM; rangeIndex <= RangeType.RANGE_EXTREME; rangeIndex++) {
                    if (maxr >= rangeIndex) {
                        ranges[0][rangeIndex] = WeaponType.AIRBORNE_WEAPON_RANGES[rangeIndex] * rangeMultiplier;
                    } else {
                        ranges[0][rangeIndex] = 0;
                    }
                }
            }

        } else {
            if ((firingEntity instanceof Dropship) && !firingEntity.isAirborne()
                    && !firingEntity.isSpaceborne() && clientGUI.getDisplayedWeapon().isPresent()
                    && (clientGUI.getDisplayedWeapon().get().getLocation() == Dropship.LOC_AFT)) {
                // AFT weapons on landed DS can only fire into its own 7 hexes
                ranges[0] = new int[] { -1, 0, 0, 0, 0 };
            }
        }
    }

    /**
     * @return True when, for the given movement path, the currently selected weapon
     *         ends up being underwater.
     * @param movePath The movement path that is considered for the selected unit
     */
    private boolean testUnderWater(MovePath movePath) {
        return (movePath != null) && testUnderWater(movePath.getFinalCoords(),
                !movePath.isJumping(), movePath.getFinalElevation());
    }

    /**
     * @return True when, for the present firingEntity and firingPosition, the
     *         currently selected weapon
     *         ends up being underwater.
     */
    private boolean testUnderWater() {
        return testUnderWater(firingPosition, true, firingEntity.getElevation());
    }

    /**
     * @return True when, at the given position and elevation and when allowSubmerge
     *         is true,
     *         the currently selected weapon ends up being underwater
     */
    private boolean testUnderWater(Coords position, boolean allowSubmerge, int unitElevation) {
        if ((firingEntity == null) || clientGUI.getDisplayedWeapon().isEmpty() || (position == null)
                || (game.getBoard().getHex(position) == null)) {
            return false;
        }

        int location = clientGUI.getDisplayedWeapon().get().getLocation();
        Hex hex = game.getBoard().getHex(position);
        int waterDepth = hex.terrainLevel(Terrains.WATER);

        // if this is a ship/sub on the surface and we have a weapon that only has water
        // ranges, consider it an underwater weapon for the purposes of displaying range
        // brackets
        if (waterDepth > 0 && firingEntity.isSurfaceNaval() &&
                ranges[0][1] == 0 && ranges[1][1] > 0) {
            return true;
        }

        if ((waterDepth > 0) && allowSubmerge && (unitElevation < 0)) {
            if ((firingEntity instanceof Mek) && !firingEntity.isProne() && (waterDepth == 1)) {
                return firingEntity.locationIsLeg(location);
            } else {
                return true;
            }
        }
        return false;
    }
}
