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
import megamek.common.autoresolve.component.Formation;
import megamek.common.strategicBattleSystems.SBFUnit;

import java.util.function.Consumer;

import static megamek.client.ui.swing.tooltip.SBFInGameObjectTooltip.ownerColor;

public class AttackReporter {

    private final IGame game;
    private final Consumer<PublicReportEntry> reportConsumer;

    public AttackReporter(IGame game, Consumer<PublicReportEntry> reportConsumer) {
        this.reportConsumer = reportConsumer;
        this.game = game;
    }
    public void reportAttackStart(Formation attacker, int unitNumber, Formation target) {
        var report = new PublicReportEntry(2001).noNL();
        report.add(new UnitReportEntry(attacker, unitNumber, ownerColor(attacker, game)).text());
        report.add(new FormationReportEntry(target, game).text());
        reportConsumer.accept(report);
    }

    public void reportCannotSucceed(String toHitDesc) {
        reportConsumer.accept(new PublicReportEntry(2010).add(toHitDesc));
    }

    public void reportToHitValue(TargetRoll toHitValue) {
        // e.g. "Needed X to hit"
        reportConsumer.accept(new PublicReportEntry(2003).indent().add(toHitValue.getValue())
            .add(toHitValue.toString()));
    }

    public void reportAttackRoll(Roll roll, Formation attacker) {
        var report = new PublicReportEntry(2020).indent();
        report.add(new PlayerNameReportEntry(game.getPlayer(attacker.getOwnerId())).text());
        report.add(new RollReportEntry(roll).reportText());
        reportConsumer.accept(report);
    }

    public void reportAttackMiss() {
        reportConsumer.accept(new PublicReportEntry(2012).indent(2));
    }

    public void reportAttackHit() {
        reportConsumer.accept(new PublicReportEntry(2013).indent(2));
    }

    public void reportDamageDealt(SBFUnit targetUnit, int damage, int newArmor) {
        reportConsumer.accept(new PublicReportEntry(3100)
            .add(targetUnit.getName())
            .add(damage)
            .add(newArmor)
            .indent(2));
    }

    public void reportStressEpisode() {
        reportConsumer.accept(new PublicReportEntry(3090).indent(3));
    }

    public void reportUnitDestroyed() {
        reportConsumer.accept(new PublicReportEntry(3092).indent(3));
    }

    public void reportCriticalCheck() {
        // Called before rolling criticals
        reportConsumer.accept(new PublicReportEntry(3095).indent(3));
    }

    public void reportNoCrit() {
        reportConsumer.accept(new PublicReportEntry(3097).indent(3));
    }

    public void reportTargetingCrit(SBFUnit targetUnit) {
        reportConsumer.accept(new PublicReportEntry(3094)
            .add(targetUnit.getName())
            .add(targetUnit.getTargetingCrits())
            .indent(3));
    }

    public void reportDamageCrit(SBFUnit targetUnit) {
        reportConsumer.accept(new PublicReportEntry(3096)
            .add(targetUnit.getName())
            .add(targetUnit.getDamageCrits())
            .indent(3));
    }

    public void reportUnitCrippled() {
        reportConsumer.accept(new PublicReportEntry(3091).indent(3));
    }
}
