package megamek.common;

import java.util.EnumMap;

/**
 * @author Neoancient
 */
public enum BattleForceSPA {
    // From StratOps
    PRB, AFC, AT, ATxD, AMP, AECM, AM, AMS, ARTAIS, ARTAC, ARTBA,
    ARTCM5, ARTCM7, ARTCM9, ARTCM12, ARTT, ARTS, ARTLT, ARTTC, ARTSC, ARTLTC, ARM, ARS, ATMO,
    BAR, BFC, BHJ, SHLD, BH, BOMB, BT, BRID, C3BSS, C3BSM, C3EM, C3M, C3RS, C3S, C3I, CAR,
    CK, CT, CTxD, CASE, CASEII, D, DRO, DCC, DT, ES, ECM, ENE, ENG,
    SEAL, XMEC, FR, FD, HT, HELI, HPG, INARC, IF, ITSM, IT,
    KF, LG, LEAD, LPRB, LECM, LTAG, LF, MAG, MT, MTxD, MEC, MEL, MAS, LMAS, MDS, MSW,
    MASH, MFB, MHQ, SNARC, CNARC, NC3, ORO, OMNI, PNT,
    PT, PTxD, RAIL, RCN, RSD, SAW, SCR, SRCH, ST, STxD, SDS, SOA, SPC, STL, SLG, TAG, MTA, BTA,
    TELE, TSM, UMU, VRT, VTM, VTMxD, VTH, VTHxD, VTS, VTSxD, VLG, VSTOL, WAT,
    // From IOps
    ABA, BRA, BHJ2, BHJ3, BIM, DN, GLD, IRA, LAM, MCS, UCS, NOVA, CASEP, QV, RHS,
    RAMS, ECS, DJ, HJ, RBT, JAM, TSEMP, TSEMPO, TSI, VR,
    // Battleforce only
    ATAC, DB, EEE, PL, TCP,
    // AlphaStrike only
    CRW, CR, DUN, EE, FC, FF, MTN, OVL, PARA, TSMX, RCA, RFA, HTC, TRN,
    ;
    
    static EnumMap<BattleForceSPA, BattleForceSPA> transportBayDoors;
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
        return ordinal() < CRW.ordinal();
    }
    
    public boolean usedByAlphaStrike() {
        return ordinal() < ATAC.ordinal() || ordinal() >= CRW.ordinal();
    }
    
    public boolean isDoor() {
        return name().endsWith("xD");
    }
    
    public BattleForceSPA getDoor() {
        return transportBayDoors.get(this);
    }
}
