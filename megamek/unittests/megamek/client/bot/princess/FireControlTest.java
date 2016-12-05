/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
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
package megamek.client.bot.princess;

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.BipedMech;
import megamek.common.BuildingTarget;
import megamek.common.ConvFighter;
import megamek.common.Coords;
import megamek.common.Crew;
import megamek.common.CriticalSlot;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.EntityMovementType;
import megamek.common.EntityWeightClass;
import megamek.common.EquipmentType;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.ITerrain;
import megamek.common.Infantry;
import megamek.common.LargeSupportTank;
import megamek.common.LosEffects;
import megamek.common.Mech;
import megamek.common.MechWarrior;
import megamek.common.Mounted;
import megamek.common.MovePath;
import megamek.common.MoveStep;
import megamek.common.Tank;
import megamek.common.TargetRollModifier;
import megamek.common.Targetable;
import megamek.common.Terrains;
import megamek.common.ToHitData;
import megamek.common.VTOL;
import megamek.common.WeaponType;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.util.StringUtil;
import megamek.common.weapons.ATMWeapon;
import megamek.common.weapons.MMLWeapon;
import megamek.common.weapons.StopSwarmAttack;
import megamek.server.SmokeCloud;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 *
 * @version %Id%
 * @lastEditBy Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 12/18/13 1:38 PM
 */
@RunWith(JUnit4.class)
public class FireControlTest {

    private static final int MOCK_TARGET_ID = 10;

    // AC5
    private WeaponType mockWeaponTypeAC5;
    @SuppressWarnings("FieldCanBeLocal")
    private AmmoType mockAmmoTypeAC5Std;
    private Mounted mockAmmoAC5Std;
    @SuppressWarnings("FieldCanBeLocal")
    private AmmoType mockAmmoTypeAC5Flak;
    private Mounted mockAmmoAC5Flak;
    @SuppressWarnings("FieldCanBeLocal")
    private AmmoType mockAmmoTypeAC5Incendiary;
    private Mounted mockAmmoAc5Incendiary;
    @SuppressWarnings("FieldCanBeLocal")
    private AmmoType mockAmmoTypeAc5Flechette;
    private Mounted mockAmmoAc5Flechette;

    // LB10X
    private WeaponType mockLB10X;
    @SuppressWarnings("FieldCanBeLocal")
    private AmmoType mockAmmoTypeLB10XSlug;
    private Mounted mockAmmoLB10XSlug;
    @SuppressWarnings("FieldCanBeLocal")
    private AmmoType mockAmmoTypeLB10XCluster;
    private Mounted mockAmmoLB10XCluster;

    // MML
    private WeaponType mockMML5;
    @SuppressWarnings("FieldCanBeLocal")
    private AmmoType mockAmmoTypeSRM5;
    private Mounted mockAmmoSRM5;
    @SuppressWarnings("FieldCanBeLocal")
    private AmmoType mockAmmoTypeLRM5;
    private Mounted mockAmmoLRM5;
    @SuppressWarnings("FieldCanBeLocal")
    private AmmoType mockAmmoTypeInferno5;
    private Mounted mockAmmoInfero5;
    @SuppressWarnings("FieldCanBeLocal")
    private AmmoType mockAmmoTypeLrm5Frag;
    private Mounted mockAmmoLrm5Frag;

    // ATM
    private WeaponType mockAtm5;
    @SuppressWarnings("FieldCanBeLocal")
    private AmmoType mockAmmoTypeAtm5He;
    private Mounted mockAmmoAtm5He;
    @SuppressWarnings("FieldCanBeLocal")
    private AmmoType mockAmmoTypeAtm5St;
    private Mounted mockAmmoAtm5St;
    @SuppressWarnings("FieldCanBeLocal")
    private AmmoType mockAmmoTypeAtm5Er;
    private Mounted mockAmmoAtm5Er;
    @SuppressWarnings("FieldCanBeLocal")
    private AmmoType mockAmmoTypeAtm5Inferno;
    private Mounted mockAmmoAtm5Inferno;

    private Entity mockTarget;
    private EntityState mockTargetState;
    @SuppressWarnings("FieldCanBeLocal")
    private ToHitData mockTargetMoveMod;
    private Coords mockTargetCoords;

    private Entity mockShooter;
    private Coords mockShooterCoords;
    private EntityState mockShooterState;
    @SuppressWarnings("FieldCanBeLocal")
    private ToHitData mockShooterMoveMod;
    private Crew mockCrew;

    private GameOptions mockGameOptions;
    private IHex mockHex;
    private IBoard mockBoard;
    private IGame mockGame;

    private Princess mockPrincess;

    private ArrayList<Mounted> shooterWeapons;
    private Mounted mockPPC;
    private Mounted mockML;
    private Mounted mockLRM5;
    private WeaponFireInfo mockPPCFireInfo;
    private WeaponFireInfo mockMLFireInfo;
    private WeaponFireInfo mockLRMFireInfo;

    private Map<Mounted, Double> testToHitThreshold;

    private FireControl testFireControl;


    @Before
    public void setUp() {
        mockPrincess = Mockito.mock(Princess.class);

        BehaviorSettings mockBehavior = Mockito.mock(BehaviorSettings.class);
        Mockito.when(mockPrincess.getBehaviorSettings()).thenReturn(mockBehavior);

        BasicPathRanker mockPathRanker = Mockito.mock(BasicPathRanker.class);
        Mockito.when(mockPrincess.getPathRanker()).thenReturn(mockPathRanker);

        IHonorUtil mockHonorUtil = Mockito.mock(IHonorUtil.class);
        Mockito.when(mockPrincess.getHonorUtil()).thenReturn(mockHonorUtil);

        mockShooter = Mockito.mock(BipedMech.class);
        Mockito.when(mockShooter.getId()).thenReturn(1);
        Mockito.when(mockShooter.getMaxWeaponRange()).thenReturn(21);
        Mockito.when(mockShooter.getHeatCapacity()).thenReturn(10);
        Mockito.when(mockShooter.getHeat()).thenReturn(0);
        mockShooterState = Mockito.mock(EntityState.class);
        mockShooterCoords = new Coords(0, 0);
        Mockito.when(mockShooterState.getPosition()).thenReturn(mockShooterCoords);
        mockShooterMoveMod = new ToHitData();

        mockCrew = Mockito.mock(Crew.class);
        Mockito.when(mockCrew.getPiloting()).thenReturn(5);
        Mockito.when(mockCrew.getGunnery()).thenReturn(4);
        Mockito.when(mockShooter.getCrew()).thenReturn(mockCrew);

        mockTargetState = Mockito.mock(EntityState.class);
        Mockito.when(mockTargetState.isBuilding()).thenReturn(false);
        Mockito.when(mockTargetState.getHeat()).thenReturn(0);
        mockTargetMoveMod = new ToHitData();
        mockTargetCoords = new Coords(10, 0);
        Mockito.when(mockTargetState.getPosition()).thenReturn(mockTargetCoords);

        mockGameOptions = Mockito.mock(GameOptions.class);

        mockHex = Mockito.mock(IHex.class);

        mockBoard = Mockito.mock(IBoard.class);
        Mockito.when(mockBoard.getHex(Mockito.any(Coords.class))).thenReturn(mockHex);
        Mockito.when(mockBoard.contains(Mockito.any(Coords.class))).thenReturn(true);


        mockGame = Mockito.mock(IGame.class);
        Mockito.when(mockGame.getOptions()).thenReturn(mockGameOptions);
        Mockito.when(mockGame.getBoard()).thenReturn(mockBoard);

        mockTarget = Mockito.mock(BipedMech.class);
        Mockito.when(mockTarget.getDisplayName()).thenReturn("mock target");
        Mockito.when(mockTarget.getId()).thenReturn(MOCK_TARGET_ID);
        Mockito.when(mockTarget.isMilitary()).thenReturn(true);

        testFireControl = Mockito.spy(new FireControl(mockPrincess));
        Mockito.doReturn(mockShooterMoveMod)
               .when(testFireControl)
               .getAttackerMovementModifier(Mockito.any(IGame.class), Mockito.anyInt(),
                                            Mockito.any(EntityMovementType.class));
        Mockito.doReturn(mockTargetMoveMod)
               .when(testFireControl)
               .getTargetMovementModifier(Mockito.anyInt(), Mockito.anyBoolean(), Mockito.anyBoolean(),
                                          Mockito.any(IGame.class));

        // AC5
        mockWeaponTypeAC5 = Mockito.mock(WeaponType.class);
        Mockito.when(mockWeaponTypeAC5.getAmmoType()).thenReturn(AmmoType.T_AC);
        mockAmmoTypeAC5Std = Mockito.mock(AmmoType.class);
        Mockito.when(mockAmmoTypeAC5Std.getAmmoType()).thenReturn(AmmoType.T_AC);
        Mockito.when(mockAmmoTypeAC5Std.getMunitionType()).thenReturn(AmmoType.M_STANDARD);
        mockAmmoAC5Std = Mockito.mock(Mounted.class);
        Mockito.when(mockAmmoAC5Std.getType()).thenReturn(mockAmmoTypeAC5Std);
        Mockito.when(mockAmmoAC5Std.isAmmoUsable()).thenReturn(true);
        mockAmmoTypeAC5Flak = Mockito.mock(AmmoType.class);
        Mockito.when(mockAmmoTypeAC5Flak.getAmmoType()).thenReturn(AmmoType.T_AC);
        Mockito.when(mockAmmoTypeAC5Flak.getMunitionType()).thenReturn(AmmoType.M_FLAK);
        mockAmmoAC5Flak = Mockito.mock(Mounted.class);
        Mockito.when(mockAmmoAC5Flak.getType()).thenReturn(mockAmmoTypeAC5Flak);
        Mockito.when(mockAmmoAC5Flak.isAmmoUsable()).thenReturn(true);
        mockAmmoTypeAC5Incendiary = Mockito.mock(AmmoType.class);
        Mockito.when(mockAmmoTypeAC5Incendiary.getMunitionType()).thenReturn(AmmoType.M_INCENDIARY_AC);
        Mockito.when(mockAmmoTypeAC5Incendiary.getAmmoType()).thenReturn(AmmoType.T_AC);
        mockAmmoAc5Incendiary = Mockito.mock(Mounted.class);
        Mockito.when(mockAmmoAc5Incendiary.getType()).thenReturn(mockAmmoTypeAC5Incendiary);
        Mockito.when(mockAmmoAc5Incendiary.isAmmoUsable()).thenReturn(true);
        mockAmmoTypeAc5Flechette = Mockito.mock(AmmoType.class);
        Mockito.when(mockAmmoTypeAc5Flechette.getAmmoType()).thenReturn(AmmoType.T_AC);
        Mockito.when(mockAmmoTypeAc5Flechette.getMunitionType()).thenReturn(AmmoType.M_FLECHETTE);
        mockAmmoAc5Flechette = Mockito.mock(Mounted.class);
        Mockito.when(mockAmmoAc5Flechette.getType()).thenReturn(mockAmmoTypeAc5Flechette);
        Mockito.when(mockAmmoAc5Flechette.isAmmoUsable()).thenReturn(true);

        // LB10X
        mockLB10X = Mockito.mock(WeaponType.class);
        mockAmmoTypeLB10XSlug = Mockito.mock(AmmoType.class);
        mockAmmoLB10XSlug = Mockito.mock(Mounted.class);
        mockAmmoTypeLB10XCluster = Mockito.mock(AmmoType.class);
        mockAmmoLB10XCluster = Mockito.mock(Mounted.class);
        Mockito.when(mockLB10X.getAmmoType()).thenReturn(AmmoType.T_AC_LBX);
        Mockito.when(mockAmmoTypeLB10XSlug.getAmmoType()).thenReturn(AmmoType.T_AC_LBX);
        Mockito.when(mockAmmoTypeLB10XSlug.getMunitionType()).thenReturn(AmmoType.M_STANDARD);
        Mockito.when(mockAmmoLB10XSlug.getType()).thenReturn(mockAmmoTypeLB10XSlug);
        Mockito.when(mockAmmoLB10XSlug.isAmmoUsable()).thenReturn(true);
        Mockito.when(mockAmmoTypeLB10XCluster.getAmmoType()).thenReturn(AmmoType.T_AC_LBX);
        Mockito.when(mockAmmoTypeLB10XCluster.getMunitionType()).thenReturn(AmmoType.M_CLUSTER);
        Mockito.when(mockAmmoLB10XCluster.getType()).thenReturn(mockAmmoTypeLB10XCluster);
        Mockito.when(mockAmmoLB10XCluster.isAmmoUsable()).thenReturn(true);

        // MML
        mockMML5 = Mockito.mock(MMLWeapon.class);
        mockAmmoTypeSRM5 = Mockito.mock(AmmoType.class);
        mockAmmoSRM5 = Mockito.mock(Mounted.class);
        mockAmmoTypeLRM5 = Mockito.mock(AmmoType.class);
        mockAmmoLRM5 = Mockito.mock(Mounted.class);
        mockAmmoTypeInferno5 = Mockito.mock(AmmoType.class);
        mockAmmoInfero5 = Mockito.mock(Mounted.class);
        mockAmmoTypeLrm5Frag = Mockito.mock(AmmoType.class);
        mockAmmoLrm5Frag = Mockito.mock(Mounted.class);
        Mockito.when(mockMML5.getAmmoType()).thenReturn(AmmoType.T_MML);
        Mockito.when(mockAmmoTypeSRM5.getMunitionType()).thenReturn(AmmoType.M_STANDARD);
        Mockito.when(mockAmmoTypeSRM5.getAmmoType()).thenReturn(AmmoType.T_MML);
        Mockito.when(mockAmmoSRM5.getType()).thenReturn(mockAmmoTypeSRM5);
        Mockito.when(mockAmmoSRM5.isAmmoUsable()).thenReturn(true);
        Mockito.when(mockAmmoTypeLRM5.getMunitionType()).thenReturn(AmmoType.M_STANDARD);
        Mockito.when(mockAmmoTypeLRM5.hasFlag(Mockito.any(BigInteger.class))).thenReturn(false);
        Mockito.when(mockAmmoTypeLRM5.hasFlag(Mockito.eq(AmmoType.F_MML_LRM))).thenReturn(true);
        Mockito.when(mockAmmoTypeLRM5.getAmmoType()).thenReturn(AmmoType.T_MML);
        Mockito.when(mockAmmoLRM5.getType()).thenReturn(mockAmmoTypeLRM5);
        Mockito.when(mockAmmoLRM5.isAmmoUsable()).thenReturn(true);
        Mockito.when(mockAmmoTypeInferno5.getMunitionType()).thenReturn(AmmoType.M_INFERNO);
        Mockito.when(mockAmmoTypeInferno5.getAmmoType()).thenReturn(AmmoType.T_MML);
        Mockito.when(mockAmmoInfero5.getType()).thenReturn(mockAmmoTypeInferno5);
        Mockito.when(mockAmmoInfero5.isAmmoUsable()).thenReturn(true);
        Mockito.when(mockAmmoTypeLrm5Frag.getMunitionType()).thenReturn(AmmoType.M_FRAGMENTATION);
        Mockito.when(mockAmmoTypeLrm5Frag.hasFlag(Mockito.eq(AmmoType.F_MML_LRM))).thenReturn(true);
        Mockito.when(mockAmmoTypeLrm5Frag.getAmmoType()).thenReturn(AmmoType.T_MML);
        Mockito.when(mockAmmoLrm5Frag.getType()).thenReturn(mockAmmoTypeLrm5Frag);
        Mockito.when(mockAmmoLrm5Frag.isAmmoUsable()).thenReturn(true);

        // ATM
        mockAtm5 = Mockito.mock(ATMWeapon.class);
        mockAmmoTypeAtm5He = Mockito.mock(AmmoType.class);
        mockAmmoAtm5He = Mockito.mock(Mounted.class);
        mockAmmoTypeAtm5St = Mockito.mock(AmmoType.class);
        mockAmmoAtm5St = Mockito.mock(Mounted.class);
        mockAmmoTypeAtm5Er = Mockito.mock(AmmoType.class);
        mockAmmoAtm5Er = Mockito.mock(Mounted.class);
        mockAmmoTypeAtm5Inferno = Mockito.mock(AmmoType.class);
        mockAmmoAtm5Inferno = Mockito.mock(Mounted.class);
        Mockito.when(mockAtm5.getAmmoType()).thenReturn(AmmoType.T_ATM);
        Mockito.when(mockAtm5.getRackSize()).thenReturn(5);
        Mockito.when(mockAmmoTypeAtm5He.getAmmoType()).thenReturn(AmmoType.T_ATM);
        Mockito.when(mockAmmoTypeAtm5He.getMunitionType()).thenReturn(AmmoType.M_HIGH_EXPLOSIVE);
        Mockito.when(mockAmmoTypeAtm5He.getRackSize()).thenReturn(5);
        Mockito.when(mockAmmoAtm5He.getType()).thenReturn(mockAmmoTypeAtm5He);
        Mockito.when(mockAmmoAtm5He.isAmmoUsable()).thenReturn(true);
        Mockito.when(mockAmmoTypeAtm5St.getMunitionType()).thenReturn(AmmoType.M_STANDARD);
        Mockito.when(mockAmmoTypeAtm5St.getAmmoType()).thenReturn(AmmoType.T_ATM);
        Mockito.when(mockAmmoTypeAtm5St.getRackSize()).thenReturn(5);
        Mockito.when(mockAmmoAtm5St.getType()).thenReturn(mockAmmoTypeAtm5St);
        Mockito.when(mockAmmoAtm5St.isAmmoUsable()).thenReturn(true);
        Mockito.when(mockAmmoTypeAtm5Er.getMunitionType()).thenReturn(AmmoType.M_EXTENDED_RANGE);
        Mockito.when(mockAmmoTypeAtm5Er.getAmmoType()).thenReturn(AmmoType.T_ATM);
        Mockito.when(mockAmmoTypeAtm5Er.getRackSize()).thenReturn(5);
        Mockito.when(mockAmmoAtm5Er.getType()).thenReturn(mockAmmoTypeAtm5Er);
        Mockito.when(mockAmmoAtm5Er.isAmmoUsable()).thenReturn(true);
        Mockito.when(mockAmmoTypeAtm5Inferno.getMunitionType()).thenReturn(AmmoType.M_IATM_IIW);
        Mockito.when(mockAmmoTypeAtm5Inferno.getAmmoType()).thenReturn(AmmoType.T_ATM);
        Mockito.when(mockAmmoTypeAtm5Inferno.getRackSize()).thenReturn(5);
        Mockito.when(mockAmmoAtm5Inferno.getType()).thenReturn(mockAmmoTypeAtm5Inferno);
        Mockito.when(mockAmmoAtm5Inferno.isAmmoUsable()).thenReturn(true);

        shooterWeapons = new ArrayList<>(3);
        Mockito.when(mockShooter.getWeaponList()).thenReturn(shooterWeapons);

        mockPPC = Mockito.mock(Mounted.class);
        shooterWeapons.add(mockPPC);
        mockPPCFireInfo = Mockito.mock(WeaponFireInfo.class);
        Mockito.when(mockPPCFireInfo.getProbabilityToHit()).thenReturn(0.5);
        Mockito.doReturn(mockPPCFireInfo).when(testFireControl).buildWeaponFireInfo(Mockito.any(Entity.class),
                                                                                    Mockito.any(EntityState.class),
                                                                                    Mockito.any(Targetable.class),
                                                                                    Mockito.any(EntityState.class),
                                                                                    Mockito.eq(mockPPC),
                                                                                    Mockito.any(IGame.class),
                                                                                    Mockito.anyBoolean());
        Mockito.doReturn(mockPPCFireInfo).when(testFireControl).buildWeaponFireInfo(Mockito.any(Entity.class),
                                                                                    Mockito.any(MovePath.class),
                                                                                    Mockito.any(Targetable.class),
                                                                                    Mockito.any(EntityState.class),
                                                                                    Mockito.eq(mockPPC),
                                                                                    Mockito.any(IGame.class),
                                                                                    Mockito.anyBoolean(),
                                                                                    Mockito.anyBoolean());
        Mockito.doReturn(mockPPCFireInfo).when(testFireControl).buildWeaponFireInfo(Mockito.any(Entity.class),
                                                                                    Mockito.any(Targetable.class),
                                                                                    Mockito.eq(mockPPC),
                                                                                    Mockito.any(IGame.class),
                                                                                    Mockito.anyBoolean());

        mockML = Mockito.mock(Mounted.class);
        shooterWeapons.add(mockML);
        mockMLFireInfo = Mockito.mock(WeaponFireInfo.class);
        Mockito.when(mockMLFireInfo.getProbabilityToHit()).thenReturn(0.0);
        Mockito.doReturn(mockMLFireInfo).when(testFireControl).buildWeaponFireInfo(Mockito.any(Entity.class),
                                                                                   Mockito.any(EntityState.class),
                                                                                   Mockito.any(Targetable.class),
                                                                                   Mockito.any(EntityState.class),
                                                                                   Mockito.eq(mockML),
                                                                                   Mockito.any(IGame.class),
                                                                                   Mockito.anyBoolean());
        Mockito.doReturn(mockMLFireInfo).when(testFireControl).buildWeaponFireInfo(Mockito.any(Entity.class),
                                                                                   Mockito.any(MovePath.class),
                                                                                   Mockito.any(Targetable.class),
                                                                                   Mockito.any(EntityState.class),
                                                                                   Mockito.eq(mockML),
                                                                                   Mockito.any(IGame.class),
                                                                                   Mockito.anyBoolean(),
                                                                                   Mockito.anyBoolean());
        Mockito.doReturn(mockMLFireInfo).when(testFireControl).buildWeaponFireInfo(Mockito.any(Entity.class),
                                                                                   Mockito.any(Targetable.class),
                                                                                   Mockito.eq(mockML),
                                                                                   Mockito.any(IGame.class),
                                                                                   Mockito.anyBoolean());

        mockLRM5 = Mockito.mock(Mounted.class);
        shooterWeapons.add(mockLRM5);
        mockLRMFireInfo = Mockito.mock(WeaponFireInfo.class);
        Mockito.when(mockLRMFireInfo.getProbabilityToHit()).thenReturn(0.6);
        Mockito.doReturn(mockLRMFireInfo).when(testFireControl).buildWeaponFireInfo(Mockito.any(Entity.class),
                                                                                    Mockito.any(EntityState.class),
                                                                                    Mockito.any(Targetable.class),
                                                                                    Mockito.any(EntityState.class),
                                                                                    Mockito.eq(mockLRM5),
                                                                                    Mockito.any(IGame.class),
                                                                                    Mockito.anyBoolean());
        Mockito.doReturn(mockLRMFireInfo).when(testFireControl).buildWeaponFireInfo(Mockito.any(Entity.class),
                                                                                    Mockito.any(MovePath.class),
                                                                                    Mockito.any(Targetable.class),
                                                                                    Mockito.any(EntityState.class),
                                                                                    Mockito.eq(mockLRM5),
                                                                                    Mockito.any(IGame.class),
                                                                                    Mockito.anyBoolean(),
                                                                                    Mockito.anyBoolean());
        Mockito.doReturn(mockLRMFireInfo).when(testFireControl).buildWeaponFireInfo(Mockito.any(Entity.class),
                                                                                    Mockito.any(Targetable.class),
                                                                                    Mockito.eq(mockLRM5),
                                                                                    Mockito.any(IGame.class),
                                                                                    Mockito.anyBoolean());


        testToHitThreshold = new HashMap<>();
        for (Mounted weapon : mockShooter.getWeaponList()) {
            testToHitThreshold.put(weapon, 0.0);
        }
    }


    @Test
    public void testGetHardTargetAmmo() {

        // Test an ammo list with only 1 bin of standard ammo.
        List<Mounted> testAmmoList = new ArrayList<>(1);
        testAmmoList.add(mockAmmoAC5Std);
        FireControl testFireControl = new FireControl(mockPrincess);
        Assert.assertEquals(mockAmmoAC5Std, testFireControl.getHardTargetAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Test an ammo list with only 1 bin of flak ammo.
        testAmmoList = new ArrayList<>(1);
        testAmmoList.add(mockAmmoAC5Flak);
        Assert.assertNull(testFireControl.getHardTargetAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Test an ammo list with 1 each of standard and flak.
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoAC5Flak);
        testAmmoList.add(mockAmmoAC5Std);
        Assert.assertEquals(mockAmmoAC5Std, testFireControl.getHardTargetAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Test LBX weaponry.
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoLB10XCluster);
        testAmmoList.add(mockAmmoLB10XSlug);
        Assert.assertEquals(mockAmmoLB10XSlug, testFireControl.getHardTargetAmmo(testAmmoList, mockLB10X, 5));

        // Test MMLs
        testAmmoList = new ArrayList<>(3);
        testAmmoList.add(mockAmmoLRM5);
        testAmmoList.add(mockAmmoSRM5);
        testAmmoList.add(mockAmmoInfero5);
        Assert.assertEquals(mockAmmoSRM5, testFireControl.getHardTargetAmmo(testAmmoList, mockMML5, 4));
        Assert.assertEquals(mockAmmoLRM5, testFireControl.getHardTargetAmmo(testAmmoList, mockMML5, 8));
        Assert.assertEquals(mockAmmoLRM5, testFireControl.getHardTargetAmmo(testAmmoList, mockMML5, 10));

        // Test MMLs without LRMs.
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoSRM5);
        testAmmoList.add(mockAmmoInfero5);
        Assert.assertEquals(mockAmmoSRM5, testFireControl.getHardTargetAmmo(testAmmoList, mockMML5, 4));
        Assert.assertEquals(mockAmmoSRM5, testFireControl.getHardTargetAmmo(testAmmoList, mockMML5, 8));
        Assert.assertNull(testFireControl.getHardTargetAmmo(testAmmoList, mockMML5, 10));

        // Test MMLs without SRMs.
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoLRM5);
        testAmmoList.add(mockAmmoInfero5);
        Assert.assertEquals(mockAmmoLRM5, testFireControl.getHardTargetAmmo(testAmmoList, mockMML5, 4));
        Assert.assertEquals(mockAmmoLRM5, testFireControl.getHardTargetAmmo(testAmmoList, mockMML5, 8));
        Assert.assertEquals(mockAmmoLRM5, testFireControl.getHardTargetAmmo(testAmmoList, mockMML5, 10));
    }

    @Test
    public void testGetAntiAirAmmo() {

        // Test an ammo list with only 1 bin.
        List<Mounted> testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoAC5Std);
        FireControl testFireControl = new FireControl(mockPrincess);
        Assert.assertNull(testFireControl.getAntiAirAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Add the flak ammo.
        testAmmoList.add(mockAmmoAC5Flak);
        Assert.assertEquals(mockAmmoAC5Flak, testFireControl.getAntiAirAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Test a list with 2 bins of standard and 0 flak ammo.
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoAC5Std);
        testAmmoList.add(mockAmmoAC5Std);
        Assert.assertNull(testFireControl.getAntiAirAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Test LBX weaponry.
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoLB10XCluster);
        testAmmoList.add(mockAmmoLB10XSlug);
        Assert.assertEquals(mockAmmoLB10XCluster, testFireControl.getAntiAirAmmo(testAmmoList, mockLB10X, 5));
    }

    @Test
    public void testGetClusterAmmo() {

        // Test an ammo list with only 1 bin of cluster ammo.
        List<Mounted> testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoLB10XCluster);
        FireControl testFireControl = new FireControl(mockPrincess);
        Assert.assertEquals(mockAmmoLB10XCluster, testFireControl.getClusterAmmo(testAmmoList, mockLB10X, 5));

        // Test an ammo list with only 1 bin of slug ammo.
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoLB10XSlug);
        testFireControl = new FireControl(mockPrincess);
        Assert.assertNull(testFireControl.getClusterAmmo(testAmmoList, mockLB10X, 5));

        // Test with both loaded
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoLB10XCluster);
        testAmmoList.add(mockAmmoLB10XSlug);
        Assert.assertEquals(mockAmmoLB10XCluster, testFireControl.getClusterAmmo(testAmmoList, mockLB10X, 5));
    }

    @Test
    public void testGetHeatAmmo() {

        // Test an ammo list with only 1 bin of incendiary ammo.
        List<Mounted> testAmmoList = new ArrayList<>(1);
        testAmmoList.add(mockAmmoAc5Incendiary);
        FireControl testFireControl = new FireControl(mockPrincess);
        Assert.assertEquals(mockAmmoAc5Incendiary, testFireControl.getIncendiaryAmmo(testAmmoList, mockWeaponTypeAC5,
                                                                                     5));

        // Test an ammo list with only 1 bin of standard ammo.
        testAmmoList = new ArrayList<>(1);
        testAmmoList.add(mockAmmoAC5Std);
        Assert.assertNull(testFireControl.getIncendiaryAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Test a list with multiple types of ammo.
        testAmmoList = new ArrayList<>(3);
        testAmmoList.add(mockAmmoAC5Std);
        testAmmoList.add(mockAmmoAc5Incendiary);
        testAmmoList.add(mockAmmoAC5Flak);
        Assert.assertEquals(mockAmmoAc5Incendiary, testFireControl.getIncendiaryAmmo(testAmmoList, mockWeaponTypeAC5,
                                                                                     5));

        // Test LBX
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoLB10XCluster);
        testAmmoList.add(mockAmmoLB10XSlug);
        Assert.assertNull(testFireControl.getIncendiaryAmmo(testAmmoList, mockLB10X, 5));

        // Test MMLs
        testAmmoList = new ArrayList<>(3);
        testAmmoList.add(mockAmmoLRM5);
        testAmmoList.add(mockAmmoSRM5);
        testAmmoList.add(mockAmmoInfero5);
        Assert.assertEquals(mockAmmoInfero5, testFireControl.getIncendiaryAmmo(testAmmoList, mockMML5, 4));
        Assert.assertEquals(mockAmmoInfero5, testFireControl.getIncendiaryAmmo(testAmmoList, mockMML5, 8));
        Assert.assertNull(testFireControl.getIncendiaryAmmo(testAmmoList, mockMML5, 10));
    }

    @Test
    public void testGetAntiInfantryAmmo() {

        // Test an ammo list with only 1 bin of flechette ammo.
        List<Mounted> testAmmoList = new ArrayList<>(1);
        testAmmoList.add(mockAmmoAc5Flechette);
        FireControl testFireControl = new FireControl(mockPrincess);
        Assert.assertEquals(mockAmmoAc5Flechette, testFireControl.getAntiInfantryAmmo(testAmmoList,
                                                                                      mockWeaponTypeAC5, 5));

        // Test an ammo list with only 1 bin of standard ammo.
        testAmmoList = new ArrayList<>(1);
        testAmmoList.add(mockAmmoAC5Std);
        Assert.assertNull(testFireControl.getAntiInfantryAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Test a list with multiple types of ammo.
        testAmmoList = new ArrayList<>(3);
        testAmmoList.add(mockAmmoAC5Std);
        testAmmoList.add(mockAmmoAC5Flak);
        testAmmoList.add(mockAmmoAc5Flechette);
        Assert.assertEquals(mockAmmoAc5Flechette, testFireControl.getAntiInfantryAmmo(testAmmoList,
                                                                                      mockWeaponTypeAC5, 5));

        // Test LBX
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoLB10XCluster);
        testAmmoList.add(mockAmmoLB10XSlug);
        Assert.assertEquals(mockAmmoLB10XCluster, testFireControl.getAntiInfantryAmmo(testAmmoList, mockLB10X, 5));

        // Test MMLs
        testAmmoList = new ArrayList<>(4);
        testAmmoList.add(mockAmmoLRM5);
        testAmmoList.add(mockAmmoSRM5);
        testAmmoList.add(mockAmmoInfero5);
        testAmmoList.add(mockAmmoLrm5Frag);
        Assert.assertEquals(mockAmmoInfero5, testFireControl.getAntiInfantryAmmo(testAmmoList, mockMML5, 4));
        Assert.assertEquals(mockAmmoLrm5Frag, testFireControl.getAntiInfantryAmmo(testAmmoList, mockMML5, 8));
        Assert.assertEquals(mockAmmoLrm5Frag, testFireControl.getAntiInfantryAmmo(testAmmoList, mockMML5, 10));
    }

    @Test
    public void testGetAntiVeeAmmo() {

        // Test an ammo list with only 1 bin of standard ammo.
        List<Mounted> testAmmoList = new ArrayList<>(1);
        testAmmoList.add(mockAmmoAC5Std);
        FireControl testFireControl = new FireControl(mockPrincess);
        Assert.assertNull(testFireControl.getAntiVeeAmmo(testAmmoList, mockWeaponTypeAC5, 5, false));

        // Test an ammo list with only 1 bin of incendiary ammo.
        testAmmoList = new ArrayList<>(1);
        testAmmoList.add(mockAmmoAc5Incendiary);
        testFireControl = new FireControl(mockPrincess);
        Assert.assertNull(testFireControl.getAntiVeeAmmo(testAmmoList, mockWeaponTypeAC5, 5, false));

        // Test a list with multiple types of ammo.
        testAmmoList = new ArrayList<>(3);
        testAmmoList.add(mockAmmoAC5Std);
        testAmmoList.add(mockAmmoAc5Incendiary);
        testAmmoList.add(mockAmmoAC5Flak);
        Assert.assertNull(testFireControl.getAntiVeeAmmo(testAmmoList, mockWeaponTypeAC5, 5, true));
        Assert.assertNull(testFireControl.getAntiVeeAmmo(testAmmoList, mockWeaponTypeAC5, 5, false));

        // Test LBX
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoLB10XCluster);
        testAmmoList.add(mockAmmoLB10XSlug);
        Assert.assertEquals(mockAmmoLB10XCluster, testFireControl.getAntiVeeAmmo(testAmmoList, mockLB10X, 5, false));

        // Test MMLs
        testAmmoList = new ArrayList<>(4);
        testAmmoList.add(mockAmmoLRM5);
        testAmmoList.add(mockAmmoSRM5);
        testAmmoList.add(mockAmmoInfero5);
        testAmmoList.add(mockAmmoLrm5Frag);
        Assert.assertEquals(mockAmmoInfero5, testFireControl.getAntiVeeAmmo(testAmmoList, mockMML5, 4, false));
        Assert.assertEquals(mockAmmoInfero5, testFireControl.getAntiVeeAmmo(testAmmoList, mockMML5, 8, false));
        Assert.assertNull(testFireControl.getAntiVeeAmmo(testAmmoList, mockMML5, 4, true));
        Assert.assertNull(testFireControl.getAntiVeeAmmo(testAmmoList, mockMML5, 8, true));
        Assert.assertNull(testFireControl.getAntiVeeAmmo(testAmmoList, mockMML5, 10, false));
    }

    @Test
    public void testGetAtmAmmo() {

        // Test a list with just HE ammo.
        List<Mounted> testAmmoList = new ArrayList<>(1);
        testAmmoList.add(mockAmmoAtm5He);
        FireControl testFireControl = new FireControl(mockPrincess);
        Assert.assertEquals(mockAmmoAtm5He, testFireControl.getAtmAmmo(testAmmoList, 5, mockTargetState, false));
        Assert.assertNull(testFireControl.getAtmAmmo(testAmmoList, 15, mockTargetState, false));

        // Test a list with just Standard ammo.
        testAmmoList = new ArrayList<>(1);
        testAmmoList.add(mockAmmoAtm5St);
        Assert.assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 5, mockTargetState, false));
        Assert.assertNull(testFireControl.getAtmAmmo(testAmmoList, 20, mockTargetState, false));

        // Test a list with just ER ammo.
        testAmmoList = new ArrayList<>(1);
        testAmmoList.add(mockAmmoAtm5Er);
        Assert.assertEquals(mockAmmoAtm5Er, testFireControl.getAtmAmmo(testAmmoList, 5, mockTargetState, false));

        // Test a list with all 3 ammo types
        testAmmoList = new ArrayList<>(3);
        testAmmoList.add(mockAmmoAtm5He);
        testAmmoList.add(mockAmmoAtm5Er);
        testAmmoList.add(mockAmmoAtm5St);
        Assert.assertEquals(mockAmmoAtm5Er, testFireControl.getAtmAmmo(testAmmoList, 20, mockTargetState, false));
        Assert.assertEquals(mockAmmoAtm5Er, testFireControl.getAtmAmmo(testAmmoList, 12, mockTargetState, false));
        Assert.assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 8, mockTargetState, false));
        Assert.assertEquals(mockAmmoAtm5He, testFireControl.getAtmAmmo(testAmmoList, 6, mockTargetState, false));
        Assert.assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 5, mockTargetState, false));
        Assert.assertEquals(mockAmmoAtm5He, testFireControl.getAtmAmmo(testAmmoList, 3, mockTargetState, false));

        // Test a list with just HE and Standard ammo types.
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoAtm5He);
        testAmmoList.add(mockAmmoAtm5St);
        Assert.assertNull(testFireControl.getAtmAmmo(testAmmoList, 20, mockTargetState, false));
        Assert.assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 12, mockTargetState, false));
        Assert.assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 8, mockTargetState, false));
        Assert.assertEquals(mockAmmoAtm5He, testFireControl.getAtmAmmo(testAmmoList, 6, mockTargetState, false));
        Assert.assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 5, mockTargetState, false));
        Assert.assertEquals(mockAmmoAtm5He, testFireControl.getAtmAmmo(testAmmoList, 3, mockTargetState, false));

        // Test a list with just HE and ER ammo types.
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoAtm5He);
        testAmmoList.add(mockAmmoAtm5Er);
        Assert.assertEquals(mockAmmoAtm5Er, testFireControl.getAtmAmmo(testAmmoList, 20, mockTargetState, false));
        Assert.assertEquals(mockAmmoAtm5Er, testFireControl.getAtmAmmo(testAmmoList, 12, mockTargetState, false));
        Assert.assertEquals(mockAmmoAtm5Er, testFireControl.getAtmAmmo(testAmmoList, 8, mockTargetState, false));
        Assert.assertEquals(mockAmmoAtm5He, testFireControl.getAtmAmmo(testAmmoList, 6, mockTargetState, false));
        Assert.assertEquals(mockAmmoAtm5He, testFireControl.getAtmAmmo(testAmmoList, 5, mockTargetState, false));
        Assert.assertEquals(mockAmmoAtm5He, testFireControl.getAtmAmmo(testAmmoList, 3, mockTargetState, false));

        // Test a list with just Standard and ER ammo types.
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoAtm5St);
        testAmmoList.add(mockAmmoAtm5Er);
        Assert.assertEquals(mockAmmoAtm5Er, testFireControl.getAtmAmmo(testAmmoList, 20, mockTargetState, false));
        Assert.assertEquals(mockAmmoAtm5Er, testFireControl.getAtmAmmo(testAmmoList, 12, mockTargetState, false));
        Assert.assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 8, mockTargetState, false));
        Assert.assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 6, mockTargetState, false));
        Assert.assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 5, mockTargetState, false));
        Assert.assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 3, mockTargetState, false));

        // Test targets that should be hit with infernos.
        Mockito.when(mockTargetState.isBuilding()).thenReturn(true);
        testAmmoList.add(mockAmmoAtm5He);
        testAmmoList.add(mockAmmoAtm5Er);
        testAmmoList.add(mockAmmoAtm5St);
        testAmmoList.add(mockAmmoAtm5Inferno);
        Assert.assertEquals(mockAmmoAtm5Inferno, testFireControl.getAtmAmmo(testAmmoList, 8, mockTargetState, false));
        Assert.assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 8, mockTargetState, true));
        Mockito.when(mockTargetState.isBuilding()).thenReturn(false);
        Mockito.when(mockTargetState.getHeat()).thenReturn(9);
        Assert.assertEquals(mockAmmoAtm5Inferno, testFireControl.getAtmAmmo(testAmmoList, 8, mockTargetState, false));
        Assert.assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 8, mockTargetState, true));
        Mockito.when(mockTargetState.getHeat()).thenReturn(0);
    }

    @Test
    public void testGetGeneralMmlAmmo() {

        // Test a list with just SRM ammo.
        List<Mounted> testAmmoList = new ArrayList<>(1);
        testAmmoList.add(mockAmmoSRM5);
        FireControl testFireControl = new FireControl(mockPrincess);
        Assert.assertEquals(mockAmmoSRM5, testFireControl.getGeneralMmlAmmo(testAmmoList, 6));
        Assert.assertNull(testFireControl.getGeneralMmlAmmo(testAmmoList, 10));

        // Test a list with just LRM ammo.
        testAmmoList = new ArrayList<>(1);
        testAmmoList.add(mockAmmoLRM5);
        Assert.assertEquals(mockAmmoLRM5, testFireControl.getGeneralMmlAmmo(testAmmoList, 10));
        Assert.assertEquals(mockAmmoLRM5, testFireControl.getGeneralMmlAmmo(testAmmoList, 3));

        // Test a list with both types of ammo.
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoLRM5);
        testAmmoList.add(mockAmmoSRM5);
        Assert.assertEquals(mockAmmoLRM5, testFireControl.getGeneralMmlAmmo(testAmmoList, 10));
        Assert.assertEquals(mockAmmoLRM5, testFireControl.getGeneralMmlAmmo(testAmmoList, 6));
        Assert.assertEquals(mockAmmoSRM5, testFireControl.getGeneralMmlAmmo(testAmmoList, 4));
    }

    @Test
    public void testGetPreferredAmmo() {
        Entity mockShooter = Mockito.mock(BipedMech.class);
        Targetable mockTarget = Mockito.mock(BipedMech.class);
        Mockito.when(((Entity) mockTarget).getArmorType(Mockito.anyInt())).thenReturn(EquipmentType.T_ARMOR_STANDARD);
        FireControl testFireControl = new FireControl(mockPrincess);

        Crew mockCrew = Mockito.mock(Crew.class);
        Mockito.when(mockShooter.getCrew()).thenReturn(mockCrew);
        Mockito.when(((Entity) mockTarget).getCrew()).thenReturn(mockCrew);

        PilotOptions mockOptions = Mockito.mock(PilotOptions.class);
        Mockito.when(mockCrew.getOptions()).thenReturn(mockOptions);
        Mockito.when(mockOptions.booleanOption(Mockito.anyString())).thenReturn(false);

        ArrayList<Mounted> testAmmoList = new ArrayList<>(5);
        testAmmoList.add(mockAmmoAtm5He);
        testAmmoList.add(mockAmmoAtm5Er);
        testAmmoList.add(mockAmmoAtm5St);
        testAmmoList.add(mockAmmoAC5Std);
        testAmmoList.add(mockAmmoAC5Flak);
        testAmmoList.add(mockAmmoAc5Flechette);
        testAmmoList.add(mockAmmoAc5Incendiary);
        testAmmoList.add(mockAmmoLB10XCluster);
        testAmmoList.add(mockAmmoLB10XSlug);
        testAmmoList.add(mockAmmoSRM5);
        testAmmoList.add(mockAmmoInfero5);
        testAmmoList.add(mockAmmoLRM5);
        Mockito.when(mockShooter.getAmmo()).thenReturn(testAmmoList);
        Mockito.when(mockShooter.getPosition()).thenReturn(new Coords(10, 10));

        // Test ATMs
        Mockito.when(mockTarget.getPosition()).thenReturn(new Coords(10, 30));
        Assert.assertEquals(mockAmmoAtm5Er, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockAtm5));
        Mockito.when(mockTarget.getPosition()).thenReturn(new Coords(10, 22));
        Assert.assertEquals(mockAmmoAtm5Er, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockAtm5));
        Mockito.when(mockTarget.getPosition()).thenReturn(new Coords(10, 18));
        Assert.assertEquals(mockAmmoAtm5St, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockAtm5));
        Mockito.when(mockTarget.getPosition()).thenReturn(new Coords(10, 16));
        Assert.assertEquals(mockAmmoAtm5He, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockAtm5));
        Mockito.when(mockTarget.getPosition()).thenReturn(new Coords(10, 15));
        Assert.assertEquals(mockAmmoAtm5St, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockAtm5));
        Mockito.when(mockTarget.getPosition()).thenReturn(new Coords(10, 13));
        Assert.assertEquals(mockAmmoAtm5He, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockAtm5));

        // Test shooting an AC5 at a building.
        mockTarget = Mockito.mock(BuildingTarget.class);
        Mockito.when(mockTarget.getPosition()).thenReturn(new Coords(10, 15));
        Assert.assertEquals(mockAmmoAc5Incendiary, testFireControl.getPreferredAmmo(mockShooter, mockTarget,
                                                                                    mockWeaponTypeAC5));

        // Test shooting an LBX at an airborne target.
        mockTarget = Mockito.mock(VTOL.class);
        Mockito.when(((Entity) mockTarget).getArmorType(Mockito.anyInt())).thenReturn(EquipmentType.T_ARMOR_STANDARD);
        Mockito.when(mockTarget.getPosition()).thenReturn(new Coords(10, 15));
        Mockito.when(mockTarget.isAirborne()).thenReturn(true);
        Assert.assertEquals(mockAmmoLB10XCluster, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockLB10X));

        // Test shooting an LBX at a tank.
        mockTarget = Mockito.mock(Tank.class);
        Mockito.when(((Entity) mockTarget).getArmorType(Mockito.anyInt())).thenReturn(EquipmentType.T_ARMOR_STANDARD);
        Mockito.when(mockTarget.getPosition()).thenReturn(new Coords(10, 15));
        Assert.assertEquals(mockAmmoLB10XCluster, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockLB10X));

        // Test shooting an AC at infantry.
        mockTarget = Mockito.mock(Infantry.class);
        Mockito.when(((Entity) mockTarget).getArmorType(Mockito.anyInt())).thenReturn(EquipmentType.T_ARMOR_STANDARD);
        Mockito.when(mockTarget.getPosition()).thenReturn(new Coords(10, 15));
        Assert.assertTrue(
                mockAmmoAc5Flechette.equals(testFireControl.getPreferredAmmo(mockShooter, mockTarget,
                                                                             mockWeaponTypeAC5))
                || mockAmmoAc5Incendiary.equals(testFireControl.getPreferredAmmo(mockShooter, mockTarget,
                                                                                 mockWeaponTypeAC5)));

        // Test a LBX at a heavily damaged target.
        mockTarget = Mockito.mock(BipedMech.class);
        Mockito.when(((Entity) mockTarget).getArmorType(Mockito.anyInt())).thenReturn(EquipmentType.T_ARMOR_STANDARD);
        Mockito.when(mockTarget.getPosition()).thenReturn(new Coords(10, 15));
        Mockito.when(((Entity) mockTarget).getDamageLevel()).thenReturn(Entity.DMG_HEAVY);
        Assert.assertEquals(mockAmmoLB10XCluster, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockLB10X));

        // Test a hot target.
        Mockito.when(((Entity) mockTarget).getDamageLevel()).thenReturn(Entity.DMG_LIGHT);
        Mockito.when(((Entity) mockTarget).getHeat()).thenReturn(12);
        Assert.assertEquals(mockAmmoInfero5, testFireControl.getPreferredAmmo(mockShooter, mockTarget,
                                                                              mockMML5));
        Mockito.when(((Entity) mockTarget).getArmorType(Mockito.anyInt()))
               .thenReturn(EquipmentType.T_ARMOR_HEAT_DISSIPATING);
        Assert.assertEquals(mockAmmoSRM5, testFireControl.getPreferredAmmo(mockShooter, mockTarget,
                                                                           mockMML5));
        Mockito.when(((Entity) mockTarget).getArmorType(Mockito.anyInt())).thenReturn(EquipmentType.T_ARMOR_STANDARD);

        // Test a normal target.
        Mockito.when(((Entity) mockTarget).getHeat()).thenReturn(4);
        Assert.assertEquals(mockAmmoAC5Std, testFireControl.getPreferredAmmo(mockShooter, mockTarget,
                                                                             mockWeaponTypeAC5));
        Assert.assertEquals(mockAmmoSRM5, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockMML5));
    }

    @Test
    public void testGuessToHitModifierHelperForAnyAttack() {

        // Test the most vanilla case we can.
        Mockito.when(mockShooterState.isProne()).thenReturn(false);
        Mockito.when(mockShooter.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_POS_ANTI_AIR))).thenReturn(false);
        Mockito.when(mockTargetState.isImmobile()).thenReturn(false);
        Mockito.when(mockTargetState.getMovementType()).thenReturn(EntityMovementType.MOVE_NONE);
        Mockito.when(mockTargetState.getPosition()).thenReturn(new Coords(10, 0));
        Mockito.when(mockTargetState.isProne()).thenReturn(false);
        Mockito.when(mockTarget.isAirborne()).thenReturn(false);
        Mockito.when(mockTarget.isAirborneVTOLorWIGE()).thenReturn(false);
        Mockito.when(mockGameOptions.booleanOption(Mockito.eq(OptionsConstants.ADVGRNDMOV_TACOPS_STANDING_STILL)))
               .thenReturn(false);
        Mockito.when(mockHex.terrainLevel(Terrains.WOODS)).thenReturn(ITerrain.LEVEL_NONE);
        Mockito.when(mockHex.terrainLevel(Terrains.JUNGLE)).thenReturn(ITerrain.LEVEL_NONE);
        ToHitData expected = new ToHitData();
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));

        // Test ground units firing on airborne aeros.
        ConvFighter mockFighter = Mockito.mock(ConvFighter.class);
        Mockito.when(mockFighter.isNOE()).thenReturn(true);
        EntityState mockFighterState = Mockito.mock(EntityState.class);
        Mockito.when(mockFighterState.isAirborneAero()).thenReturn(true);
        Mockito.when(mockFighterState.isImmobile()).thenReturn(false);
        Mockito.when(mockFighterState.getMovementType()).thenReturn(EntityMovementType.MOVE_SAFE_THRUST);
        Mockito.when(mockFighterState.getPosition()).thenReturn(new Coords(10, 0));
        Mockito.when(mockFighterState.isProne()).thenReturn(false);
        Mockito.doReturn(new Coords(0, 2)).when(testFireControl).getNearestPointInFlightPath(Mockito.any(Coords.class),
                                                                                             Mockito.any(Aero.class));
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_TAR_AERO_NOE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockFighter,
                                                                                             mockFighterState,
                                                                                             10,
                                                                                             mockGame));
        Mockito.doReturn(new Coords(0, 1)).when(testFireControl).getNearestPointInFlightPath(Mockito.any(Coords.class),
                                                                                             Mockito.any(Aero.class));
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_TAR_AERO_NOE_ADJ);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockFighter,
                                                                                             mockFighterState,
                                                                                             10,
                                                                                             mockGame));

        // Test industrial mechs.
        Mockito.when(((Mech) mockShooter).getCockpitType()).thenReturn(Mech.COCKPIT_INDUSTRIAL);
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_INDUSTRIAL);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        Mockito.when(((Mech) mockShooter).getCockpitType()).thenReturn(Mech.COCKPIT_PRIMITIVE_INDUSTRIAL);
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_PRIMATIVE_INDUSTRIAL);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        Mockito.when(((Mech) mockShooter).getCockpitType()).thenReturn(Mech.COCKPIT_STANDARD);

        // Test attacking a superheavy mech.
        Mockito.when(((Mech) mockTarget).getCockpitType()).thenReturn(Mech.COCKPIT_SUPERHEAVY);
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_TAR_SUPER);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        Mockito.when(((Mech) mockTarget).getCockpitType()).thenReturn(Mech.COCKPIT_STANDARD);

        // Test attacking a grounded dropship.
        Dropship mockDropship = Mockito.mock(Dropship.class);
        Mockito.when(mockDropship.isAirborne()).thenReturn(false);
        Mockito.when(mockDropship.isAirborneVTOLorWIGE()).thenReturn(false);
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_TAR_GROUND_DS);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockDropship,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));

        // Test the shooter having a null position.
        Mockito.when(mockShooterState.getPosition()).thenReturn(null);
        expected = new ToHitData(FireControl.TH_NULL_POSITION);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        Mockito.when(mockShooterState.getPosition()).thenReturn(new Coords(0, 0));

        // Test the target having a null position.
        Mockito.when(mockTargetState.getPosition()).thenReturn(null);
        expected = new ToHitData(FireControl.TH_NULL_POSITION);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        Mockito.when(mockTargetState.getPosition()).thenReturn(new Coords(10, 0));

        // Make the shooter prone.
        Mockito.when(mockShooterState.isProne()).thenReturn(true);
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_ATT_PRONE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        Mockito.when(mockShooterState.isProne()).thenReturn(false);

        // Make the target immobile.
        Mockito.when(mockTargetState.isImmobile()).thenReturn(true);
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_TAR_IMMOBILE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        Mockito.when(mockTargetState.isImmobile()).thenReturn(false);

        // Have the target fall prone adjacent.
        Mockito.when(mockTargetState.isProne()).thenReturn(true);
        Mockito.when(mockTargetState.getPosition()).thenReturn(new Coords(0, 1));
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_TAR_PRONE_ADJ);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             1,
                                                                                             mockGame));
        Mockito.when(mockTargetState.getPosition()).thenReturn(new Coords(10, 0)); // Move the target away.
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_TAR_PRONE_RANGE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        Mockito.when(mockTargetState.getMovementType()).thenReturn(EntityMovementType.MOVE_SKID); // Have the target
        // skid.
        expected.addModifier(FireControl.TH_TAR_SKID);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        Mockito.when(mockTargetState.isProne()).thenReturn(false);
        Mockito.when(mockTargetState.getMovementType()).thenReturn(EntityMovementType.MOVE_NONE);

        // Turn on Tac-Ops Standing Still rules.
        Mockito.when(mockGameOptions.booleanOption(Mockito.eq(OptionsConstants.ADVGRNDMOV_TACOPS_STANDING_STILL)))
               .thenReturn(true);
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_TAR_NO_MOVE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        Mockito.when(mockTargetState.getMovementType()).thenReturn(EntityMovementType.MOVE_WALK); // Walking target.
        expected = new ToHitData();
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        Mockito.when(mockGameOptions.booleanOption(Mockito.eq(OptionsConstants.ADVGRNDMOV_TACOPS_STANDING_STILL)))
               .thenReturn(false);
        Mockito.when(mockTargetState.getMovementType()).thenReturn(EntityMovementType.MOVE_NONE);

        // Have the target sprint.
        Mockito.when(mockTargetState.getMovementType()).thenReturn(EntityMovementType.MOVE_SPRINT);
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_TAR_SPRINT);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        Mockito.when(mockTargetState.getMovementType()).thenReturn(EntityMovementType.MOVE_NONE);

        // Stand the target in light woods.
        Mockito.when(mockHex.terrainLevel(Terrains.WOODS)).thenReturn(1);
        expected = new ToHitData();
        expected.addModifier(1, FireControl.TH_WOODS);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        Mockito.when(mockHex.terrainLevel(Terrains.WOODS)).thenReturn(ITerrain.LEVEL_NONE);

        // Stand the target in heavy woods.
        Mockito.when(mockHex.terrainLevel(Terrains.WOODS)).thenReturn(2);
        expected = new ToHitData();
        expected.addModifier(2, FireControl.TH_WOODS);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        Mockito.when(mockHex.terrainLevel(Terrains.WOODS)).thenReturn(ITerrain.LEVEL_NONE);

        // Stand the target in super heavy woods.
        Mockito.when(mockHex.terrainLevel(Terrains.WOODS)).thenReturn(3);
        expected = new ToHitData();
        expected.addModifier(3, FireControl.TH_WOODS);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        Mockito.when(mockHex.terrainLevel(Terrains.WOODS)).thenReturn(ITerrain.LEVEL_NONE);

        // Stand the target in jungle.
        Mockito.when(mockHex.terrainLevel(Terrains.JUNGLE)).thenReturn(2);
        expected = new ToHitData();
        expected.addModifier(2, FireControl.TH_WOODS);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        Mockito.when(mockHex.terrainLevel(Terrains.JUNGLE)).thenReturn(ITerrain.LEVEL_NONE);

        // Give the shooter the anti-air quirk but fire on a ground target.
        Mockito.when(mockShooter.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_POS_ANTI_AIR))).thenReturn(true);
        expected = new ToHitData();
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        Mockito.when(mockShooter.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_POS_ANTI_AIR))).thenReturn(false);

        // Give the shooter the anti-air quirk, and fire on an airborne target.
        Mockito.when(mockShooter.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_POS_ANTI_AIR))).thenReturn(true);
        mockTarget = Mockito.mock(ConvFighter.class);
        Mockito.when(mockTarget.isAirborne()).thenReturn(true);
        Mockito.when(mockTarget.isAirborneVTOLorWIGE()).thenReturn(false);
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_ANTI_AIR);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        Mockito.when(mockShooter.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_POS_ANTI_AIR))).thenReturn(false);
        mockTarget = Mockito.mock(BipedMech.class);
        Mockito.when(mockTarget.isAirborne()).thenReturn(false);
        Mockito.when(mockTarget.isAirborneVTOLorWIGE()).thenReturn(false);

        // Firing at Battle Armor
        mockTarget = Mockito.mock(BattleArmor.class);
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_TAR_BA);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        mockTarget = Mockito.mock(BipedMech.class);

        // Firing at an ejected mechwarrior.
        mockTarget = Mockito.mock(MechWarrior.class);
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_TAR_MW);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        mockTarget = Mockito.mock(BipedMech.class);

        // Firing at infantry
        mockTarget = Mockito.mock(Infantry.class);
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_TAR_INF);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        mockTarget = Mockito.mock(BipedMech.class);

        // Target is out of range.
        Mockito.when(mockShooter.getMaxWeaponRange()).thenReturn(5);
        expected = new ToHitData(FireControl.TH_RNG_TOO_FAR);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        Mockito.when(mockShooter.getMaxWeaponRange()).thenReturn(21);

        // Target is in smoke.
        // Light smoke
        Mockito.when(mockHex.terrainLevel(Terrains.SMOKE)).thenReturn(
                SmokeCloud.SMOKE_LIGHT);
        expected = new ToHitData();
        expected.addModifier(1, FireControl.TH_SMOKE);

        // Heavy Smoke
        Mockito.when(mockHex.terrainLevel(Terrains.SMOKE)).thenReturn(
                SmokeCloud.SMOKE_HEAVY);
        expected = new ToHitData();
        expected.addModifier(2, FireControl.TH_SMOKE);

        // Light LI smoke
        Mockito.when(mockHex.terrainLevel(Terrains.SMOKE)).thenReturn(
                SmokeCloud.SMOKE_LI_LIGHT);
        expected = new ToHitData();
        expected.addModifier(1, FireControl.TH_SMOKE);

        // Chaff Smoke
        Mockito.when(mockHex.terrainLevel(Terrains.SMOKE)).thenReturn(
                SmokeCloud.SMOKE_CHAFF_LIGHT);
        expected = new ToHitData();
        expected.addModifier(1, FireControl.TH_SMOKE);

        Mockito.when(mockHex.terrainLevel(Terrains.SMOKE)).thenReturn(
                SmokeCloud.SMOKE_NONE);
    }

    private void assertToHitDataEquals(ToHitData expected, Object actual) {
        Assert.assertNotNull(actual);
        Assert.assertTrue("actual: " + actual.getClass().getName(), actual instanceof ToHitData);
        ToHitData actualTHD = (ToHitData) actual;
        StringBuilder failure = new StringBuilder();
        if (expected.getValue() != actualTHD.getValue()) {
            failure.append("\nExpected: ").append(expected.getValue());
            failure.append("\nActual:   ").append(actualTHD.getValue());
        }
        Set<TargetRollModifier> expectedMods = new HashSet<>(expected.getModifiers());
        Set<TargetRollModifier> actualMods = new HashSet<>(actualTHD.getModifiers());
        if (!expectedMods.equals(actualMods)) {
            failure.append("\nExpected: ").append(expected.getDesc());
            failure.append("\nActual:   ").append(actualTHD.getDesc());
        }
        if (!StringUtil.isNullOrEmpty(failure.toString())) {
            Assert.fail(failure.toString());
        }
    }

    @Test
    public void testGuessToHitModifierPhysical() {
        ToHitData expected;

        // guessToHitModifierHelperForAnyAttack being tested elsewhere.
        Mockito.doReturn(new ToHitData())
               .when(testFireControl)
               .guessToHitModifierHelperForAnyAttack(Mockito.any(Entity.class), Mockito.any(EntityState.class),
                                                     Mockito.any(Targetable.class), Mockito.any(EntityState.class),
                                                     Mockito.anyInt(), Mockito.any(IGame.class));
        mockTargetCoords = new Coords(0, 1);
        Mockito.when(mockTargetState.getPosition()).thenReturn(mockTargetCoords);
        Mockito.doReturn(true).when(testFireControl).isInArc(Mockito.any(Coords.class), Mockito.anyInt(),
                                                             Mockito.any(Coords.class), Mockito.anyInt());
        IHex mockShooterHex = Mockito.mock(IHex.class);
        Mockito.when(mockShooterHex.getLevel()).thenReturn(0);
        Mockito.when(mockBoard.getHex(Mockito.eq(mockShooterState.getPosition()))).thenReturn(mockShooterHex);
        Mockito.when(mockShooter.getElevation()).thenReturn(0);
        Mockito.when(mockShooter.relHeight()).thenReturn(2);
        Mockito.when(mockShooter.getWeightClass()).thenReturn(EntityWeightClass.WEIGHT_LIGHT);
        Mockito.when(mockShooter.isLocationBad(Mech.LOC_LARM)).thenReturn(false);
        Mockito.when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, Mech.LOC_LARM)).thenReturn(true);
        Mockito.when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, Mech.LOC_LARM)).thenReturn(true);
        Mockito.when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_LARM)).thenReturn(true);
        Mockito.when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_HAND, Mech.LOC_LARM)).thenReturn(true);
        Mockito.when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_UPPER_LEG, Mech.LOC_LLEG)).thenReturn(true);
        Mockito.when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_LOWER_LEG, Mech.LOC_LLEG)).thenReturn(true);
        Mockito.when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_FOOT, Mech.LOC_LLEG)).thenReturn(true);

        IHex mockTargetHex = Mockito.mock(IHex.class);
        Mockito.when(mockTargetHex.getLevel()).thenReturn(0);
        Mockito.when(mockBoard.getHex(Mockito.eq(mockTargetState.getPosition()))).thenReturn(mockTargetHex);
        Mockito.when(mockTarget.getElevation()).thenReturn(0);
        Mockito.when(mockTarget.getHeight()).thenReturn(2);

        // Test a regular kick.
        expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting() - 2, FireControl.TH_PHY_BASE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_KICK,
                                                                                   mockGame));

        // Test a superheavy mech attempting a kick.
        Mockito.when(((Mech) mockShooter).getCockpitType()).thenReturn(Mech.COCKPIT_SUPERHEAVY);
        expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting() - 2, FireControl.TH_PHY_BASE);
        expected.addModifier(FireControl.TH_PHY_SUPER);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_KICK,
                                                                                   mockGame));
        Mockito.when(((Mech) mockShooter).getCockpitType()).thenReturn(Mech.COCKPIT_STANDARD);

        // Test turning on the TacOps Attacker Weight modifier.
        Mockito.when(mockGameOptions.booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_PHYSICAL_ATTACK_PSR)).thenReturn(true);
        expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting() - 2, FireControl.TH_PHY_BASE);
        expected.addModifier(FireControl.TH_PHY_LIGHT);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_KICK,
                                                                                   mockGame));
        Mockito.when(mockShooter.getWeightClass()).thenReturn(EntityWeightClass.WEIGHT_MEDIUM);
        expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting() - 2, FireControl.TH_PHY_BASE);
        expected.addModifier(FireControl.TH_PHY_MEDIUM);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_KICK,
                                                                                   mockGame));
        Mockito.when(mockShooter.getWeightClass()).thenReturn(EntityWeightClass.WEIGHT_HEAVY);
        expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting() - 2, FireControl.TH_PHY_BASE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_KICK,
                                                                                   mockGame));
        Mockito.when(mockGameOptions.booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_PHYSICAL_ATTACK_PSR)).thenReturn(false);
        Mockito.when(mockShooter.getWeightClass()).thenReturn(EntityWeightClass.WEIGHT_LIGHT);

        // Test trying to kick infantry in a different hex.
        Entity infantryTarget = Mockito.mock(Infantry.class);
        Mockito.when(infantryTarget.getElevation()).thenReturn(0);
        Mockito.when(infantryTarget.getHeight()).thenReturn(1);
        expected = new ToHitData(FireControl.TH_PHY_K_INF_RNG);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   infantryTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_KICK,
                                                                                   mockGame));
        Mockito.when(mockTargetState.getPosition()).thenReturn(new Coords(0, 0)); // Move them into my hex.
        expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting() - 2, FireControl.TH_PHY_BASE);
        expected.addModifier(FireControl.TH_PHY_K_INF);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   infantryTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_KICK,
                                                                                   mockGame));
        Mockito.when(mockTargetState.getPosition()).thenReturn(new Coords(0, 1));

        // Test kicking with a busted foot.
        Mockito.when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_FOOT, Mech.LOC_LLEG)).thenReturn(false);
        expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting() - 2, FireControl.TH_PHY_BASE);
        expected.addModifier(FireControl.TH_PHY_K_FOOT);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_KICK,
                                                                                   mockGame));
        Mockito.when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_FOOT, Mech.LOC_LLEG)).thenReturn(true);

        // Test kicking with a bad lower leg actuator.
        Mockito.when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_LOWER_LEG, Mech.LOC_LLEG)).thenReturn(false);
        expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting() - 2, FireControl.TH_PHY_BASE);
        expected.addModifier(FireControl.TH_PHY_K_LOWER_LEG);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_KICK,
                                                                                   mockGame));
        Mockito.when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_LOWER_LEG, Mech.LOC_LLEG)).thenReturn(true);

        // Test kicking with a bad upper leg actuator.
        Mockito.when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_UPPER_LEG, Mech.LOC_LLEG)).thenReturn(false);
        expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting() - 2, FireControl.TH_PHY_BASE);
        expected.addModifier(FireControl.TH_PHY_K_UPPER_LEG);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_KICK,
                                                                                   mockGame));
        Mockito.when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_UPPER_LEG, Mech.LOC_RLEG)).thenReturn(true);

        // Test kicking with a busted hip.
        Mockito.when(mockShooter.hasHipCrit()).thenReturn(true);
        expected = new ToHitData(FireControl.TH_PHY_K_HIP);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_KICK,
                                                                                   mockGame));
        Mockito.when(mockShooter.hasHipCrit()).thenReturn(false);

        // Test trying to kick while prone.
        expected = new ToHitData(FireControl.TH_PHY_K_PRONE);
        Mockito.when(mockShooterState.isProne()).thenReturn(true);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_KICK,
                                                                                   mockGame));
        Mockito.when(mockShooterState.isProne()).thenReturn(false);

        // Test a regular punch.
        expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting(), FireControl.TH_PHY_BASE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_PUNCH,
                                                                                   mockGame));

        // Test having the 'easy to pilot' quirk.
        Mockito.when(mockShooter.hasQuirk(OptionsConstants.QUIRK_POS_EASY_PILOT)).thenReturn(true);
        expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting(), FireControl.TH_PHY_BASE);
        expected.addModifier(FireControl.TH_PHY_EASY_PILOT);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_PUNCH,
                                                                                   mockGame));
        Mockito.when(mockCrew.getPiloting()).thenReturn(2); // Pilot to good to use the quirk.
        expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting(), FireControl.TH_PHY_BASE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_PUNCH,
                                                                                   mockGame));
        Mockito.when(mockShooter.hasQuirk(OptionsConstants.QUIRK_POS_EASY_PILOT)).thenReturn(false);
        Mockito.when(mockCrew.getPiloting()).thenReturn(5);

        /// Test having a damaged/missing hand.
        Mockito.when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_HAND, Mech.LOC_LARM)).thenReturn(false);
        expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting(), FireControl.TH_PHY_BASE);
        expected.addModifier(FireControl.TH_PHY_P_HAND);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_PUNCH,
                                                                                   mockGame));
        Mockito.when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_HAND, Mech.LOC_LARM)).thenReturn(true);

        /// Test having a damaged/missing upper arm.
        Mockito.when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, Mech.LOC_LARM)).thenReturn(false);
        expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting(), FireControl.TH_PHY_BASE);
        expected.addModifier(FireControl.TH_PHY_P_UPPER_ARM);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_PUNCH,
                                                                                   mockGame));
        Mockito.when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, Mech.LOC_LARM)).thenReturn(true);

        /// Test having a damaged/missing lower arm.
        Mockito.when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_LARM)).thenReturn(false);
        expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting(), FireControl.TH_PHY_BASE);
        expected.addModifier(FireControl.TH_PHY_P_LOWER_ARM);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_PUNCH,
                                                                                   mockGame));
        Mockito.when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_LARM)).thenReturn(true);

        // Test trying to punch with a bad shoulder.
        Mockito.when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, Mech.LOC_RARM)).thenReturn(false);
        expected = new ToHitData(FireControl.TH_PHY_P_NO_SHOULDER);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.RIGHT_PUNCH,
                                                                                   mockGame));

        // Test trying to punch with a destroyed arm.
        Mockito.when(mockShooter.isLocationBad(Mech.LOC_RARM)).thenReturn(true);
        expected = new ToHitData(FireControl.TH_PHY_P_NO_ARM);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.RIGHT_PUNCH,
                                                                                   mockGame));

        // Test trying to punch an infantry target.
        infantryTarget = Mockito.mock(Infantry.class);
        Mockito.when(infantryTarget.getElevation()).thenReturn(1);
        Mockito.when(infantryTarget.getHeight()).thenReturn(1);
        expected = new ToHitData(FireControl.TH_PHY_P_TAR_INF);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   infantryTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_PUNCH,
                                                                                   mockGame));

        // Test trying to punch while prone.
        Mockito.when(mockShooterState.isProne()).thenReturn(true);
        expected = new ToHitData(FireControl.TH_PHY_P_TAR_PRONE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_PUNCH,
                                                                                   mockGame));

        // Test the target being at the wrong elevation for a punch.
        Mockito.when(mockShooterHex.getLevel()).thenReturn(1);
        expected = new ToHitData(FireControl.TH_PHY_TOO_MUCH_ELEVATION);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_PUNCH,
                                                                                   mockGame));

        // Test an attacker with the 'no arms' quirk trying to punch.
        Mockito.when(mockShooter.hasQuirk(OptionsConstants.QUIRK_NEG_NO_ARMS)).thenReturn(true);
        expected = new ToHitData(FireControl.TH_PHY_P_NO_ARMS_QUIRK);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_PUNCH,
                                                                                   mockGame));

        // Test the target not being in the attack arc.
        Mockito.doReturn(false).when(testFireControl).isInArc(Mockito.any(Coords.class), Mockito.anyInt(),
                                                              Mockito.any(Coords.class), Mockito.anyInt());
        expected = new ToHitData(FireControl.TH_PHY_NOT_IN_ARC);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_PUNCH,
                                                                                   mockGame));

        // Test the target being more than 1 hex away.
        Mockito.when(mockTargetState.getPosition()).thenReturn(new Coords(10, 10));
        expected = new ToHitData(FireControl.TH_PHY_TOO_FAR);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_PUNCH,
                                                                                   mockGame));

        // Test an attacker that is not a mech.
        Entity mockVee = Mockito.mock(Tank.class);
        expected = new ToHitData(FireControl.TH_PHY_NOT_MECH);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockVee, null, mockTarget,
                                                                                   mockTargetState,
                                                                                   PhysicalAttackType.CHARGE,
                                                                                   mockGame));
    }

    @Test
    public void testGuessToHitModifierForWeapon() {
        ToHitData expected;
        Mockito.when(mockGameOptions.booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE)).thenReturn(false);
        Mockito.when(mockTarget.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_POS_LOW_PROFILE))).thenReturn(false);
        Mockito.when(mockShooterState.getFacing()).thenReturn(1);
        Mockito.doReturn(true).when(testFireControl).isInArc(Mockito.any(Coords.class), Mockito.anyInt(),
                                                             Mockito.any(Coords.class), Mockito.anyInt());
        Mockito.doReturn(new ToHitData())
               .when(testFireControl)
               .guessToHitModifierHelperForAnyAttack(Mockito.any(Entity.class), Mockito.any(EntityState.class),
                                                     Mockito.any(Targetable.class), Mockito.any(EntityState.class),
                                                     Mockito.anyInt(), Mockito.any(IGame.class));
        LosEffects spyLosEffects = Mockito.spy(new LosEffects());
        Mockito.doReturn(spyLosEffects)
               .when(testFireControl)
               .getLosEffects(Mockito.any(IGame.class), Mockito.anyInt(), Mockito.any(Targetable.class),
                              Mockito.any(Coords.class), Mockito.any(Coords.class), Mockito.anyBoolean());
        Mockito.doReturn(new ToHitData()).when(spyLosEffects).losModifiers(Mockito.eq(mockGame));

        IHex mockTargetHex = Mockito.mock(IHex.class);
        Mockito.when(mockBoard.getHex(Mockito.eq(mockTargetCoords))).thenReturn(mockTargetHex);
        Mockito.when(mockTargetHex.containsTerrain(Terrains.WATER)).thenReturn(false); // todo test water

        final int MOCK_WEAPON_ID = 1;
        Mounted mockWeapon = Mockito.mock(Mounted.class);
        Mockito.when(mockWeapon.canFire()).thenReturn(true);
        Mockito.when(mockWeapon.getLocation()).thenReturn(Mech.LOC_RARM);
        Mockito.when(mockShooter.getEquipmentNum(Mockito.eq(mockWeapon))).thenReturn(MOCK_WEAPON_ID);
        Mockito.when(mockShooter.isSecondaryArcWeapon(MOCK_WEAPON_ID)).thenReturn(false);


        WeaponType mockWeaponType = Mockito.mock(WeaponType.class);
        Mockito.when(mockWeapon.getType()).thenReturn(mockWeaponType);
        Mockito.when(mockWeaponType.getAmmoType()).thenReturn(AmmoType.T_AC);
        Mockito.when(mockWeaponType.getRanges(Mockito.eq(mockWeapon))).thenReturn(new int[]{3, 6, 12, 18, 24});
        Mockito.when(mockWeaponType.getMinimumRange()).thenReturn(3);
        Mockito.when(mockWeaponType.hasFlag(Mockito.eq(WeaponType.F_DIRECT_FIRE))).thenReturn(true);

        Mounted mockAmmo = Mockito.mock(Mounted.class);
        Mockito.when(mockWeapon.getLinked()).thenReturn(mockAmmo);
        Mockito.when(mockAmmo.getUsableShotsLeft()).thenReturn(10);

        // Test the vanilla case.
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_MEDIUM_RANGE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));

        // Test weapon quirks.
        Mockito.when(mockWeapon.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_WEAP_POS_ACCURATE))).thenReturn(true);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_MEDIUM_RANGE);
        expected.addModifier(FireControl.TH_ACCURATE_WEAP);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        Mockito.when(mockWeapon.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_WEAP_POS_ACCURATE))).thenReturn(false);
        Mockito.when(mockWeapon.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_WEAP_NEG_INACCURATE))).thenReturn(true);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_MEDIUM_RANGE);
        expected.addModifier(FireControl.TH_INACCURATE_WEAP);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        Mockito.when(mockWeapon.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_WEAP_NEG_INACCURATE))).thenReturn(false);

        // Test long range shooter quirks.
        Mockito.when(mockTargetState.getPosition()).thenReturn(new Coords(0, 15));
        Mockito.when(mockShooter.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_POS_IMP_TARG_L))).thenReturn(true);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_LONG_RANGE);
        expected.addModifier(FireControl.TH_IMP_TARG_LONG);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        Mockito.when(mockShooter.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_POS_IMP_TARG_L))).thenReturn(false);
        Mockito.when(mockShooter.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_POS_VAR_RNG_TARG_S))).thenReturn(true);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_LONG_RANGE);
        expected.addModifier(FireControl.TH_VAR_RNG_TARG_SHORT_AT_LONG);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        Mockito.when(mockShooter.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_POS_VAR_RNG_TARG_S))).thenReturn(false);
        Mockito.when(mockShooter.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_POS_VAR_RNG_TARG_L))).thenReturn(true);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_LONG_RANGE);
        expected.addModifier(FireControl.TH_VAR_RNG_TARG_LONG_AT_LONG);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        Mockito.when(mockShooter.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_POS_VAR_RNG_TARG_L))).thenReturn(false);
        Mockito.when(mockShooter.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_NEG_POOR_TARG_L))).thenReturn(true);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_LONG_RANGE);
        expected.addModifier(FireControl.TH_POOR_TARG_LONG);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        Mockito.when(mockShooter.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_NEG_POOR_TARG_L))).thenReturn(false);
        Mockito.when(mockTargetState.getPosition()).thenReturn(mockTargetCoords);

        // Test medium range shooter quirks.
        Mockito.when(mockShooter.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_POS_IMP_TARG_M))).thenReturn(true);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_MEDIUM_RANGE);
        expected.addModifier(FireControl.TH_IMP_TARG_MEDIUM);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        Mockito.when(mockShooter.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_POS_IMP_TARG_M))).thenReturn(false);
        Mockito.when(mockShooter.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_NEG_POOR_TARG_M))).thenReturn(true);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_MEDIUM_RANGE);
        expected.addModifier(FireControl.TH_POOR_TARG_MEDIUM);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        Mockito.when(mockShooter.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_NEG_POOR_TARG_M))).thenReturn(false);

        // Test short range shooter quirks.
        Mockito.when(mockTargetState.getPosition()).thenReturn(new Coords(0, 5));
        Mockito.when(mockShooter.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_POS_IMP_TARG_S))).thenReturn(true);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_SHORT_RANGE);
        expected.addModifier(FireControl.TH_IMP_TARG_SHORT);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        Mockito.when(mockShooter.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_POS_IMP_TARG_S))).thenReturn(false);
        Mockito.when(mockShooter.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_POS_VAR_RNG_TARG_S))).thenReturn(true);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_SHORT_RANGE);
        expected.addModifier(FireControl.TH_VAR_RNG_TARG_SHORT_AT_SHORT);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        Mockito.when(mockShooter.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_POS_VAR_RNG_TARG_S))).thenReturn(false);
        Mockito.when(mockShooter.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_POS_VAR_RNG_TARG_L))).thenReturn(true);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_SHORT_RANGE);
        expected.addModifier(FireControl.TH_VAR_RNG_TARG_LONG_AT_SHORT);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        Mockito.when(mockShooter.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_POS_VAR_RNG_TARG_L))).thenReturn(false);
        Mockito.when(mockShooter.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_NEG_POOR_TARG_S))).thenReturn(true);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_SHORT_RANGE);
        expected.addModifier(FireControl.TH_POOR_TARG_SHORT);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        Mockito.when(mockShooter.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_NEG_POOR_TARG_S))).thenReturn(false);
        Mockito.when(mockTargetState.getPosition()).thenReturn(mockTargetCoords);

        // Test a low-profile target.
        Mockito.when(mockTarget.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_POS_LOW_PROFILE))).thenReturn(true);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_MEDIUM_RANGE);
        expected.addModifier(FireControl.TH_TAR_LOW_PROFILE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        Mockito.when(mockTarget.hasQuirk(Mockito.eq(OptionsConstants.QUIRK_POS_LOW_PROFILE))).thenReturn(false);

        // Test a targeting computer.
        Mockito.when(mockShooter.hasTargComp()).thenReturn(true);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_MEDIUM_RANGE);
        expected.addModifier(FireControl.TH_TARGETTING_COMP);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        Mockito.when(mockWeaponType.hasFlag(Mockito.eq(WeaponType.F_DIRECT_FIRE))).thenReturn(false);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_MEDIUM_RANGE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        Mockito.when(mockWeaponType.hasFlag(Mockito.eq(WeaponType.F_DIRECT_FIRE))).thenReturn(false);
        Mockito.when(mockShooter.hasTargComp()).thenReturn(false);

        // Test ammo mods.
        AmmoType mockAmmoType = Mockito.mock(AmmoType.class);
        Mockito.when(mockAmmo.getType()).thenReturn(mockAmmoType);
        Mockito.when(mockAmmoType.getToHitModifier()).thenReturn(1);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_MEDIUM_RANGE);
        expected.addModifier(1, FireControl.TH_AMMO_MOD);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        Mockito.when(mockAmmoType.getToHitModifier()).thenReturn(0);

        // Test target size mods.
        LargeSupportTank mockLargeTank = Mockito.mock(LargeSupportTank.class);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_MEDIUM_RANGE);
        expected.addModifier(FireControl.TH_RNG_LARGE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockLargeTank, mockTargetState,
                                                                                    mockWeapon, mockGame));
        Mockito.when(((Mech) mockTarget).getCockpitType()).thenReturn(Mech.COCKPIT_SUPERHEAVY);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_MEDIUM_RANGE);
        expected.addModifier(FireControl.TH_RNG_LARGE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        Mockito.when(((Mech) mockTarget).getCockpitType()).thenReturn(Mech.COCKPIT_STANDARD);

        // Test weapon mods.
        Mockito.when(mockWeaponType.getToHitModifier()).thenReturn(-2);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_MEDIUM_RANGE);
        expected.addModifier(-2, FireControl.TH_WEAPON_MOD);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        Mockito.when(mockWeaponType.getToHitModifier()).thenReturn(0);

        // Test heat mods.
        Mockito.when(mockShooter.getHeatFiringModifier()).thenReturn(1);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_MEDIUM_RANGE);
        expected.addModifier(1, FireControl.TH_HEAT);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        Mockito.when(mockShooter.getHeatFiringModifier()).thenReturn(0);

        // Test fighter's at altitude
        ConvFighter mockFighter = Mockito.mock(ConvFighter.class);
        Mockito.when(mockFighter.getAltitude()).thenReturn(3);
        Mockito.when(mockFighter.getTargetId()).thenReturn(2);
        EntityState mockFighterState = Mockito.mock(EntityState.class);
        Mockito.when(mockFighterState.isAirborneAero()).thenReturn(true);
        Mockito.when(mockFighterState.isBuilding()).thenReturn(false);
        Mockito.when(mockFighterState.getHeat()).thenReturn(0);
        Mockito.when(mockFighterState.getPosition()).thenReturn(mockTargetCoords);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_LONG_RANGE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockFighter, mockFighterState,
                                                                                    mockWeapon, mockGame));
        Mockito.when(mockFighter.getTargetId()).thenReturn(1); // Target aero is also firing on shooter.
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_SHORT_RANGE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockFighter, mockFighterState,
                                                                                    mockWeapon, mockGame));

        // Test changing the range.
        Mockito.when(mockTargetState.getPosition()).thenReturn(new Coords(5, 0));
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_SHORT_RANGE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        Mockito.when(mockTargetState.getPosition()).thenReturn(new Coords(1, 0));
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(3, FireControl.TH_MINIMUM_RANGE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        Mockito.when(mockGameOptions.booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE)).thenReturn(true);
        Mockito.when(mockTargetState.getPosition()).thenReturn(new Coords(20, 0));
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_EXTREME_RANGE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        // todo Test infantry range mods.
        Mockito.when(mockTargetState.getPosition()).thenReturn(mockTargetCoords);
        Mockito.when(mockGameOptions.booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE)).thenReturn(false);

        // todo Test swarming and leg attacks.

        // Test sensor damage.
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_MEDIUM_RANGE);
        Mockito.when(mockShooter.getBadCriticals(Mockito.eq(CriticalSlot.TYPE_SYSTEM), Mockito.eq(Mech.SYSTEM_SENSORS),
                                                 Mockito.eq(Mech.LOC_HEAD))).thenReturn(2);
        expected.addModifier(2, FireControl.TH_SENSORS);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        Tank mockTank = Mockito.mock(Tank.class); // Tank sensor damage is a little different.
        Mockito.when(mockTank.getCrew()).thenReturn(mockCrew);
        expected = new ToHitData(mockTank.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_MEDIUM_RANGE);
        Mockito.when(mockTank.getSensorHits()).thenReturn(1);
        expected.addModifier(1, FireControl.TH_SENSORS);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockTank, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        Mockito.when(mockShooter.getBadCriticals(Mockito.eq(CriticalSlot.TYPE_SYSTEM), Mockito.eq(Mech.SYSTEM_SENSORS),
                                                 Mockito.eq(Mech.LOC_HEAD))).thenReturn(0);

        // Test stopping swarm attacks.
        WeaponType mockSwarmStop = Mockito.mock(StopSwarmAttack.class);
        Mockito.when(mockSwarmStop.getRanges(Mockito.eq(mockWeapon))).thenReturn(new int[]{0, 0, 0, 0, 0});
        Mockito.when(mockTargetState.getPosition()).thenReturn(new Coords(0, 0));
        Mockito.when(mockWeapon.getType()).thenReturn(mockSwarmStop);
        Mockito.when(mockShooter.getSwarmTargetId()).thenReturn(Entity.NONE); // Invalid attack.
        expected = new ToHitData(FireControl.TH_STOP_SWARM_INVALID);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        Mockito.when(mockShooter.getSwarmTargetId()).thenReturn(10); // Valid attack.
        expected = new ToHitData(FireControl.TH_SWARM_STOPPED);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        Mockito.when(mockWeapon.getType()).thenReturn(mockWeaponType);
        Mockito.when(mockTargetState.getPosition()).thenReturn(mockTargetCoords);

        // Test shooting infantry at 0 range.
        Mockito.when(mockTargetState.getPosition()).thenReturn(new Coords(0, 0));
        expected = new ToHitData(FireControl.TH_INF_ZERO_RNG);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        //todo Infantry on Infantry violence.
        Mockito.when(mockTargetState.getPosition()).thenReturn(mockTargetCoords);

        // Test being out of range.
        Mockito.when(mockTargetState.getPosition()).thenReturn(new Coords(0, 100));
        expected = new ToHitData(FireControl.TH_OUT_OF_RANGE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        Mockito.when(mockTargetState.getPosition()).thenReturn(mockTargetCoords);

        // Test the target being out of arc.
        Mockito.doReturn(false).when(testFireControl).isInArc(Mockito.any(Coords.class), Mockito.anyInt(),
                                                              Mockito.any(Coords.class), Mockito.anyInt());
        expected = new ToHitData(FireControl.TH_WEAPON_NO_ARC);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));


        // Test a prone mech w/ no arms.
        Mockito.when(mockShooterState.isProne()).thenReturn(true);
        Mockito.when(mockShooter.isLocationBad(Mech.LOC_RARM)).thenReturn(true);
        Mockito.when(mockShooter.isLocationBad(Mech.LOC_LARM)).thenReturn(true);
        expected = new ToHitData(FireControl.TH_WEAP_PRONE_ARMLESS);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        // Propping self up on firing arm.
        Mockito.when(mockShooter.isLocationBad(Mech.LOC_LARM)).thenReturn(false);
        expected = new ToHitData(FireControl.TH_WEAP_ARM_PROP);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        // Trying to fire a leg weapon.
        Mockito.when(mockWeapon.getLocation()).thenReturn(Mech.LOC_LLEG);
        expected = new ToHitData(FireControl.TH_WEAP_PRONE_LEG);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        Mockito.when(mockShooterState.isProne()).thenReturn(false);

        // Test a weapon that is out of ammo.
        Mockito.when(mockAmmo.getUsableShotsLeft()).thenReturn(0);
        expected = new ToHitData(FireControl.TH_WEAP_NO_AMMO);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
        Mockito.when(mockAmmo.getUsableShotsLeft()).thenReturn(10);
        Mockito.when(mockWeapon.getLinked()).thenReturn(null);
        expected = new ToHitData(FireControl.TH_WEAP_NO_AMMO);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));

        // Test a weapon that cannot fire.
        Mockito.when(mockWeapon.canFire()).thenReturn(false);
        expected = new ToHitData(FireControl.TH_WEAP_CANNOT_FIRE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter, mockShooterState,
                                                                                    mockTarget, mockTargetState,
                                                                                    mockWeapon, mockGame));
    }

    @Test
    public void testGuessAirToGroundStrikeToHitModifier() {
        ToHitData expected;
        MovePath mockFlightPathGood = Mockito.mock(MovePath.class);
        MovePath mockFlightPathBad = Mockito.mock(MovePath.class);
        Mockito.doReturn(new ToHitData())
               .when(testFireControl)
               .guessToHitModifierHelperForAnyAttack(Mockito.any(Entity.class), Mockito.any(EntityState.class),
                                                     Mockito.any(Targetable.class), Mockito.any(EntityState.class),
                                                     Mockito.anyInt(), Mockito.any(IGame.class));
        Mockito.doReturn(true).when(testFireControl).isTargetUnderFlightPath(Mockito.any(MovePath.class),
                                                                             Mockito.any(EntityState.class));
        Mockito.doReturn(false).when(testFireControl).isTargetUnderFlightPath(Mockito.eq(mockFlightPathBad),
                                                                              Mockito.any(EntityState.class));

        Mounted mockWeapon = Mockito.mock(Mounted.class);
        Mockito.when(mockWeapon.canFire()).thenReturn(true);

        WeaponType mockWeaponType = Mockito.mock(WeaponType.class);
        Mockito.when(mockWeapon.getType()).thenReturn(mockWeaponType);
        Mockito.when(mockWeaponType.getAmmoType()).thenReturn(AmmoType.T_AC);

        Mounted mockAmmo = Mockito.mock(Mounted.class);
        Mockito.when(mockWeapon.getLinked()).thenReturn(mockAmmo);
        Mockito.when(mockAmmo.getUsableShotsLeft()).thenReturn(10);

        ConvFighter mockFighter = Mockito.mock(ConvFighter.class);
        Mockito.when(mockFighter.getCrew()).thenReturn(mockCrew);

        // Test the vanilla case.
        expected = new ToHitData(mockCrew.getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_AIR_STRIKE);
        assertToHitDataEquals(expected, testFireControl.guessAirToGroundStrikeToHitModifier(mockFighter,
                                                                                            mockShooterState,
                                                                                            mockTarget,
                                                                                            mockTargetState,
                                                                                            mockFlightPathGood,
                                                                                            mockWeapon,
                                                                                            mockGame,
                                                                                            true));
        assertToHitDataEquals(expected, testFireControl.guessAirToGroundStrikeToHitModifier(mockFighter,
                                                                                            mockShooterState,
                                                                                            mockTarget,
                                                                                            mockTargetState,
                                                                                            mockFlightPathGood,
                                                                                            mockWeapon,
                                                                                            mockGame,
                                                                                            false));

        // Test the target not being under our flight path.
        expected = new ToHitData(FireControl.TH_AIR_STRIKE_PATH);
        assertToHitDataEquals(expected, testFireControl.guessAirToGroundStrikeToHitModifier(mockFighter,
                                                                                            mockShooterState,
                                                                                            mockTarget,
                                                                                            mockTargetState,
                                                                                            mockFlightPathBad,
                                                                                            mockWeapon,
                                                                                            mockGame,
                                                                                            false));

        // Test a weapon that is out of ammo.
        Mockito.when(mockAmmo.getUsableShotsLeft()).thenReturn(0);
        expected = new ToHitData(FireControl.TH_WEAP_NO_AMMO);
        assertToHitDataEquals(expected, testFireControl.guessAirToGroundStrikeToHitModifier(mockFighter,
                                                                                            mockShooterState,
                                                                                            mockTarget,
                                                                                            mockTargetState,
                                                                                            mockFlightPathGood,
                                                                                            mockWeapon,
                                                                                            mockGame,
                                                                                            true));

        // Test a weapon who's ammo has been destroyed.
        Mockito.when(mockWeapon.getLinked()).thenReturn(null);
        expected = new ToHitData(FireControl.TH_WEAP_NO_AMMO);
        assertToHitDataEquals(expected, testFireControl.guessAirToGroundStrikeToHitModifier(mockFighter,
                                                                                            mockShooterState,
                                                                                            mockTarget,
                                                                                            mockTargetState,
                                                                                            mockFlightPathGood,
                                                                                            mockWeapon,
                                                                                            mockGame,
                                                                                            true));

        // Test a weapon unable to fire.
        Mockito.when(mockWeapon.canFire()).thenReturn(false);
        expected = new ToHitData(FireControl.TH_WEAP_CANNOT_FIRE);
        assertToHitDataEquals(expected, testFireControl.guessAirToGroundStrikeToHitModifier(mockFighter,
                                                                                            mockShooterState,
                                                                                            mockTarget,
                                                                                            mockTargetState,
                                                                                            mockFlightPathGood,
                                                                                            mockWeapon,
                                                                                            mockGame,
                                                                                            true));
    }

    @Test
    public void testIsTargetUnderFlightPath() {

        // Test the target being under the path.
        Vector<MoveStep> pathSteps = new Vector<>(1);
        MoveStep mockStep = Mockito.mock(MoveStep.class);
        pathSteps.add(mockStep);
        MovePath mockPath = Mockito.mock(MovePath.class);
        Mockito.when(mockPath.getSteps()).thenReturn(pathSteps.elements());
        Mockito.when(mockStep.getPosition()).thenReturn(mockTargetCoords);
        Assert.assertTrue(testFireControl.isTargetUnderFlightPath(mockPath, mockTargetState));

        // Test the target not being under the path.
        pathSteps = new Vector<>(1);
        mockStep = Mockito.mock(MoveStep.class);
        pathSteps.add(mockStep);
        mockPath = Mockito.mock(MovePath.class);
        Mockito.when(mockPath.getSteps()).thenReturn(pathSteps.elements());
        Mockito.when(mockStep.getPosition()).thenReturn(mockShooterCoords);
        Assert.assertFalse(testFireControl.isTargetUnderFlightPath(mockPath, mockTargetState));
    }

    @Test
    public void testCalculateUtility() {
        final double TOLERANCE = 0.00001;
        int overheatTolerance = 5;
        double baseUtility = 20.6154;
        MechWarrior mockPilot = Mockito.mock(MechWarrior.class);
        Mockito.when(mockPilot.getId()).thenReturn(20);
        Mockito.when(mockPilot.isMilitary()).thenReturn(true);

        // Basic firing plan test.
        FiringPlan testFiringPlan = Mockito.spy(new FiringPlan(mockTarget));
        Mockito.doReturn(15.0).when(testFiringPlan).getExpectedDamage();
        Mockito.doReturn(0.46129).when(testFiringPlan).getExpectedCriticals();
        Mockito.doReturn(0.02005).when(testFiringPlan).getKillProbability();
        Mockito.doReturn(0).when(testFiringPlan).getHeat();
        Mockito.doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(Mockito.any(Targetable.class), Mockito.anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        Assert.assertEquals(baseUtility, testFiringPlan.getUtility(), TOLERANCE);

        // Make the target a commander.
        Mockito.when(mockTarget.isCommander()).thenReturn(true);
        testFiringPlan = Mockito.spy(new FiringPlan(mockTarget));
        Mockito.doReturn(15.0).when(testFiringPlan).getExpectedDamage();
        Mockito.doReturn(0.46129).when(testFiringPlan).getExpectedCriticals();
        Mockito.doReturn(0.02005).when(testFiringPlan).getKillProbability();
        Mockito.doReturn(0).when(testFiringPlan).getHeat();
        Mockito.doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(Mockito.any(Targetable.class), Mockito.anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        Assert.assertEquals(baseUtility * (1 + FireControl.COMMANDER_UTILITY), testFiringPlan.getUtility(), TOLERANCE);
        Mockito.when(mockTarget.isCommander()).thenReturn(false);

        // Make the target a sub-commander.
        Mockito.when(mockTarget.hasC3()).thenReturn(true);
        testFiringPlan = Mockito.spy(new FiringPlan(mockTarget));
        Mockito.doReturn(15.0).when(testFiringPlan).getExpectedDamage();
        Mockito.doReturn(0.46129).when(testFiringPlan).getExpectedCriticals();
        Mockito.doReturn(0.02005).when(testFiringPlan).getKillProbability();
        Mockito.doReturn(0).when(testFiringPlan).getHeat();
        Mockito.doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(Mockito.any(Targetable.class), Mockito.anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        Assert.assertEquals(baseUtility * (1 + FireControl.SUB_COMMANDER_UTILITY), testFiringPlan.getUtility(),
                            TOLERANCE);
        Mockito.when(mockTarget.hasC3()).thenReturn(false);

        // Make the target a Strategic Building Target.
        BuildingTarget mockBuilding = Mockito.mock(BuildingTarget.class);
        Mockito.when(mockBuilding.getPosition()).thenReturn(new Coords(5, 5));
        BehaviorSettings mockBehavior = Mockito.mock(BehaviorSettings.class);
        Mockito.when(mockPrincess.getBehaviorSettings()).thenReturn(mockBehavior);
        Set<String> testTargets = new HashSet<>(1);
        testTargets.add("0606");
        Mockito.when(mockBehavior.getStrategicBuildingTargets()).thenReturn(testTargets);
        testFiringPlan = Mockito.spy(new FiringPlan(mockBuilding));
        Mockito.doReturn(15.0).when(testFiringPlan).getExpectedDamage();
        Mockito.doReturn(0.46129).when(testFiringPlan).getExpectedCriticals();
        Mockito.doReturn(0.02005).when(testFiringPlan).getKillProbability();
        Mockito.doReturn(0).when(testFiringPlan).getHeat();
        Mockito.doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(Mockito.any(Targetable.class), Mockito.anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        Assert.assertEquals(baseUtility * (1 + FireControl.STRATEGIC_TARGET_UTILITY), testFiringPlan.getUtility(),
                            TOLERANCE);
        Mockito.when(mockBuilding.getPosition()).thenReturn(new Coords(10, 10)); // A building not on the list.
        testFiringPlan = Mockito.spy(new FiringPlan(mockBuilding));
        Mockito.doReturn(15.0).when(testFiringPlan).getExpectedDamage();
        Mockito.doReturn(0.46129).when(testFiringPlan).getExpectedCriticals();
        Mockito.doReturn(0.02005).when(testFiringPlan).getKillProbability();
        Mockito.doReturn(0).when(testFiringPlan).getHeat();
        Mockito.doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(Mockito.any(Targetable.class), Mockito.anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        Assert.assertEquals(baseUtility, testFiringPlan.getUtility(), TOLERANCE);

        // Make the target a priority unit target
        Set<Integer> testPriorityUnits = new HashSet<>(1);
        testPriorityUnits.add(MOCK_TARGET_ID);
        Mockito.when(mockPrincess.getPriorityUnitTargets()).thenReturn(testPriorityUnits);
        testFiringPlan = Mockito.spy(new FiringPlan(mockTarget));
        Mockito.doReturn(15.0).when(testFiringPlan).getExpectedDamage();
        Mockito.doReturn(0.46129).when(testFiringPlan).getExpectedCriticals();
        Mockito.doReturn(0.02005).when(testFiringPlan).getKillProbability();
        Mockito.doReturn(0).when(testFiringPlan).getHeat();
        Mockito.doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(Mockito.any(Targetable.class), Mockito.anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        Assert.assertEquals(baseUtility * (1 + FireControl.PRIORITY_TARGET_UTILITY), testFiringPlan.getUtility(),
                            TOLERANCE);
        Mockito.when(mockBehavior.getPriorityUnitTargets()).thenReturn(new HashSet<Integer>(0));
        Mockito.when(mockPrincess.getPriorityUnitTargets()).thenReturn(new HashSet<Integer>(0));

        // Attack an ejected pilot.
        testFiringPlan = Mockito.spy(new FiringPlan(mockPilot));
        Mockito.doReturn(15.0).when(testFiringPlan).getExpectedDamage();
        Mockito.doReturn(0.46129).when(testFiringPlan).getExpectedCriticals();
        Mockito.doReturn(0.02005).when(testFiringPlan).getKillProbability();
        Mockito.doReturn(0).when(testFiringPlan).getHeat();
        Mockito.doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(Mockito.any(Targetable.class), Mockito.anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        Assert.assertEquals(-979.3846, testFiringPlan.getUtility(), TOLERANCE);
        Assert.assertTrue(baseUtility > testFiringPlan.getUtility());

        // Increase the kill chance.
        testFiringPlan = Mockito.spy(new FiringPlan(mockTarget));
        Mockito.doReturn(15.0).when(testFiringPlan).getExpectedDamage();
        Mockito.doReturn(0.46129).when(testFiringPlan).getExpectedCriticals();
        Mockito.doReturn(0.12005).when(testFiringPlan).getKillProbability();
        Mockito.doReturn(0).when(testFiringPlan).getHeat();
        Mockito.doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(Mockito.any(Targetable.class), Mockito.anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        Assert.assertEquals(25.6154, testFiringPlan.getUtility(), TOLERANCE);
        Assert.assertTrue(baseUtility < testFiringPlan.getUtility());

        // Decrease the kill chance.
        testFiringPlan = Mockito.spy(new FiringPlan(mockTarget));
        Mockito.doReturn(15.0).when(testFiringPlan).getExpectedDamage();
        Mockito.doReturn(0.46129).when(testFiringPlan).getExpectedCriticals();
        Mockito.doReturn(0.01005).when(testFiringPlan).getKillProbability();
        Mockito.doReturn(0).when(testFiringPlan).getHeat();
        Mockito.doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(Mockito.any(Targetable.class), Mockito.anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        Assert.assertEquals(20.1154, testFiringPlan.getUtility(), TOLERANCE);
        Assert.assertTrue(baseUtility > testFiringPlan.getUtility());

        // Increase the # crits.
        testFiringPlan = Mockito.spy(new FiringPlan(mockTarget));
        Mockito.doReturn(15.0).when(testFiringPlan).getExpectedDamage();
        Mockito.doReturn(0.86129).when(testFiringPlan).getExpectedCriticals();
        Mockito.doReturn(0.02005).when(testFiringPlan).getKillProbability();
        Mockito.doReturn(0).when(testFiringPlan).getHeat();
        Mockito.doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(Mockito.any(Targetable.class), Mockito.anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        Assert.assertEquals(24.6154, testFiringPlan.getUtility(), TOLERANCE);
        Assert.assertTrue(baseUtility < testFiringPlan.getUtility());

        // Decrease the # crits.
        testFiringPlan = Mockito.spy(new FiringPlan(mockTarget));
        Mockito.doReturn(15.0).when(testFiringPlan).getExpectedDamage();
        Mockito.doReturn(0.26129).when(testFiringPlan).getExpectedCriticals();
        Mockito.doReturn(0.02005).when(testFiringPlan).getKillProbability();
        Mockito.doReturn(0).when(testFiringPlan).getHeat();
        Mockito.doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(Mockito.any(Targetable.class), Mockito.anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        Assert.assertEquals(18.6154, testFiringPlan.getUtility(), TOLERANCE);
        Assert.assertTrue(baseUtility > testFiringPlan.getUtility());

        // Test a higher damage plan.
        testFiringPlan = Mockito.spy(new FiringPlan(mockTarget));
        Mockito.doReturn(20.0).when(testFiringPlan).getExpectedDamage();
        Mockito.doReturn(0.46129).when(testFiringPlan).getExpectedCriticals();
        Mockito.doReturn(0.02005).when(testFiringPlan).getKillProbability();
        Mockito.doReturn(0).when(testFiringPlan).getHeat();
        Mockito.doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(Mockito.any(Targetable.class), Mockito.anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        Assert.assertEquals(25.6154, testFiringPlan.getUtility(), TOLERANCE);
        Assert.assertTrue(baseUtility < testFiringPlan.getUtility());

        // Test a lower damage plan.
        testFiringPlan = Mockito.spy(new FiringPlan(mockTarget));
        Mockito.doReturn(5.0).when(testFiringPlan).getExpectedDamage();
        Mockito.doReturn(0.46129).when(testFiringPlan).getExpectedCriticals();
        Mockito.doReturn(0.02005).when(testFiringPlan).getKillProbability();
        Mockito.doReturn(0).when(testFiringPlan).getHeat();
        Mockito.doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(Mockito.any(Targetable.class), Mockito.anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        Assert.assertEquals(10.6154, testFiringPlan.getUtility(), TOLERANCE);
        Assert.assertTrue(baseUtility > testFiringPlan.getUtility());

        // Test a higher heat plan.
        testFiringPlan = Mockito.spy(new FiringPlan(mockTarget));
        Mockito.doReturn(15.0).when(testFiringPlan).getExpectedDamage();
        Mockito.doReturn(0.46129).when(testFiringPlan).getExpectedCriticals();
        Mockito.doReturn(0.02005).when(testFiringPlan).getKillProbability();
        Mockito.doReturn(15).when(testFiringPlan).getHeat();
        Mockito.doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(Mockito.any(Targetable.class), Mockito.anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        Assert.assertEquals(-29.3846, testFiringPlan.getUtility(), TOLERANCE);
        Assert.assertTrue(baseUtility > testFiringPlan.getUtility());

        // Test a higher heat tolerance.
        overheatTolerance = 10;
        testFiringPlan = Mockito.spy(new FiringPlan(mockTarget));
        Mockito.doReturn(15.0).when(testFiringPlan).getExpectedDamage();
        Mockito.doReturn(0.46129).when(testFiringPlan).getExpectedCriticals();
        Mockito.doReturn(0.02005).when(testFiringPlan).getKillProbability();
        Mockito.doReturn(15).when(testFiringPlan).getHeat();
        Mockito.doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(Mockito.any(Targetable.class), Mockito.anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        Assert.assertEquals(-4.38459, testFiringPlan.getUtility(), TOLERANCE);
        Assert.assertTrue(baseUtility > testFiringPlan.getUtility());

        // Test a lower heat tolerance.
        overheatTolerance = 0;
        testFiringPlan = Mockito.spy(new FiringPlan(mockTarget));
        Mockito.doReturn(15.0).when(testFiringPlan).getExpectedDamage();
        Mockito.doReturn(0.46129).when(testFiringPlan).getExpectedCriticals();
        Mockito.doReturn(0.02005).when(testFiringPlan).getKillProbability();
        Mockito.doReturn(15).when(testFiringPlan).getHeat();
        Mockito.doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(Mockito.any(Targetable.class), Mockito.anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        Assert.assertEquals(-54.3846, testFiringPlan.getUtility(), TOLERANCE);
        Assert.assertTrue(baseUtility > testFiringPlan.getUtility());

        // Basic punch attack.
        PhysicalInfo testPhysicalInfo = Mockito.spy(new PhysicalInfo(mockPrincess));
        Mockito.doReturn(15.0).when(testPhysicalInfo).getExpectedDamage();
        Mockito.doReturn(0.46129).when(testPhysicalInfo).getExpectedCriticals();
        Mockito.doReturn(0.02005).when(testPhysicalInfo).getKillProbability();
        Mockito.doReturn(0.5).when(testPhysicalInfo).getProbabilityToHit();
        testFireControl.calculateUtility(testPhysicalInfo);
        Assert.assertEquals(baseUtility, testPhysicalInfo.getUtility(), TOLERANCE);

        // Test a punch that cannot hit.
        testPhysicalInfo = Mockito.spy(new PhysicalInfo(mockPrincess));
        Mockito.doReturn(15.0).when(testPhysicalInfo).getExpectedDamage();
        Mockito.doReturn(0.46129).when(testPhysicalInfo).getExpectedCriticals();
        Mockito.doReturn(0.02005).when(testPhysicalInfo).getKillProbability();
        Mockito.doReturn(0.0).when(testPhysicalInfo).getProbabilityToHit();
        testFireControl.calculateUtility(testPhysicalInfo);
        Assert.assertEquals(-10000, testPhysicalInfo.getUtility(), TOLERANCE);

        // Kick an ejected pilot.
        testPhysicalInfo = Mockito.spy(new PhysicalInfo(mockPrincess));
        Mockito.doReturn(15.0).when(testPhysicalInfo).getExpectedDamage();
        Mockito.doReturn(0.46129).when(testPhysicalInfo).getExpectedCriticals();
        Mockito.doReturn(0.02005).when(testPhysicalInfo).getKillProbability();
        Mockito.doReturn(0.5).when(testPhysicalInfo).getProbabilityToHit();
        Mockito.doReturn(mockPilot).when(testPhysicalInfo).getTarget();
        testFireControl.calculateUtility(testPhysicalInfo);
        Assert.assertEquals(-979.3846, testPhysicalInfo.getUtility(), TOLERANCE);
        Assert.assertTrue(baseUtility > testPhysicalInfo.getUtility());

        // Increase the kill chance.
        testPhysicalInfo = Mockito.spy(new PhysicalInfo(mockPrincess));
        Mockito.doReturn(15.0).when(testPhysicalInfo).getExpectedDamage();
        Mockito.doReturn(0.46129).when(testPhysicalInfo).getExpectedCriticals();
        Mockito.doReturn(0.12005).when(testPhysicalInfo).getKillProbability();
        Mockito.doReturn(0.5).when(testPhysicalInfo).getProbabilityToHit();
        testFireControl.calculateUtility(testPhysicalInfo);
        Assert.assertEquals(25.6154, testPhysicalInfo.getUtility(), TOLERANCE);
        Assert.assertTrue(baseUtility < testPhysicalInfo.getUtility());

        // Decrease the kill chance.
        testPhysicalInfo = Mockito.spy(new PhysicalInfo(mockPrincess));
        Mockito.doReturn(15.0).when(testPhysicalInfo).getExpectedDamage();
        Mockito.doReturn(0.46129).when(testPhysicalInfo).getExpectedCriticals();
        Mockito.doReturn(0.01005).when(testPhysicalInfo).getKillProbability();
        Mockito.doReturn(0.5).when(testPhysicalInfo).getProbabilityToHit();
        testFireControl.calculateUtility(testPhysicalInfo);
        Assert.assertEquals(20.1154, testPhysicalInfo.getUtility(), TOLERANCE);
        Assert.assertTrue(baseUtility > testPhysicalInfo.getUtility());

        // Increase the # crits.
        testPhysicalInfo = Mockito.spy(new PhysicalInfo(mockPrincess));
        Mockito.doReturn(15.0).when(testPhysicalInfo).getExpectedDamage();
        Mockito.doReturn(0.86129).when(testPhysicalInfo).getExpectedCriticals();
        Mockito.doReturn(0.02005).when(testPhysicalInfo).getKillProbability();
        Mockito.doReturn(0.5).when(testPhysicalInfo).getProbabilityToHit();
        testFireControl.calculateUtility(testPhysicalInfo);
        Assert.assertEquals(24.6154, testPhysicalInfo.getUtility(), TOLERANCE);
        Assert.assertTrue(baseUtility < testPhysicalInfo.getUtility());

        // Decrease the # crits.
        testPhysicalInfo = Mockito.spy(new PhysicalInfo(mockPrincess));
        Mockito.doReturn(15.0).when(testPhysicalInfo).getExpectedDamage();
        Mockito.doReturn(0.26129).when(testPhysicalInfo).getExpectedCriticals();
        Mockito.doReturn(0.02005).when(testPhysicalInfo).getKillProbability();
        Mockito.doReturn(0.5).when(testPhysicalInfo).getProbabilityToHit();
        testFireControl.calculateUtility(testPhysicalInfo);
        Assert.assertEquals(18.6154, testPhysicalInfo.getUtility(), TOLERANCE);
        Assert.assertTrue(baseUtility > testPhysicalInfo.getUtility());

        // Test a higher damage plan.
        testPhysicalInfo = Mockito.spy(new PhysicalInfo(mockPrincess));
        Mockito.doReturn(20.0).when(testPhysicalInfo).getExpectedDamage();
        Mockito.doReturn(0.46129).when(testPhysicalInfo).getExpectedCriticals();
        Mockito.doReturn(0.02005).when(testPhysicalInfo).getKillProbability();
        Mockito.doReturn(0.5).when(testPhysicalInfo).getProbabilityToHit();
        testFireControl.calculateUtility(testPhysicalInfo);
        Assert.assertEquals(25.6154, testPhysicalInfo.getUtility(), TOLERANCE);
        Assert.assertTrue(baseUtility < testPhysicalInfo.getUtility());

        // Test a lower damage plan.
        testPhysicalInfo = Mockito.spy(new PhysicalInfo(mockPrincess));
        Mockito.doReturn(5.0).when(testPhysicalInfo).getExpectedDamage();
        Mockito.doReturn(0.46129).when(testPhysicalInfo).getExpectedCriticals();
        Mockito.doReturn(0.02005).when(testPhysicalInfo).getKillProbability();
        Mockito.doReturn(0.5).when(testPhysicalInfo).getProbabilityToHit();
        testFireControl.calculateUtility(testPhysicalInfo);
        Assert.assertEquals(10.6154, testPhysicalInfo.getUtility(), TOLERANCE);
        Assert.assertTrue(baseUtility > testPhysicalInfo.getUtility());
    }

    @Test
    public void testGuessFullFiringPlan() {
        FiringPlan expected;
        Mockito.when(mockShooter.getPosition()).thenReturn(mockShooterCoords);
        Mockito.when(mockShooter.isOffBoard()).thenReturn(false);
        Mockito.when(mockShooter.getHeatCapacity()).thenReturn(16);
        Mockito.when(mockShooter.getHeat()).thenReturn(0);
        Mockito.when(mockTarget.getPosition()).thenReturn(mockTargetCoords);
        Mockito.when(mockTarget.isOffBoard()).thenReturn(false);
        Mockito.when(mockBoard.contains(Mockito.eq(mockShooterCoords))).thenReturn(true);
        Mockito.when(mockBoard.contains(Mockito.eq(mockTargetCoords))).thenReturn(true);
        Mockito.doNothing().when(testFireControl).calculateUtility(Mockito.any(FiringPlan.class), Mockito.anyInt(),
                                                                   Mockito.anyBoolean());

        // Test the normal case.
        expected = new FiringPlan(mockTarget);
        expected.add(mockPPCFireInfo);
        expected.add(mockLRMFireInfo);
        FiringPlan actual = testFireControl.guessFullFiringPlan(mockShooter, mockShooterState, mockTarget,
                                                                mockTargetState, mockGame);
        Assert.assertEquals(new HashSet<>(expected), new HashSet<>(actual));

        // Test the target not being on the board.
        Mockito.when(mockTarget.getPosition()).thenReturn(null);
        expected = new FiringPlan(mockTarget);
        Assert.assertEquals(expected, testFireControl.guessFullFiringPlan(mockShooter, mockShooterState, mockTarget,
                                                                          mockTargetState, mockGame));

        // Test the shooter not being on the board.
        Mockito.when(mockShooter.getPosition()).thenReturn(null);
        expected = new FiringPlan(mockTarget);
        Assert.assertEquals(expected, testFireControl.guessFullFiringPlan(mockShooter, mockShooterState, mockTarget,
                                                                          mockTargetState, mockGame));
    }

    @Test
    public void testGuessFullAirToGroundPlan() {
        FiringPlan expected;
        Mockito.when(mockShooter.getPosition()).thenReturn(mockShooterCoords);
        Mockito.when(mockShooter.isOffBoard()).thenReturn(false);
        Mockito.when(mockTarget.getPosition()).thenReturn(mockTargetCoords);
        Mockito.when(mockTarget.isOffBoard()).thenReturn(false);
        Mockito.when(mockBoard.contains(Mockito.eq(mockShooterCoords))).thenReturn(true);
        Mockito.when(mockBoard.contains(Mockito.eq(mockTargetCoords))).thenReturn(true);
        Mockito.doNothing().when(testFireControl).calculateUtility(Mockito.any(FiringPlan.class), Mockito.anyInt(),
                                                                   Mockito.anyBoolean());

        MovePath mockFlightPath = Mockito.mock(MovePath.class);

        // Test the normal case.
        expected = new FiringPlan(mockTarget);
        expected.add(mockPPCFireInfo);
        expected.add(mockLRMFireInfo);
        FiringPlan actual = testFireControl.guessFullAirToGroundPlan(mockShooter, mockTarget, mockTargetState,
                                                                     mockFlightPath, mockGame, true);
        Assert.assertEquals(new HashSet<>(expected), new HashSet<>(actual));

        // test the target not being on the board.
        Mockito.when(mockTarget.getPosition()).thenReturn(null);
        expected = new FiringPlan(mockTarget);
        Assert.assertEquals(expected, testFireControl.guessFullAirToGroundPlan(mockShooter, mockTarget,
                                                                               mockTargetState, mockFlightPath,
                                                                               mockGame, true));

        // Test the shooter not being on the board.
        Mockito.when(mockShooter.getPosition()).thenReturn(null);
        expected = new FiringPlan(mockTarget);
        Assert.assertEquals(expected, testFireControl.guessFullAirToGroundPlan(mockShooter, mockTarget,
                                                                               mockTargetState, mockFlightPath,
                                                                               mockGame, true));
    }

    @Test
    public void testGetFullFiringPlan() {
        FiringPlan expected;
        Mockito.when(mockShooter.getPosition()).thenReturn(mockShooterCoords);
        Mockito.when(mockShooter.isOffBoard()).thenReturn(false);
        Mockito.when(mockTarget.getPosition()).thenReturn(mockTargetCoords);
        Mockito.when(mockTarget.isOffBoard()).thenReturn(false);
        Mockito.when(mockBoard.contains(Mockito.eq(mockShooterCoords))).thenReturn(true);
        Mockito.when(mockBoard.contains(Mockito.eq(mockTargetCoords))).thenReturn(true);
        Mockito.doNothing().when(testFireControl).calculateUtility(Mockito.any(FiringPlan.class), Mockito.anyInt(),
                                                                   Mockito.anyBoolean());

        // Test the normal case.
        expected = new FiringPlan(mockTarget);
        expected.add(mockPPCFireInfo);
        expected.add(mockLRMFireInfo);
        FiringPlan actual = testFireControl.getFullFiringPlan(mockShooter, mockTarget, testToHitThreshold,
                                                              mockGame);
        Assert.assertEquals(new HashSet<>(expected), new HashSet<>(actual));

        // test the target not being on the board.
        Mockito.when(mockTarget.getPosition()).thenReturn(null);
        expected = new FiringPlan(mockTarget);
        Assert.assertEquals(expected, testFireControl.getFullFiringPlan(mockShooter, mockTarget,
                                                                        testToHitThreshold, mockGame));
        Mockito.when(mockTarget.getPosition()).thenReturn(mockTargetCoords);

        // Test the shooter not being on the board.
        Mockito.when(mockShooter.getPosition()).thenReturn(null);
        expected = new FiringPlan(mockTarget);
        Assert.assertEquals(expected, testFireControl.getFullFiringPlan(mockShooter, mockTarget,
                                                                        testToHitThreshold, mockGame));
        Mockito.when(mockShooter.getPosition()).thenReturn(mockShooterCoords);

        // Test the LRMs not having a good enough chance to hit.
        testToHitThreshold.put(mockLRM5, 1.0);
        expected = new FiringPlan(mockTarget);
        expected.add(mockPPCFireInfo);
        Assert.assertEquals(expected, testFireControl.getFullFiringPlan(mockShooter, mockTarget,
                                                                        testToHitThreshold, mockGame));
        testToHitThreshold.put(mockLRM5, 0.0);
    }

    @Test
    public void testCalcFiringPlansUnderHeat() {
        FiringPlan alphaStrike = new FiringPlan(mockTarget);

        Mockito.when(mockShooter.getChassis()).thenReturn("mock chassis");

        Mockito.when(mockPPCFireInfo.getProbabilityToHit()).thenReturn(0.6);
        Mockito.when(mockPPCFireInfo.getHeat()).thenReturn(10);
        Mockito.when(mockPPCFireInfo.getExpectedDamageOnHit()).thenReturn(10.0);
        Mockito.when(mockPPCFireInfo.getExpectedCriticals()).thenReturn(0.46);
        Mockito.when(mockPPCFireInfo.getKillProbability()).thenReturn(0.002);
        Mockito.when(mockPPCFireInfo.getWeapon()).thenReturn(mockPPC);
        Mockito.when(mockPPCFireInfo.getShooter()).thenReturn(mockShooter);
        Mockito.when(mockPPCFireInfo.getDebugDescription()).thenReturn("mock PPC");
        alphaStrike.add(mockPPCFireInfo);

        Mockito.when(mockMLFireInfo.getProbabilityToHit()).thenReturn(0.6);
        Mockito.when(mockMLFireInfo.getHeat()).thenReturn(3);
        Mockito.when(mockMLFireInfo.getExpectedDamageOnHit()).thenReturn(5.0);
        Mockito.when(mockMLFireInfo.getExpectedCriticals()).thenReturn(0.0);
        Mockito.when(mockMLFireInfo.getKillProbability()).thenReturn(0.0);
        Mockito.when(mockMLFireInfo.getWeapon()).thenReturn(mockML);
        Mockito.when(mockMLFireInfo.getShooter()).thenReturn(mockShooter);
        Mockito.when(mockMLFireInfo.getDebugDescription()).thenReturn("mock ML");
        alphaStrike.add(mockMLFireInfo);

        Mockito.when(mockLRMFireInfo.getProbabilityToHit()).thenReturn(0.6);
        Mockito.when(mockLRMFireInfo.getHeat()).thenReturn(1);
        Mockito.when(mockLRMFireInfo.getExpectedDamageOnHit()).thenReturn(3.0);
        Mockito.when(mockLRMFireInfo.getExpectedCriticals()).thenReturn(0.0);
        Mockito.when(mockLRMFireInfo.getKillProbability()).thenReturn(0.0);
        Mockito.when(mockLRMFireInfo.getWeapon()).thenReturn(mockLRM5);
        Mockito.when(mockLRMFireInfo.getShooter()).thenReturn(mockShooter);
        Mockito.when(mockLRMFireInfo.getDebugDescription()).thenReturn("mock LRM");
        alphaStrike.add(mockLRMFireInfo);

        Mounted mockMG = Mockito.mock(Mounted.class);
        shooterWeapons.add(mockMG);
        WeaponFireInfo mockMGFireInfo = Mockito.mock(WeaponFireInfo.class);
        Mockito.when(mockMGFireInfo.getProbabilityToHit()).thenReturn(0.6);
        Mockito.when(mockMGFireInfo.getHeat()).thenReturn(0);
        Mockito.when(mockMGFireInfo.getExpectedDamageOnHit()).thenReturn(2.0);
        Mockito.when(mockMGFireInfo.getExpectedCriticals()).thenReturn(0.0);
        Mockito.when(mockMGFireInfo.getKillProbability()).thenReturn(0.0);
        Mockito.when(mockMGFireInfo.getWeapon()).thenReturn(mockMG);
        Mockito.when(mockMGFireInfo.getShooter()).thenReturn(mockShooter);
        Mockito.when(mockMGFireInfo.getDebugDescription()).thenReturn("mock MG");
        alphaStrike.add(mockMGFireInfo);
        
        Mockito.doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(Mockito.any(Targetable.class), Mockito.anyDouble());

        FiringPlan[] expected = new FiringPlan[15];
        expected[0] = new FiringPlan(mockTarget);
        expected[0].add(mockMGFireInfo);
        expected[0].setUtility(1.2);
        expected[1] = new FiringPlan(mockTarget);
        expected[1].add(mockMGFireInfo);
        expected[1].add(mockLRMFireInfo);
        expected[1].setUtility(3.0);
        expected[2] = new FiringPlan(mockTarget);
        expected[2].add(mockMGFireInfo);
        expected[2].add(mockLRMFireInfo);
        expected[2].setUtility(3.0);
        expected[3] = new FiringPlan(mockTarget);
        expected[3].add(mockMGFireInfo);
        expected[3].add(mockMLFireInfo);
        expected[3].setUtility(4.2);
        expected[4] = new FiringPlan(mockTarget);
        expected[4].add(mockMGFireInfo);
        expected[4].add(mockLRMFireInfo);
        expected[4].add(mockMLFireInfo);
        expected[4].setUtility(6.0);
        expected[5] = new FiringPlan(mockTarget);
        expected[5].add(mockMGFireInfo);
        expected[5].add(mockLRMFireInfo);
        expected[5].add(mockMLFireInfo);
        expected[5].setUtility(6.0);
        expected[6] = new FiringPlan(mockTarget);
        expected[6].add(mockMGFireInfo);
        expected[6].add(mockLRMFireInfo);
        expected[6].add(mockMLFireInfo);
        expected[6].setUtility(6.0);
        expected[7] = new FiringPlan(mockTarget);
        expected[7].add(mockMGFireInfo);
        expected[7].add(mockLRMFireInfo);
        expected[7].add(mockMLFireInfo);
        expected[7].setUtility(6.0);
        expected[8] = new FiringPlan(mockTarget);
        expected[8].add(mockMGFireInfo);
        expected[8].add(mockLRMFireInfo);
        expected[8].add(mockMLFireInfo);
        expected[8].setUtility(6.0);
        expected[9] = new FiringPlan(mockTarget);
        expected[9].add(mockMGFireInfo);
        expected[9].add(mockLRMFireInfo);
        expected[9].add(mockMLFireInfo);
        expected[9].setUtility(6.0);
        expected[10] = new FiringPlan(mockTarget);
        expected[10].add(mockMGFireInfo);
        expected[10].add(mockPPCFireInfo);
        expected[10].setUtility(11.9);
        expected[11] = new FiringPlan(mockTarget);
        expected[11].add(mockMGFireInfo);
        expected[11].add(mockLRMFireInfo);
        expected[11].add(mockPPCFireInfo);
        expected[11].setUtility(13.7);
        expected[12] = new FiringPlan(mockTarget);
        expected[12].add(mockMGFireInfo);
        expected[12].add(mockLRMFireInfo);
        expected[12].add(mockPPCFireInfo);
        expected[12].setUtility(13.7);
        expected[13] = new FiringPlan(mockTarget);
        expected[13].add(mockMGFireInfo);
        expected[13].add(mockMLFireInfo);
        expected[13].add(mockPPCFireInfo);
        expected[13].setUtility(14.9);
        expected[14] = new FiringPlan(mockTarget);
        expected[14].add(mockMGFireInfo);
        expected[14].add(mockLRMFireInfo);
        expected[14].add(mockMLFireInfo);
        expected[14].add(mockPPCFireInfo);
        expected[14].setUtility(16.7);
        FiringPlan[] actual = testFireControl.calcFiringPlansUnderHeat(mockShooter, alphaStrike);
        assertArrayEquals(expected, actual);
    }

    private void assertArrayEquals(FiringPlan[] expected, Object actual) {
        Assert.assertNotNull(actual);
        Assert.assertTrue("actual: " + actual.getClass().getName(), actual instanceof FiringPlan[]);

        FiringPlan[] actualArray = (FiringPlan[]) actual;
        Assert.assertEquals(expected.length, actualArray.length);

        StringBuilder failure = new StringBuilder();
        for (int i = 0; i < expected.length; i++) {
            if ((expected[i] == null) && (actualArray[i] != null)) {
                failure.append("\nExpected[").append(i).append("]: null");
                failure.append("\nActual[").append(i).append("]:   ").append(actualArray[i].getDebugDescription(true));
                continue;
            }
            if (!expected[i].equals(actualArray[i])) {
                failure.append("\nExpected[").append(i).append("]: ").append(expected[i].getDebugDescription(true));
                if (actualArray[i] == null) {
                    failure.append("\nActual[").append(i).append("]:   null");
                } else {
                    failure.append("\nActual[").append(i).append("]:   ").append(actualArray[i].getDebugDescription
                            (true));
                }
            }
        }

        if (!StringUtil.isNullOrEmpty(failure.toString())) {
            Assert.fail(failure.toString());
        }
    }

    /**
     * Test to make sure that Princess will choose a FiringPlan that shots at
     * a MechWarrior, instead of choosing to do nothing.
     */
    @Test
    public void testCalcFiringPlansAtMechwarrior() {
        mockTarget = Mockito.mock(MechWarrior.class);
        Mockito.when(mockPPCFireInfo.getProbabilityToHit()).thenReturn(0.6);
        Mockito.when(mockPPCFireInfo.getHeat()).thenReturn(10);
        Mockito.when(mockPPCFireInfo.getExpectedDamageOnHit()).thenReturn(10.0);

        Mockito.when(mockMLFireInfo.getProbabilityToHit()).thenReturn(0.6);
        Mockito.when(mockMLFireInfo.getHeat()).thenReturn(3);
        Mockito.when(mockMLFireInfo.getExpectedDamageOnHit()).thenReturn(5.0);

        Mockito.when(mockLRMFireInfo.getProbabilityToHit()).thenReturn(0.6);
        Mockito.when(mockLRMFireInfo.getHeat()).thenReturn(1);
        Mockito.when(mockLRMFireInfo.getExpectedDamageOnHit()).thenReturn(3.0);

        Mockito.doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(Mockito.any(Targetable.class), Mockito.anyDouble());
        
        Mockito.when(mockShooter.getPosition()).thenReturn(mockShooterCoords);
        Mockito.when(mockTarget.getPosition()).thenReturn(mockTargetCoords);
        Mockito.when(mockShooter.getWeaponList()).thenReturn(shooterWeapons);
        FiringPlan plan = testFireControl.getBestFiringPlan(mockShooter, mockTarget, mockGame,
                                                            testToHitThreshold);
        Assert.assertNotEquals(0, plan.getUtility(), 0.00001);
    }
}
