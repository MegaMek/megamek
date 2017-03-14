/**
 * 
 */
package megamek.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import megamek.common.Entity;
import megamek.common.Mech;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.Mounted;
import megamek.common.loaders.BLKFile;
import megamek.common.loaders.EntityLoadingException;

/**
 * Temporary utility to batch-process omni units for the purpose of correctly tagging pod-mounted
 * equipment
 * 
 * @author cwspain
 *
 */
public class TemporaryOmniRework {

    /**
     * Changes all eligible equipment to pod-mounted
     * @param en
     */
    private static void setAllPod(Entity en) {
        for (Mounted m : en.getEquipment()) {
            if (m.getLocation() != Entity.LOC_NONE && !m.getType().isOmniFixedOnly()) {
                m.setOmniPodMounted(true);
            }
        }
    }
    
    /**
     * Iterates through all units in the MechSummaryCache and sets all eligible equipment on
     * omni units to pod-mounted.
     */
    private static void resetAllPod() {
        Map<String,Set<String>> allChassis = new TreeMap<>();
        for (MechSummary ms : MechSummaryCache.getInstance().getAllMechs()) {
            try {
                Entity en = new MechFileParser(ms.getSourceFile(), 
                        ms.getEntryName()).getEntity();
                if (!en.isOmni()) {
                    continue;
                }
                
                setAllPod(en);

                try {
                    if (en instanceof Mech) {
                        FileOutputStream out = new FileOutputStream(ms.getSourceFile());
                        PrintStream p = new PrintStream(out);

                        p.println(((Mech)en).getMtf());
                        p.close();
                        out.close();
                    } else {
                        BLKFile.encode(ms.getSourceFile().toString(), en);                            
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (EntityLoadingException ex) {
                ex.printStackTrace();
            }
        }
    }
    /**
     * Collects set of all omni chassis names and outputs them to chassis.txt, one per line.
     */
    private static void printChassisList() {
        Set<String> allChassis = new TreeSet<>();
        for (MechSummary ms : MechSummaryCache.getInstance().getAllMechs()) {
            try {
                Entity en = new MechFileParser(ms.getSourceFile(), 
                        ms.getEntryName()).getEntity();
                if (!en.isOmni()) {
                    continue;
                }
                allChassis.add(ms.getChassis());
            } catch (EntityLoadingException ex) {
                ex.printStackTrace();
            }
        }
        File out = new File("chassis.txt");
        try {
            FileOutputStream os = new FileOutputStream(out);
            PrintWriter pw = new PrintWriter(os);
            for (String chassis : allChassis) {
                pw.print("\t");
                pw.println(chassis);
            }
            pw.close();
            os.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        while (!MechSummaryCache.getInstance().isInitialized()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        //printChassisList();
        resetAllPod();
    }
}
