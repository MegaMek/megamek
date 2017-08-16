package megamek.utils;

import megamek.common.Entity;
import megamek.common.ITechnology;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.loaders.EntityLoadingException;

/**
 * Compares computed static tech level to what is in the unit file and reports all units that differ.
 * 
 * @author Neoancient
 *
 */

public class TechLevelCompareTool {
    
    public static void main(String[] args) {
        int bad = 0;
        MechSummaryCache msc = MechSummaryCache.getInstance();
        while (!msc.isInitialized()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        for (MechSummary ms : msc.getAllMechs()) {
            Entity en = null;
            try {
                en = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
            } catch (EntityLoadingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (null != en) {
                ITechnology.SimpleTechLevel fixed = ITechnology.convertTechConstantsToSimple(en.getTechLevel());
                ITechnology.SimpleTechLevel calc = en.getStaticTechLevel();
                if (fixed.compareTo(calc) < 0) {
                    System.out.println(en.getShortName() + ": " + fixed.toString() + "/" + calc.toString());
                    bad++;
                    en.recalculateTechAdvancement();
                }
            } else {
                System.err.println("Could not load entity " + ms.getName());
            }
        }
        System.out.println("Failed: " + bad + "/" + msc.getAllMechs().length);
    }

}
