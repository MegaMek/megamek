package megamek.common;

import java.io.*;
import java.util.zip.*;
import com.sun.java.util.collections.*;
import java.awt.*;

/*
 * Setting this up as static so a client and server running in the same
 * process can share it
 */

public class MechSummaryCache
{
    private static MechSummaryCache m_instance = null;
    
    public static synchronized MechSummaryCache getInstance()
    {
        return getInstance(null);
    }

    public static synchronized MechSummaryCache getInstance(Dialog wD)
    {
        if (m_instance == null) {
            m_instance = new MechSummaryCache(wD);
        }
        return m_instance;
    }
    
    private MechSummary[] m_data;
    private Map m_nameMap;
    private static final char SEPARATOR = '|';
    private static final File ROOT = new File(Settings.mechDirectory);
    private static final File CACHE = new File(ROOT, "mechcache.txt");

    private Dialog waitDialog;
    private int loadedFromCacheCount = 0;
    private int loadedFromFilesCount = 0;
    private int loadedFromZipsCount = 0;
    
    private MechSummaryCache(Dialog wD)
    {
        if (wD != null) {
            this.waitDialog = wD;
        }
        m_nameMap = new Hashtable();
        loadMechData();
    }
    
    public MechSummary[] getAllMechs() { return m_data; }
    
    public MechSummary getMech(String sRef)
    {
        return (MechSummary)m_nameMap.get(sRef);
    }
    
    private void loadMechData()
    {
        Vector vMechs = new Vector();
        Set sKnownFiles = new HashSet();
        long lLastCheck = 0;

        System.out.println("");
        System.out.println("Reading mech files:");

        // check the cache
        try {
            if (CACHE.exists() && CACHE.lastModified() >= megamek.MegaMek.TIMESTAMP) {
                System.out.println("Reading from mechcache file");
                lLastCheck = CACHE.lastModified();
                BufferedReader br = new BufferedReader(new FileReader(CACHE));
                String s;
                while ((s = br.readLine()) != null) {
                    MechSummary ms = new MechSummary();
                    // manually do a string tokenizer.  Much faster
                    int nIndex1 = s.indexOf(SEPARATOR);
                    ms.setName(s.substring(0, nIndex1));
                    int nIndex2 = s.indexOf(SEPARATOR, nIndex1 + 1);
                    ms.setChassis(s.substring(nIndex1 + 1, nIndex2));
                    nIndex1 = nIndex2;
                    nIndex2 = s.indexOf(SEPARATOR, nIndex1 + 1);
                    ms.setModel(s.substring(nIndex1 + 1, nIndex2));
                    nIndex1 = nIndex2;
                    nIndex2 = s.indexOf(SEPARATOR, nIndex1 + 1);
                    ms.setUnitType(s.substring(nIndex1 + 1, nIndex2));
                    nIndex1 = nIndex2;
                    nIndex2 = s.indexOf(SEPARATOR, nIndex1 + 1);
                    ms.setSourceFile(new File(s.substring(nIndex1 + 1, nIndex2)));
                    nIndex1 = nIndex2;
                    nIndex2 = s.indexOf(SEPARATOR, nIndex1 + 1);
                    ms.setEntryName(s.substring(nIndex1 + 1, nIndex2));
                    // have to translate "null" to null
                    if (ms.getEntryName().equals("null")) {
                        ms.setEntryName(null);
                    }
                    nIndex1 = nIndex2;
                    nIndex2 = s.indexOf(SEPARATOR, nIndex1 + 1);
                    ms.setYear(Integer.parseInt(s.substring(nIndex1 + 1, nIndex2)));
                    nIndex1 = nIndex2;
                    nIndex2 = s.indexOf(SEPARATOR, nIndex1 + 1);
                    ms.setType(Integer.parseInt(s.substring(nIndex1 + 1, nIndex2)));
                    nIndex1 = nIndex2;
                    nIndex2 = s.indexOf(SEPARATOR, nIndex1 + 1);
                    ms.setTons(Integer.parseInt(s.substring(nIndex1 + 1, nIndex2)));
                    ms.setBV(Integer.parseInt(s.substring(nIndex2 + 1)));
                    
                    // Verify that this file still exists and is older than
                    //  the cache.
                    File fSource = ms.getSourceFile();
                    if (fSource.exists() && fSource.lastModified() < lLastCheck) {
                        vMechs.addElement(ms);
                        sKnownFiles.add(ms.getSourceFile().toString());
                        if (waitDialog != null) {
                            ((Label)waitDialog.getComponent(3)).setText(String.valueOf(++loadedFromCacheCount));
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            System.out.println("Unable to load mechcache: " + e.getMessage());
            e.printStackTrace();
        }
        
        
        // load any changes since the last check time
        boolean bNeedsUpdate = loadMechsFromDirectory(vMechs, sKnownFiles, lLastCheck, ROOT);
        
        // convert to array
        m_data = new MechSummary[vMechs.size()];
        vMechs.copyInto(m_data);
        
        // store map references
        for (int x = 0; x < m_data.length; x++) {
            m_nameMap.put(m_data[x].getName(), m_data[x]);
        }
        
        // save updated cache back to disk
        if (bNeedsUpdate) {
            try {
                saveCache();
            } catch (Exception e) {
                System.out.println("Unable to save mech cache");
            }
        }
        
        System.out.println(m_data.length + " mechs loaded.");
        System.out.println("");
    }
    
    private void saveCache()
        throws Exception
    {
        System.out.println("Saving mechcache");
        FileWriter wr = new FileWriter(CACHE);
        for (int x = 0; x < m_data.length; x++) {
            wr.write(m_data[x].getName() + SEPARATOR + 
                    m_data[x].getChassis() + SEPARATOR + 
                    m_data[x].getModel() + SEPARATOR + 
                    m_data[x].getUnitType() + SEPARATOR + 
                    m_data[x].getSourceFile().getPath() + SEPARATOR + 
                    m_data[x].getEntryName() + SEPARATOR + 
                    m_data[x].getYear() + SEPARATOR +
                    m_data[x].getType() + SEPARATOR + 
                    m_data[x].getTons() + SEPARATOR + 
                    m_data[x].getBV() + "\r\n");
        }
        wr.flush();
        wr.close();
    }
    
    // Loading a complete mech object for each summary is a bear and should be 
    // changed, but it lets me use the existing parsers
    private boolean loadMechsFromDirectory(Vector vMechs, Set sKnownFiles, long lLastCheck, File fDir)
    {
        boolean bNeedsUpdate = false;
        System.out.println("Looking in " + fDir.getPath());
        String[] sa = fDir.list();
        // begin block: remove me when TDB moves armor into xml files
        Vector xmlFiles = new Vector();
        for (int j = 0; j < sa.length; j++) {
            if (sa[j].endsWith(".xml")) {
                xmlFiles.addElement(sa[j]);
            }
        }
        // end block
        for (int x = 0; x < sa.length; x++) {
            File f = new File(fDir, sa[x]);
            if (f.equals(CACHE)) { continue; }
            if (f.isDirectory()) {
                 // recursion is fun
                bNeedsUpdate |= loadMechsFromDirectory(vMechs, sKnownFiles, lLastCheck, f);
                continue;
            }
            if (f.getName().indexOf('.') == -1) { continue; }
            // begin block: change me when TDB moves armor into xml files
            if (f.getName().endsWith(".dbm")) {
                // Check if this file has a matching ".xml" file, if
                //  not, print a warning message.  Either way, skip
                //  the file, since it is handled by TdbFile.java.
                boolean foundMatch = false;
                for (int i = 0; i < xmlFiles.size(); i++) {
                    String partial = (String)xmlFiles.elementAt(i);
                    if (partial.substring(0,partial.length() - 3).equals(sa[x].substring(0,sa[x].length() - 3))) {
                        foundMatch = true;
                    }
                }
                if (!foundMatch) {
                    System.out.println("Failure to load mech >> " + sa[x]);
                    System.out.println("  -Both Drawing Board save files (.xml and .dbm) must be present for each Drawing Board mech.");
                }
                continue;
            }
            // end block
            if (f.getName().toLowerCase().endsWith(".zip")) {
                bNeedsUpdate |= loadMechsFromZipFile(vMechs, sKnownFiles, lLastCheck, f);
                continue;
            }
            if (f.lastModified() < lLastCheck  && sKnownFiles.contains(f.toString())) {
                continue;
            }
            try {
                System.out.println("Loading from " + f);
                MechFileParser mfp = new MechFileParser(f);
                Entity m = mfp.getEntity();
                MechSummary ms = new MechSummary();
                ms.setName(m.getShortName());
                ms.setChassis(m.getChassis());
                ms.setModel(m.getModel());
                ms.setUnitType(MechSummary.determineUnitType(m));
                ms.setSourceFile(f);
                ms.setEntryName(null);
                ms.setYear(m.getYear());
                ms.setType(m.getTechLevel());
                ms.setTons((int)m.getWeight());
                ms.setBV(m.calculateBattleValue());
                vMechs.addElement(ms);
                sKnownFiles.add(f.toString());
                bNeedsUpdate = true;
                if (waitDialog != null) {
                    ((Label)waitDialog.getComponent(5)).setText(String.valueOf(++loadedFromFilesCount));
                }
            } catch (EntityLoadingException ex) {
                System.err.println("Unable to load file " + f.getName() + " : " + ex.getMessage());
                continue;
            }
        }
        
        return bNeedsUpdate;
    }
    
    private boolean loadMechsFromZipFile(Vector vMechs, Set sKnownFiles, long lLastCheck, File fZipFile) {
        boolean bNeedsUpdate = false;
        ZipFile zFile;
        try {
            zFile = new ZipFile(fZipFile);
        } catch (Exception ex) {
            System.err.println("Unable to load file " + fZipFile.getName() + " : " + ex.getMessage());
            return false;
        }
        System.out.println("Looking in zip file " + fZipFile.getPath());
        
        for (java.util.Enumeration i = zFile.entries(); i.hasMoreElements();){
            ZipEntry zEntry = (ZipEntry)i.nextElement();
            
            if (zEntry.isDirectory()) { 
                continue; 
            }
            if (Math.max(fZipFile.lastModified(), zEntry.getTime()) < lLastCheck && sKnownFiles.contains(fZipFile.toString())) {
                continue;
            }
            
            try {
                System.out.println("Loading from " + fZipFile.getPath() + " >> " + zEntry.getName());
                if (zEntry.getName().toLowerCase().endsWith(".xml") || zEntry.getName().toLowerCase().endsWith(".dbm")) {
                    System.out.println("  -Mech files from The Drawing Board cannot be loaded from zip files.");
                    continue;
                }
                MechFileParser mfp = new MechFileParser(zFile.getInputStream(zEntry), zEntry.getName());
                Entity m = mfp.getEntity();
                MechSummary ms = new MechSummary();
                ms.setName(m.getShortName());
                ms.setChassis(m.getChassis());
                ms.setModel(m.getModel());
                ms.setUnitType(MechSummary.determineUnitType(m));
                ms.setSourceFile(fZipFile);
                ms.setEntryName(zEntry.getName());
                ms.setYear(m.getYear());
                ms.setType(m.getTechLevel());
                ms.setTons((int)m.getWeight());
                ms.setBV(m.calculateBattleValue());
                vMechs.addElement(ms);
                sKnownFiles.add(zEntry.getName());
                bNeedsUpdate = true;
                if (waitDialog != null) {
                    ((Label)waitDialog.getComponent(7)).setText(String.valueOf(++loadedFromZipsCount));
                }
            } catch (Exception ex) {
                System.err.println("Unable to load file " + zEntry.getName() + " : " + ex.getMessage());
                continue;
            }
        }
        
        try {
            zFile.close();
        } catch (Exception ex) {
            // whatever.
        }
        
        return bNeedsUpdate;
    }
}
