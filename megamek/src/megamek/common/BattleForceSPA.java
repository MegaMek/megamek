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
    PRB, AFC, AT, ATxD, AMP, AECM, AM, AMS, ARTAIS, ARTAC,
    ARTCM5, ARTCM7, ARTCM9, ARTCM12, ARTT, ARTS, ARTLT,ARTTC, ARTSC, ARTLTC, ARM, ARS, ATMO,
    BAR, BFC, BHJ, SHLD, BH, BOMB, BT, BRID, C3BSS, C3BSM, C3EM, C3M, C3RS, C3S, C3I, CAR,
    CK, CT, CTxD, CASE, CASEII, D, DRO, DCC, DT, ES, ECM, ENE, ENG,
    SEAL, XMEC, FR, FD, HT, HELI, HPG, INARC, IF, ITSM, IT,
    KF, LG, LEAD, LPRB, LECM, LTAG, LF, MAG, MT, MTxD, MEC, MEL, MAS, LMAS, MDS, MSW,
    MASH, MFB, MHQ, SNARC, CNARC, NC3, ORO, OMNI, PNT,
    PT, PTxD, RAIL, RCN, RSD, SAW, SCR, SRCH, ST, STxD, SDS, SOA, SPC, STL, SLG, TAG, MTA, BTA,
    TELE, TSM, UMU, VRT, VTM, VTMxD, VTH, VTHxD, VTS, VTSxD, VLG, VSTOL, WAT,
    //Battleforce only
    EEE,
    //AlphaStrike only
    ABA, BRA, BHJ2, BHJ3, BIM, CRW, CR, DN, DUN, EE, FC, FF, GLD, IRA, LAM, MCS, UCS, MTN,
    NOVA, OVL, PARA, CASEP, TSMX, QV, RHS, RCA, RFA, RAMS, ECS, DJ, HJ, RBT, JAM, TSEMP,
    TSEMPO, HTC, TRN, TSI, VR
    ;
    
    static EnumMap<BattleForceSPA,BattleForceSPA> transportBayDoors;
    static {
        transportBayDoors = new EnumMap<>(BattleForceSPA.class);
        transportBayDoors.put(AT, ATxD);
        transportBayDoors.put(CT, CTxD);
        transportBayDoors.put(MT, MTxD);
        transportBayDoors.put(PT, PTxD);
        transportBayDoors.put(ST, STxD);
        transportBayDoors.put(VTM, VTMxD);
        transportBayDoors.put(VTH, VTHxD);
        transportBayDoors.put(VTS, VTSxD);
    }
    
    public boolean usedByBattleForce() {
        return ordinal() < ABA.ordinal();
    }
    
    public boolean usedByAlphaStrike() {
        return ordinal() < EEE.ordinal() || ordinal() >= ABA.ordinal();
    }
    
    public boolean isDoor() {
        return name().endsWith("xD");
    }
    
    public BattleForceSPA getDoor() {
        return transportBayDoors.get(this);
    }
}
