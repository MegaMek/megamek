/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.common;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.units.Entity;
import megamek.common.units.IAero;
import megamek.common.units.UnitLocation;

/** A transient, server-confirmed visual event. It is not an attack order or another damage/rules calculation. */
public record ResolvedAttack(UUID id, Kind kind, UnitLocation attacker, UnitLocation target, int targetType,
      int equipmentIndex, String equipmentName, int limb, boolean hit, List<Mount> mounts, Shot shot,
      List<Impact> impacts) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public ResolvedAttack {
        mounts = mounts == null ? singleMount(attacker, equipmentIndex) : List.copyOf(mounts);
        impacts = impacts == null ? List.of() : List.copyOf(impacts);
    }

    public ResolvedAttack(UUID id, Kind kind, UnitLocation attacker, UnitLocation target, int targetType,
          int equipmentIndex, String equipmentName, int limb, boolean hit, List<Mount> mounts, Shot shot) {
        this(id, kind, attacker, target, targetType, equipmentIndex, equipmentName, limb, hit, mounts, shot, List.of());
    }

    /** Observed incoming locations; damage weights distribute visuals, never recalculate hits or damage. */
    public record Impact(String location, boolean rear, int weight) implements Serializable { }

    public ResolvedAttack withImpacts(List<Impact> locations) {
        return new ResolvedAttack(id, kind, attacker, target, targetType, equipmentIndex, equipmentName, limb, hit,
              mounts, shot, locations);
    }

    public ResolvedAttack(UUID id, Kind kind, UnitLocation attacker, UnitLocation target, int targetType,
          int equipmentIndex, String equipmentName, int limb, boolean hit, List<Mount> mounts) {
        this(id, kind, attacker, target, targetType, equipmentIndex, equipmentName, limb, hit, mounts, null);
    }

    public ResolvedAttack(UUID id, Kind kind, UnitLocation attacker, UnitLocation target, int targetType,
          int equipmentIndex, String equipmentName, int limb, boolean hit) {
        this(id, kind, attacker, target, targetType, equipmentIndex, equipmentName, limb, hit, singleMount(attacker, equipmentIndex));
    }

    /** Identity of a physical gun, including the owning fighter when a squadron fires a logical group. */
    public record Mount(int entityId, int equipmentIndex, Shot shot) implements Serializable {
        public Mount(int entityId, int equipmentIndex) { this(entityId, equipmentIndex, null); }
    }

    /** Observed firing configuration, not another equipment matcher or attack-resolution calculation. */
    public record Shot(String mode, Set<String> munitions, boolean artillery, boolean defensive, int shots,
          int missiles, boolean indirect, Integer missileHits, UnitLocation launch, UnitLocation impact,
          boolean ballistic, int rackSize, boolean ppc) implements Serializable {
        public Shot { munitions = Set.copyOf(munitions); }

        public Shot(String mode, Set<String> munitions, boolean artillery, boolean defensive, int shots,
              int missiles, boolean indirect, Integer missileHits, UnitLocation launch, UnitLocation impact,
              boolean ballistic, int rackSize) {
            this(mode, munitions, artillery, defensive, shots, missiles, indirect, missileHits, launch, impact, ballistic, rackSize, false);
        }

        public Shot(String mode, Set<String> munitions, boolean artillery, boolean defensive, int shots,
              int missiles, boolean indirect, Integer missileHits, UnitLocation launch, UnitLocation impact) {
            this(mode, munitions, artillery, defensive, shots, missiles, indirect, missileHits, launch, impact, false, 0);
        }

        public Shot(String mode, Set<String> munitions, boolean artillery, boolean defensive, int shots,
              int missiles, boolean indirect, Integer missileHits) {
            this(mode, munitions, artillery, defensive, shots, missiles, indirect, missileHits, null, null);
        }

        public Shot(String mode, Set<String> munitions, boolean artillery, boolean defensive, int shots,
              int missiles, boolean indirect) {
            this(mode, munitions, artillery, defensive, shots, missiles, indirect, null);
        }

        /** Null means the rules resolved attack value/damage, without an individual missile-hit count. */
        public Shot withResolution(AmmoType ammo, Integer hits) {
            return new Shot(mode, ammo == null ? munitions : ammo.getMunitionType().stream()
                  .map(Enum::name).collect(java.util.stream.Collectors.toSet()), artillery, defensive, shots,
                  missiles, indirect, hits, launch, impact, ballistic, rackSize, ppc);
        }

        public Shot asDefensive() {
            return new Shot(mode, munitions, artillery, true, shots, missiles, indirect, missileHits, launch, impact, ballistic, rackSize, ppc);
        }

        /** Artillery supplies its observed launch and resolved landing, including scatter. No client re-roll. */
        public Shot withTrajectory(UnitLocation origin, UnitLocation destination) {
            return new Shot(mode, munitions, true, defensive, shots, missiles, true, missileHits, origin, destination, ballistic, rackSize, ppc);
        }

        public static Shot capture(Mounted<?> mount) {
            if (!(mount instanceof WeaponMounted weapon)) { return null; }
            var ammo = weapon.getLinked() == null ? null : weapon.getLinked().getType();
            Set<String> munitions = ammo instanceof AmmoType type ? type.getMunitionType().stream()
                  .map(Enum::name).collect(java.util.stream.Collectors.toSet()) : Set.of();
            return new Shot(weapon.curMode().getName(), munitions, weapon.getType().hasFlag(WeaponType.F_ARTILLERY),
                  weapon.getType().hasFlag(WeaponType.F_AMS) || weapon.getType().hasFlag(WeaponType.F_AMS_BAY),
                  weapon.getCurrentShots(), weapon.getType().hasFlag(WeaponType.F_MISSILE)
                        ? weapon.getType().hasFlag(WeaponType.F_LARGE_MISSILE) ? 1 : Math.max(1, weapon.getType().getRackSize()) : 0,
                  weapon.curMode().isIndirect(), null, null, null,
                  weapon.getType().hasFlag(WeaponType.F_BALLISTIC), weapon.getType().getRackSize(), weapon.getType().hasFlag(WeaponType.F_PPC));
        }
    }

    private static List<Mount> singleMount(UnitLocation attacker, int equipmentIndex) {
        return equipmentIndex < 0 ? List.of() : List.of(new Mount(attacker.entityId(), equipmentIndex));
    }

    /** Resolve once while handling the confirmed attack. Presentation never rebuilds group membership. */
    public static List<Mount> captureMounts(Entity attacker, int equipmentIndex) {
        return physicalMounts(equipmentIndex < 0 ? null : attacker.getEquipment(equipmentIndex))
              .map(mount -> new Mount(mount.getEntity().getId(), mount.getEquipmentNum(), Shot.capture(mount))).distinct().toList();
    }

    private static Stream<? extends Mounted<?>> physicalMounts(Mounted<?> mount) {
        if (mount == null || mount.isInoperable()) {
            return Stream.empty();
        }
        if (mount instanceof WeaponMounted weapon) {
            if (weapon.isWeaponGroup() && mount.getEntity() instanceof IAero aero) {
                return aero.getWeaponGroupMembers(weapon).stream().flatMap(ResolvedAttack::physicalMounts);
            }
            if (!weapon.getBayWeapons().isEmpty()) {
                return weapon.getBayWeapons().stream().flatMap(ResolvedAttack::physicalMounts);
            }
        }
        return Stream.of(mount);
    }

    public enum Kind { SHOT, PUNCH, KICK, PUSH, CLUB, DEATH }
}
