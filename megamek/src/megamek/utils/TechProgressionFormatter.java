/**
 * * MegaMek - Copyright (C) 2017 - The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import megamek.common.EquipmentType;
import megamek.common.MiscType;
import megamek.common.TechConstants;

/**
 * Goes through the list of EquipmentType and uses the old-style tech progression data
 * to generate the code for the new style used by TechProgression.
 * 
 * This class is intended to be temporary.
 * 
 * @author Neoancient
 *
 */
public class TechProgressionFormatter {
    
    private static final File SRC_DIR = new File("megamek/src/megamek");
    
    private static Map<String,String> miscMap = new HashMap<>();
    
    private static int getProgressionIndex(int techLevel) {
        switch(techLevel) {
        case TechConstants.T_INTRO_BOXSET:
        case TechConstants.T_IS_TW_NON_BOX:
        case TechConstants.T_IS_TW_ALL:
        case TechConstants.T_CLAN_TW:
        case TechConstants.T_TW_ALL:
        case TechConstants.T_ALL:
        case TechConstants.T_ALLOWED_ALL:
            return 2;
        case TechConstants.T_IS_ADVANCED:
        case TechConstants.T_CLAN_ADVANCED:
            return 1;
        case TechConstants.T_IS_EXPERIMENTAL:
        case TechConstants.T_CLAN_EXPERIMENTAL:
            return 0;
        }
        return 2;
    }
    
    private static String formatCode(EquipmentType eq) {
        boolean clan = false;
        boolean is = false;
        StringBuilder sb = new StringBuilder();
        Map<Integer,Integer> techYears = new HashMap<>();
        for (Map.Entry<Integer,Integer> e : eq.getTechLevels().entrySet()) {
            if (e.getKey() >= eq.getIntroductionDate()) {
                techYears.merge(getProgressionIndex(e.getValue()),
                        e.getKey(), (y1, y2) -> Math.min(y1, y2));
            }
            if (TechConstants.isClan(e.getValue())) {
                clan = true;
            } else if (TechConstants.isClan(TechConstants.getOppositeTechLevel(e.getValue()))){
                is = true;
            } else {
                clan = is = true;
            }
        }
        if (clan && is) {
            sb.append("        misc.techProgression.setTechBase(TechProgression.TECH_BASE_ALL);\n");
            sb.append("        misc.techProgression.setProgression(");
        } else if (clan) {
            sb.append("        misc.techProgression.setTechBase(TechProgression.TECH_BASE_CLAN);\n");
            sb.append("        misc.techProgression.setClanProgression(");
        } else {
            sb.append("        misc.techProgression.setTechBase(TechProgression.TECH_BASE_IS);\n");
            sb.append("        misc.techProgression.setISProgression(");
        }
        for (int i = 0; i <= 2; i++) {
            if (techYears.containsKey(i)) {
                sb.append(techYears.get(i));
            } else {
                sb.append("DATE_NONE");
            }
            if (i < 2) {
                sb.append(", ");
            }
        }
        if (eq.getExtinctionDate() != EquipmentType.DATE_NONE) {
            sb.append(", ");
            sb.append(eq.getExtinctionDate());
            if (eq.getReintruductionDate() != EquipmentType.DATE_NONE) {
                sb.append(", ");
                sb.append(eq.getReintruductionDate());
            }
        }
        sb.append(");\n");
        
        sb.append("        misc.techProgression.setTechRating(RATING_")
            .append(eq.getTechRatingName()).append(");\n");
        sb.append("        misc.techProgression.setAvailability( new int[] { ");
        for (int i = 0; i < 4; i++) {
            sb.append("RATING_").append(eq.getAvailabilityName(i));
            if (i < 3) {
                sb.append(", ");
            }
        }
        sb.append(" });\n");
        
        return sb.toString();
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        EquipmentType.initializeTypes();
        
        for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e.hasMoreElements();) {
            final EquipmentType eq = e.nextElement();
            if (eq instanceof MiscType) {
                miscMap.put(eq.getInternalName(), formatCode(eq));
            }
        }
        
        File oldFile = new File(SRC_DIR, "common/MiscType.java");
        File newFile = new File(SRC_DIR, "common/MiscTypeOld.java");
        
        if (!newFile.exists()) {
            oldFile.renameTo(newFile);
        }

        oldFile = new File(SRC_DIR, "common/MiscTypeOld.java");
        newFile = new File(SRC_DIR, "common/MiscType.java");

        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(oldFile);
            os = new FileOutputStream(newFile);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        PrintWriter pw = new PrintWriter(os);
        Pattern namePattern = Pattern.compile(".*\"(.*?)\".*");
        EquipmentType etype = null;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String line = null;
            while (null != (line = reader.readLine())) {
                if (etype == null) {
                    Matcher m = namePattern.matcher(line);
                    if (m.matches()) {
                        etype = EquipmentType.get(m.group(1));
                    }
                }
                if (line.contains("new MiscType()")) {
                    etype = null;
                } else if (line.contains("return misc")) {
                    if (etype == null) {
                        System.err.println("equipment name not found");
                    } else {
                        pw.print(miscMap.get(etype.getInternalName()));
                    }
                }
                pw.println(line);
            }
            pw.close();
            is.close();
            os.close();
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }

}
