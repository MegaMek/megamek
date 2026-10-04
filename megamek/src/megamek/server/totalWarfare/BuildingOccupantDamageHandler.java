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
package megamek.server.totalWarfare;

import java.util.Vector;

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.WeaponType;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Infantry;
import megamek.logging.MMLogger;

/**
 * Damages the infantry inside a building hex when the building is attacked from outside (TW p. 172). The building's
 * type sets the share of the attack that reaches them, and every infantry unit inside takes that share in full: the
 * damage is "applied equally to all affected infantry units", where the sentence before it speaks of dividing damage
 * when that is what it means.
 *
 * <p>For conventional infantry the share is then converted like any other attack on them (TW p. 216): by the row of
 * the weapon that made it, except that damage from other infantry is never reduced, and burst-fire weapons count as
 * direct fire (the dice they roll against infantry in the open do not reach inside). Battle armour takes the share in
 * 5-point groupings.</p>
 */
class BuildingOccupantDamageHandler extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(BuildingOccupantDamageHandler.class);

    /** Battle armour inside a building takes damage in groupings of this many points. */
    private static final int BATTLE_ARMOR_GROUPING = 5;

    BuildingOccupantDamageHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Applies an attack on a building hex to the infantry inside it.
     *
     * @param building          the building attacked
     * @param damage            the damage of the attack on the building
     * @param hexCoords         the building hex attacked
     * @param damageClass       the attacking weapon's infantry damage class, or
     *                          {@link WeaponType#WEAPON_INFANTRY_ORIGIN} for an attack by conventional infantry
     *
     * @return the reports of the damage
     */
    Vector<Report> damageInfantryIn(IBuilding building, int damage, Coords hexCoords, int damageClass) {
        Vector<Report> reports = new Vector<>();
        int share = building.damageReachingInfantryInside(damage);
        int conversionClass = conversionClassFor(damageClass);
        for (Entity entity : getGame().getEntitiesVector()) {
            if (!(entity instanceof Infantry infantry) || !isInside(infantry, building, hexCoords)) {
                continue;
            }
            int damageTaken = damageTakenBy(infantry, share, conversionClass);
            LOGGER.debug("[InfantryInBuilding] {} inside {} at {}: attack of {} leaves {} for the infantry, "
                        + "{} after conversion (damage class {})", infantry.getShortName(), building.getName(),
                  hexCoords, damage, share, damageTaken, conversionClass);
            reports.addAll(applyDamage(infantry, share, damageTaken));
            Report.addNewline(reports);
        }
        return reports;
    }

    /**
     * The row of TW p. 216 a share is converted by. Damage from other infantry always equals the standard damage.
     * Burst-fire weapons count as direct fire: a building shields its occupants from the dice they roll in the open.
     */
    private static int conversionClassFor(int damageClass) {
        boolean isBurstFire = (damageClass >= WeaponType.WEAPON_BURST_HALF_D6)
              && (damageClass <= WeaponType.WEAPON_BURST_7D6);
        if (isBurstFire || (damageClass == WeaponType.WEAPON_NA)) {
            return WeaponType.WEAPON_DIRECT_FIRE;
        }
        return damageClass;
    }

    private boolean isInside(Infantry infantry, IBuilding building, Coords hexCoords) {
        Coords position = infantry.getPosition();
        boolean isInTheHex = (position != null) && position.equals(hexCoords);
        boolean isOnTheBuildingsBoard = infantry.getBoardId() == building.getBoardId();
        if (!isInTheHex || !isOnTheBuildingsBoard) {
            return false;
        }
        // In the hex but not on its roof
        return Compute.isInBuilding(getGame(), infantry, position);
    }

    /** The damage one infantry unit inside takes from the share: converted for conventional infantry. */
    private static int damageTakenBy(Infantry infantry, int share, int conversionClass) {
        if ((share <= 0) || (infantry instanceof BattleArmor)) {
            return Math.max(share, 0);
        }
        boolean isNonInfantryAgainstMechanized = infantry.isMechanized()
              && (conversionClass != WeaponType.WEAPON_INFANTRY_ORIGIN);
        return Compute.directBlowInfantryDamage(share, 0, conversionClass, isNonInfantryAgainstMechanized, false);
    }

    private Vector<Report> applyDamage(Infantry infantry, int share, int damageTaken) {
        Vector<Report> reports = new Vector<>();
        if (damageTaken <= 0) {
            Report noDamage = new Report(6445);
            noDamage.indent(3);
            noDamage.subject = infantry.getId();
            noDamage.add(infantry.getDisplayName());
            reports.addElement(noDamage);
            return reports;
        }
        Report damageReport = new Report(6450);
        damageReport.indent(3);
        damageReport.subject = infantry.getId();
        damageReport.add(share);
        damageReport.add(infantry.getDisplayName());
        reports.addElement(damageReport);
        int grouping = (infantry instanceof BattleArmor) ? BATTLE_ARMOR_GROUPING : damageTaken;
        int remaining = damageTaken;
        while (remaining > 0) {
            int next = Math.min(grouping, remaining);
            HitData hit = infantry.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
            reports.addAll(gameManager.damageEntity(infantry, hit, next));
            remaining -= next;
        }
        return reports;
    }
}
