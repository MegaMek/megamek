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

import java.util.Enumeration;
import java.util.Vector;

import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.FighterSquadron;
import megamek.common.HitData;
import megamek.common.IGame;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

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
     * @param s
     */
    public ScreenLauncherBayHandler(ToHitData t, WeaponAttackAction w, IGame g, Server s) {
        super(t, w, g, s);
    }
    
    /**
     * handle this weapons firing
     * 
     * @return a <code>boolean</code> value indicating wether this should be
     *         kept or not
     */
    public boolean handle(IGame.Phase phase, Vector<Report> vPhaseReport) {
        if (!this.cares(phase)) {
            return true;
        }
     
        //same as ScreenLauncher handler, except run multiple times depending on
        //how many screen launchers in bay
        
        //Report weapon attack and its to-hit value.
        r = new Report(3115);
        r.indent();
        r.newlines = 0;
        r.subject = subjectId;
        r.add(wtype.getName());
        r.messageId = 3120;
        r.add(target.getDisplayName(), true);
        vPhaseReport.addElement(r);
        if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
            r = new Report(3135);
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
            return false;
        } else if (toHit.getValue() == ToHitData.AUTOMATIC_FAIL) {
            r = new Report(3140);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
        } else if (toHit.getValue() == ToHitData.AUTOMATIC_SUCCESS) {
            r = new Report(3145);
            r.newlines = 0;
            r.subject = subjectId;
            r.add(toHit.getDesc());
            vPhaseReport.addElement(r);
        }
        
        addHeat();
        
        //iterate through by number of weapons in bay
        for(int i = 0; i < weapon.getBayWeapons().size(); i++) {      
            //deliver screen
            Coords coords = target.getPosition();
            server.deliverScreen(coords, vPhaseReport);       
            //damage any entities in the hex
            for (Enumeration<Entity> impactHexHits = game.getEntities(coords);impactHexHits.hasMoreElements();) {
                Entity entity = impactHexHits.nextElement();
                //if fighter squadron all fighters are damaged
                if(entity instanceof FighterSquadron) {
                    for(Entity fighter : ((FighterSquadron)entity).getFighters()) {
                        ToHitData toHit = new ToHitData();
                        toHit.setHitTable(ToHitData.HIT_NORMAL);
                        HitData hit = fighter.rollHitLocation(toHit.getHitTable(), ToHitData.SIDE_FRONT);
                        hit.setCapital(false);
                        vPhaseReport.addAll( server.damageEntity(fighter, hit, attackValue));
                        server.creditKill(fighter, ae);
                    }
                } else {  
                    ToHitData toHit = new ToHitData();
                    toHit.setHitTable(ToHitData.HIT_NORMAL);
                    HitData hit = entity.rollHitLocation(toHit.getHitTable(), ToHitData.SIDE_FRONT);
                    hit.setCapital(false);
                    vPhaseReport.addAll( server.damageEntity(entity, hit, attackValue));
                    server.creditKill(entity, ae);
                }
            }
        }
        return false;
    }
    
}
