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

package megamek.common.loaders;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import java.util.Hashtable;
import java.util.Vector;

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

/**
 * Based on the hmpread.c program and the MtfFile object.  This class
 * can not load any Mixed tech or Level 3 Mechs or any vehicles.
 *
 * @author <a href="mailto:mnewcomb@sourceforge.net">Michael Newcomb</a>
 */
public class HmpFile
  implements MechLoader
{
  private String name;
  private String model;

  private ChassisType chassisType;
  private TechType techType;
  private int year;
  private int rulesLevel;

  private int tonnage;

  private int heatSinks;
  private HeatSinkType heatSinkType;

  private int walkMP;
  private int jumpMP;

  private int laArmor;
  private int ltArmor;
  private int ltrArmor;
  private int llArmor;

  private int raArmor;
  private int rtArmor;
  private int rtrArmor;
  private int rlArmor;

  private int headArmor;

  private int ctArmor;
  private int ctrArmor;

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
    throws EntityLoadingException
  {
    try
    {
      DataInputStream dis = new DataInputStream(is);

      byte[] buffer = new byte[5];
      dis.read(buffer);
      String version = new String(buffer);
	
      short designType = readUnsignedByte(dis);

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

      chassisType = ChassisType.getType(readUnsignedShort(dis));

      InternalStructureType internalStructureType =
        InternalStructureType.getType(readUnsignedShort(dis));

      int engineRating = readUnsignedShort(dis);

      EngineType engineType = EngineType.getType(readUnsignedShort(dis));

      walkMP = readUnsignedShort(dis);
      jumpMP = readUnsignedShort(dis);

      heatSinks = readUnsignedShort(dis);

      heatSinkType = HeatSinkType.getType(readUnsignedShort(dis));

      int armorType = readUnsignedShort(dis);

      // ??
      dis.skipBytes(2);

      laArmor = readUnsignedShort(dis);

      // ??
      dis.skipBytes(4);

      ltArmor = readUnsignedShort(dis);

      // ??
      dis.skipBytes(4);

      llArmor = readUnsignedShort(dis);

      // ??
      dis.skipBytes(4);

      raArmor = readUnsignedShort(dis);

      // ??
      dis.skipBytes(4);

      rtArmor = readUnsignedShort(dis);

      // ??
      dis.skipBytes(4);

      rlArmor = readUnsignedShort(dis);

      // ??
      dis.skipBytes(4);

      headArmor = readUnsignedShort(dis);

      // ??
      dis.skipBytes(4);

      ctArmor = readUnsignedShort(dis);

      // ??
      dis.skipBytes(2);

      ltrArmor = readUnsignedShort(dis);

      rtrArmor = readUnsignedShort(dis);

      ctrArmor = readUnsignedShort(dis);

      int myomerType = readUnsignedShort(dis);

      int weapons = readUnsignedShort(dis);
      for (int i = 1; i <= weapons; i++)
      {
        int weaponCount = readUnsignedShort(dis);
        int weaponType = readUnsignedShort(dis);
        WeaponLocation weaponLocation =
          WeaponLocation.getType(readUnsignedShort(dis));
        int weaponAmmo = readUnsignedShort(dis);

        // ??
        dis.skipBytes(2);

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
      throw new EntityLoadingException("I/O Error reading file");
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
      for (int i = 0; i < mech.getNumberOfCriticals(location); i++) {
     if (mech.getCritical(location, i) == null) {
        long critical = criticals[i];
        String criticalName = getCriticalName(critical, techType);

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
                        isSplit(critical, techType) ) {
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
                    criticals[i] = 0;
                    // Compact criticals again
                    compactCriticals(criticals);
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

  private static boolean isSplit(long critical, TechType techType) {
      // Only Heavy Gauss Rifles, Artillery, and AC20 weapons can be split.
      boolean result = false;
      long baseCrit = critical & 0x0000FFFF;
      if ( (TechType.INNER_SPHERE.equals(techType) &&   // Inner Sphere weapons
            (baseCrit == 0x41 ||  // AC20
             baseCrit == 0x4E ||  // LBXAC20
             baseCrit == 0x57 ||  // UltraAC20
             baseCrit == 0x71 ||  // ArrowIVSystem
             baseCrit == 0x87 ||  // SniperArtillery
             baseCrit == 0x88 ||  // ThumperArtillery
             baseCrit == 0x123)   // HeavyGaussRifle
            ) ||
           (TechType.CLAN.equals(techType) &&           // Clan weapons
            (baseCrit == 0x45 ||  // LBXAC20
             baseCrit == 0x4A ||  // UltraAC20
             baseCrit == 0x55 ||  // ArrowIVSystem
             baseCrit == 0x68 ||  // SniperArtillery
             baseCrit == 0x69)    // ThumperArtillery
            ) ) {
          result = ( (critical & 0xFFFF0000) != 0 );
      }
      return result;
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
    criticals.put(new Long(0x09), "Heat Sink");
    criticals.put(new Long(0x0B), "Jump Jet");
    criticals.put(new Long(0x11), "Hatchet");
    criticals.put(new Long(0x1F), "Sword");
    criticals.put(new Long(0x16), "Triple Strength Myomer");

    // inner sphere criticals
    //
    Hashtable isCriticals = new Hashtable();
    criticals.put(TechType.INNER_SPHERE, isCriticals);
    //    isCriticals.put(new Long(0x09), "ISDouble Heat Sink");
    isCriticals.put(new Long(0x0A), "ISDouble Heat Sink");
    isCriticals.put(new Long(0x12), "ISTargeting Computer");
    isCriticals.put(new Long(0x14), "Endo Steel");
    isCriticals.put(new Long(0x15), "Ferro-Fibrous");
    isCriticals.put(new Long(0x17), "ISMASC");
    isCriticals.put(new Long(0x18), "ISArtemisIV");
    isCriticals.put(new Long(0x19), "ISCASE");
    isCriticals.put(new Long(0x23), "Stealth Armor");
    isCriticals.put(new Long(0x33), "ISERLargeLaser");
    isCriticals.put(new Long(0x34), "ISERPPC");
    isCriticals.put(new Long(0x35), "ISFlamer");
    isCriticals.put(new Long(0x39), "ISSmallLaser");
    isCriticals.put(new Long(0x37), "ISLargeLaser");
    isCriticals.put(new Long(0x38), "ISMediumLaser");
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
    isCriticals.put(new Long(0x5A), "ISERMediumLaser");
    isCriticals.put(new Long(0x5B), "ISERSmallLaser");
    isCriticals.put(new Long(0x5C), "ISAntiPersonnelPod");
    isCriticals.put(new Long(0x60), "ISLRM5");
    isCriticals.put(new Long(0x61), "ISLRM10");
    isCriticals.put(new Long(0x62), "ISLRM15");
    isCriticals.put(new Long(0x63), "ISLRM20");
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
    isCriticals.put(new Long(0x121), "ISRotaryAC2");
    isCriticals.put(new Long(0x122), "ISRotaryAC5");
    isCriticals.put(new Long(0x123), "ISHeavyGaussRifle");
    isCriticals.put(new Long(0x129), "ISRocketLauncher10");
    isCriticals.put(new Long(0x12A), "ISRocketLauncher15");
    isCriticals.put(new Long(0x12B), "ISRocketLauncher20");
    isCriticals.put(new Long(0x01f0), "ISLRM5 Ammo");
    isCriticals.put(new Long(0x01f1), "ISLRM10 Ammo");
    isCriticals.put(new Long(0x01f2), "ISLRM15 Ammo");
    isCriticals.put(new Long(0x01f3), "ISLRM20 Ammo");
    isCriticals.put(new Long(0x01ce), "ISAC2 Ammo");
    isCriticals.put(new Long(0x01d0), "ISAC10 Ammo");
    isCriticals.put(new Long(0x01d2), "ISAMS Ammo");
    isCriticals.put(new Long(0x01cf), "ISAC5 Ammo");
    isCriticals.put(new Long(0x01d1), "ISAC20 Ammo");
    isCriticals.put(new Long(0x01d6), "ISLightGauss Ammo");
    isCriticals.put(new Long(0x01d7), "ISGauss Ammo");
    isCriticals.put(new Long(0x01db), "ISLBXAC2 Ammo");
    isCriticals.put(new Long(0x01dc), "ISLBXAC5 Ammo");
    isCriticals.put(new Long(0x01df), "ISMG Ammo");
    isCriticals.put(new Long(0x01e4), "ISUltraAC2 Ammo");
    isCriticals.put(new Long(0x01e5), "ISUltraAC5 Ammo");
    isCriticals.put(new Long(0x01e6), "ISUltraAC10 Ammo");
    isCriticals.put(new Long(0x01e7), "ISUltraAC20 Ammo");
    isCriticals.put(new Long(0x01dd), "ISLBXAC10 Ammo");
    isCriticals.put(new Long(0x01de), "ISLBXAC20 Ammo");
    isCriticals.put(new Long(0x01f6), "ISiNarc Pods");
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
    isCriticals.put(new Long(0x02b1), "ISRotaryAC2 Ammo");
    isCriticals.put(new Long(0x02b2), "ISRotaryAC5 Ammo");
    isCriticals.put(new Long(0x02b3), "ISHeavyGauss Ammo");
    isCriticals.put(new Long(0x01f7), "ISSRM2 Ammo");
    isCriticals.put(new Long(0x01f8), "ISSRM4 Ammo");
    isCriticals.put(new Long(0x01f9), "ISSRM6 Ammo");
    isCriticals.put(new Long(0x0224), "ISLRTorpedo15 Ammo");
    isCriticals.put(new Long(0x0225), "ISLRTorpedo20 Ammo");
    isCriticals.put(new Long(0x0222), "ISLRTorpedo5 Ammo");
    isCriticals.put(new Long(0x0223), "ISLRTorpedo10 Ammo");
    isCriticals.put(new Long(0x0227), "ISSRTorpedo4 Ammo");
    isCriticals.put(new Long(0x0226), "ISSRTorpedo2 Ammo");
    isCriticals.put(new Long(0x0228), "ISSRTorpedo6 Ammo");

    // clan criticals
    //
    Hashtable clanCriticals = new Hashtable();
    criticals.put(TechType.CLAN, clanCriticals);
    clanCriticals.put(new Long(0x0A), "CLDouble Heat Sink");
    clanCriticals.put(new Long(0x12), "CLTargeting Computer");
    clanCriticals.put(new Long(0x14), "Endo Steel");
    clanCriticals.put(new Long(0x15), "Ferro-Fibrous");
    clanCriticals.put(new Long(0x17), "CLMASC");
    clanCriticals.put(new Long(0x18), "CLArtemisIV");
    clanCriticals.put(new Long(0x33), "CLERLargeLaser");
    clanCriticals.put(new Long(0x34), "CLERMediumLaser");
    clanCriticals.put(new Long(0x35), "CLERSmallLaser");
    clanCriticals.put(new Long(0x36), "CLERPPC");
    clanCriticals.put(new Long(0x39), "CLSmallLaser");
    clanCriticals.put(new Long(0x37), "CLFlamer");
    clanCriticals.put(new Long(0x38), "CLMediumLaser");
    clanCriticals.put(new Long(0x3A), "CLPPC");
    clanCriticals.put(new Long(0x3C), "CLLargePulseLaser");
    clanCriticals.put(new Long(0x3D), "CLMediumPulseLaser");
    clanCriticals.put(new Long(0x3E), "CLSmallPulseLaser");
    clanCriticals.put(new Long(0x3F), "CLAC5");
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
    clanCriticals.put(new Long(0x85), "CLFlamer (Vehicle)");
    clanCriticals.put(new Long(0x92), "CLLRTorpedo5");
    clanCriticals.put(new Long(0x93), "CLLRTorpedo10");
    clanCriticals.put(new Long(0x94), "CLLRTorpedo15");
    clanCriticals.put(new Long(0x95), "CLLRTorpedo20");
    clanCriticals.put(new Long(0x96), "CLSRTorpedo2");
    clanCriticals.put(new Long(0x97), "CLSRTorpedo4");
    clanCriticals.put(new Long(0x98), "CLSRTorpedo6");
    clanCriticals.put(new Long(0xA8), "CLMicroPulseLaser");
    clanCriticals.put(new Long(0xAD), "CLLightMG");
    clanCriticals.put(new Long(0xAE), "CLHeavyMG");
    clanCriticals.put(new Long(0xAF), "CLLightActiveProbe");
    clanCriticals.put(new Long(0xB4), "CLLightTAG");
    clanCriticals.put(new Long(0xFC), "CLATM3");
    clanCriticals.put(new Long(0xFD), "CLATM6");
    clanCriticals.put(new Long(0xFE), "CLATM9");
    clanCriticals.put(new Long(0xFF), "CLATM12");
    clanCriticals.put(new Long(0x01f0), "CLLRM5 Ammo");
    clanCriticals.put(new Long(0x01f1), "CLLRM10 Ammo");
    clanCriticals.put(new Long(0x01f2), "CLLRM15 Ammo");
    clanCriticals.put(new Long(0x01f3), "CLLRM20 Ammo");
    clanCriticals.put(new Long(0x01ce), "CLAC2 Ammo");
    clanCriticals.put(new Long(0x01d0), "CLAMS Ammo");
    clanCriticals.put(new Long(0x01cf), "CLAC5 Ammo");
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
    clanCriticals.put(new Long(0x0215), "CLFlamer Ammo");
    clanCriticals.put(new Long(0x023d), "CLLightMG Ammo");
    clanCriticals.put(new Long(0x023e), "CLHeavyMG Ammo");
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
    clanCriticals.put(new Long(0x0224), "CLLRTorpedo15 Ammo");
    clanCriticals.put(new Long(0x0225), "CLLRTorpedo20 Ammo");
    clanCriticals.put(new Long(0x0222), "CLLRTorpedo5 Ammo");
    clanCriticals.put(new Long(0x0223), "CLLRTorpedo10 Ammo");
    clanCriticals.put(new Long(0x0227), "CLSRTorpedo4 Ammo");
    clanCriticals.put(new Long(0x0226), "CLSRTorpedo2 Ammo");
    clanCriticals.put(new Long(0x0228), "CLSRTorpedo6 Ammo");
    clanCriticals.put(new Long(0x028c), "CLATM3 Ammo");
    clanCriticals.put(new Long(0x028d), "CLATM6 Ammo");
    clanCriticals.put(new Long(0x028e), "CLATM9 Ammo");
    clanCriticals.put(new Long(0x028f), "CLATM12 Ammo");
  }

  private String getCriticalName(long critical, TechType techType)
  {
    return getCriticalName(new Long(critical), techType);
  }

    private String getCriticalName(Long critical, TechType techType)  {

        short thirdByte = 0;
        if (critical.longValue() > Short.MAX_VALUE) {
            thirdByte = (short)(critical.longValue() >> 16);
            critical = new Long(critical.longValue() & 0xFFFF);
        }
        final long value = critical.longValue();

        String critName = (String) criticals.get(critical);
        if (critName == null) {
            Hashtable techCriticals = (Hashtable) criticals.get(techType);
            if (techCriticals != null) {
                critName = (String) techCriticals.get(critical);
            }
        }

        // Lame kludge for MG ammo (which can come in half ton increments)
        if (critName != null && critName.endsWith("MG Ammo")) {
            critName += " (" + thirdByte + ")";
        }

        // Unexpected parsing failures should be passed on so that
        //  they can be dealt with properly.
        if ( critName == null &&
             value != 0  &&     // 0x00 Empty
             value != 7  &&     // 0x07 Lower Leg Actuator (on a quad)
             value != 8  &&     // 0x08 Foot Actuator (on a quad)
             value != 15 ) {    // 0x0F Fusion Engine
            critName = "UnknownCritical(0x" + Integer.toHexString(critical.intValue()) + ")";
        }

        if ( critName == null && value == 0)
            return "-Empty-";

        return critName;
    }

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

  public static void main(String[] args)
    throws Exception
  {
    for (int i = 0; i < args.length; i++)
    {
      HmpFile hmpFile = new HmpFile(new FileInputStream(args[i]));
      System.out.println(hmpFile.getEntity());
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
  public static final ArmorType STEALTH = new ArmorType("Stealth", 2);
  public static final ArmorType REACTIVE = new ArmorType("Reactive", 3);
  public static final ArmorType REFLECTIVE = new ArmorType("Reflective", 4);
  public static final ArmorType HARDENED = new ArmorType("Hardened", 5);
  public static final ArmorType LIGHT_FERRO_FIBROUS =
    new ArmorType("Light Ferror-Fibrous", 6);
  public static final ArmorType HEAVY_FERRO_FIBROUS =
    new ArmorType("Heavy Ferror-Fibrous", 7);
  public static final ArmorType PATCHWORK =
    new ArmorType("Patchwork", 8);

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
    new InternalStructureType("Biped", 0);
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

  public static final TechType INNER_SPHERE = new TechType("Inner Sphere", 0);
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
