/*
 * MegaMek - Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
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

import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.Serializable;
import java.util.Objects;

/**
 * @author dirk
 */
public class SpecialHexDisplay implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 27470795993329492L;

    public enum Type {
        ARTILLERY_AUTOHIT(new File(Configuration.hexesDir(), 
                "artyauto.gif").toString()) { //$NON-NLS-1$
            @Override
            public boolean drawBefore() {
                return false;
            }

            @Override
            public boolean drawAfter() {
                return true;
            }
        },
        ARTILLERY_ADJUSTED(new File(Configuration.hexesDir(), 
                "artyadj.gif").toString()) { //$NON-NLS-1$
            @Override
            public boolean drawBefore() {
                return false;
            }

            @Override
            public boolean drawAfter() {
                return true;
            }
        },
        ARTILLERY_INCOMING(new File(Configuration.hexesDir(), 
                "artyinc.gif").toString()), //$NON-NLS-1$
        ARTILLERY_TARGET(new File(Configuration.hexesDir(), 
                "artytarget.gif").toString()) { //$NON-NLS-1$
            @Override
            public boolean drawBefore() {
                return false;
            }
        },
        ARTILLERY_HIT(new File(Configuration.hexesDir(), 
                "artyhit.gif").toString()) { //$NON-NLS-1$
            @Override
            public boolean drawBefore() {
                return false;
            }
        },
        PLAYER_NOTE(new File(Configuration.hexesDir(), 
                "note.png").toString()) { //$NON-NLS-1$
            @Override
            public boolean drawBefore() {
                return true;
            }

            @Override
            public boolean drawAfter() {
                return true;
            }
        };

        private transient Image defaultImage;
        private final String defaultImagePath;

        Type(String iconPath) {
            defaultImagePath = iconPath;
        }

        public void init(Toolkit toolkit) {
            if (defaultImagePath != null) {
                defaultImage = toolkit.getImage(defaultImagePath);
            }

        }

        public Image getDefaultImage() {
            return defaultImage;
        }

        public void setDefaultImage(Image defaultImage) {
            this.defaultImage = defaultImage;
        }

        public boolean drawBefore() {
            return true;
        }

        public boolean drawAfter() {
            return false;
        }
    }

    /**
     * Defines that only the owner can see an obscured display.
     */
    public static int SHD_OBSCURED_OWNER = 0;
    /**
     * Defines that only the owner and members of his team can see an obscured
     * display.
     */
    public static int SHD_OBSCURED_TEAM = 1;
    /**
     * Defines that everyone can see an obscured display.
     */
    public static int SHD_OBSCURED_ALL = 2;
    
    
    private String info;
    private Type type;
    private int round;

    private IPlayer owner = null;

    private int obscured = SHD_OBSCURED_ALL;

    public static int NO_ROUND = -99;

    public SpecialHexDisplay(Type type, int round, IPlayer owner, String info) {
        this.type = type;
        this.info = info;
        this.round = round;
        this.owner = owner;
    }

    public SpecialHexDisplay(Type type, int round, IPlayer owner, String info,
            int obscured) {
        this.type = type;
        this.info = info;
        this.round = round;
        this.owner = owner;
        this.obscured = obscured;
    }

    public boolean thisRound(int testRound) {
        if (NO_ROUND == round) {
            return true;
        }
        return testRound == round;
    }

    /** Does this SpecialHexDisplayObjet concern a round in the future? */
    public boolean futureRound(int testRound) {
        if (NO_ROUND == round) {
            return true;
        }
        return testRound > round;
    }

    /** Does this SpecialHexDisplayObjet concern a round in the past? */
    public boolean pastRound(int testRound) {
        if (NO_ROUND == round) {
            return true;
        }
        return testRound < round;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public IPlayer getOwner() {
        return owner;
    }

    public void setOwner(IPlayer owner) {
        this.owner = owner;
    }

    public void setObscuredLevel(int o){
        if (o >= SHD_OBSCURED_OWNER && o <= SHD_OBSCURED_ALL){
            obscured = o;
        }
    }
    
    public int getObscuredLevel(){
        return obscured;
    }
    
    /**
     * Determines whether this special hex should be obscurred from the given
     * <code>IPlayer</code>.
     * 
     * @param other
     * @return
     */
    public boolean isObscured(IPlayer other) {
        if (obscured == SHD_OBSCURED_OWNER && other != null 
                && owner.equals(other)){
            return false;
        } else if (obscured == SHD_OBSCURED_TEAM && other != null 
                && owner.getTeam() == other.getTeam()){
            return false;
        } else if (obscured == SHD_OBSCURED_ALL){
            return false;
        }
        return true;
    }

    public void setObscured(int obscured) {
        this.obscured = obscured;
    }

    /**
     * @param phase
     * @param curRound
     * @return
     */
    public boolean drawNow(IGame.Phase phase, int curRound,
            IPlayer playerChecking) {
        boolean shouldDisplay = thisRound(curRound)
                || (pastRound(curRound) && type.drawBefore())
                || (futureRound(curRound) && type.drawAfter());

        if (phase.isBefore(IGame.Phase.PHASE_OFFBOARD)
                && ((type == Type.ARTILLERY_TARGET) 
                        || (type == Type.ARTILLERY_HIT))) {
            shouldDisplay = shouldDisplay || thisRound(curRound - 1);
        }
        
        // Arty icons for the owner are drawn in BoardView1.drawArtillery
        //  and shouldn't be drawn twice
        if (isOwner(playerChecking)
                && (type == Type.ARTILLERY_AUTOHIT
                        || type == Type.ARTILLERY_ADJUSTED
                        || type == Type.ARTILLERY_INCOMING 
                        || type == Type.ARTILLERY_TARGET)){
            return false;
        }

        // Only display obscured hexes to owner
        if (isObscured(playerChecking)) {
            return false;
        }

        return shouldDisplay;
    }

    /**
     * @param toPlayer
     * @return
     */
    public boolean isOwner(IPlayer toPlayer) {
        if ((owner == null) || owner.equals(toPlayer)) {
            return true;
        }
        return false;
    }
    
    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if((null == obj) || (getClass() != obj.getClass())) {
            return false;
        }
        final SpecialHexDisplay other = (SpecialHexDisplay) obj;
        return (type == other.type) && Objects.equals(owner, other.owner) && (round == other.round);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(type, owner, round);
    }
    
    public String toString() {
        return "SHD: " + type.name() + ", " + "round " + round + ", by "
                + owner.getName();
    }
}
