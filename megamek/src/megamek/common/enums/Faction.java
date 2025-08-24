/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.enums;

import java.util.HashMap;
import java.util.Map;

// --- Faction Enum ---
public enum Faction {
    NONE(-1, FactionAffiliation.NONE, "None", "None"),
    IS(0, FactionAffiliation.IS, "IS", "IS"),
    CC(1, FactionAffiliation.IS, "CC", "CC"),
    CF(2, FactionAffiliation.IS, "CF", "CIR"),
    CP(3, FactionAffiliation.IS, "CP", "CDP"),
    CS(4, FactionAffiliation.IS, "CS", "CS"),
    DC(5, FactionAffiliation.IS, "DC", "DC"),
    EI(6, FactionAffiliation.IS, "EI", "CEI"),
    FC(7, FactionAffiliation.IS, "FC", "FC"),
    FR(8, FactionAffiliation.IS, "FR", "FRR"),
    FS(9, FactionAffiliation.IS, "FS", "FS"),
    FW(10, FactionAffiliation.IS, "FW", "FWL"),
    LC(11, FactionAffiliation.IS, "LC", "LA"),
    MC(12, FactionAffiliation.IS, "MC", "MOC"),
    MH(13, FactionAffiliation.IS, "MH", "MH"),
    OA(14, FactionAffiliation.IS, "OA", "OA"),
    TA(15, FactionAffiliation.IS, "TA", "TA"),
    TC(16, FactionAffiliation.IS, "TC", "TC"),
    TH(17, FactionAffiliation.IS, "TH", "TH"),
    RD(18, FactionAffiliation.IS, "RD", "RD"),
    RS(19, FactionAffiliation.IS, "RS", "ROS"),
    RA(20, FactionAffiliation.IS, "RA", "RA"),
    RW(21, FactionAffiliation.IS, "RW", "RWR"),
    WB(22, FactionAffiliation.IS, "WB", "WOB"),
    MERC(23, FactionAffiliation.IS, "Merc", "MERC"),
    PER(24, FactionAffiliation.IS, "Per", "Periphery"),
    CLAN(25, FactionAffiliation.CLAN, "Clan", "CLAN"),
    CBR(26, FactionAffiliation.CLAN, "CBR", "CB"),
    CBS(27, FactionAffiliation.CLAN, "CBS", "CBS"),
    CCY(28, FactionAffiliation.CLAN, "CCY", "CCO"),
    CCC(29, FactionAffiliation.CLAN, "CCC", "CCC"),
    CFM(30, FactionAffiliation.CLAN, "CFM", "CFM"),
    CGB(31, FactionAffiliation.CLAN, "CGB", "CGB"),
    CGS(32, FactionAffiliation.CLAN, "CGS", "CGS"),
    CHH(33, FactionAffiliation.CLAN, "CHH", "CHH"),
    CIH(34, FactionAffiliation.CLAN, "CIH", "CIH"),
    CJF(35, FactionAffiliation.CLAN, "CJF", "CJF"),
    CMN(36, FactionAffiliation.CLAN, "CMN", "CMG"),
    CNC(37, FactionAffiliation.CLAN, "CNC", "CNC"),
    CSF(38, FactionAffiliation.CLAN, "CSF", "CDS"),
    CSJ(39, FactionAffiliation.CLAN, "CSJ", "CSJ"),
    CSR(40, FactionAffiliation.CLAN, "CSR", "CSR"),
    CSV(41, FactionAffiliation.CLAN, "CSV", "CSV"),
    CSA(42, FactionAffiliation.CLAN, "CSA", "CSA"),
    CWM(43, FactionAffiliation.CLAN, "CWM", "CWI"),
    CWF(44, FactionAffiliation.CLAN, "CWF", "CW"),
    CWX(45, FactionAffiliation.CLAN, "CWX", "CWIE"),
    CWV(46, FactionAffiliation.CLAN, "CWV", "CWOV");

    private final int index;
    private final FactionAffiliation affiliation;
    private final String codeMM;
    private final String codeIO;
    private static final Map<Integer, Faction> INDEX_LOOKUP = new HashMap<>();
    private static final Map<String, Faction> IO_ABBR_LOOKUP = new HashMap<>();
    private static final Map<String, Faction> MM_ABBR_LOOKUP = new HashMap<>();

    static {
        for (Faction f : values()) {
            INDEX_LOOKUP.put(f.index, f);
            MM_ABBR_LOOKUP.put(f.codeMM, f);
            IO_ABBR_LOOKUP.put(f.codeIO, f);
        }
    }

    Faction(int idx, FactionAffiliation affiliation, String codeMM, String codeIO) {
        this.index = idx;
        this.affiliation = affiliation;
        this.codeMM = codeMM;
        this.codeIO = codeIO;
    }

    public int getIndex() {return index;}

    public FactionAffiliation getAffiliation() {return affiliation;}

    public String getCode() {return codeMM;}

    public String getCodeMM() {return codeMM;}

    public String getCodeIO() {return codeIO;}

    public boolean isClan() {
        return this.affiliation == FactionAffiliation.CLAN;
    }

    public static Faction fromIndex(int idx) {
        Faction f = INDEX_LOOKUP.get(idx);
        if (f == null) {throw new IllegalArgumentException("Invalid Faction index: " + idx);}
        return f;
    }

    public static Faction fromMMAbbr(String abbr) {
        // These abbreviations may have sub-faction dot codes; strip them.
        String baseAbbr = abbr.split("\\.")[0];
        return MM_ABBR_LOOKUP.getOrDefault(baseAbbr, NONE);
    }

    public static Faction fromIOAbbr(String abbr) {
        String baseAbbr = abbr.split("\\.")[0];
        return IO_ABBR_LOOKUP.getOrDefault(baseAbbr, NONE);
    }

    public static Faction fromAbbr(String abbr) {
        // This is a generic method to handle both MM and IO abbreviations.
        Faction faction = fromMMAbbr(abbr);
        if (faction == NONE) {
            faction = fromIOAbbr(abbr);
        }
        return faction;
    }
}
