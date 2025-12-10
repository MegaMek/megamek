/*
  Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2004-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.units;

import java.io.Serial;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.Player;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;

/**
 * @author Sebastian Brocks This class describes a MekWarrior that has ejected from its ride.
 */

public class MekWarrior extends EjectedCrew {

    @Serial
    private static final long serialVersionUID = 6227549671448329770L;
    private int pickedUpById = Entity.NONE;
    private String pickedUpByExternalId = "-1";
    private boolean landed = true;

    /**
     * Create a new MekWarrior
     *
     * @param originalRide the <code>Entity</code> that was this MW's original ride
     */
    public MekWarrior(Entity originalRide) {
        super(originalRide);
        setChassis(EjectedCrew.PILOT_EJECT_NAME);
    }

    public MekWarrior(Crew crew, Player owner, Game game) {
        super(crew, owner, game);
        setChassis(EjectedCrew.PILOT_EJECT_NAME);
    }

    /**
     * This constructor is so MULParser can load these entities
     */
    public MekWarrior() {
        super();
        setChassis(EjectedCrew.PILOT_EJECT_NAME);
    }

    @Override
    public boolean isSelectableThisTurn() {
        return (pickedUpById == Entity.NONE) && super.isSelectableThisTurn();
    }


    /**
     * @return the <code>int</code> external id of the unit that picked up this MW
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
     * Ejected pilots do not get killed by ammo/fusion engine explosions so that means they are still up in the air and
     * do not land until the end of the turn.
     *
     */
    public void setLanded(boolean landed) {
        this.landed = landed;
    }

    public boolean hasLanded() {
        return landed;
    }

    @Override
    public boolean isCrippled() {
        //Ejected MekWarriors should always attempt to flee according to Forced Withdrawal.
        return true;
    }

    @Override
    public boolean doomedInAtmosphere() {
        if (game == null) {
            return true;
        } else {
            // Aero pilots have parachutes and can thus survive being airborne
            Entity originalRide = game.getEntityFromAllSources(getOriginalRideId());
            return originalRide instanceof Aero || originalRide instanceof LandAirMek;
        }
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_INFANTRY | Entity.ETYPE_MEKWARRIOR;
    }

    @Override
    public boolean canSpot() {
        return super.canSpot() && !gameOptions().booleanOption(OptionsConstants.ADVANCED_PILOTS_CANNOT_SPOT);
    }
}
