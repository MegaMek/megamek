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
package megamek.client.ui.panels.phaseDisplay.lobby;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Nullable;
import megamek.common.force.Force;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Plans how the local player's unassigned C3 Master and C3 Slave units fill the four configuration templates of the
 * C3 Configuration Diagram (CR p.199), keeping the player's lobby lances whole.
 *
 * <p>Each colored block of the diagram is one lance: the head unit's own lance, lances run by a lance master, and
 * (Configurations 3 and 4) lances of slaves the head runs itself. A lobby force that holds units directly is treated
 * as one lance, and all of its C3 units go into a single block or none - a lance is never split across blocks or
 * networks. A lance that fits no block is left out and reported with the reason. Units in no force, and units of a
 * force too big to be a lance, fill the blocks no lance took, picked the way the template would be filled by hand:
 * the head prefers an exact computer-count fit and lance master slots take the smallest masters, so multi-computer
 * units stay free for company roles.</p>
 */
final class C3FormationPlanner {
    private static final MMLogger LOGGER = MMLogger.create(C3FormationPlanner.class);

    /** Slaves in one lance block: a master runs three (CR p.198). */
    private static final int LANCE_SLAVES = Entity.MAX_C3M_SUBORDINATES;

    /**
     * A lobby force holding more C3 units than this directly is not a lance (a Level II of six is the largest
     * lance-sized formation) - its units are planned as if they were in no force.
     */
    private static final int MAX_LANCE_SIZE = 6;

    private final Game game;
    private final int localPlayerId;

    C3FormationPlanner(Game game, int localPlayerId) {
        this.game = game;
        this.localPlayerId = localPlayerId;
    }

    /**
     * The slot layout of one configuration (CR p.199 C3 Configuration Diagram): slaves the head runs itself, and the
     * slave count of each lance under a lance master. Canonical sizes are 12, 12, 11 and 10 units.
     *
     * @param configurationNumber the configuration, 1 to 4
     * @param directSlaveCount    slaves the head unit runs itself, in blocks of three
     * @param lanceSlaveCounts    the slave slots of each lance master's lance
     */
    record ConfigurationShape(int configurationNumber, int directSlaveCount, int[] lanceSlaveCounts) {

        static ConfigurationShape of(int configurationNumber) {
            return switch (configurationNumber) {
                case 2 -> new ConfigurationShape(2, 3, new int[] { 3, 3 });
                case 3 -> new ConfigurationShape(3, 6, new int[] { 3 });
                case 4 -> new ConfigurationShape(4, 9, new int[0]);
                default -> new ConfigurationShape(1, 0, new int[] { 3, 3, 2 });
            };
        }

        int totalSlots() {
            int total = 1 + directSlaveCount + lanceSlaveCounts.length;
            for (int lanceSlaveCount : lanceSlaveCounts) {
                total += lanceSlaveCount;
            }
            return total;
        }

        /**
         * The lance that is the company's own lance in Configuration 1 - the two-slave lance under the head's third
         * lance master - or -1 where the company's own lance is the head's own slaves.
         */
        int companyLanceIndex() {
            return ((directSlaveCount == 0) && (lanceSlaveCounts.length > 0)) ? (lanceSlaveCounts.length - 1) : -1;
        }
    }

    /** Why a lance stayed out of a template or force plan. */
    enum LeftOutReason {
        /** More slaves than one master can run. */
        TOO_MANY_SLAVES,
        /** More masters than the lance's block can hold. */
        TOO_MANY_MASTERS,
        /** Slaves without a master, where no block lets the head run them. */
        NO_MASTER,
        /** A lance with its own master, in a configuration with no lance master slot (Configuration 4). */
        NO_LANCE_MASTER_PLACE,
        /** The lance fits a kind of block, but every block of that kind is already taken. */
        NO_FREE_BLOCK
    }

    /**
     * A lance that was not placed.
     *
     * @param lanceName   the lobby force's name
     * @param reason      why it stayed out
     * @param masterCount its C3 Master units
     * @param slaveCount  its C3 Slave units
     */
    record LeftOutLance(String lanceName, LeftOutReason reason, int masterCount, int slaveCount) {
    }

    /**
     * One lobby force's unassigned C3 units, treated as a lance.
     *
     * @param forceId    the lobby force
     * @param name       the force's name, for reports
     * @param companyId  the parent force, so lances of one company are preferred together; {@link Force#NO_FORCE}
     *                   for a top-level force
     * @param masters    C3 Master units, fewest computers first
     * @param slaves     C3 Slave units, in id order
     */
    private record Lance(int forceId, String name, int companyId, List<Entity> masters, List<Entity> slaves) {
        int size() {
            return masters.size() + slaves.size();
        }
    }

    /** The units still to plan: whole lances, and units in no lance. Lists are mutable. */
    private record Pool(List<Lance> lances, List<Entity> looseMasters, List<Entity> looseSlaves) {
        boolean isEmpty() {
            boolean hasLooseUnits = !looseMasters.isEmpty() || !looseSlaves.isEmpty();
            return lances.isEmpty() && !hasLooseUnits;
        }
    }

    /**
     * Units planned into one configuration template, slot by slot in the template's layout; empty slots are
     * {@code null}.
     *
     * @param configurationNumber the configuration, 1 to 4
     * @param head                the company (head) unit, or {@code null} while that slot is empty
     * @param directSlaves        the slaves the head runs itself
     * @param lanceMasters        one per lance master slot
     * @param lanceSlaves         the slave slots of each lance master's lance
     * @param leftOut             lances that stayed out, with the reason
     */
    record TemplateAssignment(int configurationNumber, @Nullable Entity head, Entity[] directSlaves,
          Entity[] lanceMasters, Entity[][] lanceSlaves, List<LeftOutLance> leftOut) {

        int filledCount() {
            return assignedUnits().size();
        }

        int totalSlots() {
            return ConfigurationShape.of(configurationNumber).totalSlots();
        }

        boolean isComplete() {
            return filledCount() == totalSlots();
        }

        List<Entity> assignedUnits() {
            List<Entity> units = new ArrayList<>();
            if (head != null) {
                units.add(head);
            }
            addAssigned(units, directSlaves);
            addAssigned(units, lanceMasters);
            for (Entity[] lance : lanceSlaves) {
                addAssigned(units, lance);
            }
            return units;
        }

        private static void addAssigned(List<Entity> units, Entity[] slots) {
            for (Entity slot : slots) {
                if (slot != null) {
                    units.add(slot);
                }
            }
        }
    }

    /**
     * One planned remainder lance: a master with slaves, or - all-master - a master heading other masters.
     *
     * @param master     the lance's master
     * @param members    its slaves, or the masters in slave roles
     * @param allMasters {@code true} for an all-master lance
     */
    record PlannedLance(Entity master, List<Entity> members, boolean allMasters) {
    }

    /**
     * How the whole unassigned force partitions into networks.
     *
     * @param companies     complete configurations, most consolidated first
     * @param lances        remainder lances
     * @param leftoverUnits units that fit no network
     * @param leftOut       lances that stayed out, with the reason
     */
    record ForcePlan(List<TemplateAssignment> companies, List<PlannedLance> lances, int leftoverUnits,
          List<LeftOutLance> leftOut) {
        boolean isEmpty() {
            return companies.isEmpty() && lances.isEmpty();
        }
    }

    /** The kinds of block a template is made of; each takes one whole lance. */
    private enum BlockKind { HEAD, LANCE_MASTER, HEAD_RUN }

    /**
     * One lance-sized block of a template.
     *
     * @param kind          what the block is
     * @param index         the lance master slot (LANCE_MASTER) or the head's slave block (HEAD_RUN); 0 for HEAD
     * @param slaveCapacity slave slots in the block
     */
    private record Block(BlockKind kind, int index, int slaveCapacity) {
    }

    /**
     * Plans one configuration template from the unassigned force. Every lance that can head the configuration is
     * tried as the head's lance, and the one filling the most slots wins (on a tie, an exact computer-count head,
     * then the lowest force id). Lances of the head's company are placed before other lances.
     *
     * @param configurationNumber the configuration, 1 to 4
     *
     * @return the planned slots and the lances left out
     */
    TemplateAssignment planTemplate(int configurationNumber) {
        TemplateAssignment assignment = planTemplate(ConfigurationShape.of(configurationNumber), collectPool());
        LOGGER.debug("[C3Manager] Configuration {} auto-assign filled {} of {} slots; left out: {}",
              configurationNumber, assignment.filledCount(), assignment.totalSlots(), assignment.leftOut());
        return assignment;
    }

    /**
     * Partitions the whole unassigned force into networks: complete configurations first, most consolidated
     * (4 down to 1), then each remaining lance as its own network, then units in no lance as remainder lances and
     * all-master lances.
     */
    ForcePlan planForce() {
        Pool pool = collectPool();
        List<TemplateAssignment> companies = new ArrayList<>();
        boolean progress = true;
        while (progress && !pool.isEmpty()) {
            progress = false;
            for (int configurationNumber = 4; (configurationNumber >= 1) && !progress; configurationNumber--) {
                TemplateAssignment assignment = planTemplate(ConfigurationShape.of(configurationNumber), pool);
                if (assignment.isComplete()) {
                    companies.add(withoutLeftOut(assignment));
                    removeFromPool(pool, assignment.assignedUnits());
                    progress = true;
                }
            }
        }

        List<PlannedLance> plannedLances = new ArrayList<>();
        List<LeftOutLance> leftOut = new ArrayList<>();
        int leftoverUnits = 0;
        for (Lance lance : pool.lances()) {
            PlannedLance plannedLance = planRemainderLance(lance);
            if (plannedLance != null) {
                plannedLances.add(plannedLance);
                continue;
            }
            leftoverUnits += lance.size();
            LeftOutReason reason = remainderLanceProblem(lance);
            if (reason != null) {
                leftOut.add(new LeftOutLance(lance.name(), reason, lance.masters().size(), lance.slaves().size()));
            }
        }

        List<Entity> masters = new ArrayList<>(pool.looseMasters());
        List<Entity> slaves = new ArrayList<>(pool.looseSlaves());
        sortByComputerCount(masters);
        while (!masters.isEmpty() && !slaves.isEmpty()) {
            Entity lanceMaster = masters.removeFirst();
            plannedLances.add(new PlannedLance(lanceMaster,
                  takeFirst(slaves, Math.min(LANCE_SLAVES, slaves.size())), false));
        }
        while (masters.size() >= 2) {
            Entity lanceMaster = masters.removeFirst();
            plannedLances.add(new PlannedLance(lanceMaster,
                  takeFirst(masters, Math.min(LANCE_SLAVES, masters.size())), true));
        }
        leftoverUnits += masters.size() + slaves.size();
        LOGGER.debug("[C3Manager] Force plan: {} configurations, {} lances, {} units left over; left out: {}",
              companies.size(), plannedLances.size(), leftoverUnits, leftOut);
        return new ForcePlan(companies, plannedLances, leftoverUnits, leftOut);
    }

    /** A lance that is a network on its own: one master with slaves, or two to four masters. */
    @Nullable
    private PlannedLance planRemainderLance(Lance lance) {
        int masterCount = lance.masters().size();
        int slaveCount = lance.slaves().size();
        boolean slavesFitOneMaster = (slaveCount >= 1) && (slaveCount <= LANCE_SLAVES);
        if ((masterCount == 1) && slavesFitOneMaster) {
            return new PlannedLance(lance.masters().getFirst(), new ArrayList<>(lance.slaves()), false);
        }
        boolean mastersFitOneLance = (masterCount >= 2) && (masterCount <= (LANCE_SLAVES + 1));
        if ((slaveCount == 0) && mastersFitOneLance) {
            List<Entity> members = new ArrayList<>(lance.masters());
            Entity master = members.removeFirst();
            return new PlannedLance(master, members, true);
        }
        return null;
    }

    /** Why a lance cannot be a network on its own; {@code null} for a lone master, which simply has no partners. */
    @Nullable
    private LeftOutReason remainderLanceProblem(Lance lance) {
        int masterCount = lance.masters().size();
        int slaveCount = lance.slaves().size();
        if (slaveCount > LANCE_SLAVES) {
            return LeftOutReason.TOO_MANY_SLAVES;
        }
        if (masterCount == 0) {
            return LeftOutReason.NO_MASTER;
        }
        if (masterCount >= 2) {
            return LeftOutReason.TOO_MANY_MASTERS;
        }
        return null;
    }

    private TemplateAssignment planTemplate(ConfigurationShape shape, Pool pool) {
        List<Lance> headCandidates = new ArrayList<>();
        for (Lance lance : pool.lances()) {
            if (fitsHead(lance, shape)) {
                headCandidates.add(lance);
            }
        }
        if (headCandidates.isEmpty()) {
            return fillTemplate(shape, pool, null);
        }
        TemplateAssignment best = null;
        boolean bestHasExactHead = false;
        for (Lance headLance : headCandidates) {
            TemplateAssignment candidate = fillTemplate(shape, pool, headLance);
            boolean hasExactHead = (candidate.head() != null)
                  && (candidate.head().getOperableC3MCount() == shape.configurationNumber());
            boolean fillsMore = (best != null) && (candidate.filledCount() > best.filledCount());
            boolean fillsTheSame = (best != null) && (candidate.filledCount() == best.filledCount());
            boolean hasBetterHead = hasExactHead && !bestHasExactHead;
            boolean isBetter = (best == null) || fillsMore || (fillsTheSame && hasBetterHead);
            if (isBetter) {
                best = candidate;
                bestHasExactHead = hasExactHead;
            }
        }
        return best;
    }

    /**
     * Fills one template: the head's lance (if any) into the head block, other lances into the blocks they fit -
     * the head's company first - and finally units in no lance into the blocks still empty.
     */
    private TemplateAssignment fillTemplate(ConfigurationShape shape, Pool pool, @Nullable Lance headLance) {
        Entity[] head = new Entity[1];
        Entity[] directSlaves = new Entity[shape.directSlaveCount()];
        Entity[] lanceMasters = new Entity[shape.lanceSlaveCounts().length];
        Entity[][] lanceSlaves = new Entity[shape.lanceSlaveCounts().length][];
        for (int lanceIndex = 0; lanceIndex < lanceSlaves.length; lanceIndex++) {
            lanceSlaves[lanceIndex] = new Entity[shape.lanceSlaveCounts()[lanceIndex]];
        }
        Slots slots = new Slots(shape, head, directSlaves, lanceMasters, lanceSlaves);
        List<Block> freeBlocks = blocksOf(shape);
        List<LeftOutLance> leftOut = new ArrayList<>();

        int headCompanyId = Force.NO_FORCE;
        Set<Integer> placedCompanies = new HashSet<>();
        if (headLance != null) {
            Block headBlock = freeBlocks.removeFirst();
            placeHeadLance(headLance, headBlock, slots);
            headCompanyId = headLance.companyId();
            placedCompanies.add(headCompanyId);
        }

        List<Lance> unplaced = new ArrayList<>();
        for (Lance lance : lancesInPlacementOrder(pool.lances(), headLance, headCompanyId)) {
            Block block = findFreeBlock(lance, freeBlocks);
            if (block == null) {
                unplaced.add(lance);
                continue;
            }
            freeBlocks.remove(block);
            placeLance(lance, block, slots);
            placedCompanies.add(lance.companyId());
        }
        // A lance that fits but found no room is only news within a company the template drew from
        for (Lance lance : unplaced) {
            LeftOutReason reason = templateProblem(lance, shape);
            boolean isInPlacedCompany = (lance.companyId() != Force.NO_FORCE)
                  && placedCompanies.contains(lance.companyId());
            if ((reason != LeftOutReason.NO_FREE_BLOCK) || isInPlacedCompany) {
                leftOut.add(new LeftOutLance(lance.name(), reason, lance.masters().size(), lance.slaves().size()));
            }
        }

        fillWithLooseUnits(freeBlocks, slots, pool);
        return new TemplateAssignment(shape.configurationNumber(), head[0], directSlaves, lanceMasters, lanceSlaves,
              leftOut);
    }

    /** The template's slot arrays, written by block. */
    private record Slots(ConfigurationShape shape, Entity[] head, Entity[] directSlaves, Entity[] lanceMasters,
          Entity[][] lanceSlaves) {

        /** Puts a slave in the block's slave slot {@code slotIndex}. */
        void setSlave(Block block, int slotIndex, Entity slave) {
            switch (block.kind()) {
                case HEAD -> {
                    if (shape.companyLanceIndex() >= 0) {
                        lanceSlaves[shape.companyLanceIndex()][slotIndex] = slave;
                    } else {
                        directSlaves[slotIndex] = slave;
                    }
                }
                case HEAD_RUN -> directSlaves[(block.index() * LANCE_SLAVES) + slotIndex] = slave;
                case LANCE_MASTER -> lanceSlaves[block.index()][slotIndex] = slave;
            }
        }

        /** The master slot of a block that holds a second master: a lance master, or Configuration 1's head lance. */
        void setMaster(Block block, Entity master) {
            int lanceIndex = (block.kind() == BlockKind.HEAD) ? shape.companyLanceIndex() : block.index();
            lanceMasters[lanceIndex] = master;
        }

        boolean hasMaster(Block block) {
            return switch (block.kind()) {
                case HEAD -> (shape.companyLanceIndex() < 0) || (lanceMasters[shape.companyLanceIndex()] != null);
                case LANCE_MASTER -> lanceMasters[block.index()] != null;
                case HEAD_RUN -> true;
            };
        }
    }

    /** The template as lance blocks: the head's lance first, then lance master lances, then the head's lances. */
    private List<Block> blocksOf(ConfigurationShape shape) {
        List<Block> blocks = new ArrayList<>();
        int companyLanceIndex = shape.companyLanceIndex();
        int headSlaveCapacity = (companyLanceIndex >= 0) ? shape.lanceSlaveCounts()[companyLanceIndex]
              : Math.min(LANCE_SLAVES, shape.directSlaveCount());
        blocks.add(new Block(BlockKind.HEAD, 0, headSlaveCapacity));
        for (int lanceIndex = 0; lanceIndex < shape.lanceSlaveCounts().length; lanceIndex++) {
            if (lanceIndex != companyLanceIndex) {
                blocks.add(new Block(BlockKind.LANCE_MASTER, lanceIndex, shape.lanceSlaveCounts()[lanceIndex]));
            }
        }
        int headLanceCount = (shape.directSlaveCount() + LANCE_SLAVES - 1) / LANCE_SLAVES;
        for (int headLanceIndex = 1; headLanceIndex < headLanceCount; headLanceIndex++) {
            int capacity = Math.min(LANCE_SLAVES, shape.directSlaveCount() - (headLanceIndex * LANCE_SLAVES));
            blocks.add(new Block(BlockKind.HEAD_RUN, headLanceIndex, capacity));
        }
        return blocks;
    }

    /**
     * True when the lance can be the head's own lance. Configurations 2-4: one master with enough computers and up
     * to three slaves. Configuration 1: the head plus at most one lance master and two slaves - the slaves need that
     * lance master, since a dedicated company master runs no slaves.
     */
    private boolean fitsHead(Lance lance, ConfigurationShape shape) {
        int masterCount = lance.masters().size();
        int slaveCount = lance.slaves().size();
        if (shape.companyLanceIndex() >= 0) {
            boolean hasRoomForMasters = (masterCount >= 1) && (masterCount <= 2);
            boolean hasRoomForSlaves = slaveCount <= shape.lanceSlaveCounts()[shape.companyLanceIndex()];
            boolean slavesHaveAMaster = (slaveCount == 0) || (masterCount == 2);
            return hasRoomForMasters && hasRoomForSlaves && slavesHaveAMaster;
        }
        if (masterCount != 1) {
            return false;
        }
        boolean hasEnoughComputers = lance.masters().getFirst().getOperableC3MCount() >= shape.configurationNumber();
        boolean hasRoomForSlaves = slaveCount <= Math.min(LANCE_SLAVES, shape.directSlaveCount());
        return hasEnoughComputers && hasRoomForSlaves;
    }

    private boolean fits(Lance lance, Block block) {
        int masterCount = lance.masters().size();
        int slaveCount = lance.slaves().size();
        boolean slavesFitBlock = slaveCount <= block.slaveCapacity();
        boolean hasSlaves = slaveCount >= 1;
        return switch (block.kind()) {
            case HEAD -> false;
            case LANCE_MASTER -> (masterCount == 1) && slavesFitBlock;
            case HEAD_RUN -> (masterCount == 0) && hasSlaves && slavesFitBlock;
        };
    }

    @Nullable
    private Block findFreeBlock(Lance lance, List<Block> freeBlocks) {
        for (Block block : freeBlocks) {
            if (fits(lance, block)) {
                return block;
            }
        }
        return null;
    }

    /** Why the lance has no place in this configuration, assuming every block were free. */
    private LeftOutReason templateProblem(Lance lance, ConfigurationShape shape) {
        for (Block block : blocksOf(shape)) {
            boolean fitsThisBlock = (block.kind() == BlockKind.HEAD) ? fitsHead(lance, shape) : fits(lance, block);
            if (fitsThisBlock) {
                return LeftOutReason.NO_FREE_BLOCK;
            }
        }
        int masterCount = lance.masters().size();
        if (lance.slaves().size() > LANCE_SLAVES) {
            return LeftOutReason.TOO_MANY_SLAVES;
        }
        if (masterCount >= 2) {
            return LeftOutReason.TOO_MANY_MASTERS;
        }
        if (masterCount == 0) {
            return LeftOutReason.NO_MASTER;
        }
        return LeftOutReason.NO_LANCE_MASTER_PLACE;
    }

    private void placeHeadLance(Lance lance, Block headBlock, Slots slots) {
        List<Entity> masters = new ArrayList<>(lance.masters());
        Entity headUnit = pickCompanyCandidate(masters, slots.shape().configurationNumber());
        masters.remove(headUnit);
        slots.head()[0] = headUnit;
        if (!masters.isEmpty()) {
            slots.setMaster(headBlock, masters.getFirst());
        }
        for (int slotIndex = 0; slotIndex < lance.slaves().size(); slotIndex++) {
            slots.setSlave(headBlock, slotIndex, lance.slaves().get(slotIndex));
        }
    }

    private void placeLance(Lance lance, Block block, Slots slots) {
        if (!lance.masters().isEmpty()) {
            slots.setMaster(block, lance.masters().getFirst());
        }
        for (int slotIndex = 0; slotIndex < lance.slaves().size(); slotIndex++) {
            slots.setSlave(block, slotIndex, lance.slaves().get(slotIndex));
        }
    }

    /** The head's company first, then the other lances; both in force id order. The head's lance is left out. */
    private List<Lance> lancesInPlacementOrder(List<Lance> lances, @Nullable Lance headLance, int headCompanyId) {
        List<Lance> sameCompany = new ArrayList<>();
        List<Lance> others = new ArrayList<>();
        for (Lance lance : lances) {
            if (lance == headLance) {
                continue;
            }
            if ((headCompanyId != Force.NO_FORCE) && (lance.companyId() == headCompanyId)) {
                sameCompany.add(lance);
            } else {
                others.add(lance);
            }
        }
        sameCompany.addAll(others);
        return sameCompany;
    }

    /**
     * Fills the blocks no lance took with units in no lance: the head (exact computer-count fit preferred), the
     * smallest masters for master slots, then slaves - the head's own slaves first, then its other lances, then
     * each lance master's lance. A block's slaves need its master, so a lance master slot left empty gets none.
     */
    private void fillWithLooseUnits(List<Block> freeBlocks, Slots slots, Pool pool) {
        List<Entity> masters = new ArrayList<>(pool.looseMasters());
        List<Entity> slaves = new ArrayList<>(pool.looseSlaves());
        for (Block block : freeBlocks) {
            if (block.kind() == BlockKind.HEAD) {
                Entity headUnit = pickCompanyCandidate(masters, slots.shape().configurationNumber());
                if (headUnit == null) {
                    continue;
                }
                masters.remove(headUnit);
                slots.head()[0] = headUnit;
                sortByComputerCount(masters);
                if ((slots.shape().companyLanceIndex() >= 0) && !masters.isEmpty()) {
                    slots.setMaster(block, masters.removeFirst());
                }
            }
        }
        sortByComputerCount(masters);
        for (Block block : freeBlocks) {
            if ((block.kind() == BlockKind.LANCE_MASTER) && !masters.isEmpty()) {
                slots.setMaster(block, masters.removeFirst());
            }
        }
        List<Block> slaveOrder = new ArrayList<>();
        for (BlockKind kind : List.of(BlockKind.HEAD, BlockKind.HEAD_RUN, BlockKind.LANCE_MASTER)) {
            for (Block block : freeBlocks) {
                if (block.kind() == kind) {
                    slaveOrder.add(block);
                }
            }
        }
        for (Block block : slaveOrder) {
            boolean blockIsHeaded = (block.kind() != BlockKind.HEAD) || (slots.head()[0] != null);
            if (!blockIsHeaded || !slots.hasMaster(block)) {
                continue;
            }
            for (int slotIndex = 0; (slotIndex < block.slaveCapacity()) && !slaves.isEmpty(); slotIndex++) {
                slots.setSlave(block, slotIndex, slaves.removeFirst());
            }
        }
    }

    /** A copy of the assignment without left-out notes, for force plans where templates are only trials. */
    private TemplateAssignment withoutLeftOut(TemplateAssignment assignment) {
        return new TemplateAssignment(assignment.configurationNumber(), assignment.head(), assignment.directSlaves(),
              assignment.lanceMasters(), assignment.lanceSlaves(), List.of());
    }

    private void removeFromPool(Pool pool, List<Entity> usedUnits) {
        Set<Entity> used = new HashSet<>(usedUnits);
        pool.lances().removeIf(lance -> used.containsAll(lance.masters()) && used.containsAll(lance.slaves()));
        pool.looseMasters().removeAll(used);
        pool.looseSlaves().removeAll(used);
    }

    /**
     * Collects the local player's unassigned C3 Master and C3 Slave units, grouped into whole lances by lobby force.
     * Peer systems (C3i, NC3, Nova CEWS) and units already networked are skipped.
     */
    private Pool collectPool() {
        Map<Integer, List<Entity>> unitsByForce = new LinkedHashMap<>();
        List<Entity> looseUnits = new ArrayList<>();
        List<Entity> candidates = new ArrayList<>(game.inGameTWEntities());
        candidates.sort(Comparator.comparingInt(Entity::getId));
        for (Entity entity : candidates) {
            boolean isOwnUnit = entity.getOwnerId() == localPlayerId;
            boolean isHierarchicalC3 = (entity.getOperableC3MCount() > 0) || entity.hasC3S();
            boolean isPeerSystem = entity.hasNhC3();
            boolean isAlreadyNetworked = game.getC3NetworkMembers(entity).size() > 1;
            boolean isPlannable = isOwnUnit && isHierarchicalC3 && !isPeerSystem && !isAlreadyNetworked;
            if (!isPlannable) {
                continue;
            }
            int forceId = game.getForces().getForceId(entity);
            if (forceId == Force.NO_FORCE) {
                looseUnits.add(entity);
            } else {
                unitsByForce.computeIfAbsent(forceId, unused -> new ArrayList<>()).add(entity);
            }
        }

        List<Lance> lances = new ArrayList<>();
        List<Integer> forceIds = new ArrayList<>(unitsByForce.keySet());
        forceIds.sort(Integer::compare);
        for (int forceId : forceIds) {
            List<Entity> units = unitsByForce.get(forceId);
            if (units.size() > MAX_LANCE_SIZE) {
                looseUnits.addAll(units);
                continue;
            }
            Force force = game.getForces().getForce(forceId);
            List<Entity> masters = new ArrayList<>();
            List<Entity> slaves = new ArrayList<>();
            splitByRole(units, masters, slaves);
            lances.add(new Lance(forceId, force.getName(), force.getParentId(), masters, slaves));
        }

        looseUnits.sort(Comparator.comparingInt(Entity::getId));
        List<Entity> looseMasters = new ArrayList<>();
        List<Entity> looseSlaves = new ArrayList<>();
        splitByRole(looseUnits, looseMasters, looseSlaves);
        return new Pool(lances, looseMasters, looseSlaves);
    }

    private void splitByRole(List<Entity> units, List<Entity> masters, List<Entity> slaves) {
        for (Entity unit : units) {
            if (unit.getOperableC3MCount() > 0) {
                masters.add(unit);
            } else {
                slaves.add(unit);
            }
        }
        sortByComputerCount(masters);
    }

    /** The company unit for a configuration: an exact computer-count fit if one exists, else the smallest fit. */
    @Nullable
    private static Entity pickCompanyCandidate(List<Entity> masters, int configurationNumber) {
        Entity bestCandidate = null;
        for (Entity master : masters) {
            int computerCount = master.getOperableC3MCount();
            if (computerCount == configurationNumber) {
                return master;
            }
            if ((computerCount > configurationNumber) && ((bestCandidate == null)
                  || (computerCount < bestCandidate.getOperableC3MCount()))) {
                bestCandidate = master;
            }
        }
        return bestCandidate;
    }

    private static void sortByComputerCount(List<Entity> masters) {
        masters.sort(Comparator.comparingInt(Entity::getOperableC3MCount).thenComparing(Entity::getId));
    }

    /** Removes and returns the first count entries of the list. */
    private static List<Entity> takeFirst(List<Entity> units, int count) {
        List<Entity> taken = new ArrayList<>(units.subList(0, count));
        units.subList(0, count).clear();
        return taken;
    }
}
