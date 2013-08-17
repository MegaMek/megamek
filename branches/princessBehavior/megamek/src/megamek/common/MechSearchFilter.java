/*
 * MegaMek - Copyright (C) 2002, 2003 Ben Mazur (bmazur@sev.org)
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

import megamek.common.loaders.EntityLoadingException;


/**
 *
 * @author JSmyrloglou
 */
public class MechSearchFilter {

    public String sWalk;
    public int iWalk;
    public String sJump;
    public int iJump;
    public int iArmor;
    public String sWep1Count;
    public Object oWep1;
    public String sWep2Count;
    public Object oWep2;
    public int iWepAndOr;
    public String sStartYear;
    public String sEndYear;
    public boolean bCheckEquipment;
    public Object oEquipment;

    public static boolean isTechMatch(MechSummary mech, int nTechType) {
        return ((nTechType == TechConstants.T_ALL)
                || (nTechType == mech.getType())
                || ((nTechType == TechConstants.T_IS_TW_ALL)
                && ((mech.getType() <= TechConstants.T_IS_TW_NON_BOX)
                || (mech.getType() == TechConstants.T_INTRO_BOXSET)))
                || ((nTechType == TechConstants.T_TW_ALL)
                && ((mech.getType() <= TechConstants.T_IS_TW_NON_BOX)
                || (mech.getType() <= TechConstants.T_INTRO_BOXSET)
                || (mech.getType() <= TechConstants.T_CLAN_TW)))
                || ((nTechType == TechConstants.T_ALL_IS)
                && ((mech.getType() <= TechConstants.T_IS_TW_NON_BOX)
                || (mech.getType() == TechConstants.T_INTRO_BOXSET)
                || (mech.getType() == TechConstants.T_IS_ADVANCED)
                || (mech.getType() == TechConstants.T_IS_EXPERIMENTAL)
                || (mech.getType() == TechConstants.T_IS_UNOFFICIAL)))
                || ((nTechType == TechConstants.T_ALL_CLAN)
                && ((mech.getType() == TechConstants.T_CLAN_TW)
                || (mech.getType() == TechConstants.T_CLAN_ADVANCED)
                || (mech.getType() == TechConstants.T_CLAN_EXPERIMENTAL)
                || (mech.getType() == TechConstants.T_CLAN_UNOFFICIAL))));

    }

    public static boolean isMatch(MechSummary mech, MechSearchFilter f) {
        if (f == null) {
            return true;
        }
        try {
            Entity entity = new MechFileParser(mech.getSourceFile(), mech.getEntryName()).getEntity();

            int walk = -1;
            try {
                walk = Integer.parseInt(f.sWalk);
            } catch (NumberFormatException ne) {
                //ignore
            }
            if (walk > -1) {
                if (f.iWalk == 0) { // at least
                    if (entity.getWalkMP() < walk) {
                        return false;
                    }
                } else if (f.iWalk == 1) { // equal to
                    if (walk != entity.getWalkMP()) {
                        return false;
                    }
                } else if (f.iWalk == 2) { // not more than
                    if (entity.getWalkMP() > walk) {
                        return false;
                    }
                }
            }

            int jump = -1;
            try {
                jump = Integer.parseInt(f.sJump);
            } catch (NumberFormatException ne) {
                //ignore
            }
            if (jump > -1) {
                if (f.iJump == 0) { // at least
                    if (entity.getJumpMP() < jump) {
                        return false;
                    }
                } else if (f.iJump == 1) { // equal to
                    if (jump != entity.getJumpMP()) {
                        return false;
                    }
                } else if (f.iJump == 2) { // not more than
                    if (entity.getJumpMP() > jump) {
                        return false;
                    }
                }
            }

            int sel = f.iArmor;
            if (sel > 0) {
                int armor = entity.getTotalArmor();
                int maxArmor = entity.getTotalInternal() * 2 + 3;
                if (sel == 1) {
                    if (armor < (maxArmor * .25)) {
                        return false;
                    }
                } else if (sel == 2) {
                    if (armor < (maxArmor * .5)) {
                        return false;
                    }
                } else if (sel == 3) {
                    if (armor < (maxArmor * .75)) {
                        return false;
                    }
                } else if (sel == 4) {
                    if (armor < (maxArmor * .9)) {
                        return false;
                    }
                }
            }

            boolean weaponLine1Active = false;
            boolean weaponLine2Active = false;
            boolean foundWeapon1 = false;
            boolean foundWeapon2 = false;

            int count = 0;
            int weapon1 = -1;
            try {
                weapon1 = Integer.parseInt(f.sWep1Count);
            } catch (NumberFormatException ne) {
                //ignore
            }
            if (weapon1 > -1) {
                weaponLine1Active = true;
                for (int i = 0; i < entity.getWeaponList().size(); i++) {
                    WeaponType wt = (WeaponType) (entity.getWeaponList().get(i)).getType();
                    if (wt.getName().equals(f.oWep1)) {
                        count++;
                    }
                }
                if (count >= weapon1) {
                    foundWeapon1 = true;
                }
            }

            count = 0;
            int weapon2 = -1;
            try {
                weapon2 = Integer.parseInt(f.sWep2Count);
            } catch (NumberFormatException ne) {
                //ignore
            }
            if (weapon2 > -1) {
                weaponLine2Active = true;
                for (int i = 0; i < entity.getWeaponList().size(); i++) {
                    WeaponType wt = (WeaponType) (entity.getWeaponList().get(i)).getType();
                    if (wt.getName().equals(f.oWep2)) {
                        count++;
                    }
                }
                if (count >= weapon2) {
                    foundWeapon2 = true;
                }
            }

            int startYear = Integer.MIN_VALUE;
            int endYear = Integer.MAX_VALUE;
            try {
                startYear = Integer.parseInt(f.sStartYear);
            } catch (NumberFormatException ne) {
                //ignore
            }
            try {
                endYear = Integer.parseInt(f.sEndYear);
            } catch (NumberFormatException ne) {
                //ignore
            }
            if ((entity.getYear() < startYear) || (entity.getYear() > endYear)) {
                return false;
            }

            if (weaponLine1Active && !weaponLine2Active && !foundWeapon1) {
                return false;
            }
            if (weaponLine2Active && !weaponLine1Active && !foundWeapon2) {
                return false;
            }
            if (weaponLine1Active && weaponLine2Active) {
                if (f.iWepAndOr == 0 /* 0 is "or" choice */) {
                    if (!foundWeapon1 && !foundWeapon2) {
                        return false;
                    }
                } else { // "and" choice in effect
                    if (!foundWeapon1 || !foundWeapon2) {
                        return false;
                    }
                }
            }

            count = 0;
            if (f.bCheckEquipment) {
                for (Mounted m : entity.getEquipment()) {
                    EquipmentType mt = m.getType();
                    if (mt.getName().equals(f.oEquipment)) {
                        count++;
                    }
                }
                if (count < 1) {
                    return false;
                }
            }
        } catch (EntityLoadingException ex) {
            //shouldn't happen
            return false;
        }

        return true;
    }
}
