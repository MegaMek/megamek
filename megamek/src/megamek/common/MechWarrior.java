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
package megamek.common;

/**
 * @author Sebastian Brocks
 * This class describes a MechWarrior that has ejected from its ride.
 */

public class MechWarrior extends Infantry {
    
    private int originalRideId;
    private int originalRideExternalId;
    private int pickedUpById = Entity.NONE;
    private int pickedUpByExternalId = Entity.NONE;

    public MechWarrior(Entity originalRide) {
        super();
        setCrew(originalRide.getCrew());
        setChassis("MechWarrior");
        setModel(originalRide.getCrew().getName());
        setWeight(1);
        setOwner(originalRide.getOwner());
        initializeInternal(1, Infantry.LOC_INFANTRY);
        setOriginalRideId(originalRide.getId());
        setOriginalRideExternalId(originalRide.getExternalId());
    }
    
    public boolean isSelectableThisTurn(Game game) {
        return (pickedUpById == Entity.NONE)
            && super.isSelectableThisTurn( game );
    }

    public int getOriginalRideId() {
        return originalRideId;
    }
    public void setOriginalRideId(int originalRideId) {
        this.originalRideId = originalRideId;
    }
    public int getOriginalRideExternalId() {
        return originalRideExternalId;
    }
    public void setOriginalRideExternalId(int originalRideExternalId) {
        this.originalRideExternalId = originalRideExternalId;
    }
    public int getPickedUpByExternalId() {
        return pickedUpByExternalId;
    }
    public void setPickedUpByExternalId(int pickedUpByExternalId) {
        this.pickedUpByExternalId = pickedUpByExternalId;
    }
    public int getPickedUpById() {
        return pickedUpById;
    }
    public void setPickedUpById(int pickedUpById) {
        this.pickedUpById = pickedUpById;
    }

    /**
     * Mek pilots have not inherent battle value.
     * <p/>
     * Overrides <code>Infantry#calculateBattleValue()</code>.
     */
    public int calculateBattleValue() {
        return 0;
    }

}
