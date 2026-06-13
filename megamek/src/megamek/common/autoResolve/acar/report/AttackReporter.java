/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.autoResolve.acar.report;

import static megamek.client.ui.clientGUI.tooltip.SBFInGameObjectTooltip.ownerColor;

import java.util.function.Consumer;

import megamek.common.autoResolve.acar.SimulationManager;
import megamek.common.autoResolve.component.Formation;
import megamek.common.game.IGame;
import megamek.common.rolls.Roll;
import megamek.common.rolls.TargetRoll;
import megamek.common.strategicBattleSystems.SBFUnit;

public record AttackReporter(IGame game, Consumer<PublicReportEntry> reportConsumer) implements IAttackReporter {

    public static IAttackReporter create(SimulationManager manager) {
        if (manager.isLogSuppressed()) {
            return DummyAttackReporter.instance();
        }
        return new AttackReporter(manager.getGame(), manager::addReport);
    }

    @Override
    public void reportAttackStart(Formation attacker, int unitNumber, Formation target, SBFUnit targetUnit) {
        var report = new PublicReportEntry("acar.firingPhase.attackAnnouncement");
        report.add(new UnitReportEntry(attacker, unitNumber, ownerColor(attacker, game)).text());
        report.add(new UnitReportEntry(target, targetUnit, ownerColor(target, game)).text());
        reportConsumer.accept(report);
    }

    @Override
    public void reportCannotSucceed(String toHitDesc) {
        reportConsumer.accept(new PublicReportEntry("acar.firingPhase.attackCannotSucceed").add(toHitDesc));
    }

    @Override
    public void reportToHitValue(TargetRoll toHitValue) {
        // e.g. "Needed X to hit"
        reportConsumer.accept(new PublicReportEntry("acar.firingPhase.hitThreshold").indent().add(toHitValue.getValue())
              .add(toHitValue.toString()));
    }

    @Override
    public void reportAttackRoll(Roll roll, Formation attacker) {
        var report = new PublicReportEntry("acar.initiative.rollAnnouncementCustom").indent().noNL();
        report.add(new PlayerNameReportEntry(game.getPlayer(attacker.getOwnerId())).text());
        report.add(new RollReportEntry(roll).reportText());
        reportConsumer.accept(report);
    }

    @Override
    public void reportAttackMiss() {
        reportConsumer.accept(new PublicReportEntry("acar.firingPhase.attackMisses").indent(1));
    }

    @Override
    public void reportAttackHit() {
        reportConsumer.accept(new PublicReportEntry("acar.firingPhase.attackHits").indent(1));
    }

    @Override
    public void reportDamageDealt(SBFUnit targetUnit, int damage, int newArmor) {
        reportConsumer.accept(new PublicReportEntry("acar.firingPhase.receivedDamage")
              .add(targetUnit.getName())
              .add(damage)
              .add(newArmor)
              .indent(2));
    }

    @Override
    public void reportStressEpisode() {
        reportConsumer.accept(new PublicReportEntry("acar.firingPhase.highStress").indent(3));
    }

    @Override
    public void reportUnitDestroyed() {
        reportConsumer.accept(new PublicReportEntry("acar.firingPhase.destroyed").indent(3));
    }

    @Override
    public void reportCriticalCheck() {
        // Called before rolling critical slots
        reportConsumer.accept(new PublicReportEntry("acar.firingPhase.internalDamageRoll").indent(3));
    }

    @Override
    public void reportNoCrit() {
        reportConsumer.accept(new PublicReportEntry("acar.firingPhase.noInternalDamage").indent(3));
    }

    @Override
    public void reportTargetingCrit(SBFUnit targetUnit) {
        reportConsumer.accept(new PublicReportEntry("acar.firingPhase.targetingDamage")
              .add(targetUnit.getName())
              .add(targetUnit.getTargetingCrits())
              .indent(3));
    }

    @Override
    public void reportDamageCrit(SBFUnit targetUnit) {
        reportConsumer.accept(new PublicReportEntry("acar.firingPhase.weaponDamage")
              .add(targetUnit.getName())
              .add(targetUnit.getDamageCrits())
              .indent(3));
    }

    @Override
    public void reportUnitCrippled() {
        reportConsumer.accept(new PublicReportEntry("acar.firingPhase.crippled").indent(3));
    }
}
