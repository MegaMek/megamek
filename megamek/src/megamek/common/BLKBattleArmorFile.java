/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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
 * BLkFile.java
 *
 * Created on April 6, 2002, 2:06 AM
 */

/**
 * This class loads BattleArmor BLK files.
 *
 * @author  Suvarov454@sourceforge.net (James A. Damour )
 * @version $revision:$
 */
package megamek.common;

import java.io.*;

import megamek.common.util.*;

public class BLKBattleArmorFile implements MechLoader {

    BuildingBlock dataFile;
    // HACK!!!  BattleArmor movement reuses Mech and Vehicle movement.
    private static final String[] MOVES = { "", "Leg", "", "", "", "Jump" };

    /** Creates new BLkFile */
    public BLKBattleArmorFile(InputStream is) {

        dataFile = new BuildingBlock(is);

    }

    public BLKBattleArmorFile(BuildingBlock bb) {
        dataFile = bb;
    }

    //if it's a block file it should have this...
    public boolean isMine() {

        if (dataFile.exists("blockversion") ) return true;

        return false;

    }

    public Entity getEntity() throws EntityLoadingException {

        BattleArmor t = new BattleArmor();

        if (!dataFile.exists("name")) throw new EntityLoadingException("Could not find name block.");
        t.setChassis(dataFile.getDataAsString("Name")[0]);

        if (!dataFile.exists("model")) throw new EntityLoadingException("Could not find model block.");
            t.setModel(dataFile.getDataAsString("Model")[0]);

        if (!dataFile.exists("year")) throw new EntityLoadingException("Could not find year block.");
        t.setYear(dataFile.getDataAsInt("year")[0]);

        if (!dataFile.exists("type")) throw new EntityLoadingException("Could not find type block.");

        if (dataFile.getDataAsString("type")[0].equals("IS")) {
            if (t.getYear() == 3025) {
                t.setTechLevel(TechConstants.T_IS_LEVEL_1);
            } else {
                t.setTechLevel(TechConstants.T_IS_LEVEL_2);
            }
        } else {
            t.setTechLevel(TechConstants.T_CLAN_LEVEL_2);
        }

        if (!dataFile.exists("tonnage")) throw new EntityLoadingException("Could not find weight block.");
        t.weight = dataFile.getDataAsFloat("tonnage")[0];

        if (!dataFile.exists("BV")) throw new EntityLoadingException("Could not find BV block.");
        t.setBattleValue( dataFile.getDataAsInt("BV")[0] );

        if (!dataFile.exists("motion_type")) throw new EntityLoadingException("Could not find movement block.");
        String sMotion = dataFile.getDataAsString("motion_type")[0];
        int nMotion = -1;
        for (int x = 0; x < MOVES.length; x++) {
            if (sMotion.equals(MOVES[x])) {
                nMotion = x;
                break;
            }
        }
        if (nMotion == -1) throw new EntityLoadingException("Invalid movment type: " + sMotion);
        t.setMovementType(nMotion);

        if (!dataFile.exists("cruiseMP")) throw new EntityLoadingException("Could not find cruiseMP block.");
        t.setOriginalWalkMP(dataFile.getDataAsInt("cruiseMP")[0]);

        if (dataFile.exists("jumpingMP"))
            t.setOriginalJumpMP(dataFile.getDataAsInt("jumpingMP")[0]);

        if (!dataFile.exists("armor") ) throw new EntityLoadingException("Could not find armor block.");

        int[] armor = dataFile.getDataAsInt("armor");

        // Each trooper has the same amount of armor
        if (armor.length != 1) {
            throw new EntityLoadingException("Incorrect armor array length");
        }

        // add the body to the armor array
        for (int x = 1; x < t.locations(); x++) {
            t.initializeArmor(armor[0], x);
        }

        t.autoSetInternal();

        loadEquipment(t, "Squad", BattleArmor.LOC_SQUAD);
        String[] abbrs = t.getLocationAbbrs();
        for ( int loop = 1; loop < t.locations(); loop++ ) {
            loadEquipment( t, abbrs[loop], loop );
        }
        return t;
    }

    private void loadEquipment(BattleArmor t, String sName, int nLoc)
            throws EntityLoadingException
    {
        String[] saEquip = dataFile.getDataAsString(sName + " Equipment");
        if (saEquip == null) return;

        // prefix is "Clan " or "IS "
        String prefix;
        if (t.getTechLevel() == TechConstants.T_CLAN_LEVEL_2) {
            prefix = "Clan ";
        } else {
            prefix = "IS ";
        }

        for (int x = 0; x < saEquip.length; x++) {
            String equipName = saEquip[x].trim();
            EquipmentType etype = EquipmentType.getByMtfName(equipName);

            if (etype == null) {
                etype = EquipmentType.getByMepName(equipName);
            }

            if (etype == null) {
                // try w/ prefix
                etype = EquipmentType.getByMepName(prefix + equipName);
            }

            if (etype != null) {
                try {
                    t.addEquipment(etype, nLoc);
                } catch (LocationFullException ex) {
                    throw new EntityLoadingException(ex.getMessage());
                }
            }
            else if ( !equipName.equals("0") ) { System.err.println("Could not find " + equipName + " for " + t.getShortName() ); } //killme
        }
    }
}
