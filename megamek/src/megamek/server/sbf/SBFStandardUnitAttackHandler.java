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
package megamek.server.sbf;

import megamek.common.Compute;
import megamek.common.Roll;
import megamek.common.actions.sbf.SBFStandardUnitAttack;
import megamek.common.strategicBattleSystems.*;

import java.util.List;

import static megamek.client.ui.swing.tooltip.SBFInGameObjectTooltip.ownerColor;

public class SBFStandardUnitAttackHandler extends AbstractSBFActionHandler {

    public SBFStandardUnitAttackHandler(SBFStandardUnitAttack action, SBFGameManager gameManager) {
        super(action, gameManager);
    }

    @Override
    public boolean cares() {
        return game().getPhase().isFiring();
    }

    @Override
    public void handle() {
        SBFStandardUnitAttack attack = (SBFStandardUnitAttack) getAction();
        if (attack.isDataValid(game())) {
            //noinspection OptionalGetWithoutIsPresent
            SBFFormation attacker = game().getFormation(attack.getEntityId()).get();
            //noinspection OptionalGetWithoutIsPresent
            SBFFormation target = game().getFormation(attack.getTargetId()).get();
            SBFUnit attackingUnit = attacker.getUnits().get(attack.getUnitNumber());
            List<SBFUnit> targetUnits = target.getUnits();
            SBFUnit targetUnit = targetUnits.get(0);

            SBFToHitData toHit = SBFToHitData.compiletoHit(game(), attack);
            SBFReportEntry report = new SBFReportEntry(2001).noNL();
            report.add(new SBFUnitReportEntry(attacker, attack.getUnitNumber(), ownerColor(attacker, game())).text());
            report.add(new SBFFormationReportEntry(target, game()).text());
            addReport(report);

            if (toHit.cannotSucceed()) {
                addReport(new SBFReportEntry(2010).add(toHit.getDesc()));
            } else {
                addReport(new SBFReportEntry(2003).add(toHit.getValue()).noNL());
                Roll roll = Compute.rollD6(2);
                report = new SBFReportEntry(2020).noNL();
                report.add(new SBFPlayerNameReportEntry(game().getPlayer(attacker.getOwnerId())).text());
                report.add(new SBFRollReportEntry(roll).noNL().text());
                addReport(report);

                if (roll.getIntValue() < toHit.getValue()) {
                    addReport(new SBFPublicReportEntry(2012));
                } else {
                    addReport(new SBFPublicReportEntry(2013));
                    int damage = attackingUnit.getCurrentDamage().getDamage(attack.getRange()).damage;
                    if (damage > 0) {
                        int newArmor = targetUnit.getCurrentArmor() - damage;
                        addReport(new SBFPublicReportEntry(3100).add(damage).add(damage));
                        if (newArmor < 0) {
                            newArmor = 0;
                        }
                        targetUnits.get(0).setCurrentArmor(newArmor);
                        if (newArmor == 0) {
                            addReport(new SBFPublicReportEntry(3092));
                        }
                        if (newArmor * 2 < targetUnit.getArmor()) {
                            targetUnit.addDamageCrit();
                        }
                        gameManager().sendUnitUpdate(target);
                    } else {
                        addReport(new SBFPublicReportEntry(3068));
                    }
                }
            }
        }
        setFinished();
    }
}
