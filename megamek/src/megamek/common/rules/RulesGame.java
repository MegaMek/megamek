package megamek.common.rules;
/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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


import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.units.Entity;

public abstract class RulesGame {

    /**
     * Allow ammo dumping.
     *
     * @return true if ammo dumping is allowed
     */
    public abstract boolean ammoDumping();

    /**
     * Is an entity eligible for a phase
     *
     * @param entity the unit being considered
     * @param phase  what phase it is in
     *
     * @return is it eligible
     */
    public abstract boolean eligibleForPhase(Entity entity,
          @Nullable GamePhase phase);

    /**
     * Return the number of units to move.
     *
     * @param num_normal_turns array of normal turns
     * @param index            the current index
     * @param min              the minimum value
     * @param frontLoadOption  true if front load option is enabled
     *
     * @return the initiative order
     */
    public abstract int getInitiativeOrder(int[] num_normal_turns,
          int index,
          int min,
          boolean frontLoadOption);

    /**
     * Is there a BV bump for tag?
     *
     * @param entity     The entity being considered
     * @param bvReport   the report
     * @param adjustedBV the adjusted BV so far
     * @param tagCount   how many tags in the force
     * @param hasGuided  does it have guided? (default false)
     *
     * @return adjusted BV value with bump if applicable
     */
    public abstract double tagBVBump(Entity entity,
          CalculationReport bvReport,
          double adjustedBV,
          long tagCount,
          boolean hasGuided);

    /**
     * Allow minefields or not
     *
     * @param toMinefields OptionsConstants.ADVANCED_MINEFIELDS
     *
     * @return Allow in core or TO
     */
    public abstract boolean allowMinefields(boolean toMinefields);

    /**
     * Helped function for tagBVBump to get the equipment descriptor for a mounted item.
     *
     * @param mounted
     * @param entity
     *
     * @return
     */
    public String equipmentDescriptor(Mounted<?> mounted,
          Entity entity) {
        if (mounted.getType() instanceof WeaponType) {
            String descriptor = mounted.getType().getShortName() +
                  " (" +
                  entity.getLocationAbbr(mounted.getLocation()) +
                  ")";
            if (mounted.isMekTurretMounted()) {
                descriptor += " (T)";
            }
            if (mounted.isRearMounted() || (mounted.getType().hasFlag(WeaponType.F_VGL)
                  && (mounted.getFacing() >= 2)
                  && (mounted.getFacing() <= 4))) {
                descriptor += " (R)";
            }
            return descriptor;
        } else if ((mounted.getType() instanceof MiscType) && ((MiscType) mounted.getType()).isVibroblade()) {
            return mounted.getType().getShortName() + " (" + entity.getLocationAbbr(mounted.getLocation()) + ")";
        } else if (mounted.getType() instanceof AmmoType) {
            String shortName = mounted.getType().getShortName();
            return shortName + (shortName.contains("Ammo") ? "" : " Ammo");
        } else {
            return mounted.getType().getShortName();
        }
    }

    /**
     * Is walk on deployment supported?
     *
     * @return Can they walk ok
     */
    public abstract boolean isWalkOnDeployment();

    /**
     * For use by TW to enable walk on deployment. Not used by Core
     *
     * @param walkOn should walk-on deployment be enabled
     */
    public abstract void setWalkOnDeployment(boolean walkOn);

    /**
     * Walk-on deployment. Skip deployment for eligible units Note: Bot does not get Walk-on deployment for now.
     * TODO bot walk-on deployment when enabled will remove the isBot check.
     *
     * @param entity The mek under consideration
     *
     * @return true if they can walk on, false if they cannot
     */
    public boolean canWalkOnThisRound(Entity entity) {
        if (!isWalkOnDeployment() || entity.getGame() == null) {
            return false;
        }

        int deploymentRound = entity.getDeployRound();
        int currentRound = entity.getGame().getCurrentRound();
        int startingPos = entity.getStartingPos();
        return (!entity.isDeployed() && deploymentRound >= 0
              && deploymentRound <= currentRound
              && startingPos != Board.START_ANY
              && startingPos != Board.START_CENTER
              && startingPos <= Board.NUM_ZONES
              && !entity.getOwner().isBot());
    }

    /**
     * Should the entity be included in movement / initiative?
     *
     * @param phase  what phase is it
     * @param entity what entity is it
     *
     * @return True by default
     */
    public boolean includeInMovement(GamePhase phase,
          Entity entity) {
        return true;
    }

    /**
     * This gets the deployment width. It is called from both Player and Entity
     *
     * @param player          What player's deployment area (used for bot check)
     * @param deploymentArea  what deployment area
     * @param deploymentWidth what deployment width
     *
     * @return the updated deployment width (1 for walk-on)
     */
    public int getDeploymentWidth(Player player,
          int deploymentArea,
          int deploymentWidth) {
        if (isWalkOnDeployment()) {
            int width = 1;
            if (deploymentArea == Board.START_ANY ||
                  deploymentArea == Board.START_CENTER ||
                  deploymentArea > Board.NUM_ZONES ||
                  player.isBot()) {
                width = deploymentWidth;
            }
            return width;
        }
        return deploymentWidth;
    }
}
