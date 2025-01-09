/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common.autoresolve.acar.report;

import megamek.common.IGame;
import megamek.common.Roll;
import megamek.common.TargetRoll;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.component.Formation;
import megamek.common.strategicBattleSystems.SBFUnit;

import java.util.function.Consumer;

import static megamek.client.ui.swing.tooltip.SBFInGameObjectTooltip.ownerColor;

public class AttackReporter implements IAttackReporter {

    private final IGame game;
    private final Consumer<PublicReportEntry> reportConsumer;

    private AttackReporter(IGame game, Consumer<PublicReportEntry> reportConsumer) {
        this.reportConsumer = reportConsumer;
        this.game = game;
    }

    public static IAttackReporter create(SimulationManager manager) {
        if (manager.isLogSuppressed()) {
            return DummyAttackReporter.instance();
        }
        return new AttackReporter(manager.getGame(), manager::addReport);
    }

    @Override
    public void reportAttackStart(Formation attacker, int unitNumber, Formation target) {
        var report = new PublicReportEntry("acar.firingPhase.attackAnnouncement");
        report.add(new UnitReportEntry(attacker, unitNumber, ownerColor(attacker, game)).text());
        report.add(new FormationReportEntry(target, game).text());
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
        // Called before rolling criticals
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
