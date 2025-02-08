package megamek.utilities;

import megamek.client.bot.princess.CardinalEdge;
import megamek.common.*;
import megamek.common.util.BoardUtilities;
import megamek.utilities.ai.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

public class PrincessFineTuning {

    private static class DatasetParser {
        private final File file;
        // attackAction is not being considered right now
        private final List<ActionAndState> actionAndStates = new ArrayList<>();
        private final MekSummaryCache mekSummaryCache = MekSummaryCache.getInstance(true);
        private final Map<Integer, Entity> entities = new HashMap<>();

        public DatasetParser(File file) {
            this.file = file;
        }

        public List<ActionAndState> parse() {


            if (actionAndStates.isEmpty()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("PLAYER_ID\tENTITY_ID\tCHASSIS\tMODEL\tFACING\tFROM_X\tFROM_Y\tTO_X\tTO_Y\tHEXES_MOVED\tDISTANCE\tMP_USED\tMAX_MP\tMP_P\tHEAT_P\tARMOR_P\tINTERNAL_P\tJUMPING\tPRONE\tLEGAL\tSTEPS")) {
                            // Parse action line
                            String actionLine = reader.readLine();
                            if (actionLine == null) break;
                            UnitAction action = parseActionLine(actionLine);

                            // Parse state block
                            line = reader.readLine(); // State header
                            if (line == null // !line.startsWith("ROUND\tPHASE\tPLAYER_ID\tENTITY_ID\tCHASSIS\tMODEL\tTYPE\tROLE\tX\tY\tFACING\tMP\tHEAT\tPRONE\tAIRBORNE\tOFF_BOARD\tCRIPPLED\tDESTROYED\tARMOR_P\tINTERNAL_P\tDONE")
                            || !line.startsWith("ROUND\tPHASE\tTEAM_ID\tPLAYER_ID\tENTITY_ID\tCHASSIS\tMODEL\tTYPE\tROLE\tX\tY\tFACING\tMP\tHEAT\tPRONE\tAIRBORNE\tOFF_BOARD\tCRIPPLED\tDESTROYED\tARMOR_P\tINTERNAL_P\tDONE")) {
                                throw new RuntimeException("Invalid state header after action");
                            }

                            List<UnitState> states = new ArrayList<>();
                            Integer currentRound = null;
                            while ((line = reader.readLine()) != null && (!line.startsWith("PLAYER_ID\tENTITY_ID"))) {
                                if (line.trim().isEmpty()) continue;
                                if (line.startsWith("ROUND")) break;
                                UnitState state = parseStateLine(line);
                                if (currentRound == null) {
                                    currentRound = state.round();
                                } else if (currentRound != state.round()) {
                                    throw new RuntimeException("State block has inconsistent rounds");
                                }
                                states.add(state);
                            }

                            if (currentRound == null) {
                                throw new RuntimeException("State block has no valid states");
                            }
                            actionAndStates.add(new ActionAndState(currentRound, action, states));

                            // If line is an action header, the outer loop will handle it
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Error reading file", e);
                }
            }
            return actionAndStates;
        }

        private UnitAction parseActionLine(String actionLine) {
            String[] parts = actionLine.split("\t");
            int entityId = Integer.parseInt(parts[1]);
            int facing = Integer.parseInt(parts[4]);
            int fromX = Integer.parseInt(parts[5]);
            int fromY = Integer.parseInt(parts[6]);
            int toX = Integer.parseInt(parts[7]);
            int toY = Integer.parseInt(parts[8]);
            int hexesMoved = Integer.parseInt(parts[9]);
            int distance = Integer.parseInt(parts[10]);
            int mpUsed = Integer.parseInt(parts[11]);
            int maxMp = Integer.parseInt(parts[12]);
            double mpP = Double.parseDouble(parts[13]);
            double heatP = Double.parseDouble(parts[14]);
            double armorP = Double.parseDouble(parts[15]);
            double internalP = Double.parseDouble(parts[16]);
            boolean jumping = parts[17].equals("1");
            boolean prone = parts[18].equals("1");
            boolean legal = parts[19].equals("1");
            // Need to check if the unit in the previous turn had moved or not

            return new UnitAction(entityId, facing, fromX, fromY, toX, toY, hexesMoved, distance, mpUsed, maxMp, mpP, heatP, armorP,
                internalP, jumping, prone, legal);
        }

        private UnitState parseStateLine(String stateLine) {
            String[] parts = stateLine.split("\t");
            int round = Integer.parseInt(parts[0]);
            int teamId = Integer.parseInt(parts[2]);
            int playerId = Integer.parseInt(parts[3]);
            int entityId = Integer.parseInt(parts[4]);
            String chassis = parts[5];
            String model = parts[6];
            String type = parts[7];
            UnitRole role = UnitRole.valueOf(parts[8]);
            int x = Integer.parseInt(parts[9]);
            int y = Integer.parseInt(parts[10]);
            int facing = Integer.parseInt(parts[11]);
            double mp = Double.parseDouble(parts[12]);
            double heat = Double.parseDouble(parts[13]);
            boolean prone = parts[14].equals("1");
            boolean airborne = parts[15].equals("1");
            boolean offBoard = parts[16].equals("1");
            boolean crippled = parts[17].equals("1");
            boolean destroyed = parts[18].equals("1");
            double armorP = Double.parseDouble(parts[19]);
            double internalP = Double.parseDouble(parts[20]);
            boolean done = parts[21].equals("1");
            int maxRange = 0;
            int turnsWithoutMovement = 0;
            int totalDamage = 0;
            Entity entity = null;
            if (!type.equals("MekWarrior") && !type.equals("EjectedCrew")) {
                entity = entities.computeIfAbsent(entityId, i -> MekSummary.loadEntity(chassis + " " + model));
            }

            if (entity != null) {
                maxRange = entity.getMaxWeaponRange();
                totalDamage = Compute.computeTotalDamage(entity.getWeaponList());
                entity.setInitialBV(entity.calculateBattleValue(true, true));
            }

            return new UnitState(entityId, teamId, round, playerId, chassis, model, type, role, x, y, facing, mp, heat, prone, airborne, offBoard,
                crippled, destroyed, armorP, internalP, done,maxRange, totalDamage, turnsWithoutMovement, entity);
        }
    }

    public static class ParameterOptimizer {
        private final List<ActionAndState> actionAndStates;
        private final CostFunction costFunction;
        private final Map<Integer, UnitState> unitStateMap = new HashMap<>();
        private final Map<Integer, UnitState> nextUnitStateMap = new HashMap<>();
        private static final int MAX_ITERATIONS = 100_000;
        private static final double TOLERANCE = 1e-6;
        private static final double BASE_LR = 1e-3;
        private static final double MAX_LR = 1e-1;
        private static final int CYCLE_LENGTH = 2000;

        private double learningRate;
        private static final int PATIENCE = 50;
        private BehaviorParameters velocity = new BehaviorParameters(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0);
        private double momentum = 0.9;

        public ParameterOptimizer(List<ActionAndState> actionAndStates,
                                          CostFunction costFunction,
                                          double learningRate) {
            this.actionAndStates = actionAndStates;
            this.costFunction = costFunction;
            this.learningRate = learningRate;
        }

        public BehaviorParameters optimize() {
            // initialize with random values
            BehaviorParameters params = new BehaviorParameters(
                Math.random(), Math.random(), Math.random(), Math.random(), Math.random(),
                Math.random(), Math.random(), Math.random(), Math.random(), Math.random(),
                Math.random(), Math.random(), Math.random(), Math.random(), Math.random(),
                Math.random(), Math.random(), Math.random(), Math.random(), Math.random(),
                Math.random(), Math.random()*1.5, Math.random()*1.5, Math.random()*1.5, Math.random()*1.5,
                Math.random()*1.5, Math.random()*1.5, Math.random()*1.5, Math.random()*1.5
                );

            double bestLoss = Double.MAX_VALUE;
            int noImprovementCount = 0;
            List<ActionAndState> preparedActionAndState = new ArrayList<>();
            for (int i = 0; i < actionAndStates.size(); i++) {
                if (i + 1 >= actionAndStates.size()) break;
                preparedActionAndState.add(actionAndStates.get(i));
                preparedActionAndState.add(actionAndStates.get(i + 1));
            }
            for (int i = 0; i < MAX_ITERATIONS; i++) {
                learningRate = BASE_LR + (MAX_LR-BASE_LR)*(1 + Math.cos(2*Math.PI*i/CYCLE_LENGTH))/2;
                // Add exploration noise
                if(i % 100 == 0) {
                    params = addExplorationNoise(params);
                }
                BehaviorParameters gradient = computeGradient(params);
                velocity = velocity.multiply(momentum).add(gradient.multiply(learningRate));
                params = params.subtract(velocity);

                // Clamp parameters
                params = params.clamp(0.0, 1.0);

                // Adaptive restart
                double currentLoss = computeLoss(params, preparedActionAndState);
                if(currentLoss < bestLoss) {
                    bestLoss = currentLoss;
                } else {
                    if(++noImprovementCount > PATIENCE) {
                        learningRate *= 0.5;
                        params = addExplorationNoise(params);
                        noImprovementCount = 0;
                    }
                }

                // Print progress
                if(i % 100 == 0) {
                    System.out.printf("\tIter %6d | Loss: %.2e | LR: %.2e\n",
                        i, bestLoss, learningRate);
                }
                if (bestLoss < TOLERANCE) {
                    break;
                }
            }

            System.out.printf("Final loss: %.16f%n", bestLoss);
            return params;
        }

        private BehaviorParameters clipGradient(BehaviorParameters grad) {
            double maxGrad = 1.0;
            return grad.multiply(maxGrad/grad.maxAbs());
        }

        private BehaviorParameters addExplorationNoise(BehaviorParameters params) {
            Random rand = new Random();
            return params.add(new BehaviorParameters(
                rand.nextGaussian()*0.01, rand.nextGaussian()*0.01,
                rand.nextGaussian()*0.01, rand.nextGaussian()*0.01,
                rand.nextGaussian()*0.01, rand.nextGaussian()*0.01,
                rand.nextGaussian()*0.01, rand.nextGaussian()*0.01,
                rand.nextGaussian()*0.01, rand.nextGaussian()*0.01,
                rand.nextGaussian()*0.01, rand.nextGaussian()*0.01,
                rand.nextGaussian()*0.01, rand.nextGaussian()*0.01,
                rand.nextGaussian()*0.01, rand.nextGaussian()*0.01,
                rand.nextGaussian()*0.01, rand.nextGaussian()*0.01,
                rand.nextGaussian()*0.01, rand.nextGaussian()*0.01,
                rand.nextGaussian()*0.01, rand.nextGaussian()*0.01,
                rand.nextGaussian()*0.01, rand.nextGaussian()*0.01,
                rand.nextGaussian()*0.01, rand.nextGaussian()*0.01,
                rand.nextGaussian()*0.01, rand.nextGaussian()*0.01,
                rand.nextGaussian()*0.01
            ));
        }

        private double computeLoss(BehaviorParameters params, List<ActionAndState> batch) {
            List<Double> results = new ArrayList<>();

            var iterator = batch.iterator();
            var futureIterator = batch.iterator();

            var availableIds = List.of(8, 9, 10, 11, 12, 13);
            int randomIndex = new Random().nextInt(availableIds.size());
            while (iterator.hasNext() && futureIterator.hasNext()) {
                futureIterator.next();
                if (!futureIterator.hasNext()) break;
                ActionAndState futureActionAndState = futureIterator.next();
                ActionAndState actionAndState = iterator.next();
                iterator.next();
                if (actionAndState.unitAction().id() != randomIndex) continue;
                unitStateMap.clear();
                unitStateMap.putAll(actionAndState.boardUnitState().stream()
                    .collect(Collectors.toMap(UnitState::id, Function.identity())));
                nextUnitStateMap.clear();
                nextUnitStateMap.putAll(futureActionAndState.boardUnitState().stream()
                    .collect(Collectors.toMap(UnitState::id, Function.identity())));
                results.add(costFunction.resolve(
                    actionAndState.unitAction(), unitStateMap, nextUnitStateMap, params
                ));
            }
            double mse = results.stream().mapToDouble(v -> v * v).average().orElse(0.0);
            double reg = params.stream().mapToDouble(v -> v * v).sum() * 1e-4;
            // Mean Squared Error loss
            return mse + reg;
        }

        private List<ActionAndState> sampleBatch(List<ActionAndState> actionAndStates, int batchSize) {
            List<ActionAndState> batch = new ArrayList<>();
            Random rand = new Random();
            for (int i = 0; i < batchSize; i++) {
                int idx = rand.nextInt(actionAndStates.size()-1);
                batch.add(actionAndStates.get(idx));
                batch.add(actionAndStates.get(idx + 1));
            }
            return batch;
        }

        private BehaviorParameters computeGradient(BehaviorParameters params) {
            BehaviorParameters gradient = new BehaviorParameters(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            List<ActionAndState> batch = sampleBatch(actionAndStates, 50);
            double baseLoss = computeLoss(params, batch);
            // Numerical differentiation (perturb each parameter slightly)
            for (int i = 0; i < params.size(); i++) {
                double epsilon = adaptiveEpsilon(params.get(i));
                BehaviorParameters perturbed = params.perturb(i, epsilon);
                double perturbedLoss = computeLoss(perturbed, batch);
                double derivative = (perturbedLoss - baseLoss) / epsilon;
                gradient = gradient.set(i, derivative);
            }
            return clipGradient(gradient);
        }
        private double adaptiveEpsilon(double paramValue) {
            return Math.max(1e-8, 1e-5*Math.abs(paramValue));
        }

    }

    // Example usage
    public static void main(String[] args) {
        // Initialize with your dataset and cost function
        List<ActionAndState> data =
            new DatasetParser(new File("/Users/coppio/Projects/megamek/megamek/logs/game_actions_test_01.tsv"))
                .parse();
        var mapSettings = MapSettings.getInstance();
        mapSettings.setBoardSize(45, 45);
        mapSettings.setMapSize(1, 1);
        Board board = BoardUtilities.generateRandom(mapSettings);

        CostFunction costFunction = new UtilityPathRankerCostFunction(CardinalEdge.NORTH,
            new UtilityPathRankerCostFunction.CostFunctionSwarmContext(),
            board);
        ExtendedCostFunction extendedCostFunction = new ExtendedCostFunction(costFunction);
        ParameterOptimizer optimizer =
            new ParameterOptimizer(data, extendedCostFunction, 0.01);

        // Then run full optimization with best LR
        BehaviorParameters optimalParams = optimizer.optimize();
        System.out.println("Optimal Parameters: " + optimalParams);
    }
}
