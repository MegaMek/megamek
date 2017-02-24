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
import java.io.IOException;
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

import megamek.common.AmmoType;
import megamek.common.BombType;
import megamek.common.EquipmentType;
import megamek.common.MiscType;
import megamek.common.TechConstants;
import megamek.common.WeaponType;

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
    
    private static final File SRC_DIR = new File("megamek/src");
    
    private static Map<String,String> miscMap = new HashMap<>();
    private static Map<String,String> weaponMap = new HashMap<>();
    private static Map<String,String> ammoMap = new HashMap<>();
    private static Map<String,String> bombMap = new HashMap<>();
    
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
    
    private static String formatCode(EquipmentType eq, String prefix) {
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
            sb.append("        " + prefix + "techProgression.setTechBase(TechProgression.TECH_BASE_ALL);\n");
            sb.append("        " + prefix + "techProgression.setProgression(");
        } else if (clan) {
            sb.append("        " + prefix + "techProgression.setTechBase(TechProgression.TECH_BASE_CLAN);\n");
            sb.append("        " + prefix + "techProgression.setClanProgression(");
        } else {
            sb.append("        " + prefix + "techProgression.setTechBase(TechProgression.TECH_BASE_IS);\n");
            sb.append("        " + prefix + "techProgression.setISProgression(");
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
        
        sb.append("        " + prefix + "techProgression.setTechRating(RATING_")
            .append(eq.getTechRatingName()).append(");\n");
        sb.append("        " + prefix + "techProgression.setAvailability( new int[] { ");
        for (int i = 0; i < 4; i++) {
            sb.append("RATING_").append(eq.getAvailabilityName(i));
            if (i < 3) {
                sb.append(", ");
            }
        }
        sb.append(" });\n");
        
        return sb.toString();
    }
    
    public static String formatMunitionMutator(String name, int weight, Map<Integer,Integer> techLevel,
            int introDate, int extinctDate, int reintroDate, int techRating, int[] availRating,
            String rulesRef) {
        StringBuilder sb = new StringBuilder();
        boolean isClan = TechConstants.isClan(techLevel.get(introDate));
        Map<Integer,Integer> techYears = new HashMap<>();
        for (Map.Entry<Integer,Integer> e : techLevel.entrySet()) {
            if (e.getKey() >= introDate) {
                techYears.merge(getProgressionIndex(e.getValue()),
                        e.getKey(), (y1, y2) -> Math.min(y1, y2));
            }
        }
        sb.append("                munitions.add(new MunitionMutator(\"")
            .append(name).append("\", ").append(weight).append(", M_")
            .append(name.toUpperCase().replaceAll("\\-", "_"))
            .append(",\n");
        if (isClan) {
            sb.append("                                TechProgression.TECH_BASE_CLAN, ");
        } else {
            sb.append("                                TechProgression.TECH_BASE_IS, ");
        }
        sb.append("new int[] { ");
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
        if (extinctDate > 0) {
            sb.append(", ");
            sb.append(extinctDate);
            if (reintroDate > 0) {
                sb.append(", ");
                sb.append(reintroDate);
            }
        }
        sb.append(" },\n");
        sb.append("                                RATING_");
        sb.append(EquipmentType.getRatingName(techRating));
        sb.append(", new int[] { ");
        for (int i = 0; i < availRating.length; i++) {
            sb.append("RATING_");
            sb.append(EquipmentType.getRatingName(availRating[i]));
            if (i < availRating.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(" }, \"");
        sb.append(rulesRef);
        sb.append("\"));\n");
        
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
                miscMap.put(eq.getInternalName(), formatCode(eq, "misc."));
            } else if (eq instanceof WeaponType) {
                weaponMap.put(eq.getInternalName(), formatCode(eq, ""));
            } else if (eq instanceof BombType) {
                bombMap.put(eq.getInternalName(), formatCode(eq, "bomb."));
            } else if (eq instanceof AmmoType) {
                ammoMap.put(eq.getInternalName(), formatCode(eq, "ammo."));
            }
        }
        
//        updateMiscType();
//        updateWeaponType();
//        updateAmmoType();
        printBombConversion();
    }

    @SuppressWarnings("unused")
    private static void updateMiscType() {
        File oldFile = new File(SRC_DIR, "megamek/common/MiscType.java");
        File newFile = new File(SRC_DIR, "megamek/common/MiscType.java.old");
        
        if (!newFile.exists()) {
            oldFile.renameTo(newFile);
        }

        oldFile = new File(SRC_DIR, "megamek/common/MiscType.java.old");
        newFile = new File(SRC_DIR, "megamek/common/MiscType.java");

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
    
    @SuppressWarnings("unused")
    private static void updateWeaponType() {
        for (String key : weaponMap.keySet()) {
            EquipmentType wtype = EquipmentType.get(key);
            String fName = wtype.getClass().getName().replaceAll("\\.", "/") + ".java";
            File f = new File(SRC_DIR, fName);
            File backup = new File(SRC_DIR, fName + ".old");
            if (!backup.exists()) {
                f.renameTo(backup);
            }
            
            f = new File(SRC_DIR, fName);
            backup = new File(SRC_DIR, fName + ".old");
            
            InputStream is = null;
            OutputStream os = null;
            try {
                is = new FileInputStream(backup);
                os = new FileOutputStream(f);
            } catch (IOException ex) {
                
            }
            PrintWriter pw = new PrintWriter(os);
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
                String line = null;
                boolean finished = false;
                /* Find constructor */
                do {
                    line = reader.readLine();
                    if (line != null) {
                        pw.println(line);
                        if (line.startsWith("import megamek.common.TechConstants;")) {
                            pw.println("import megamek.common.TechProgression;");
                        }
                    }
                } while (line != null && !line.matches(".*public\\s+" + wtype.getClass().getSimpleName() + ".*"));
                
                while (null != (line = reader.readLine())) {
                    if (!finished && line.matches("\\s+\\}\\s*$")) {
                        pw.print(weaponMap.get(key));
                        finished = true;
                    }
                    pw.println(line);
                }
                pw.close();
                is.close();
                os.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }            
        }
    }

    @SuppressWarnings("unused")
    private static void updateAmmoType() {
        File oldFile = new File(SRC_DIR, "megamek/common/AmmoType.java");
        File newFile = new File(SRC_DIR, "megamek/common/AmmoType.java.old");
        
        if (!newFile.exists()) {
            oldFile.renameTo(newFile);
        }

        oldFile = new File(SRC_DIR, "megamek/common/AmmoType.java.old");
        newFile = new File(SRC_DIR, "megamek/common/AmmoType.java");

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
                        if (!(etype instanceof AmmoType)) {
                            etype = null;
                        }
                    }
                }
                if (line.contains("new AmmoType()")) {
                    etype = null;
                } else if (line.contains("return ammo;")) {
                    if (etype == null) {
                        System.err.println("equipment name not found");
                    } else {
                        pw.print(ammoMap.get(etype.getInternalName()));
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
    
    private static void printBombConversion() {
        //Bomb internal names are set by constants, making file processing difficult
        for (String bomb : bombMap.keySet()) {
            System.out.println(bomb);
            System.out.println(bombMap.get(bomb));
        }
    }
}
