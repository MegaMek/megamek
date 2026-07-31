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
package megamek.client.ratgenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;

/**
 * Covers wiring the networks of a generated force.
 *
 * <p>Entities are mocked because what is under test is which units get joined to which network, not
 * how a Mek reports its own equipment - a real Entity would need a full unit file to answer
 * {@code hasC3i()}.</p>
 */
class C3NetworkConfiguratorTest {

    /** Records what the configurator writes, so a mock can be asserted against. */
    private static class NetworkState {
        String netId;
        String masterUuid;
        final List<String> c3iLinks = new ArrayList<>();
    }

    private static Entity networkUnit(int id, boolean hasC3i, boolean hasMaster, boolean hasSlave,
          NetworkState state) {
        Entity entity = mock(Entity.class);
        when(entity.hasC3i()).thenReturn(hasC3i);
        when(entity.hasC3M()).thenReturn(hasMaster);
        when(entity.hasC3MM()).thenReturn(false);
        when(entity.hasC3S()).thenReturn(hasSlave);
        when(entity.getId()).thenReturn(id);
        when(entity.getExternalIdAsString()).thenReturn("unit-" + id);
        when(entity.getC3UUIDAsString()).thenReturn("uuid-" + id);
        when(entity.getShortName()).thenReturn("Unit " + id);
        when(entity.getFreeC3iUUID()).thenReturn(0);

        doAnswerNetId(entity, state);
        return entity;
    }

    private static void doAnswerNetId(Entity entity, NetworkState state) {
        org.mockito.Mockito.doAnswer(invocation -> {
            state.netId = "self";
            return null;
        }).when(entity).setC3NetIdSelf();
        org.mockito.Mockito.doAnswer(invocation -> {
            state.netId = "joined";
            return null;
        }).when(entity).setC3NetId(org.mockito.ArgumentMatchers.any(Entity.class));
        org.mockito.Mockito.doAnswer(invocation -> {
            state.masterUuid = invocation.getArgument(0);
            return null;
        }).when(entity).setC3MasterIsUUIDAsString(org.mockito.ArgumentMatchers.anyString());
        org.mockito.Mockito.doAnswer(invocation -> {
            state.c3iLinks.add(invocation.getArgument(1));
            return null;
        }).when(entity).setC3iNextUUIDAsString(org.mockito.ArgumentMatchers.anyInt(),
              org.mockito.ArgumentMatchers.anyString());
    }

    /**
     * Builds a formation whose children carry the given entities. The descriptors are mocked because
     * {@code ForceDescriptor.entity} is only ever set by loading a unit file, which a unit test has no
     * business doing to exercise network wiring.
     */
    private static ForceDescriptor formationOf(List<Entity> members) {
        ArrayList<ForceDescriptor> children = new ArrayList<>();
        for (Entity member : members) {
            ForceDescriptor child = emptyFormation();
            when(child.getEntity()).thenReturn(member);
            children.add(child);
        }
        ForceDescriptor formation = emptyFormation();
        when(formation.getSubForces()).thenReturn(children);
        return formation;
    }

    /** A formation with no children, no entity and no flags. */
    private static ForceDescriptor emptyFormation() {
        ForceDescriptor formation = mock(ForceDescriptor.class);
        when(formation.getSubForces()).thenReturn(new ArrayList<>());
        when(formation.getAttached()).thenReturn(new ArrayList<>());
        when(formation.getFlags()).thenReturn(new HashSet<>());
        when(formation.parseName()).thenReturn("formation");
        return formation;
    }

    /**
     * A Level II is six units, which is exactly the C3i node limit, so the whole formation forms one
     * network.
     */
    @Test
    void everyC3iUnitInAFormationJoinsOneNetwork() {
        List<NetworkState> states = new ArrayList<>();
        List<Entity> members = new ArrayList<>();
        for (int id = 0; id < 6; id++) {
            NetworkState state = new NetworkState();
            states.add(state);
            members.add(networkUnit(id, true, false, false, state));
        }

        C3NetworkConfigurator.configure(formationOf(members));

        assertEquals("self", states.getFirst().netId, "the first unit anchors the network");
        for (int index = 1; index < states.size(); index++) {
            assertEquals("joined", states.get(index).netId, "unit " + index + " must join it");
            assertEquals(List.of("uuid-0"), states.get(index).c3iLinks,
                  "and must record the anchor's UUID so the link survives a save");
        }
    }

    /** The links are what a campaign rebuilds from, so a net id on its own is not enough. */
    @Test
    void c3iMembershipIsRecordedAsUuidsNotJustNetIds() {
        NetworkState anchor = new NetworkState();
        NetworkState joiner = new NetworkState();
        C3NetworkConfigurator.configure(formationOf(List.of(
              networkUnit(0, true, false, false, anchor),
              networkUnit(1, true, false, false, joiner))));

        assertTrue(anchor.c3iLinks.isEmpty(), "the anchor has nothing to point at");
        assertEquals(List.of("uuid-0"), joiner.c3iLinks);
    }

    @Test
    void aFormationFlaggedForC3GetsAMasterAndSlaves() {
        NetworkState master = new NetworkState();
        NetworkState slaveOne = new NetworkState();
        NetworkState slaveTwo = new NetworkState();
        ForceDescriptor formation = formationOf(List.of(
              networkUnit(0, false, true, false, master),
              networkUnit(1, false, false, true, slaveOne),
              networkUnit(2, false, false, true, slaveTwo)));
        formation.getFlags().add("c3");

        C3NetworkConfigurator.configure(formation);

        assertNull(master.masterUuid, "the master answers to nobody");
        assertEquals("uuid-0", slaveOne.masterUuid);
        assertEquals("uuid-0", slaveTwo.masterUuid);
    }

    /** A C3 master carries three slaves; a fourth is left unattached rather than overfilling it. */
    @Test
    void aC3MasterTakesNoMoreThanThreeSlaves() {
        List<NetworkState> slaveStates = new ArrayList<>();
        List<Entity> members = new ArrayList<>();
        members.add(networkUnit(0, false, true, false, new NetworkState()));
        for (int id = 1; id <= 4; id++) {
            NetworkState state = new NetworkState();
            slaveStates.add(state);
            members.add(networkUnit(id, false, false, true, state));
        }
        ForceDescriptor formation = formationOf(members);
        formation.getFlags().add("c3");

        C3NetworkConfigurator.configure(formation);

        long attached = slaveStates.stream().filter(state -> state.masterUuid != null).count();
        assertEquals(3, attached, "three slaves attach, the fourth does not");
    }

    /** Units the caller excludes are not wired, so a selection the user pared back stays pared back. */
    @Test
    void excludedUnitsAreNotWired() {
        NetworkState included = new NetworkState();
        NetworkState excluded = new NetworkState();
        Entity includedUnit = networkUnit(0, true, false, false, included);
        Entity excludedUnit = networkUnit(1, true, false, false, excluded);

        C3NetworkConfigurator.configure(formationOf(List.of(includedUnit, excludedUnit)),
              entity -> entity == includedUnit);

        assertEquals("self", included.netId);
        assertNull(excluded.netId, "an excluded unit is left alone");
    }

    @Test
    void aFormationWithNothingNetworkableIsLeftAlone() {
        NetworkState plain = new NetworkState();
        C3NetworkConfigurator.configure(formationOf(List.of(
              networkUnit(0, false, false, false, plain))));
        assertNull(plain.netId);
        assertNull(plain.masterUuid);
    }

    @Test
    void aNullForceIsIgnored() {
        C3NetworkConfigurator.configure(null);
    }

    /** Sub-formations are wired too, each as its own network. */
    @Test
    void nestedFormationsAreEachWired() {
        NetworkState first = new NetworkState();
        NetworkState second = new NetworkState();
        ForceDescriptor child = formationOf(List.of(
              networkUnit(0, true, false, false, first),
              networkUnit(1, true, false, false, second)));
        ForceDescriptor parent = new ForceDescriptor();
        parent.addSubForce(child);

        C3NetworkConfigurator.configure(parent);

        assertNotNull(first.netId, "a network nested one level down is still wired");
        assertEquals("joined", second.netId);
    }
}
