/**
 * MegaMek - Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
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

package megamek.common;

import java.io.*;

public class Minefield implements Serializable, Cloneable {

    public static final int TYPE_CONVENTIONAL = 0;
    public static final int TYPE_COMMAND_DETONATED = 1;
    public static final int TYPE_VIBRABOMB = 2;
    public static final int TYPE_THUNDER = 3;
    public static final int TYPE_THUNDER_INFERNO = 4;
    public static final int TYPE_THUNDER_ACTIVE = 5;
    public static final int TYPE_THUNDER_VIBRABOMB = 6;

    public static final int TRIGGER_NONE = 0;

    public static final int CLEAR_NUMBER_WEAPON = 5;
    public static final int CLEAR_NUMBER_INFANTRY = 10;
    public static final int CLEAR_NUMBER_INFANTRY_ACCIDENT = 5;
    public static final int CLEAR_NUMBER_SWEEPER = 6;
    public static final int CLEAR_NUMBER_SWEEPER_ACCIDENT = 2;

    public static final int TO_HIT_SIDE = ToHitData.SIDE_FRONT;
    public static final int TO_HIT_TABLE = ToHitData.HIT_KICK;

    public static final int MAX_DAMAGE = 20;

    public static final String IMAGE_FILE = "data/hexes/minefieldsign.gif";

    private static String[] names = {"Conventional", "Command-detonated", "Vibrabomb", "Thunder", "Thunder-Inferno", "Thunder-Active", "Thunder-Vibrabomb"};
    
    private Coords coords = null;
    private int playerId = Player.PLAYER_NONE;
    private int damage = 0;
    private int secondaryDamage = 0;
    private int setting = 0;
    private int trigger = TRIGGER_NONE;
    private int type = -1;
    private boolean areaEffect = false;
    private boolean oneUse = false;

    private Minefield() {
    }

    public static Minefield createConventionalMF(Coords coords, int playerId) {
        Minefield mf = new Minefield();

        mf.damage = 6;
        mf.type = TYPE_CONVENTIONAL;
        mf.trigger = 7;
        mf.coords = coords;
        mf.playerId = playerId;
        return mf;
    }

    public static Minefield createCommandDetonatedMF(Coords coords, int playerId) {
        Minefield mf = new Minefield();
        
        mf.damage = 10;
        mf.secondaryDamage = 4;
        mf.areaEffect = true;
        mf.oneUse = true;
        mf.type = TYPE_COMMAND_DETONATED;
        mf.coords = coords;
        mf.playerId = playerId;
        return mf;
    }

    public static Minefield createVibrabombMF(Coords coords, int playerId, int setting) {
        Minefield mf = new Minefield();
        
        mf.damage = 10;
        mf.areaEffect = true;
        mf.oneUse = true;
        mf.setting = setting;
        mf.type = TYPE_VIBRABOMB;
        mf.coords = coords;
        mf.playerId = playerId;
        return mf;
    }
    
    public static Minefield createThunderVibrabombMF(Coords coords, int playerId, int damage, int setting) {
        Minefield mf = new Minefield();
        
        mf.damage = damage;
        mf.areaEffect = true;
        mf.oneUse = true;
        mf.setting = setting;
        mf.type = TYPE_VIBRABOMB;
        mf.coords = coords;
        mf.playerId = playerId;
        return mf;
    }
    
    public static Minefield createThunderMF(Coords coords, int playerId, int damage) {
        Minefield mf = new Minefield();
        
        mf.damage = damage;
        mf.type = TYPE_THUNDER;
        mf.trigger = 7;
        mf.coords = coords;
        mf.playerId = playerId;
        return mf;
    }
    
    public static Minefield createThunderInfernoMF(Coords coords, int playerId, int damage) {
        Minefield mf = new Minefield();
        
        mf.damage = damage;
        mf.type = TYPE_THUNDER_INFERNO;
        mf.trigger = 7;
        mf.coords = coords;
        mf.playerId = playerId;
        return mf;
    }
    
    public static Minefield createThunderActiveMF(Coords coords, int playerId, int damage) {
        Minefield mf = new Minefield();
        
        mf.damage = damage;
        mf.type = TYPE_THUNDER_ACTIVE;
        mf.trigger = 7;
        mf.coords = coords;
        mf.playerId = playerId;
        return mf;
    }
    
    public Object clone() {
        Minefield mf = new Minefield();
        
        mf.playerId = playerId;
        mf.coords = coords;
        mf.damage = damage;
        mf.secondaryDamage = secondaryDamage;
        mf.areaEffect = areaEffect;
        mf.oneUse = oneUse;
        mf.setting = setting;
        mf.type = type;
        
        return mf;
    }

    public boolean equals(Object object) {
        Minefield mf;
        try {
            mf = (Minefield) object;
        } catch (Exception e) {
            return false;
        }
        
        if (
            mf.playerId == this.playerId &&
            mf.coords.equals(coords) &&
            mf.type == this.type) {
            return true;
        } else {
            return false;
        }
    }
    
    public void setDamage(int damage) {
        this.damage = damage;
    }
    
    public Coords getCoords() {
        return coords;
    }

    public int getDamage() {
        return damage;
    }

    public int getSecondaryDamage() {
        return secondaryDamage;
    }

    public int getTrigger() {
        return trigger;
    }

    public boolean isAreaEffect() {
        return areaEffect;
    }

    public boolean isOneUse() {
        return oneUse;
    }

    public int getSetting() {
        return setting;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return names[getType()];
    }

    public int getPlayerId() {
        return playerId;
    }

}
