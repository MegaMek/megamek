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
    
    public EjectedCrew(Entity originalRide) {
        super();
        setCrew(originalRide.getCrew());
        System.out.println("Ejecting crew size: " + originalRide.getCrew().getSize());
        setChassis("Vehicle Crew");
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
    
    public EjectedCrew(Crew crew, IPlayer owner, IGame game) {
        super();
        setCrew(crew);
        setChassis("Vehicle Crew");
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

    @Override
    public void newRound(int number) {
        super.newRound(number);
        getCrew().setEjected(false);
    }

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
    
}
