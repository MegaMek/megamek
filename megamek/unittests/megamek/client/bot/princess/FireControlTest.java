/*
 * Copyright (c) 2000-2011 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.bot.princess;

import megamek.client.bot.princess.PathRanker.PathRankerType;
import megamek.codeUtilities.StringUtility;
import megamek.common.*;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.weapons.StopSwarmAttack;
import megamek.common.weapons.missiles.ATMWeapon;
import megamek.common.weapons.missiles.MMLWeapon;
import megamek.server.SmokeCloud;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import java.math.BigInteger;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 12/18/13 1:38 PM
 */
public class FireControlTest {

    private static final int MOCK_TARGET_ID = 10;

    // AC5
    private Mounted mockWeaponAC5;
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
    private Mounted mockWeaponLB10X;
    private WeaponType mockLB10X;
    @SuppressWarnings("FieldCanBeLocal")
    private AmmoType mockAmmoTypeLB10XSlug;
    private Mounted mockAmmoLB10XSlug;
    @SuppressWarnings("FieldCanBeLocal")
    private AmmoType mockAmmoTypeLB10XCluster;
    private Mounted mockAmmoLB10XCluster;

    // MML
    private Mounted mockWeaponMML5;
    private WeaponType mockMML5;
    @SuppressWarnings("FieldCanBeLocal")
    private AmmoType mockAmmoTypeSRM5;
    private Mounted mockAmmoSRM5;
    @SuppressWarnings("FieldCanBeLocal")
    private AmmoType mockAmmoTypeLRM5;
    private Mounted mockAmmoLRM5;
    @SuppressWarnings("FieldCanBeLocal")
    private AmmoType mockAmmoTypeInferno5;
    private Mounted mockAmmoInferno5;
    @SuppressWarnings("FieldCanBeLocal")
    private AmmoType mockAmmoTypeLrm5Frag;
    private Mounted mockAmmoLrm5Frag;

    // ATM
    private Mounted mockAtm5Weapon;
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
    private Hex mockHex;
    private Board mockBoard;
    private Game mockGame;

    private Princess mockPrincess;

    private ArrayList<Mounted> shooterWeapons;
    private Mounted mockPPC;
    private Mounted mockML;
    private Mounted mockLRM5;
    private WeaponFireInfo mockPPCFireInfo;
    private WeaponFireInfo mockMLFireInfo;
    private WeaponFireInfo mockLRMFireInfo;
    private WeaponFireInfo mockMMLLRM5FireInfo;
    private WeaponFireInfo mockMMLSRM5FireInfo;
    private WeaponFireInfo mockLB10XSlugFireInfo;
    private WeaponFireInfo mockLB10XClusterFireInfo;

    private Map<Mounted, Double> testToHitThreshold;

    private FireControl testFireControl;

    @BeforeAll
    public static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    public void beforeEach() {
        mockPrincess = mock(Princess.class);

        final BehaviorSettings mockBehavior = mock(BehaviorSettings.class);
        when(mockPrincess.getBehaviorSettings()).thenReturn(mockBehavior);

        final BasicPathRanker mockPathRanker = mock(BasicPathRanker.class);
        when(mockPrincess.getPathRanker(PathRankerType.Basic)).thenReturn(mockPathRanker);

        final IHonorUtil mockHonorUtil = mock(IHonorUtil.class);
        when(mockPrincess.getHonorUtil()).thenReturn(mockHonorUtil);

        mockShooter = mock(BipedMech.class);
        when(mockShooter.getId()).thenReturn(1);
        when(mockShooter.getMaxWeaponRange()).thenReturn(21);
        when(mockShooter.getHeatCapacity()).thenReturn(10);
        when(mockShooter.getHeat()).thenReturn(0);
        mockShooterState = mock(EntityState.class);
        mockShooterCoords = new Coords(0, 0);
        when(mockShooterState.getPosition()).thenReturn(mockShooterCoords);
        mockShooterMoveMod = new ToHitData();

        mockCrew = mock(Crew.class);
        when(mockCrew.getPiloting()).thenReturn(5);
        when(mockCrew.getGunnery()).thenReturn(4);
        when(mockShooter.getCrew()).thenReturn(mockCrew);

        mockTargetState = mock(EntityState.class);
        when(mockTargetState.isBuilding()).thenReturn(false);
        when(mockTargetState.getHeat()).thenReturn(0);
        mockTargetMoveMod = new ToHitData();
        mockTargetCoords = new Coords(10, 0);
        when(mockTargetState.getPosition()).thenReturn(mockTargetCoords);

        mockGameOptions = mock(GameOptions.class);
        // logic within getFullFiringPlan checks if this feature is turned on then checks whether the
        // weapon type is AMS
        // since it's more of a pain to set up all the weapon types, we simply pretend the feature is turned on
        when(mockGameOptions.booleanOption(eq(OptionsConstants.ADVCOMBAT_TACOPS_MANUAL_AMS)))
            .thenReturn(true);

        mockHex = mock(Hex.class);

        mockBoard = mock(Board.class);
        when(mockBoard.getHex(any(Coords.class))).thenReturn(mockHex);
        when(mockBoard.contains(any(Coords.class))).thenReturn(true);

        mockGame = mock(Game.class);
        when(mockGame.getOptions()).thenReturn(mockGameOptions);
        when(mockGame.getBoard()).thenReturn(mockBoard);

        mockTarget = mock(BipedMech.class);
        when(mockTarget.getDisplayName()).thenReturn("mock target");
        when(mockTarget.getId()).thenReturn(MOCK_TARGET_ID);
        when(mockTarget.isMilitary()).thenReturn(true);

        testFireControl = spy(new FireControl(mockPrincess));
        doReturn(mockShooterMoveMod)
               .when(testFireControl)
               .getAttackerMovementModifier(any(Game.class), anyInt(), nullable(EntityMovementType.class));
        doReturn(mockTargetMoveMod)
               .when(testFireControl)
               .getTargetMovementModifier(anyInt(), anyBoolean(), anyBoolean(), any(Game.class));

        doReturn(false).when(testFireControl).isCommander(any(Entity.class));
        doReturn(false).when(testFireControl).isSubCommander(any(Entity.class));

        // AC5
        mockWeaponTypeAC5 = mock(WeaponType.class);
        mockWeaponAC5 = mock(Mounted.class);
        when(mockWeaponAC5.getType()).thenReturn(mockWeaponTypeAC5);
        when(mockWeaponTypeAC5.getAmmoType()).thenReturn(AmmoType.T_AC);
        mockAmmoTypeAC5Std = mock(AmmoType.class);
        when(mockAmmoTypeAC5Std.getAmmoType()).thenReturn(AmmoType.T_AC);
        when(mockAmmoTypeAC5Std.getMunitionType()).thenReturn(EnumSet.of(AmmoType.Munitions.M_STANDARD));
        mockAmmoAC5Std = mock(Mounted.class);
        when(mockAmmoAC5Std.getType()).thenReturn(mockAmmoTypeAC5Std);
        when(mockAmmoAC5Std.isAmmoUsable()).thenReturn(true);
        mockAmmoTypeAC5Flak = mock(AmmoType.class);
        when(mockAmmoTypeAC5Flak.getAmmoType()).thenReturn(AmmoType.T_AC);
        when(mockAmmoTypeAC5Flak.getMunitionType()).thenReturn(EnumSet.of(AmmoType.Munitions.M_FLAK));
        mockAmmoAC5Flak = mock(Mounted.class);
        when(mockAmmoAC5Flak.getType()).thenReturn(mockAmmoTypeAC5Flak);
        when(mockAmmoAC5Flak.isAmmoUsable()).thenReturn(true);
        mockAmmoTypeAC5Incendiary = mock(AmmoType.class);
        when(mockAmmoTypeAC5Incendiary.getMunitionType()).thenReturn(EnumSet.of(AmmoType.Munitions.M_INCENDIARY_AC));
        when(mockAmmoTypeAC5Incendiary.getAmmoType()).thenReturn(AmmoType.T_AC);
        mockAmmoAc5Incendiary = mock(Mounted.class);
        when(mockAmmoAc5Incendiary.getType()).thenReturn(mockAmmoTypeAC5Incendiary);
        when(mockAmmoAc5Incendiary.isAmmoUsable()).thenReturn(true);
        mockAmmoTypeAc5Flechette = mock(AmmoType.class);
        when(mockAmmoTypeAc5Flechette.getAmmoType()).thenReturn(AmmoType.T_AC);
        when(mockAmmoTypeAc5Flechette.getMunitionType()).thenReturn(EnumSet.of(AmmoType.Munitions.M_FLECHETTE));
        mockAmmoAc5Flechette = mock(Mounted.class);
        when(mockAmmoAc5Flechette.getType()).thenReturn(mockAmmoTypeAc5Flechette);
        when(mockAmmoAc5Flechette.isAmmoUsable()).thenReturn(true);

        doReturn(true).when(mockAmmoTypeAC5Std).equalsAmmoTypeOnly(eq(mockAmmoTypeAC5Std));
        doReturn(true).when(mockAmmoTypeAC5Std).equalsAmmoTypeOnly(eq(mockAmmoTypeAC5Flak));
        doReturn(true).when(mockAmmoTypeAC5Std).equalsAmmoTypeOnly(eq(mockAmmoTypeAC5Incendiary));
        doReturn(true).when(mockAmmoTypeAC5Std).equalsAmmoTypeOnly(eq(mockAmmoTypeAc5Flechette));
        doReturn(true).when(mockAmmoTypeAC5Flak).equalsAmmoTypeOnly(eq(mockAmmoTypeAC5Std));
        doReturn(true).when(mockAmmoTypeAC5Flak).equalsAmmoTypeOnly(eq(mockAmmoTypeAC5Flak));
        doReturn(true).when(mockAmmoTypeAC5Flak).equalsAmmoTypeOnly(eq(mockAmmoTypeAC5Incendiary));
        doReturn(true).when(mockAmmoTypeAC5Flak).equalsAmmoTypeOnly(eq(mockAmmoTypeAc5Flechette));
        doReturn(true).when(mockAmmoTypeAC5Incendiary).equalsAmmoTypeOnly(eq(mockAmmoTypeAC5Std));
        doReturn(true).when(mockAmmoTypeAC5Incendiary).equalsAmmoTypeOnly(eq(mockAmmoTypeAC5Flak));
        doReturn(true).when(mockAmmoTypeAC5Incendiary).equalsAmmoTypeOnly(eq(mockAmmoTypeAC5Incendiary));
        doReturn(true).when(mockAmmoTypeAC5Incendiary).equalsAmmoTypeOnly(eq(mockAmmoTypeAc5Flechette));
        doReturn(true).when(mockAmmoTypeAc5Flechette).equalsAmmoTypeOnly(eq(mockAmmoTypeAC5Std));
        doReturn(true).when(mockAmmoTypeAc5Flechette).equalsAmmoTypeOnly(eq(mockAmmoTypeAC5Flak));
        doReturn(true).when(mockAmmoTypeAc5Flechette).equalsAmmoTypeOnly(eq(mockAmmoTypeAC5Incendiary));
        doReturn(true).when(mockAmmoTypeAc5Flechette).equalsAmmoTypeOnly(eq(mockAmmoTypeAc5Flechette));

        // LB10X
        mockLB10X = mock(WeaponType.class);
        mockAmmoTypeLB10XSlug = mock(AmmoType.class);
        mockAmmoLB10XSlug = mock(Mounted.class);
        mockAmmoTypeLB10XCluster = mock(AmmoType.class);
        mockAmmoLB10XCluster = mock(Mounted.class);
        mockWeaponLB10X = mock(Mounted.class);
        when(mockWeaponLB10X.getType()).thenReturn(mockLB10X);
        when(mockLB10X.getAmmoType()).thenReturn(AmmoType.T_AC_LBX);
        when(mockAmmoTypeLB10XSlug.getAmmoType()).thenReturn(AmmoType.T_AC_LBX);
        when(mockAmmoTypeLB10XSlug.getMunitionType()).thenReturn(EnumSet.of(AmmoType.Munitions.M_STANDARD));
        when(mockAmmoLB10XSlug.getType()).thenReturn(mockAmmoTypeLB10XSlug);
        when(mockAmmoLB10XSlug.isAmmoUsable()).thenReturn(true);
        when(mockAmmoTypeLB10XCluster.getAmmoType()).thenReturn(AmmoType.T_AC_LBX);
        when(mockAmmoTypeLB10XCluster.getMunitionType()).thenReturn(EnumSet.of(AmmoType.Munitions.M_CLUSTER));
        when(mockAmmoLB10XCluster.getType()).thenReturn(mockAmmoTypeLB10XCluster);
        when(mockAmmoLB10XCluster.isAmmoUsable()).thenReturn(true);

        doReturn(true).when(mockAmmoTypeLB10XSlug).equalsAmmoTypeOnly(eq(mockAmmoTypeLB10XSlug));
        doReturn(true).when(mockAmmoTypeLB10XSlug).equalsAmmoTypeOnly(eq(mockAmmoTypeLB10XCluster));
        doReturn(true).when(mockAmmoTypeLB10XCluster).equalsAmmoTypeOnly(eq(mockAmmoTypeLB10XSlug));
        doReturn(true).when(mockAmmoTypeLB10XCluster).equalsAmmoTypeOnly(eq(mockAmmoTypeLB10XCluster));

        mockLB10XSlugFireInfo = mock(WeaponFireInfo.class);
        mockLB10XClusterFireInfo = mock(WeaponFireInfo.class);
        // TN 8, average cluster
        when(mockLB10XSlugFireInfo.getProbabilityToHit()).thenReturn(0.4166);
        when(mockLB10XSlugFireInfo.getExpectedDamage()).thenReturn(0.58*6);
        // TN 5 (as flak), average cluster
        when(mockLB10XClusterFireInfo.getProbabilityToHit()).thenReturn(0.8333);
        when(mockLB10XClusterFireInfo.getExpectedDamage()).thenReturn(0.8333*6);

        // Firing Slug ammo
        doReturn(mockLB10XSlugFireInfo).when(testFireControl).buildWeaponFireInfo(any(Entity.class),
                any(EntityState.class), any(Targetable.class), any(EntityState.class), eq(mockWeaponLB10X), eq(mockAmmoLB10XSlug),
                any(Game.class), anyBoolean());
        doReturn(mockLB10XSlugFireInfo).when(testFireControl).buildWeaponFireInfo(any(Entity.class),
                any(MovePath.class), any(Targetable.class), any(EntityState.class), eq(mockWeaponLB10X), eq(mockAmmoLB10XSlug),
                any(Game.class), anyBoolean(), anyBoolean());
        doReturn(mockLB10XSlugFireInfo).when(testFireControl).buildWeaponFireInfo(any(Entity.class),
                any(Targetable.class), eq(mockWeaponLB10X), eq(mockAmmoLB10XSlug), any(Game.class), anyBoolean());

        // Firing Cluster ammo
        doReturn(mockLB10XClusterFireInfo).when(testFireControl).buildWeaponFireInfo(any(Entity.class),
                any(EntityState.class), any(Targetable.class), any(EntityState.class), eq(mockWeaponLB10X), eq(mockAmmoLB10XCluster),
                any(Game.class), anyBoolean());
        doReturn(mockLB10XClusterFireInfo).when(testFireControl).buildWeaponFireInfo(any(Entity.class),
                any(MovePath.class), any(Targetable.class), any(EntityState.class), eq(mockWeaponLB10X), eq(mockAmmoLB10XCluster),
                any(Game.class), anyBoolean(), anyBoolean());
        doReturn(mockLB10XClusterFireInfo).when(testFireControl).buildWeaponFireInfo(any(Entity.class),
                any(Targetable.class), eq(mockWeaponLB10X), eq(mockAmmoLB10XCluster), any(Game.class), anyBoolean());


        // MML
        mockMML5 = mock(MMLWeapon.class);
        mockAmmoTypeSRM5 = mock(AmmoType.class);
        mockAmmoSRM5 = mock(Mounted.class);
        mockAmmoTypeLRM5 = mock(AmmoType.class);
        mockAmmoLRM5 = mock(Mounted.class);
        mockAmmoTypeInferno5 = mock(AmmoType.class);
        mockAmmoInferno5 = mock(Mounted.class);
        mockAmmoTypeLrm5Frag = mock(AmmoType.class);
        mockAmmoLrm5Frag = mock(Mounted.class);
        mockWeaponMML5 = mock(Mounted.class);
        when(mockWeaponMML5.getType()).thenReturn(mockMML5);
        when(mockMML5.getAmmoType()).thenReturn(AmmoType.T_MML);
        when(mockAmmoTypeSRM5.getMunitionType()).thenReturn(EnumSet.of(AmmoType.Munitions.M_STANDARD));
        when(mockAmmoTypeSRM5.getAmmoType()).thenReturn(AmmoType.T_MML);
        when(mockAmmoSRM5.getType()).thenReturn(mockAmmoTypeSRM5);
        when(mockAmmoSRM5.isAmmoUsable()).thenReturn(true);
        when(mockAmmoTypeLRM5.getMunitionType()).thenReturn(EnumSet.of(AmmoType.Munitions.M_STANDARD));
        when(mockAmmoTypeLRM5.hasFlag(any(BigInteger.class))).thenReturn(false);
        when(mockAmmoTypeLRM5.hasFlag(eq(AmmoType.F_MML_LRM))).thenReturn(true);
        when(mockAmmoTypeLRM5.getAmmoType()).thenReturn(AmmoType.T_MML);
        when(mockAmmoLRM5.getType()).thenReturn(mockAmmoTypeLRM5);
        when(mockAmmoLRM5.isAmmoUsable()).thenReturn(true);
        when(mockAmmoTypeInferno5.getMunitionType()).thenReturn(EnumSet.of(AmmoType.Munitions.M_INFERNO));
        when(mockAmmoTypeInferno5.getAmmoType()).thenReturn(AmmoType.T_MML);
        when(mockAmmoInferno5.getType()).thenReturn(mockAmmoTypeInferno5);
        when(mockAmmoInferno5.isAmmoUsable()).thenReturn(true);
        when(mockAmmoTypeLrm5Frag.getMunitionType()).thenReturn(EnumSet.of(AmmoType.Munitions.M_FRAGMENTATION));
        when(mockAmmoTypeLrm5Frag.hasFlag(eq(AmmoType.F_MML_LRM))).thenReturn(true);
        when(mockAmmoTypeLrm5Frag.getAmmoType()).thenReturn(AmmoType.T_MML);
        when(mockAmmoLrm5Frag.getType()).thenReturn(mockAmmoTypeLrm5Frag);
        when(mockAmmoLrm5Frag.isAmmoUsable()).thenReturn(true);

        doReturn(true).when(mockAmmoTypeSRM5).equalsAmmoTypeOnly(eq(mockAmmoTypeSRM5));
        doReturn(true).when(mockAmmoTypeSRM5).equalsAmmoTypeOnly(eq(mockAmmoTypeLRM5));
        doReturn(true).when(mockAmmoTypeSRM5).equalsAmmoTypeOnly(eq(mockAmmoTypeInferno5));
        doReturn(true).when(mockAmmoTypeSRM5).equalsAmmoTypeOnly(eq(mockAmmoTypeLrm5Frag));
        doReturn(true).when(mockAmmoTypeLRM5).equalsAmmoTypeOnly(eq(mockAmmoTypeSRM5));
        doReturn(true).when(mockAmmoTypeLRM5).equalsAmmoTypeOnly(eq(mockAmmoTypeLRM5));
        doReturn(true).when(mockAmmoTypeLRM5).equalsAmmoTypeOnly(eq(mockAmmoTypeInferno5));
        doReturn(true).when(mockAmmoTypeLRM5).equalsAmmoTypeOnly(eq(mockAmmoTypeLrm5Frag));
        doReturn(true).when(mockAmmoTypeInferno5).equalsAmmoTypeOnly(eq(mockAmmoTypeSRM5));
        doReturn(true).when(mockAmmoTypeInferno5).equalsAmmoTypeOnly(eq(mockAmmoTypeLRM5));
        doReturn(true).when(mockAmmoTypeInferno5).equalsAmmoTypeOnly(eq(mockAmmoTypeInferno5));
        doReturn(true).when(mockAmmoTypeInferno5).equalsAmmoTypeOnly(eq(mockAmmoTypeLrm5Frag));
        doReturn(true).when(mockAmmoTypeLrm5Frag).equalsAmmoTypeOnly(eq(mockAmmoTypeSRM5));
        doReturn(true).when(mockAmmoTypeLrm5Frag).equalsAmmoTypeOnly(eq(mockAmmoTypeLRM5));
        doReturn(true).when(mockAmmoTypeLrm5Frag).equalsAmmoTypeOnly(eq(mockAmmoTypeInferno5));
        doReturn(true).when(mockAmmoTypeLrm5Frag).equalsAmmoTypeOnly(eq(mockAmmoTypeLrm5Frag));

        // ATM
        mockAtm5Weapon = mock(Mounted.class);
        mockAtm5 = mock(ATMWeapon.class);
        mockAmmoTypeAtm5He = mock(AmmoType.class);
        mockAmmoAtm5He = mock(Mounted.class);
        mockAmmoTypeAtm5St = mock(AmmoType.class);
        mockAmmoAtm5St = mock(Mounted.class);
        mockAmmoTypeAtm5Er = mock(AmmoType.class);
        mockAmmoAtm5Er = mock(Mounted.class);
        mockAmmoTypeAtm5Inferno = mock(AmmoType.class);
        mockAmmoAtm5Inferno = mock(Mounted.class);
        when(mockAtm5Weapon.getType()).thenReturn(mockAtm5);
        when(mockAtm5.getAmmoType()).thenReturn(AmmoType.T_ATM);
        when(mockAtm5.getRackSize()).thenReturn(5);
        when(mockAmmoTypeAtm5He.getAmmoType()).thenReturn(AmmoType.T_ATM);
        when(mockAmmoTypeAtm5He.getMunitionType()).thenReturn(EnumSet.of(AmmoType.Munitions.M_HIGH_EXPLOSIVE));
        when(mockAmmoTypeAtm5He.getRackSize()).thenReturn(5);
        when(mockAmmoAtm5He.getType()).thenReturn(mockAmmoTypeAtm5He);
        when(mockAmmoAtm5He.isAmmoUsable()).thenReturn(true);
        when(mockAmmoTypeAtm5St.getMunitionType()).thenReturn(EnumSet.of(AmmoType.Munitions.M_STANDARD));
        when(mockAmmoTypeAtm5St.getAmmoType()).thenReturn(AmmoType.T_ATM);
        when(mockAmmoTypeAtm5St.getRackSize()).thenReturn(5);
        when(mockAmmoAtm5St.getType()).thenReturn(mockAmmoTypeAtm5St);
        when(mockAmmoAtm5St.isAmmoUsable()).thenReturn(true);
        when(mockAmmoTypeAtm5Er.getMunitionType()).thenReturn(EnumSet.of(AmmoType.Munitions.M_EXTENDED_RANGE));
        when(mockAmmoTypeAtm5Er.getAmmoType()).thenReturn(AmmoType.T_ATM);
        when(mockAmmoTypeAtm5Er.getRackSize()).thenReturn(5);
        when(mockAmmoAtm5Er.getType()).thenReturn(mockAmmoTypeAtm5Er);
        when(mockAmmoAtm5Er.isAmmoUsable()).thenReturn(true);
        when(mockAmmoTypeAtm5Inferno.getMunitionType()).thenReturn(EnumSet.of(AmmoType.Munitions.M_IATM_IIW));
        when(mockAmmoTypeAtm5Inferno.getAmmoType()).thenReturn(AmmoType.T_ATM);
        when(mockAmmoTypeAtm5Inferno.getRackSize()).thenReturn(5);
        when(mockAmmoAtm5Inferno.getType()).thenReturn(mockAmmoTypeAtm5Inferno);
        when(mockAmmoAtm5Inferno.isAmmoUsable()).thenReturn(true);

        doReturn(true).when(mockAmmoTypeAtm5He).equalsAmmoTypeOnly(eq(mockAmmoTypeAtm5He));
        doReturn(true).when(mockAmmoTypeAtm5He).equalsAmmoTypeOnly(eq(mockAmmoTypeAtm5St));
        doReturn(true).when(mockAmmoTypeAtm5He).equalsAmmoTypeOnly(eq(mockAmmoTypeAtm5Er));
        doReturn(true).when(mockAmmoTypeAtm5He).equalsAmmoTypeOnly(eq(mockAmmoTypeAtm5Inferno));
        doReturn(true).when(mockAmmoTypeAtm5St).equalsAmmoTypeOnly(eq(mockAmmoTypeAtm5He));
        doReturn(true).when(mockAmmoTypeAtm5St).equalsAmmoTypeOnly(eq(mockAmmoTypeAtm5St));
        doReturn(true).when(mockAmmoTypeAtm5St).equalsAmmoTypeOnly(eq(mockAmmoTypeAtm5Er));
        doReturn(true).when(mockAmmoTypeAtm5St).equalsAmmoTypeOnly(eq(mockAmmoTypeAtm5Inferno));
        doReturn(true).when(mockAmmoTypeAtm5Er).equalsAmmoTypeOnly(eq(mockAmmoTypeAtm5He));
        doReturn(true).when(mockAmmoTypeAtm5Er).equalsAmmoTypeOnly(eq(mockAmmoTypeAtm5St));
        doReturn(true).when(mockAmmoTypeAtm5Er).equalsAmmoTypeOnly(eq(mockAmmoTypeAtm5Er));
        doReturn(true).when(mockAmmoTypeAtm5Er).equalsAmmoTypeOnly(eq(mockAmmoTypeAtm5Inferno));
        doReturn(true).when(mockAmmoTypeAtm5Inferno).equalsAmmoTypeOnly(eq(mockAmmoTypeAtm5He));
        doReturn(true).when(mockAmmoTypeAtm5Inferno).equalsAmmoTypeOnly(eq(mockAmmoTypeAtm5St));
        doReturn(true).when(mockAmmoTypeAtm5Inferno).equalsAmmoTypeOnly(eq(mockAmmoTypeAtm5Er));
        doReturn(true).when(mockAmmoTypeAtm5Inferno).equalsAmmoTypeOnly(eq(mockAmmoTypeAtm5Inferno));

        shooterWeapons = new ArrayList<>(3);
        when(mockShooter.getWeaponList()).thenReturn(shooterWeapons);

        // Weapon that will skip check for indirect fire mode
        WeaponType mockWeaponType = mock(WeaponType.class);
        when(mockWeaponType.hasFlag(any())).thenReturn(false);
        when(mockWeaponType.hasModeType(anyString())).thenReturn(false);
        mockPPC = mock(Mounted.class);
        when(mockPPC.getType()).thenReturn(mockWeaponType);
        shooterWeapons.add(mockPPC);
        mockPPCFireInfo = mock(WeaponFireInfo.class);
        when(mockPPCFireInfo.getProbabilityToHit()).thenReturn(0.5);
        doReturn(mockPPCFireInfo).when(testFireControl).buildWeaponFireInfo(any(Entity.class),
                any(EntityState.class), any(Targetable.class), any(EntityState.class), eq(mockPPC), any(Mounted.class),
                any(Game.class), anyBoolean());
        doReturn(mockPPCFireInfo).when(testFireControl).buildWeaponFireInfo(any(Entity.class),
                any(MovePath.class), any(Targetable.class), any(EntityState.class), eq(mockPPC), any(Mounted.class),
                any(Game.class), anyBoolean(), anyBoolean());
        doReturn(mockPPCFireInfo).when(testFireControl).buildWeaponFireInfo(any(Entity.class),
                any(Targetable.class), eq(mockPPC), any(Mounted.class), any(Game.class), anyBoolean());

        mockML = mock(Mounted.class);
        shooterWeapons.add(mockML);
        when(mockML.getType()).thenReturn(mockWeaponType);
        mockMLFireInfo = mock(WeaponFireInfo.class);
        when(mockMLFireInfo.getProbabilityToHit()).thenReturn(0.0);
        doReturn(mockMLFireInfo).when(testFireControl).buildWeaponFireInfo(any(Entity.class),
                any(EntityState.class), any(Targetable.class), any(EntityState.class), eq(mockML), any(Mounted.class),
                any(Game.class), anyBoolean());
        doReturn(mockMLFireInfo).when(testFireControl).buildWeaponFireInfo(any(Entity.class),
                any(MovePath.class), any(Targetable.class), any(EntityState.class), eq(mockML), any(Mounted.class),
                any(Game.class), anyBoolean(), anyBoolean());
        doReturn(mockMLFireInfo).when(testFireControl).buildWeaponFireInfo(any(Entity.class),
                any(Targetable.class), eq(mockML), any(Mounted.class), any(Game.class), anyBoolean());

        mockLRM5 = mock(Mounted.class);
        when(mockLRM5.getType()).thenReturn(mockWeaponType);
        shooterWeapons.add(mockLRM5);
        mockLRMFireInfo = mock(WeaponFireInfo.class);
        when(mockLRMFireInfo.getProbabilityToHit()).thenReturn(0.6);
        doReturn(mockLRMFireInfo).when(testFireControl).buildWeaponFireInfo(any(Entity.class),
                any(EntityState.class), any(Targetable.class), any(EntityState.class), eq(mockLRM5), any(Mounted.class),
                any(Game.class), anyBoolean());
        doReturn(mockLRMFireInfo).when(testFireControl).buildWeaponFireInfo(any(Entity.class),
                any(MovePath.class), any(Targetable.class), any(EntityState.class), eq(mockLRM5), any(Mounted.class),
                any(Game.class), anyBoolean(), anyBoolean());
        doReturn(mockLRMFireInfo).when(testFireControl).buildWeaponFireInfo(any(Entity.class),
                any(Targetable.class), eq(mockLRM5), any(Mounted.class), any(Game.class), anyBoolean());

        when(mockWeaponMML5.getType()).thenReturn(mockWeaponType);
        shooterWeapons.add(mockWeaponMML5);
        mockMMLLRM5FireInfo = mock(WeaponFireInfo.class);
        mockMMLSRM5FireInfo = mock(WeaponFireInfo.class);
        when(mockMMLLRM5FireInfo.getProbabilityToHit()).thenReturn(0.6);
        when(mockMMLLRM5FireInfo.getExpectedDamage()).thenReturn(0.6*5);
        when(mockMMLSRM5FireInfo.getProbabilityToHit()).thenReturn(0.0);
        when(mockMMLSRM5FireInfo.getExpectedDamage()).thenReturn(0.0*10);

        // Firing LRM5 ammo
        doReturn(mockMMLLRM5FireInfo).when(testFireControl).buildWeaponFireInfo(any(Entity.class),
                any(EntityState.class), any(Targetable.class), any(EntityState.class), eq(mockWeaponMML5), eq(mockAmmoLRM5),
                any(Game.class), anyBoolean());
        doReturn(mockMMLLRM5FireInfo).when(testFireControl).buildWeaponFireInfo(any(Entity.class),
                any(MovePath.class), any(Targetable.class), any(EntityState.class), eq(mockWeaponMML5), eq(mockAmmoLRM5),
                any(Game.class), anyBoolean(), anyBoolean());
        doReturn(mockMMLLRM5FireInfo).when(testFireControl).buildWeaponFireInfo(any(Entity.class),
                any(Targetable.class), eq(mockWeaponMML5), eq(mockAmmoLRM5), any(Game.class), anyBoolean());

        // Firing SRM5 ammo
        doReturn(mockMMLSRM5FireInfo).when(testFireControl).buildWeaponFireInfo(any(Entity.class),
                any(EntityState.class), any(Targetable.class), any(EntityState.class), eq(mockWeaponMML5), eq(mockAmmoSRM5),
                any(Game.class), anyBoolean());
        doReturn(mockMMLSRM5FireInfo).when(testFireControl).buildWeaponFireInfo(any(Entity.class),
                any(MovePath.class), any(Targetable.class), any(EntityState.class), eq(mockWeaponMML5), eq(mockAmmoSRM5),
                any(Game.class), anyBoolean(), anyBoolean());
        doReturn(mockMMLSRM5FireInfo).when(testFireControl).buildWeaponFireInfo(any(Entity.class),
                any(Targetable.class), eq(mockWeaponMML5), eq(mockAmmoSRM5), any(Game.class), anyBoolean());


        // Mock the getAmmo(Mounted) call return value
        ArrayList<Mounted> mockAmmoList = new ArrayList<Mounted>();
        mockAmmoList.add(mockAmmoLRM5);
        mockAmmoList.add(mockAmmoSRM5);
        when(mockShooter.getAmmo(any(Mounted.class))).thenReturn(mockAmmoList);

        testToHitThreshold = new HashMap<>();
        for (final Mounted weapon : mockShooter.getWeaponList()) {
            testToHitThreshold.put(weapon, 0.0);
        }
    }

    @Test
    public void testGetHardTargetAmmo() {
        // Test an ammo list with only 1 bin of standard ammo.
        List<Mounted> testAmmoList = new ArrayList<>(1);
        testAmmoList.add(mockAmmoAC5Std);
        final FireControl testFireControl = new FireControl(mockPrincess);
        assertEquals(mockAmmoAC5Std, testFireControl.getHardTargetAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Test an ammo list with only 1 bin of flak ammo.
        testAmmoList = new ArrayList<>(1);
        testAmmoList.add(mockAmmoAC5Flak);
        assertNull(testFireControl.getHardTargetAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Test an ammo list with 1 each of standard and flak.
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoAC5Flak);
        testAmmoList.add(mockAmmoAC5Std);
        assertEquals(mockAmmoAC5Std, testFireControl.getHardTargetAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Test LBX weaponry.
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoLB10XCluster);
        testAmmoList.add(mockAmmoLB10XSlug);
        assertEquals(mockAmmoLB10XSlug, testFireControl.getHardTargetAmmo(testAmmoList, mockLB10X, 5));

        // Test MMLs
        testAmmoList = new ArrayList<>(3);
        testAmmoList.add(mockAmmoLRM5);
        testAmmoList.add(mockAmmoSRM5);
        testAmmoList.add(mockAmmoInferno5);
        assertEquals(mockAmmoSRM5, testFireControl.getHardTargetAmmo(testAmmoList, mockMML5, 4));
        assertEquals(mockAmmoLRM5, testFireControl.getHardTargetAmmo(testAmmoList, mockMML5, 8));
        assertEquals(mockAmmoLRM5, testFireControl.getHardTargetAmmo(testAmmoList, mockMML5, 10));

        // Test MMLs without LRMs.
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoSRM5);
        testAmmoList.add(mockAmmoInferno5);
        assertEquals(mockAmmoSRM5, testFireControl.getHardTargetAmmo(testAmmoList, mockMML5, 4));
        assertEquals(mockAmmoSRM5, testFireControl.getHardTargetAmmo(testAmmoList, mockMML5, 8));
        assertNull(testFireControl.getHardTargetAmmo(testAmmoList, mockMML5, 10));

        // Test MMLs without SRMs.
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoLRM5);
        testAmmoList.add(mockAmmoInferno5);
        assertEquals(mockAmmoLRM5, testFireControl.getHardTargetAmmo(testAmmoList, mockMML5, 4));
        assertEquals(mockAmmoLRM5, testFireControl.getHardTargetAmmo(testAmmoList, mockMML5, 8));
        assertEquals(mockAmmoLRM5, testFireControl.getHardTargetAmmo(testAmmoList, mockMML5, 10));
    }

    @Test
    public void testGetAntiAirAmmo() {
        // Test an ammo list with only 1 bin.
        List<Mounted> testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoAC5Std);
        final FireControl testFireControl = new FireControl(mockPrincess);
        assertNull(testFireControl.getAntiAirAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Add the flak ammo.
        testAmmoList.add(mockAmmoAC5Flak);
        assertEquals(mockAmmoAC5Flak, testFireControl.getAntiAirAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Test a list with 2 bins of standard and 0 flak ammo.
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoAC5Std);
        testAmmoList.add(mockAmmoAC5Std);
        assertNull(testFireControl.getAntiAirAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Test LBX weaponry.
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoLB10XCluster);
        testAmmoList.add(mockAmmoLB10XSlug);
        assertEquals(mockAmmoLB10XCluster, testFireControl.getAntiAirAmmo(testAmmoList, mockLB10X, 5));
    }

    @Test
    public void testGetClusterAmmo() {
        // Test an ammo list with only 1 bin of cluster ammo.
        List<Mounted> testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoLB10XCluster);
        FireControl testFireControl = new FireControl(mockPrincess);
        assertEquals(mockAmmoLB10XCluster, testFireControl.getClusterAmmo(testAmmoList, mockLB10X, 5));

        // Test an ammo list with only 1 bin of slug ammo.
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoLB10XSlug);
        testFireControl = new FireControl(mockPrincess);
        assertNull(testFireControl.getClusterAmmo(testAmmoList, mockLB10X, 5));

        // Test with both loaded
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoLB10XCluster);
        testAmmoList.add(mockAmmoLB10XSlug);
        assertEquals(mockAmmoLB10XCluster, testFireControl.getClusterAmmo(testAmmoList, mockLB10X, 5));
    }

    @Test
    public void testGetHeatAmmo() {
        // Test an ammo list with only 1 bin of incendiary ammo.
        List<Mounted> testAmmoList = new ArrayList<>(1);
        testAmmoList.add(mockAmmoAc5Incendiary);
        final FireControl testFireControl = new FireControl(mockPrincess);
        assertEquals(mockAmmoAc5Incendiary, testFireControl.getIncendiaryAmmo(testAmmoList, mockWeaponTypeAC5,
                                                                                     5));

        // Test an ammo list with only 1 bin of standard ammo.
        testAmmoList = new ArrayList<>(1);
        testAmmoList.add(mockAmmoAC5Std);
        assertNull(testFireControl.getIncendiaryAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Test a list with multiple types of ammo.
        testAmmoList = new ArrayList<>(3);
        testAmmoList.add(mockAmmoAC5Std);
        testAmmoList.add(mockAmmoAc5Incendiary);
        testAmmoList.add(mockAmmoAC5Flak);
        assertEquals(mockAmmoAc5Incendiary, testFireControl.getIncendiaryAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Test LBX
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoLB10XCluster);
        testAmmoList.add(mockAmmoLB10XSlug);
        assertNull(testFireControl.getIncendiaryAmmo(testAmmoList, mockLB10X, 5));

        // Test MMLs
        testAmmoList = new ArrayList<>(3);
        testAmmoList.add(mockAmmoLRM5);
        testAmmoList.add(mockAmmoSRM5);
        testAmmoList.add(mockAmmoInferno5);
        assertEquals(mockAmmoInferno5, testFireControl.getIncendiaryAmmo(testAmmoList, mockMML5, 4));
        assertEquals(mockAmmoInferno5, testFireControl.getIncendiaryAmmo(testAmmoList, mockMML5, 8));
        assertNull(testFireControl.getIncendiaryAmmo(testAmmoList, mockMML5, 10));
    }

    @Test
    public void testGetAntiInfantryAmmo() {
        // Test an ammo list with only 1 bin of flechette ammo.
        List<Mounted> testAmmoList = new ArrayList<>(1);
        testAmmoList.add(mockAmmoAc5Flechette);
        final FireControl testFireControl = new FireControl(mockPrincess);
        assertEquals(mockAmmoAc5Flechette, testFireControl.getAntiInfantryAmmo(testAmmoList,
                mockWeaponTypeAC5, 5));

        // Test an ammo list with only 1 bin of standard ammo.
        testAmmoList = new ArrayList<>(1);
        testAmmoList.add(mockAmmoAC5Std);
        assertNull(testFireControl.getAntiInfantryAmmo(testAmmoList, mockWeaponTypeAC5, 5));

        // Test a list with multiple types of ammo.
        testAmmoList = new ArrayList<>(3);
        testAmmoList.add(mockAmmoAC5Std);
        testAmmoList.add(mockAmmoAC5Flak);
        testAmmoList.add(mockAmmoAc5Flechette);
        assertEquals(mockAmmoAc5Flechette, testFireControl.getAntiInfantryAmmo(testAmmoList,
                mockWeaponTypeAC5, 5));

        // Test LBX
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoLB10XCluster);
        testAmmoList.add(mockAmmoLB10XSlug);
        assertEquals(mockAmmoLB10XCluster, testFireControl.getAntiInfantryAmmo(testAmmoList, mockLB10X, 5));

        // Test MMLs
        testAmmoList = new ArrayList<>(4);
        testAmmoList.add(mockAmmoLRM5);
        testAmmoList.add(mockAmmoSRM5);
        testAmmoList.add(mockAmmoInferno5);
        testAmmoList.add(mockAmmoLrm5Frag);
        assertEquals(mockAmmoInferno5, testFireControl.getAntiInfantryAmmo(testAmmoList, mockMML5, 4));
        assertEquals(mockAmmoLrm5Frag, testFireControl.getAntiInfantryAmmo(testAmmoList, mockMML5, 8));
        assertEquals(mockAmmoLrm5Frag, testFireControl.getAntiInfantryAmmo(testAmmoList, mockMML5, 10));
    }

    @Test
    public void testGetAntiVeeAmmo() {
        // Test an ammo list with only 1 bin of standard ammo.
        List<Mounted> testAmmoList = new ArrayList<>(1);
        testAmmoList.add(mockAmmoAC5Std);
        FireControl testFireControl = new FireControl(mockPrincess);
        assertNull(testFireControl.getAntiVeeAmmo(testAmmoList, mockWeaponTypeAC5, 5, false));

        // Test an ammo list with only 1 bin of incendiary ammo.
        testAmmoList = new ArrayList<>(1);
        testAmmoList.add(mockAmmoAc5Incendiary);
        testFireControl = new FireControl(mockPrincess);
        assertNull(testFireControl.getAntiVeeAmmo(testAmmoList, mockWeaponTypeAC5, 5, false));

        // Test a list with multiple types of ammo.
        testAmmoList = new ArrayList<>(3);
        testAmmoList.add(mockAmmoAC5Std);
        testAmmoList.add(mockAmmoAc5Incendiary);
        testAmmoList.add(mockAmmoAC5Flak);
        assertNull(testFireControl.getAntiVeeAmmo(testAmmoList, mockWeaponTypeAC5, 5, true));
        assertNull(testFireControl.getAntiVeeAmmo(testAmmoList, mockWeaponTypeAC5, 5, false));

        // Test LBX
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoLB10XCluster);
        testAmmoList.add(mockAmmoLB10XSlug);
        assertEquals(mockAmmoLB10XCluster, testFireControl.getAntiVeeAmmo(testAmmoList, mockLB10X, 5, false));

        // Test MMLs
        testAmmoList = new ArrayList<>(4);
        testAmmoList.add(mockAmmoLRM5);
        testAmmoList.add(mockAmmoSRM5);
        testAmmoList.add(mockAmmoInferno5);
        testAmmoList.add(mockAmmoLrm5Frag);
        assertEquals(mockAmmoInferno5, testFireControl.getAntiVeeAmmo(testAmmoList, mockMML5, 4, false));
        assertEquals(mockAmmoInferno5, testFireControl.getAntiVeeAmmo(testAmmoList, mockMML5, 8, false));
        assertNull(testFireControl.getAntiVeeAmmo(testAmmoList, mockMML5, 4, true));
        assertNull(testFireControl.getAntiVeeAmmo(testAmmoList, mockMML5, 8, true));
        assertNull(testFireControl.getAntiVeeAmmo(testAmmoList, mockMML5, 10, false));
    }

    @Test
    public void testGetAtmAmmo() {
        // Test a list with just HE ammo.
        List<Mounted> testAmmoList = new ArrayList<>(1);
        testAmmoList.add(mockAmmoAtm5He);
        final FireControl testFireControl = new FireControl(mockPrincess);
        assertEquals(mockAmmoAtm5He, testFireControl.getAtmAmmo(testAmmoList, 5, mockTargetState, false));
        assertNull(testFireControl.getAtmAmmo(testAmmoList, 15, mockTargetState, false));

        // Test a list with just Standard ammo.
        testAmmoList = new ArrayList<>(1);
        testAmmoList.add(mockAmmoAtm5St);
        assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 5, mockTargetState, false));
        assertNull(testFireControl.getAtmAmmo(testAmmoList, 20, mockTargetState, false));

        // Test a list with just ER ammo.
        testAmmoList = new ArrayList<>(1);
        testAmmoList.add(mockAmmoAtm5Er);
        assertEquals(mockAmmoAtm5Er, testFireControl.getAtmAmmo(testAmmoList, 5, mockTargetState, false));

        // Test a list with all 3 ammo types
        testAmmoList = new ArrayList<>(3);
        testAmmoList.add(mockAmmoAtm5He);
        testAmmoList.add(mockAmmoAtm5Er);
        testAmmoList.add(mockAmmoAtm5St);
        assertEquals(mockAmmoAtm5Er, testFireControl.getAtmAmmo(testAmmoList, 20, mockTargetState, false));
        assertEquals(mockAmmoAtm5Er, testFireControl.getAtmAmmo(testAmmoList, 12, mockTargetState, false));
        assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 8, mockTargetState, false));
        assertEquals(mockAmmoAtm5He, testFireControl.getAtmAmmo(testAmmoList, 6, mockTargetState, false));
        assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 5, mockTargetState, false));
        assertEquals(mockAmmoAtm5He, testFireControl.getAtmAmmo(testAmmoList, 3, mockTargetState, false));

        // Test a list with just HE and Standard ammo types.
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoAtm5He);
        testAmmoList.add(mockAmmoAtm5St);
        assertNull(testFireControl.getAtmAmmo(testAmmoList, 20, mockTargetState, false));
        assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 12, mockTargetState, false));
        assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 8, mockTargetState, false));
        assertEquals(mockAmmoAtm5He, testFireControl.getAtmAmmo(testAmmoList, 6, mockTargetState, false));
        assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 5, mockTargetState, false));
        assertEquals(mockAmmoAtm5He, testFireControl.getAtmAmmo(testAmmoList, 3, mockTargetState, false));

        // Test a list with just HE and ER ammo types.
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoAtm5He);
        testAmmoList.add(mockAmmoAtm5Er);
        assertEquals(mockAmmoAtm5Er, testFireControl.getAtmAmmo(testAmmoList, 20, mockTargetState, false));
        assertEquals(mockAmmoAtm5Er, testFireControl.getAtmAmmo(testAmmoList, 12, mockTargetState, false));
        assertEquals(mockAmmoAtm5Er, testFireControl.getAtmAmmo(testAmmoList, 8, mockTargetState, false));
        assertEquals(mockAmmoAtm5He, testFireControl.getAtmAmmo(testAmmoList, 6, mockTargetState, false));
        assertEquals(mockAmmoAtm5He, testFireControl.getAtmAmmo(testAmmoList, 5, mockTargetState, false));
        assertEquals(mockAmmoAtm5He, testFireControl.getAtmAmmo(testAmmoList, 3, mockTargetState, false));

        // Test a list with just Standard and ER ammo types.
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoAtm5St);
        testAmmoList.add(mockAmmoAtm5Er);
        assertEquals(mockAmmoAtm5Er, testFireControl.getAtmAmmo(testAmmoList, 20, mockTargetState, false));
        assertEquals(mockAmmoAtm5Er, testFireControl.getAtmAmmo(testAmmoList, 12, mockTargetState, false));
        assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 8, mockTargetState, false));
        assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 6, mockTargetState, false));
        assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 5, mockTargetState, false));
        assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 3, mockTargetState, false));

        // Test targets that should be hit with infernos.
        when(mockTargetState.isBuilding()).thenReturn(true);
        testAmmoList.add(mockAmmoAtm5He);
        testAmmoList.add(mockAmmoAtm5Er);
        testAmmoList.add(mockAmmoAtm5St);
        testAmmoList.add(mockAmmoAtm5Inferno);
        assertEquals(mockAmmoAtm5Inferno, testFireControl.getAtmAmmo(testAmmoList, 8, mockTargetState, false));
        assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 8, mockTargetState, true));
        when(mockTargetState.isBuilding()).thenReturn(false);
        when(mockTargetState.getHeat()).thenReturn(9);
        assertEquals(mockAmmoAtm5Inferno, testFireControl.getAtmAmmo(testAmmoList, 8, mockTargetState, false));
        assertEquals(mockAmmoAtm5St, testFireControl.getAtmAmmo(testAmmoList, 8, mockTargetState, true));
        when(mockTargetState.getHeat()).thenReturn(0);
    }

    @Test
    public void testGetGeneralMmlAmmo() {
        // Test a list with just SRM ammo.
        List<Mounted> testAmmoList = new ArrayList<>(1);
        testAmmoList.add(mockAmmoSRM5);
        final FireControl testFireControl = new FireControl(mockPrincess);
        assertEquals(mockAmmoSRM5, testFireControl.getGeneralMmlAmmo(testAmmoList, 6));
        assertNull(testFireControl.getGeneralMmlAmmo(testAmmoList, 10));

        // Test a list with just LRM ammo.
        testAmmoList = new ArrayList<>(1);
        testAmmoList.add(mockAmmoLRM5);
        assertEquals(mockAmmoLRM5, testFireControl.getGeneralMmlAmmo(testAmmoList, 10));
        assertEquals(mockAmmoLRM5, testFireControl.getGeneralMmlAmmo(testAmmoList, 3));

        // Test a list with both types of ammo.
        testAmmoList = new ArrayList<>(2);
        testAmmoList.add(mockAmmoLRM5);
        testAmmoList.add(mockAmmoSRM5);
        assertEquals(mockAmmoLRM5, testFireControl.getGeneralMmlAmmo(testAmmoList, 10));
        assertEquals(mockAmmoLRM5, testFireControl.getGeneralMmlAmmo(testAmmoList, 6));
        assertEquals(mockAmmoSRM5, testFireControl.getGeneralMmlAmmo(testAmmoList, 4));
    }

    @Test
    public void testGetPreferredAmmo() {
        final Entity mockShooter = mock(BipedMech.class);
        Targetable mockTarget = mock(BipedMech.class);
        when(((Entity) mockTarget).getArmorType(anyInt())).thenReturn(EquipmentType.T_ARMOR_STANDARD);
        final FireControl testFireControl = new FireControl(mockPrincess);

        final Crew mockCrew = mock(Crew.class);
        when(mockShooter.getCrew()).thenReturn(mockCrew);
        when(((Entity) mockTarget).getCrew()).thenReturn(mockCrew);

        final PilotOptions mockOptions = mock(PilotOptions.class);
        when(mockCrew.getOptions()).thenReturn(mockOptions);
        when(mockOptions.booleanOption(anyString())).thenReturn(false);

        final ArrayList<Mounted> testAmmoList = new ArrayList<>(5);
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
        testAmmoList.add(mockAmmoInferno5);
        testAmmoList.add(mockAmmoLRM5);
        when(mockShooter.getAmmo()).thenReturn(testAmmoList);
        when(mockShooter.getPosition()).thenReturn(new Coords(10, 10));

        // Test ATMs
        when(mockTarget.getPosition()).thenReturn(new Coords(10, 30));
        when(mockAtm5Weapon.getLinked()).thenReturn(mockAmmoAtm5Er);
        assertEquals(mockAmmoAtm5Er, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockAtm5Weapon));
        when(mockTarget.getPosition()).thenReturn(new Coords(10, 22));
        assertEquals(mockAmmoAtm5Er, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockAtm5Weapon));
        when(mockTarget.getPosition()).thenReturn(new Coords(10, 18));
        when(mockAtm5Weapon.getLinked()).thenReturn(mockAmmoAtm5St);
        assertEquals(mockAmmoAtm5St, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockAtm5Weapon));
        when(mockTarget.getPosition()).thenReturn(new Coords(10, 16));
        when(mockAtm5Weapon.getLinked()).thenReturn(mockAmmoAtm5He);
        assertEquals(mockAmmoAtm5He, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockAtm5Weapon));
        when(mockTarget.getPosition()).thenReturn(new Coords(10, 15));
        when(mockAtm5Weapon.getLinked()).thenReturn(mockAmmoAtm5St);
        assertEquals(mockAmmoAtm5St, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockAtm5Weapon));
        when(mockTarget.getPosition()).thenReturn(new Coords(10, 13));
        when(mockAtm5Weapon.getLinked()).thenReturn(mockAmmoAtm5He);
        assertEquals(mockAmmoAtm5He, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockAtm5Weapon));

        // Test shooting an AC5 at a building.
        mockTarget = mock(BuildingTarget.class);
        when(mockTarget.getPosition()).thenReturn(new Coords(10, 15));
        when(mockWeaponAC5.getLinked()).thenReturn(mockAmmoAc5Incendiary);
        assertEquals(mockAmmoAc5Incendiary, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockWeaponAC5));

        // Test shooting an LBX at an airborne target.
        mockTarget = mock(VTOL.class);
        when(((Entity) mockTarget).getArmorType(anyInt())).thenReturn(EquipmentType.T_ARMOR_STANDARD);
        when(mockTarget.getPosition()).thenReturn(new Coords(10, 15));
        when(mockTarget.isAirborne()).thenReturn(true);
        when(mockWeaponLB10X.getLinked()).thenReturn(mockAmmoLB10XCluster);
        assertEquals(mockAmmoLB10XCluster, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockWeaponLB10X));

        // Test shooting an LBX at a tank.
        mockTarget = mock(Tank.class);
        when(((Entity) mockTarget).getArmorType(anyInt())).thenReturn(EquipmentType.T_ARMOR_STANDARD);
        when(mockTarget.getPosition()).thenReturn(new Coords(10, 15));
        when(mockWeaponLB10X.getLinked()).thenReturn(mockAmmoLB10XCluster);
        assertEquals(mockAmmoLB10XCluster, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockWeaponLB10X));

        // Test shooting an AC at infantry.
        mockTarget = mock(Infantry.class);
        when(((Entity) mockTarget).getArmorType(anyInt())).thenReturn(EquipmentType.T_ARMOR_STANDARD);
        when(mockTarget.getPosition()).thenReturn(new Coords(10, 15));
        when(mockWeaponAC5.getLinked()).thenReturn(mockAmmoAc5Flechette);
        assertTrue(
                mockAmmoAc5Flechette.equals(testFireControl.getPreferredAmmo(mockShooter, mockTarget,
                        mockWeaponAC5))
                || mockAmmoAc5Incendiary.equals(testFireControl.getPreferredAmmo(mockShooter, mockTarget,
                        mockWeaponAC5)));

        // Test a LBX at a heavily damaged target.
        mockTarget = mock(BipedMech.class);
        when(((Entity) mockTarget).getArmorType(anyInt())).thenReturn(EquipmentType.T_ARMOR_STANDARD);
        when(mockTarget.getPosition()).thenReturn(new Coords(10, 15));
        when(((Entity) mockTarget).getDamageLevel()).thenReturn(Entity.DMG_HEAVY);
        when(mockWeaponLB10X.getLinked()).thenReturn(mockAmmoLB10XCluster);
        assertEquals(mockAmmoLB10XCluster, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockWeaponLB10X));

        // Test a hot target.
        when(((Entity) mockTarget).getDamageLevel()).thenReturn(Entity.DMG_LIGHT);
        when(((Entity) mockTarget).getHeat()).thenReturn(12);
        when(mockWeaponMML5.getLinked()).thenReturn(mockAmmoInferno5);
        assertEquals(mockAmmoInferno5, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockWeaponMML5));
        when(((Entity) mockTarget).getArmorType(anyInt())).thenReturn(EquipmentType.T_ARMOR_HEAT_DISSIPATING);
        when(mockWeaponMML5.getLinked()).thenReturn(mockAmmoSRM5);
        assertEquals(mockAmmoSRM5, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockWeaponMML5));
        when(((Entity) mockTarget).getArmorType(anyInt())).thenReturn(EquipmentType.T_ARMOR_STANDARD);

        // Test a normal target.
        when(((Entity) mockTarget).getHeat()).thenReturn(4);
        when(mockWeaponAC5.getLinked()).thenReturn(mockAmmoAC5Std);
        assertEquals(mockAmmoAC5Std, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockWeaponAC5));
        when(mockWeaponMML5.getLinked()).thenReturn(mockAmmoSRM5);
        assertEquals(mockAmmoSRM5, testFireControl.getPreferredAmmo(mockShooter, mockTarget, mockWeaponMML5));
    }

    @Test
    public void testGuessToHitModifierHelperForAnyAttack() {
        // Test the most vanilla case we can.
        when(mockShooterState.isProne()).thenReturn(false);
        when(mockShooter.hasQuirk(eq(OptionsConstants.QUIRK_POS_ANTI_AIR))).thenReturn(false);
        when(mockTargetState.isImmobile()).thenReturn(false);
        when(mockTargetState.getMovementType()).thenReturn(EntityMovementType.MOVE_NONE);
        when(mockTargetState.getPosition()).thenReturn(new Coords(10, 0));
        when(mockTargetState.isProne()).thenReturn(false);
        when(mockTarget.isAirborne()).thenReturn(false);
        when(mockTarget.isAirborneVTOLorWIGE()).thenReturn(false);
        when(mockGameOptions.booleanOption(eq(OptionsConstants.ADVGRNDMOV_TACOPS_STANDING_STILL))).thenReturn(false);
        when(mockHex.terrainLevel(Terrains.WOODS)).thenReturn(Terrain.LEVEL_NONE);
        when(mockHex.terrainLevel(Terrains.JUNGLE)).thenReturn(Terrain.LEVEL_NONE);
        when(mockPrincess.getMaxWeaponRange(any(Entity.class), anyBoolean())).thenReturn(21);
        ToHitData expected = new ToHitData();
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(
                mockShooter, mockShooterState, mockTarget, mockTargetState, 10, mockGame));

        // Test ground units firing on airborne aeros.
        final ConvFighter mockFighter = mock(ConvFighter.class);
        when(mockFighter.isNOE()).thenReturn(true);
        final EntityState mockFighterState = mock(EntityState.class);
        when(mockFighterState.isAirborneAero()).thenReturn(true);
        when(mockFighterState.isImmobile()).thenReturn(false);
        when(mockFighterState.getMovementType()).thenReturn(EntityMovementType.MOVE_SAFE_THRUST);
        when(mockFighterState.getPosition()).thenReturn(new Coords(10, 0));
        when(mockFighterState.isProne()).thenReturn(false);
        doReturn(new Coords(0, 2)).when(testFireControl).getNearestPointInFlightPath(
                any(Coords.class), any(Aero.class));
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_TAR_AERO_NOE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(
                mockShooter, mockShooterState, mockFighter, mockFighterState, 10, mockGame));
        doReturn(new Coords(0, 1)).when(testFireControl).getNearestPointInFlightPath(
                any(Coords.class), any(Aero.class));
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_TAR_AERO_NOE_ADJ);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(
                mockShooter, mockShooterState, mockFighter, mockFighterState, 10, mockGame));

        // Test industrial mechs.
        when(((Mech) mockShooter).getCockpitType()).thenReturn(Mech.COCKPIT_INDUSTRIAL);
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_INDUSTRIAL);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(
                mockShooter, mockShooterState, mockTarget, mockTargetState, 10, mockGame));
        when(((Mech) mockShooter).getCockpitType()).thenReturn(Mech.COCKPIT_PRIMITIVE_INDUSTRIAL);
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_PRIMATIVE_INDUSTRIAL);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(
                mockShooter, mockShooterState, mockTarget, mockTargetState, 10, mockGame));
        when(((Mech) mockShooter).getCockpitType()).thenReturn(Mech.COCKPIT_STANDARD);

        // Test attacking a superheavy mech.
        when(((Mech) mockTarget).getCockpitType()).thenReturn(Mech.COCKPIT_SUPERHEAVY);
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_TAR_SUPER);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(
                mockShooter, mockShooterState, mockTarget, mockTargetState, 10, mockGame));
        when(((Mech) mockTarget).getCockpitType()).thenReturn(Mech.COCKPIT_STANDARD);

        // Test attacking a grounded dropship.
        final Dropship mockDropship = mock(Dropship.class);
        when(mockDropship.isAirborne()).thenReturn(false);
        when(mockDropship.isAirborneVTOLorWIGE()).thenReturn(false);
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_TAR_GROUND_DS);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(
                mockShooter, mockShooterState, mockDropship, mockTargetState, 10, mockGame));

        // Test the shooter having a null position.
        when(mockShooterState.getPosition()).thenReturn(null);
        expected = new ToHitData(FireControl.TH_NULL_POSITION);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(
                mockShooter, mockShooterState, mockTarget, mockTargetState, 10, mockGame));
        when(mockShooterState.getPosition()).thenReturn(new Coords(0, 0));

        // Test the target having a null position.
        when(mockTargetState.getPosition()).thenReturn(null);
        expected = new ToHitData(FireControl.TH_NULL_POSITION);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(
                mockShooter, mockShooterState, mockTarget, mockTargetState, 10, mockGame));
        when(mockTargetState.getPosition()).thenReturn(new Coords(10, 0));

        // Make the shooter prone.
        when(mockShooterState.isProne()).thenReturn(true);
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_ATT_PRONE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        when(mockShooterState.isProne()).thenReturn(false);

        // Make the target immobile.
        when(mockTargetState.isImmobile()).thenReturn(true);
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_TAR_IMMOBILE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        when(mockTargetState.isImmobile()).thenReturn(false);

        // Have the target fall prone adjacent.
        when(mockTargetState.isProne()).thenReturn(true);
        when(mockTargetState.getPosition()).thenReturn(new Coords(0, 1));
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_TAR_PRONE_ADJ);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             1,
                                                                                             mockGame));
        when(mockTargetState.getPosition()).thenReturn(new Coords(10, 0)); // Move the target away.
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_TAR_PRONE_RANGE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        when(mockTargetState.getMovementType()).thenReturn(EntityMovementType.MOVE_SKID); // Have the target
        // skid.
        expected.addModifier(FireControl.TH_TAR_SKID);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        when(mockTargetState.isProne()).thenReturn(false);
        when(mockTargetState.getMovementType()).thenReturn(EntityMovementType.MOVE_NONE);

        // Turn on Tac-Ops Standing Still rules.
        when(mockGameOptions.booleanOption(eq(OptionsConstants.ADVGRNDMOV_TACOPS_STANDING_STILL)))
               .thenReturn(true);
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_TAR_NO_MOVE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        when(mockTargetState.getMovementType()).thenReturn(EntityMovementType.MOVE_WALK); // Walking target.
        expected = new ToHitData();
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        when(mockGameOptions.booleanOption(eq(OptionsConstants.ADVGRNDMOV_TACOPS_STANDING_STILL)))
               .thenReturn(false);
        when(mockTargetState.getMovementType()).thenReturn(EntityMovementType.MOVE_NONE);

        // Have the target sprint.
        when(mockTargetState.getMovementType()).thenReturn(EntityMovementType.MOVE_SPRINT);
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_TAR_SPRINT);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        when(mockTargetState.getMovementType()).thenReturn(EntityMovementType.MOVE_NONE);

        // Stand the target in light woods.
        when(mockHex.terrainLevel(Terrains.WOODS)).thenReturn(1);
        expected = new ToHitData();
        expected.addModifier(1, FireControl.TH_WOODS);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        when(mockHex.terrainLevel(Terrains.WOODS)).thenReturn(Terrain.LEVEL_NONE);

        // Stand the target in heavy woods.
        when(mockHex.terrainLevel(Terrains.WOODS)).thenReturn(2);
        expected = new ToHitData();
        expected.addModifier(2, FireControl.TH_WOODS);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        when(mockHex.terrainLevel(Terrains.WOODS)).thenReturn(Terrain.LEVEL_NONE);

        // Stand the target in super heavy woods.
        when(mockHex.terrainLevel(Terrains.WOODS)).thenReturn(3);
        expected = new ToHitData();
        expected.addModifier(3, FireControl.TH_WOODS);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        when(mockHex.terrainLevel(Terrains.WOODS)).thenReturn(Terrain.LEVEL_NONE);

        // Stand the target in jungle.
        when(mockHex.terrainLevel(Terrains.JUNGLE)).thenReturn(2);
        expected = new ToHitData();
        expected.addModifier(2, FireControl.TH_WOODS);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        when(mockHex.terrainLevel(Terrains.JUNGLE)).thenReturn(Terrain.LEVEL_NONE);

        // Give the shooter the anti-air quirk but fire on a ground target.
        when(mockShooter.hasQuirk(eq(OptionsConstants.QUIRK_POS_ANTI_AIR))).thenReturn(true);
        expected = new ToHitData();
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        when(mockShooter.hasQuirk(eq(OptionsConstants.QUIRK_POS_ANTI_AIR))).thenReturn(false);

        // Give the shooter the anti-air quirk, and fire on an airborne target.
        when(mockShooter.hasQuirk(eq(OptionsConstants.QUIRK_POS_ANTI_AIR))).thenReturn(true);
        mockTarget = mock(ConvFighter.class);
        when(mockTarget.isAirborne()).thenReturn(true);
        when(mockTarget.isAirborneVTOLorWIGE()).thenReturn(false);
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_ANTI_AIR);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        when(mockShooter.hasQuirk(eq(OptionsConstants.QUIRK_POS_ANTI_AIR))).thenReturn(false);
        mockTarget = mock(BipedMech.class);
        when(mockTarget.isAirborne()).thenReturn(false);
        when(mockTarget.isAirborneVTOLorWIGE()).thenReturn(false);

        // Firing at Battle Armor
        mockTarget = mock(BattleArmor.class);
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_TAR_BA);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        mockTarget = mock(BipedMech.class);

        // Firing at an ejected mechwarrior.
        mockTarget = mock(MechWarrior.class);
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_TAR_MW);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        mockTarget = mock(BipedMech.class);

        // Firing at infantry
        mockTarget = mock(Infantry.class);
        expected = new ToHitData();
        expected.addModifier(FireControl.TH_TAR_INF);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        mockTarget = mock(BipedMech.class);

        // Target is out of range.
        when(mockPrincess.getMaxWeaponRange(any(Entity.class), anyBoolean())).thenReturn(5);
        expected = new ToHitData(FireControl.TH_RNG_TOO_FAR);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierHelperForAnyAttack(mockShooter,
                                                                                             mockShooterState,
                                                                                             mockTarget,
                                                                                             mockTargetState,
                                                                                             10,
                                                                                             mockGame));
        when(mockShooter.getMaxWeaponRange()).thenReturn(21);

        // Target is in smoke.
        // Light smoke
        when(mockHex.terrainLevel(Terrains.SMOKE)).thenReturn(
                SmokeCloud.SMOKE_LIGHT);
        expected = new ToHitData();
        expected.addModifier(1, FireControl.TH_SMOKE);

        // Heavy Smoke
        when(mockHex.terrainLevel(Terrains.SMOKE)).thenReturn(
                SmokeCloud.SMOKE_HEAVY);
        expected = new ToHitData();
        expected.addModifier(2, FireControl.TH_SMOKE);

        // Light LI smoke
        when(mockHex.terrainLevel(Terrains.SMOKE)).thenReturn(
                SmokeCloud.SMOKE_LI_LIGHT);
        expected = new ToHitData();
        expected.addModifier(1, FireControl.TH_SMOKE);

        // Chaff Smoke
        when(mockHex.terrainLevel(Terrains.SMOKE)).thenReturn(
                SmokeCloud.SMOKE_CHAFF_LIGHT);
        expected = new ToHitData();
        expected.addModifier(1, FireControl.TH_SMOKE);

        when(mockHex.terrainLevel(Terrains.SMOKE)).thenReturn(
                SmokeCloud.SMOKE_NONE);
    }

    private void assertToHitDataEquals(final ToHitData expected,
                                       final Object actual) {
        assertNotNull(actual);
        assertTrue(actual instanceof ToHitData, "actual: " + actual.getClass().getName());
        final ToHitData actualTHD = (ToHitData) actual;
        final StringBuilder failure = new StringBuilder();
        if (expected.getValue() != actualTHD.getValue()) {
            failure.append("\nExpected: ").append(expected.getValue());
            failure.append("\nActual:   ").append(actualTHD.getValue());
        }
        final Set<TargetRollModifier> expectedMods = new HashSet<>(expected.getModifiers());
        final Set<TargetRollModifier> actualMods = new HashSet<>(actualTHD.getModifiers());
        if (!expectedMods.equals(actualMods)) {
            failure.append("\nExpected: ").append(expected.getDesc());
            failure.append("\nActual:   ").append(actualTHD.getDesc());
        }
        if (!StringUtility.isNullOrBlank(failure.toString())) {
            fail(failure.toString());
        }
    }

    @Test
    public void testGuessToHitModifierPhysical() {

        // guessToHitModifierHelperForAnyAttack being tested elsewhere.
        doReturn(new ToHitData())
               .when(testFireControl)
               .guessToHitModifierHelperForAnyAttack(any(Entity.class), any(EntityState.class),
                                                     any(Targetable.class), any(EntityState.class),
                                                     anyInt(), any(Game.class));
        mockTargetCoords = new Coords(0, 1);
        when(mockTargetState.getPosition()).thenReturn(mockTargetCoords);
        doReturn(true).when(testFireControl).isInArc(any(Coords.class), anyInt(),
                                                             any(Coords.class), anyInt());
        final Hex mockShooterHex = mock(Hex.class);
        when(mockShooterHex.getLevel()).thenReturn(0);
        when(mockBoard.getHex(eq(mockShooterState.getPosition()))).thenReturn(mockShooterHex);
        when(mockShooter.getElevation()).thenReturn(0);
        when(mockShooter.relHeight()).thenReturn(2);
        when(mockShooter.getWeightClass()).thenReturn(EntityWeightClass.WEIGHT_LIGHT);
        when(mockShooter.isLocationBad(Mech.LOC_LARM)).thenReturn(false);
        when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, Mech.LOC_LARM)).thenReturn(true);
        when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, Mech.LOC_LARM)).thenReturn(true);
        when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_LARM)).thenReturn(true);
        when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_HAND, Mech.LOC_LARM)).thenReturn(true);
        when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_UPPER_LEG, Mech.LOC_LLEG)).thenReturn(true);
        when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_LOWER_LEG, Mech.LOC_LLEG)).thenReturn(true);
        when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_FOOT, Mech.LOC_LLEG)).thenReturn(true);

        final Hex mockTargetHex = mock(Hex.class);
        when(mockTargetHex.getLevel()).thenReturn(0);
        when(mockBoard.getHex(eq(mockTargetState.getPosition()))).thenReturn(mockTargetHex);
        when(mockTarget.getElevation()).thenReturn(0);
        when(mockTarget.getHeight()).thenReturn(2);

        // Test a regular kick.
        ToHitData expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting() - 2, FireControl.TH_PHY_BASE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter,
                mockShooterState, mockTarget, mockTargetState, PhysicalAttackType.LEFT_KICK, mockGame));

        // Test a superheavy mech attempting a kick.
        when(((Mech) mockShooter).getCockpitType()).thenReturn(Mech.COCKPIT_SUPERHEAVY);
        expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting() - 2, FireControl.TH_PHY_BASE);
        expected.addModifier(FireControl.TH_PHY_SUPER);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter,
                mockShooterState, mockTarget, mockTargetState, PhysicalAttackType.LEFT_KICK, mockGame));
        when(((Mech) mockShooter).getCockpitType()).thenReturn(Mech.COCKPIT_STANDARD);

        // Test turning on the TacOps Attacker Weight modifier.
        when(mockGameOptions.booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_PHYSICAL_ATTACK_PSR)).thenReturn(true);
        expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting() - 2, FireControl.TH_PHY_BASE);
        expected.addModifier(FireControl.TH_PHY_LIGHT);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter,
                mockShooterState, mockTarget, mockTargetState, PhysicalAttackType.LEFT_KICK, mockGame));
        when(mockShooter.getWeightClass()).thenReturn(EntityWeightClass.WEIGHT_MEDIUM);
        expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting() - 2, FireControl.TH_PHY_BASE);
        expected.addModifier(FireControl.TH_PHY_MEDIUM);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter,
                mockShooterState, mockTarget, mockTargetState, PhysicalAttackType.LEFT_KICK, mockGame));
        when(mockShooter.getWeightClass()).thenReturn(EntityWeightClass.WEIGHT_HEAVY);
        expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting() - 2, FireControl.TH_PHY_BASE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter,
                mockShooterState, mockTarget, mockTargetState, PhysicalAttackType.LEFT_KICK, mockGame));
        when(mockGameOptions.booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_PHYSICAL_ATTACK_PSR)).thenReturn(false);
        when(mockShooter.getWeightClass()).thenReturn(EntityWeightClass.WEIGHT_LIGHT);

        // Test trying to kick infantry in a different hex.
        Entity infantryTarget = mock(Infantry.class);
        when(infantryTarget.getElevation()).thenReturn(0);
        when(infantryTarget.getHeight()).thenReturn(1);
        expected = new ToHitData(FireControl.TH_PHY_K_INF_RNG);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter,
                mockShooterState, infantryTarget, mockTargetState, PhysicalAttackType.LEFT_KICK, mockGame));
        when(mockTargetState.getPosition()).thenReturn(new Coords(0, 0)); // Move them into my hex.
        expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting() - 2, FireControl.TH_PHY_BASE);
        expected.addModifier(FireControl.TH_PHY_K_INF);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter,
                mockShooterState, infantryTarget, mockTargetState, PhysicalAttackType.LEFT_KICK, mockGame));
        when(mockTargetState.getPosition()).thenReturn(new Coords(0, 1));

        // Test kicking with a busted foot.
        when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_FOOT, Mech.LOC_LLEG)).thenReturn(false);
        expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting() - 2, FireControl.TH_PHY_BASE);
        expected.addModifier(FireControl.TH_PHY_K_FOOT);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter,
                mockShooterState, mockTarget, mockTargetState, PhysicalAttackType.LEFT_KICK, mockGame));
        when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_FOOT, Mech.LOC_LLEG)).thenReturn(true);

        // Test kicking with a bad lower leg actuator.
        when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_LOWER_LEG, Mech.LOC_LLEG)).thenReturn(false);
        expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting() - 2, FireControl.TH_PHY_BASE);
        expected.addModifier(FireControl.TH_PHY_K_LOWER_LEG);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter,
                mockShooterState, mockTarget, mockTargetState, PhysicalAttackType.LEFT_KICK, mockGame));
        when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_LOWER_LEG, Mech.LOC_LLEG)).thenReturn(true);

        // Test kicking with a bad upper leg actuator.
        when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_UPPER_LEG, Mech.LOC_LLEG)).thenReturn(false);
        expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting() - 2, FireControl.TH_PHY_BASE);
        expected.addModifier(FireControl.TH_PHY_K_UPPER_LEG);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter,
                mockShooterState, mockTarget, mockTargetState, PhysicalAttackType.LEFT_KICK, mockGame));
        when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_UPPER_LEG, Mech.LOC_RLEG)).thenReturn(true);

        // Test kicking with a busted hip.
        when(mockShooter.hasHipCrit()).thenReturn(true);
        expected = new ToHitData(FireControl.TH_PHY_K_HIP);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter,
                mockShooterState, mockTarget, mockTargetState, PhysicalAttackType.LEFT_KICK, mockGame));
        when(mockShooter.hasHipCrit()).thenReturn(false);

        // Test trying to kick while prone.
        expected = new ToHitData(FireControl.TH_PHY_K_PRONE);
        when(mockShooterState.isProne()).thenReturn(true);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter,
                mockShooterState, mockTarget, mockTargetState, PhysicalAttackType.LEFT_KICK, mockGame));
        when(mockShooterState.isProne()).thenReturn(false);

        // Test a regular punch.
        expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting(), FireControl.TH_PHY_BASE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter,
                mockShooterState, mockTarget, mockTargetState, PhysicalAttackType.LEFT_PUNCH, mockGame));

        // Test having the 'easy to pilot' quirk.
        when(mockShooter.hasQuirk(OptionsConstants.QUIRK_POS_EASY_PILOT)).thenReturn(true);
        expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting(), FireControl.TH_PHY_BASE);
        expected.addModifier(FireControl.TH_PHY_EASY_PILOT);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter,
                mockShooterState, mockTarget, mockTargetState, PhysicalAttackType.LEFT_PUNCH, mockGame));
        when(mockCrew.getPiloting()).thenReturn(2); // Pilot to good to use the quirk.
        expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting(), FireControl.TH_PHY_BASE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter,
                mockShooterState, mockTarget, mockTargetState, PhysicalAttackType.LEFT_PUNCH, mockGame));
        when(mockShooter.hasQuirk(OptionsConstants.QUIRK_POS_EASY_PILOT)).thenReturn(false);
        when(mockCrew.getPiloting()).thenReturn(5);

        /// Test having a damaged/missing hand.
        when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_HAND, Mech.LOC_LARM)).thenReturn(false);
        expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting(), FireControl.TH_PHY_BASE);
        expected.addModifier(FireControl.TH_PHY_P_HAND);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_PUNCH,
                                                                                   mockGame));
        when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_HAND, Mech.LOC_LARM)).thenReturn(true);

        /// Test having a damaged/missing upper arm.
        when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, Mech.LOC_LARM)).thenReturn(false);
        expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting(), FireControl.TH_PHY_BASE);
        expected.addModifier(FireControl.TH_PHY_P_UPPER_ARM);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_PUNCH,
                                                                                   mockGame));
        when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, Mech.LOC_LARM)).thenReturn(true);

        /// Test having a damaged/missing lower arm.
        when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_LARM)).thenReturn(false);
        expected = new ToHitData();
        expected.addModifier(mockCrew.getPiloting(), FireControl.TH_PHY_BASE);
        expected.addModifier(FireControl.TH_PHY_P_LOWER_ARM);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_PUNCH,
                                                                                   mockGame));
        when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_LARM)).thenReturn(true);

        // Test trying to punch with a bad shoulder.
        when(mockShooter.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, Mech.LOC_RARM)).thenReturn(false);
        expected = new ToHitData(FireControl.TH_PHY_P_NO_SHOULDER);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.RIGHT_PUNCH,
                                                                                   mockGame));

        // Test trying to punch with a destroyed arm.
        when(mockShooter.isLocationBad(Mech.LOC_RARM)).thenReturn(true);
        expected = new ToHitData(FireControl.TH_PHY_P_NO_ARM);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.RIGHT_PUNCH,
                                                                                   mockGame));

        // Test trying to punch an infantry target.
        infantryTarget = mock(Infantry.class);
        when(infantryTarget.getElevation()).thenReturn(1);
        when(infantryTarget.getHeight()).thenReturn(1);
        expected = new ToHitData(FireControl.TH_PHY_P_TAR_INF);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   infantryTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_PUNCH,
                                                                                   mockGame));

        // Test trying to punch while prone.
        when(mockShooterState.isProne()).thenReturn(true);
        expected = new ToHitData(FireControl.TH_PHY_P_TAR_PRONE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_PUNCH,
                                                                                   mockGame));

        // Test the target being at the wrong elevation for a punch.
        when(mockShooterHex.getLevel()).thenReturn(1);
        expected = new ToHitData(FireControl.TH_PHY_TOO_MUCH_ELEVATION);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_PUNCH,
                                                                                   mockGame));

        // Test an attacker with the 'no arms' quirk trying to punch.
        when(mockShooter.hasQuirk(OptionsConstants.QUIRK_NEG_NO_ARMS)).thenReturn(true);
        expected = new ToHitData(FireControl.TH_PHY_P_NO_ARMS_QUIRK);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_PUNCH,
                                                                                   mockGame));

        // Test the target not being in the attack arc.
        doReturn(false).when(testFireControl).isInArc(any(Coords.class), anyInt(),
                                                              any(Coords.class), anyInt());
        expected = new ToHitData(FireControl.TH_PHY_NOT_IN_ARC);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_PUNCH,
                                                                                   mockGame));

        // Test the target being more than 1 hex away.
        when(mockTargetState.getPosition()).thenReturn(new Coords(10, 10));
        expected = new ToHitData(FireControl.TH_PHY_TOO_FAR);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockShooter, mockShooterState,
                                                                                   mockTarget, mockTargetState,
                                                                                   PhysicalAttackType.LEFT_PUNCH,
                                                                                   mockGame));

        // Test an attacker that is not a mech.
        final Entity mockVee = mock(Tank.class);
        expected = new ToHitData(FireControl.TH_PHY_NOT_MECH);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierPhysical(mockVee, null, mockTarget,
                                                                                   mockTargetState,
                                                                                   PhysicalAttackType.CHARGE,
                                                                                   mockGame));
    }

    @Test
    public void testGuessToHitModifierForWeapon() {
        when(mockGameOptions.booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE)).thenReturn(false);
        when(mockTarget.hasQuirk(eq(OptionsConstants.QUIRK_POS_LOW_PROFILE))).thenReturn(false);
        when(mockShooterState.getFacing()).thenReturn(1);
        doReturn(true).when(testFireControl).isInArc(any(Coords.class), anyInt(),
                any(Coords.class), anyInt());
        doReturn(new ToHitData())
               .when(testFireControl)
               .guessToHitModifierHelperForAnyAttack(any(Entity.class), any(EntityState.class),
                       any(Targetable.class), any(EntityState.class), anyInt(), any(Game.class));
        final LosEffects spyLosEffects = spy(new LosEffects());
        doReturn(spyLosEffects)
               .when(testFireControl)
               .getLosEffects(any(Game.class), any(Entity.class), any(Targetable.class),
                       any(Coords.class), any(Coords.class), anyBoolean());
        doReturn(new ToHitData()).when(spyLosEffects).losModifiers(eq(mockGame));

        final Hex mockTargetHex = mock(Hex.class);
        when(mockBoard.getHex(eq(mockTargetCoords))).thenReturn(mockTargetHex);
        when(mockTargetHex.containsTerrain(Terrains.WATER)).thenReturn(false); // todo test water

        final int MOCK_WEAPON_ID = 1;
        final Mounted mockWeapon = mock(Mounted.class);
        when(mockWeapon.canFire()).thenReturn(true);
        when(mockWeapon.getLocation()).thenReturn(Mech.LOC_RARM);
        when(mockShooter.getEquipmentNum(eq(mockWeapon))).thenReturn(MOCK_WEAPON_ID);
        when(mockShooter.isSecondaryArcWeapon(MOCK_WEAPON_ID)).thenReturn(false);

        final WeaponType mockWeaponType = mock(WeaponType.class);
        when(mockWeapon.getType()).thenReturn(mockWeaponType);
        when(mockWeaponType.getAmmoType()).thenReturn(AmmoType.T_AC);
        when(mockWeaponType.getRanges(eq(mockWeapon), any(Mounted.class))).thenReturn(new int[] { 3, 6, 12, 18, 24 });
        when(mockWeaponType.getMinimumRange()).thenReturn(3);
        when(mockWeaponType.hasFlag(eq(WeaponType.F_DIRECT_FIRE))).thenReturn(true);

        final Mounted mockAmmo = mock(Mounted.class);
        when(mockWeapon.getLinked()).thenReturn(mockAmmo);
        when(mockAmmo.getUsableShotsLeft()).thenReturn(10);

        // Test the vanilla case.
        ToHitData expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_MEDIUM_RANGE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));

        // Test weapon quirks.
        when(mockWeapon.hasQuirk(eq(OptionsConstants.QUIRK_WEAP_POS_ACCURATE))).thenReturn(true);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_MEDIUM_RANGE);
        expected.addModifier(FireControl.TH_ACCURATE_WEAP);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        when(mockWeapon.hasQuirk(eq(OptionsConstants.QUIRK_WEAP_POS_ACCURATE))).thenReturn(false);
        when(mockWeapon.hasQuirk(eq(OptionsConstants.QUIRK_WEAP_NEG_INACCURATE))).thenReturn(true);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_MEDIUM_RANGE);
        expected.addModifier(FireControl.TH_INACCURATE_WEAP);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        when(mockWeapon.hasQuirk(eq(OptionsConstants.QUIRK_WEAP_NEG_INACCURATE))).thenReturn(false);

        // Test long range shooter quirks.
        when(mockTargetState.getPosition()).thenReturn(new Coords(0, 15));
        when(mockShooter.hasQuirk(eq(OptionsConstants.QUIRK_POS_IMP_TARG_L))).thenReturn(true);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_LONG_RANGE);
        expected.addModifier(FireControl.TH_IMP_TARG_LONG);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        when(mockShooter.hasQuirk(eq(OptionsConstants.QUIRK_POS_IMP_TARG_L))).thenReturn(false);
        when(mockShooter.hasQuirk(eq(OptionsConstants.QUIRK_POS_VAR_RNG_TARG_S))).thenReturn(true);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_LONG_RANGE);
        expected.addModifier(FireControl.TH_VAR_RNG_TARG_SHORT_AT_LONG);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        when(mockShooter.hasQuirk(eq(OptionsConstants.QUIRK_POS_VAR_RNG_TARG_S))).thenReturn(false);
        when(mockShooter.hasQuirk(eq(OptionsConstants.QUIRK_POS_VAR_RNG_TARG_L))).thenReturn(true);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_LONG_RANGE);
        expected.addModifier(FireControl.TH_VAR_RNG_TARG_LONG_AT_LONG);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        when(mockShooter.hasQuirk(eq(OptionsConstants.QUIRK_POS_VAR_RNG_TARG_L))).thenReturn(false);
        when(mockShooter.hasQuirk(eq(OptionsConstants.QUIRK_NEG_POOR_TARG_L))).thenReturn(true);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_LONG_RANGE);
        expected.addModifier(FireControl.TH_POOR_TARG_LONG);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        when(mockShooter.hasQuirk(eq(OptionsConstants.QUIRK_NEG_POOR_TARG_L))).thenReturn(false);
        when(mockTargetState.getPosition()).thenReturn(mockTargetCoords);

        // Test medium range shooter quirks.
        when(mockShooter.hasQuirk(eq(OptionsConstants.QUIRK_POS_IMP_TARG_M))).thenReturn(true);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_MEDIUM_RANGE);
        expected.addModifier(FireControl.TH_IMP_TARG_MEDIUM);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        when(mockShooter.hasQuirk(eq(OptionsConstants.QUIRK_POS_IMP_TARG_M))).thenReturn(false);
        when(mockShooter.hasQuirk(eq(OptionsConstants.QUIRK_NEG_POOR_TARG_M))).thenReturn(true);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_MEDIUM_RANGE);
        expected.addModifier(FireControl.TH_POOR_TARG_MEDIUM);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        when(mockShooter.hasQuirk(eq(OptionsConstants.QUIRK_NEG_POOR_TARG_M))).thenReturn(false);

        // Test short range shooter quirks.
        when(mockTargetState.getPosition()).thenReturn(new Coords(0, 5));
        when(mockShooter.hasQuirk(eq(OptionsConstants.QUIRK_POS_IMP_TARG_S))).thenReturn(true);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_SHORT_RANGE);
        expected.addModifier(FireControl.TH_IMP_TARG_SHORT);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        when(mockShooter.hasQuirk(eq(OptionsConstants.QUIRK_POS_IMP_TARG_S))).thenReturn(false);
        when(mockShooter.hasQuirk(eq(OptionsConstants.QUIRK_POS_VAR_RNG_TARG_S))).thenReturn(true);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_SHORT_RANGE);
        expected.addModifier(FireControl.TH_VAR_RNG_TARG_SHORT_AT_SHORT);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        when(mockShooter.hasQuirk(eq(OptionsConstants.QUIRK_POS_VAR_RNG_TARG_S))).thenReturn(false);
        when(mockShooter.hasQuirk(eq(OptionsConstants.QUIRK_POS_VAR_RNG_TARG_L))).thenReturn(true);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_SHORT_RANGE);
        expected.addModifier(FireControl.TH_VAR_RNG_TARG_LONG_AT_SHORT);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        when(mockShooter.hasQuirk(eq(OptionsConstants.QUIRK_POS_VAR_RNG_TARG_L))).thenReturn(false);
        when(mockShooter.hasQuirk(eq(OptionsConstants.QUIRK_NEG_POOR_TARG_S))).thenReturn(true);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_SHORT_RANGE);
        expected.addModifier(FireControl.TH_POOR_TARG_SHORT);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        when(mockShooter.hasQuirk(eq(OptionsConstants.QUIRK_NEG_POOR_TARG_S))).thenReturn(false);
        when(mockTargetState.getPosition()).thenReturn(mockTargetCoords);

        // Test a targeting computer.
        when(mockShooter.hasTargComp()).thenReturn(true);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_MEDIUM_RANGE);
        expected.addModifier(FireControl.TH_TARGETTING_COMP);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        when(mockWeaponType.hasFlag(eq(WeaponType.F_DIRECT_FIRE))).thenReturn(false);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_MEDIUM_RANGE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        when(mockWeaponType.hasFlag(eq(WeaponType.F_DIRECT_FIRE))).thenReturn(false);
        when(mockShooter.hasTargComp()).thenReturn(false);

        // Test ammo mods.
        final AmmoType mockAmmoType = mock(AmmoType.class);
        when(mockAmmo.getType()).thenReturn(mockAmmoType);
        when(mockAmmoType.getToHitModifier()).thenReturn(1);
        when(mockAmmoType.getMunitionType()).thenReturn(EnumSet.of(AmmoType.Munitions.M_STANDARD));
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_MEDIUM_RANGE);
        expected.addModifier(1, FireControl.TH_AMMO_MOD);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        when(mockAmmoType.getToHitModifier()).thenReturn(0);

        // Test target size mods.
        final LargeSupportTank mockLargeTank = mock(LargeSupportTank.class);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_MEDIUM_RANGE);
        expected.addModifier(FireControl.TH_RNG_LARGE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockLargeTank, mockTargetState, mockWeapon, mockAmmo, mockGame));
        when(((Mech) mockTarget).getCockpitType()).thenReturn(Mech.COCKPIT_SUPERHEAVY);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_MEDIUM_RANGE);
        expected.addModifier(FireControl.TH_RNG_LARGE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        when(((Mech) mockTarget).getCockpitType()).thenReturn(Mech.COCKPIT_STANDARD);

        // Test weapon mods.
        when(mockWeaponType.getToHitModifier()).thenReturn(-2);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_MEDIUM_RANGE);
        expected.addModifier(-2, FireControl.TH_WEAPON_MOD);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        when(mockWeaponType.getToHitModifier()).thenReturn(0);

        // Test heat mods.
        when(mockShooter.getHeatFiringModifier()).thenReturn(1);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_MEDIUM_RANGE);
        expected.addModifier(1, FireControl.TH_HEAT);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        when(mockShooter.getHeatFiringModifier()).thenReturn(0);

        // Test fighter's at altitude
        final ConvFighter mockFighter = mock(ConvFighter.class);
        when(mockFighter.getAltitude()).thenReturn(3);
        when(mockFighter.getId()).thenReturn(2);
        final EntityState mockFighterState = mock(EntityState.class);
        when(mockFighterState.isAirborneAero()).thenReturn(true);
        when(mockFighterState.isBuilding()).thenReturn(false);
        when(mockFighterState.getHeat()).thenReturn(0);
        when(mockFighterState.getPosition()).thenReturn(mockTargetCoords);
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_LONG_RANGE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockFighter, mockFighterState, mockWeapon, mockAmmo, mockGame));
        when(mockFighter.getId()).thenReturn(1); // Target aero is also firing on shooter.
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_SHORT_RANGE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockFighter, mockFighterState, mockWeapon, mockAmmo, mockGame));

        // Test changing the range.
        when(mockTargetState.getPosition()).thenReturn(new Coords(5, 0));
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_SHORT_RANGE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        when(mockTargetState.getPosition()).thenReturn(new Coords(1, 0));
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(3, FireControl.TH_MINIMUM_RANGE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        when(mockGameOptions.booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE)).thenReturn(true);
        when(mockTargetState.getPosition()).thenReturn(new Coords(20, 0));
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_EXTREME_RANGE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        // todo Test infantry range mods.
        when(mockTargetState.getPosition()).thenReturn(mockTargetCoords);
        when(mockGameOptions.booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE)).thenReturn(false);

        // todo Test swarming and leg attacks.

        // Test sensor damage.
        expected = new ToHitData(mockShooter.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_MEDIUM_RANGE);
        when(mockShooter.getBadCriticals(eq(CriticalSlot.TYPE_SYSTEM), eq(Mech.SYSTEM_SENSORS),
                eq(Mech.LOC_HEAD))).thenReturn(2);
        expected.addModifier(2, FireControl.TH_SENSORS);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        final Tank mockTank = mock(Tank.class); // Tank sensor damage is a little different.
        when(mockTank.getCrew()).thenReturn(mockCrew);
        expected = new ToHitData(mockTank.getCrew().getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_MEDIUM_RANGE);
        when(mockTank.getSensorHits()).thenReturn(1);
        expected.addModifier(1, FireControl.TH_SENSORS);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockTank,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        when(mockShooter.getBadCriticals(eq(CriticalSlot.TYPE_SYSTEM), eq(Mech.SYSTEM_SENSORS),
                eq(Mech.LOC_HEAD))).thenReturn(0);

        // Test stopping swarm attacks.
        final WeaponType mockSwarmStop = mock(StopSwarmAttack.class);
        when(mockSwarmStop.getRanges(eq(mockWeapon), any(Mounted.class))).thenReturn(new int[]{0, 0, 0, 0, 0});
        when(mockTargetState.getPosition()).thenReturn(new Coords(0, 0));
        when(mockWeapon.getType()).thenReturn(mockSwarmStop);
        when(mockShooter.getSwarmTargetId()).thenReturn(Entity.NONE); // Invalid attack.
        expected = new ToHitData(FireControl.TH_STOP_SWARM_INVALID);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        when(mockShooter.getSwarmTargetId()).thenReturn(10); // Valid attack.
        expected = new ToHitData(FireControl.TH_SWARM_STOPPED);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        when(mockWeapon.getType()).thenReturn(mockWeaponType);
        when(mockTargetState.getPosition()).thenReturn(mockTargetCoords);

        // Test shooting infantry at 0 range.
        when(mockTargetState.getPosition()).thenReturn(new Coords(0, 0));
        expected = new ToHitData(FireControl.TH_INF_ZERO_RNG);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        // TODO : Infantry on Infantry violence.
        when(mockTargetState.getPosition()).thenReturn(mockTargetCoords);

        // Test being out of range.
        when(mockTargetState.getPosition()).thenReturn(new Coords(0, 100));
        expected = new ToHitData(FireControl.TH_OUT_OF_RANGE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        when(mockTargetState.getPosition()).thenReturn(mockTargetCoords);

        // Test the target being out of arc.
        doReturn(false).when(testFireControl).isInArc(any(Coords.class), anyInt(),
                any(Coords.class), anyInt());
        expected = new ToHitData(FireControl.TH_WEAPON_NO_ARC);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));

        // Test a prone mech w/ no arms.
        when(mockShooterState.isProne()).thenReturn(true);
        when(mockShooter.isLocationBad(Mech.LOC_RARM)).thenReturn(true);
        when(mockShooter.isLocationBad(Mech.LOC_LARM)).thenReturn(true);
        expected = new ToHitData(FireControl.TH_WEAP_PRONE_ARMLESS);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        // Propping self up on firing arm.
        when(mockShooter.isLocationBad(Mech.LOC_LARM)).thenReturn(false);
        expected = new ToHitData(FireControl.TH_WEAP_ARM_PROP);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        // Trying to fire a leg weapon.
        when(mockWeapon.getLocation()).thenReturn(Mech.LOC_LLEG);
        expected = new ToHitData(FireControl.TH_WEAP_PRONE_LEG);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockAmmo, mockGame));
        when(mockShooterState.isProne()).thenReturn(false);

        // Test a weapon that is out of ammo.
        when(mockAmmo.getUsableShotsLeft()).thenReturn(0);
        expected = new ToHitData(FireControl.TH_WEAP_NO_AMMO);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockWeapon.getLinked(), mockGame));
        when(mockAmmo.getUsableShotsLeft()).thenReturn(10);
        when(mockWeapon.getLinked()).thenReturn(null);
        expected = new ToHitData(FireControl.TH_WEAP_NO_AMMO);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockWeapon.getLinked(), mockGame));

        // Test a weapon that cannot fire.
        when(mockWeapon.canFire()).thenReturn(false);
        expected = new ToHitData(FireControl.TH_WEAP_CANNOT_FIRE);
        assertToHitDataEquals(expected, testFireControl.guessToHitModifierForWeapon(mockShooter,
                mockShooterState, mockTarget, mockTargetState, mockWeapon, mockWeapon.getLinked(), mockGame));
    }

    @Test
    public void testGuessAirToGroundStrikeToHitModifier() {
        final MovePath mockFlightPathGood = mock(MovePath.class);
        final MovePath mockFlightPathBad = mock(MovePath.class);
        doReturn(new ToHitData())
               .when(testFireControl)
               .guessToHitModifierHelperForAnyAttack(any(Entity.class), any(EntityState.class),
                       any(Targetable.class), any(EntityState.class), anyInt(), any(Game.class));
        doReturn(true).when(testFireControl).isTargetUnderFlightPath(any(MovePath.class),
                any(EntityState.class));
        doReturn(false).when(testFireControl).isTargetUnderFlightPath(eq(mockFlightPathBad),
                any(EntityState.class));

        final Mounted mockWeapon = mock(Mounted.class);
        when(mockWeapon.canFire()).thenReturn(true);

        final WeaponType mockWeaponType = mock(WeaponType.class);
        when(mockWeapon.getType()).thenReturn(mockWeaponType);
        when(mockWeaponType.getAmmoType()).thenReturn(AmmoType.T_AC);

        final Mounted mockAmmo = mock(Mounted.class);
        when(mockWeapon.getLinked()).thenReturn(mockAmmo);
        when(mockAmmo.getUsableShotsLeft()).thenReturn(10);

        final ConvFighter mockFighter = mock(ConvFighter.class);
        when(mockFighter.getCrew()).thenReturn(mockCrew);

        // Test the vanilla case.
        ToHitData expected = new ToHitData(mockCrew.getGunnery(), FireControl.TH_GUNNERY);
        expected.addModifier(FireControl.TH_AIR_STRIKE);
        assertToHitDataEquals(expected, testFireControl.guessAirToGroundStrikeToHitModifier(
                mockFighter, mockShooterState, mockTarget, mockTargetState, mockFlightPathGood,
                mockWeapon, null, mockGame, true));
        assertToHitDataEquals(expected, testFireControl.guessAirToGroundStrikeToHitModifier(
                mockFighter, mockShooterState, mockTarget, mockTargetState, mockFlightPathGood,
                mockWeapon, null, mockGame, false));

        // Test the target not being under our flight path.
        expected = new ToHitData(FireControl.TH_AIR_STRIKE_PATH);
        assertToHitDataEquals(expected, testFireControl.guessAirToGroundStrikeToHitModifier(
                mockFighter, mockShooterState, mockTarget, mockTargetState, mockFlightPathBad,
                mockWeapon, null, mockGame, false));

        // Test a weapon that is out of ammo.
        when(mockAmmo.getUsableShotsLeft()).thenReturn(0);
        expected = new ToHitData(FireControl.TH_WEAP_NO_AMMO);
        assertToHitDataEquals(expected, testFireControl.guessAirToGroundStrikeToHitModifier(
                mockFighter, mockShooterState, mockTarget, mockTargetState, mockFlightPathGood,
                mockWeapon, null, mockGame, true));

        // Test a weapon who's ammo has been destroyed.
        when(mockWeapon.getLinked()).thenReturn(null);
        expected = new ToHitData(FireControl.TH_WEAP_NO_AMMO);
        assertToHitDataEquals(expected, testFireControl.guessAirToGroundStrikeToHitModifier(
                mockFighter, mockShooterState, mockTarget, mockTargetState, mockFlightPathGood,
                mockWeapon, null, mockGame, true));

        // Test a weapon unable to fire.
        when(mockWeapon.canFire()).thenReturn(false);
        expected = new ToHitData(FireControl.TH_WEAP_CANNOT_FIRE);
        assertToHitDataEquals(expected, testFireControl.guessAirToGroundStrikeToHitModifier(
                mockFighter, mockShooterState, mockTarget, mockTargetState, mockFlightPathGood,
                mockWeapon, null, mockGame, true));
    }

    @Test
    public void testIsTargetUnderFlightPath() {
        // Test the target being under the path.
        Vector<MoveStep> pathSteps = new Vector<>(1);
        MoveStep mockStep = mock(MoveStep.class);
        pathSteps.add(mockStep);
        MovePath mockPath = mock(MovePath.class);
        when(mockPath.getSteps()).thenReturn(pathSteps.elements());
        when(mockStep.getPosition()).thenReturn(mockTargetCoords);
        assertTrue(testFireControl.isTargetUnderFlightPath(mockPath, mockTargetState));

        // Test the target not being under the path.
        pathSteps = new Vector<>(1);
        mockStep = mock(MoveStep.class);
        pathSteps.add(mockStep);
        mockPath = mock(MovePath.class);
        when(mockPath.getSteps()).thenReturn(pathSteps.elements());
        when(mockStep.getPosition()).thenReturn(mockShooterCoords);
        assertFalse(testFireControl.isTargetUnderFlightPath(mockPath, mockTargetState));
    }

    @Test
    @DisabledForJreRange(min = JRE.JAVA_17, max = JRE.OTHER)
    public void testCalculateUtility() {
        final double TOLERANCE = 0.00001;
        int overheatTolerance = 5;
        final double baseUtility = 20.6154;
        final MechWarrior mockPilot = mock(MechWarrior.class);
        when(mockPilot.getId()).thenReturn(20);
        when(mockPilot.isMilitary()).thenReturn(true);

        // Basic firing plan test.
        FiringPlan testFiringPlan = spy(new FiringPlan(mockTarget));
        doReturn(15.0).when(testFiringPlan).getExpectedDamage();
        doReturn(0.46129).when(testFiringPlan).getExpectedCriticals();
        doReturn(0.02005).when(testFiringPlan).getKillProbability();
        doReturn(0).when(testFiringPlan).getHeat();
        doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(any(Targetable.class), anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        assertEquals(baseUtility, testFiringPlan.getUtility(), TOLERANCE);

        // Make the target a commander.
        testFiringPlan = spy(new FiringPlan(mockTarget));
        doReturn(15.0).when(testFiringPlan).getExpectedDamage();
        doReturn(0.46129).when(testFiringPlan).getExpectedCriticals();
        doReturn(0.02005).when(testFiringPlan).getKillProbability();
        doReturn(0).when(testFiringPlan).getHeat();
        doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(any(Targetable.class), anyDouble());
        doReturn(true).when(testFireControl).isCommander(any(Entity.class));
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        assertEquals(baseUtility * (1 + FireControl.COMMANDER_UTILITY), testFiringPlan.getUtility(), TOLERANCE);
        doReturn(false).when(testFireControl).isCommander(any(Entity.class));

        // Make the target a sub-commander.
        testFiringPlan = spy(new FiringPlan(mockTarget));
        doReturn(15.0).when(testFiringPlan).getExpectedDamage();
        doReturn(0.46129).when(testFiringPlan).getExpectedCriticals();
        doReturn(0.02005).when(testFiringPlan).getKillProbability();
        doReturn(0).when(testFiringPlan).getHeat();
        doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(any(Targetable.class), anyDouble());
        doReturn(true).when(testFireControl).isSubCommander(any(Entity.class));
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        assertEquals(baseUtility * (1 + FireControl.SUB_COMMANDER_UTILITY),
                testFiringPlan.getUtility(), TOLERANCE);
        doReturn(false).when(testFireControl).isSubCommander(any(Entity.class));

        // Make the target a Strategic Building Target.
        final BuildingTarget mockBuilding = mock(BuildingTarget.class);
        when(mockBuilding.getPosition()).thenReturn(new Coords(5, 5));
        final BehaviorSettings mockBehavior = mock(BehaviorSettings.class);
        when(mockPrincess.getBehaviorSettings()).thenReturn(mockBehavior);
        final Set<String> testTargets = new HashSet<>(1);
        testTargets.add("0606");
        when(mockBehavior.getStrategicBuildingTargets()).thenReturn(testTargets);
        testFiringPlan = spy(new FiringPlan(mockBuilding));
        doReturn(15.0).when(testFiringPlan).getExpectedDamage();
        doReturn(0.46129).when(testFiringPlan).getExpectedCriticals();
        doReturn(0.02005).when(testFiringPlan).getKillProbability();
        doReturn(0).when(testFiringPlan).getHeat();
        doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(any(Targetable.class), anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        assertEquals(baseUtility * (1 + FireControl.STRATEGIC_TARGET_UTILITY), testFiringPlan.getUtility(),
                            TOLERANCE);
        when(mockBuilding.getPosition()).thenReturn(new Coords(10, 10)); // A building not on the list.
        testFiringPlan = spy(new FiringPlan(mockBuilding));
        doReturn(15.0).when(testFiringPlan).getExpectedDamage();
        doReturn(0.46129).when(testFiringPlan).getExpectedCriticals();
        doReturn(0.02005).when(testFiringPlan).getKillProbability();
        doReturn(0).when(testFiringPlan).getHeat();
        doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(any(Targetable.class), anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        assertEquals(baseUtility, testFiringPlan.getUtility(), TOLERANCE);

        // Make the target a priority unit target
        final Set<Integer> testPriorityUnits = new HashSet<>(1);
        testPriorityUnits.add(MOCK_TARGET_ID);
        when(mockPrincess.getPriorityUnitTargets()).thenReturn(testPriorityUnits);
        testFiringPlan = spy(new FiringPlan(mockTarget));
        doReturn(15.0).when(testFiringPlan).getExpectedDamage();
        doReturn(0.46129).when(testFiringPlan).getExpectedCriticals();
        doReturn(0.02005).when(testFiringPlan).getKillProbability();
        doReturn(0).when(testFiringPlan).getHeat();
        doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(any(Targetable.class), anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        assertEquals(baseUtility * (1 + FireControl.PRIORITY_TARGET_UTILITY), testFiringPlan.getUtility(),
                            TOLERANCE);
        when(mockBehavior.getPriorityUnitTargets()).thenReturn(new HashSet<>(0));
        when(mockPrincess.getPriorityUnitTargets()).thenReturn(new HashSet<>(0));

        // Attack an ejected pilot.
        testFiringPlan = spy(new FiringPlan(mockPilot));
        doReturn(15.0).when(testFiringPlan).getExpectedDamage();
        doReturn(0.46129).when(testFiringPlan).getExpectedCriticals();
        doReturn(0.02005).when(testFiringPlan).getKillProbability();
        doReturn(0).when(testFiringPlan).getHeat();
        doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(any(Targetable.class), anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        assertEquals(-979.3846, testFiringPlan.getUtility(), TOLERANCE);
        assertTrue(baseUtility > testFiringPlan.getUtility());

        // Increase the kill chance.
        testFiringPlan = spy(new FiringPlan(mockTarget));
        doReturn(15.0).when(testFiringPlan).getExpectedDamage();
        doReturn(0.46129).when(testFiringPlan).getExpectedCriticals();
        doReturn(0.12005).when(testFiringPlan).getKillProbability();
        doReturn(0).when(testFiringPlan).getHeat();
        doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(any(Targetable.class), anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        assertEquals(25.6154, testFiringPlan.getUtility(), TOLERANCE);
        assertTrue(baseUtility < testFiringPlan.getUtility());

        // Decrease the kill chance.
        testFiringPlan = spy(new FiringPlan(mockTarget));
        doReturn(15.0).when(testFiringPlan).getExpectedDamage();
        doReturn(0.46129).when(testFiringPlan).getExpectedCriticals();
        doReturn(0.01005).when(testFiringPlan).getKillProbability();
        doReturn(0).when(testFiringPlan).getHeat();
        doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(any(Targetable.class), anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        assertEquals(20.1154, testFiringPlan.getUtility(), TOLERANCE);
        assertTrue(baseUtility > testFiringPlan.getUtility());

        // Increase the # crits.
        testFiringPlan = spy(new FiringPlan(mockTarget));
        doReturn(15.0).when(testFiringPlan).getExpectedDamage();
        doReturn(0.86129).when(testFiringPlan).getExpectedCriticals();
        doReturn(0.02005).when(testFiringPlan).getKillProbability();
        doReturn(0).when(testFiringPlan).getHeat();
        doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(any(Targetable.class), anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        assertEquals(24.6154, testFiringPlan.getUtility(), TOLERANCE);
        assertTrue(baseUtility < testFiringPlan.getUtility());

        // Decrease the # crits.
        testFiringPlan = spy(new FiringPlan(mockTarget));
        doReturn(15.0).when(testFiringPlan).getExpectedDamage();
        doReturn(0.26129).when(testFiringPlan).getExpectedCriticals();
        doReturn(0.02005).when(testFiringPlan).getKillProbability();
        doReturn(0).when(testFiringPlan).getHeat();
        doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(any(Targetable.class), anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        assertEquals(18.6154, testFiringPlan.getUtility(), TOLERANCE);
        assertTrue(baseUtility > testFiringPlan.getUtility());

        // Test a higher damage plan.
        testFiringPlan = spy(new FiringPlan(mockTarget));
        doReturn(20.0).when(testFiringPlan).getExpectedDamage();
        doReturn(0.46129).when(testFiringPlan).getExpectedCriticals();
        doReturn(0.02005).when(testFiringPlan).getKillProbability();
        doReturn(0).when(testFiringPlan).getHeat();
        doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(any(Targetable.class), anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        assertEquals(25.6154, testFiringPlan.getUtility(), TOLERANCE);
        assertTrue(baseUtility < testFiringPlan.getUtility());

        // Test a lower damage plan.
        testFiringPlan = spy(new FiringPlan(mockTarget));
        doReturn(5.0).when(testFiringPlan).getExpectedDamage();
        doReturn(0.46129).when(testFiringPlan).getExpectedCriticals();
        doReturn(0.02005).when(testFiringPlan).getKillProbability();
        doReturn(0).when(testFiringPlan).getHeat();
        doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(any(Targetable.class), anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        assertEquals(10.6154, testFiringPlan.getUtility(), TOLERANCE);
        assertTrue(baseUtility > testFiringPlan.getUtility());

        // Test a higher heat plan.
        testFiringPlan = spy(new FiringPlan(mockTarget));
        doReturn(15.0).when(testFiringPlan).getExpectedDamage();
        doReturn(0.46129).when(testFiringPlan).getExpectedCriticals();
        doReturn(0.02005).when(testFiringPlan).getKillProbability();
        doReturn(15).when(testFiringPlan).getHeat();
        doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(any(Targetable.class), anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        assertEquals(-29.3846, testFiringPlan.getUtility(), TOLERANCE);
        assertTrue(baseUtility > testFiringPlan.getUtility());

        // Test a higher heat tolerance.
        overheatTolerance = 10;
        testFiringPlan = spy(new FiringPlan(mockTarget));
        doReturn(15.0).when(testFiringPlan).getExpectedDamage();
        doReturn(0.46129).when(testFiringPlan).getExpectedCriticals();
        doReturn(0.02005).when(testFiringPlan).getKillProbability();
        doReturn(15).when(testFiringPlan).getHeat();
        doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(any(Targetable.class), anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        assertEquals(-4.38459, testFiringPlan.getUtility(), TOLERANCE);
        assertTrue(baseUtility > testFiringPlan.getUtility());

        // Test a lower heat tolerance.
        overheatTolerance = 0;
        testFiringPlan = spy(new FiringPlan(mockTarget));
        doReturn(15.0).when(testFiringPlan).getExpectedDamage();
        doReturn(0.46129).when(testFiringPlan).getExpectedCriticals();
        doReturn(0.02005).when(testFiringPlan).getKillProbability();
        doReturn(15).when(testFiringPlan).getHeat();
        doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(any(Targetable.class), anyDouble());
        testFireControl.calculateUtility(testFiringPlan, overheatTolerance, false);
        assertEquals(-54.3846, testFiringPlan.getUtility(), TOLERANCE);
        assertTrue(baseUtility > testFiringPlan.getUtility());

        // Basic punch attack.
        PhysicalInfo testPhysicalInfo = spy(new PhysicalInfo(mockPrincess));
        doReturn(15.0).when(testPhysicalInfo).getExpectedDamage();
        doReturn(0.46129).when(testPhysicalInfo).getExpectedCriticals();
        doReturn(0.02005).when(testPhysicalInfo).getKillProbability();
        doReturn(0.5).when(testPhysicalInfo).getProbabilityToHit();
        testFireControl.calculateUtility(testPhysicalInfo);
        assertEquals(baseUtility, testPhysicalInfo.getUtility(), TOLERANCE);

        // Test a punch that cannot hit.
        testPhysicalInfo = spy(new PhysicalInfo(mockPrincess));
        doReturn(15.0).when(testPhysicalInfo).getExpectedDamage();
        doReturn(0.46129).when(testPhysicalInfo).getExpectedCriticals();
        doReturn(0.02005).when(testPhysicalInfo).getKillProbability();
        doReturn(0.0).when(testPhysicalInfo).getProbabilityToHit();
        testFireControl.calculateUtility(testPhysicalInfo);
        assertEquals(-10000, testPhysicalInfo.getUtility(), TOLERANCE);

        // Kick an ejected pilot.
        testPhysicalInfo = spy(new PhysicalInfo(mockPrincess));
        doReturn(15.0).when(testPhysicalInfo).getExpectedDamage();
        doReturn(0.46129).when(testPhysicalInfo).getExpectedCriticals();
        doReturn(0.02005).when(testPhysicalInfo).getKillProbability();
        doReturn(0.5).when(testPhysicalInfo).getProbabilityToHit();
        doReturn(mockPilot).when(testPhysicalInfo).getTarget();
        testFireControl.calculateUtility(testPhysicalInfo);
        assertEquals(-979.3846, testPhysicalInfo.getUtility(), TOLERANCE);
        assertTrue(baseUtility > testPhysicalInfo.getUtility());

        // Increase the kill chance.
        testPhysicalInfo = spy(new PhysicalInfo(mockPrincess));
        doReturn(15.0).when(testPhysicalInfo).getExpectedDamage();
        doReturn(0.46129).when(testPhysicalInfo).getExpectedCriticals();
        doReturn(0.12005).when(testPhysicalInfo).getKillProbability();
        doReturn(0.5).when(testPhysicalInfo).getProbabilityToHit();
        testFireControl.calculateUtility(testPhysicalInfo);
        assertEquals(25.6154, testPhysicalInfo.getUtility(), TOLERANCE);
        assertTrue(baseUtility < testPhysicalInfo.getUtility());

        // Decrease the kill chance.
        testPhysicalInfo = spy(new PhysicalInfo(mockPrincess));
        doReturn(15.0).when(testPhysicalInfo).getExpectedDamage();
        doReturn(0.46129).when(testPhysicalInfo).getExpectedCriticals();
        doReturn(0.01005).when(testPhysicalInfo).getKillProbability();
        doReturn(0.5).when(testPhysicalInfo).getProbabilityToHit();
        testFireControl.calculateUtility(testPhysicalInfo);
        assertEquals(20.1154, testPhysicalInfo.getUtility(), TOLERANCE);
        assertTrue(baseUtility > testPhysicalInfo.getUtility());

        // Increase the # crits.
        testPhysicalInfo = spy(new PhysicalInfo(mockPrincess));
        doReturn(15.0).when(testPhysicalInfo).getExpectedDamage();
        doReturn(0.86129).when(testPhysicalInfo).getExpectedCriticals();
        doReturn(0.02005).when(testPhysicalInfo).getKillProbability();
        doReturn(0.5).when(testPhysicalInfo).getProbabilityToHit();
        testFireControl.calculateUtility(testPhysicalInfo);
        assertEquals(24.6154, testPhysicalInfo.getUtility(), TOLERANCE);
        assertTrue(baseUtility < testPhysicalInfo.getUtility());

        // Decrease the # crits.
        testPhysicalInfo = spy(new PhysicalInfo(mockPrincess));
        doReturn(15.0).when(testPhysicalInfo).getExpectedDamage();
        doReturn(0.26129).when(testPhysicalInfo).getExpectedCriticals();
        doReturn(0.02005).when(testPhysicalInfo).getKillProbability();
        doReturn(0.5).when(testPhysicalInfo).getProbabilityToHit();
        testFireControl.calculateUtility(testPhysicalInfo);
        assertEquals(18.6154, testPhysicalInfo.getUtility(), TOLERANCE);
        assertTrue(baseUtility > testPhysicalInfo.getUtility());

        // Test a higher damage plan.
        testPhysicalInfo = spy(new PhysicalInfo(mockPrincess));
        doReturn(20.0).when(testPhysicalInfo).getExpectedDamage();
        doReturn(0.46129).when(testPhysicalInfo).getExpectedCriticals();
        doReturn(0.02005).when(testPhysicalInfo).getKillProbability();
        doReturn(0.5).when(testPhysicalInfo).getProbabilityToHit();
        testFireControl.calculateUtility(testPhysicalInfo);
        assertEquals(25.6154, testPhysicalInfo.getUtility(), TOLERANCE);
        assertTrue(baseUtility < testPhysicalInfo.getUtility());

        // Test a lower damage plan.
        testPhysicalInfo = spy(new PhysicalInfo(mockPrincess));
        doReturn(5.0).when(testPhysicalInfo).getExpectedDamage();
        doReturn(0.46129).when(testPhysicalInfo).getExpectedCriticals();
        doReturn(0.02005).when(testPhysicalInfo).getKillProbability();
        doReturn(0.5).when(testPhysicalInfo).getProbabilityToHit();
        testFireControl.calculateUtility(testPhysicalInfo);
        assertEquals(10.6154, testPhysicalInfo.getUtility(), TOLERANCE);
        assertTrue(baseUtility > testPhysicalInfo.getUtility());
    }

    @Test
    public void testGuessFullFiringPlan() {
        when(mockShooter.getPosition()).thenReturn(mockShooterCoords);
        when(mockShooter.isOffBoard()).thenReturn(false);
        when(mockShooter.getHeatCapacity()).thenReturn(16);
        when(mockShooter.getHeat()).thenReturn(0);
        when(mockTarget.getPosition()).thenReturn(mockTargetCoords);
        when(mockTarget.isOffBoard()).thenReturn(false);
        when(mockBoard.contains(eq(mockShooterCoords))).thenReturn(true);
        when(mockBoard.contains(eq(mockTargetCoords))).thenReturn(true);
        doNothing().when(testFireControl).calculateUtility(any(FiringPlan.class), anyInt(), anyBoolean());

        // Test the normal case.
        FiringPlan expected = new FiringPlan(mockTarget);
        expected.add(mockPPCFireInfo);
        expected.add(mockLRMFireInfo);
        final FiringPlan actual = testFireControl.guessFullFiringPlan(mockShooter, mockShooterState,
                mockTarget, mockTargetState, mockGame);
        assertEquals(new HashSet<>(expected), new HashSet<>(actual));

        // Test the target not being on the board.
        when(mockTarget.getPosition()).thenReturn(null);
        expected = new FiringPlan(mockTarget);
        assertEquals(expected, testFireControl.guessFullFiringPlan(mockShooter, mockShooterState,
                mockTarget, mockTargetState, mockGame));

        // Test the shooter not being on the board.
        when(mockShooter.getPosition()).thenReturn(null);
        expected = new FiringPlan(mockTarget);
        assertEquals(expected, testFireControl.guessFullFiringPlan(mockShooter, mockShooterState,
                mockTarget, mockTargetState, mockGame));
    }

    @Test
    public void testGuessFullAirToGroundPlan() {
        FiringPlan expected;
        when(mockShooter.getPosition()).thenReturn(mockShooterCoords);
        when(mockShooter.isOffBoard()).thenReturn(false);
        when(mockShooter.getBombs(any(BigInteger.class))).thenReturn(new Vector<>(0));
        when(mockTarget.getPosition()).thenReturn(mockTargetCoords);
        when(mockTarget.isOffBoard()).thenReturn(false);
        when(mockBoard.contains(eq(mockShooterCoords))).thenReturn(true);
        when(mockBoard.contains(eq(mockTargetCoords))).thenReturn(true);
        doNothing().when(testFireControl).calculateUtility(any(FiringPlan.class), anyInt(), anyBoolean());

        final MovePath mockFlightPath = mock(MovePath.class);
        when(mockFlightPath.getFinalAltitude()).thenReturn(5);

        // Test the normal case.
        when(mockPPCFireInfo.getExpectedDamage()).thenReturn(10.0);
        when(mockLRMFireInfo.getExpectedDamage()).thenReturn(5.0);
        expected = new FiringPlan(mockTarget);
        expected.add(mockPPCFireInfo);
        expected.add(mockLRMFireInfo);
        final FiringPlan actual = testFireControl.guessFullAirToGroundPlan(mockShooter, mockTarget, mockTargetState,
                mockFlightPath, mockGame, true);
        assertEquals(new HashSet<>(expected), new HashSet<>(actual));

        // test the target not being on the board.
        when(mockTarget.getPosition()).thenReturn(null);
        expected = new FiringPlan(mockTarget);
        assertEquals(expected, testFireControl.guessFullAirToGroundPlan(mockShooter, mockTarget,
                mockTargetState, mockFlightPath, mockGame, true));

        // Test the shooter not being on the board.
        when(mockShooter.getPosition()).thenReturn(null);
        expected = new FiringPlan(mockTarget);
        assertEquals(expected, testFireControl.guessFullAirToGroundPlan(mockShooter, mockTarget,
                mockTargetState, mockFlightPath, mockGame, true));
    }

    private void prepForFullFiringPlan(ArrayList<Mounted> wepList, ArrayList<Mounted> ammoList) {
        when(mockShooter.getPosition()).thenReturn(mockShooterCoords);
        when(mockShooter.isOffBoard()).thenReturn(false);
        when(mockTarget.getPosition()).thenReturn(mockTargetCoords);
        when(mockTarget.isOffBoard()).thenReturn(false);
        when(mockBoard.contains(eq(mockShooterCoords))).thenReturn(true);
        when(mockBoard.contains(eq(mockTargetCoords))).thenReturn(true);

        // Set up weapons and ammo
        shooterWeapons.clear();
        testToHitThreshold.clear();
        for (Mounted weapon: wepList) {
            when(weapon.canFire()).thenReturn(true);
            shooterWeapons.add(weapon);
            testToHitThreshold.put(weapon, 0.0);
        }
        ArrayList<Mounted> mockAmmoList = new ArrayList<Mounted>();
        for (Mounted ammo: ammoList) {
            mockAmmoList.add(ammo);
        }
        when(mockShooter.getAmmo(any(Mounted.class))).thenReturn(mockAmmoList);

        doNothing().when(testFireControl).calculateUtility(any(FiringPlan.class), anyInt(), anyBoolean());
    }

    @Test
    public void testGetFullFiringPlan() {
        ArrayList<Mounted> wepList = new ArrayList<Mounted>(Arrays.asList(mockPPC, mockLRM5));
        ArrayList<Mounted> ammoList = new ArrayList<Mounted>(Arrays.asList(mockAmmoLRM5, mockAmmoSRM5));
        prepForFullFiringPlan(wepList, ammoList);

        // Test the normal case.
        FiringPlan expected = new FiringPlan(mockTarget);
        expected.add(mockPPCFireInfo);
        expected.add(mockLRMFireInfo);
        final FiringPlan actual = testFireControl.getFullFiringPlan(mockShooter, mockTarget,
                testToHitThreshold, mockGame);
        assertEquals(new HashSet<>(expected), new HashSet<>(actual));

        // test the target not being on the board.
        when(mockTarget.getPosition()).thenReturn(null);
        expected = new FiringPlan(mockTarget);
        assertEquals(expected, testFireControl.getFullFiringPlan(mockShooter, mockTarget,
                testToHitThreshold, mockGame));
        when(mockTarget.getPosition()).thenReturn(mockTargetCoords);

        // Test the shooter not being on the board.
        when(mockShooter.getPosition()).thenReturn(null);
        expected = new FiringPlan(mockTarget);
        assertEquals(expected, testFireControl.getFullFiringPlan(mockShooter, mockTarget,
                testToHitThreshold, mockGame));
        when(mockShooter.getPosition()).thenReturn(mockShooterCoords);

        // Test the LRMs not having a good enough chance to hit.
        testToHitThreshold.put(mockLRM5, 1.0);
        expected = new FiringPlan(mockTarget);
        expected.add(mockPPCFireInfo);
        assertEquals(expected, testFireControl.getFullFiringPlan(mockShooter, mockTarget,
                testToHitThreshold, mockGame));
        testToHitThreshold.put(mockLRM5, 0.0);
    }

    @Test
    public void testChooseAppropriateMMLAmmoForLongRange() {
        ArrayList<Mounted> wepList = new ArrayList<Mounted>(Arrays.asList(mockWeaponMML5));
        ArrayList<Mounted> ammoList = new ArrayList<Mounted>(Arrays.asList(mockAmmoSRM5, mockAmmoLRM5));
        prepForFullFiringPlan(wepList, ammoList);

        // Simulating longer-range engagement
        // Should get the plan back with an LRM5 shot
        FiringPlan expected = new FiringPlan(mockTarget);
        expected.add(mockMMLLRM5FireInfo);
        final FiringPlan actual = testFireControl.getFullFiringPlan(mockShooter, mockTarget,
                testToHitThreshold, mockGame);
        assertEquals(new HashSet<>(expected), new HashSet<>(actual));
    }

    @Test
    public void testChooseAppropriateMMLAmmoForShortRange() {
        ArrayList<Mounted> wepList = new ArrayList<Mounted>(Arrays.asList(mockWeaponMML5));
        ArrayList<Mounted> ammoList = new ArrayList<Mounted>(Arrays.asList(mockAmmoLRM5, mockAmmoSRM5));
        prepForFullFiringPlan(wepList, ammoList);

        // Simulating closer-range engagement
        when(mockMMLSRM5FireInfo.getProbabilityToHit()).thenReturn(0.6);
        when(mockMMLSRM5FireInfo.getExpectedDamage()).thenReturn(0.6*10);

        // Should get the plan back with an SRM5 shot
        FiringPlan expected = new FiringPlan(mockTarget);
        expected.add(mockMMLSRM5FireInfo);
        final FiringPlan actual = testFireControl.getFullFiringPlan(mockShooter, mockTarget,
                testToHitThreshold, mockGame);
        assertEquals(new HashSet<>(expected), new HashSet<>(actual));
    }

    @Test
    public void testChooseLBXAmmoForEngagingFlyer() {
        ArrayList<Mounted> wepList = new ArrayList<Mounted>(Arrays.asList(mockWeaponLB10X));
        ArrayList<Mounted> ammoList = new ArrayList<Mounted>(Arrays.asList(mockAmmoLB10XSlug, mockAmmoLB10XCluster));
        prepForFullFiringPlan(wepList, ammoList);

        // Should get the plan back with a Cluster shot
        FiringPlan expected = new FiringPlan(mockTarget);
        expected.add(mockLB10XClusterFireInfo);
        final FiringPlan actual = testFireControl.getFullFiringPlan(mockShooter, mockTarget,
                testToHitThreshold, mockGame);
        assertEquals(new HashSet<>(expected), new HashSet<>(actual));
    }

    @Test
    public void testCalcFiringPlansUnderHeat() {
        final FiringPlan alphaStrike = new FiringPlan(mockTarget);

        when(mockShooter.getChassis()).thenReturn("mock chassis");

        when(mockPPCFireInfo.getProbabilityToHit()).thenReturn(0.6);
        when(mockPPCFireInfo.getHeat()).thenReturn(10);
        when(mockPPCFireInfo.getExpectedDamageOnHit()).thenReturn(10.0);
        when(mockPPCFireInfo.getExpectedCriticals()).thenReturn(0.46);
        when(mockPPCFireInfo.getKillProbability()).thenReturn(0.002);
        when(mockPPCFireInfo.getWeapon()).thenReturn(mockPPC);
        when(mockPPCFireInfo.getShooter()).thenReturn(mockShooter);
        when(mockPPCFireInfo.getDebugDescription()).thenReturn("mock PPC");
        alphaStrike.add(mockPPCFireInfo);

        when(mockMLFireInfo.getProbabilityToHit()).thenReturn(0.6);
        when(mockMLFireInfo.getHeat()).thenReturn(3);
        when(mockMLFireInfo.getExpectedDamageOnHit()).thenReturn(5.0);
        when(mockMLFireInfo.getExpectedCriticals()).thenReturn(0.0);
        when(mockMLFireInfo.getKillProbability()).thenReturn(0.0);
        when(mockMLFireInfo.getWeapon()).thenReturn(mockML);
        when(mockMLFireInfo.getShooter()).thenReturn(mockShooter);
        when(mockMLFireInfo.getDebugDescription()).thenReturn("mock ML");
        alphaStrike.add(mockMLFireInfo);

        when(mockLRMFireInfo.getProbabilityToHit()).thenReturn(0.6);
        when(mockLRMFireInfo.getHeat()).thenReturn(1);
        when(mockLRMFireInfo.getExpectedDamageOnHit()).thenReturn(3.0);
        when(mockLRMFireInfo.getExpectedCriticals()).thenReturn(0.0);
        when(mockLRMFireInfo.getKillProbability()).thenReturn(0.0);
        when(mockLRMFireInfo.getWeapon()).thenReturn(mockLRM5);
        when(mockLRMFireInfo.getShooter()).thenReturn(mockShooter);
        when(mockLRMFireInfo.getDebugDescription()).thenReturn("mock LRM");
        alphaStrike.add(mockLRMFireInfo);

        final Mounted mockMG = mock(Mounted.class);
        shooterWeapons.add(mockMG);
        final WeaponFireInfo mockMGFireInfo = mock(WeaponFireInfo.class);
        when(mockMGFireInfo.getProbabilityToHit()).thenReturn(0.6);
        when(mockMGFireInfo.getHeat()).thenReturn(0);
        when(mockMGFireInfo.getExpectedDamageOnHit()).thenReturn(2.0);
        when(mockMGFireInfo.getExpectedCriticals()).thenReturn(0.0);
        when(mockMGFireInfo.getKillProbability()).thenReturn(0.0);
        when(mockMGFireInfo.getWeapon()).thenReturn(mockMG);
        when(mockMGFireInfo.getShooter()).thenReturn(mockShooter);
        when(mockMGFireInfo.getDebugDescription()).thenReturn("mock MG");
        alphaStrike.add(mockMGFireInfo);

        doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(any(Targetable.class), anyDouble());

        final FiringPlan[] expected = new FiringPlan[15];
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
        final FiringPlan[] actual = testFireControl.calcFiringPlansUnderHeat(mockShooter, alphaStrike);
        assertArrayEquals(expected, actual);
    }

    private void assertArrayEquals(final FiringPlan[] expected, final Object actual) {
        assertNotNull(actual);
        assertTrue(actual instanceof FiringPlan[], "actual: " + actual.getClass().getName());

        final FiringPlan[] actualArray = (FiringPlan[]) actual;
        assertEquals(expected.length, actualArray.length);

        final StringBuilder failure = new StringBuilder();
        for (int i = 0; i < expected.length; i++) {
            if ((null == expected[i]) && (null != actualArray[i])) {
                failure.append("\nExpected[").append(i).append("]: null");
                failure.append("\nActual[").append(i).append("]:   ").append(actualArray[i].getDebugDescription(true));
                continue;
            }
            if (!expected[i].equals(actualArray[i])) {
                failure.append("\nExpected[").append(i).append("]: ").append(expected[i].getDebugDescription(true));
                if (null == actualArray[i]) {
                    failure.append("\nActual[").append(i).append("]:   null");
                } else {
                    failure.append("\nActual[").append(i).append("]:   ").append(actualArray[i].getDebugDescription
                            (true));
                }
            }
        }

        if (!StringUtility.isNullOrBlank(failure.toString())) {
            fail(failure.toString());
        }
    }

    /**
     * Test to make sure that Princess will choose a FiringPlan that shoots at
     * a MechWarrior, instead of choosing to do nothing.
     */
    @Test
    public void testCalcFiringPlansAtMechWarrior() {
        mockTarget = mock(MechWarrior.class);
        when(mockPPCFireInfo.getProbabilityToHit()).thenReturn(0.6);
        when(mockPPCFireInfo.getHeat()).thenReturn(10);
        when(mockPPCFireInfo.getExpectedDamageOnHit()).thenReturn(10.0);

        when(mockMLFireInfo.getProbabilityToHit()).thenReturn(0.6);
        when(mockMLFireInfo.getHeat()).thenReturn(3);
        when(mockMLFireInfo.getExpectedDamageOnHit()).thenReturn(5.0);

        when(mockLRMFireInfo.getProbabilityToHit()).thenReturn(0.6);
        when(mockLRMFireInfo.getHeat()).thenReturn(1);
        when(mockLRMFireInfo.getExpectedDamageOnHit()).thenReturn(3.0);

        doReturn(0.0).when(testFireControl).calcDamageAllocationUtility(any(Targetable.class), anyDouble());

        when(mockShooter.getPosition()).thenReturn(mockShooterCoords);
        when(mockTarget.getPosition()).thenReturn(mockTargetCoords);
        when(mockShooter.getWeaponList()).thenReturn(shooterWeapons);
        final FiringPlan plan = testFireControl.getBestFiringPlan(mockShooter, mockTarget, mockGame,
                testToHitThreshold);
        assertFalse(0.00001 > Math.abs(0 - plan.getUtility()), "Expected not 0.0.  Got " + plan.getUtility());
    }
}
