/*
 * MegaMek - Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
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
import java.io.Serializable;

/**
 * @author dirk
 */
public class SpecialHexDisplay implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 27470795993329492L;

    public enum Type {
        ARTILLERY_AUTOHIT   ("data/images/hexes/artyauto.gif") {
            public boolean drawBefore() {
                return false;
            }

            public boolean drawAfter() {
                return true;
            }
        },
        ARTILLERY_ADJUSTED  ("data/images/hexes/artyadj.gif") {
            public boolean drawBefore() {
                return false;
            }

            public boolean drawAfter() {
                return true;
            }
        },
        ARTILLERY_INCOMING   ("data/images/hexes/artyinc.gif"),
        ARTILLERY_TARGET      ("data/images/hexes/artytarget.gif"){
            public boolean drawBefore() {
                return false;
            }
        },
        ARTILLERY_HIT        ("data/images/hexes/artyhit.gif") {
            public boolean drawBefore() {
                return false;
            }
        },
        PLAYER_NOTE         (null);
    
        private transient Image defaultImage;
        private final String defaultImagePath;

        Type(String iconPath) {
            defaultImagePath = iconPath;
        }

        public void init(Toolkit toolkit) {
            if(defaultImagePath != null) {
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
    };

    private String info;
    private Type type;
    private int round;

    private String owner = null;
    
    private boolean obscured = true;
    
    public static int NO_ROUND = -99;

    /**
     * Special constructor only for deserialization use.
     *
     */
    @SuppressWarnings("unused")
    private SpecialHexDisplay() {
    }
    
    public SpecialHexDisplay(Type type) {
        this.type = type;
        round = NO_ROUND;
    }

    public SpecialHexDisplay(Type type, String info) {
        this.type = type;
        this.info = info;
        round = NO_ROUND;
    }

    public SpecialHexDisplay(Type type, int round, String info) {
        this.type = type;
        this.info = info;
        this.round = round;
    }

    public SpecialHexDisplay(Type type, int round, String owner, String info) {
        this.type = type;
        this.info = info;
        this.round = round;
        this.owner = owner;
    }
    
    public SpecialHexDisplay(Type type, int round, String owner, String info, boolean obscured) {
        this.type = type;
        this.info = info;
        this.round = round;
        this.owner = owner;
        this.obscured = obscured; 
    }

    public boolean thisRound(int round) {
        if (NO_ROUND == this.round) {
            return true;
        }
        return round == this.round;
    }

    /** Does this SpecialHexDisplayObjet concern a round in the future? */
    public boolean futureRound(int round) {
        if(NO_ROUND == this.round) {
            return true;
        }
        return round > this.round;
    }
    
    /** Does this SpecialHexDisplayObjet concern a round in the past? */
    public boolean pastRound(int round) {
        if(NO_ROUND == this.round) {
            return true;
        }
        return round < this.round;
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

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public boolean isObscured() {
        return obscured;
    }

    public void setObscured(boolean obscured) {
        this.obscured = obscured;
    }

    /**
     * @param phase
     * @param round
     * @return
     */
    public boolean drawNow(IGame.Phase phase, int round) {
        boolean shouldDisplay = thisRound(round) || 
            (pastRound(round) && type.drawBefore()) ||
            (futureRound(round) && type.drawAfter());
        if(phase.isBefore(IGame.Phase.PHASE_OFFBOARD) && 
                (type == Type.ARTILLERY_TARGET || type == Type.ARTILLERY_HIT)
        ) {
            //hack to display atry targets the round after the hit.
            shouldDisplay = shouldDisplay || thisRound(round-1);
        }
        
        //System.err.println("turn: " + round + " Special type: " + type + " drawing: " + shouldDisplay + " details: " + info);
            
        return shouldDisplay;
    }

    /**
     * @param toPlayer
     * @return
     */
    public boolean isOwner(String toPlayer) {
        if(owner == null || owner.equals(toPlayer)) {
            return true;
        }
        
        return false;
    }
}
