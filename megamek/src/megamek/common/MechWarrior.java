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

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.common.options.OptionsConstants;

/**
 * @author Sebastian Brocks This class describes a MechWarrior that has ejected
 *         from its ride.
 */

public class MechWarrior extends EjectedCrew {

    private static final long serialVersionUID = 6227549671448329770L;
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
        super(originalRide);
        setChassis(EjectedCrew.PILOT_EJECT_NAME);
    }
    
    public MechWarrior(Crew crew, Player owner, Game game) {
        super(crew, owner, game);
        setChassis(EjectedCrew.PILOT_EJECT_NAME);
    }

    /**
     * This constructor is so MULParser can load these entities
     */
    public MechWarrior() {
        super();
        setChassis(EjectedCrew.PILOT_EJECT_NAME);
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

    @Override
    public int doBattleValueCalculation(boolean ignoreC3, boolean ignoreSkill, CalculationReport calculationReport) {
        return 0;
    }

    /**
     * Ejected pilots do not get killed by ammo/fusion engine explosions
     * so that means they are still up in the air and do not land until the end of the turn.
     * @param landed
     */
    public void setLanded(boolean landed) {
        this.landed = landed;
    }

    public boolean hasLanded() {
        return landed;
    }

    @Override
    public boolean isCrippled() {
        return true; //Ejected mechwarriors should always attempt to flee according to Forced Withdrawal.
    }
    
    @Override
    //Aero pilots have parachutes and can thus survive being airborne
    public boolean doomedInAtmosphere() {
        return (game.getEntity(getOriginalRideId()) != null && !game.getEntity(getOriginalRideId()).isAero());
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_INFANTRY | Entity.ETYPE_MECHWARRIOR;
    }
    
    @Override
    public boolean canSpot() {
        return super.canSpot() && !game.getOptions().booleanOption(OptionsConstants.ADVANCED_PILOTS_CANNOT_SPOT);
    }
}
