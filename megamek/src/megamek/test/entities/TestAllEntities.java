package megamek.test.entities;

import megamek.common.*;
import megamek.common.Mech;
import megamek.common.MechSummaryCache;

import java.util.Enumeration;
import java.lang.StringBuffer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import gd.xml.*;
import gd.xml.tiny.*;

public class TestAllEntities implements MechSummaryCache.Listener
{
    public final static String CONFIG_FILENAME = "data/mechfiles/UnitVerifierOptions.xml";

    public final static String BASE_NODE = "testallentities";
    public final static String BASE_MECH_NODE = "mech";
    public final static String BASE_TANK_NODE = "tank";

    private MechSummaryCache mechSummaryCache = null;
    private TestXMLOption mechOption = new TestXMLOption();
    private TestXMLOption tankOption = new TestXMLOption();

    public TestAllEntities(File config, boolean all)
    {
        ParsedXML root = null;
        try
        {
            root = TinyParser.parseXML(new FileInputStream(config));
            for (Enumeration e = root.elements(); e.hasMoreElements(); )
            {
                ParsedXML child = (ParsedXML) e.nextElement();
                if (child.getTypeName().equals("tag")
                    && child.getName().equals(BASE_NODE))
                {
                    readOptions(child);
                }
            }
            System.out.println("Using config file: " + config.getPath());
        } catch (ParseException e)
        {
            System.out.println("Failure parsing config file:");
            System.out.println(e.getMessage());
        } catch (FileNotFoundException e)
        {
            System.out.println("Configfile not found:");
            System.out.println(e.getMessage());
        }

        if (all)
        {
            mechSummaryCache = MechSummaryCache.getInstance();
            MechSummaryCache.addListener(this);
        }
    }

    public void checkEntity(Entity entity, String fileString, boolean all)
    {
        try
        {
            StringBuffer buff = new StringBuffer();
            TestEntity testEntity = null;
            if (entity instanceof Mech)
                testEntity = new TestMech((Mech)entity, mechOption, fileString);
            else if (entity instanceof Tank)
                testEntity = new TestTank((Tank)entity, tankOption, fileString);
            else
            {
                System.out.println("UnknownType: "+entity.getDisplayName());
                System.out.println("Found in: "+fileString);
                return;
            }


            if (all)
            {
                buff = testEntity.printEntity();
            }
            else
            {
                if (!testEntity.correctEntity(buff))
                {
                    System.out.println(testEntity.getName());
                    System.out.println("Found in: "+testEntity.fileString);
                }
            }
            System.out.print(buff);
        } catch(EngineException e)
        {
            System.out.println(entity.getDisplayName());
            System.out.println(e.getMessage());
        }
    }

    public Entity loadEntity(File f, String entityName)
    {
        Entity entity = null;
        try
        {
            entity = new MechFileParser(f, entityName).getEntity();
        } catch (megamek.common.loaders.EntityLoadingException e)
        {
            System.out.println("Exception: "+e.toString());
        }
        return entity;
    }

    //This is the listener method that MechSummaryCache calls when it
    // finishes loading all the mechs.  This should only happen if no
    // specific files were passed to main() as arguments (which implies
    // all units that are loaded when MegaMek normally runs should be
    // checked).
    public void doneLoading()
    {
        MechSummary[] ms = mechSummaryCache.getAllMechs();
        System.out.println("\n");


        System.out.println("Mech Options:");
        System.out.println(mechOption.printOptions());
        System.out.println("\nTank Options:");
        System.out.println(tankOption.printOptions());

        for (int i = 0; i < ms.length; i++)
        {
            if (ms[i].getUnitType().equals("Mek") ||
                    ms[i].getUnitType().equals("Tank"))
            {
                Entity entity =loadEntity(ms[i].getSourceFile(),
                        ms[i].getEntryName());
                if (entity==null)
                    continue;
                checkEntity(entity, ms[i].getSourceFile().toString(), false);
            }
        }
    }

    private void readOptions(ParsedXML node)
    {
        for (Enumeration e = node.elements(); e.hasMoreElements(); )
        {
            ParsedXML child = (ParsedXML) e.nextElement();
            if (child.getName().equals(BASE_TANK_NODE))
                tankOption.readXMLOptions(child);
            else if (child.getName().equals(BASE_MECH_NODE))
                mechOption.readXMLOptions(child);
        }
    }

    public static void main( String[] args )
    {
        File f = null;
        String entityName = null;
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].equals("-file"))
            {
                
                if (args.length <= i)
                {
                    System.out.println("Missing argument filename!");
                    return;
                }
                i++;
                f = new File(args[i]);
                if (!f.exists())
                {
                    System.out.println("Can't find: "+args[i]+"!");
                    return;
                }
                if (args[i].endsWith(".zip"))
                {
                    if (args.length <= i+1)
                    {
                        System.out.println("Missing Entity Name!");
                        return;
                    }
                    i++;
                    entityName = args[i];
                }
            }
        }

        File config = new File(CONFIG_FILENAME);
        if (f != null)
        {
            Entity entity = null;
            try
            {
                System.err.println(entityName);
                entity = new MechFileParser(f, entityName).getEntity();
            } catch (megamek.common.loaders.EntityLoadingException e)
            {
                System.err.println("Exception: "+e.toString());
                System.err.println("Exception: "+e.getMessage());
                return;
            }
            new TestAllEntities(config, false).checkEntity(entity, f.toString(), true);
        }
        else
        {
            TestAllEntities te = new TestAllEntities(config, true);
        }
    }
}
