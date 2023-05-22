package megamek.utilities;

import java.util.Set;
import java.util.TreeSet;

import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.Mounted;
import megamek.common.SimpleTechLevel;
import megamek.common.WeaponType;
import megamek.common.loaders.EntityLoadingException;

/**
 * Compares computed static tech level to what is in the unit file and reports all units that have
 * equipment that exceeds the declared tech level, followed by a list of all the equipment that caused failures.
 * 
 * Note that some failures may be due to system or construction options rather than EquipmentType.
 * 
 * @author Neoancient
 *
 */

public class TechLevelCompareTool {
    
    static Set<EquipmentType> weaponSet = new TreeSet<>((e1, e2) -> e1.getName().compareTo(e2.getName()));
    static Set<EquipmentType> ammoSet = new TreeSet<>((e1, e2) -> e1.getName().compareTo(e2.getName()));
    static Set<EquipmentType> miscSet = new TreeSet<>((e1, e2) -> e1.getName().compareTo(e2.getName()));
    
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
                 SimpleTechLevel fixed = SimpleTechLevel.convertCompoundToSimple(en.getTechLevel());
                 SimpleTechLevel calc = en.getStaticTechLevel();
                if (fixed.compareTo(calc) < 0) {
                    System.out.println(en.getShortName() + ": " + fixed + "/" + calc);
                    for (Mounted m : en.getEquipment()) {
                        if (fixed.compareTo(m.getType().getStaticTechLevel()) < 0) {
                            if (m.getType() instanceof WeaponType) {
                                weaponSet.add(m.getType());
                            } else if (m.getType() instanceof AmmoType) {
                                ammoSet.add(m.getType());
                            } else {
                                miscSet.add(m.getType());
                            }
                        }
                    }
                    bad++;
                }
            } else {
                System.err.println("Could not load entity " + ms.getName());
            }
        }
        System.out.println("Weapons:");
        for (EquipmentType et : weaponSet) {
            System.out.println("\t" + et.getName() + " (" + et.getStaticTechLevel().toString() + ")");
        }                        
        System.out.println("Ammo:");
        for (EquipmentType et : ammoSet) {
            System.out.println("\t" + et.getName() + " (" + et.getStaticTechLevel().toString() + ")");
        }                        
        System.out.println("MiscType:");
        for (EquipmentType et : miscSet) {
            System.out.println("\t" + et.getName() + " (" + et.getStaticTechLevel().toString() + ")");
        }                        
        System.out.println("Failed: " + bad + "/" + msc.getAllMechs().length);
    }

}
