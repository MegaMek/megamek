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

/* OMIT_FOR_JHMPREAD_COMPILATION BLOCK_BEGIN */
package megamek.common.loaders;
/* BLOCK_END */

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;

import java.util.Hashtable;
import java.util.Vector;

/* OMIT_FOR_JHMPREAD_COMPILATION BLOCK_BEGIN */
import megamek.common.BipedMech;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.LocationFullException;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.QuadMech;
import megamek.common.TechConstants;
import megamek.common.WeaponType;
/* BLOCK_END */

/**
 * Based on the hmpread.c program and the MtfFile object.
 *
 * Note that this class doubles as both a MM Heavy Metal Pro parser
 * and a HMP to MTF file converter (when the "main" method is used).
 *
 * @author <a href="mailto:mnewcomb@sourceforge.net">Michael Newcomb</a>
 * Modified by Ryan McConnell (oscarmm) with lots of help from Ian Hamilton.
 */
public class HmpFile
/* OMIT_FOR_JHMPREAD_COMPILATION BLOCK_BEGIN */
    implements MechLoader
/* BLOCK_END */
{
  private String name;
  private String model;

  private ChassisType chassisType;

  private TechType techType;

  private TechType mixedBaseTechType;
  private TechType engineTechType;
  private TechType heatSinkTechType;
  private TechType physicalWeaponTechType;
  private TechType targetingComputerTechType;
  private TechType myomerTechType;
  private TechType armorTechType;

  private int year;
  private int rulesLevel;

  private int tonnage;

  private InternalStructureType internalStructureType;
  private int engineRating;
  private EngineType engineType;
  private ArmorType armorType;

  private int heatSinks;
  private HeatSinkType heatSinkType;

  private int walkMP;
  private int jumpMP;

  private int laArmor;
  private ArmorType laArmorType;
  private int ltArmor;
  private ArmorType ltArmorType;
  private int ltrArmor;
  private ArmorType ltrArmorType;
  private int llArmor;
  private ArmorType llArmorType;

  private int raArmor;
  private ArmorType raArmorType;
  private int rtArmor;
  private ArmorType rtArmorType;
  private int rtrArmor;
  private ArmorType rtrArmorType;
  private int rlArmor;
  private ArmorType rlArmorType;

  private int headArmor;
  private ArmorType headArmorType;

  private int ctArmor;
  private ArmorType ctArmorType;
  private int ctrArmor;
  private ArmorType ctrArmorType;

  private MyomerType myomerType;

  private int totalWeaponCount;
  private int[][] weaponArray;

  private long[] laCriticals = new long[12];
  private long[] ltCriticals = new long[12];
  private long[] llCriticals = new long[12];

  private long[] raCriticals = new long[12];
  private long[] rtCriticals = new long[12];
  private long[] rlCriticals = new long[12];

  private long[] headCriticals = new long[12];
  private long[] ctCriticals = new long[12];

  private Hashtable spreadEquipment = new Hashtable();
  private Vector vSplitWeapons = new Vector();

  public HmpFile(InputStream is)
/* OMIT_FOR_JHMPREAD_COMPILATION BLOCK_BEGIN */
      throws EntityLoadingException
/* BLOCK_END */
  {
    try
    {
      DataInputStream dis = new DataInputStream(is);

      byte[] buffer = new byte[5];
      dis.read(buffer);
      String version = new String(buffer);

      // this next one no longer seems accurate...
      DesignType designType =
          DesignType.getType(readUnsignedByte(dis));

      // ??
      dis.skipBytes(3);

      // some flags saying which Clans use this design
      dis.skipBytes(3);

      // ??
      dis.skipBytes(1);

      // some flags saying which Inner Sphere factions use this design
      dis.skipBytes(3);

      // ??
      dis.skipBytes(1);

      tonnage = readUnsignedShort(dis);

      buffer = new byte[readUnsignedShort(dis)];
      dis.read(buffer);
      name = new String(buffer);

      buffer = new byte[readUnsignedShort(dis)];
      dis.read(buffer);
      model = new String(buffer);

      year = readUnsignedShort(dis);

      rulesLevel = readUnsignedShort(dis);

      long cost = readUnsignedInt(dis);

      // ??
      dis.skipBytes(22);

      // section with BF2 stuff
      int bf2Length = readUnsignedShort(dis);
      dis.skipBytes(bf2Length);

      techType = TechType.getType(readUnsignedShort(dis));

      if (techType == TechType.MIXED) {
          // We've got a total of 7 shorts here.
          // The first one is the mech's "base" chassis technology type.
          //  It also doubles as the internal structure type.
          mixedBaseTechType = TechType.getType(readUnsignedShort(dis));
          // Next we have engine, heat sinks, physical attack weapon,
          //  targeting computer, myomer, and finally armor.  Note that
          //  these 14 bytes are always present in mixed-tech designs,
          //  whether the specific equipment exists on the mech or not.
          engineTechType = TechType.getType(readUnsignedShort(dis));
          heatSinkTechType = TechType.getType(readUnsignedShort(dis));
          physicalWeaponTechType = TechType.getType(readUnsignedShort(dis));
          targetingComputerTechType = TechType.getType(readUnsignedShort(dis));
          myomerTechType = TechType.getType(readUnsignedShort(dis));
          armorTechType = TechType.getType(readUnsignedShort(dis));
      }

      chassisType = ChassisType.getType(readUnsignedShort(dis));

      internalStructureType =
        InternalStructureType.getType(readUnsignedShort(dis));

      engineRating = readUnsignedShort(dis);
      engineType = EngineType.getType(readUnsignedShort(dis));

      walkMP = readUnsignedShort(dis);
      jumpMP = readUnsignedShort(dis);

      heatSinks = readUnsignedShort(dis);
      heatSinkType = HeatSinkType.getType(readUnsignedShort(dis));

      armorType = ArmorType.getType(readUnsignedShort(dis));

      if (armorType == ArmorType.PATCHWORK) {
          laArmorType = ArmorType.getType(readUnsignedShort(dis));
          ltArmorType = ArmorType.getType(readUnsignedShort(dis));
          llArmorType = ArmorType.getType(readUnsignedShort(dis));
          raArmorType = ArmorType.getType(readUnsignedShort(dis));
          rtArmorType = ArmorType.getType(readUnsignedShort(dis));
          rlArmorType = ArmorType.getType(readUnsignedShort(dis));
          headArmorType = ArmorType.getType(readUnsignedShort(dis));
          ctArmorType = ArmorType.getType(readUnsignedShort(dis));
          ltrArmorType = ArmorType.getType(readUnsignedShort(dis));
          rtrArmorType = ArmorType.getType(readUnsignedShort(dis));
          ctrArmorType = ArmorType.getType(readUnsignedShort(dis));
      }

      dis.skipBytes(2); // ??
      laArmor = readUnsignedShort(dis);
      dis.skipBytes(4); // ??
      ltArmor = readUnsignedShort(dis);
      dis.skipBytes(4); // ??
      llArmor = readUnsignedShort(dis);
      dis.skipBytes(4); // ??
      raArmor = readUnsignedShort(dis);
      dis.skipBytes(4); // ??
      rtArmor = readUnsignedShort(dis);
      dis.skipBytes(4); // ??
      rlArmor = readUnsignedShort(dis);
      dis.skipBytes(4); // ??
      headArmor = readUnsignedShort(dis);
      dis.skipBytes(4); // ??
      ctArmor = readUnsignedShort(dis);
      dis.skipBytes(2); // ??
      ltrArmor = readUnsignedShort(dis);
      rtrArmor = readUnsignedShort(dis);
      ctrArmor = readUnsignedShort(dis);

      myomerType =
          MyomerType.getType(readUnsignedShort(dis));

      totalWeaponCount = readUnsignedShort(dis);
      weaponArray = new int[totalWeaponCount][4];
      for (int i = 0; i < totalWeaponCount; i++)
      {
          weaponArray[i][0] = readUnsignedShort(dis); //weapon count
          weaponArray[i][1] = readUnsignedShort(dis); //weapon type
          weaponArray[i][2] = readUnsignedShort(dis); //weapon location
          weaponArray[i][3] = readUnsignedShort(dis); //ammo

          dis.skipBytes(2); // ??

          // manufacturer name
          dis.skipBytes(readUnsignedShort(dis));
      }

      // left arm criticals
      for(int x = 0; x < 12; x++)
      {
        laCriticals[x] = readUnsignedInt(dis);
      }	

      // left torso criticals
      for(int x = 0; x < 12; x++)
      {
        ltCriticals[x] = readUnsignedInt(dis);
      }	

      // left leg criticals
      for(int x = 0; x < 12; x++)
      {
        llCriticals[x] = readUnsignedInt(dis);
      }	

      // right arm criticals
      for(int x = 0; x < 12; x++)
      {
        raCriticals[x] = readUnsignedInt(dis);
      }	

      // right torso criticals
      for(int x = 0; x < 12; x++)
      {
        rtCriticals[x] = readUnsignedInt(dis);
      }	

      // right leg criticals
      for(int x = 0; x < 12; x++)
      {
        rlCriticals[x] = readUnsignedInt(dis);
      }	

      // head criticals
      for(int x = 0; x < 12; x++)
      {
        headCriticals[x] = readUnsignedInt(dis);
      }	

      // center torso criticals
      for(int x = 0; x < 12; x++)
      {
        ctCriticals[x] = readUnsignedInt(dis);
      }	

      dis.close();
    }
    catch (IOException ex)
    {
/* OMIT_FOR_JHMPREAD_COMPILATION BLOCK_BEGIN */
        throw new EntityLoadingException("I/O Error reading file");
/* BLOCK_END */
    }
  }

  private short readUnsignedByte(DataInputStream dis)
    throws IOException
  {
    short b = dis.readByte();
    b += b < 0 ? 256 : 0;
    return b;
  }

  private int readUnsignedShort(DataInputStream dis)
    throws IOException
  {
    int b2 = readUnsignedByte(dis);

    int b1 = readUnsignedByte(dis);
    b1 <<= 8;

    return b1 + b2;
  }

  private long readUnsignedInt(DataInputStream dis)
    throws IOException
  {
    long b4 = readUnsignedByte(dis);

    long b3 = readUnsignedByte(dis);
    b3 <<= 8;

    long b2 = readUnsignedByte(dis);
    b2 <<= 16;

    long b1 = readUnsignedByte(dis);
    b1 <<= 32;

    return b1 + b2 + b3 + b4;
  }

/* OMIT_FOR_JHMPREAD_COMPILATION BLOCK_BEGIN */
  public Entity getEntity()
    throws EntityLoadingException
  {
    try
    {
      Mech mech =
        chassisType == ChassisType.QUADRAPED ? (Mech) new QuadMech() :
        (Mech) new BipedMech();

      mech.setChassis(name);
      mech.setModel(model);
      mech.setYear(year);

      mech.setOmni(chassisType == ChassisType.BIPED_OMNI ||
                   chassisType == ChassisType.QUADRAPED_OMNI);

      int techLevel = rulesLevel == 1 ? TechConstants.T_IS_LEVEL_1 :
        techType == TechType.CLAN ? TechConstants.T_CLAN_LEVEL_2 :
        TechConstants.T_IS_LEVEL_2;
      mech.setTechLevel(techLevel);

      mech.setWeight(tonnage);

      mech.setOriginalWalkMP(walkMP);
      mech.setOriginalJumpMP(jumpMP);

      mech.autoSetInternal();

      mech.initializeArmor(laArmor, Mech.LOC_LARM);
      mech.initializeArmor(ltArmor, Mech.LOC_LT);
      mech.initializeRearArmor(ltrArmor, Mech.LOC_LT);
      mech.initializeArmor(llArmor, Mech.LOC_LLEG);

      mech.initializeArmor(raArmor, Mech.LOC_RARM);
      mech.initializeArmor(rtArmor, Mech.LOC_RT);
      mech.initializeRearArmor(rtrArmor, Mech.LOC_RT);
      mech.initializeArmor(rlArmor, Mech.LOC_RLEG);

      mech.initializeArmor(headArmor, Mech.LOC_HEAD);

      mech.initializeArmor(ctArmor, Mech.LOC_CT);
      mech.initializeRearArmor(ctrArmor, Mech.LOC_CT);

      setupCriticals(mech);

      if (mech.isClan())
      {
        mech.addClanCase();
      }
            
      // add any heat sinks not allocated
      //
      mech.addEngineSinks(heatSinks - mech.heatSinks(),
                          heatSinkType == HeatSinkType.DOUBLE);

      return mech;
    }
    catch (Exception e)
    {
      e.printStackTrace();
      throw new EntityLoadingException(e.getMessage());
    }
  }

  private void removeArmActuators(Mech mech, long[] criticals, int location)
  {
      // Quad have leg and foot actuators, not arm and hand actuators.
      if ( mech.getMovementType() == Entity.MovementType.QUAD ) {
          if (!isLowerLegActuator(criticals[2])) {
              mech.setCritical(location, 2, null);
          }
          if (!isFootActuator(criticals[3])) {
              mech.setCritical(location, 3, null);
          }
      } else {
          if (!isLowerArmActuator(criticals[2])) {
              mech.setCritical(location, 2, null);
          }
          if (!isHandActuator(criticals[3])) {
              mech.setCritical(location, 3, null);
          }
      }
  }

  private void setupCriticals(Mech mech)
    throws EntityLoadingException
  {
    removeArmActuators(mech, laCriticals, Mech.LOC_LARM);
    removeArmActuators(mech, raCriticals, Mech.LOC_RARM);

    compactCriticals(rlCriticals);
    setupCriticals(mech, rlCriticals, Mech.LOC_RLEG);
    compactCriticals(llCriticals);
    setupCriticals(mech, llCriticals, Mech.LOC_LLEG);
    compactCriticals(raCriticals);
    setupCriticals(mech, raCriticals, Mech.LOC_RARM);
    compactCriticals(laCriticals);
    setupCriticals(mech, laCriticals, Mech.LOC_LARM);
    compactCriticals(rtCriticals);
    setupCriticals(mech, rtCriticals, Mech.LOC_RT);
    compactCriticals(ltCriticals);
    setupCriticals(mech, ltCriticals, Mech.LOC_LT);
    compactCriticals(ctCriticals);
    setupCriticals(mech, ctCriticals, Mech.LOC_CT);
    setupCriticals(mech, headCriticals, Mech.LOC_HEAD);
  }

  private void setupCriticals(Mech mech, long[] criticals, int location)
    throws EntityLoadingException {
      // Use pass-by-value in case we need the original criticals
      //  later (getMtf for example).
      long[] crits = (long[])criticals.clone();

      for (int i = 0; i < mech.getNumberOfCriticals(location); i++) {
     if (mech.getCritical(location, i) == null) {
        long critical = crits[i];
        String criticalName = getCriticalName(critical);

        if (isFusionEngine(critical)) {

          mech.setCritical(location, i,
                           new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                                            Mech.SYSTEM_ENGINE));
        }
        else if (criticalName != null) {
            EquipmentType equipment = null;
          try {
            equipment = EquipmentType.get(criticalName);
            if (equipment != null) {
              boolean rearMounted = equipment instanceof WeaponType &&
                isRearMounted(critical);
              if (equipment.isSpreadable()) {
                Mounted m = (Mounted) spreadEquipment.get(equipment);
                if (m != null) {
                  CriticalSlot criticalSlot =
                    new CriticalSlot(CriticalSlot.TYPE_EQUIPMENT,
                                     mech.getEquipmentNum(m),
                                     equipment.isHittable());
                  mech.addCritical(location, criticalSlot);
                }
                else {
                  m = mech.addEquipment(equipment, location, rearMounted);
                  spreadEquipment.put(equipment, m);
                }
              }
              else if ( equipment instanceof WeaponType &&
                        equipment.hasFlag(WeaponType.F_SPLITABLE) ) {
                // do we already have this one in this or an outer location?
                Mounted m = null;
                boolean bFound = false;
                for (int x = 0, n = vSplitWeapons.size(); x < n; x++) {
                  m = (Mounted)vSplitWeapons.elementAt(x);
                  int nLoc = m.getLocation();
                  if ((nLoc == location || location == Mech.getInnerLocation(nLoc))
                      && m.getType() == equipment) {
                    bFound = true;
                    break;
                  }
                }
                if (bFound) {
                  m.setFoundCrits(m.getFoundCrits() + 1);
                  if (m.getFoundCrits() >= equipment.getCriticals(mech)) {
                    vSplitWeapons.removeElement(m);
                  }
                  // give the most restrictive location for arcs
                  m.setLocation(Mech.mostRestrictiveLoc(location, m.getLocation()));
                }
                else {
                  // make a new one
                  m = new Mounted(mech, equipment);
                  m.setSplit(true);
                  m.setFoundCrits(1);
                  vSplitWeapons.addElement(m);
                }
                mech.addEquipment(m, location, rearMounted);
              }
              else {
                mech.addEquipment(equipment, location, rearMounted);
              }
            } else {
                if (!criticalName.equals("-Empty-")) {
                    //Can't load this piece of equipment!
                    // Add it to the list so we can show the user.
                    mech.addFailedEquipment(criticalName);
                    // Make the failed equipment an empty slot
                    crits[i] = 0;
                    // Compact criticals again
                    compactCriticals(crits);
                    // Re-parse the same slot, since the compacting
                    //  could have moved new equipment to this slot
                    i--;
                }
            }
          } catch (LocationFullException ex) {
              System.err.print( "Location was full when adding " );
              System.err.print( equipment.getInternalName() );
              System.err.print( " to slot #" );
              System.err.print( i );
              System.err.print( " of location " );
              System.err.println( location );
              ex.printStackTrace( System.err );
              System.err.println( "... aborting entity loading." );
            throw new EntityLoadingException(ex.getMessage());
          }
        }
     }
    }
  }
/* BLOCK_END */

  private boolean isLowerArmActuator(long critical)
  {
    return critical == 0x03;
  }

  private static boolean isHandActuator(long critical)
  {
    return critical == 0x04;
  }

  private static boolean isLowerLegActuator(long critical)
  {
    return critical == 0x07;
  }

  private static boolean isFootActuator(long critical)
  {
    return critical == 0x08;
  }

  private static boolean isFusionEngine(long critical)
  {
    return critical == 0x0F;
  }

  private static boolean isRearMounted(long critical)
  {
    return (critical & 0xFFFF0000) != 0;
  }

  private static final Hashtable criticals = new Hashtable();
  static
  {
    // common criticals
    //
    criticals.put(new Long(0x00), "-Empty-");
    criticals.put(new Long(0x01), "Shoulder");
    criticals.put(new Long(0x02), "Upper Arm Actuator");
    criticals.put(new Long(0x03), "Lower Arm Actuator");
    criticals.put(new Long(0x04), "Hand Actuator");
    criticals.put(new Long(0x05), "Hip");
    criticals.put(new Long(0x06), "Upper Leg Actuator");
    criticals.put(new Long(0x07), "Lower Leg Actuator");
    criticals.put(new Long(0x08), "Foot Actuator");
    criticals.put(new Long(0x09), "Heat Sink");

    criticals.put(new Long(0x0B), "Jump Jet");
    criticals.put(new Long(0x0C), "Life Support");
    criticals.put(new Long(0x0D), "Sensors");
    criticals.put(new Long(0x0E), "Cockpit");
    criticals.put(new Long(0x0F), "Fusion Engine");
    criticals.put(new Long(0x10), "Gyro");

    criticals.put(new Long(0x14), "Endo Steel");
    criticals.put(new Long(0x15), "Ferro-Fibrous");
    criticals.put(new Long(0x16), "Triple Strength Myomer");

    criticals.put(new Long(0x1c), "Reactive Armor");
    criticals.put(new Long(0x1d), "Laser-Reflective Armor");

    criticals.put(new Long(0x21), "Light Ferro-Fibrous");
    criticals.put(new Long(0x22), "Heavy Ferro-Fibrous");


    // criticals for mechs with a base type of inner sphere
    //
    Hashtable isCriticals = new Hashtable();
    criticals.put(TechType.INNER_SPHERE, isCriticals);
    isCriticals.put(new Long(0x0A), "ISDouble Heat Sink");

    isCriticals.put(new Long(0x11), "Hatchet");
    isCriticals.put(new Long(0x12), "ISTargeting Computer");

    isCriticals.put(new Long(0x17), "ISMASC");
    isCriticals.put(new Long(0x18), "ISArtemisIV");
    isCriticals.put(new Long(0x19), "ISCASE");

    isCriticals.put(new Long(0x1F), "Sword");

    isCriticals.put(new Long(0x23), "Stealth Armor");

    isCriticals.put(new Long(0x33), "ISERLargeLaser");
    isCriticals.put(new Long(0x34), "ISERPPC");
    isCriticals.put(new Long(0x35), "ISFlamer");

    isCriticals.put(new Long(0x37), "ISLargeLaser");
    isCriticals.put(new Long(0x38), "ISMediumLaser");
    isCriticals.put(new Long(0x39), "ISSmallLaser");
    isCriticals.put(new Long(0x3A), "ISPPC");
    isCriticals.put(new Long(0x3B), "ISLargePulseLaser");
    isCriticals.put(new Long(0x3C), "ISMediumPulseLaser");
    isCriticals.put(new Long(0x3D), "ISSmallPulseLaser");
    isCriticals.put(new Long(0x3E), "ISAC2");
    isCriticals.put(new Long(0x3F), "ISAC5");
    isCriticals.put(new Long(0x40), "ISAC10");
    isCriticals.put(new Long(0x41), "ISAC20");
    isCriticals.put(new Long(0x42), "ISAntiMissileSystem");

    isCriticals.put(new Long(0x46), "ISLightGaussRifle");
    isCriticals.put(new Long(0x47), "ISGaussRifle");

    isCriticals.put(new Long(0x4B), "ISLBXAC2");
    isCriticals.put(new Long(0x4C), "ISLBXAC5");
    isCriticals.put(new Long(0x4D), "ISLBXAC10");
    isCriticals.put(new Long(0x4E), "ISLBXAC20");
    isCriticals.put(new Long(0x4F), "ISMachine Gun");

    isCriticals.put(new Long(0x54), "ISUltraAC2");
    isCriticals.put(new Long(0x55), "ISUltraAC5");
    isCriticals.put(new Long(0x56), "ISUltraAC10");
    isCriticals.put(new Long(0x57), "ISUltraAC20");
    isCriticals.put(new Long(0x58), "CLERMicroLaser");

    isCriticals.put(new Long(0x5A), "ISERMediumLaser");
    isCriticals.put(new Long(0x5B), "ISERSmallLaser");
    isCriticals.put(new Long(0x5C), "ISAntiPersonnelPod");

    isCriticals.put(new Long(0x60), "ISLRM5");
    isCriticals.put(new Long(0x61), "ISLRM10");
    isCriticals.put(new Long(0x62), "ISLRM15");
    isCriticals.put(new Long(0x63), "ISLRM20");

    isCriticals.put(new Long(0x64), "CLLightActiveProbe");
    isCriticals.put(new Long(0x65), "CLLightTAG");

    isCriticals.put(new Long(0x66), "ISImprovedNarc");
    isCriticals.put(new Long(0x67), "ISSRM2");
    isCriticals.put(new Long(0x68), "ISSRM4");
    isCriticals.put(new Long(0x69), "ISSRM6");
    isCriticals.put(new Long(0x6A), "ISStreakSRM2");
    isCriticals.put(new Long(0x6B), "ISStreakSRM4");
    isCriticals.put(new Long(0x6C), "ISStreakSRM6");

    isCriticals.put(new Long(0x71), "ISArrowIVSystem");

    isCriticals.put(new Long(0x73), "ISBeagleActiveProbe");

    isCriticals.put(new Long(0x75), "ISC3MasterComputer");
    isCriticals.put(new Long(0x76), "ISC3SlaveUnit");
    isCriticals.put(new Long(0x77), "ISImprovedC3CPU");
    isCriticals.put(new Long(0x78), "ISGuardianECM");
    isCriticals.put(new Long(0x79), "ISNarcBeacon");
    isCriticals.put(new Long(0x7A), "ISTAG");
    isCriticals.put(new Long(0x7B), "ISLRM5 (OS)");
    isCriticals.put(new Long(0x7C), "ISLRM10 (OS)");
    isCriticals.put(new Long(0x7D), "ISLRM15 (OS)");
    isCriticals.put(new Long(0x7E), "ISLRM20 (OS)");
    isCriticals.put(new Long(0x7F), "ISSRM2 (OS)");
    isCriticals.put(new Long(0x80), "ISSRM4 (OS)");
    isCriticals.put(new Long(0x81), "ISSRM6 (OS)");
    isCriticals.put(new Long(0x82), "ISStreakSRM2 (OS)");
    isCriticals.put(new Long(0x83), "ISStreakSRM4 (OS)");
    isCriticals.put(new Long(0x84), "ISStreakSRM6 (OS)");
    isCriticals.put(new Long(0x85), "ISFlamer (Vehicle)");

    isCriticals.put(new Long(0x87), "ISSniperArtillery");
    isCriticals.put(new Long(0x88), "ISThumperArtillery");
    isCriticals.put(new Long(0x89), "ISMRM10");
    isCriticals.put(new Long(0x8A), "ISMRM20");
    isCriticals.put(new Long(0x8B), "ISMRM30");
    isCriticals.put(new Long(0x8C), "ISMRM40");

    isCriticals.put(new Long(0x8E), "ISMRM10 (OS)");
    isCriticals.put(new Long(0x8F), "ISMRM20 (OS)");
    isCriticals.put(new Long(0x90), "ISMRM30 (OS)");
    isCriticals.put(new Long(0x91), "ISMRM40 (OS)");
    isCriticals.put(new Long(0x92), "ISLRTorpedo5");
    isCriticals.put(new Long(0x93), "ISLRTorpedo10");
    isCriticals.put(new Long(0x94), "ISLRTorpedo15");
    isCriticals.put(new Long(0x95), "ISLRTorpedo20");
    isCriticals.put(new Long(0x96), "ISSRTorpedo2");
    isCriticals.put(new Long(0x97), "ISSRTorpedo4");
    isCriticals.put(new Long(0x98), "ISSRTorpedo6");

    isCriticals.put(new Long(0xA7), "CLERLargeLaser");
    isCriticals.put(new Long(0xA8), "CLERMediumLaser");
    isCriticals.put(new Long(0xA9), "CLERSmallLaser");

    isCriticals.put(new Long(0xAA), "CLERPPC");
    isCriticals.put(new Long(0xAB), "CLFlamer");

    isCriticals.put(new Long(0xB0), "CLLargePulseLaser");
    isCriticals.put(new Long(0xB1), "CLMediumPulseLaser");
    isCriticals.put(new Long(0xB2), "CLSmallPulseLaser");

    isCriticals.put(new Long(0xB4), "CLAntiMissileSystem");
    isCriticals.put(new Long(0xB5), "CLGaussRifle");
    isCriticals.put(new Long(0xB6), "CLLBXAC2");

    isCriticals.put(new Long(0xBA), "CLMG");
    isCriticals.put(new Long(0xBB), "CLUltraAC2");
    isCriticals.put(new Long(0xBC), "CLUltraAC5");
    isCriticals.put(new Long(0xBD), "CLUltraAC10");
    isCriticals.put(new Long(0xBE), "CLUltraAC20");
    isCriticals.put(new Long(0xBF), "CLLRM5");
    isCriticals.put(new Long(0xC0), "CLLRM10");
    isCriticals.put(new Long(0xC1), "CLLRM15");
    isCriticals.put(new Long(0xC2), "CLLRM20");
    isCriticals.put(new Long(0xC3), "CLSRM2");
    isCriticals.put(new Long(0xC4), "CLSRM4");
    isCriticals.put(new Long(0xC5), "CLSRM6");
    isCriticals.put(new Long(0xC6), "CLStreakSRM2");
    isCriticals.put(new Long(0xC7), "CLStreakSRM4");
    isCriticals.put(new Long(0xC8), "CLStreakSRM6");
    isCriticals.put(new Long(0xC9), "CLArrowIVSystem");
    isCriticals.put(new Long(0xCA), "CLAntiPersonnelPod");
    isCriticals.put(new Long(0xCB), "CLActiveProbe");
    isCriticals.put(new Long(0xCC), "CLECMSuite");
    isCriticals.put(new Long(0xCD), "CLNarcBeacon");
    isCriticals.put(new Long(0xCE), "CLTAG");

    isCriticals.put(new Long(0xD0), "CLLRM5 (OS)");
    isCriticals.put(new Long(0xD1), "CLLRM10 (OS)");
    isCriticals.put(new Long(0xD2), "CLLRM15 (OS)");
    isCriticals.put(new Long(0xD3), "CLLRM20 (OS)");
    isCriticals.put(new Long(0xD4), "CLSRM2 (OS)");
    isCriticals.put(new Long(0xD5), "CLSRM2 (OS)");
    isCriticals.put(new Long(0xD6), "CLSRM2 (OS)");
    isCriticals.put(new Long(0xD7), "CLStreakSRM2 (OS)");
    isCriticals.put(new Long(0xD8), "CLStreakSRM4 (OS)");
    isCriticals.put(new Long(0xD9), "CLStreakSRM6 (OS)");
    isCriticals.put(new Long(0xDA), "CLFlamer (Vehicle)");

    isCriticals.put(new Long(0xDC), "CLSniperArtillery");
    isCriticals.put(new Long(0xDD), "CLThumperArtillery");
    isCriticals.put(new Long(0xDE), "CLLRTorpedo5");
    isCriticals.put(new Long(0xDF), "CLLRTorpedo10");
    isCriticals.put(new Long(0xE0), "CLLRTorpedo15");
    isCriticals.put(new Long(0xE1), "CLLRTorpedo20");
    isCriticals.put(new Long(0xE2), "CLSRTorpedo2");
    isCriticals.put(new Long(0xE3), "CLSRTorpedo4");
    isCriticals.put(new Long(0xE4), "CLSRTorpedo6");

    isCriticals.put(new Long(0xF4), "CLHeavyLargeLaser");
    isCriticals.put(new Long(0xF5), "CLHeavyMediumLaser");
    isCriticals.put(new Long(0xF6), "CLHeavySmallLaser");

    isCriticals.put(new Long(0x121), "ISRotaryAC2");
    isCriticals.put(new Long(0x122), "ISRotaryAC5");
    isCriticals.put(new Long(0x123), "ISHeavyGaussRifle");

    isCriticals.put(new Long(0x129), "ISRocketLauncher10");
    isCriticals.put(new Long(0x12A), "ISRocketLauncher15");
    isCriticals.put(new Long(0x12B), "ISRocketLauncher20");

    isCriticals.put(new Long(0x01ce), "ISAC2 Ammo");
    isCriticals.put(new Long(0x01cf), "ISAC5 Ammo");
    isCriticals.put(new Long(0x01d0), "ISAC10 Ammo");
    isCriticals.put(new Long(0x01d1), "ISAC20 Ammo");
    isCriticals.put(new Long(0x01d2), "ISAMS Ammo");

    isCriticals.put(new Long(0x01d6), "ISLightGauss Ammo");
    isCriticals.put(new Long(0x01d7), "ISGauss Ammo");

    isCriticals.put(new Long(0x01db), "ISLBXAC2 Ammo");
    isCriticals.put(new Long(0x01dc), "ISLBXAC5 Ammo");
    isCriticals.put(new Long(0x01dd), "ISLBXAC10 Ammo");
    isCriticals.put(new Long(0x01de), "ISLBXAC20 Ammo");
    isCriticals.put(new Long(0x01df), "ISMG Ammo");

    isCriticals.put(new Long(0x01e4), "ISUltraAC2 Ammo");
    isCriticals.put(new Long(0x01e5), "ISUltraAC5 Ammo");
    isCriticals.put(new Long(0x01e6), "ISUltraAC10 Ammo");
    isCriticals.put(new Long(0x01e7), "ISUltraAC20 Ammo");

    isCriticals.put(new Long(0x01f0), "ISLRM5 Ammo");
    isCriticals.put(new Long(0x01f1), "ISLRM10 Ammo");
    isCriticals.put(new Long(0x01f2), "ISLRM15 Ammo");
    isCriticals.put(new Long(0x01f3), "ISLRM20 Ammo");

    isCriticals.put(new Long(0x01f6), "ISiNarc Pods");
    isCriticals.put(new Long(0x01f7), "ISSRM2 Ammo");
    isCriticals.put(new Long(0x01f8), "ISSRM4 Ammo");
    isCriticals.put(new Long(0x01f9), "ISSRM6 Ammo");
    isCriticals.put(new Long(0x01fa), "ISStreakSRM2 Ammo");
    isCriticals.put(new Long(0x01fb), "ISStreakSRM4 Ammo");
    isCriticals.put(new Long(0x01fc), "ISStreakSRM6 Ammo");

    isCriticals.put(new Long(0x0201), "ISArrowIV Ammo");

    isCriticals.put(new Long(0x0209), "ISNarc Pods");

    isCriticals.put(new Long(0x0215), "ISFlamer Ammo");

    isCriticals.put(new Long(0x0217), "ISSniper Ammo");
    isCriticals.put(new Long(0x0218), "ISThumper Ammo");
    isCriticals.put(new Long(0x0219), "ISMRM10 Ammo");
    isCriticals.put(new Long(0x021a), "ISMRM20 Ammo");
    isCriticals.put(new Long(0x021b), "ISMRM30 Ammo");
    isCriticals.put(new Long(0x021c), "ISMRM40 Ammo");

    isCriticals.put(new Long(0x0222), "ISLRTorpedo5 Ammo");
    isCriticals.put(new Long(0x0223), "ISLRTorpedo10 Ammo");
    isCriticals.put(new Long(0x0224), "ISLRTorpedo15 Ammo");
    isCriticals.put(new Long(0x0225), "ISLRTorpedo20 Ammo");
    isCriticals.put(new Long(0x0226), "ISSRTorpedo2 Ammo");
    isCriticals.put(new Long(0x0227), "ISSRTorpedo4 Ammo");
    isCriticals.put(new Long(0x0228), "ISSRTorpedo6 Ammo");

    isCriticals.put(new Long(0x0244), "CLAMS Ammo");
    isCriticals.put(new Long(0x0245), "CLGauss Ammo");
    isCriticals.put(new Long(0x0246), "CLLBXAC2 Ammo");
    isCriticals.put(new Long(0x0247), "CLLBXAC5 Ammo");
    isCriticals.put(new Long(0x0248), "CLLBXAC10 Ammo");
    isCriticals.put(new Long(0x0249), "CLLBXAC20 Ammo");
    isCriticals.put(new Long(0x024A), "CLMG Ammo");
    isCriticals.put(new Long(0x024B), "CLUltraAC2 Ammo");
    isCriticals.put(new Long(0x024C), "CLUltraAC5 Ammo");
    isCriticals.put(new Long(0x024D), "CLUltraAC10 Ammo");
    isCriticals.put(new Long(0x024E), "CLUltraAC20 Ammo");
    isCriticals.put(new Long(0x024F), "CLLRM5 Ammo");
    isCriticals.put(new Long(0x0250), "CLLRM10 Ammo");
    isCriticals.put(new Long(0x0251), "CLLRM15 Ammo");
    isCriticals.put(new Long(0x0252), "CLLRM20 Ammo");
    isCriticals.put(new Long(0x0253), "CLSRM2 Ammo");
    isCriticals.put(new Long(0x0254), "CLSRM4 Ammo");
    isCriticals.put(new Long(0x0255), "CLSRM6 Ammo");
    isCriticals.put(new Long(0x0256), "CLStreakSRM2 Ammo");
    isCriticals.put(new Long(0x0257), "CLStreakSRM4 Ammo");
    isCriticals.put(new Long(0x0258), "CLStreakSRM6 Ammo");
    isCriticals.put(new Long(0x0259), "CLArrowIV Ammo");

    isCriticals.put(new Long(0x025D), "CLNarc Pods");

    isCriticals.put(new Long(0x026A), "CLFlamer (Vehicle) Ammo");
    isCriticals.put(new Long(0x026C), "CLSniper Ammo");
    isCriticals.put(new Long(0x026D), "CLThumper Ammo");
    isCriticals.put(new Long(0x026E), "CLLRTorpedo5 Ammo");
    isCriticals.put(new Long(0x026F), "CLLRTorpedo10 Ammo");
    isCriticals.put(new Long(0x0270), "CLLRTorpedo15 Ammo");
    isCriticals.put(new Long(0x0271), "CLLRTorpedo20 Ammo");
    isCriticals.put(new Long(0x0272), "CLSRTorpedo2 Ammo");
    isCriticals.put(new Long(0x0273), "CLSRTorpedo4 Ammo");
    isCriticals.put(new Long(0x0274), "CLSRTorpedo6 Ammo");

    isCriticals.put(new Long(0x02b1), "ISRotaryAC2 Ammo");
    isCriticals.put(new Long(0x02b2), "ISRotaryAC5 Ammo");
    isCriticals.put(new Long(0x02b3), "ISHeavyGauss Ammo");

    // criticals for mechs with a base type of clan
    //
    Hashtable clanCriticals = new Hashtable();
    criticals.put(TechType.CLAN, clanCriticals);
    clanCriticals.put(new Long(0x0A), "CLDouble Heat Sink");

    clanCriticals.put(new Long(0x12), "CLTargeting Computer");

    clanCriticals.put(new Long(0x17), "CLMASC");
    clanCriticals.put(new Long(0x18), "CLArtemisIV");

    clanCriticals.put(new Long(0x21), "Light Ferro-Fibrous"); //?
    clanCriticals.put(new Long(0x22), "Heavy Ferro-Fibrous"); //?

    clanCriticals.put(new Long(0x33), "CLERLargeLaser");
    clanCriticals.put(new Long(0x34), "CLERMediumLaser");
    clanCriticals.put(new Long(0x35), "CLERSmallLaser");
    clanCriticals.put(new Long(0x36), "CLERPPC");
    clanCriticals.put(new Long(0x37), "CLFlamer");
    //    clanCriticals.put(new Long(0x38), "CLMediumLaser");
    //    clanCriticals.put(new Long(0x39), "CLSmallLaser");
    //    clanCriticals.put(new Long(0x3A), "CLPPC");

    clanCriticals.put(new Long(0x3C), "CLLargePulseLaser");
    clanCriticals.put(new Long(0x3D), "CLMediumPulseLaser");
    clanCriticals.put(new Long(0x3E), "CLSmallPulseLaser");
    //    clanCriticals.put(new Long(0x3F), "CLAC5");
    clanCriticals.put(new Long(0x40), "CLAntiMissileSystem");
    clanCriticals.put(new Long(0x41), "CLGaussRifle");
    clanCriticals.put(new Long(0x42), "CLLBXAC2");
    clanCriticals.put(new Long(0x43), "CLLBXAC5");
    clanCriticals.put(new Long(0x44), "CLLBXAC10");
    clanCriticals.put(new Long(0x45), "CLLBXAC20");
    clanCriticals.put(new Long(0x46), "CLMG");
    clanCriticals.put(new Long(0x47), "CLUltraAC2");
    clanCriticals.put(new Long(0x48), "CLUltraAC5");
    clanCriticals.put(new Long(0x49), "CLUltraAC10");
    clanCriticals.put(new Long(0x4A), "CLUltraAC20");
    clanCriticals.put(new Long(0x4B), "CLLRM5");
    clanCriticals.put(new Long(0x4C), "CLLRM10");
    clanCriticals.put(new Long(0x4D), "CLLRM15");
    clanCriticals.put(new Long(0x4E), "CLLRM20");
    clanCriticals.put(new Long(0x4F), "CLSRM2");
    clanCriticals.put(new Long(0x50), "CLSRM4");
    clanCriticals.put(new Long(0x51), "CLSRM6");
    clanCriticals.put(new Long(0x52), "CLStreakSRM2");
    clanCriticals.put(new Long(0x53), "CLStreakSRM4");
    clanCriticals.put(new Long(0x54), "CLStreakSRM6");
    clanCriticals.put(new Long(0x55), "CLArrowIVSystem");
    clanCriticals.put(new Long(0x56), "CLAntiPersonnelPod");
    clanCriticals.put(new Long(0x57), "CLActiveProbe");
    clanCriticals.put(new Long(0x58), "CLECMSuite");
    clanCriticals.put(new Long(0x59), "CLNarcBeacon");
    clanCriticals.put(new Long(0x5A), "CLTAG");
    clanCriticals.put(new Long(0x5B), "CLERMicroLaser");
    clanCriticals.put(new Long(0x5C), "CLLRM5 (OS)");
    clanCriticals.put(new Long(0x5D), "CLLRM10 (OS)");
    clanCriticals.put(new Long(0x5E), "CLLRM15 (OS)");
    clanCriticals.put(new Long(0x5F), "CLLRM20 (OS)");
    clanCriticals.put(new Long(0x60), "CLSRM2 (OS)");
    clanCriticals.put(new Long(0x61), "CLSRM4 (OS)");
    clanCriticals.put(new Long(0x62), "CLSRM6 (OS)");
    clanCriticals.put(new Long(0x63), "CLStreakSRM2 (OS)");
    clanCriticals.put(new Long(0x64), "CLStreakSRM4 (OS)");
    clanCriticals.put(new Long(0x65), "CLStreakSRM6 (OS)");
    clanCriticals.put(new Long(0x66), "CLFlamer (Vehicle)");
    clanCriticals.put(new Long(0x67), "CLSRM2");
    clanCriticals.put(new Long(0x68), "CLSniperArtillery");
    clanCriticals.put(new Long(0x69), "CLThumperArtillery");
    clanCriticals.put(new Long(0x6A), "CLLRTorpedo5");
    clanCriticals.put(new Long(0x6B), "CLLRTorpedo10");
    clanCriticals.put(new Long(0x6C), "CLLRTorpedo15");
    clanCriticals.put(new Long(0x6D), "CLLRTorpedo20");
    clanCriticals.put(new Long(0x6E), "CLSRTorpedo2");
    clanCriticals.put(new Long(0x6F), "CLSRTorpedo4");
    clanCriticals.put(new Long(0x70), "CLSRTorpedo6");

    clanCriticals.put(new Long(0x7B), "CLLRM5 (OS)");
    clanCriticals.put(new Long(0x7C), "CLLRM10 (OS)");
    clanCriticals.put(new Long(0x7D), "CLLRM15 (OS)");
    clanCriticals.put(new Long(0x7E), "CLLRM20 (OS)");
    clanCriticals.put(new Long(0x7F), "CLSRM2 (OS)");
    clanCriticals.put(new Long(0x80), "CLHeavyLargeLaser");
    clanCriticals.put(new Long(0x81), "CLHeavyMediumLaser");
    clanCriticals.put(new Long(0x82), "CLHeavySmallLaser");
    clanCriticals.put(new Long(0x83), "ISERLargeLaser");
    clanCriticals.put(new Long(0x84), "ISERPPC");
    clanCriticals.put(new Long(0x85), "ISFlamer");

    clanCriticals.put(new Long(0x87), "ISLargeLaser");
    clanCriticals.put(new Long(0x88), "ISMediumLaser");
    clanCriticals.put(new Long(0x89), "ISSmallLaser");
    clanCriticals.put(new Long(0x8A), "ISPPC");
    clanCriticals.put(new Long(0x8B), "ISLargePulseLaser");
    clanCriticals.put(new Long(0x8C), "ISMediumPulseLaser");
    clanCriticals.put(new Long(0x8D), "ISSmallPulseLaser");
    clanCriticals.put(new Long(0x8E), "ISAC2");
    clanCriticals.put(new Long(0x8F), "ISAC5");
    clanCriticals.put(new Long(0x90), "ISAC10");
    clanCriticals.put(new Long(0x91), "ISAC20");
    clanCriticals.put(new Long(0x92), "ISAntiMissileSystem");
    //    clanCriticals.put(new Long(0x93), "CLLRTorpedo10");
    //    clanCriticals.put(new Long(0x94), "CLLRTorpedo15");
    //    clanCriticals.put(new Long(0x95), "CLLRTorpedo20");
    clanCriticals.put(new Long(0x96), "ISLightGaussRifle");
    clanCriticals.put(new Long(0x97), "ISGaussRifle");
    //    clanCriticals.put(new Long(0x98), "CLSRTorpedo6");

    clanCriticals.put(new Long(0x9B), "ISLBXAC2");
    clanCriticals.put(new Long(0x9C), "ISLBXAC5");
    clanCriticals.put(new Long(0x9D), "ISLBXAC10");
    clanCriticals.put(new Long(0x9E), "ISLBXAC20");
    clanCriticals.put(new Long(0x9F), "ISMachine Gun");

    clanCriticals.put(new Long(0xA4), "ISUltraAC2");
    clanCriticals.put(new Long(0xA5), "ISUltraAC5");
    clanCriticals.put(new Long(0xA6), "ISUltraAC10");
    clanCriticals.put(new Long(0xA7), "ISUltraAC20");
    clanCriticals.put(new Long(0xA8), "CLMicroPulseLaser");

    clanCriticals.put(new Long(0xAA), "ISERMediumLaser");
    clanCriticals.put(new Long(0xAB), "ISERSmallLaser");
    clanCriticals.put(new Long(0xAC), "ISAntiPersonnelPod");

    clanCriticals.put(new Long(0xAD), "CLLightMG");
    clanCriticals.put(new Long(0xAE), "CLHeavyMG");
    clanCriticals.put(new Long(0xAF), "CLLightActiveProbe");

    clanCriticals.put(new Long(0xB0), "ISLRM5");
    clanCriticals.put(new Long(0xB1), "ISLRM10");
    clanCriticals.put(new Long(0xB2), "ISLRM15");
    clanCriticals.put(new Long(0xB3), "ISLRM20");
    clanCriticals.put(new Long(0xB4), "CLLightTAG");

    clanCriticals.put(new Long(0xB6), "ISImprovedNarc");
    clanCriticals.put(new Long(0xB7), "ISSRM2");
    clanCriticals.put(new Long(0xB8), "ISSRM4");
    clanCriticals.put(new Long(0xB9), "ISSRM6");
    clanCriticals.put(new Long(0xBA), "ISStreakSRM2");
    clanCriticals.put(new Long(0xBB), "ISStreakSRM4");
    clanCriticals.put(new Long(0xBC), "ISStreakSRM6");

    clanCriticals.put(new Long(0xC3), "ISBeagleActiveProbe");
    clanCriticals.put(new Long(0xC5), "ISC3MasterComputer");
    clanCriticals.put(new Long(0xC6), "ISC3SlaveUnit");
    clanCriticals.put(new Long(0xC7), "ISImprovedC3CPU");
    clanCriticals.put(new Long(0xC8), "ISGuardianECM");
    clanCriticals.put(new Long(0xC9), "ISNarcBeacon");
    clanCriticals.put(new Long(0xCA), "ISTAG");

    clanCriticals.put(new Long(0xCB), "ISLRM5 (OS)");
    clanCriticals.put(new Long(0xCC), "ISLRM10 (OS)");
    clanCriticals.put(new Long(0xCD), "ISLRM15 (OS)");
    clanCriticals.put(new Long(0xCE), "ISLRM20 (OS)");
    clanCriticals.put(new Long(0xCF), "ISSRM2 (OS)");
    clanCriticals.put(new Long(0xD0), "ISSRM4 (OS)");
    clanCriticals.put(new Long(0xD1), "ISSRM6 (OS)");
    clanCriticals.put(new Long(0xD2), "ISStreakSRM2 (OS)");
    clanCriticals.put(new Long(0xD3), "ISStreakSRM4 (OS)");
    clanCriticals.put(new Long(0xD4), "ISStreakSRM6 (OS)");
    clanCriticals.put(new Long(0xD5), "ISFlamer (Vehicle)");

    clanCriticals.put(new Long(0xE2), "ISLRTorpedo5");
    clanCriticals.put(new Long(0xE3), "ISLRTorpedo10");
    clanCriticals.put(new Long(0xE4), "ISLRTorpedo15");
    clanCriticals.put(new Long(0xE5), "ISLRTorpedo20");
    clanCriticals.put(new Long(0xE6), "ISSRTorpedo2");
    clanCriticals.put(new Long(0xE7), "ISSRTorpedo4");
    clanCriticals.put(new Long(0xE8), "ISSRTorpedo6");

    clanCriticals.put(new Long(0xFC), "CLATM3");
    clanCriticals.put(new Long(0xFD), "CLATM6");
    clanCriticals.put(new Long(0xFE), "CLATM9");
    clanCriticals.put(new Long(0xFF), "CLATM12");


    clanCriticals.put(new Long(0x0121), "ISRotaryAC2");
    clanCriticals.put(new Long(0x0122), "ISRotaryAC5");

    //    clanCriticals.put(new Long(0x01ce), "CLAC2 Ammo");
    clanCriticals.put(new Long(0x01d0), "CLAMS Ammo");
    //    clanCriticals.put(new Long(0x01cf), "CLAC5 Ammo");
    clanCriticals.put(new Long(0x01d1), "CLGauss Ammo");
    clanCriticals.put(new Long(0x01d2), "CLLBXAC2 Ammo");
    clanCriticals.put(new Long(0x01d3), "CLLBXAC5 Ammo");
    clanCriticals.put(new Long(0x01d4), "CLLBXAC10 Ammo");
    clanCriticals.put(new Long(0x01d5), "CLLBXAC20 Ammo");
    clanCriticals.put(new Long(0x01d6), "CLMG Ammo");
    clanCriticals.put(new Long(0x01d7), "CLUltraAC2 Ammo");
    clanCriticals.put(new Long(0x01d8), "CLUltraAC5 Ammo");
    clanCriticals.put(new Long(0x01d9), "CLUltraAC10 Ammo");
    clanCriticals.put(new Long(0x01da), "CLUltraAC20 Ammo");
    clanCriticals.put(new Long(0x01db), "CLLRM5 Ammo");
    clanCriticals.put(new Long(0x01dc), "CLLRM10 Ammo");
    clanCriticals.put(new Long(0x01dd), "CLLRM15 Ammo");
    clanCriticals.put(new Long(0x01de), "CLLRM20 Ammo");
    clanCriticals.put(new Long(0x01df), "CLSRM2 Ammo");
    clanCriticals.put(new Long(0x01e0), "CLSRM4 Ammo");
    clanCriticals.put(new Long(0x01e1), "CLSRM6 Ammo");
    clanCriticals.put(new Long(0x01e2), "CLStreakSRM2 Ammo");
    clanCriticals.put(new Long(0x01e3), "CLStreakSRM4 Ammo");
    clanCriticals.put(new Long(0x01e4), "CLStreakSRM6 Ammo");
    clanCriticals.put(new Long(0x01e5), "CLArrowIV Ammo");
    clanCriticals.put(new Long(0x01e9), "CLNarc Pods");
    //    clanCriticals.put(new Long(0x0215), "CLFlamer Ammo");

    clanCriticals.put(new Long(0x01f0), "CLLRM5 Ammo");
    clanCriticals.put(new Long(0x01f1), "CLLRM10 Ammo");
    clanCriticals.put(new Long(0x01f2), "CLLRM15 Ammo");
    clanCriticals.put(new Long(0x01f3), "CLLRM20 Ammo");
    clanCriticals.put(new Long(0x01f6), "CLFlamer (Vehicle) Ammo");
    clanCriticals.put(new Long(0x01f7), "CLSRM2 Ammo");
    clanCriticals.put(new Long(0x01f8), "CLSniper Ammo");
    clanCriticals.put(new Long(0x01f9), "CLThumper Ammo");
    clanCriticals.put(new Long(0x01fa), "CLLRTorpedo5 Ammo");
    clanCriticals.put(new Long(0x01fb), "CLLRTorpedo10 Ammo");
    clanCriticals.put(new Long(0x01fc), "CLLRTorpedo15 Ammo");
    clanCriticals.put(new Long(0x01fd), "CLLRTorpedo20 Ammo");
    clanCriticals.put(new Long(0x01fe), "CLSRTorpedo2 Ammo");
    clanCriticals.put(new Long(0x01ff), "CLSRTorpedo4 Ammo");
    clanCriticals.put(new Long(0x0200), "CLSRTorpedo6 Ammo");

    clanCriticals.put(new Long(0x02B1), "ISRotaryAC2 Ammo");
    clanCriticals.put(new Long(0x02B2), "ISRotaryAC5 Ammo");
    clanCriticals.put(new Long(0x021E), "ISAC2 Ammo");
    clanCriticals.put(new Long(0x021F), "ISAC5 Ammo");
    clanCriticals.put(new Long(0x0220), "ISAC10 Ammo");
    clanCriticals.put(new Long(0x0221), "ISAC20 Ammo");
    clanCriticals.put(new Long(0x0222), "ISAMS Ammo");
    //    clanCriticals.put(new Long(0x0223), "CLLRTorpedo10 Ammo");
    //    clanCriticals.put(new Long(0x0224), "CLLRTorpedo15 Ammo");
    //    clanCriticals.put(new Long(0x0225), "CLLRTorpedo20 Ammo");
    clanCriticals.put(new Long(0x0226), "ISLightGauss Ammo");
    clanCriticals.put(new Long(0x0227), "ISGauss Ammo");
    //    clanCriticals.put(new Long(0x0228), "CLSRTorpedo6 Ammo");

    clanCriticals.put(new Long(0x022B), "ISLBXAC2 Ammo");
    clanCriticals.put(new Long(0x022C), "ISLBXAC5 Ammo");
    clanCriticals.put(new Long(0x022D), "ISLBXAC10 Ammo");
    clanCriticals.put(new Long(0x022E), "ISLBXAC20 Ammo");
    clanCriticals.put(new Long(0x022F), "ISMG Ammo");

    clanCriticals.put(new Long(0x0234), "ISUltraAC2 Ammo");
    clanCriticals.put(new Long(0x0235), "ISUltraAC5 Ammo");
    clanCriticals.put(new Long(0x0236), "ISUltraAC10 Ammo");
    clanCriticals.put(new Long(0x0237), "ISUltraAC20 Ammo");

    clanCriticals.put(new Long(0x023d), "CLLightMG Ammo");
    clanCriticals.put(new Long(0x023e), "CLHeavyMG Ammo");

    clanCriticals.put(new Long(0x0240), "ISLRM5 Ammo");
    clanCriticals.put(new Long(0x0241), "ISLRM10 Ammo");
    clanCriticals.put(new Long(0x0242), "ISLRM15 Ammo");
    clanCriticals.put(new Long(0x0243), "ISLRM20 Ammo");

    clanCriticals.put(new Long(0x0246), "ISiNarc Pods");
    clanCriticals.put(new Long(0x0247), "ISSRM2 Ammo");
    clanCriticals.put(new Long(0x0248), "ISSRM4 Ammo");
    clanCriticals.put(new Long(0x0249), "ISSRM6 Ammo");
    clanCriticals.put(new Long(0x024A), "ISStreakSRM2 Ammo");
    clanCriticals.put(new Long(0x024B), "ISStreakSRM4 Ammo");
    clanCriticals.put(new Long(0x024C), "ISStreakSRM6 Ammo");

    clanCriticals.put(new Long(0x0259), "ISNarc Pods");

    clanCriticals.put(new Long(0x0265), "ISFlamer (Vehicle) Ammo");

    clanCriticals.put(new Long(0x0272), "ISLRTorpedo15 Ammo");
    clanCriticals.put(new Long(0x0273), "ISLRTorpedo20 Ammo");
    clanCriticals.put(new Long(0x0274), "ISLRTorpedo5 Ammo");
    clanCriticals.put(new Long(0x0275), "ISLRTorpedo10 Ammo");
    clanCriticals.put(new Long(0x0276), "ISSRTorpedo4 Ammo");
    clanCriticals.put(new Long(0x0277), "ISSRTorpedo2 Ammo");
    clanCriticals.put(new Long(0x0278), "ISSRTorpedo6 Ammo");

    clanCriticals.put(new Long(0x028c), "CLATM3 Ammo");
    clanCriticals.put(new Long(0x028d), "CLATM6 Ammo");
    clanCriticals.put(new Long(0x028e), "CLATM9 Ammo");
    clanCriticals.put(new Long(0x028f), "CLATM12 Ammo");
  }

  private String getCriticalName(long critical)
  {
    return getCriticalName(new Long(critical));
  }

    private String getCriticalName(Long critical)  {
        // Critical slots are 4 bytes long.  The first two bytes are
        //  the type, the third is the ammo count, and I don't know
        //  what the fourth is.

        byte ammoCount = 0;
        if (critical.longValue() > Short.MAX_VALUE) {
            //Grab the ammo count from the third byte.
            ammoCount = (byte)((critical.longValue() >> 16) & 0xFF);
            //Mask off everything but the first two bytes.
            critical = new Long(critical.longValue() & 0xFFFF);
        }

        //At this point, the critical value is an unsigned integer.

        //First try "shared" criticals.
        String critName = (String) criticals.get(critical);

        if (critName == null) {
            TechType tType = techType;

            if (tType == TechType.MIXED) {
                if (critical.intValue() == 0x0A) {
                    tType = heatSinkTechType;
                } else if (critical.intValue() == 0x11
                           || critical.intValue() == 0x1F) {
                    tType = physicalWeaponTechType;
                } else if (critical.intValue() == 0x12) {
                    tType = targetingComputerTechType;
                } else if (critical.intValue() == 0x17) {
                    tType = myomerTechType;
                } else {
                    //Mixed tech mechs lookup most equipment using their "base" or
                    // "preferred" technology type.
                tType = mixedBaseTechType;
            }
                    }

            //Attempt to lookup equipment using the appropriate
            // tech type.
            Hashtable techCriticals = (Hashtable) criticals.get(tType);
            if (techCriticals != null) {
                critName = (String) techCriticals.get(critical);
            }
        }

        // MG ammo can come in half ton increments, so we have to look
        //  up the actual ammo count.  Other weapons have their counts
        //  hard-coded.
        if (critName != null && critName.endsWith("MG Ammo")) {
            critName += " (" + ammoCount + ")";
        }

        // Unexpected parsing failures should be passed on so that
        //  they can be dealt with properly.
        if ( critName == null ) {
            critName = "UnknownCritical(0x" + Integer.toHexString(critical.intValue()) + ")";
        }

        if ( critName == null && critical.longValue() == 0)
            return "-Empty-";

        return critName;
    }

/* OMIT_FOR_JHMPREAD_COMPILATION BLOCK_BEGIN */
    /**
     * This function moves all "empty" slots to the end of a location's
     * critical list.
     *
     * MegaMek adds equipment to the first empty slot available in a
     * location.  This means that any "holes" (empty slots not at the
     * end of a location), will cause the file crits and MegaMek's crits
     * to become out of sync.
     */
    private void compactCriticals(long[] criticals) {
        int firstEmpty = -1;
        for (int slot = 0; slot < criticals.length; slot++) {
            if (criticals[slot] == 0) {
                firstEmpty = slot;
            }
            if (firstEmpty != -1 && criticals[slot] != 0) {
                //move this to the first empty slot
                criticals[firstEmpty] = criticals[slot];
                //mark the old slot empty
                criticals[slot] = 0;
                //restart just after the moved slot's new location
                slot = firstEmpty;
                firstEmpty = -1;
            }
        }
    }
/* BLOCK_END */

    public String getMtf() {
        StringBuffer sb = new StringBuffer();
        String nl = "\r\n";  // DOS friendly

        // Write Output MTF
	sb.append( "Version:1.0" ).append( nl );
	sb.append( name ).append( nl );
	sb.append( model ).append( nl );
	sb.append( nl );

        sb.append( "Config:" ).append( chassisType );
	sb.append( nl );
        sb.append( "TechBase:" ).append( techType );
        if (techType == TechType.MIXED)
            sb.append( " (" ).append( mixedBaseTechType ).append( " Chassis)" );
        sb.append( nl );
	sb.append( "Era:" ).append( year ).append( nl );
	sb.append( "Rules Level:" ).append( rulesLevel );
	sb.append( nl );
        sb.append( nl );

        sb.append( "Mass:" ).append( tonnage ).append( nl );
	sb.append( "Engine:" ).append( engineRating ).append( " " )
            .append( engineType ).append( " Engine" );
        if (mixedBaseTechType != engineTechType)
            sb.append( " (" ).append(engineTechType).append( ")" );
        sb.append(nl);
        sb.append( "Structure:" ).append( internalStructureType ).append(nl);
	sb.append( "Myomer:" ).append( myomerType ).append( nl );
        sb.append( nl );

        sb.append( "Heat Sinks:" ).append( heatSinks ).append( " " )
            .append( heatSinkType ).append( nl );
	sb.append( "Walk MP:" ).append( walkMP ).append( nl );
	sb.append( "Jump MP:" ).append( jumpMP ).append( nl );
	sb.append( nl );

	sb.append( "Armor:" ).append( armorType );
        if (mixedBaseTechType != armorTechType)
            sb.append( " (" ).append(armorTechType).append( ")" );
        sb.append(nl);
        boolean isPatchwork = false;
        if (armorType == ArmorType.PATCHWORK) isPatchwork = true;
        sb.append("LA Armor:").append( laArmor );
        if (isPatchwork)
            sb.append( " (" ).append( laArmorType ).append( ")" );
        sb.append( nl );
        sb.append("RA Armor:").append( raArmor );
        if (isPatchwork)
            sb.append( " (" ).append( raArmorType ).append( ")" );
        sb.append( nl );
        sb.append("LT Armor:").append( ltArmor );
        if (isPatchwork)
            sb.append( " (" ).append( ltArmorType ).append( ")" );
        sb.append( nl );
        sb.append("RT Armor:").append( rtArmor );
        if (isPatchwork)
            sb.append( " (" ).append( rtArmorType ).append( ")" );
        sb.append( nl );
        sb.append("CT Armor:").append( ctArmor );
        if (isPatchwork)
            sb.append( " (" ).append( ctArmorType ).append( ")" );
        sb.append( nl );
        sb.append("HD Armor:").append( headArmor );
        if (isPatchwork)
            sb.append( " (" ).append( headArmorType ).append( ")" );
        sb.append( nl );
        sb.append("LL Armor:").append( llArmor );
        if (isPatchwork)
            sb.append( " (" ).append( llArmorType ).append( ")" );
        sb.append( nl );
        sb.append("RL Armor:").append( rlArmor );
        if (isPatchwork)
            sb.append( " (" ).append( rlArmorType ).append( ")" );
        sb.append( nl );
        sb.append("RTL Armor:").append( ltrArmor );
        if (isPatchwork)
            sb.append( " (" ).append( ltrArmorType ).append( ")" );
        sb.append( nl );
        sb.append("RTR Armor:").append( rtrArmor );
        if (isPatchwork)
            sb.append( " (" ).append( rtrArmorType ).append( ")" );
        sb.append( nl );
        sb.append("RTC Armor:").append( ctrArmor );
        if (isPatchwork)
            sb.append( " (" ).append( ctrArmorType ).append( ")" );
        sb.append( nl );
	sb.append( nl );

        sb.append( "Weapons:" ).append( totalWeaponCount ).append( nl );
        for( int x = 0; x < totalWeaponCount; x++ ) {
            sb.append(weaponArray[x][0]).append( " " )
                .append(getCriticalName(weaponArray[x][1]))
                .append( ", " )
                .append(WeaponLocation.getType(weaponArray[x][2]));
            if (weaponArray[x][3] > 0)
                sb.append( ", Ammo:" ).append(weaponArray[x][3]);
            sb.append( nl );
	}

        sb.append( nl );
	sb.append( "Left Arm:" ).append( nl );
	for( int x = 0; x < 12; x++ )
            sb.append(getCriticalName(laCriticals[x])).append( nl );
        sb.append( nl );

	sb.append( "Right Arm:" ).append( nl );
	for( int x = 0; x < 12; x++ )
            sb.append(getCriticalName(raCriticals[x])).append( nl );
        sb.append( nl );

        sb.append( "Left Torso:" ).append( nl );
	for( int x = 0; x < 12; x++ )
            sb.append(getCriticalName(ltCriticals[x])).append( nl );
        sb.append( nl );

        sb.append( "Right Torso:" ).append( nl );
	for( int x = 0; x < 12; x++ )
            sb.append(getCriticalName(rtCriticals[x])).append( nl );
        sb.append( nl );

        sb.append( "Center Torso:" ).append( nl );
	for( int x = 0; x < 12; x++ )
            sb.append(getCriticalName(ctCriticals[x])).append( nl );
        sb.append( nl );

        sb.append( "Head:" ).append( nl );
	for( int x = 0; x < 12; x++ )
            sb.append(getCriticalName(headCriticals[x])).append( nl );
        sb.append( nl );

        sb.append( "Left Leg:" ).append( nl );
	for( int x = 0; x < 12; x++ )
            sb.append(getCriticalName(llCriticals[x])).append( nl );
        sb.append( nl );

        sb.append( "Right Leg:" ).append( nl );
	for( int x = 0; x < 12; x++ )
            sb.append(getCriticalName(rlCriticals[x])).append( nl );

        return sb.toString();
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Hmpread (Java Edition) version 0.9");
            System.out.println("--------------------------------------");
            System.out.println();
            System.out.println("Drag and drop \".hmp\" files onto this exe to convert them to \".mtf\" files.\nMultiple files may be processed at once.  Files may also be specified on\nthe command line.");
            System.out.println();
            System.out.println("Press <enter> to quit...");
            try {
                System.in.read(); //pause
            } catch (Exception e) {
                //ignore
            }
            return;
        }
        for (int i = 0; i < args.length; i++) {
            String filename = args[i];
            if (!filename.endsWith(".hmp")) {
                System.out.println("Error: Input file must have Heavy Metal Pro extension '.hmp'");
                System.out.println();
                System.out.println("Press <enter> to quit...");
                try {
                    System.in.read(); //pause
                } catch (Exception e) {
                    //ignore
                }
                return;
            }
            HmpFile hmpFile = null;
            try {
                hmpFile = new HmpFile(new FileInputStream(args[i]));
            } catch (Exception e) {
                System.out.println(e);
                return;
            }
            filename = filename.substring(0, filename.lastIndexOf(".hmp"));
            filename += ".mtf";
            BufferedWriter out = null;
            try {
                out = new BufferedWriter(new FileWriter(new File(filename)));
                out.write(hmpFile.getMtf());
            } catch (Exception e) {
                System.out.println(e);
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException ex) {
                        //ignore
                    }
                }
            }
        }
    }
}

abstract class HMPType
{
    private String name;
    private int id;

    protected HMPType(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public String toString() {
        return name;
    }

    public boolean equals( Object other ) {

        // Assume the other object doesn't equal this one.
        boolean result = false;

        // References to the same object are equal.
        if ( this == other ) {
            result = true;
        }

        // If the other object is an instance of
        // this object's class, then recast it.
        else if ( this.getClass().isInstance(other) ) {
            HMPType cast = (HMPType) other;

            // The two objects match if their names and IDs match.
            if ( this.name.equals(cast.name) &&
                 this.id == cast.id ) {
                result = true;
            }
        }

        // Return the result
        return result;
    }
}

class DesignType
  extends HMPType
{
  public static final Hashtable types = new Hashtable();

  public static final DesignType STANDARD = new DesignType("Standard", 1);
  public static final DesignType MODIFIED = new DesignType("Modified", 2);
  public static final DesignType CUSTOM = new DesignType("Custom", 3);

  private DesignType(String name, int id)
  {
    super(name, id);
    types.put(new Integer(id), this);
  }

  public static DesignType getType(int i)
  {
    return (DesignType) types.get(new Integer(i));
  }
}

class ArmorType
  extends HMPType
{
  public static final Hashtable types = new Hashtable();

  public static final ArmorType STANDARD = new ArmorType("Standard", 0);
  public static final ArmorType FERRO_FIBROUS =
    new ArmorType("Ferro-Fibrous", 1);
  public static final ArmorType REACTIVE = new ArmorType("Reactive", 2);
  public static final ArmorType REFLECTIVE = new ArmorType("Reflective", 3);
  public static final ArmorType HARDENED = new ArmorType("Hardened", 4);
  public static final ArmorType LIGHT_FERRO_FIBROUS =
    new ArmorType("Light Ferro-Fibrous", 5);
  public static final ArmorType HEAVY_FERRO_FIBROUS =
    new ArmorType("Heavy Ferro-Fibrous", 6);
  public static final ArmorType PATCHWORK =
    new ArmorType("Patchwork", 7);
  public static final ArmorType STEALTH = new ArmorType("Stealth", 8);

  private ArmorType(String name, int id)
  {
    super(name, id);
    types.put(new Integer(id), this);
  }

  public static ArmorType getType(int i)
  {
    return (ArmorType) types.get(new Integer(i));
  }
}

class EngineType
  extends HMPType
{
  public static final Hashtable types = new Hashtable();

  public static final EngineType FUSION = new EngineType("Fusion", 0);
  public static final EngineType XL = new EngineType("XL", 1);
  public static final EngineType XXL = new EngineType("XXL", 2);
  public static final EngineType COMPACT = new EngineType("Compact", 3);
  public static final EngineType ICE = new EngineType("I.C.E", 4);
  public static final EngineType LIGHT = new EngineType("Light", 5);

  private EngineType(String name, int id)
  {
    super(name, id);
    types.put(new Integer(id), this);
  }

  public static EngineType getType(int i)
  {
    return (EngineType) types.get(new Integer(i));
  }
}

class HeatSinkType
  extends HMPType
{
  public static final Hashtable types = new Hashtable();

  public static final HeatSinkType SINGLE = new HeatSinkType("Single", 0);
  public static final HeatSinkType DOUBLE = new HeatSinkType("Double", 1);
  public static final HeatSinkType COMPACT = new HeatSinkType("Compact", 2);
  public static final HeatSinkType LASER = new HeatSinkType("Laser", 3);

  private HeatSinkType(String name, int id)
  {
    super(name, id);
    types.put(new Integer(id), this);
  }

  public static HeatSinkType getType(int i)
  {
    return (HeatSinkType) types.get(new Integer(i));
  }
}

class ChassisType
  extends HMPType
{
  public static final Hashtable types = new Hashtable();

  public static final ChassisType BIPED = new ChassisType("Biped", 0);
  public static final ChassisType QUADRAPED = new ChassisType("Quadraped", 1);
  public static final ChassisType LAM = new ChassisType("LAM", 2);
  public static final ChassisType ARMLESS = new ChassisType("Armless", 3);
  public static final ChassisType BIPED_OMNI =
    new ChassisType("Biped Omni", 10);
  public static final ChassisType QUADRAPED_OMNI =
    new ChassisType("Quadraped Omni", 11);

  private ChassisType(String name, int id)
  {
    super(name, id);
    types.put(new Integer(id), this);
  }

  public static ChassisType getType(int i)
  {
    return (ChassisType) types.get(new Integer(i));
  }
}

class InternalStructureType
  extends HMPType
{
  public static final Hashtable types = new Hashtable();

  public static final InternalStructureType STANDARD =
    new InternalStructureType("Standard", 0);
  public static final InternalStructureType ENDO_STEEL =
    new InternalStructureType("Endo Steel", 1);
  public static final InternalStructureType COMPOSITE =
    new InternalStructureType("Composite", 2);
  public static final InternalStructureType REINFORCED =
    new InternalStructureType("Reinforced", 3);
  public static final InternalStructureType UTILITY =
    new InternalStructureType("Utility", 4);

  private InternalStructureType(String name, int id)
  {
    super(name, id);
    types.put(new Integer(id), this);
  }

  public static InternalStructureType getType(int i)
  {
    return (InternalStructureType) types.get(new Integer(i));
  }
}

class TechType
  extends HMPType
{
  public static final Hashtable types = new Hashtable();

  public static final TechType INNER_SPHERE = new TechType("IS", 0);
  public static final TechType CLAN = new TechType("Clan", 1);
  public static final TechType MIXED = new TechType("Mixed", 2);

  private TechType(String name, int id)
  {
    super(name, id);
    types.put(new Integer(id), this);
  }

  public static TechType getType(int i)
  {
    return (TechType) types.get(new Integer(i));
  }
}

class MyomerType
  extends HMPType
{
  public static final Hashtable types = new Hashtable();

  public static final MyomerType STANDARD = new MyomerType("Standard", 0);
  public static final MyomerType TRIPLE_STRENGTH =
    new MyomerType("Triple-Strength", 1);
  public static final MyomerType MASC = new MyomerType("MASC", 2);

  private MyomerType(String name, int id)
  {
    super(name, id);
    types.put(new Integer(id), this);
  }

  public static MyomerType getType(int i)
  {
    return (MyomerType) types.get(new Integer(i));
  }
}

class WeaponLocation
  extends HMPType
{
  public static final Hashtable types = new Hashtable();

  public static final WeaponLocation LEFT_ARM =
    new WeaponLocation("Left Arm", 1);
  public static final WeaponLocation LEFT_TORSO =
    new WeaponLocation("Left Torso", 2);
  public static final WeaponLocation LEFT_LEG =
    new WeaponLocation("Left Leg", 3);
  public static final WeaponLocation RIGHT_ARM =
    new WeaponLocation("Right Arm", 4);
  public static final WeaponLocation RIGHT_TORSO =
    new WeaponLocation("Right Torso", 5);
  public static final WeaponLocation RIGHT_LEG =
    new WeaponLocation("Right Leg", 6);
  public static final WeaponLocation HEAD = new WeaponLocation("Head", 7);
  public static final WeaponLocation CENTER_TORSO =
    new WeaponLocation("Center Torso", 8);

  public static final WeaponLocation LEFT_TORSO_R =
    new WeaponLocation("Left Torso (R)", 11);
  public static final WeaponLocation LEFT_LEG_R =
    new WeaponLocation("Left Leg (R)", 12);
  public static final WeaponLocation RIGHT_TORSO_R =
    new WeaponLocation("Right Torso (R)", 14);
  public static final WeaponLocation RIGHT_LEG_R =
    new WeaponLocation("Right Leg (R)", 15);

  public static final WeaponLocation HEAD_R =
    new WeaponLocation("Head (R)", 16);
  public static final WeaponLocation CENTER_TORSO_R =
    new WeaponLocation("Center Torso (R)", 17);

  private WeaponLocation(String name, int id)
  {
    super(name, id);
    types.put(new Integer(id), this);
  }

  public static WeaponLocation getType(int i)
  {
    return (WeaponLocation) types.get(new Integer(i));
  }

}

