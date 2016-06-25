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
import megamek.common.SupportTank;
import megamek.common.SupportVTOL;
import megamek.common.Tank;

/**
 * Contains the options determining quirks of the unit
 *
 * @author Taharqa (Jay Lawson)
 */
public class Quirks extends AbstractOptions {
    private static final long serialVersionUID = 7618380522964885740L;
    public static final String POS_QUIRKS = "PosQuirks"; //$NON-NLS-1$
    public static final String NEG_QUIRKS = "NegQuirks"; //$NON-NLS-1$

    @Override
    public synchronized void initialize() {
        //positive quirks
        IBasicOptionGroup posQuirk = addGroup("pos_quirks", POS_QUIRKS); //$NON-NLS-1$
        addOption(posQuirk, OptionsConstants.QUIRK_POS_ANTI_AIR, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_ATMO_FLYER, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_BATTLE_COMP, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_BATTLE_FIST, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_COMBAT_COMPUTER, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_COMMAND_MECH, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_COMPACT, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_COWL, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_DISTRACTING, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_DOCKING_ARMS, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_EASY_MAINTAIN, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_EASY_PILOT, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_EXT_TWIST, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_FAST_RELOAD, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_HEAD_EJECT, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_HYPER_ACTUATOR, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_IMP_COM, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_IMP_LIFE_SUPPORT, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_IMP_TARG_L, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_IMP_TARG_M, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_IMP_TARG_S, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_IMPROVED_SENSORS, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_INTERNAL_BOMB, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_LOW_PROFILE, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_MOD_WEAPONS, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_MULTI_TRAC, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_OVERHEAD_ARMS, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_POWER_REVERSE, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_PRO_ACTUATOR, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_REINFORCED_LEGS, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_RUMBLE_SEAT, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_SEARCHLIGHT, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_STABLE, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_TRAILER_HITCH, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_VAR_RNG_TARG_L, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_VAR_RNG_TARG_S, false);
        addOption(posQuirk, OptionsConstants.QUIRK_POS_VTOL_ROTOR, false);

        
        //not yet implemented
        //Docking Arms (docking unimplemented)
        //Fast Reload (no game effect at present)
        //Improved Communications
        //Internal Bomb Bay
        //Variable Range Targeting
        //VTOL Rotor Arrangement (no vee adv move rules)
        //Compact Mech

        // negative quirks
        IBasicOptionGroup negQuirk = addGroup("neg_quirks", NEG_QUIRKS); //$NON-NLS-1$
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_BAD_REP, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_CRAMPED_COCKPIT, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_DIFFICULT_EJECT, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_DIFFICULT_MAINTAIN, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_EXP_ACTUATOR, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_FLAWED_COOLING, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_FRAGILE_FUEL, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_GAS_HOG, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_HARD_PILOT, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_ILLEGAL_DESIGN, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_LARGE_DROPPER, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_LOW_ARMS, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_NO_ARMS, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_NO_EJECT, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_NO_TWIST, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_NON_STANDARD, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_OBSOLETE, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_POOR_LIFE_SUPPORT, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_POOR_PERFORMANCE, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_POOR_SEALING, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_POOR_TARG_L, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_POOR_TARG_M, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_POOR_TARG_S, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_POOR_WORK, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_PROTOTYPE, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_SENSOR_GHOSTS, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_SUSCEPTIBLE_CWS, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_UNBALANCED, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_UNSTREAMLINED, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_WEAK_HEAD_1, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_WEAK_HEAD_2, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_WEAK_HEAD_3, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_WEAK_HEAD_4, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_WEAK_HEAD_5, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_WEAK_LEGS, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_WEAK_UNDERCARRIAGE,false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_ATMO_INSTABILITY, false);
        addOption(negQuirk, OptionsConstants.QUIRK_NEG_OVERSIZED, false);
        
        //quirks not implemented yet
        //Cooling System Flaws
        //Exposed Weapon Linkage (weapon-specific, sort of)
        //Gas Hog
        //Large Dropship (no docking)
        //Un-streamlined
        //Weak Head Armor
        //Weak Undercarriage (no landing)
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.options.AbstractOptions#getOptionsInfoImp()
     */
    @Override
    protected AbstractOptionsInfo getOptionsInfoImp() {
        return QuirksInfo.getInstance();
    }

    public static boolean isQuirkLegalFor(IOption quirk, Entity en) {
        String qName = quirk.getName();
        
        if ((en.getEngine() != null) &&
                ((en.getEngine().getEngineType() == Engine.COMBUSTION_ENGINE) ||
                (en.getEngine().getEngineType() == Engine.FUEL_CELL)) &&
                qName.equals(OptionsConstants.QUIRK_NEG_GAS_HOG)) {
            return true;
        }

        if(en instanceof Mech) {
            if (qName.equals(OptionsConstants.QUIRK_POS_BATTLE_FIST)) {
                // Mechs with a hand actuator can have battlefists
                if (en.hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_LARM) 
                        || en.hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_RARM)) {
                    return true;
                } else {
                    return false;
                }
            }
            if(qName.equals(OptionsConstants.QUIRK_POS_ATMO_FLYER)
                    || qName.equals(OptionsConstants.QUIRK_NEG_ATMO_INSTABILITY)
                    || qName.equals(OptionsConstants.QUIRK_POS_DOCKING_ARMS)
                    || qName.equals(OptionsConstants.QUIRK_NEG_FRAGILE_FUEL)
                    || qName.equals(OptionsConstants.QUIRK_POS_INTERNAL_BOMB)
                    || qName.equals(OptionsConstants.QUIRK_POS_TRAILER_HITCH)
                    || qName.equals(OptionsConstants.QUIRK_NEG_LARGE_DROPPER)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_UNDERCARRIAGE)
                    || qName.equals(OptionsConstants.QUIRK_POS_VTOL_ROTOR)
                    || qName.equals(OptionsConstants.QUIRK_NEG_GAS_HOG)
                    || qName.equals(OptionsConstants.QUIRK_POS_POWER_REVERSE)
                    || qName.equals(OptionsConstants.QUIRK_NEG_UNSTREAMLINED)) {
                return false;
            }
            return true;
        }

        if(en instanceof GunEmplacement) {
            return false;
        }

        if(en instanceof Tank) {

            // Power reverse only legal for wheeled or tracked combat vehicles
            if (qName.equals(OptionsConstants.QUIRK_POS_POWER_REVERSE)) {
                if ((en.getMovementMode() == EntityMovementMode.WHEELED
                        || en.getMovementMode() == EntityMovementMode.TRACKED)
                        && !((en instanceof SupportTank)
                                || (en instanceof SupportVTOL))) {
                    return true;
                } else {
                    return false;
                }
            } else if(qName.equals(OptionsConstants.QUIRK_POS_ATMO_FLYER)
                    || qName.equals(OptionsConstants.QUIRK_POS_COMBAT_COMPUTER)
                    || qName.equals(OptionsConstants.QUIRK_POS_COMMAND_MECH)
                    || qName.equals(OptionsConstants.QUIRK_POS_COWL)
                    || qName.equals(OptionsConstants.QUIRK_POS_DOCKING_ARMS)
                    || qName.equals(OptionsConstants.QUIRK_POS_EASY_PILOT)
                    || qName.equals(OptionsConstants.QUIRK_POS_EXT_TWIST)
                    || qName.equals(OptionsConstants.QUIRK_POS_HYPER_ACTUATOR)
                    || qName.equals(OptionsConstants.QUIRK_POS_IMP_LIFE_SUPPORT)
                    || qName.equals(OptionsConstants.QUIRK_POS_INTERNAL_BOMB)
                    || qName.equals(OptionsConstants.QUIRK_POS_MULTI_TRAC)
                    || qName.equals(OptionsConstants.QUIRK_POS_REINFORCED_LEGS)
                    || qName.equals(OptionsConstants.QUIRK_POS_STABLE)
                    || qName.equals(OptionsConstants.QUIRK_POS_OVERHEAD_ARMS)
                    || qName.equals(OptionsConstants.QUIRK_POS_STABLE)
                    || qName.equals(OptionsConstants.QUIRK_POS_COMPACT)
                    || qName.equals(OptionsConstants.QUIRK_POS_BATTLE_FIST)
                    || qName.equals(OptionsConstants.QUIRK_POS_HEAD_EJECT)
                    || qName.equals(OptionsConstants.QUIRK_POS_PRO_ACTUATOR)
                    || qName.equals(OptionsConstants.QUIRK_NEG_ATMO_INSTABILITY)
                    || qName.equals(OptionsConstants.QUIRK_NEG_CRAMPED_COCKPIT)
                    || qName.equals(OptionsConstants.QUIRK_NEG_DIFFICULT_EJECT)
                    || qName.equals(OptionsConstants.QUIRK_NEG_EXP_ACTUATOR)
                    || qName.equals(OptionsConstants.QUIRK_NEG_NO_ARMS)
                    || qName.equals(OptionsConstants.QUIRK_NEG_NO_EJECT)
                    || qName.equals(OptionsConstants.QUIRK_NEG_NO_TWIST)
                    || qName.equals(OptionsConstants.QUIRK_NEG_LARGE_DROPPER)
                    || qName.equals(OptionsConstants.QUIRK_NEG_POOR_LIFE_SUPPORT)
                    || qName.equals(OptionsConstants.QUIRK_NEG_UNBALANCED)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_LEGS)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_UNDERCARRIAGE)
                    || qName.equals(OptionsConstants.QUIRK_POS_VTOL_ROTOR)
                    || qName.equals(OptionsConstants.QUIRK_NEG_FLAWED_COOLING)
                    || qName.equals(OptionsConstants.QUIRK_NEG_UNSTREAMLINED)
                    || qName.equals(OptionsConstants.QUIRK_NEG_GAS_HOG)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_HEAD_1)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_HEAD_2)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_HEAD_3)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_HEAD_4)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_HEAD_5)) {
                return false;
            }
            
            if(!en.getEngine().isFusion() && qName.equals(OptionsConstants.QUIRK_NEG_FRAGILE_FUEL)) {
                return false;
            }
            
            if(qName.equals(OptionsConstants.QUIRK_POS_TRAILER_HITCH)
                    && (en.getMovementMode() == EntityMovementMode.HOVER)) {
                return false;
            }
            
            if(qName.equals(OptionsConstants.QUIRK_POS_TRAILER_HITCH)
                    && (en.getMovementMode() == EntityMovementMode.VTOL)) {
                return false;
            }
                        
            return true;
        }
              
        if(en instanceof BattleArmor) {
            if(qName.equals(OptionsConstants.QUIRK_POS_ATMO_FLYER)
                    || qName.equals(OptionsConstants.QUIRK_POS_ANTI_AIR)
                    || qName.equals(OptionsConstants.QUIRK_POS_BATTLE_COMP)
                    || qName.equals(OptionsConstants.QUIRK_POS_COMBAT_COMPUTER)
                    || qName.equals(OptionsConstants.QUIRK_POS_COMMAND_MECH)
                    || qName.equals(OptionsConstants.QUIRK_POS_COWL)
                    || qName.equals(OptionsConstants.QUIRK_POS_DOCKING_ARMS)
                    || qName.equals(OptionsConstants.QUIRK_POS_EXT_TWIST)
                    || qName.equals(OptionsConstants.QUIRK_POS_HYPER_ACTUATOR)
                    || qName.equals(OptionsConstants.QUIRK_POS_IMPROVED_SENSORS)
                    || qName.equals(OptionsConstants.QUIRK_POS_IMP_LIFE_SUPPORT)
                    || qName.equals(OptionsConstants.QUIRK_POS_IMP_TARG_S)
                    || qName.equals(OptionsConstants.QUIRK_POS_IMP_TARG_M)
                    || qName.equals(OptionsConstants.QUIRK_POS_IMP_TARG_L)
                    || qName.equals(OptionsConstants.QUIRK_POS_INTERNAL_BOMB)
                    || qName.equals(OptionsConstants.QUIRK_POS_MULTI_TRAC)
                    || qName.equals(OptionsConstants.QUIRK_POS_LOW_PROFILE)
                    || qName.equals(OptionsConstants.QUIRK_POS_REINFORCED_LEGS)
                    || qName.equals(OptionsConstants.QUIRK_POS_STABLE)
                    || qName.equals(OptionsConstants.QUIRK_POS_SEARCHLIGHT)
                    || qName.equals(OptionsConstants.QUIRK_POS_TRAILER_HITCH)
                    || qName.equals(OptionsConstants.QUIRK_NEG_ATMO_INSTABILITY)
                    || qName.equals(OptionsConstants.QUIRK_NEG_CRAMPED_COCKPIT)
                    || qName.equals(OptionsConstants.QUIRK_NEG_DIFFICULT_EJECT)
                    || qName.equals(OptionsConstants.QUIRK_NEG_EXP_ACTUATOR)
                    || qName.equals(OptionsConstants.QUIRK_NEG_FRAGILE_FUEL)
                    || qName.equals(OptionsConstants.QUIRK_NEG_NO_ARMS)
                    || qName.equals(OptionsConstants.QUIRK_NEG_NO_EJECT)
                    || qName.equals(OptionsConstants.QUIRK_POS_HEAD_EJECT)
                    || qName.equals(OptionsConstants.QUIRK_NEG_NO_TWIST)
                    || qName.equals(OptionsConstants.QUIRK_NEG_LARGE_DROPPER)
                    || qName.equals(OptionsConstants.QUIRK_NEG_POOR_LIFE_SUPPORT)
                    || qName.equals(OptionsConstants.QUIRK_NEG_UNBALANCED)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_LEGS)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_UNDERCARRIAGE)
                    || qName.equals(OptionsConstants.QUIRK_POS_VTOL_ROTOR)
                    || qName.equals(OptionsConstants.QUIRK_NEG_FLAWED_COOLING)
                    || qName.equals(OptionsConstants.QUIRK_NEG_UNSTREAMLINED)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_HEAD_1)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_HEAD_2)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_HEAD_3)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_HEAD_4)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_HEAD_5)
                    || qName.equals(OptionsConstants.QUIRK_NEG_GAS_HOG)
                    || qName.equals(OptionsConstants.QUIRK_POS_RUMBLE_SEAT)
                    || qName.equals(OptionsConstants.QUIRK_NEG_POOR_PERFORMANCE)
                    || qName.equals(OptionsConstants.QUIRK_POS_POWER_REVERSE)
                    || qName.equals(OptionsConstants.QUIRK_POS_STABLE)
                    || qName.equals(OptionsConstants.QUIRK_POS_COMPACT)
                    || qName.equals(OptionsConstants.QUIRK_POS_OVERHEAD_ARMS)   
                    || qName.equals(OptionsConstants.QUIRK_POS_PRO_ACTUATOR)                    
                    || qName.equals(OptionsConstants.QUIRK_POS_BATTLE_FIST)) {
                return false;
            }
            return true;
        }

        if(en instanceof Jumpship) {
            if(qName.equals(OptionsConstants.QUIRK_POS_ATMO_FLYER)
                    || qName.equals(OptionsConstants.QUIRK_POS_ANTI_AIR)
                    || qName.equals(OptionsConstants.QUIRK_POS_BATTLE_COMP)
                    || qName.equals(OptionsConstants.QUIRK_POS_COMBAT_COMPUTER)
                    || qName.equals(OptionsConstants.QUIRK_POS_COMMAND_MECH)
                    || qName.equals(OptionsConstants.QUIRK_POS_COWL)
                    || qName.equals(OptionsConstants.QUIRK_POS_DOCKING_ARMS)
                    || qName.equals(OptionsConstants.QUIRK_POS_EXT_TWIST)
                    || qName.equals(OptionsConstants.QUIRK_POS_FAST_RELOAD)
                    || qName.equals(OptionsConstants.QUIRK_POS_HYPER_ACTUATOR)
                    || qName.equals(OptionsConstants.QUIRK_POS_IMPROVED_SENSORS)
                    || qName.equals(OptionsConstants.QUIRK_POS_IMP_LIFE_SUPPORT)
                    || qName.equals(OptionsConstants.QUIRK_POS_INTERNAL_BOMB)
                    || qName.equals(OptionsConstants.QUIRK_POS_MOD_WEAPONS)
                    || qName.equals(OptionsConstants.QUIRK_POS_MULTI_TRAC)
                    || qName.equals(OptionsConstants.QUIRK_POS_LOW_PROFILE)
                    || qName.equals(OptionsConstants.QUIRK_POS_LOW_PROFILE)
                    || qName.equals(OptionsConstants.QUIRK_POS_REINFORCED_LEGS)
                    || qName.equals(OptionsConstants.QUIRK_POS_STABLE)
                    || qName.equals(OptionsConstants.QUIRK_POS_TRAILER_HITCH)
                    || qName.equals(OptionsConstants.QUIRK_POS_SEARCHLIGHT)
                    || qName.equals(OptionsConstants.QUIRK_NEG_ATMO_INSTABILITY)
                    || qName.equals(OptionsConstants.QUIRK_NEG_CRAMPED_COCKPIT)
                    || qName.equals(OptionsConstants.QUIRK_NEG_DIFFICULT_EJECT)
                    || qName.equals(OptionsConstants.QUIRK_NEG_EXP_ACTUATOR)
                    || qName.equals(OptionsConstants.QUIRK_NEG_NO_ARMS)
                    || qName.equals(OptionsConstants.QUIRK_NEG_NO_EJECT)
                    || qName.equals(OptionsConstants.QUIRK_POS_HEAD_EJECT)
                    || qName.equals(OptionsConstants.QUIRK_NEG_NO_TWIST)
                    || qName.equals(OptionsConstants.QUIRK_NEG_LARGE_DROPPER)
                    || qName.equals(OptionsConstants.QUIRK_NEG_POOR_LIFE_SUPPORT)
                    || qName.equals(OptionsConstants.QUIRK_NEG_UNBALANCED)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_LEGS)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_UNDERCARRIAGE)
                    || qName.equals(OptionsConstants.QUIRK_POS_VTOL_ROTOR)
                    || qName.equals(OptionsConstants.QUIRK_NEG_FLAWED_COOLING)
                    || qName.equals(OptionsConstants.QUIRK_NEG_UNSTREAMLINED)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_HEAD_1)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_HEAD_2)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_HEAD_3)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_HEAD_4)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_HEAD_5)
                    || qName.equals(OptionsConstants.QUIRK_NEG_GAS_HOG)
                    || qName.equals(OptionsConstants.QUIRK_POS_RUMBLE_SEAT)
                    || qName.equals(OptionsConstants.QUIRK_NEG_POOR_PERFORMANCE)
                    || qName.equals(OptionsConstants.QUIRK_POS_DISTRACTING)
                    || qName.equals(OptionsConstants.QUIRK_NEG_POOR_SEALING)
                    || qName.equals(OptionsConstants.QUIRK_POS_POWER_REVERSE)
                    || qName.equals(OptionsConstants.QUIRK_POS_STABLE)
                    || qName.equals(OptionsConstants.QUIRK_POS_COMPACT)
                    || qName.equals(OptionsConstants.QUIRK_POS_PRO_ACTUATOR)
                    || qName.equals(OptionsConstants.QUIRK_POS_BATTLE_FIST)) {
                return false;
            }
            return true;
        } else if (en instanceof Dropship) {
            if (qName.equals(OptionsConstants.QUIRK_POS_ANTI_AIR)
                    || qName.equals(OptionsConstants.QUIRK_POS_BATTLE_COMP)
                    || qName.equals(OptionsConstants.QUIRK_POS_COMBAT_COMPUTER)
                    || qName.equals(OptionsConstants.QUIRK_POS_COMMAND_MECH)
                    || qName.equals(OptionsConstants.QUIRK_POS_COWL)
                    || qName.equals(OptionsConstants.QUIRK_POS_EXT_TWIST)
                    || qName.equals(OptionsConstants.QUIRK_POS_FAST_RELOAD)
                    || qName.equals(OptionsConstants.QUIRK_POS_HYPER_ACTUATOR)
                    || qName.equals(OptionsConstants.QUIRK_POS_IMPROVED_SENSORS)
                    || qName.equals(OptionsConstants.QUIRK_POS_IMP_LIFE_SUPPORT)
                    || qName.equals(OptionsConstants.QUIRK_POS_MULTI_TRAC)
                    || qName.equals(OptionsConstants.QUIRK_POS_LOW_PROFILE)
                    || qName.equals(OptionsConstants.QUIRK_POS_LOW_PROFILE)
                    || qName.equals(OptionsConstants.QUIRK_POS_REINFORCED_LEGS)
                    || qName.equals(OptionsConstants.QUIRK_POS_STABLE)
                    || qName.equals(OptionsConstants.QUIRK_POS_SEARCHLIGHT)
                    || qName.equals(OptionsConstants.QUIRK_POS_TRAILER_HITCH)
                    || qName.equals(OptionsConstants.QUIRK_NEG_CRAMPED_COCKPIT)
                    || qName.equals(OptionsConstants.QUIRK_NEG_DIFFICULT_EJECT)
                    || qName.equals(OptionsConstants.QUIRK_NEG_EXP_ACTUATOR)
                    || qName.equals(OptionsConstants.QUIRK_NEG_NO_ARMS)
                    || qName.equals(OptionsConstants.QUIRK_NEG_NO_EJECT)
                    || qName.equals(OptionsConstants.QUIRK_POS_HEAD_EJECT)
                    || qName.equals(OptionsConstants.QUIRK_NEG_NO_TWIST)
                    || qName.equals(OptionsConstants.QUIRK_NEG_POOR_LIFE_SUPPORT)
                    || qName.equals(OptionsConstants.QUIRK_NEG_UNBALANCED)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_UNDERCARRIAGE)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_LEGS)
                    || qName.equals(OptionsConstants.QUIRK_POS_VTOL_ROTOR)
                    || qName.equals(OptionsConstants.QUIRK_NEG_FLAWED_COOLING)
                    || qName.equals(OptionsConstants.QUIRK_POS_RUMBLE_SEAT)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_HEAD_1)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_HEAD_2)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_HEAD_3)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_HEAD_4)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_HEAD_5)
                    || qName.equals(OptionsConstants.QUIRK_NEG_POOR_SEALING)
                    || qName.equals(OptionsConstants.QUIRK_POS_POWER_REVERSE)
                    || qName.equals(OptionsConstants.QUIRK_POS_STABLE)
                    || qName.equals(OptionsConstants.QUIRK_POS_COMPACT)
                    || qName.equals(OptionsConstants.QUIRK_POS_PRO_ACTUATOR)
                    || qName.equals(OptionsConstants.QUIRK_POS_BATTLE_FIST)) {
                return false;
            }
            return true;
        } else if (en instanceof Aero) {
            if (qName.equals(OptionsConstants.QUIRK_POS_ANTI_AIR)
                    || qName.equals(OptionsConstants.QUIRK_POS_BATTLE_COMP)
                    || qName.equals(OptionsConstants.QUIRK_POS_COMMAND_MECH)
                    || qName.equals(OptionsConstants.QUIRK_POS_COWL)
                    || qName.equals(OptionsConstants.QUIRK_POS_DOCKING_ARMS)
                    || qName.equals(OptionsConstants.QUIRK_POS_EXT_TWIST)
                    || qName.equals(OptionsConstants.QUIRK_POS_HYPER_ACTUATOR)
                    || qName.equals(OptionsConstants.QUIRK_POS_IMPROVED_SENSORS)
                    || qName.equals(OptionsConstants.QUIRK_POS_IMP_LIFE_SUPPORT)
                    || qName.equals(OptionsConstants.QUIRK_POS_MULTI_TRAC)
                    || qName.equals(OptionsConstants.QUIRK_POS_PRO_ACTUATOR)
                    || qName.equals(OptionsConstants.QUIRK_POS_LOW_PROFILE)
                    || qName.equals(OptionsConstants.QUIRK_POS_STABLE)
                    || qName.equals(OptionsConstants.QUIRK_POS_SEARCHLIGHT)
                    || qName.equals(OptionsConstants.QUIRK_POS_TRAILER_HITCH)
                    || qName.equals(OptionsConstants.QUIRK_POS_REINFORCED_LEGS)
                    || qName.equals(OptionsConstants.QUIRK_NEG_EXP_ACTUATOR)
                    || qName.equals(OptionsConstants.QUIRK_NEG_NO_ARMS)
                    || qName.equals(OptionsConstants.QUIRK_NEG_NO_TWIST)
                    || qName.equals(OptionsConstants.QUIRK_NEG_LARGE_DROPPER)
                    || qName.equals(OptionsConstants.QUIRK_NEG_UNBALANCED)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_LEGS)
                    || qName.equals(OptionsConstants.QUIRK_POS_HEAD_EJECT)
                    || qName.equals(OptionsConstants.QUIRK_POS_VTOL_ROTOR)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_HEAD_1)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_HEAD_2)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_HEAD_3)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_HEAD_4)
                    || qName.equals(OptionsConstants.QUIRK_NEG_WEAK_HEAD_5)
                    || qName.equals(OptionsConstants.QUIRK_NEG_POOR_SEALING)
                    || qName.equals(OptionsConstants.QUIRK_POS_POWER_REVERSE)
                    || qName.equals(OptionsConstants.QUIRK_POS_STABLE)
                    || qName.equals(OptionsConstants.QUIRK_POS_COMPACT)
                    || qName.equals(OptionsConstants.QUIRK_POS_BATTLE_FIST)) {
                return false;
            }
            return true;
        } else if (en instanceof Protomech) {
            if (qName.equals(OptionsConstants.QUIRK_WEAP_POS_ACCURATE)
                    || qName.equals(OptionsConstants.QUIRK_POS_DISTRACTING)
                    || qName.equals(OptionsConstants.QUIRK_POS_EASY_MAINTAIN)
                    || qName.equals(OptionsConstants.QUIRK_POS_EXT_TWIST)
                    || qName.equals(OptionsConstants.QUIRK_POS_IMP_COM)
                    || qName.equals(OptionsConstants.QUIRK_POS_IMPROVED_SENSORS)
                    || qName.equals(OptionsConstants.QUIRK_POS_IMP_TARG_S)
                    || qName.equals(OptionsConstants.QUIRK_POS_IMP_TARG_M)
                    || qName.equals(OptionsConstants.QUIRK_POS_IMP_TARG_L)
                    || qName.equals(OptionsConstants.QUIRK_POS_VAR_RNG_TARG_L)
                    || qName.equals(OptionsConstants.QUIRK_WEAP_NEG_AMMO_FEED_PROBLEMS)
                    || qName.equals(OptionsConstants.QUIRK_NEG_BAD_REP)
                    || qName.equals(OptionsConstants.QUIRK_NEG_DIFFICULT_MAINTAIN)
                    || qName.equals(OptionsConstants.QUIRK_WEAP_NEG_EXPOSED_LINKAGE)
                    || qName.equals(OptionsConstants.QUIRK_NEG_HARD_PILOT)
                    || qName.equals(OptionsConstants.QUIRK_WEAP_NEG_INACCURATE)
                    || qName.equals(OptionsConstants.QUIRK_NEG_NO_TWIST)
                    || qName.equals(OptionsConstants.QUIRK_POS_HEAD_EJECT)
                    || qName.equals(OptionsConstants.QUIRK_NEG_OBSOLETE)
                    || qName.equals(OptionsConstants.QUIRK_NEG_POOR_PERFORMANCE)
                    || qName.equals(OptionsConstants.QUIRK_NEG_POOR_SEALING)
                    || qName.equals(OptionsConstants.QUIRK_NEG_POOR_TARG_S)
                    || qName.equals(OptionsConstants.QUIRK_NEG_POOR_TARG_M)
                    || qName.equals(OptionsConstants.QUIRK_NEG_POOR_TARG_L)
                    || qName.equals(OptionsConstants.QUIRK_NEG_POOR_WORK)
                    || qName.equals(OptionsConstants.QUIRK_NEG_PROTOTYPE)
                    || qName.equals(OptionsConstants.QUIRK_POS_PRO_ACTUATOR)
                    || qName.equals(OptionsConstants.QUIRK_NEG_SENSOR_GHOSTS))
                    {
                return true;
            }
            return false;
        }


        return false;

    }

    private static class QuirksInfo extends AbstractOptionsInfo {
        private static boolean initliazed = false;
        private static AbstractOptionsInfo instance = new QuirksInfo();

        public static AbstractOptionsInfo getInstance() {
            if (!initliazed) {
                initliazed = true;
                // Create a new dummy Quirks; ensures values initialized
                // Otherwise, could have issues when loading saved games
                new Quirks();
            }
            return instance;
        }

        protected QuirksInfo() {
            super("QuirksInfo"); //$NON-NLS-1$
        }
    }
}
