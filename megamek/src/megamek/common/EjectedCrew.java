/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package megamek.common;

import java.util.HashMap;
import java.util.Map;

import megamek.common.options.OptionsConstants;
import megamek.common.weapons.infantry.InfantryWeapon;
import org.apache.logging.log4j.LogManager;

/** This class describes a vehicle crew that has abandoned its vehicle and now
 * functions as a rifle foot platoon of equal size.
 *
 * @author Klaus Mittag
 */
public class EjectedCrew extends Infantry {
    protected int originalRideId;
    protected String originalRideExternalId;
    // set up movement for Aero pilots and vessel crews
    protected int currentVelocity = 0;
    protected int nextVelocity = currentVelocity;
    
    // Maps "transported" crew, passengers to a host ship, so we can match them up again post-game
    private Map<String,Integer> nOtherCrew = new HashMap<>();
    private Map<String,Integer> passengers = new HashMap<>();
    
    private static final long serialVersionUID = 8136710237585797372L;
    
    public static final String VEE_EJECT_NAME = "Vehicle Crew";
    public static final String PILOT_EJECT_NAME = "Pilot";
    public static final String MW_EJECT_NAME = "MechWarrior";
    public static final String SPACE_EJECT_NAME = "Spacecraft Crew from ";
    public static final int EJ_CREW_MAX_MEN = 50; //See SO p27

    public EjectedCrew(Entity originalRide) {
        super();
        setCrew(originalRide.getCrew());
        LogManager.getLogger().info("Ejecting crew size: " + originalRide.getCrew().getSize());
        setChassis(VEE_EJECT_NAME);
        setModel(originalRide.getCrew().getName());
        //setWeight(1); // Copied from original MechWarrior code, but does this really do anything?

        // Generate the display name, then add the original ride's name.
        setDisplayName(getDisplayName() + " of " + originalRide.getDisplayName());

        // Finish initializing this unit.
        setOwner(originalRide.getOwner());
        initializeInternal(originalRide.getCrew().getSize(), Infantry.LOC_INFANTRY);
        if (originalRide.getCrew().getSlotCount() > 1) {
            int dead = 0;
            for (int i = 0; i < originalRide.getCrew().getSlotCount(); i++) {
                if (originalRide.getCrew().isDead(i)) {
                    dead++;
                }
            }
            setInternal(originalRide.getCrew().getSize() - dead, Infantry.LOC_INFANTRY);
        }
        setOriginalRideId(originalRide.getId());
        setOriginalRideExternalId(originalRide.getExternalIdAsString());
        Game tmpGame = originalRide.getGame();
        if (tmpGame != null
            && (!(this instanceof MechWarrior) 
                    || tmpGame.getOptions().booleanOption(OptionsConstants.ADVANCED_ARMED_MECHWARRIORS))) {
            try {
                addEquipment(EquipmentType.get(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE),
                        Infantry.LOC_INFANTRY);
                setPrimaryWeapon((InfantryWeapon) InfantryWeapon.get(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE));
            } catch (Exception ex) {
                LogManager.getLogger().error("", ex);
            }
        }
    }
    
    /**
     * Used to set up an ejected crew for large spacecraft per rules in SO p27
     * Multiple entities will be set up, each with a different strength
     * @param originalRide - the launching spacecraft
     * @param escapedThisRound - The number of people that got out this round
     */
    public EjectedCrew(Aero originalRide, int escapedThisRound) {
        super();
        setCrew(new Crew(CrewType.CREW));
        setChassis(SPACE_EJECT_NAME);
        setModel(originalRide.getDisplayName());

        // Generate the display name, then add the original ride's name.
        setDisplayName(getDisplayName() + " of " + originalRide.getDisplayName());
        
        initializeInternal(escapedThisRound, Infantry.LOC_INFANTRY);
        
        setOriginalRideId(originalRide.getId());
        setOriginalRideExternalId(originalRide.getExternalIdAsString());
    }
    
    /**
     * This constructor is so MULParser can load these entities
     */
    public EjectedCrew() {
        super();
        setCrew(new Crew(CrewType.CREW));
        setChassis(VEE_EJECT_NAME);
        //this constructor is just so that the MUL parser can read these units in so
        // assign some arbitrarily large number here for the internal so that locations will get 
        //the actual current number of trooper correct.
        initializeInternal(Integer.MAX_VALUE, Infantry.LOC_INFANTRY);
    }
    
    public EjectedCrew(Crew crew, Player owner, Game game) {
        super();
        setCrew(crew);
        setChassis(VEE_EJECT_NAME);
        setModel(crew.getName());

        // Finish initializing this unit.
        setOwner(owner);
        initializeInternal(crew.getSize(), Infantry.LOC_INFANTRY);
        if (crew.getSlotCount() > 1) {
            int dead = 0;
            for (int i = 0; i < crew.getSlotCount(); i++) {
                if (crew.isDead(i)) {
                    dead++;
                }
            }
            setInternal(crew.getSize() - dead, Infantry.LOC_INFANTRY);
        }
        Game tmpGame = game;
        if (tmpGame != null
            && (!(this instanceof MechWarrior) 
                    || tmpGame.getOptions().booleanOption(OptionsConstants.ADVANCED_ARMED_MECHWARRIORS))) {
            try {
                addEquipment(EquipmentType.get(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE),
                        Infantry.LOC_INFANTRY);
                setPrimaryWeapon((InfantryWeapon) InfantryWeapon.get(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE));
            } catch (Exception ex) {
                LogManager.getLogger().error("", ex);
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
    
    /**
     * Returns a mapping of how many crewmembers from other units this unit is carrying
     * and what ship they're from by external ID 
     */
    public Map<String,Integer> getNOtherCrew() {
        return nOtherCrew;
    }
    
    /**
     * Convenience method to return all crew from other craft aboard from the above Map
     * @return
     */
    public int getTotalOtherCrew() {
        int toReturn = 0;
        for (String name : getNOtherCrew().keySet()) {
            toReturn += getNOtherCrew().get(name);
        }
        return toReturn;
    }
    
    /**
     * Adds a number of crewmembers from another ship keyed by that ship's external ID
     * @param id The external ID of the ship these crew came from
     * @param n The number to add
     */
    public void addNOtherCrew(String id, int n) {
       if (nOtherCrew.containsKey(id)) {
           nOtherCrew.replace(id, nOtherCrew.get(id) + n);
       } else {
           nOtherCrew.put(id, n);
       }
    }
    
    /**
     * Returns a mapping of how many passengers from other units this unit is carrying
     * and what ship they're from by external ID 
     */
    public Map<String,Integer> getPassengers() {
        return passengers;
    }
    
    /**
     * Convenience method to return all passengers aboard from the above Map
     * @return
     */
    public int getTotalPassengers() {
        int toReturn = 0;
        for (String name : getPassengers().keySet()) {
            toReturn += getPassengers().get(name);
        }
        return toReturn;
    }
    
    /**
     * Adds a number of passengers from another ship keyed by that ship's external ID
     * @param id The external ID of the ship these passengers came from
     * @param n The number to add
     */
    public void addPassengers(String id, int n) {
       if (passengers.containsKey(id)) {
           passengers.replace(id, passengers.get(id) + n);
       } else {
           passengers.put(id, n);
       }
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
    
    // Handle pilot/escape pod velocity for Aeros
    
    public int getCurrentVelocity() {
        // if using advanced movement then I just want to sum up
        // the different vectors
        if ((game != null) && game.useVectorMove()) {
            return getVelocity();
        }
        return currentVelocity;
    }

    public void setCurrentVelocity(int velocity) {
        currentVelocity = velocity;
    }

    public int getNextVelocity() {
        return nextVelocity;
    }

    public void setNextVelocity(int velocity) {
        nextVelocity = velocity;
    }
    
    //Is this pilot/crew suited for vacuum/harsh environmental conditions?
    @Override
    public boolean doomedInSpace() {
        return !hasSpaceSuit();
    }

}
