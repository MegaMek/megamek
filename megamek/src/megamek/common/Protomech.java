/*
 * MegaMek - Copyright (C) 2003,2004 Ben Mazur (bmazur@sev.org)
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

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Enumeration;

import megamek.common.preference.PreferenceManager;

/**
 * Protomechs.  Level 2 Clan equipment.
 */
public class Protomech
    extends Entity
    implements Serializable
{
    public static final int      NUM_PMECH_LOCATIONS = 6;
    public static final String[] LOCATION_NAMES = {"Head",
    "Torso", "Right Arm", "Left Arm", "Legs", "Main Gun"};

    public static final String[] LOCATION_ABBRS = {"HD", "T", "RA", "LA", "L", "MG"};
    //weapon bools
    private boolean bHasMainGun;
    private boolean bHasRArmGun;
    private boolean bHasLArmGun;
    private boolean bHasTorsoAGun;
    private boolean bHasTorsoBGun;
    //weapon indices
    private int MainGunNum;
    private int RArmGunNum;
    private int LArmGunNum;
    private int TorsoAGunNum;
    private int TorsoBGunNum;
    // locations

    //Pilot damage caused so far by crits to this location. 
    // Needed for location destruction pilot damage.
    private int PilotDamageTaken[] = {0, 0, 0, 0, 0, 0};

    /**
     * The battle value of this unit.  This value should
     * be set when the unit's file is read.
     */
    private int         myBV = 0;

    /*
    ** Not every Protomech has a main gun.
    ** N.B. Regardless of the value set here, the variable is initialized to
    ** <code>false</code> until after the <code>Entity</code> is initialized,
    ** which is too late to allow main gun armor, hence the convoluted reverse
    ** logic.
    */
    private boolean     m_bHasNoMainGun = false;

    public static final int        LOC_HEAD            = 0;
    public static final int        LOC_TORSO           = 1;
    public static final int        LOC_RARM            = 2;
    public static final int        LOC_LARM            = 3;
    public static final int        LOC_LEG             = 4;
    public static final int        LOC_MAINGUN         = 5;
    //Near miss reprs.
    public static final int        LOC_NMISS           = 6;
    //"Systems".  These represent protomech critical hits; which remain constant regardless of proto.
    //doesn't matter what gets hit in a proto section, just the number of times it's been critted
    //so just have the right number of these systems and it works.
    public static final int             SYSTEM_ARMCRIT                  = 0;
    public static final int             SYSTEM_LEGCRIT                  = 1;
    public static final int             SYSTEM_HEADCRIT                 = 2;
    public static final int             SYSTEM_TORSOCRIT                = 3;
    public static final int             SYSTEM_TORSO_WEAPON_A           = 4;
    public static final int             SYSTEM_TORSO_WEAPON_B           = 5;
    private static final int[] NUM_OF_SLOTS = {2, 3, 2, 2, 3, 0};
    public static final int[] POSSIBLE_PILOT_DAMAGE = {1, 3, 1, 1, 1, 0};
    public static final String systemNames[] = {"Arm", "Leg", "Head", "Torso"};
    /**
     * Construct a new, blank, pmech.
     */
    public Protomech() {
        super();
        setCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_HEADCRIT));
        setCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_HEADCRIT));
        setCritical(LOC_RARM, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_ARMCRIT));
        setCritical(LOC_RARM, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_ARMCRIT));
        setCritical(LOC_LARM, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_ARMCRIT));
        setCritical(LOC_LARM, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_ARMCRIT));
        setCritical(LOC_TORSO, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_TORSOCRIT));
        setCritical(LOC_TORSO, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_TORSOCRIT));
        setCritical(LOC_TORSO, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_TORSOCRIT));
        setCritical(LOC_LEG, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LEGCRIT));
        setCritical(LOC_LEG, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LEGCRIT));
        setCritical(LOC_LEG, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LEGCRIT));
        bHasMainGun=false;
        bHasRArmGun=false;
        bHasLArmGun=false;
        bHasTorsoAGun=false;
        bHasTorsoBGun=false;



    }
    protected int[] getNoOfSlots() {
        return NUM_OF_SLOTS;
    }

    /**
     * Returns # of pilot damage points taken
     * due to crits to the location so far.
     */
    public int getPilotDamageTaken(int loc) {
        return PilotDamageTaken[loc];
    }

    /**
     * Get the weapon in the given torso location (if any).
     *
     * @param   isTorsoA - a <code>boolean</code> that is <code>true</code>
     *          if the weapon in "Torso A" is needed; <code>false</code> if
     *          the weapon in "Torso B" is needed.
     * @return  the <code>Mounted</code> weapon at the needed location.
     *          This value will be <code>null</code> if no weapon is in
     *          the indicated location.
     */
    public Mounted getTorsoWeapon( boolean isTorsoA ) {
        Mounted weapon = null;
        if ( isTorsoA && bHasTorsoAGun ) {
            weapon = getEquipment( TorsoAGunNum );
        }
        else if ( !isTorsoA && bHasTorsoBGun ) {
            weapon = getEquipment( TorsoBGunNum );
        }
        return weapon;
    }

    /**
     * Tells the Protomech to note  pilot damage
     * taken from crit damage to the location
     */
    public void setPilotDamageTaken(int loc, int damage) {
        PilotDamageTaken[loc]=damage;
    }

    /**
     * Protos don't take piloting skill rolls.
     */
    public PilotingRollData getBasePilotingRoll()
    {
        return new PilotingRollData( this.getId(),
                                     PilotingRollData.CHECK_FALSE,
                                     "Protomeks never take PSRs." );
    }

    /**
     *A "shaded" critical is a box shaded on the record sheet,
     * implies pilot damage when hit.  Returns whether shaded.
     */
    public boolean shaded(int loc, int numHit)
    {
        switch(loc){
        case LOC_HEAD:
        case LOC_LARM:
        case LOC_RARM:
            return (2 == numHit);
        case LOC_TORSO:
            return (0 < numHit);
        case LOC_MAINGUN:
        case LOC_NMISS:
            return false;
        case LOC_LEG:
            return (3 == numHit);
        }
        return false;
    }

    public int getWalkMP(boolean gravity) {
        int wmp=getOriginalWalkMP();
        int legCrits=this.getCritsHit(LOC_LEG);
        int i;
        int j;
        // Gravity, Protos can't get faster
        if (gravity) j = applyGravityEffectsOnMP(wmp);
        else j = wmp;
        if (j < wmp) wmp = j;
        switch(legCrits)
        {
            case 0:
            break;
            case 1:
            wmp--;
            break;
            case 2:
            wmp=wmp/2;
            break;
            case 3:
            wmp=0;
            break;
        }
        if (game != null) {
            i = game.getTemperatureDifference();
            return Math.max(wmp - i, 0);
        } else return wmp;
     }

     /**
     * Counts the # of crits taken by proto in the location.
     * Needed in several places, due to proto set criticals.
     */
    public int getCritsHit(int loc) {
        int count=0;
        for(int i=0;i<this.getNumberOfCriticals(loc);i++) {
            CriticalSlot ccs = getCritical(loc, i);
            if ( ccs.isDamaged() || ccs.isBreached() ) {
                count++;
            }
        }
        return count;
    }

    public static int getInnerLocation(int location)
    {
        return LOC_TORSO;
    }

     /**
     * Add in any piloting skill mods
     */
      public PilotingRollData addEntityBonuses(PilotingRollData roll) {
          return roll;
      }
  /**
   * Returns the number of total critical slots in a location
   */
    public int getNumberOfCriticals(int loc)
    {
        switch(loc) {
        case LOC_MAINGUN:
            return 0;
        case LOC_HEAD:
        case LOC_LARM:
        case LOC_RARM:
            return 2;
        case LOC_LEG:
        case LOC_TORSO:
            return 3;
        }
        return 0;
}
    /**
     * Override Entity#newRound() method.
     */
    public void newRound(int roundNumber) {


        setSecondaryFacing(getFacing());
        super.newRound(roundNumber);


    } // End public void newRound()

    /**
     * This pmech's jumping MP modified for missing jump jets and gravity.
     */
    public int getJumpMP() {
        int jump=this.jumpMP;
        int torsoCrits=this.getCritsHit(LOC_TORSO);
        switch(torsoCrits)
        {
            case 0:
            break;
            case 1:
            if (jump > 0) jump--;
            break;
            case 2:
            jump=jump/2;
            break;
        }
        if (applyGravityEffectsOnMP(jump) > jump) return jump;
        else return applyGravityEffectsOnMP(jump);
    }

    /**
     * Returns this mech's jumping MP, modified for missing & underwater jets.
     */
    public int getJumpMPWithTerrain() {
        if (getPosition() == null) {
            return getJumpMP();
        }

        int waterLevel = game.board.getHex(getPosition()).terrainLevel(Terrains.WATER);
        if (waterLevel <= 0) {
            return getJumpMP();
        } else  {
            return 0;
        }
    }
         public int getHeatCapacityWithWater() {
        return getHeatCapacity();
    }




    /**
     * Returns the about of heat that the entity can sink each
     * turn.   Pmechs have no heat.
     */
    public int getHeatCapacity() {


        return 999;
    }
   
    public String[] getLocationNames() {
        return LOCATION_NAMES;
    }
    /**
     * Returns the name of the type of movement used.
     * This is pmech-specific.
     */
    public String getMovementString(int mtype) {
        switch(mtype) {
        case MOVE_NONE :
            return "None";
        case MOVE_WALK :
            return "Walked";
        case MOVE_RUN :
            return "Ran";
        case MOVE_JUMP :
            return "Jumped";
        default :
            return "Unknown!";
        }
    }

    /**
     * Returns the name of the type of movement used.
     * This is pmech-specific.
     */
    public String getMovementAbbr(int mtype) {
        switch(mtype) {
        case MOVE_NONE :
            return "N";
        case MOVE_WALK :
            return "W";
        case MOVE_RUN :
            return "R";
        case MOVE_JUMP :
            return "J";
        default :
            return "?";
        }
    }

    public boolean canChangeSecondaryFacing() {
        return !(this.getCritsHit(LOC_LEG)>2);
    }
   public int getEngineCritHeat() {
        return 0;
    }
    /**
     * Can this pmech torso twist in the given direction?
     */
    public boolean isValidSecondaryFacing(int dir) {
        int rotate = dir - getFacing();
        if (canChangeSecondaryFacing()) {
            return rotate == 0 || rotate == 1 || rotate == -1 || rotate == -5;
        } else {
            return rotate == 0;
        }
    }

    /**
     * Return the nearest valid direction to torso twist in
     */
    public int clipSecondaryFacing(int dir) {
        if (isValidSecondaryFacing(dir)) {
            return dir;
        }
        // otherwise, twist once in the appropriate direction
        final int rotate = (dir + (6 - getFacing())) % 6;
        return rotate >= 3 ? (getFacing() + 5) % 6 : (getFacing() + 1) % 6;
    }

    public boolean hasRearArmor(int loc) {
        return false;
    }

     public int getRunMPwithoutMASC(boolean gravity) {
        return getRunMP(gravity);
    }


    /**
     * Returns the Compute.ARC that the weapon fires into.
     */
    public int getWeaponArc(int wn) {
        final Mounted mounted = getEquipment(wn);
        // rear mounted?
        if (mounted.isRearMounted()) {
            return Compute.ARC_REAR;
        }
        // front mounted
        switch(mounted.getLocation()) {
        case LOC_TORSO :
            return Compute.ARC_FORWARD;
        case LOC_RARM :
             return Compute.ARC_RIGHTARM;
        case LOC_LARM :
             return Compute.ARC_LEFTARM;
             case LOC_MAINGUN:
             return Compute.ARC_MAINGUN;
        default :
            return Compute.ARC_360;
        }
    }

    /**
     * Returns true if this weapon fires into the secondary facing arc.  If
     * false, assume it fires into the primary.
     */
    public boolean isSecondaryArcWeapon(int weaponId) {
             return true;
    }

    /**
     * Rolls up a hit location
     */
    public HitData rollHitLocation(int table, int side) {
        return rollHitLocation(table, side, LOC_NONE, IAimingModes.AIM_MODE_NONE);
    }

    public HitData rollHitLocation(int table, int side, int aimedLocation, int aimingMode) {
        int roll = -1;
        
        if ((aimedLocation != LOC_NONE) &&
                (aimingMode == IAimingModes.AIM_MODE_IMMOBILE)) {
            roll = Compute.d6(2);
            
            if ((5 < roll) && (roll < 9)) {
                return new HitData(aimedLocation, side == ToHitData.SIDE_REAR, true);
            }
            
        }
        
        roll = Compute.d6(2);
        try {
            PrintWriter pw = PreferenceManager.getClientPreferences().getMekHitLocLog();
            
            if ( pw != null ) {
                pw.print( table );
                pw.print( "\t" );
                pw.print( side );
                pw.print( "\t" );
                pw.println( roll );
            }
        } catch ( Throwable thrown ) {
            thrown.printStackTrace();
        }
        
        switch( roll ) {
        case 2:
            return new HitData(Protomech.LOC_MAINGUN);
        case 3:
        case 11:
            return new HitData(Protomech.LOC_NMISS);
        case 4:
            return new HitData(Protomech.LOC_RARM);
        case 5:
        case 9:
            return new HitData(Protomech.LOC_LEG);
        case 6:
        case 7:
        case 8:
            return new HitData(Protomech.LOC_TORSO);
        case 10:
            return new HitData(Protomech.LOC_RARM);
        case 12:
            return new HitData(Protomech.LOC_HEAD);
            
        }
        
        
        return null;
    }

 /**
  *Protos can't transfer crits.
  */

     public boolean canTransferCriticals(int loc) {
        return false;
     }
    /**
     * Gets the location that excess damage transfers to
     */
    public HitData getTransferLocation(HitData hit) {
        switch(hit.getLocation()) {
        case LOC_NMISS:
        return new HitData(LOC_NONE);
        case LOC_LARM :
        case LOC_LEG :
        case LOC_RARM :
        case LOC_HEAD :
        case LOC_MAINGUN:
            return new HitData(LOC_TORSO, hit.isRear());
        case LOC_TORSO :
        default:
            return new HitData(LOC_DESTROYED);
        }
    }

    /**
     * Gets the location that is destroyed recursively
     */
    public int getDependentLocation(int loc) {

            return LOC_NONE;
       }

    /**
     * Sets the internal structure for the pmech.
     *
     * @param head head
     * @param ct center torso
     * @param t right/left torso
     * @param arm right/left arm
     * @param leg right/left leg
     */
    public  void setInternal(int head, int torso, int arm, int legs, int mainGun ) {
          initializeInternal(head, LOC_HEAD);
        initializeInternal(torso, LOC_TORSO);
        initializeInternal(arm, LOC_RARM);
        initializeInternal(arm, LOC_LARM);
        initializeInternal(legs, LOC_LEG);
        initializeInternal(mainGun, LOC_MAINGUN);
    }

    /**
     * Set the internal structure to the appropriate value for the pmech's
     * weight class
     */
    public void autoSetInternal() {
        int mainGunIS = hasMainGun() ? 1: Entity.ARMOR_NA;
        switch ((int)weight) {
            //                     H, TSO,ARM,LEGS,MainGun
            case 2  : setInternal(1,2,1,2,mainGunIS); break;
            case 3  : setInternal(1,3,1,2,mainGunIS); break;
            case 4  : setInternal(1,4,1,3,mainGunIS); break;
            case 5  : setInternal(1,5,1,3,mainGunIS); break;
            case 6  : setInternal(2,6,2,4,mainGunIS); break;
            case 7  : setInternal(2,7,2,4,mainGunIS); break;
            case 8  : setInternal(2,8,2,5,mainGunIS); break;
            case 9  : setInternal(2,9,2,5,mainGunIS); break;
        }
    }
    /**
     * Creates a new mount for this equipment and adds it in.
     */
  public Mounted addEquipment(EquipmentType etype, int loc)
        throws LocationFullException
    {
        return addEquipment(etype, loc, false, -1);
    }


    public Mounted addEquipment(EquipmentType etype, int loc, boolean rearMounted)
        throws LocationFullException
    {
        Mounted mounted = new Mounted(this, etype);
        addEquipment(mounted, loc, rearMounted, -1);
        return mounted;
    }
    public Mounted addEquipment(EquipmentType etype, int loc, boolean rearMounted, int shots)
    throws LocationFullException
    {
        Mounted mounted = new Mounted(this, etype);
        addEquipment(mounted, loc, rearMounted, shots);
        return mounted;

    }

    /**
     * Mounts the specified weapon in the specified location.
     */
    protected void addEquipment(Mounted mounted, int loc, boolean rearMounted, int shots)
        throws LocationFullException
    {
        if(mounted.getType() instanceof AmmoType) {
            //Damn protomech ammo; nasty hack, should be cleaner
            if( -1 != shots ){
                mounted.setShotsLeft(shots);
                super.addEquipment(mounted,loc,rearMounted);
                return;
            }
        }

        if(mounted.getType() instanceof WeaponType) {
            switch(loc) {
                case LOC_HEAD:
                case LOC_LEG:
                case LOC_NMISS:
                throw new LocationFullException("Weapon " + mounted.getName() + " can't be mounted in " + getLocationAbbr(loc));
                case LOC_MAINGUN:
                if(bHasMainGun)
                {
                    throw new LocationFullException("Already has Main Gun");
                }
                else
                {
                    bHasMainGun=true;
                    mounted.setLocation(loc, rearMounted);
                    equipmentList.addElement(mounted);
                    weaponList.addElement(mounted);
                    MainGunNum=getEquipmentNum(mounted);
                }
                break;
                case LOC_LARM:
                if(bHasLArmGun)
                {
                    throw new LocationFullException("Already has LArm Gun");
                }
                else
                {
                    bHasLArmGun=true;
                    mounted.setLocation(loc, rearMounted);
                    equipmentList.addElement(mounted);
                    weaponList.addElement(mounted);
                    LArmGunNum=getEquipmentNum(mounted);
                }
                break;
                case LOC_RARM:
                if(bHasRArmGun)
                {
                    throw new LocationFullException("Already has RArm Gun");
                }
                else
                {
                    bHasRArmGun=true;
                    mounted.setLocation(loc, rearMounted);
                    equipmentList.addElement(mounted);
                    weaponList.addElement(mounted);
                    RArmGunNum=getEquipmentNum(mounted);
                }
                break;
                case LOC_TORSO:
                if(bHasTorsoAGun)
                {
                    if(bHasTorsoBGun)
                    {
                        throw new LocationFullException("Already has both torso guns");
                    }
                    else
                    {
                    bHasTorsoBGun=true;
                    mounted.setLocation(loc, rearMounted);
                    equipmentList.addElement(mounted);
                    weaponList.addElement(mounted);
                    TorsoBGunNum=getEquipmentNum(mounted);
                    }
                }
                else
                    {
                    bHasTorsoAGun=true;
                    mounted.setLocation(loc, rearMounted);
                    equipmentList.addElement(mounted);
                    weaponList.addElement(mounted);
                    TorsoAGunNum=getEquipmentNum(mounted);
                    }
                    break;
                }
            } else
            { super.addEquipment(mounted,loc,rearMounted);
        }
    }










     /**
     * Calculates the battle value of this pmech.  UNIMPLEMENTED and UNCOMPLETE.
     *
     */
    public int calculateBattleValue() {

        // Was our battle value set at construction?
        if ( myBV > 0 ) {
            // Adjust BV for crew skills.
            double pilotFactor = crew.getBVSkillMultiplier();
            return (int)(pilotFactor * (double)myBV);
        }

        double dbv = 0; // defensive battle value
        double obv = 0; // offensive bv

        // total armor points
        dbv += getTotalArmor();

        // total internal structure
        dbv += getTotalInternal() / 2;

        // add defensive equipment
        double dEquipmentBV = 0;
        for (Enumeration i = equipmentList.elements(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            EquipmentType etype = mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed())
                continue;

            if ((etype instanceof WeaponType && ((WeaponType)etype).getAmmoType() == AmmoType.T_AMS)
            || (etype instanceof AmmoType && ((AmmoType)etype).getAmmoType() == AmmoType.T_AMS)
            || etype.hasFlag(MiscType.F_ECM)) {
                dEquipmentBV += etype.getBV(this);
            }
        }
        dbv += dEquipmentBV;
        dbv +=weight;
        // adjust for target movement modifier
        int tmmRan = Compute.getTargetMovementModifier(getOriginalRunMP(), false).getValue();
        if (tmmRan > 5) {
            tmmRan = 5;
        }
        double[] tmmFactors = { 1.0, 1.1, 1.2, 1.3, 1.4, 1.5 };

        dbv *= (tmmFactors[tmmRan]+.1);

        double weaponBV = 0;

        // figure out base weapon bv
        double weaponsBVFront = 0;
        double weaponsBVRear = 0;
        for (Enumeration i = weaponList.elements(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            WeaponType wtype = (WeaponType)mounted.getType();
            double dBV = wtype.getBV(this);

            // don't count destroyed equipment
            if (mounted.isDestroyed())
                continue;

            // don't count AMS, it's defensive
            if (wtype.getAmmoType() == AmmoType.T_AMS) {
                continue;
            }


                weaponsBVFront += dBV;

        }
        if (weaponsBVFront > weaponsBVRear) {
            weaponBV += weaponsBVFront;
            weaponBV += (weaponsBVRear * 0.5);
        } else {
            weaponBV += weaponsBVRear;
            weaponBV += (weaponsBVFront * 0.5);
        }

        // add ammo bv
        double ammoBV = 0;
        for (Enumeration i = ammoList.elements(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            AmmoType atype = (AmmoType)mounted.getType();

            // don't count depleted ammo
            if (mounted.getShotsLeft() == 0)
                continue;

            // don't count AMS, it's defensive
            if (atype.getAmmoType() == AmmoType.T_AMS) {
                continue;
            }

            ammoBV += atype.getBV(this);
        }
        weaponBV += ammoBV;

        // adjust further for speed factor
        double speedFactor = getOriginalRunMP() - 5;
        speedFactor /= 10;
        speedFactor++;
        speedFactor = Math.pow(speedFactor, 1.2);
        speedFactor = Math.round(speedFactor * 100) / 100.0;

        obv = weaponBV * speedFactor;



        // and then factor in pilot
        double pilotFactor = crew.getBVSkillMultiplier();

        return (int)Math.round((dbv + obv) * pilotFactor);


    }

    /**
     * Returns an end-of-battle report for this pmech
     */
    public String victoryReport() {
        StringBuffer report = new StringBuffer();

        report.append(getDisplayName());
        report.append('\n');
        report.append("Pilot : " + crew.getDesc());
        report.append('\n');
        report.append("Kills: " + getKillNumber());
        report.append('\n');

        return report.toString();
    }


    public int getMaxElevationChange() {
        return 1;
    }

    public int getArmor(int loc, boolean rear)
    {
        if(loc==LOC_NMISS)
        {
            return ARMOR_NA;
        }
        else
        {
            return super.getArmor(loc, rear);
        }
    }
    public int getInternal(int loc)
    {
        if(loc==LOC_NMISS)
        {
            return ARMOR_NA;
        }
        else
        {
            return super.getInternal(loc);
        }
    }
    protected String[] getLocationAbbrs()
    {
        return LOCATION_ABBRS;
    }

  public String getLocationAbbr(int loc)
    {
        if(loc==LOC_NMISS) {
        return "a near miss";}
        else {
            return super.getLocationAbbr(loc);
        }
    }

    /**
     * Sets the battle value of this unit.  Please note that the BV
     * of all Battle Armor units is dictated by the BMRr, page 155.
     *
     * @param   bv - the <code>int</code> battle value of this unit.
     */
    public void setBattleValue( int bv ) { myBV = bv; }

    /*
    ** Not every Protomech has a main gun.
    */
    public boolean hasMainGun() 
    { 
        return !m_bHasNoMainGun; 
    }

    /*
    ** Not every Protomech has a main gun.
    */
    public void setHasMainGun(boolean b)
    {
        m_bHasNoMainGun = !b;
    }

    /**
     * Returns the number of locations in the entity
     */
    public int locations() {
        if ( m_bHasNoMainGun ) {
            return NUM_PMECH_LOCATIONS - 1;
        }
        return NUM_PMECH_LOCATIONS;
    }
        
    /**
     * Protomechs have no piloting skill (set to 5 for BV purposes)
     */
    public void setCrew(Pilot p) {
        super.setCrew(new Pilot(p.getName(), p.getGunnery(), 5));
    }

    public boolean canCharge() {
        // Protos can't Charge
        return false;
    };

    public boolean canDFA() {
        // Protos can't DFA
        return false;
    };
}
