/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.FighterSquadron;
import megamek.common.HitData;
import megamek.common.Game;
import megamek.common.Report;
import megamek.common.TargetRoll;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.GamePhase;
import megamek.server.GameManager;

/**
 * @author Jay Lawson
 */
public class ScreenLauncherBayHandler extends AmmoBayWeaponHandler {

    /**
     * 
     */

    private static final long serialVersionUID = -1618484541772117621L;

    /**
     * @param t
     * @param w
     * @param g
     * @param m
     */
    public ScreenLauncherBayHandler(ToHitData t, WeaponAttackAction w, Game g,
            GameManager m) {
        super(t, w, g, m);
    }

    /**
     * handle this weapons firing
     * 
     * @return a <code>boolean</code> value indicating wether this should be
     *         kept or not
     */
    @Override
    public boolean handle(GamePhase phase, Vector<Report> vPhaseReport) {
        if (!this.cares(phase)) {
            return true;
        }

        // same as ScreenLauncher handler, except run multiple times depending
        // on
        // how many screen launchers in bay

        // Report weapon attack and its to-hit value.
        Report r = new Report(3115);
        r.indent();
        r.newlines = 0;
        r.subject = subjectId;
        r.add(wtype.getName());
        r.messageId = 3120;
        r.add(target.getDisplayName(), true);
        vPhaseReport.addElement(r);
        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            r = new Report(3135);
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
            return false;
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
            r = new Report(3140);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            r = new Report(3145);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
        }

        addHeat();

        // iterate through by number of weapons in bay
        for (int i = 0; i < weapon.getBayWeapons().size(); i++) {
            // deliver screen
            Coords coords = target.getPosition();
            gameManager.deliverScreen(coords, vPhaseReport);
            // damage any entities in the hex
            for (Entity entity : game.getEntitiesVector(coords)) {
                // if fighter squadron all fighters are damaged
                if (entity instanceof FighterSquadron) {
                    entity.getSubEntities().forEach(
                    ent -> {
                        ToHitData squadronToHit = new ToHitData();
                        squadronToHit.setHitTable(ToHitData.HIT_NORMAL);
                        HitData hit = ent.rollHitLocation(squadronToHit.getHitTable(), ToHitData.SIDE_FRONT);
                        hit.setCapital(false);
                        vPhaseReport.addAll(gameManager.damageEntity(ent, hit, attackValue));
                        gameManager.creditKill(ent, ae);
                    });
                } else {
                    ToHitData hexToHit = new ToHitData();
                    hexToHit.setHitTable(ToHitData.HIT_NORMAL);
                    HitData hit = entity.rollHitLocation(hexToHit.getHitTable(), ToHitData.SIDE_FRONT);
                    hit.setCapital(false);
                    vPhaseReport.addAll(gameManager.damageEntity(entity, hit, attackValue));
                    gameManager.creditKill(entity, ae);
                }
            }
        }
        return false;
    }

}
