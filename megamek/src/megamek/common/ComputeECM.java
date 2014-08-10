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
        return ComputeECM.getECMFieldSize(ae, a, b) > 0;
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
        return ComputeECM.getECCMFieldSize(ae, a, b) > 0;
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
        return ComputeECM.getAngelECMFieldSize(ae, a, b) > 0;
    }    

    /**
     * This method returns the highest number of enemy ECM fields of ae between
     * points a and b
     *
     * @param ae
     * @param a
     * @param b
     * @return
     */
    public static double getECMFieldSize(Entity ae, Coords a, Coords b) {
    
        if (ae.getGame().getBoard().inSpace()) {
            // normal ECM effects don't apply in space
            return 0;
        }
        if ((a == null) || (b == null)) {
            return 0;
        }
    
        LinkedList<ECMInfo> enemyECMInfo = new LinkedList<ECMInfo>();
        LinkedList<ECMInfo> friendlyECCMInfo = new LinkedList<ECMInfo>();
    
        for (Entity ent : ae.game.getEntitiesVector()) {
            Coords entPos = ent.getPosition();
            if (ent.isEnemyOf(ae) && (entPos != null) && ent.hasActiveECM()) {
                ECMInfo newInfo = new ECMInfo(ent.getECMRange(), entPos,
                                              ent.getECMStrength());
                enemyECMInfo.add(newInfo);
            }
            if (!ent.isEnemyOf(ae) && (entPos != null) && ent.hasActiveECCM()) {
                ECMInfo newInfo = new ECMInfo(ent.getECMRange(), entPos,
                                              ent.getECMStrength());
                friendlyECCMInfo.add(newInfo);
            }
    
            // Check the ECM effects of the entity's passengers.
            for (Entity other : ent.getLoadedUnits()) {
                if (other.isEnemyOf(ae) && other.hasActiveECM()
                    && (entPos != null)) {
                    ECMInfo newInfo = new ECMInfo(ent.getECMRange(), entPos,
                                                  ent.getECMStrength());
                    enemyECMInfo.add(newInfo);
                }
                if (!other.isEnemyOf(ae) && other.hasActiveECCM()
                    && (entPos != null)) {
                    ECMInfo newInfo = new ECMInfo(ent.getECMRange(), entPos,
                                                  ent.getECMStrength());
                    friendlyECCMInfo.add(newInfo);
                }
            }
        }
    
        // none? get out of here
        if ((enemyECMInfo.size() == 0) && !ae.isINarcedWith(INarcPod.ECM)) {
            return 0;
        }
    
        // get intervening Coords.
        ArrayList<Coords> coords = Coords.intervening(a, b);
        // loop through all intervening coords, check each if they are ECM
        // affected
        double worstECM = 0;
        for (Coords c : coords) {
            // > 0: affected by enemy ECM
            // 0: unaffected by enemy ECM
            // <0: in friendly ECCM
            double ecmStatus = 0;
            // if we're at ae's Position, figure in a possible
            // iNarc ECM pod
            if (c.equals(ae.getPosition()) && ae.isINarcedWith(INarcPod.ECM)) {
                ecmStatus++;
            }
            // First, subtract strength for each enemy ECM that affects us
            for (ECMInfo ecmInfo : enemyECMInfo) {
                int nDist = c.distance(ecmInfo.pos);
                if (nDist <= ecmInfo.range) {
                    ecmStatus += ecmInfo.strength;
                }
            }
            // now, add strength for each friendly ECCM
            for (ECMInfo ecmInfo : friendlyECCMInfo) {
                int nDist = c.distance(ecmInfo.pos);
                if (nDist <= ecmInfo.range) {
                    ecmStatus -= ecmInfo.strength;
                }
            }
            // if any coords in the line are affected, the whole line is
            if (ecmStatus > worstECM) {
                worstECM = ecmStatus;
            }
        }
        return worstECM;
    }

    /**
     * This method returns the highest number of enemy ECCM fields of ae between
     * points a and b
     *
     * @param ae
     * @param a
     * @param b
     * @return
     */
    public static double getECCMFieldSize(Entity ae, Coords a, Coords b) {
        if (ae.getGame().getBoard().inSpace()) {
            // normal ECM effects don't apply in space
            return 0;
        }
        if ((a == null) || (b == null)) {
            return 0;
        }
    
        // Only grab enemies with active ECM
        LinkedList<ECMInfo> enemyECMInfo = new LinkedList<ECMInfo>();
        LinkedList<ECMInfo> friendlyECCMInfo = new LinkedList<ECMInfo>();
        for (Enumeration<Entity> e = ae.game.getEntities(); e.hasMoreElements(); ) {
            Entity ent = e.nextElement();
            Coords entPos = ent.getPosition();
            if (ent.isEnemyOf(ae) && ent.hasActiveECCM() && (entPos != null)) {
                ECMInfo newInfo = new ECMInfo(ent.getECMRange(), entPos,
                                              ent.getECMStrength());
                enemyECMInfo.add(newInfo);
            }
            if (!ent.isEnemyOf(ae) && ent.hasActiveECM() && (entPos != null)) {
                ECMInfo newInfo = new ECMInfo(ent.getECMRange(), entPos,
                                              ent.getECMStrength());
                friendlyECCMInfo.add(newInfo);
            }
    
            // Check the ECM effects of the entity's passengers.
            for (Entity other : ent.getLoadedUnits()) {
                if (other.isEnemyOf(ae) && other.hasActiveECCM()
                    && (entPos != null)) {
                    ECMInfo newInfo = new ECMInfo(ent.getECMRange(), entPos,
                                                  ent.getECMStrength());
                    enemyECMInfo.add(newInfo);
                }
                if (!other.isEnemyOf(ae) && other.hasActiveECM()
                    && (entPos != null)) {
                    ECMInfo newInfo = new ECMInfo(ent.getECMRange(), entPos,
                                                  ent.getECMStrength());
                    friendlyECCMInfo.add(newInfo);
                }
            }
        }
    
        // none? get out of here
        if (enemyECMInfo.size() == 0) {
            return 0;
        }
    
        // get intervening Coords.
        ArrayList<Coords> coords = Coords.intervening(a, b);
        // loop through all intervening coords, check each if they are ECCM
        // affected
        double worstECCM = 0;
        for (Coords c : coords) {
            // > 0: in friendly ECM
            // 0: unaffected by enemy ECCM
            // <0: affected by enemy ECCM
            double eccmStatus = 0;
            // if we're at ae's Position, figure in a possible
            // iNarc ECM pod
            if (c.equals(ae.getPosition()) && ae.isINarcedWith(INarcPod.ECM)) {
                eccmStatus--;
            }
    
            // First, subtract strength for each enemy ECM that affects us
            for (ECMInfo ecmInfo : enemyECMInfo) {
                int nDist = c.distance(ecmInfo.pos);
                if (nDist <= ecmInfo.range) {
                    eccmStatus += ecmInfo.strength;
                }
            }
            // now, add strength for each friendly ECCM
            for (ECMInfo ecmInfo : friendlyECCMInfo) {
                int nDist = c.distance(ecmInfo.pos);
                if (nDist <= ecmInfo.range) {
                    eccmStatus -= ecmInfo.strength;
                }
            }
            // if any coords in the line are affected, the whole line is
            if (eccmStatus > worstECCM) {
                worstECCM = eccmStatus;
            }
        }
        return worstECCM;
    }



    public static double getAngelECMFieldSize(Entity ae, Coords a, Coords b) {
        if (ae.getGame().getBoard().inSpace()) {
            // normal Angel ECM effects don't apply in space
            return 0;
        }
        if ((a == null) || (b == null)) {
            return 0;
        }
    
        // we have to track regular ECM here as well because some ECCM might
        // get "soaked up" by the regular ECM. I have a rules query in that
        // asks for confirmation on whether regular ECM should always be soaked
        // before Angel
        // http://bg.battletech.com/forums/index.php/topic,27121.new.html#new
    
        LinkedList<ECMInfo> enemyOtherECMInfo = new LinkedList<ECMInfo>();
        LinkedList<ECMInfo> enemyAngelECMInfo = new LinkedList<ECMInfo>();
        LinkedList<ECMInfo> friendlyECCMInfo = new LinkedList<ECMInfo>();
    
        for (Entity ent : ae.game.getEntitiesVector()) {
            Coords entPos = ent.getPosition();
            // add each angel ECM at its ECM strength
            if (ent.isEnemyOf(ae) && (entPos != null)) {
                if (ent.hasActiveAngelECM()) {
                    ECMInfo newInfo = new ECMInfo(ent.getECMRange(), entPos,
                                                  ent.getECMStrength());
                    enemyAngelECMInfo.add(newInfo);
                } else if (ent.hasActiveECM()) {
                    ECMInfo newInfo = new ECMInfo(ent.getECMRange(), entPos,
                                                  ent.getECMStrength());
                    enemyOtherECMInfo.add(newInfo);
                }
            }
            if (!ent.isEnemyOf(ae) && ent.hasActiveECCM() && (entPos != null)) {
                ECMInfo newInfo = new ECMInfo(ent.getECMRange(), entPos,
                                              ent.getECMStrength());
                friendlyECCMInfo.add(newInfo);
            }
    
            // Check the angel ECM effects of the entity's passengers.
            for (Entity other : ent.getLoadedUnits()) {
                if (other.isEnemyOf(ae) && (entPos != null)) {
                    if (other.hasActiveAngelECM()) {
                        ECMInfo newInfo = new ECMInfo(ent.getECMRange(), entPos,
                                                      ent.getECMStrength());
                        enemyAngelECMInfo.add(newInfo);
                    } else if (other.hasActiveECM()) {
                        ECMInfo newInfo = new ECMInfo(ent.getECMRange(), entPos,
                                                      ent.getECMStrength());
                        enemyOtherECMInfo.add(newInfo);
                    }
                }
                if (!other.isEnemyOf(ae) && other.hasActiveECCM()
                    && (entPos != null)) {
                    ECMInfo newInfo = new ECMInfo(ent.getECMRange(), entPos,
                                                  ent.getECMStrength());
                    friendlyECCMInfo.add(newInfo);
                }
            }
        }
    
        // none? get out of here
        if (enemyAngelECMInfo.size() == 0) {
            return 0;
        }
    
        // get intervening Coords.
        ArrayList<Coords> coords = Coords.intervening(a, b);
        // loop through all intervening coords, check each if they are ECM
        // affected
        double worstECM = 0;
        for (Coords c : coords) {
            // > 0: affected by enemy Angel ECM
            // 0: unaffected by enemy Angel ECM
            // <0: in friendly ECCM
            double ecmStatus = 0;
            // fist calculate other ECM
            if (c.equals(ae.getPosition()) && ae.isINarcedWith(INarcPod.ECM)) {
                ecmStatus++;
            }
    
            for (ECMInfo ecmInfo : enemyOtherECMInfo) {
                int nDist = c.distance(ecmInfo.pos);
                if (nDist <= ecmInfo.range) {
                    ecmStatus += ecmInfo.strength;
                }
            }
    
            // now, add friendly ECCM
            for (ECMInfo ecmInfo : friendlyECCMInfo) {
                int nDist = c.distance(ecmInfo.pos);
                if (nDist <= ecmInfo.range) {
                    ecmStatus -= ecmInfo.strength;
                }
            }
            // if ecmStatus is greater than zero then we have more other ECM
            // than ECCM,
            // but we don't care about that so reset to zero
            if (ecmStatus > 0) {
                ecmStatus = 0;
            }
            // now get the angel ECM
            for (ECMInfo ecmInfo : enemyAngelECMInfo) {
                int nDist = c.distance(ecmInfo.pos);
                if (nDist <= ecmInfo.range) {
                    ecmStatus += ecmInfo.strength;
                }
            }
    
            // if any coords in the line are affected, the whole line is
            if (ecmStatus > worstECM) {
                worstECM = ecmStatus;
            }
        }
        return worstECM;
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
        
        ArrayList<ECMInfo> allEcmInfo = new ArrayList<ECMInfo>(entities.size());
        
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
