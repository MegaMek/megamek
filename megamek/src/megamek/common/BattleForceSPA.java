/**
 * 
 */
package megamek.common;

import java.util.EnumMap;

/**
 * @author Neoancient
 *
 */
public enum BattleForceSPA {
    PRB, AFC, AT, ATxD, ATxF, AMP, AECM, AM, AMS, ARTAIS, ARTAC,
    ARTCM5, ARTCM7, ARTCM9, ARTCM12, ARTT, ARTS, ARTLT,ARTTC, ARTSC, ARTLTC, ARM, ARS, ATMO,
    BAR, BFC, BHJ, SHLD, BH, BOMB, BT, BRID, C3BSS, C3BSM, C3EM, C3M, C3RS, C3S, C3I, CAR,
    CK, CT, CTxD, CASE, CASEII, D, DRO, DCC, DT, ES, EEE, ECM, ENE, ENG,
    SEAL, XMEC, FR, FD, HT, HELI, HPG, INARC, IF, ITSM, IT,
    KF, LG, LEAD, LPRB, LECM, LTAG, LF, MAG, MT, MTxD, MEC, MEL, MAS, LMAS, MDS, MSW,
    MASH, MFB, MHQ, SNARC, CNARC, NC3, ORO, OMNI, PNT,
    PT, PTxD, RAIL, RCN, RSD, SAW, SCR, SRCH, ST, STxD, SDS, SOA, SPC, STL, SLG, TAG, MTA, BTA,
    TELE, TSM, UMU, VRT, VTM, VTH, VTMxD, VTHxD, VLG, VSTOL, WAT
    ;
    
    static EnumMap<BattleForceSPA,BattleForceSPA> transportDoors;
    static {
        transportDoors = new EnumMap<>(BattleForceSPA.class);
        transportDoors.put(AT, ATxD);
        transportDoors.put(CT, CTxD);
        transportDoors.put(MT, MTxD);
        transportDoors.put(PT, PTxD);
        transportDoors.put(ST, STxD);
        transportDoors.put(VTM, VTMxD);
        transportDoors.put(VTH, VTHxD);
    }
    
    private boolean battleForce;
    private boolean alphaStrike;
    
    BattleForceSPA(boolean bf, boolean as) {
        battleForce = bf;
        alphaStrike = as;
    }
    
    BattleForceSPA() {
        this(true, true);
    }
    
    public boolean usedByBattleForce() {
        return battleForce;
    }
    
    public boolean usedByAlphaStrike() {
        return alphaStrike;
    }

    public BattleForceSPA getDoor() {
        return transportDoors.get(this);
    }
    
    public boolean isDoor() {
        return name().endsWith("xD");
    }
}
