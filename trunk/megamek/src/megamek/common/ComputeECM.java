/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;


/**
 * Similar to the Compute class, this class contains various static methods for
 * common computations related to ECM.
 * 
 * @author arlith
 *
 */
public class ComputeECM {

    /**
     * This method checks to see if a line from a to b is affected by an ECM
     * field of the enemy of ae
     *
     * @param ae
     * @param a
     * @param b
     * @return
     */
    public static boolean isAffectedByECM(Entity ae, Coords a, Coords b) {
        ECMInfo ecmInfo = getECMEffects(ae, a, b, null);
        // ECM present if any positive Angel ECM, or positive ECM without 
        //  Angel ECCM
        return (ecmInfo.strength > 0 && ecmInfo.angelStrength == 0) 
                || (ecmInfo.angelStrength > 0);
    }

    /**
     * This method checks to see if a line from a to b is affected by an ECCM
     * field of the enemy of ae
     *
     * @param ae
     * @param a
     * @param b
     * @return
     */
    public static boolean isAffectedByECCM(Entity ae, Coords a, Coords b) {
        ECMInfo ecmInfo = getECMEffects(ae, a, b, null);
        // Any negative ECM strength without Angel ECM, or any negative Angel 
        //  ECCM
        return (ecmInfo.strength < 0 && ecmInfo.angelStrength == 0) 
                || (ecmInfo.angelStrength < 0);
    }
    
    /**
     * This method checks to see if a line from a to b is affected by an Angel
     * ECM field of the enemy of ae
     *
     * @param ae
     * @param a
     * @param b
     * @return count that shows if you are in an friendly ECCM field positive
     * number means you are in an friendly ECCM field Negative number
     * means you are in a enemy ECM field 0 means you are not effect by
     * enemy or friendly fields.
     */
    public static boolean isAffectedByAngelECM(Entity ae, Coords a, Coords b) {
        ECMInfo ecmInfo = getECMEffects(ae, a, b, null);
        return ecmInfo.angelStrength > 0;
    }    

    /**
     * Check for the total number of fighter/small craft ECM bubbles in space
     * along the path from a to b
     */
    public static int getSmallCraftECM(Entity ae, Coords a, Coords b) {
        if (!ae.getGame().getBoard().inSpace()) {
            // only matters in space
            return 0;
        }
        // Only grab enemies with active ECM
        Vector<Coords> vEnemyECMCoords = new Vector<Coords>(16);
        Vector<Integer> vEnemyECMRanges = new Vector<Integer>(16);
        Vector<Coords> vFriendlyECCMCoords = new Vector<Coords>(16);
        Vector<Integer> vFriendlyECCMRanges = new Vector<Integer>(16);
        Vector<Coords> vFriendlyBAPCoords = new Vector<Coords>(16);
        Vector<Integer> vFriendlyBAPRanges = new Vector<Integer>(16);
        Vector<Integer> vFriendlyBAPFacings = new Vector<Integer>(16);
        for (Enumeration<Entity> e = ae.game.getEntities(); e.hasMoreElements(); ) {
            Entity ent = e.nextElement();
            Coords entPos = ent.getPosition();
            if ((entPos == null) && (ent.getTransportId() != Entity.NONE)) {
                Entity carrier = ae.game.getEntity(ent.getTransportId());
                if ((null != carrier) && carrier.loadedUnitsHaveActiveECM()) {
                    entPos = carrier.getPosition();
                }
            }
            if (ent.isEnemyOf(ae) && ent.hasActiveECM() && (entPos != null)
                && !ent.isLargeCraft()) {
                vEnemyECMCoords.addElement(entPos);
                vEnemyECMRanges.addElement(new Integer(ent.getECMRange()));
            }
            if (!ent.isEnemyOf(ae) && ent.hasActiveECCM() && (entPos != null)
                && !ent.isLargeCraft()) {
                vFriendlyECCMCoords.addElement(entPos);
                vFriendlyECCMRanges.addElement(new Integer(ent.getECMRange()));
            }
            if (!ent.isEnemyOf(ae) && ent.hasBAP(false) && (entPos != null)) {
                vFriendlyBAPCoords.addElement(entPos);
                vFriendlyBAPRanges.addElement(new Integer(ent.getBAPRange()));
                vFriendlyBAPFacings.addElement(new Integer(ent.getFacing()));
            }
    
            // TODO: do docked dropships give ECM benefit?
        }
    
        // none? get out of here
        if (vEnemyECMCoords.size() == 0) {
            return 0;
        }
    
        // get intervening Coords.
        ArrayList<Coords> coords = Coords.intervening(a, b);
        // loop through all intervening coords, check each if they are ECM
        // affected
        int totalECM = 0;
        // check for split hexes
        boolean bDivided = ((a.degree(b) % 60) == 30);
        int x = 0;
        int prevEcmStatus = 0;
        boolean prevEccmPresent = false;
        for (Coords c : coords) {
            int ecmStatus = 0;
            boolean eccmPresent = false;
            // first, subtract 1 for each enemy ECM that affects us
            Enumeration<Integer> ranges = vEnemyECMRanges.elements();
            for (Coords enemyECMCoords : vEnemyECMCoords) {
                int range = ranges.nextElement().intValue();
                int nDist = c.distance(enemyECMCoords);
                if (nDist <= range) {
                    ecmStatus++;
                }
            }
            // now check for friendly eccm
            ranges = vFriendlyECCMRanges.elements();
            for (Coords friendlyECCMCoords : vFriendlyECCMCoords) {
                int range = ranges.nextElement().intValue();
                int nDist = c.distance(friendlyECCMCoords);
                if (nDist <= range) {
                    eccmPresent = true;
                    break;
                }
            }
            // if eccm still not present, check for BAP
            if (!eccmPresent) {
                ranges = vFriendlyBAPRanges.elements();
                Enumeration<Integer> facings = vFriendlyBAPFacings.elements();
                for (Coords friendlyBAPCoords : vFriendlyBAPCoords) {
                    int range = ranges.nextElement().intValue();
                    int nDist = c.distance(friendlyBAPCoords);
                    int facing = facings.nextElement().intValue();
                    if (nDist <= range) {
                        // still might need to check for right arc if using
                        // medium range
                        if ((range < 7)
                            || Compute.isInArc(friendlyBAPCoords, facing,
                                               c, Compute.ARC_NOSE)) {
                            eccmPresent = true;
                            break;
                        }
                    }
                }
            }
            // if any coords in the line are affected, the whole line is
            if (!bDivided || ((x % 3) == 0)) {
                if ((ecmStatus > 0) && !eccmPresent) {
                    totalECM++;
                }
            } else if (((x % 3) == 2)) {
                // if we are looking at the second split hex then both this one
                // and the prior need to have ECM
                // becaue the advantage should go to the defender
                if ((ecmStatus > 0) && !eccmPresent && (prevEcmStatus > 0)
                    && !prevEccmPresent) {
                    totalECM++;
                }
            }
            x++;
            prevEccmPresent = eccmPresent;
            prevEcmStatus = ecmStatus;
    
        }
        return totalECM;
    }

    /**
     * Check for the total number of fighter/small craft ECM bubbles in space
     * along the path from a to b
     */
    public static int getLargeCraftECM(Entity ae, Coords a, Coords b) {
        if (!ae.getGame().getBoard().inSpace()) {
            // only matters in space
            return 0;
        }
        // Only grab enemies with active ECM
        Vector<Coords> vEnemyECMCoords = new Vector<Coords>(16);
        Vector<Integer> vEnemyECMRanges = new Vector<Integer>(16);
        Vector<Coords> vFriendlyECCMCoords = new Vector<Coords>(16);
        Vector<Integer> vFriendlyECCMRanges = new Vector<Integer>(16);
        Vector<Coords> vFriendlyBAPCoords = new Vector<Coords>(16);
        Vector<Integer> vFriendlyBAPRanges = new Vector<Integer>(16);
        Vector<Integer> vFriendlyBAPFacings = new Vector<Integer>(16);
        for (Enumeration<Entity> e = ae.game.getEntities(); e.hasMoreElements(); ) {
            Entity ent = e.nextElement();
            Coords entPos = ent.getPosition();
            if ((entPos == null) && (ent.getTransportId() != Entity.NONE)) {
                Entity carrier = ae.game.getEntity(ent.getTransportId());
                if ((null != carrier) && carrier.loadedUnitsHaveActiveECM()) {
                    entPos = carrier.getPosition();
                }
            }
            if (ent.isEnemyOf(ae) && ent.hasActiveECM() && (entPos != null)
                && ent.isLargeCraft()) {
                vEnemyECMCoords.addElement(entPos);
                vEnemyECMRanges.addElement(new Integer(ent.getECMRange()));
            }
            if (!ent.isEnemyOf(ae) && ent.hasActiveECCM() && (entPos != null)
                && !ent.isLargeCraft()) {
                vFriendlyECCMCoords.addElement(entPos);
                vFriendlyECCMRanges.addElement(new Integer(ent.getECMRange()));
            }
            if (!ent.isEnemyOf(ae) && ent.hasBAP(false) && (entPos != null)) {
                vFriendlyBAPCoords.addElement(entPos);
                vFriendlyBAPRanges.addElement(new Integer(ent.getBAPRange()));
                vFriendlyBAPFacings.addElement(new Integer(ent.getFacing()));
    
            }
            // TODO: do docked dropships give ECM benefit?
        }
    
        // none? get out of here
        if (vEnemyECMCoords.size() == 0) {
            return 0;
        }
    
        // get intervening Coords.
        ArrayList<Coords> coords = Coords.intervening(a, b);
        // loop through all intervening coords, check each if they are ECM
        // affected
        int totalECM = 0;
        boolean bDivided = ((a.degree(b) % 60) == 30);
        int x = 0;
        int prevEcmStatus = 0;
        for (Coords c : coords) {
            // > 0: in friendly ECCM
            // 0: unaffected by enemy ECM
            // <0: affected by enemy ECM
            int ecmStatus = 0;
            // first, subtract 1 for each enemy ECM that affects us
            Enumeration<Integer> ranges = vEnemyECMRanges.elements();
            for (Coords enemyECMCoords : vEnemyECMCoords) {
                int range = ranges.nextElement().intValue();
                int nDist = c.distance(enemyECMCoords);
                if (nDist <= range) {
                    ecmStatus++;
                }
            }
            // now check for friendly small craft eccm
            ranges = vFriendlyECCMRanges.elements();
            for (Coords friendlyECCMCoords : vFriendlyECCMCoords) {
                int range = ranges.nextElement().intValue();
                int nDist = c.distance(friendlyECCMCoords);
                if (nDist <= range) {
                    ecmStatus--;
                }
            }
            // now check BAP
            ranges = vFriendlyBAPRanges.elements();
            Enumeration<Integer> facings = vFriendlyBAPFacings.elements();
            for (Coords friendlyBAPCoords : vFriendlyBAPCoords) {
                int range = ranges.nextElement().intValue();
                int nDist = c.distance(friendlyBAPCoords);
                int facing = facings.nextElement().intValue();
                if (nDist <= range) {
                    // still might need to check for right arc if using medium
                    // range
                    if ((range < 7)
                        || Compute.isInArc(friendlyBAPCoords, facing, c,
                                           Compute.ARC_NOSE)) {
                        ecmStatus = ecmStatus - 2;
                    }
                }
            }
            // if any coords in the line are affected, the whole line is
            if (!bDivided || ((x % 3) == 0)) {
                if (ecmStatus > 0) {
                    totalECM++;
                }
            } else if ((x % 3) == 2) {
                // if we are looking at the second split hex then both this one
                // and the prior need to have ECM
                // becaue the advantage should go to the defender
                if ((ecmStatus > 0) && (prevEcmStatus > 0)) {
                    totalECM++;
                }
            }
            x++;
            prevEcmStatus = ecmStatus;
        }
        return totalECM;
    }
    
    /**
     * Go through each entity in the supplied list and calculate the information
     * for any ECM and ECCM it has and return the collection of ECMInfos.
     * 
     * @param entities  The list of entities to compute information for
     * @return          An ECMInfo entry for each active ECM and ECCM fielded.
     */
    public static List<ECMInfo> computeAllEntitiesECMInfo(
            Vector<Entity> entities) {
        
        LinkedList<ECMInfo> allEcmInfo = new LinkedList<ECMInfo>();
        
        for (Entity e : entities) {
            ECMInfo ecmInfo = e.getECMInfo();
            if (ecmInfo != null) {
                allEcmInfo.add(ecmInfo);
            }
            ECMInfo eccmInfo = e.getECCMInfo();
            if (eccmInfo != null) {
                allEcmInfo.add(eccmInfo);
            }
        }
        
        return allEcmInfo;
    }
    
    /**
     * Returns the total ECM effects on the supplied unit.
     *
     * @param ae
     * @param a
     * @param b
     * @return
     */
    public static ECMInfo getECMEffects(Entity ae, Coords a, Coords b, 
            List<ECMInfo> allEcmInfo) {
        ECMInfo affectedInfo = null;
        
        if (allEcmInfo == null) {
            allEcmInfo = computeAllEntitiesECMInfo(ae.game.getEntitiesVector());
        }
        
        // Get intervening Coords
        ArrayList<Coords> coords = Coords.intervening(a, b);
        // Loop through intervening coords, add effects of any in-range E(C)CM        
        for (Coords c : coords) {
            if (c.equals(ae.getPosition()) && ae.isINarcedWith(INarcPod.ECM)) {
                if (affectedInfo == null) {
                    affectedInfo = new ECMInfo(0, 1, ae);
                } else {
                    affectedInfo.strength++;
                }
            }
            for (ECMInfo ecmInfo : allEcmInfo) {
                // Is the ECMInfo in range of this position?
                int dist = c.distance(ecmInfo.pos);
                if (dist < ecmInfo.range) {
                    if (affectedInfo == null) {
                        affectedInfo = new ECMInfo(0, 0, ae);
                    }
                    affectedInfo.addECMEffects(ecmInfo);
                }
            }
                   
        }       
        return affectedInfo;
    }
    

}
