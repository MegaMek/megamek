/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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

package megamek.common.options;


import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Dropship;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.GunEmplacement;
import megamek.common.Jumpship;
import megamek.common.Mech;
import megamek.common.Protomech;
import megamek.common.Tank;

/**
 * Contains the options for partial repair properties
 *
 * @author Neth (Thomas Pfau)
 */
public class PartialRepairs extends AbstractOptions {
    private static final long serialVersionUID =  7618380522964885742L;
    public static final String PART_REPAIRS  = "PartRepairs"; 
//    public static final String NEG_QUIRKS = "NegQuirks"; //$NON-NLS-1$

    public PartialRepairs() {
        super();
    }

    @Override
    public void initialize() {
        //positive quirks
        IBasicOptionGroup partRep = addGroup("part_repairs", PART_REPAIRS); //$NON-NLS-1$
        addOption(partRep, "mech_reactor_3_crit", false); //$NON-NLS-1$
        addOption(partRep, "mech_reactor_2_crit", false); //$NON-NLS-1$
        addOption(partRep, "mech_reactor_1_crit", false); //$NON-NLS-1$
        addOption(partRep, "mech_gyro_1_crit", false); //$NON-NLS-1$
        addOption(partRep, "mech_gyro_2_crit", false); //$NON-NLS-1$
        addOption(partRep, "sensors_1_crit", false); //$NON-NLS-1$
        addOption(partRep, "mech_sensors_2_crit", false); //$NON-NLS-1$
        addOption(partRep, "veh_stabilizer_crit", false); //$NON-NLS-1$
        addOption(partRep, "veh_locked_turret", false); //$NON-NLS-1$
        addOption(partRep, "mech_engine_replace", false); //$NON-NLS-1$
        addOption(partRep, "mech_gyro_replace", false); //$NON-NLS-1$
        }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.options.AbstractOptions#getOptionsInfoImp()
     */
    @Override
    protected AbstractOptionsInfo getOptionsInfoImp() {
        return PartialRepairInfo.getInstance();
    }

    public static boolean isPartRepLegalFor(IOption quirk, Entity en) {

        if(en instanceof Mech) {
            if(quirk.getName().equals("veh_stabilizer_crit")
                    || quirk.getName().equals("veh_locked_turret")
                    ) {
                return false;
            }
            return true;
        }

        if(en instanceof GunEmplacement) {
            return false;
        }

        if(en instanceof Tank) {
            if(quirk.getName().equals("veh_locked_turret")
                    || quirk.getName().equals("veh_stabilizer_crit")
                    || quirk.getName().equals("sensors_1_crit")) {
                return true;
            }
            return false;
        }

        if(en instanceof BattleArmor) {
                return false;
        }

        if(en instanceof Jumpship) {
                return false;
        } 
        if (en instanceof Dropship) {
                return false;
        }
        if (en instanceof Aero) {
                return false;
        }
        if (en instanceof Protomech) {
            return false;
        }


        return false;

    }

    private static class PartialRepairInfo extends AbstractOptionsInfo {
        private static AbstractOptionsInfo instance = new PartialRepairInfo();

        public static AbstractOptionsInfo getInstance() {
            return instance;
        }

        protected PartialRepairInfo() {
            super("PartialRepairsInfo"); //$NON-NLS-1$
        }
    }



}
