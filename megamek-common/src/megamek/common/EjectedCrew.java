/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package megamek.common;

import megamek.common.weapons.infantry.InfantryWeapon;

/** This class describes a vehicle crew that has abandoned its vehicle and now
 * functions as a rifle foot platoon of equal size.
 *
 * @author Klaus Mittag
 */
public class EjectedCrew extends Infantry {
    protected int originalRideId;
    protected String originalRideExternalId;
    
    private static final long serialVersionUID = 8136710237585797372L;
    
    public static final String VEE_EJECT_NAME = "Vehicle Crew";
    public static final String MW_EJECT_NAME = "MechWarrior";

    public EjectedCrew(Entity originalRide) {
        super();
        setCrew(originalRide.getCrew());
        System.out.println("Ejecting crew size: " + originalRide.getCrew().getSize());
        setChassis(VEE_EJECT_NAME);
        setModel(originalRide.getCrew().getName());
        //setWeight(1); // Copied from original MechWarrior code, but does this really do anything?

        // Generate the display name, then add the original ride's name.
        StringBuffer newName = new StringBuffer(getDisplayName());
        newName.append(" of ").append(originalRide.getDisplayName());
        displayName = newName.toString();

        // Finish initializing this unit.
        setOwner(originalRide.getOwner());
        initializeInternal(originalRide.getCrew().getSize(), Infantry.LOC_INFANTRY);
        setOriginalRideId(originalRide.getId());
        setOriginalRideExternalId(originalRide.getExternalIdAsString());
        IGame tmpGame = originalRide.getGame();
        if (tmpGame != null
            && (!(this instanceof MechWarrior) 
                    || tmpGame.getOptions().booleanOption("armed_mechwarriors"))) {
            try {
                addEquipment(EquipmentType.get("InfantryAssaultRifle"),
                        Infantry.LOC_INFANTRY);
                setPrimaryWeapon((InfantryWeapon) InfantryWeapon.get("InfantryAssaultRifle"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    /**
     * This constructor is so MULParser can load these entities
     */
    public EjectedCrew() {
        super();
        setCrew(new Crew(1));
        setChassis(VEE_EJECT_NAME);
        //this constructor is just so that the MUL parser can read these units in so
        //assign some arbitrarily large number here for the internal so that locations will get 
        //the actual current number of trooper correct.
        initializeInternal(Integer.MAX_VALUE, Infantry.LOC_INFANTRY);
    }
    
    public EjectedCrew(Crew crew, IPlayer owner, IGame game) {
        super();
        setCrew(crew);
        setChassis(VEE_EJECT_NAME);
        setModel(crew.getName());
        //setWeight(1);

        // Generate the display name, then add the original ride's name.
        StringBuffer newName = new StringBuffer(getDisplayName());
        displayName = newName.toString();

        // Finish initializing this unit.
        setOwner(owner);
        initializeInternal(crew.getSize(), Infantry.LOC_INFANTRY);
        IGame tmpGame = game;
        if (tmpGame != null
            && (!(this instanceof MechWarrior) 
                    || tmpGame.getOptions().booleanOption("armed_mechwarriors"))) {
            try {
                addEquipment(EquipmentType.get("InfantryAssaultRifle"),
                        Infantry.LOC_INFANTRY);
                setPrimaryWeapon((InfantryWeapon) InfantryWeapon.get("InfantryAssaultRifle"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * @return the <code>int</code> id of this MW's original ride
     */
    public int getOriginalRideId() {
        return originalRideId;
    }

    /**
     * set the <code>int</code> id of this MW's original ride
     */
    public void setOriginalRideId(int originalRideId) {
        this.originalRideId = originalRideId;
    }

    /**
     * @return the <code>int</code> external id of this MW's original ride
     */
    public int getOriginalRideExternalId() {
        return Integer.parseInt(originalRideExternalId);
    }

    public String getOriginalRideExternalIdAsString() {
        return originalRideExternalId;
    }

    /**
     * set the <code>int</code> external id of this MW's original ride
     */
    public void setOriginalRideExternalId(String originalRideExternalId) {
        this.originalRideExternalId = originalRideExternalId;
    }

    public void setOriginalRideExternalId(int originalRideExternalId) {
        this.originalRideExternalId = Integer.toString(originalRideExternalId);
    }

    /*@Override
     * Taharqa: I don't think this should be here and I can't find a place where it is 
     * actually necessary. If you set this crew as unejected it will carry on to the original unit
     * and the after battle MULs and processing will be wrong
    public void newRound(int number) {
        super.newRound(number);
        getCrew().setEjected(false);
    }*/

    /**
     * Because they deploy in their vehicles rather than as infantry, crews
     * (including MechWarriors) never count as squads.
     * 
     * @return <code>false</code>
     */
    @Override
    public boolean isSquad() {
        return false;
    }
    
    @Override
    public boolean isCrippled() {
        // Ejected crew should always attempt to flee according to Forced Withdrawal.
        return true;
    }

}
