/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
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
package megamek.client.bot.princess;


/**
 * This class stores all the calculations of probabilities given the ruleset
 */
public class ProbabilityCalculator {


    //How likely am I to hit a certain location with weapons fire, given what direction the fire is coming from
    //Order is: LOC_HEAD,LOC_CT,LOC_RT,LOC_LT,LOC_RARM,LOC_LARM,LOC_RLEG,LOC_LLEG Defined in megamek.common.Mech
    public static final double []hit_probabilities_front={1./36,7./36,5./36,5./36,5./36,5./36,4./36,4./36};
    public static final double []hit_probabilities_rside={1./36,5./36,7./36,4./36,7./36,3./36,7./36,3./36};
    public static final double []hit_probabilities_lside={1./36,5./36,4./36,7./36,3./36,7./36,3./36,7./36};

    //How likely am I to hit a certain location with a punch, given what direction the fire is coming from
    //Order is: LOC_HEAD,LOC_CT,LOC_RT,LOC_LT,LOC_RARM,LOC_LARM,LOC_RLEG,LOC_LLEG Defined in megamek.common.Mech
    public static final double []hit_probabilities_punch_front={1./6,1./6,1./6,1./6,1./6,1./6,0,0};
    public static final double []hit_probabilities_punch_rside={1./6,1./6,2./6,  0,2./6,  0,0,0};
    public static final double []hit_probabilities_punch_lside={1./6,1./6,  0,2./6,  0,2./6,0,0};

    //How likely am I to hit a certain location with a punch, given what direction the fire is coming from
    //Order is: LOC_HEAD,LOC_CT,LOC_RT,LOC_LT,LOC_RARM,LOC_LARM,LOC_RLEG,LOC_LLEG Defined in megamek.common.Mech
    public static final double []hit_probabilities_kick_front={   0,  0,  0,  0,  0,  0,3./6,3./6};
    public static final double []hit_probabilities_kick_rside={   0,  0,  0,  0,  0,  0,  1.,  0};
    public static final double []hit_probabilities_kick_lside={   0,  0,  0,  0,  0,  0,  0,  1.};

    /**
     * returns the probability that hit_location (from class mech) is hit when the mech is attacked with weapons fire from facing attackedfrom_facing, with 0 defined as forward
     */
    static double getHitProbability(int attackedfrom_facing,int hit_location) {
        if((attackedfrom_facing==5)||(attackedfrom_facing==0)||(attackedfrom_facing==1)||(attackedfrom_facing==3)) {
            return hit_probabilities_front[hit_location];
        }
        if(attackedfrom_facing==2) {
            return hit_probabilities_rside[hit_location];
        }
        //assume attackedfrom_facing==4
        return hit_probabilities_lside[hit_location];
    }

    /**
     * returns the probability that hit_location (from class mech) is hit when the mech is attacked with a punch from facing attackedfrom_facing, with 0 defined as forward
     */
    static double getHitProbability_Punch(int attackedfrom_facing,int hit_location) {
        if((attackedfrom_facing==5)||(attackedfrom_facing==0)||(attackedfrom_facing==1)||(attackedfrom_facing==3)) {
            return hit_probabilities_punch_front[hit_location];
        }
        if(attackedfrom_facing==2) {
            return hit_probabilities_punch_rside[hit_location];
        }
        //assume attackedfrom_facing==4
        return hit_probabilities_punch_lside[hit_location];
    }

    /**
     * returns the probability that hit_location (from class mech) is hit when the mech is attacked with a kick from facing attackedfrom_facing, with 0 defined as forward
     */
    static double getHitProbability_Kick(int attackedfrom_facing,int hit_location) {
        if((attackedfrom_facing==5)||(attackedfrom_facing==0)||(attackedfrom_facing==1)||(attackedfrom_facing==3)) {
            return hit_probabilities_kick_front[hit_location];
        }
        if(attackedfrom_facing==2) {
            return hit_probabilities_kick_rside[hit_location];
        }
        //assume attackedfrom_facing==4
        return hit_probabilities_kick_lside[hit_location];
    }

    /**
     * If we roll on the critical hit table, how many criticals do we expect to cause
     */
    static double getExpectedCriticalHitCount() {
        return 0.611; // (9+2*5+3)/36
    }

}
