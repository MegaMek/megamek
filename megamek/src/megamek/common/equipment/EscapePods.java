/*

 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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


package megamek.common.equipment;

import java.io.Serial;

import megamek.common.units.Aero;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.SmallCraft;

/**
 * This class describes a group of escape pods and/or lifeboats that has launched from a larger craft
 *
 * @author MKerensky
 */
public class EscapePods extends SmallCraft {
    @Serial
    private static final long serialVersionUID = 8128620143810186608L;

    protected int originalRideId;
    protected String originalRideExternalId;

    public static final String POD_EJECT_NAME = "Pods/Lifeboats from ";
    private static final int ESCAPE_POD_FUEL = 10;
    private static final int ESCAPE_POD_SAFE_THRUST = 4;

    /**
     * Used to set up a group of launched pods/boats for large spacecraft per rules in SO p27
     *
     * @param originalRide - the launching spacecraft
     * @param nPods        - the number of escape craft in this flight
     * @param isEscapePod  - flag to indicate if this is a flight of escape pods
     */
    public EscapePods(Aero originalRide, int nPods, boolean isEscapePod) {
        super();
        //We care about the passengers, not the crew of the escape craft
        setCrew(new Crew(CrewType.CREW));
        setChassis(POD_EJECT_NAME);
        setModel(originalRide.getDisplayName());

        // Generate the display name, then add the original ride's name.
        setDisplayName(POD_EJECT_NAME + originalRide.getDisplayName());

        //Pods and boats have an SI of 1 each
        setOSI(nPods);

        //Escape pods have fuel and thrusters to maneuver with
        if (isEscapePod) {
            setFuel(ESCAPE_POD_FUEL);
            setOriginalWalkMP(ESCAPE_POD_SAFE_THRUST);
        }
        setMovementMode(EntityMovementMode.AERODYNE);

        // and an armor value of 4 per craft -- 1 point per location
        for (int i = 0; i < 4; i++) {
            initializeArmor(nPods, i);
        }

        //Placeholder for adding individuals

        setOriginalRideId(originalRide.getId());
        setOriginalRideExternalId(originalRide.getExternalIdAsString());
    }

    /**
     * This constructor is so MULParser can load these entities
     */
    public EscapePods() {
        super();
        setCrew(new Crew(CrewType.CREW));
        setChassis(POD_EJECT_NAME);
        //this constructor is just so that the MUL parser can read these units in so
        // assign some arbitrarily large number here for the internal so that locations will get 
        //the actual current number of pods correct.
        setOSI(Integer.MAX_VALUE);
        for (int i = 0; i < locations(); i++) {
            initializeArmor(Integer.MAX_VALUE, i);
        }
    }

    /**
     * @return the <code>int</code> id of this unit's original ride
     */
    public int getOriginalRideId() {
        return originalRideId;
    }

    /**
     * set the <code>int</code> id of this unit's original ride
     */
    public void setOriginalRideId(int originalRideId) {
        this.originalRideId = originalRideId;
    }

    /**
     * @return the <code>int</code> external id of this unit's original ride
     */
    public int getOriginalRideExternalId() {
        return Integer.parseInt(originalRideExternalId);
    }

    public String getOriginalRideExternalIdAsString() {
        return originalRideExternalId;
    }

    /**
     * set the <code>int</code> external id of this unit's original ride
     */
    public void setOriginalRideExternalId(String originalRideExternalId) {
        this.originalRideExternalId = originalRideExternalId;
    }

    public void setOriginalRideExternalId(int originalRideExternalId) {
        this.originalRideExternalId = Integer.toString(originalRideExternalId);
    }

    @Override
    public boolean isCrippled(boolean checkCrew) {
        // Ejected crew should always attempt to flee according to Forced Withdrawal.
        return true;
    }
}
