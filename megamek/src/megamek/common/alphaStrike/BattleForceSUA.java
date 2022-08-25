/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.alphaStrike;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

/**
 * This enum contains AlphaStrike, BattleForce and (some - WIP) Strategic BattleForce Special Unit Abilities
 * (SUAs) and some utility methods for them.
 *
 * @author Neoancient
 * @author Simon (Juliez)
 */
public enum BattleForceSUA {
    UNKNOWN,
    PRB, AC, AFC, AT, ATxD, AMP, AECM, AM, AMS, ARTAIS, ARTAC, ARTBA,
    ARTCM5, ARTCM7, ARTCM9, ARTCM12, ARTT, ARTS, ARTLT, ARTTC, ARTSC, ARTLTC, ARM, ARS, ATMO,
    BAR, BFC, BHJ, SHLD, BH, BOMB, BT, BRID, C3BSS, C3BSM, C3EM, C3M, C3RS, C3S, C3I, CAR,
    CK, CKxD, CT, CTxD, CASE, CASEII, D, DRO, DCC, DT, ES, ECM, ENE, ENG, FLK,
    SEAL, XMEC, FR, FD, HT, HELI, HPG, IATM, INARC, IF, ITSM, IT,
    KF, LG, LEAD, LPRB, LECM, LRM, LTAG, LF, MAG, MT, MTxD, MEC, MEL, MAS, LMAS, MDS, MSW,
    MASH, MFB, MHQ, SNARC, CNARC, NC3, ORO, OMNI, PNT,
    PT, PTxD, RAIL, RCN, REAR, REL, RSD, SAW, SCR, SRCH, SRM, ST, STxD, SDS, SOA, SPC, STL, SLG, TAG, MTAS, BTAS,
    TELE, TSM, TUR, TOR, UMU, VRT, VTM, VTMxD, VTH, VTHxD, VTS, VTSxD, VLG, VSTOL, WAT,
    ABA, BRA, BHJ2, BHJ3, BIM, DN, GLD, IRA, LAM, MCS, UCS, NOVA, CASEP, QV, RHS,
    RAMS, ECS, DJ, HJ, RBT, JAM, TSEMP, TSEMPO, TSI, VR,
    ATAC, DB, PL, TCP, SDCS,
    //TODO: PL, DB do not exist, TCP = Triple-Core Processor?
    // AlphaStrike only (this may be incorrect, WIP)
    CRW, CR, DUN, EE, FC, FF, MTN, OVL, PARA, TSMX, RCA, RFA, HTC, TRN, SUBS, SUBW, JMPS, JMPW,
    CAP, SCAP, FUEL, MSL,
    // SBF
    AC3, COM,
    // Placeholder for STD (Standard) damage on large AS elements with firing arcs:
    STD
    ;
    
    private static final EnumMap<BattleForceSUA, BattleForceSUA> transportBayDoors = new EnumMap<>(BattleForceSUA.class);
    private static final String INCH = "\"";

    static {
        transportBayDoors.put(AT, ATxD);
        transportBayDoors.put(CT, CTxD);
        transportBayDoors.put(CK, CKxD);
        transportBayDoors.put(MT, MTxD);
        transportBayDoors.put(PT, PTxD);
        transportBayDoors.put(ST, STxD);
        transportBayDoors.put(VTM, VTMxD);
        transportBayDoors.put(VTH, VTHxD);
        transportBayDoors.put(VTS, VTSxD);
    }
    
    public boolean isTransport() {
        return isAnyOf(AT, CT, CK, MT, PT, ST, VTM, VTH, VTS);
    }
    
    public boolean isDoor() {
        return isAnyOf(ATxD, CTxD, CKxD, MTxD, PTxD, STxD, VTMxD, VTHxD, VTSxD);
    }

    /** @return The Door SUA associated with this SUA. Returns UNKNOWN when this SUA is not a transport SUA. */
    public BattleForceSUA getDoor() {
        return transportBayDoors.getOrDefault(this, UNKNOWN);
    }

    public boolean isArtillery() {
        return isAnyOf(ARTAIS, ARTAC, ARTBA, ARTCM5, ARTCM7, ARTCM9, ARTCM12, ARTT, ARTS, ARTLT, ARTTC, ARTSC, ARTLTC);
    }

    /**
     * Creates the formatted SPA string for the given spa. For turrets this includes everything in that
     * turret.
     *
     * @return The complete formatted Special Unit Ability string such as "LRM1/1/-" or "CK15D2".
     */
    public String formatAbility(AlphaStrikeElement element) {
        Object suaObject = element.getSUA(this);
        if (!isValidAbilityObject(suaObject)) {
            return "ERROR - wrong ability object";
        }
        if (this == TUR) {
            return "TUR(" + suaObject + ")";
            //TODO: make the object a further collection?
        } else if (isAnyOf(BIM, LAM)) {
            return lamString(this, (Map) suaObject);
            //TODO: Add a class for movement codes that contains Map<>?
        } else if (isAnyOf(C3BSS, C3M, C3BSM, C3EM, INARC, CNARC, SNARC)) {
            return toString() + ((int) suaObject == 1 ? "" : (int) suaObject);
        } else if (isTransport()) {
            String result = this + suaObject.toString();
            if (element.isLargeAerospace()
                    && element.hasSUA(getDoor()) && ((int) element.getSUA(getDoor()) > 0)) {
                result += getDoor().toString() + element.getSUA(getDoor());
            }
            return result;
        } else {
            return toString() + (suaObject != null ? suaObject : "");
        }
    }

    /** @return The formatted LAM/BIM Special Ability string such as LAM(36"g/4a). */
    private static String lamString(BattleForceSUA sua, Map suaObject) {
        if (sua.isAnyOf(LAM, BIM)) {
            return "ERROR";
        }
        StringBuilder result = new StringBuilder(sua.toString());
        result.append("(");
        if (sua == LAM) {
            result.append(suaObject.get("g")).append(INCH).append("g/");
        }
        return result.append(suaObject.get("a")).append("a)").toString();
    }

    public boolean isValidAbilityObject(Object abilityObject) {
        return ((abilityObject instanceof Integer) && usesIntegerObject())
                || ((abilityObject instanceof ASDamage) && usesASDamageObject())
                || ((abilityObject instanceof ASDamageVector) && usesASDamageVectorObject());
    }


    @Override
    public String toString() {
        String spaName = super.toString();
        if (isArtillery()) {
            spaName += "-";
        } else if (this == TSEMPO) {
            spaName = "TSEMP-O";
        } else if (isDoor()) {
            spaName = "-D";
        } else if (this == ITSM) {
            spaName = "I-TSM";
        }
        return spaName;
    }

    /** Returns true if this SUA is equal to any of the given SUAs. */
    public boolean isAnyOf(BattleForceSUA sua, BattleForceSUA... furtherSuas) {
        return (this == sua) || Arrays.stream(furtherSuas).anyMatch(furtherSua -> this == furtherSua);
    }

    private boolean usesIntegerObject() {
        return isAnyOf(C3BSS, C3M, C3BSM, C3EM, INARC, CNARC, SNARC);
    }

    private boolean usesASDamageObject() {
        return this == IF;
    }

    private boolean usesASDamageVectorObject() {
        return isAnyOf(SRM, LRM, FLK, REAR, IATM, AC);
    }

}