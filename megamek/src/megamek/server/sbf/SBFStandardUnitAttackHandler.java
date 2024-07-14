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

import megamek.common.Report;
import megamek.common.actions.EntityAction;
import megamek.common.actions.sbf.SBFStandardUnitAttack;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFUnit;

import java.util.List;
import java.util.Optional;

import static org.apache.logging.log4j.LogManager.*;

public class SBFStandardUnitAttackHandler extends AbstractSBFActionHandler {

    public SBFStandardUnitAttackHandler(EntityAction action, SBFGameManager gameManager) {
        super(action, gameManager);
        if (!(action instanceof SBFStandardUnitAttack)) {
            throw new IllegalArgumentException("Can only be used for standard unit attacks");
        }
    }

    @Override
    public boolean cares() {
        return game().getPhase().isFiring();
    }

    @Override
    public void handle() {
        if (validate()) {
            SBFStandardUnitAttack attack = (SBFStandardUnitAttack) getAction();
            //noinspection OptionalGetWithoutIsPresent
            SBFFormation attacker = game().getFormation(attack.getEntityId()).get();
            //noinspection OptionalGetWithoutIsPresent
            SBFFormation target = game().getFormation(attack.getTargetId()).get();
            // Unit number is 1 based
            SBFUnit unit = attacker.getUnits().get(attack.getUnitNumber() - 1);
            List<SBFUnit> targetUnits = target.getUnits();
            SBFUnit targetUnit = targetUnits.get(0);
            int damage = unit.getCurrentDamage().M.damage;
            if (damage > 0) {
                int newArmor = targetUnit.getCurrentArmor() - damage;
                addReport(Report.publicReport(3100).add(damage).add(damage));
                if (newArmor < 0) {
                    newArmor = 0;
                }
                targetUnits.get(0).setCurrentArmor(newArmor);
                if (newArmor == 0) {
                    addReport(Report.publicReport(3092));
                }
                gameManager().sendUnitUpdate(target);
            } else {
                addReport(Report.publicReport(3068));
            }
        }
        setFinished();
    }

    private boolean validate() {
        SBFStandardUnitAttack attack = (SBFStandardUnitAttack) getAction();
        Optional<SBFFormation> possibleAttacker = game().getFormation(attack.getEntityId());
        Optional<SBFFormation> possibleTarget = game().getFormation(attack.getTargetId());
        if (attack.getEntityId() == attack.getTargetId()) {
            getLogger().error("Formations cannot attack themselves! {}", attack);
            return false;
        } else if (possibleAttacker.isEmpty()) {
            getLogger().error("Could not find attacking formation! {}", attack);
            return false;
        } else if (possibleTarget.isEmpty()) {
            getLogger().error("Could not find target formation! {}", attack);
            return false;
        } else if (possibleAttacker.get().getUnits().size() < attack.getUnitNumber()
                || attack.getUnitNumber() < 1) {
            getLogger().error("SBF Unit not found! {}", attack);
            return false;
        } else if (possibleTarget.get().getUnits().isEmpty()) {
            getLogger().error("Target has no units! {}", attack);
            return false;
        }
        return true;
    }

}
