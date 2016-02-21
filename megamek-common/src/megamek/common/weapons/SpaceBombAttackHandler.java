/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
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
/*
 * Created on Sep 23, 2004
 *
 */
package megamek.common.weapons;

import java.util.Vector;

import megamek.common.Aero;
import megamek.common.BombType;
import megamek.common.FighterSquadron;
import megamek.common.HitData;
import megamek.common.IGame;
import megamek.common.Mounted;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.Server;

/**
 * @author Jay Lawson
 */
public class SpaceBombAttackHandler extends WeaponHandler {
    /**
     * 
     */
    private static final long serialVersionUID = -2439937071168853215L;

    // int[] payload;

    /**
     * @param toHit
     * @param waa
     * @param g
     */
    public SpaceBombAttackHandler(ToHitData toHit, WeaponAttackAction waa,
            IGame g, Server s) {
        super(toHit, waa, g, s);
        generalDamageType = HitData.DAMAGE_NONE;
        // payload = waa.getBombPayload();
    }

    /**
     * Calculate the attack value based on range
     * 
     * @return an <code>int</code> representing the attack value at that range.
     */
    @Override
    protected int calcAttackValue() {
        int[] payload = waa.getBombPayload();
        if (null == payload) {
            return 0;
        }
        int nbombs = 0;
        for (int i = 0; i < payload.length; i++) {
            nbombs += payload[i];
        }
        if (bDirect) {
            nbombs = Math.min(nbombs + (toHit.getMoS() / 3), nbombs * 2);
        }
        if (bGlancing) {
            nbombs = (int) Math.floor(nbombs / 2.0);

        }
        return nbombs;
    }

    /**
     * Does this attack use the cluster hit table? necessary to determine how
     * Aero damage should be applied
     */
    @Override
    protected boolean usesClusterTable() {
        return true;
    }

    @Override
    protected void useAmmo() {
        int[] payload = waa.getBombPayload();
        if (!(ae instanceof Aero) || null == payload) {
            return;
        }
        
        // Need to remove ammo from fighters within a squadron
        if (ae instanceof FighterSquadron){
            // In a fighter squadron, we will haved dropped a salvo of bombs.
            //  The salvo consists of one bomb from each fighter equipped with
            //  a bomb of the proper type.  
            for (int type = 0; type < payload.length; type++) {
                Vector<Aero> fighters = ((FighterSquadron)ae).getFighters();
                int fighterIndex = 0;                                
                for (int i = 0; i < payload[type]; i++) {
                    boolean bombRemoved = false;
                    int iterations = 0;
                    while (!bombRemoved && iterations <= fighters.size())
                    {
                        Aero fighter = fighters.get(fighterIndex);
                        // find the first mounted bomb of this type and drop it
                        for (Mounted bomb : fighter.getBombs()) {
                            if (((BombType) bomb.getType()).getBombType() == type && 
                                    !bomb.isDestroyed()
                                    && bomb.getUsableShotsLeft() > 0) {
                                bomb.setShotsLeft(0);                                
                                bombRemoved = true;
                                break;
                            }
                        }
                        iterations++;
                        fighterIndex = (fighterIndex + 1) % fighters.size();
                    }
                    if (iterations > fighters.size()){
                        System.err.println("Error: couldn't find ammo for a " +
                                "dropped bomb in SpaceBombAttackHandler.useAmmo()");
                    }                    
                }
                // Now remove a bomb from the squadron
                if (payload[type] > 0){
                    double numSalvos = Math.ceil((payload[type] + 0.0)
                            / ((FighterSquadron) ae).getNFighters());
                    for (int salvo = 0; salvo < numSalvos; salvo++){
                        for (Mounted bomb : ae.getBombs()) {
                            if (((BombType) bomb.getType()).getBombType() == type
                                    && !bomb.isDestroyed()
                                    && bomb.getUsableShotsLeft() > 0) {
                                bomb.setShotsLeft(0);
                                break;
                            }
                        }  
                    }
                }
            }
        }else{ // Ammo expenditure for a single fighter        
            for (int type = 0; type < payload.length; type++) {
                for (int i = 0; i < payload[type]; i++) {
                    // find the first mounted bomb of this type and drop it
                    for (Mounted bomb : ae.getBombs()) {
                        if (((BombType) bomb.getType()).getBombType() == type && 
                                !bomb.isDestroyed()
                                && bomb.getUsableShotsLeft() > 0) {
                            bomb.setShotsLeft(0);
                            break;
                        }
                    }
                }
            }
        }
        
        super.useAmmo();
    }
}
