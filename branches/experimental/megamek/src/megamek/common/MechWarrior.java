/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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

/**
 * @author Sebastian Brocks This class describes a MechWarrior that has ejected
 *         from its ride.
 */

public class MechWarrior extends Infantry {

    private static final long serialVersionUID = 6227549671448329770L;
    private int originalRideId;
    private String originalRideExternalId;
    private int pickedUpById = Entity.NONE;
    private String pickedUpByExternalId = "-1";
    private boolean landed = true;

    /**
     * Create a new MechWarrior
     *
     * @param originalRide the <code>Entity</code> that was this MW's original
     *            ride
     */
    public MechWarrior(Entity originalRide) {
        super();
        setCrew(originalRide.getCrew());
        setChassis("MechWarrior");
        setModel(originalRide.getCrew().getName());
        setWeight(1);

        // Generate the display name, then add the original ride's name.
        StringBuffer newName = new StringBuffer(getDisplayName());
        newName.append(" of ").append(originalRide.getDisplayName());
        displayName = newName.toString();

        // Finish initializing this unit.
        setOwner(originalRide.getOwner());
        initializeInternal(1, Infantry.LOC_INFANTRY);
        setOriginalRideId(originalRide.getId());
        setOriginalRideExternalId(originalRide.getExternalIdAsString());
        IGame tmpGame = originalRide.getGame();
        if (tmpGame != null
                && tmpGame.getOptions().booleanOption("armed_mechwarriors")) {
            try {
                addEquipment(EquipmentType.get("InfantryAssaultRifle"),
                        Infantry.LOC_INFANTRY);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public MechWarrior(Crew crew, IPlayer owner, IGame game) {
        super();
        setCrew(crew);
        setChassis("MechWarrior");
        setModel(crew.getName());
        setWeight(1);

        // Generate the display name, then add the original ride's name.
        StringBuffer newName = new StringBuffer(getDisplayName());
        displayName = newName.toString();

        // Finish initializing this unit.
        setOwner(owner);
        initializeInternal(1, Infantry.LOC_INFANTRY);
        IGame tmpGame = game;
        if (tmpGame != null
                && tmpGame.getOptions().booleanOption("armed_mechwarriors")) {
            try {
                addEquipment(EquipmentType.get("InfantryAssaultRifle"),
                        Infantry.LOC_INFANTRY);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#isSelectableThisTurn()
     */
    @Override
    public boolean isSelectableThisTurn() {
        return (pickedUpById == Entity.NONE) && super.isSelectableThisTurn();
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
     * @return the <code>int</code> external id of the unit that picked up
     *         this MW
     */
    public int getPickedUpByExternalId() {
        return Integer.parseInt(pickedUpByExternalId);
    }
    
    public String getPickedUpByExternalIdAsString() {
        return pickedUpByExternalId;
    }

    /**
     * set the <code>int</code> external id of the unit that picked up this MW
     */
    public void setPickedUpByExternalId(String pickedUpByExternalId) {
        this.pickedUpByExternalId = pickedUpByExternalId;
    }
    
    public void setPickedUpByExternalId(int pickedUpByExternalId) {
        this.pickedUpByExternalId = Integer.toString(pickedUpByExternalId);
    }

    /**
     * @return the <code>int</code> id of the unit that picked up this MW
     */
    public int getPickedUpById() {
        return pickedUpById;
    }

    /**
     * set the <code>int</code> id of the unit that picked up this MW
     */
    public void setPickedUpById(int pickedUpById) {
        this.pickedUpById = pickedUpById;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Infantry#calculateBattleValue()
     */
    @Override
    public int calculateBattleValue() {
        return 0;
    }

    @Override
    public void newRound(int number) {
        super.newRound(number);
        getCrew().setEjected(false);
    }

    /**
     * Ejected pilots do not get killed by ammo/fusion engine explosions
     * so that means they are still up in the air and do not land until the end of the turn.
     * @param landed
     */
    public void setLanded(boolean landed){
        this.landed = landed;
    }

    public boolean hasLanded(){
        return landed;
    }

    @Override
    public boolean isCrippled() {
        return true; //Ejected mchwarriors should always attempt to flee according to Forced Withdrawal.
    }
    
    public long getEntityType(){
        return Entity.ETYPE_INFANTRY | Entity.ETYPE_MECHWARRIOR;
    }
}
