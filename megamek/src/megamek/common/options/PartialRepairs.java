/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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
package megamek.common.options;

import megamek.common.*;

/**
 * Contains the options for partial repair properties
 *
 * @author Neth (Thomas Pfau)
 */
public class PartialRepairs extends AbstractOptions {
    private static final long serialVersionUID = 7618380522964885742L;
    public static final String PART_REPAIRS  = "PartRepairs"; 

    public PartialRepairs() {
        super();
    }

    @Override
    public void initialize() {
        //partial repairs
        IBasicOptionGroup partRep = addGroup("part_repairs", PART_REPAIRS);
        addOption(partRep, "mech_reactor_3_crit", false);
        addOption(partRep, "mech_reactor_2_crit", false);
        addOption(partRep, "mech_reactor_1_crit", false);
        addOption(partRep, "mech_gyro_1_crit", false);
        addOption(partRep, "mech_gyro_2_crit", false);
        addOption(partRep, "sensors_1_crit", false);
        addOption(partRep, "mech_sensors_2_crit", false);
        addOption(partRep, "veh_stabilizer_crit", false);
        addOption(partRep, "veh_locked_turret", false);
        addOption(partRep, "mech_engine_replace", false);
        addOption(partRep, "mech_gyro_replace", false);
        addOption(partRep, "aero_avionics_replace", false); 
        addOption(partRep, "aero_cic_fcs_replace", false); 
        addOption(partRep, "aero_gear_replace", false); 
        addOption(partRep, "aero_sensor_replace", false); 
        addOption(partRep, "aero_avionics_crit", false); 
        addOption(partRep, "aero_cic_fcs_crit", false); 
        addOption(partRep, "aero_collar_crit", false); 
        addOption(partRep, "aero_engine_crit", false); 
        addOption(partRep, "aero_asf_fueltank_crit", false); 
        addOption(partRep, "aero_fueltank_crit", false); 
        addOption(partRep, "aero_gear_crit", false); 
    }

    @Override
    protected AbstractOptionsInfo getOptionsInfoImp() {
        return PartialRepairInfo.getInstance();
    }

    public static boolean isPartRepLegalFor(IOption quirk, Entity en) {
        if (en instanceof Mech) {
            return quirk.getName().equals("mech_reactor_3_crit")
                    || quirk.getName().equals("mech_reactor_2_crit")
                    || quirk.getName().equals("mech_reactor_1_crit")
                    || quirk.getName().equals("mech_gyro_2_crit")
                    || quirk.getName().equals("mech_gyro_1_crit")
                    || quirk.getName().equals("mech_sensors_2_crit")
                    || quirk.getName().equals("mech_engine_replace")
                    || quirk.getName().equals("mech_gyro_replace")
                    || quirk.getName().equals("sensors_1_crit");
        } else if (en instanceof GunEmplacement) {
            return false;
        } else if (en instanceof Tank) {
            return quirk.getName().equals("veh_locked_turret")
                    || quirk.getName().equals("veh_stabilizer_crit")
                    || quirk.getName().equals("sensors_1_crit");
        } else if (en instanceof Warship) {
            return quirk.getName().equals("aero_avionics_replace")
                    || quirk.getName().equals("aero_cic_fcs_replace")
                    || quirk.getName().equals("aero_sensor_replace")
                    || quirk.getName().equals("aero_avionics_crit")
                    || quirk.getName().equals("aero_cic_fcs_crit")
                    || quirk.getName().equals("aero_collar_crit")
                    || quirk.getName().equals("aero_engine_crit")
                    || quirk.getName().equals("aero_fueltank_crit")
                    || quirk.getName().equals("sensors_1_crit");
        } else if (en instanceof Jumpship) {
            return quirk.getName().equals("aero_avionics_replace")
                    || quirk.getName().equals("aero_cic_fcs_replace")
                    || quirk.getName().equals("aero_sensor_replace")
                    || quirk.getName().equals("aero_avionics_crit")
                    || quirk.getName().equals("aero_cic_fcs_crit")
                    || quirk.getName().equals("aero_collar_crit")
                    || quirk.getName().equals("aero_fueltank_crit")
                    || quirk.getName().equals("sensors_1_crit");
        } else if (en instanceof Dropship) {
            return quirk.getName().equals("aero_avionics_replace")
                    || quirk.getName().equals("aero_cic_fcs_replace")
                    || quirk.getName().equals("aero_gear_replace")
                    || quirk.getName().equals("aero_sensor_replace")
                    || quirk.getName().equals("aero_avionics_crit")
                    || quirk.getName().equals("aero_cic_fcs_crit")
                    || quirk.getName().equals("aero_collar_crit")
                    || quirk.getName().equals("aero_engine_crit")
                    || quirk.getName().equals("aero_fueltank_crit")
                    || quirk.getName().equals("aero_gear_crit")
                    || quirk.getName().equals("sensors_1_crit");
        } else if (en instanceof Aero) {
            return quirk.getName().equals("aero_avionics_replace")
                    || quirk.getName().equals("aero_cic_fcs_replace")
                    || quirk.getName().equals("aero_gear_replace")
                    || quirk.getName().equals("aero_sensor_replace")
                    || quirk.getName().equals("aero_avionics_crit")
                    || quirk.getName().equals("aero_cic_fcs_crit")
                    || quirk.getName().equals("aero_engine_crit")
                    || quirk.getName().equals("aero_asf_fueltank_crit")
                    || quirk.getName().equals("aero_gear_crit")
                    || quirk.getName().equals("sensors_1_crit");
        } else {
            return false;
        }
    }

    private static class PartialRepairInfo extends AbstractOptionsInfo {
        private static AbstractOptionsInfo instance = new PartialRepairInfo();

        public static AbstractOptionsInfo getInstance() {
            return instance;
        }

        protected PartialRepairInfo() {
            super("PartialRepairsInfo");
        }
    }
}
