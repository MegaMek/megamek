/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.clientGUI.boardview.spriteHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.sprite.FieldOfFireSprite;
import megamek.client.ui.clientGUI.boardview.sprite.TextMarkerSprite;
import megamek.common.Hex;
import megamek.common.HexTarget;
import megamek.common.RangeType;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.BoardHelper;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeArc;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.units.Targetable;
import megamek.common.units.Terrains;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * This BoardViewSpriteHandler handles the sprites for the firing arcs (field of fire) that can be shown for individual
 * weapons or bays. The field of fire depends on a handful of variables (position, unit facing and twists, weapon arc
 * and range). These variables can and must be set individually and are cached.
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
    private boolean isCapOrSCap = false;
    private final int[][] ranges = new int[2][5];

    public FiringArcSpriteHandler(ClientGUI clientGUI) {
        super(clientGUI);
        this.clientGUI = clientGUI;
        game = clientGUI.getClient().getGame();
    }

    /**
     * Shows the firing arcs for the given weapon on the given unit, centering on the endpoint of the planned movePath
     * if it is not null.
     *
     * @param entity   The unit carrying the weapon
     * @param weapon   the selected weapon
     * @param movePath planned movement in the movement phase
     */
    public void update(@Nullable Entity entity, @Nullable WeaponMounted weapon, @Nullable MovePath movePath) {
        if ((entity == null) || (weapon == null)) {
            clearValues();
            return;
        }

        firingEntity = weapon.getEntity();
        int weaponId = firingEntity.getEquipmentNum(weapon);
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
        isCapOrSCap = weapon.getType().isCapital() || weapon.getType().isSubCapital();

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
     * Clears the sprites and resets the cached values so no new sprites are drawn at this time. As this handler
     * requires cached values for weapon, arc etc. to draw the sprites, the {@link #clear()} method may not be
     * overridden to reset those fields as is done in other handlers.
     */
    public void clearValues() {
        clear();
        firingEntity = null;
        firingPosition = null;
        isUnderWater = false;
    }

    /**
     * For landed DropShips, the effective range is always one more as they can choose any of their secondary positions
     * as the effective origin of weapon fire.
     *
     * @return 1 for a landed DropShip, 0 otherwise
     */
    private int secondaryPositionsRangeBonus() {
        return ((firingEntity instanceof Dropship) && !firingEntity.isAirborne() &&
              !firingEntity.isSpaceborne() && clientGUI.getDisplayedWeapon().isPresent()
              && clientGUI.getDisplayedWeapon().get().getLocation() != Dropship.LOC_NOSE) ? 1 : 0;
    }

    /**
     * Draw the sprites for the currently stored values for position, unit, arc etc. Does not draw sprites if field of
     * fire is deactivated.
     */
    public void renewSprites() {
        clear();
        if (clientGUI.boardViews().isEmpty()) {
            return;
        }
        if (!GUIP.getShowFieldOfFire() || (firingEntity == null) || (firingPosition == null)
              || firingEntity.isOffBoard() || clientGUI.getDisplayedWeapon().isEmpty()) {
            return;
        }

        Board board = game.getBoard(firingEntity);

        // check if extreme range is used
        int maxRange = 4;
        if (!board.isGround() || game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE)) {
            maxRange = 5;
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
        if (board.isHighAltitude()) {
            // This is more computationally expensive as the atmospheric row hexes reduce range per hex
            // for all available range brackets Min/S/M/L/E ...
            for (int bracket = 0; bracket < maxRange; bracket++) {
                fieldFire.add(new HashSet<>());
                // Add all hexes up to the weapon range for the current range bracket
                final int currentRange = ranges[underWaterIndex()][bracket];
                fieldFire.get(bracket).addAll(firingPosition.allAtDistanceOrLess(currentRange));
                for (int previousBracket = 0; previousBracket < bracket; previousBracket++) {
                    // All hexes that were found to be a lesser range bracket must no longer be considered
                    fieldFire.get(bracket).removeAll(fieldFire.get(previousBracket));
                }
                fieldFire.get(bracket).remove(firingPosition);

                // Remove hexes that are not on the board or not in the arc
                fieldFire.get(bracket).removeIf(h -> !board.contains(h));
                fieldFire.get(bracket).removeIf(h -> !ComputeArc.isInArc(firingPosition, facing, h, arc));
                fieldFire.get(bracket).removeIf(h -> Compute.effectiveDistance(game, firingEntity,
                      new HexTarget(h, board.getBoardId(), Targetable.TYPE_HEX_CLEAR)) > currentRange);
                if (!isCapOrSCap) {
                    fieldFire.get(bracket).removeIf(h ->
                          BoardHelper.crossesSpaceAtmosphereInterface(game, board, firingPosition, h));
                }
            }
        } else {
            for (int bracket = 0; bracket < maxRange; bracket++) {
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
                fieldFire.get(bracket)
                      .removeIf(coords -> !board.contains(coords) || !ComputeArc.isInArc(firingPosition,
                            facing,
                            coords,
                            arc));
            }
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
                    FieldOfFireSprite ffSprite = new FieldOfFireSprite(clientGUI.getBoardView(firingEntity),
                          bracket,
                          loc,
                          edgesToPaint);
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
                int rangeRnd = Math.max(ranges[underWaterIndex()][bracket], 0);
                int rangeBegin = 1;
                if (bracket > 0) {
                    rangeBegin = Math.max(ranges[underWaterIndex()][bracket - 1] + 1, 1);
                }
                int dist = (rangeRnd + rangeBegin) / 2;
                // translate to the middle of the range bracket
                Coords mark = firingPosition.translated((dir[0] + facing) % 6, (dist + 1) / 2)
                      .translated((dir[1] + facing) % 6, dist / 2);
                // traverse back to the unit until a hex is onboard
                while (!board.contains(mark)) {
                    mark = Coords.nextHex(mark, firingPosition);
                }

                // add a text range marker if the found position is good
                if (board.contains(mark) && fieldFire.get(bracket).contains(mark)
                      && ((bracket > 0) || (numMinMarkers < 2))) {
                    TextMarkerSprite tS =
                          new TextMarkerSprite(clientGUI.getBoardView(firingEntity),
                                mark,
                                rangeTexts[bracket],
                                FieldOfFireSprite.getFieldOfFireColor(bracket));
                    currentSprites.add(tS);
                    if (bracket == 0) {
                        numMinMarkers++;
                    }
                }
            }
        }

        clientGUI.getBoardView(firingEntity).addSprites(currentSprites);
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
        WeaponType weaponType = weapon.getType();

        // Use the Weapon Panel's selected ammo to determine ranges, or the current
        // linked ammo if not set
        AmmoMounted ammoMounted = (clientGUI.getDisplayedAmmo().isPresent())
              ? clientGUI.getDisplayedAmmo().get()
              : weapon.getLinkedAmmo();

        // Try to get the ammo type from the selected ammo if possible, or the current
        // linked ammo if not
        AmmoType ammoType = (ammoMounted != null) ? ammoMounted.getType() : null;
        if (ammoType == null && (weapon.getLinked() != null) && (weapon.getLinked().getType() instanceof AmmoType)) {
            ammoType = (AmmoType) weapon.getLinked().getType();
        }

        // Ranges set by weapon + ammo combination, but will be updated depending on
        // selected unit
        ranges[0] = weaponType.getRanges(weapon, ammoMounted);

        // gather underwater ranges
        ranges[1] = weaponType.getWRanges();
        if (ammoType != null) {
            if ((weaponType.getAmmoType() == AmmoType.AmmoTypeEnum.SRM)
                  || (weaponType.getAmmoType() == AmmoType.AmmoTypeEnum.SRM_IMP)
                  || (weaponType.getAmmoType() == AmmoType.AmmoTypeEnum.MRM)
                  || (weaponType.getAmmoType() == AmmoType.AmmoTypeEnum.LRM)
                  || (weaponType.getAmmoType() == AmmoType.AmmoTypeEnum.LRM_IMP)
                  || (weaponType.getAmmoType() == AmmoType.AmmoTypeEnum.MML)) {
                if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_TORPEDO)) {
                    ranges[1] = weaponType.getRanges(weapon, ammoMounted);
                } else if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_MULTI_PURPOSE)) {
                    ranges[1] = weaponType.getRanges(weapon, ammoMounted);
                }
            }
        }

        // Infantry range types 4+ are simplified to
        // the usual range types as displaying 5 range circles
        // would be visual overkill (and besides this makes
        // things easier)
        if (weaponType instanceof InfantryWeapon infantryWeapon) {
            int iR = infantryWeapon.getInfantryRange();
            ranges[0] = new int[] { 0, iR, iR * 2, iR * 3, 0 };
            ranges[1] = new int[] { 0, iR / 2, (iR / 2) * 2, (iR / 2) * 3, 0 };
        }

        // Artillery gets fixed ranges, 100 as an arbitrary
        // large range for the targeting phase and
        // 6 to 17 in the other phases as it will be
        // direct fire then
        if (weaponType.hasFlag(WeaponType.F_ARTILLERY)) {
            boolean isADA = (ammoMounted != null
                  && ammoMounted.getType().getMunitionType().contains(AmmoType.Munitions.M_ADA));
            if (game.getPhase().isTargeting()) {
                ranges[0] = (!isADA ? new int[] { 0, 0, 0, 100, 0 } : new int[] { 0, 0, 0, 51, 0 });
            } else {
                ranges[0] = (!isADA ? new int[] { 6, 0, 0, 17, 0 } : weaponType.getRanges(weapon, ammoMounted));
            }
            ranges[1] = new int[] { 0, 0, 0, 0, 0 };
        }

        // Override for the MML ammunition
        if (ammoType != null) {
            if (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.MML) {
                if (ammoType.hasFlag(AmmoType.F_MML_LRM)) {
                    if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE)) {
                        ranges[0] = new int[] { 4, 5, 10, 15, 20 };
                    } else {
                        ranges[0] = new int[] { 6, 7, 14, 21, 28 };
                    }
                } else {
                    if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE)) {
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

            // keep original ranges, no underwater
            ranges[1] = new int[] { 0, 0, 0, 0, 0 };
            int maxRange;

            // In the WeaponPanel, when the weapon is out of ammo
            // or otherwise nonfunctional, SHORT range will be listed;
            // the field of fire is instead disabled
            // Here as well as in WeaponPanel, choosing a specific ammo
            // only works for the current player's units
            if (!weapon.isBreached() && !weapon.isMissing()
                  && !weapon.isDestroyed() && !weapon.isJammed()
                  && ((ammoMounted == null)
                  || (ammoMounted.getUsableShotsLeft() > 0))) {
                maxRange = weaponType.getMaxRange(weapon, ammoMounted);

                // set the standard ranges, depending on capital or no
                int rangeMultiplier = weaponType.isCapital() ? 2 : 1;
                if (game.getBoard(firingEntity).isGround()) {
                    rangeMultiplier *= 8;
                }

                for (int rangeIndex = RangeType.RANGE_MINIMUM; rangeIndex <= RangeType.RANGE_EXTREME; rangeIndex++) {
                    if (maxRange >= rangeIndex) {
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
     * @param movePath The movement path that is considered for the selected unit
     *
     * @return True when, for the given movement path, the currently selected weapon ends up being underwater.
     */
    private boolean testUnderWater(MovePath movePath) {
        return (movePath != null) && testUnderWater(movePath.getFinalCoords(),
              !movePath.isJumping(), movePath.getFinalElevation());
    }

    /**
     * @return True when, for the present firingEntity and firingPosition, the currently selected weapon ends up being
     *       underwater.
     */
    private boolean testUnderWater() {
        return testUnderWater(firingPosition, firingEntity.getBoardId(), true, firingEntity.getElevation());
    }

    /**
     * @return True when, at the given position and elevation and when allowSubmerge is true, the currently selected
     *       weapon ends up being underwater
     *       <p>
     *       LEGACY replaced with BoardLocation version when ready
     */
    private boolean testUnderWater(Coords position, boolean allowSubmerge, int unitElevation) {
        return testUnderWater(position, 0, allowSubmerge, unitElevation);
    }

    /**
     * @return True when, at the given position and elevation and when allowSubmerge is true, the currently selected
     *       weapon ends up being underwater
     */
    private boolean testUnderWater(Coords position, int boardId, boolean allowSubmerge, int unitElevation) {
        if ((firingEntity == null) || clientGUI.getDisplayedWeapon().isEmpty() || (position == null)
              || (game.getBoard(boardId).getHex(position) == null)) {
            return false;
        }

        int location = clientGUI.getDisplayedWeapon().get().getLocation();
        Hex hex = game.getBoard(boardId).getHex(position);
        int waterDepth = hex.terrainLevel(Terrains.WATER);

        // if this is a ship/sub on the surface, and we have a weapon that only has water
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
