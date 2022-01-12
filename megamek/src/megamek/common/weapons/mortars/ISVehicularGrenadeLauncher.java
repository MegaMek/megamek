/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons.mortars;

/**
 * @author Sebastian Brocks
 * @since Sep 24, 2004
 */
public class ISVehicularGrenadeLauncher extends VehicularGrenadeLauncherWeapon {
    private static final long serialVersionUID = 2888705202399185703L;

    public ISVehicularGrenadeLauncher() {
        super();
        name = "Vehicular Grenade Launcher";
        setInternalName("ISVehicularGrenadeLauncher");
        addLookupName("ISVehicularGrenadeLauncher");
    }
}
