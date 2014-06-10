/*
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

package megamek.client;

import java.io.Serializable;

import megamek.client.ui.Messages;
import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.ConvFighter;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.SmallCraft;
import megamek.common.SupportTank;
import megamek.common.Tank;

/**
 * Author: Jay Lawson
 * This class will contain all the information to generate random skills for a pilot
 * There will be several different options for the generation technique, which
 * can be set in the RandomSkillsDialog.java
 *
 * By default, this will be set to constant and regular and can therefore be used to
 * assign skills to new units as well
 */
public class RandomSkillsGenerator implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -6993878542250768464L;

    //Method
    public static final int M_TW       = 0;
    public static final int M_TAHARQA  = 1;
    public static final int M_CONSTANT = 2;
    private static String[] methodNames = { "MethodTW", "MethodTaharqa", "MethodConstant"};
    public static final int M_SIZE = methodNames.length;

    //Level
    public static final int L_GREEN = 0;
    public static final int L_REG   = 1;
    public static final int L_VET   = 2;
    public static final int L_ELITE = 3;
    private static String[] levelNames = { "Green", "Regular", "Veteran", "Elite"};
    public static final int L_SIZE = levelNames.length;

    //Type
    public static final int T_IS   = 0;
    public static final int T_CLAN = 1;
    public static final int T_MD   = 2;
    private static String[] typeNames = { "InnerSphere", "Clan", "ManeiDomini"};
    public static final int T_SIZE = typeNames.length;

    private static final int[][] skillLevels = new int[][] { { 7, 6, 5, 4, 4, 3, 2, 1, 0 },
        { 7, 7, 6, 6, 5, 4, 3, 2, 1 } };

    //current settings
    private int method;
    private int level;
    private int type;
    //boolean to foce piloting to be one above gunnery
    private boolean close;

    public static String getMethodDisplayableName(int method) {
        if ((method >= 0) && (method < M_SIZE)) {
            return Messages.getString("RandomSkillDialog." + methodNames[method]);
        }
        throw new IllegalArgumentException("Unknown method");
    }

    public static String getLevelDisplayableName(int level) {
        if ((level >= 0) && (level < L_SIZE)) {
            return Messages.getString("RandomSkillDialog." + levelNames[level]);
        }
        throw new IllegalArgumentException("Unknown level");
    }

    public static String getTypeDisplayableName(int type) {
        if ((type >= 0) && (type < T_SIZE)) {
            return Messages.getString("RandomSkillDialog." + typeNames[type]);
        }
        throw new IllegalArgumentException("Unknown type");
    }

    public RandomSkillsGenerator() {

        method = M_CONSTANT;
        level = L_REG;
        type = T_IS;
        close = false;

    }

    public int getMethod() {
        return method;
    }

    public void setMethod(int m) {
        method = m;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int l) {
        level = l;
    }

    public int getType() {
        return type;
    }

    public void setType(int t) {
        type = t;
    }

    public boolean isClose() {
        return close;
    }

    public void setClose(boolean b) {
        close = b;
    }

    /**
     * Generates random skills for an entity based on the current settings of the random skills generator,
     * but does not assign those new skills to that entity
     * @param e - an Entity
     * @return an integer array of (gunnery, piloting) skill values
     */
    public int[] getRandomSkills(Entity e) {
        return getRandomSkills(e, false);
    }

    /**
     * Generates random skills for an entity based on the current settings of the random skills generator,
     * but does not assign those new skills to that entity
     * @param e - an Entity
     * @param forceClan - a boolean that forces the type to be clan if the entity is a clan unit
     * @return an integer array of (gunnery, piloting) skill values
     */
    public int[] getRandomSkills(Entity e, boolean forceClan) {

        //dont use level and type directly because they might change
        int lvl = level;
        int ty = type;

        if(forceClan && e.isClan()) {
            ty = T_CLAN;
        }

        int[] skills = { 4, 5 };

        // constant is the easy one
        if (method == M_CONSTANT) {
            if (lvl == L_GREEN) {
                skills[0] = 5;
                skills[1] = 6;
            }
            if (lvl == L_VET) {
                skills[0] = 3;
                skills[1] = 4;
            }
            if (lvl == L_ELITE) {
                skills[0] = 2;
                skills[1] = 3;
            }
            //Now we need to make all kinds of adjustments based on the table on pg. 40 of TW

           //infantry anti-mech skill should be one higher unless foot
            if(((e instanceof Infantry) && !(e instanceof BattleArmor))
                    & (e.getMovementMode() != EntityMovementMode.INF_LEG)) {
                skills[1]++;
            }

            //gunnery is worse for support vees
            if(e instanceof SupportTank) {
                skills[0]++;
            }

            //now lets handle clanners
            if(ty == T_CLAN) {
                //mechs and battle armor are better (but not protos)
                if((e instanceof Mech) || (e instanceof BattleArmor)) {
                    skills[0]--;
                    skills[1]--;
                }
                //vees are worse
                if(e instanceof Tank) {
                    skills[0]++;
                    skills[1]++;
                }
                //gunnery is worse for infantry, conv fighters and small craft
                if(((e instanceof Infantry) && !(e instanceof BattleArmor))
                        || (e instanceof ConvFighter) || (e instanceof SmallCraft)) {
                    skills[0]++;
                }
            }

            if (ty == T_MD) {
                //according to JHS72 pg. 121, they are always considered elite
                skills[0]=2;
                skills[1]=3;
            }

            return skills;
        }

        // if using Taharqa's method, then the base skill level for each entity
        // is determined separately
        if (method == M_TAHARQA) {
            int lbonus = 0;
            if (lvl == L_GREEN) {
                lbonus -= 2;
            }
            if (lvl == L_VET) {
                lbonus += 2;
            }
            if (lvl == L_ELITE) {
                lbonus += 4;
            }

            int lvlroll = Compute.d6(2) + lbonus;

            // restate level based on roll
            if (lvlroll < 6) {
                lvl = L_GREEN;
            } else if (lvlroll < 10) {
                lvl = L_REG;
            } else if (lvlroll < 12) {
                lvl = L_VET;
            } else {
                lvl = L_ELITE;
            }
        }

        // first get the bonus
        int bonus = 0;
        if (ty == T_CLAN) {
            if ((e instanceof Mech) || (e instanceof BattleArmor)) {
                bonus+=2;
            } else if ((e instanceof Tank) || (e instanceof Infantry)) {
                bonus-=2;
            }
        }
        if (ty == T_MD) {
            bonus++;
        }

        int gunroll = Compute.d6(1) + bonus;
        int pilotroll = Compute.d6(1) + bonus;

        int glevel = 0;
        int plevel = 0;

        switch (lvl) {
            case L_REG:
                glevel = (int) Math.ceil(gunroll / 2.0) + 2;
                plevel = (int) Math.ceil(pilotroll / 2.0) + 2;
                break;
            case L_VET:
                glevel = (int) Math.ceil(gunroll / 2.0) + 3;
                plevel = (int) Math.ceil(pilotroll / 2.0) + 3;
                break;
            case L_ELITE:
                glevel = (int) Math.ceil(gunroll / 2.0) + 4;
                plevel = (int) Math.ceil(pilotroll / 2.0) + 4;
                break;
            default:
                glevel = (int) Math.ceil((gunroll + 0.5) / 2.0);
                plevel = (int) Math.ceil((pilotroll + 0.5) / 2.0);
                if (gunroll <= 0) {
                    glevel = 0;
                }
                if (pilotroll <= 0) {
                    plevel = 0;
                }
        }

        skills[0] = skillLevels[0][glevel];
        skills[1] = skillLevels[1][plevel];

        if(close) {
            skills[1] = skills[0] + 1;
        }

        return skills;
    }
}
