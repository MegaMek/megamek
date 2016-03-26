/**
 * MegaMek - Copyright (C) 2003 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
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

import java.io.Serializable;
import java.util.Objects;

public class Minefield implements Serializable, Cloneable {

    /**
     * 
     */
    private static final long serialVersionUID = 1556863068173491352L;
    public static final int TYPE_CONVENTIONAL = 0;
    public static final int TYPE_COMMAND_DETONATED = 1;
    public static final int TYPE_VIBRABOMB = 2;
    public static final int TYPE_ACTIVE = 3;
    public static final int TYPE_EMP = 4;
    public static final int TYPE_INFERNO = 5;

    public static final int TRIGGER_NONE = 0;

    public static final int CLEAR_NUMBER_WEAPON = 5;
    public static final int CLEAR_NUMBER_INFANTRY = 10;
    public static final int CLEAR_NUMBER_INFANTRY_ACCIDENT = 5;
    public static final int CLEAR_NUMBER_BA_SWEEPER = 6;
    public static final int CLEAR_NUMBER_BA_SWEEPER_ACCIDENT = 2;

    public static final int TO_HIT_SIDE = ToHitData.SIDE_FRONT;
    public static final int TO_HIT_TABLE = ToHitData.HIT_KICK;

    public static final int MAX_DAMAGE = 30;

    public static final String FILENAME_IMAGE = "minefieldsign.gif";

    private static String[] names = { "Conventional", "Command-detonated",
            "Vibrabomb", "Active", "EMP", "Inferno"};
            //"Thunder", "Thunder-Inferno", "Thunder-Active",
            //"Thunder-Vibrabomb" };
    
    public static int TYPE_SIZE = names.length;

    private Coords coords = null;
    private int playerId = IPlayer.PLAYER_NONE;
    //private int damage = 0;
    //private int secondaryDamage = 0;
    private int density = 5;
    private int type = -1;
    private int setting = 0;
    private boolean oneUse = false;
    private boolean sea = false;
    private int depth = 0;
    private boolean detonated = false;

    private Minefield() {
        //Creates a minefield
    }
    
    public static Minefield createMinefield(Coords coords, int playerId, int type, int density) {
        return createMinefield(coords, playerId, type, density, 0);
    }
    
    public static Minefield createMinefield(Coords coords, int playerId, int type, int density, boolean sea, int depth) {
        return createMinefield(coords, playerId, type, density, 0, sea, depth);
    }
    
    public static Minefield createMinefield(Coords coords, int playerId, int type, int density, int setting) {
        return createMinefield(coords, playerId, type, density, setting, false, 0);
    }

    public static Minefield createMinefield(Coords coords, int playerId, int type, int density, int setting, boolean sea, int depth) {
        Minefield mf = new Minefield();
        
        mf.type = type;
        mf.density = density;
        mf.coords = coords;
        mf.playerId = playerId;
        mf.setting = setting;
        mf.sea = sea;
        mf.depth = depth;
        return mf;
    }
    
    
    public static String getDisplayableName(int type) {
        if (type >= 0 && type < TYPE_SIZE) {
            return names[type];
        }
        throw new IllegalArgumentException("Unknown mine type");
    }

    @Override
    public Object clone() {
        Minefield mf = new Minefield();

        mf.playerId = playerId;
        mf.coords = coords;
        mf.density = density;
        mf.oneUse = oneUse;
        mf.type = type;
        mf.sea = sea;
        mf.depth = depth;

        return mf;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if((null == obj) || (getClass() != obj.getClass())) {
            return false;
        }
        final Minefield other = (Minefield) obj;
        return (playerId == other.playerId) && Objects.equals(coords, other.coords) && (type == other.type);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(playerId, coords, type);
    }

    public void setDensity(int density) {
        this.density = density;
    }

    public Coords getCoords() {
        return coords;
    }

    public int getDensity() {
        return density;
    }

    /**
     * what do we need to roll to trigger this mine
     * @return
     */
    public int getTrigger() {    
        if(density < 15) {
            return 9;
        } else if (density < 25) {
            return 8;
        } else {
            return 7;
        }
    }

    public boolean isSeaBased() {
        return sea;
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
    
    public int getDepth() {
        return depth;
    }

    public String getName() {
        return names[getType()];
    }

    public int getPlayerId() {
        return playerId;
    }
    
    public void setDetonated(boolean b) {
        this.detonated = b;
    }
    
    public boolean hasDetonated() {
        return detonated;
    }
    
    /**
     * check for a reduction in density
     * @param bonus - an <code>int</code> indicating the modifier to the target roll for reduction
     * @param direct - a <code>boolean</code> indicating whether this reduction was due to a direct explosion or
     *                    a result of another minefield in the same hex explodin
     */
    public void checkReduction(int bonus, boolean direct) {
        boolean isReduced = ((Compute.d6(2) + bonus) >= getTrigger()) || (direct && getType() != Minefield.TYPE_CONVENTIONAL && getType() != Minefield.TYPE_INFERNO);
        if(getType() == Minefield.TYPE_CONVENTIONAL && getDensity() < 10) {
            isReduced = false;
        }
        if(isReduced) {
            setDensity(getDensity() - 5);
        }    
    }
    
}
