package megamek.utils;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;

import megamek.common.*;
import megamek.common.loaders.EntityLoadingException;

public class AlphaStrikeMassConvert {
    
    private static final ASUnitType typeFilter = ASUnitType.BM;  

    public static void main(String[] args) throws EntityLoadingException {
        System.out.println("Starting AlphaStrike conversion for the unit type " + typeFilter);
        StringBuilder table = new StringBuilder(clipboardHeaderString());
        MechSummary[] units = MechSummaryCache.getInstance().getAllMechs();
        for (MechSummary unit : units) {
            Entity entity = new MechFileParser(unit.getSourceFile(), unit.getEntryName()).getEntity();
            if (entity instanceof SuperHeavyTank) {
                System.out.println(entity.getShortName());
            }
            continue;
//            if (!AlphaStrikeConverter.canConvert(entity)) {
//                continue;
//            }
//            if ((entity instanceof Aero && entity.isFighter()) || entity instanceof Mech || entity instanceof Tank) {
//                System.out.println(entity.getShortName());
//                AlphaStrikeElement ase = AlphaStrikeConverter.convertToAlphaStrike(entity);
////                if (ase.getUnitType() == typeFilter) {
//                    table.append(clipboardElementString(ase));
////                }
//            }
        }
        StringSelection stringSelection = new StringSelection(table.toString());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
        System.out.println("Finished.");
        System.exit(0);

    }
    
    private static String clipboardHeaderString() {
        List<String> headers = new ArrayList<>();
        headers.add("Chassis");
        headers.add("Model");
        headers.add("Role");
        headers.add("Type");
        headers.add("SZ");
        headers.add("MV");
        headers.add("Arm");
        headers.add("Str");
        headers.add("Thr");
        headers.add("Dmg S/M/L");
        headers.add("OV");
        headers.add("PV");
        headers.add("Specials");
        headers.add("\n");
        return String.join("\t", headers);
    }

    /** Returns a String representing the entities to export to the clipboard. */
    private static StringBuilder clipboardElementString(AlphaStrikeElement element) {
        List<String> stats = new ArrayList<>();
        stats.add(element.getChassis());
        stats.add(element.getModel());
        stats.add(element.getRole().toString());
        stats.add(element.getUnitType().toString());
        stats.add(element.getSize() + "");
        stats.add(element.getMovementAsString());
        stats.add(element.getFinalArmor() + "");
        stats.add(element.getStructure() + "");
        stats.add(element.usesThreshold() ? element.getFinalThreshold() + "" : " ");
        if (element.usesSML()) {
            stats.add(element.getStandardDamage() + "");
        } else if (element.usesSMLE()) {
            stats.add(element.getStandardDamage() + "");
        }
        stats.add(element.getOverheat() + "");
        stats.add(element.getFinalPoints()+"");
        stats.add(element.getSpecialsString());
        stats.add("\n");
        return new StringBuilder(String.join("\t", stats));
    }

}
