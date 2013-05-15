/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import megamek.common.loaders.EntityLoadingException;
import megamek.common.preference.PreferenceManager;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestEntity;
import megamek.common.verifier.TestMech;
import megamek.common.verifier.TestTank;

/**
 * Cache of the Mech summary information. Implemented as Singleton so a client
 * and server running in the same process can share it
 * 
 * @author Arlith
 * @author Others...
 */
public class MechSummaryCache {

    public static interface Listener {
        void doneLoading();
    }

    static MechSummaryCache m_instance;
    private static boolean disposeInstance = false;
    private static boolean interrupted = false;

    private boolean initialized = false;
    private boolean initializing = false;
    
    private ArrayList<Listener> listeners = new ArrayList<Listener>();

    private StringBuffer loadReport = new StringBuffer();
    private final static String CONFIG_FILENAME = "data/mechfiles/UnitVerifierOptions.xml"; // should
    // be a
    // client
    // option?
    private EntityVerifier entityVerifier = null;
    private Thread loader;

    public static synchronized MechSummaryCache getInstance() {
        if (m_instance == null) {
            m_instance = new MechSummaryCache();
        }
        if (!m_instance.initialized && !m_instance.initializing) {
            m_instance.initializing = true;
            m_instance.initialized = false;
            interrupted = false;
            disposeInstance = false;
            m_instance.loader = new Thread(new Runnable() {
                public void run() {
                    m_instance.loadMechData();
                }
            }, "Mech Cache Loader");
            m_instance.loader.setPriority(Thread.NORM_PRIORITY - 1);
            m_instance.loader.start();
        }
        return m_instance;
    }

    public static void dispose() {
        if (m_instance != null) {
            synchronized (m_instance) {
                interrupted = true;
                m_instance.loader.interrupt();
                // We can't do this, otherwise we can't notifyAll() 
                //m_instance = null;
                disposeInstance = true;                
            }
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void addListener(Listener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    private MechSummary[] m_data;
    private Map<String, MechSummary> m_nameMap;
    private Map<String, MechSummary> m_fileNameMap;
    private Map<String, String> hFailedFiles;
    private int cacheCount;
    private int fileCount;
    private int zipCount;

    private static final File ROOT = new File(PreferenceManager.getClientPreferences().getMechDirectory());
    private static final File CACHE = new File(ROOT, "units.cache");

    private MechSummaryCache() {
        m_nameMap = new HashMap<String, MechSummary>();
        m_fileNameMap = new HashMap<String, MechSummary>();
    }

    public MechSummary[] getAllMechs() {
        block();
        return m_data;
    }

    private void block() {
        if (!initialized) {
            synchronized (m_instance) {
                try {
                    m_instance.wait();
                } catch (Exception e) {

                }
            }
        }
    }

    public MechSummary getMech(String sRef) {
        block();
        if (m_nameMap.containsKey(sRef)) {
            return m_nameMap.get(sRef);
        }
        return m_fileNameMap.get(sRef);
    }

    public Map<String, String> getFailedFiles() {
        block();
        return hFailedFiles;
    }

    public void loadMechData() {
        Vector<MechSummary> vMechs = new Vector<MechSummary>();
        Set<String> sKnownFiles = new HashSet<String>();
        long lLastCheck = 0;
        entityVerifier = new EntityVerifier(new File(CONFIG_FILENAME));
        hFailedFiles = new HashMap<String, String>();

        EquipmentType.initializeTypes(); // load master equipment lists

        loadReport.append("\n");
        loadReport.append("Reading unit files:\n");

        // check the cache
        try {
            if (CACHE.exists() && (CACHE.lastModified() >= megamek.MegaMek.TIMESTAMP)) {
                loadReport.append("  Reading from unit cache file...\n");
                lLastCheck = CACHE.lastModified();
                InputStream istream = new BufferedInputStream(new FileInputStream(CACHE));
                ObjectInputStream fin = new ObjectInputStream(istream);
                Integer num_units = (Integer)fin.readObject();
                for (int i = 0; i < num_units; i++){
                    if (interrupted) {
                        done();
                        return;
                    }                    
                    MechSummary ms = (MechSummary)fin.readObject();
                    // Verify that this file still exists and is older than
                    // the cache.
                    File fSource = ms.getSourceFile();
                    if (fSource.exists()) {
                        vMechs.addElement(ms);
                        if(null == ms.getEntryName()) {
                            sKnownFiles.add(fSource.toString());
                        } else {
                            sKnownFiles.add(ms.getEntryName());
                        }                        
                        cacheCount++;
                    }                 
                }
            }
        } catch (Exception e) {
            loadReport.append("  Unable to load unit cache: ").append(e.getMessage()).append("\n");
            e.printStackTrace();
        }

        // load any changes since the last check time
        boolean bNeedsUpdate = loadMechsFromDirectory(vMechs, sKnownFiles, lLastCheck, ROOT);

        // convert to array
        m_data = new MechSummary[vMechs.size()];
        vMechs.copyInto(m_data);

        // store map references
        for (MechSummary element : m_data) {
            if (interrupted) {
                done();
                return;
            } 
            m_nameMap.put(element.getName(), element);
            String entryName = element.getEntryName();
            if (entryName == null) {
                m_fileNameMap.put(element.getSourceFile().getName(), element);
            } else {
                String unitName = entryName;

                if (unitName.indexOf("\\") > -1) {
                    unitName = unitName.substring(unitName.lastIndexOf("\\") + 1);
                }

                if (unitName.indexOf("/") > -1) {
                    unitName = unitName.substring(unitName.lastIndexOf("/") + 1);
                }

                m_fileNameMap.put(unitName, element);
            }
        }

        // save updated cache back to disk
        if (bNeedsUpdate) {
            try {
                saveCache();
            } catch (Exception e) {
                loadReport.append("  Unable to save mech cache\n");
            }
        }

        loadReport.append(m_data.length).append(" units loaded.\n");

        if (hFailedFiles.size() > 0) {
            loadReport.append("  ").append(hFailedFiles.size()).append(" units failed to load...\n");
        }
        /*
         * Enumeration failedUnits = hFailedFiles.keys(); Enumeration
         * failedUnitsDesc = hFailedFiles.elements(); while
         * (failedUnits.hasMoreElements()) { loadReport.append("
         * ").append(failedUnits.nextElement()) .append("\n");
         * loadReport.append(" --")
         * .append(failedUnitsDesc.nextElement()).append("\n"); }
         */

        System.out.print(loadReport.toString());

        done();
    }

    private synchronized void done() {
        if (m_instance != null){
            m_instance.notifyAll();
        }
        
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).doneLoading();
        }
        
        if (disposeInstance){                       
            m_instance = null;
            initialized = false;
        }
        else{
            initialized = true; 
        }                
    }

    private void saveCache() throws Exception {
        loadReport.append("Saving unit cache.\n");
        ObjectOutputStream wr = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(CACHE)));
        Integer length = new Integer(m_data.length);
        wr.writeObject(length);
        for (MechSummary element : m_data) {
            wr.writeObject(element);
        }
        wr.flush();
        wr.close();
    }

    private MechSummary getSummary(Entity e, File f, String entry) {
        MechSummary ms = new MechSummary();
        ms.setName(e.getShortNameRaw());
        ms.setChassis(e.getChassis());
        ms.setModel(e.getModel());
        ms.setUnitType(MechSummary.determineUnitType(e));
        ms.setSourceFile(f);
        ms.setEntryName(entry);
        ms.setYear(e.getYear());
        ms.setType(e.getTechLevel());
        if(e instanceof BattleArmor) {
            ms.setTons(((BattleArmor) e).getTrooperWeight());
        } else {
            ms.setTons(e.getWeight());
        }
        ms.setBV(e.calculateBattleValue());
        ms.setLevel(TechConstants.T_SIMPLE_LEVEL[e.getTechLevel()]);
        ms.setCost((long) e.getCost(false));
        ms.setUnloadedCost(((long) e.getCost(true)));
        ms.setAlternateCost((int) e.getAlternateCost());
        ms.setCanon(e.isCanon());
        ms.setWalkMp(e.getWalkMP(false, false));
        ms.setRunMp(e.getRunMP(false, false, false));
        ms.setJumpMp(e.getJumpMP(false));
        ms.setClan(e.isClan());
        if (e instanceof Mech) {
            if (((Mech) e).isIndustrial()) {
                ms.setUnitSubType("Industrial");
            } else if (e.isOmni()) {
                ms.setUnitSubType("Omni");
            } else {
                ms.setUnitSubType("BattleMech");
            }
        } else {
            ms.setUnitSubType(e.getMovementModeAsString());
        }
        // we can only test meks and vehicles right now
        if ((e instanceof Mech) || ((e instanceof Tank) && !(e instanceof GunEmplacement))) {
            TestEntity testEntity = null;
            if (e instanceof Mech) {
                testEntity = new TestMech((Mech) e, entityVerifier.mechOption, null);
            } else {
                testEntity = new TestTank((Tank) e, entityVerifier.tankOption, null);
            }
            if (!testEntity.correctEntity(new StringBuffer())) {
                ms.setLevel("F");
            }
        }
        return ms;
    }

    /**
     *  Loading a complete mech object for each summary is a bear and should be
     *  changed, but it lets me use the existing parsers
     * @param vMechs
     * @param sKnownFiles
     * @param lLastCheck
     * @param fDir
     * @return
     */
    private boolean loadMechsFromDirectory(Vector<MechSummary> vMechs, Set<String> sKnownFiles, long lLastCheck, File fDir) {
        boolean bNeedsUpdate = false;
        loadReport.append("  Looking in ").append(fDir.getPath()).append("...\n");
        int thisDirectoriesFileCount = 0;
        String[] sa = fDir.list();

        if (sa != null) {
            for (String element : sa) {
                if (interrupted) {
                    done();
                    return false;
                }
                File f = new File(fDir, element);
                if (f.equals(CACHE)) {
                    continue;
                }
                if (f.isDirectory()) {
                    if (f.getName().toLowerCase().equals("unsupported")) {
                        // Mechs in this directory are ignored because
                        // they have features not implemented in MM yet.
                        continue;
                    } else if (f.getName().toLowerCase().equals("_svn") || f.getName().toLowerCase().equals(".svn")) {
                        // This is a Subversion work directory. Lets ignore it.
                        continue;
                    }
                    // recursion is fun
                    bNeedsUpdate |= loadMechsFromDirectory(vMechs, sKnownFiles, lLastCheck, f);
                    continue;
                }
                if (f.getName().indexOf('.') == -1) {
                    continue;
                }
                if (f.getName().toLowerCase().endsWith(".txt")) {
                    continue;
                }
                if (f.getName().toLowerCase().endsWith(".log")) {
                    continue;
                }
                if (f.getName().toLowerCase().endsWith(".svn-base")) {
                    continue;
                }
                if (f.getName().toLowerCase().endsWith(".svn-work")) {
                    continue;
                }
                if (f.getName().toLowerCase().endsWith(".ds_store")) {
                    continue;
                }
                if (f.getName().equals("UnitVerifierOptions.xml")) {
                    continue;
                }
                if (f.getName().toLowerCase().endsWith(".zip")) {
                    bNeedsUpdate |= loadMechsFromZipFile(vMechs, sKnownFiles, lLastCheck, f);
                    continue;
                }
                if ((f.lastModified() < lLastCheck) && sKnownFiles.contains(f.toString())) {
                    continue;
                }
                try {
                    MechFileParser mfp = new MechFileParser(f);
                    Entity e = mfp.getEntity();
                    MechSummary ms = getSummary(e, f, null);
                    vMechs.addElement(ms);
                    sKnownFiles.add(f.toString());
                    bNeedsUpdate = true;
                    thisDirectoriesFileCount++;
                    fileCount++;
                    Iterator<String> failedEquipment = e.getFailedEquipment();
                    if (failedEquipment.hasNext()) {
                        loadReport.append("    Loading from ").append(f).append("\n");
                        while (failedEquipment.hasNext()) {
                            loadReport.append("      Failed to load equipment: ").append(failedEquipment.next()).append("\n");
                        }
                    }
                } catch (EntityLoadingException ex) {
                    loadReport.append("    Loading from ").append(f).append("\n");
                    loadReport.append("***   Unable to load file: ");
                    StringWriter stringWriter = new StringWriter();
                    PrintWriter printWriter = new PrintWriter(stringWriter);
                    ex.printStackTrace(printWriter);
                    loadReport.append(stringWriter.getBuffer()).append("\n");
                    hFailedFiles.put(f.toString(), ex.getMessage());
                    continue;
                }
            }
        }

        loadReport.append("  ...loaded ").append(thisDirectoriesFileCount).append(" files.\n");

        return bNeedsUpdate;
    }

    private boolean loadMechsFromZipFile(Vector<MechSummary> vMechs, Set<String> sKnownFiles, long lLastCheck, File fZipFile) {
        boolean bNeedsUpdate = false;
        ZipFile zFile;
        int thisZipFileCount = 0;
        try {
            zFile = new ZipFile(fZipFile);
        } catch (Exception ex) {
            loadReport.append("  Unable to load file ").append(fZipFile.getName()).append(": ");
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            ex.printStackTrace(printWriter);
            loadReport.append(stringWriter.getBuffer()).append("\n");
            return false;
        }
        loadReport.append("  Looking in zip file ").append(fZipFile.getPath()).append("...\n");
        
        for (Enumeration<?> i = zFile.entries(); i.hasMoreElements();) {
            if (interrupted) {
                done();
                return false;
            }
            ZipEntry zEntry = (ZipEntry) i.nextElement();

            if (zEntry.isDirectory()) {
                if (zEntry.getName().toLowerCase().equals("unsupported")) {
                    loadReport.append("  Do not place special 'unsupported' type folders in zip files, they must\n    be uncompressed directories to work properly.  Note that you may place\n    zip files inside of 'unsupported' type folders, though.\n");
                }
                continue;
            }
            if (zEntry.getName().toLowerCase().endsWith(".txt")) {
                continue;
            }
            if ((Math.max(fZipFile.lastModified(), zEntry.getTime()) < lLastCheck) && sKnownFiles.contains(zEntry.getName())) {
                continue;
            }

            try {
                MechFileParser mfp = new MechFileParser(zFile.getInputStream(zEntry), zEntry.getName());
                Entity e = mfp.getEntity();
                MechSummary ms = getSummary(e, fZipFile, zEntry.getName());
                vMechs.addElement(ms);
                sKnownFiles.add(zEntry.getName());
                bNeedsUpdate = true;
                thisZipFileCount++;
                zipCount++;
                Iterator<String> failedEquipment = e.getFailedEquipment();
                if (failedEquipment.hasNext()) {
                    loadReport.append("    Loading from zip file").append(" >> ").append(zEntry.getName()).append("\n");
                    while (failedEquipment.hasNext()) {
                        loadReport.append("      Failed to load equipment: ").append(failedEquipment.next()).append("\n");
                    }
                }
            } catch (Exception ex) {
                loadReport.append("    Loading from zip file").append(" >> ").append(zEntry.getName()).append("\n");
                loadReport.append("      Unable to load file: ");
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                ex.printStackTrace(printWriter);
                loadReport.append(stringWriter.getBuffer()).append("\n");
                if (!(ex.getMessage() == null)) {
                    hFailedFiles.put(zEntry.getName(), ex.getMessage());
                }
                continue;
            }
        }

        try {
            zFile.close();
        } catch (Exception ex) {
            // whatever.
        }

        loadReport.append("  ...loaded ").append(thisZipFileCount).append(" files.\n");

        return bNeedsUpdate;
    }

    public int getCacheCount() {
        return cacheCount;
    }

    public int getFileCount() {
        return fileCount;
    }

    public int getZipCount() {
        return zipCount;
    }
    
}
