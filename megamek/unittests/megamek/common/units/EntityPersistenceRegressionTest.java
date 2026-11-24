/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import megamek.common.OffBoardDirection;
import megamek.common.Player;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.equipment.*;
import megamek.common.game.IGame;
import megamek.common.loaders.MekFileParser;
import megamek.common.orders.UnitOrderAction;
import megamek.common.orders.UnitOrders;
import megamek.common.turns.TurnOrdered;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * MekHQ takes a unit's entity from one battle into the next, so any battle state left on it carries over. Before,
 * artillery fire adjustment from an earlier battle was still active in the next one (MekHQ issue #8941).
 *
 * <p>Every non-static, non-transient field of the checked {@link Entity} and {@link Mounted} classes is either listed
 * in the allow list, with the reason it may persist, or must be reset by {@link Entity#clearGameState()}. For each
 * fixture the test changes every field that must be reset, one at a time, runs clearGameState, and fails if the change
 * survived. A newly added field therefore fails this test until it is reset in clearGameState or listed in the allow
 * list.</p>
 */
class EntityPersistenceRegressionTest {
    private static final String UNIT_DIRECTORY = "testresources/data/mekfiles/";

    private static final Path ALLOWED_TO_PERSIST = Path.of("testresources", "data", "entity-persistence",
          "allowed-to-persist.txt");

    private static final Set<String> CATEGORIES = Set.of("KEEP", "GAME_RESET", "KNOWN_LEAK");

    /**
     * Every class the allow list covers. A field added to any of them must be reset or listed, and a fixture whose
     * entity or equipment is of another class fails until that class is added here.
     */
    private static final List<Class<?>> CHECKED_CLASSES = List.of(TurnOrdered.class, Entity.class, Mek.class,
          MekWithArms.class, BipedMek.class, Tank.class, SuperHeavyTank.class, SupportTank.class, VTOL.class,
          Infantry.class, ConvInfantry.class, BattleArmor.class, ProtoMek.class, Aero.class, AeroSpaceFighter.class,
          SmallCraft.class, Mounted.class, WeaponMounted.class, AmmoMounted.class, MiscMounted.class,
          BombMounted.class, InfantryWeaponMounted.class);

    /** Owned by something other than the field holding them, so they are compared by type only */
    private static final List<Class<?>> COMPARED_BY_TYPE_ONLY = List.of(Entity.class, Mounted.class, Player.class,
          IGame.class, Crew.class, EquipmentType.class, Transporter.class);

    private static final int CHANGED_NUMBER = 4242;
    private static final int MAX_COMPARISON_DEPTH = 6;
    private static final int MAX_REPORTED_VALUE_LENGTH = 160;

    /** Changes for the fields the generic changes cannot handle, keyed like the allow list */
    private static final Map<String, FieldChange> CUSTOM_CHANGES = customChanges();

    private static List<String> allowListLines;
    private static Map<String, String> allowListCategories;

    @FunctionalInterface
    private interface FieldChange {
        void change(Object owner, Field field) throws Exception;
    }

    /** One unit of each checked entity class, pinned copies of mm-data unit files */
    private enum UnitFixture {
        WOLVERINE_WVR_6R("Wolverine WVR-6R.mtf", BipedMek.class),
        VEDETTE_MEDIUM_TANK("Vedette Medium Tank.blk", Tank.class),
        BURKE_II_SUPERHEAVY_TANK("Burke II Superheavy Tank.blk", SuperHeavyTank.class),
        CELLCO_RANGER_UPU_3000("Cellco Ranger UPU-3000.blk", SupportTank.class),
        WARRIOR_H_7_ATTACK_HELICOPTER("Warrior H-7 Attack Helicopter.blk", VTOL.class),
        IS_STANDARD_BATTLE_ARMOR_LASER("IS Standard BA [Laser] (Sqd4).blk", BattleArmor.class),
        FOOT_PLATOON_LASER("Foot Platoon (DCMS) (Laser 2620+).blk", ConvInfantry.class),
        MINOTAUR_PROTOMEK("Minotaur.blk", ProtoMek.class),
        SHILONE_SL_17("Shilone SL-17.blk", AeroSpaceFighter.class),
        SHUTTLE_ST_46("Shuttle ST-46.blk", SmallCraft.class);

        private final String fileName;
        private final Class<? extends Entity> unitClass;

        UnitFixture(String fileName, Class<? extends Entity> unitClass) {
            this.fileName = fileName;
            this.unitClass = unitClass;
        }

        Entity loadEntity() throws Exception {
            Entity entity = new MekFileParser(new File(UNIT_DIRECTORY + fileName)).getEntity();
            assertNotNull(entity, name() + " parsed to no entity from " + fileName);
            assertTrue(unitClass.isInstance(entity), name() + " loaded as " + entity.getClass().getSimpleName()
                  + ", expected " + unitClass.getSimpleName());
            return entity;
        }
    }

    @BeforeAll
    static void loadAllowList() throws IOException {
        EquipmentType.initializeTypes();
        allowListLines = Files.readAllLines(ALLOWED_TO_PERSIST);
        allowListCategories = new HashMap<>();
        for (String line : allowListLines) {
            String[] parts = line.trim().split("\\s+", 3);
            if (!isComment(line) && (parts.length >= 2)) {
                allowListCategories.put(parts[0], parts[1]);
            }
        }
    }

    @Test
    void everyAllowListLineNamesAFieldOfACheckedClass() {
        Set<String> checkedFields = new HashSet<>();
        for (Class<?> checkedClass : CHECKED_CLASSES) {
            for (Field field : declaredPersistableFields(checkedClass)) {
                checkedFields.add(key(field));
            }
        }

        List<String> problems = new ArrayList<>();
        Set<String> listed = new HashSet<>();
        for (int index = 0; index < allowListLines.size(); index++) {
            String line = allowListLines.get(index);
            if (isComment(line)) {
                continue;
            }
            String[] parts = line.trim().split("\\s+", 3);
            String where = "line " + (index + 1) + " (" + line.trim() + ")";
            if (parts.length < 3) {
                problems.add(where + " needs a field, a category and a reason");
            } else if (!CATEGORIES.contains(parts[1])) {
                problems.add(where + " has a category other than " + CATEGORIES);
            } else if (!listed.add(parts[0])) {
                problems.add(where + " lists a field already listed above");
            } else if (!checkedFields.contains(parts[0])) {
                problems.add(where + " names no non-static, non-transient field of the checked classes");
            }
        }

        assertTrue(problems.isEmpty(), () -> "Problems in " + ALLOWED_TO_PERSIST + ":\n  "
              + String.join("\n  ", problems));
    }

    @Test
    void everyFieldThatMustResetCanBeChangedByTheTest() {
        List<String> problems = new ArrayList<>();
        for (Class<?> checkedClass : CHECKED_CLASSES) {
            for (Field field : declaredPersistableFields(checkedClass)) {
                if (!allowListCategories.containsKey(key(field)) && !canChange(field)) {
                    problems.add(key(field) + " (" + field.getGenericType().getTypeName() + ")");
                }
            }
        }

        assertTrue(problems.isEmpty(), () -> "These fields are not in " + ALLOWED_TO_PERSIST
              + ", so they must be reset, but the test cannot change them to check that. Give each a change in"
              + " CUSTOM_CHANGES, or list it in the allow list:\n  " + String.join("\n  ", problems));
    }

    @ParameterizedTest
    @EnumSource(UnitFixture.class)
    void battleStateDoesNotSurviveClearGameState(UnitFixture fixture) throws Exception {
        Entity reference = clearedEntity(fixture);
        Entity subject = clearedEntity(fixture);

        List<String> uncheckedClasses = new ArrayList<>(uncheckedClassesOf(reference));
        for (Mounted<?> mounted : reference.getEquipment()) {
            uncheckedClasses.addAll(uncheckedClassesOf(mounted));
        }
        assertTrue(uncheckedClasses.isEmpty(), () -> fixture.name() + " uses classes the allow list does not cover;"
              + " add them to CHECKED_CLASSES and list their fields: " + uncheckedClasses);

        Map<String, String> problems = new LinkedHashMap<>();
        for (Field field : fieldsThatMustReset(reference.getClass())) {
            if (!resetsOnClearGameState(subject, subject, reference, field, problems)) {
                subject = clearedEntity(fixture);
            }
        }

        List<Mounted<?>> referenceEquipment = reference.getEquipment();
        assertEquals(referenceEquipment.size(), subject.getEquipment().size(),
              "Two loads of " + fixture.name() + " should have the same equipment");
        for (int index = 0; index < referenceEquipment.size(); index++) {
            Mounted<?> referenceMounted = referenceEquipment.get(index);
            for (Field field : fieldsThatMustReset(referenceMounted.getClass())) {
                Mounted<?> mounted = subject.getEquipment().get(index);
                if (!resetsOnClearGameState(subject, mounted, referenceMounted, field, problems)) {
                    subject = clearedEntity(fixture);
                }
            }
        }

        assertTrue(problems.isEmpty(), () -> report(fixture, problems));
    }

    /**
     * Changes one field of the subject (or of one of its equipment), runs clearGameState, and checks the field is back
     * to the value a freshly cleared unit has.
     *
     * @return {@code false} if a problem was recorded, after which the subject may no longer be clean
     */
    private static boolean resetsOnClearGameState(Entity subject, Object owner, Object referenceOwner, Field field,
          Map<String, String> problems) throws IllegalAccessException {
        String key = key(field);
        try {
            change(owner, field);
        } catch (Exception cannotChange) {
            problems.putIfAbsent(key, "the test cannot change it (" + cannotChange + "); give it a change in"
                  + " CUSTOM_CHANGES or list it in the allow list");
            return false;
        }

        try {
            subject.clearGameState();
        } catch (RuntimeException failure) {
            problems.putIfAbsent(key, "clearGameState failed once it was changed: " + failure);
            return false;
        }

        String expected = fingerprint(field.get(referenceOwner));
        String actual = fingerprint(field.get(owner));
        if (expected.equals(actual)) {
            return true;
        }
        problems.putIfAbsent(key, "still " + abbreviate(actual) + " after clearGameState; a freshly cleared unit has "
              + abbreviate(expected));
        return false;
    }

    private static String report(UnitFixture fixture, Map<String, String> problems) {
        StringBuilder report = new StringBuilder(fixture.name())
              .append(" keeps battle state after Entity.clearGameState. Reset each field in clearGameState(), or")
              .append(" list it in ").append(ALLOWED_TO_PERSIST).append(" with the reason it may persist.\n");
        problems.forEach((key, problem) -> report.append("  ").append(key).append(": ").append(problem).append('\n'));
        report.append("Allow list lines to start from:\n");
        problems.keySet()
              .forEach(key -> report.append(String.format("%-48s KNOWN_LEAK  <why it may persist>%n", key)));
        return report.toString();
    }

    private static Entity clearedEntity(UnitFixture fixture) throws Exception {
        Entity entity = fixture.loadEntity();
        entity.clearGameState();
        return entity;
    }

    private static List<String> uncheckedClassesOf(Object value) {
        List<String> unchecked = new ArrayList<>();
        for (Class<?> type = value.getClass(); (type != null) && (type != Object.class); type = type.getSuperclass()) {
            if (!CHECKED_CLASSES.contains(type)) {
                unchecked.add(type.getName());
            }
        }
        return unchecked;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Fields
    // ---------------------------------------------------------------------------------------------------------------

    private static boolean isComment(String line) {
        return line.isBlank() || line.trim().startsWith("#");
    }

    private static String key(Field field) {
        return field.getDeclaringClass().getSimpleName() + '.' + field.getName();
    }

    private static List<Field> declaredPersistableFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (!Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers) && !field.isSynthetic()) {
                field.setAccessible(true);
                fields.add(field);
            }
        }
        return fields;
    }

    private static List<Field> persistableFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = type; (current != null) && (current != Object.class);
              current = current.getSuperclass()) {
            fields.addAll(declaredPersistableFields(current));
        }
        return fields;
    }

    private static List<Field> fieldsThatMustReset(Class<?> type) {
        return persistableFields(type).stream().filter(field -> !allowListCategories.containsKey(key(field))).toList();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Changing a field
    // ---------------------------------------------------------------------------------------------------------------

    private static Map<String, FieldChange> customChanges() {
        Map<String, FieldChange> changes = new HashMap<>();
        changes.put("Entity.aTracker", (owner, field) -> {
            Entity entity = (Entity) owner;
            entity.aTracker.setModifier(-2, new Coords(4, 4));
            entity.aTracker.setSpotterHasForwardObs(true);
            entity.aTracker.setSpotterHasCommImplant(true);
        });
        changes.put("Entity.infernos", (owner, field) -> changeEveryPrimitiveField(field.get(owner)));
        changes.put("Entity.unitOrders", (owner, field) -> ((Entity) owner).setUnitOrders(
              UnitOrderAction.EXIT_BY_EDGE.apply(UnitOrders.NONE, List.of(), OffBoardDirection.NORTH,
                    UnitOrders.FACING_AUTO, UnitOrders.FACING_AUTO, null, 1)));
        changes.put("Entity.pendingNarcPods", (owner, field) -> addTo(owner, field, new NarcPod(1, 0)));
        changes.put("Entity.narcPods", (owner, field) -> addTo(owner, field, new NarcPod(1, 0)));
        changes.put("Entity.pendingINarcPods",
              (owner, field) -> addTo(owner, field, new INarcPod(1, INarcPod.HOMING, 0)));
        changes.put("Entity.iNarcPods", (owner, field) -> addTo(owner, field, new INarcPod(1, INarcPod.HOMING, 0)));
        changes.put("Entity.carriedObjects", (owner, field) -> putIn(owner, field, 0, new ObjectiveMarker()));
        changes.put("Tank.jammedWeapons",
              (owner, field) -> addTo(owner, field, ((Entity) owner).getEquipment().get(0)));
        return changes;
    }

    private static void change(Object owner, Field field) throws Exception {
        FieldChange customChange = CUSTOM_CHANGES.get(key(field));
        if (customChange != null) {
            customChange.change(owner, field);
            return;
        }

        Class<?> type = field.getType();
        Object current = field.get(owner);
        if (type.isArray()) {
            Class<?> componentType = type.getComponentType();
            if ((current == null) || (Array.getLength(current) == 0)) {
                Object array = Array.newInstance(componentType, 1);
                Array.set(array, 0, changedValue(componentType, null));
                field.set(owner, array);
            } else {
                Array.set(current, 0, changedValue(componentType, Array.get(current, 0)));
            }
        } else if (Collection.class.isAssignableFrom(type)) {
            addTo(owner, field, changedValue(typeArgument(field, 0), null));
        } else if (Map.class.isAssignableFrom(type)) {
            putIn(owner, field, changedValue(typeArgument(field, 0), null),
                  changedValue(typeArgument(field, 1), null));
        } else {
            field.set(owner, changedValue(type, current));
        }
    }

    private static boolean canChange(Field field) {
        if (CUSTOM_CHANGES.containsKey(key(field))) {
            return true;
        }
        Class<?> type = field.getType();
        if (type.isArray()) {
            return canChangeType(type.getComponentType());
        } else if (Collection.class.isAssignableFrom(type)) {
            return canChangeType(typeArgumentOrNull(field, 0));
        } else if (Map.class.isAssignableFrom(type)) {
            return canChangeType(typeArgumentOrNull(field, 0)) && canChangeType(typeArgumentOrNull(field, 1));
        }
        return canChangeType(type);
    }

    private static boolean canChangeType(Class<?> type) {
        if (type == null) {
            return false;
        } else if (type.isEnum()) {
            return type.getEnumConstants().length > 1;
        }
        return (type.isPrimitive() && (type != void.class)) || (type == Boolean.class) || (type == Integer.class)
              || (type == Long.class) || (type == Short.class) || (type == Byte.class) || (type == Double.class)
              || (type == Float.class) || (type == Character.class) || (type == String.class)
              || (type == Coords.class);
    }

    /** @return a value of the given type that differs from the current one */
    private static Object changedValue(Class<?> type, Object current) {
        if ((type == boolean.class) || (type == Boolean.class)) {
            return (current == null) || !((Boolean) current);
        } else if ((type == int.class) || (type == Integer.class)) {
            return (current == null) ? CHANGED_NUMBER : ((Integer) current + 1);
        } else if ((type == long.class) || (type == Long.class)) {
            return (current == null) ? (long) CHANGED_NUMBER : ((Long) current + 1);
        } else if ((type == short.class) || (type == Short.class)) {
            return (short) ((current == null) ? CHANGED_NUMBER : ((Short) current + 1));
        } else if ((type == byte.class) || (type == Byte.class)) {
            return (byte) ((current == null) ? 42 : ((Byte) current + 1));
        } else if ((type == double.class) || (type == Double.class)) {
            return (current == null) ? 42.5 : ((Double) current + 1.5);
        } else if ((type == float.class) || (type == Float.class)) {
            return (current == null) ? 42.5f : ((Float) current + 1.5f);
        } else if ((type == char.class) || (type == Character.class)) {
            return (char) (((current == null) ? 'a' : (Character) current) + 1);
        } else if (type == String.class) {
            return (current == null) ? "changed" : (current + " (changed)");
        } else if (type == Coords.class) {
            return (current == null) ? new Coords(7, 7)
                  : new Coords(((Coords) current).getX() + 1, ((Coords) current).getY() + 1);
        } else if (type.isEnum()) {
            for (Object constant : type.getEnumConstants()) {
                if (constant != current) {
                    return constant;
                }
            }
        }
        throw new IllegalArgumentException("there is no change for type " + type.getName());
    }

    private static void changeEveryPrimitiveField(Object target) throws IllegalAccessException {
        for (Field field : persistableFields(target.getClass())) {
            if (field.getType().isPrimitive()) {
                field.set(target, changedValue(field.getType(), field.get(target)));
            }
        }
    }

    private static Class<?> typeArgument(Field field, int index) {
        Class<?> argument = typeArgumentOrNull(field, index);
        if (argument == null) {
            throw new IllegalArgumentException("its type arguments are not plain classes");
        }
        return argument;
    }

    private static Class<?> typeArgumentOrNull(Field field, int index) {
        Type genericType = field.getGenericType();
        if ((genericType instanceof ParameterizedType parameterizedType)
              && (parameterizedType.getActualTypeArguments()[index] instanceof Class<?> argument)) {
            return argument;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static void addTo(Object owner, Field field, Object element) throws ReflectiveOperationException {
        Collection<Object> collection = (Collection<Object>) field.get(owner);
        if (collection == null) {
            collection = newCollection(field.getType());
            field.set(owner, collection);
        }
        try {
            collection.add(element);
        } catch (UnsupportedOperationException immutable) {
            Collection<Object> copy = newCollection(field.getType());
            copy.addAll(collection);
            copy.add(element);
            field.set(owner, copy);
        }
    }

    @SuppressWarnings("unchecked")
    private static void putIn(Object owner, Field field, Object key, Object value) throws ReflectiveOperationException {
        Map<Object, Object> map = (Map<Object, Object>) field.get(owner);
        if (map == null) {
            map = newMap(field.getType());
            field.set(owner, map);
        }
        try {
            map.put(key, value);
        } catch (UnsupportedOperationException immutable) {
            Map<Object, Object> copy = newMap(field.getType());
            copy.putAll(map);
            copy.put(key, value);
            field.set(owner, copy);
        }
    }

    @SuppressWarnings("unchecked")
    private static Collection<Object> newCollection(Class<?> type) throws ReflectiveOperationException {
        if (!type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
            return (Collection<Object>) type.getDeclaredConstructor().newInstance();
        }
        return Set.class.isAssignableFrom(type) ? new HashSet<>() : new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> newMap(Class<?> type) throws ReflectiveOperationException {
        if (!type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
            return (Map<Object, Object>) type.getDeclaredConstructor().newInstance();
        }
        return new HashMap<>();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Comparing a field
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * @return a description of the value that is equal for equal values, so a field of the subject can be compared
     *       with the same field of the reference unit, which holds different instances
     */
    private static String fingerprint(Object value) throws IllegalAccessException {
        return fingerprint(value, 0, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static String fingerprint(Object value, int depth, Set<Object> visiting) throws IllegalAccessException {
        if (value == null) {
            return "null";
        }
        Class<?> type = value.getClass();
        if (type.isArray()) {
            List<String> elements = new ArrayList<>();
            for (int index = 0; index < Array.getLength(value); index++) {
                elements.add(fingerprint(Array.get(value, index), depth + 1, visiting));
            }
            return elements.toString();
        } else if (value instanceof Collection<?> collection) {
            List<String> elements = new ArrayList<>();
            for (Object element : collection) {
                elements.add(fingerprint(element, depth + 1, visiting));
            }
            if (!(value instanceof List)) {
                Collections.sort(elements);
            }
            return elements.toString();
        } else if (value instanceof Map<?, ?> map) {
            List<String> entries = new ArrayList<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                entries.add(fingerprint(entry.getKey(), depth + 1, visiting) + '='
                      + fingerprint(entry.getValue(), depth + 1, visiting));
            }
            Collections.sort(entries);
            return "{" + String.join(", ", entries) + "}";
        } else if (value instanceof Enum<?> constant) {
            return constant.name();
        } else if ((value instanceof Number) || (value instanceof Boolean) || (value instanceof Character)
              || (value instanceof CharSequence)) {
            return value.toString();
        }

        for (Class<?> comparedByTypeOnly : COMPARED_BY_TYPE_ONLY) {
            if (comparedByTypeOnly.isInstance(value)) {
                return "<" + type.getSimpleName() + ">";
            }
        }
        if (overridesObjectMethod(type, "equals", Object.class) && overridesObjectMethod(type, "toString")) {
            return value.toString();
        }
        if (!type.getName().startsWith("megamek.") || (depth >= MAX_COMPARISON_DEPTH) || !visiting.add(value)) {
            return "<" + type.getSimpleName() + ">";
        }

        try {
            List<String> fields = new ArrayList<>();
            for (Field field : persistableFields(type)) {
                fields.add(field.getName() + '=' + fingerprint(field.get(value), depth + 1, visiting));
            }
            return type.getSimpleName() + fields;
        } finally {
            visiting.remove(value);
        }
    }

    private static boolean overridesObjectMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            return type.getMethod(name, parameterTypes).getDeclaringClass() != Object.class;
        } catch (NoSuchMethodException exception) {
            return false;
        }
    }

    private static String abbreviate(String value) {
        return (value.length() <= MAX_REPORTED_VALUE_LENGTH) ? value
              : (value.substring(0, MAX_REPORTED_VALUE_LENGTH) + "...");
    }
}
