/*
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
import java.util.Hashtable;
import java.util.Vector;


public class PtfFile implements MechLoader {
    int tonnage;
    int walkMP;
    int jumpMP;
    int headArmor;
    int torsoArmor;
    int mainGunArmor;
    int rArmArmor;
    int lArmArmor;
    int legArmor;
    String name;
    int weaponCount;
    String[] weapons;
    public PtfFile(InputStream is) throws EntityLoadingException {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            name=r.readLine();
            tonnage=Integer.parseInt(r.readLine());
            walkMP=Integer.parseInt(r.readLine());
            jumpMP=Integer.parseInt(r.readLine());
            headArmor=Integer.parseInt(r.readLine());
            torsoArmor=Integer.parseInt(r.readLine());
            rArmArmor=Integer.parseInt(r.readLine());
            lArmArmor=Integer.parseInt(r.readLine());
            legArmor=Integer.parseInt(r.readLine());
            weaponCount=Integer.parseInt(r.readLine());
            weapons=new String[weaponCount*2];
            for(int i = 0; i < (weaponCount*2-1); i+=2) {
                weapons[i]= r.readLine();
                weapons[i+1] = r.readLine();
            }

            } catch (IOException ex) {
            throw new EntityLoadingException("I/O Error reading file");
        } catch (StringIndexOutOfBoundsException ex) {
            throw new EntityLoadingException("StringIndexOutOfBoundsException reading file (format error)");
        } catch (NumberFormatException ex) {
            throw new EntityLoadingException("NumberFormatException reading file (format error)");
        }

    }



    public Entity getEntity() throws EntityLoadingException
    {
        Protomech protomech = new Protomech();
        protomech.setChassis(name);
        protomech.setOriginalWalkMP(walkMP);
        protomech.setOriginalJumpMP(jumpMP);
        protomech.autoSetInternal();
        protomech.setWeight(tonnage);
        protomech.setArmor(headArmor, Protomech.LOC_HEAD);
        protomech.setArmor(torsoArmor, Protomech.LOC_TORSO);
        protomech.setArmor(mainGunArmor, Protomech.LOC_MAINGUN);
        protomech.setArmor(lArmArmor, Protomech.LOC_LARM);
        protomech.setArmor(rArmArmor, Protomech.LOC_RARM);
        protomech.setArmor(legArmor, Protomech.LOC_LEG);
        //weapons...
        EquipmentType etype;
        String info;
        Mounted m;
        int shots;
        for(int i=0;i<(weaponCount*2-1);i+=2) {
              etype = EquipmentType.getByMtfName(weapons[i+1]);
              info= weapons[i];
              if(etype instanceof AmmoType) { //clumsy hack.  Come the revolution/weapon refac...
                  shots=Integer.parseInt(info);
                  try {
                  protomech.addEquipment(etype, Protomech.LOC_TORSO, false, shots);
                  } catch (LocationFullException ex) {
                throw new EntityLoadingException(ex.getMessage());
                  }
              }
              else {
                  try {
                  protomech.addEquipment(etype, protomech.getLocationFromAbbr(info), false);
                  } catch (LocationFullException ex) {
                throw new EntityLoadingException(ex.getMessage());
                  }
              }

        }


       return protomech;
    }

}
