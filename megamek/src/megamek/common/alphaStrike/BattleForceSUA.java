/*
 * Copyright (c) 2022, 2024 - The MegaMek Team. All Rights Reserved.
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
import java.util.Comparator;
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
    // TODO : PL, DB do not exist, TCP = Triple-Core Processor?
    // AlphaStrike only (this may be incorrect, WIP)
    CRW, CR, DUN, EE, FC, FF, MTN, OVL, PAR, TSMX, RCA, RFA, HTC, TRN, SUBS, SUBW, JMPS, JMPW,
    CAP, SCAP, FUEL, MSL,
    // SBF
    AC3, COM, SBF_OMNI,
    // Placeholder for STD (Standard) damage on large AS elements with firing arcs:
    STD,
    // Placeholders for additional unit info not otherwise present in the AS element
    TRI, QUAD, AERODYNESC
    ;
    
    private static final EnumMap<BattleForceSUA, BattleForceSUA> transportBayDoors = new EnumMap<>(BattleForceSUA.class);
    private static final String[] sortedNames = new String[values().length];

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
        for (int i = 0; i < values().length; i++) {
            sortedNames[i] = values()[i].name();
        }
        Arrays.sort(sortedNames, Comparator.comparing(s -> -s.length()));
    }

    /** @return True when this SUA is an ability that may be associated with a Door value (not IT and DT!). */
    public boolean isTransport() {
        return isAnyOf(AT, CT, CK, MT, PT, ST, VTM, VTH, VTS);
    }

    /** @return True when this SUA is a door ability for a transport ability. */
    public boolean isDoor() {
        return isAnyOf(ATxD, CTxD, CKxD, MTxD, PTxD, STxD, VTMxD, VTHxD, VTSxD);
    }

    /** @return The Door SUA associated with this SUA. Returns UNKNOWN when this SUA is not a transport SUA. */
    public BattleForceSUA getDoor() {
        return transportBayDoors.getOrDefault(this, UNKNOWN);
    }

    /** @return True when this SUA is an artillery SUA such as ARTAIS. */
    public boolean isArtillery() {
        return isAnyOf(ARTAIS, ARTAC, ARTBA, ARTCM5, ARTCM7, ARTCM9, ARTCM12, ARTT, ARTS, ARTLT, ARTTC, ARTSC, ARTLTC);
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
        } else if (this == SBF_OMNI) {
            spaName = "OMNI";
        }
        return spaName;
    }

    /** Returns true if this SUA is equal to any of the given SUAs. */
    public boolean isAnyOf(BattleForceSUA sua, BattleForceSUA... furtherSuas) {
        return (this == sua) || Arrays.stream(furtherSuas).anyMatch(furtherSua -> this == furtherSua);
    }

    /** @return True when this SUA uses an Integer as its value. */
    public boolean usesIntegerObject() {
        return isAnyOf(C3BSS, C3M, C3BSM, C3EM, INARC, CNARC, SNARC, RSD, MHQ, DCC, MASH, TSEMP, TSEMPO,
                CAR, MDS, BOMB, FUEL, PNT, CRW, SCR, DT, BTAS, MTAS, JMPW, JMPS, SUBW, SUBS, SBF_OMNI)
                || isArtillery();
    }

    /** @return True when this SUA uses an Integer or Double value (the transport SUAs). */
    public boolean usesDoubleOrIntegerObject() {
        return isTransport() || this == IT;
    }

    /** @return True when this SUA uses an ASDamage as its value (only IF). */
    public boolean usesASDamageObject() {
        return this == IF;
    }

    /** @return True when this SUA uses an ASDamageVector as its value, e.g. FLK. */
    public boolean usesASDamageVectorObject() {
        return isAnyOf(SRM, LRM, FLK, REAR, IATM, AC, HT, TOR, STD, MSL, CAP, SCAP);
    }

    /** @return True when this SUA uses a Map as its value (LAM and BIM). */
    public boolean usesMapObject() {
        return isAnyOf(LAM, BIM);
    }

    /** @return True when this SUA is not accompanied by a value, e.g. TAG. */
    public boolean usesNoObject() {
        return !usesASDamageVectorObject() && !usesASDamageObject() && !usesIntegerObject()
                && !usesDoubleOrIntegerObject() && !(this == TUR) && !usesMapObject();
    }

    /** @return True when the given abilityObject is a valid value for this SUA. E.g. an ASDamageVector is valid for LRM. */
    public boolean isValidAbilityObject(Object abilityObject) {
        return (((abilityObject instanceof ASDamage) && usesASDamageObject())
                || ((abilityObject instanceof ASDamageVector) && usesASDamageVectorObject())
                || (((abilityObject instanceof Double) || (abilityObject instanceof Integer)) && usesDoubleOrIntegerObject())
                || (abilityObject instanceof Integer) && usesIntegerObject())
                || (this == TUR && abilityObject instanceof ASSpecialAbilityCollection)
                || ((abilityObject == null) && usesNoObject())
                || ((abilityObject instanceof Map) && usesMapObject());
    }

    /**
     * Tries to parse the given text to the appropriate SUA. The text may include a number or other info
     * belonging to the SUA like "IF2" or "SRM2/2". A "TUR(...)" ability will return TUR. The number or
     * other info is not checked for validity. Returns UNKNOWN if the text cannot be parsed.
     *
     * @param asText The text to translate to a BattleForceSUA
     * @return The BattleForceSUA represented by the given text or UNKNOWN
     */
    public static BattleForceSUA parse(String asText) {
        if (asText != null) {
            final String upperCaseText = asText.toUpperCase();
            String match = Arrays.stream(sortedNames).filter(upperCaseText::startsWith).findAny().orElse("UNKNOWN");
            return valueOf(match);
        } else {
            return UNKNOWN;
        }
    }
}