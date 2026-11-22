/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2011-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons;

import java.util.OptionalInt;
import java.util.Vector;

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.compute.Compute;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.rolls.Roll;
import megamek.common.units.Aero;
import megamek.common.units.ConvFighter;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.Tank;
import megamek.server.totalWarfare.TWGameManager;

/** The Taser Effects Table (TO:AUE pp.157-158), also used by EDP armor with a -2 modifier (IO:AE p.59). */
public enum TaserEffects {
    MEK, BATTLE_ARMOR, EDP;

    public void resolve(TWGameManager gameManager, Entity entityTarget, HitData hit, int armorBefore,
          Vector<Report> reports) {
        if (entityTarget.getWeight() > 100 || entityTarget.isConventionalInfantry()) {
            return;
        }
        int modifier = this == EDP ? -2 : 0;
        if (armorBefore > 0 && entityTarget.getArmorType(hit.getLocation()) == EquipmentType.T_ARMOR_ANTI_PENETRATIVE_ABLATION) {
            // Core p.201 negates the special effect. IO:AE p.80 modifies the effects roll if armor remains.
            OptionalInt ablation = Game.rulesManager.getRulesArmor().ablationArmorPiercingModifier();
            if (ablation.isEmpty()) {
                Report blocked = new Report(3702);
                blocked.subject = entityTarget.getId();
                reports.add(blocked);
                return;
            }
            if (entityTarget.getArmor(hit) > 0) {
                modifier += ablation.getAsInt();
            }
        }

        // Taser Effects Table, TO:AUE pp.157-158. Damage has already been applied to this location.
        boolean baTaser = this == BATTLE_ARMOR;
        Roll roll = Compute.rollD6(2);
        int result = roll.getIntValue() + modifier;
        Report report = new Report(this == EDP ? 3701 : 3700);
        if (modifier == 0) {
            report.add(roll);
        } else {
            report.addDataWithTooltip(result + " [" + roll.getIntValue() + " - " + -modifier + "]", roll.getReport());
        }
        report.newlines = 0;
        reports.add(report);

        if (entityTarget instanceof BattleArmor) {
            if ((!baTaser || result >= 9) && entityTarget.getInternal(hit) > 0) {
                report = new Report(3706);
                report.addDesc(entityTarget);
                report.add(entityTarget.getLocationAbbr(hit));
                reports.add(report);
                entityTarget.destroyLocation(hit.getLocation());
                if (entityTarget.getTransferLocation(hit).getLocation() == Entity.LOC_DESTROYED) {
                    reports.addAll(gameManager.destroyEntity(entityTarget, "all troopers eliminated", false));
                }
            }
            return;
        }
        if (!(entityTarget instanceof Mek || entityTarget instanceof ProtoMek || entityTarget instanceof Tank
              || entityTarget instanceof Aero)) {
            return;
        }
        boolean battleMek = entityTarget instanceof Mek mek && !mek.isIndustrial();
        boolean aerospace = entityTarget instanceof Aero && !(entityTarget instanceof ConvFighter);
        int shutdown = baTaser ? (battleMek ? 12 : 11) : (battleMek || aerospace ? 11 : 8);
        int turns = baTaser || battleMek || aerospace ? 3 : 4;
        if (result >= shutdown) {
            entityTarget.taserShutdown(turns, baTaser);
            report = new Report(3705);
            report.addDesc(entityTarget);
        } else {
            int interference = baTaser ? 1 : 2;
            entityTarget.setTaserInterference(interference, turns, !baTaser && (entityTarget instanceof Mek || aerospace));
            report = new Report(3710);
            report.addDesc(entityTarget);
            report.add(interference);
        }
        report.add(turns);
        reports.add(report);
    }
}
