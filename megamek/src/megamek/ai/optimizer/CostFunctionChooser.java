package megamek.ai.optimizer;

import megamek.client.bot.princess.CardinalEdge;
import megamek.common.Board;

import java.util.Arrays;

public enum CostFunctionChooser {
    Princess,
    Utility,
    ExtendedUtility;

    public static CostFunctionChooser fromString(String str) {
        return switch (str) {
            case "princess" -> Princess;
            case "utility" -> Utility;
            case "extendedUtility" -> ExtendedUtility;
            default -> throw new IllegalArgumentException("Invalid cost function: " + str);
        };
    }

    public static String validCostFunctions() {
        return String.join(", ", Arrays.stream(values()).map(Enum::name).toArray(String[]::new));
    }

    public CostFunction createCostFunction(CardinalEdge edge, Board board) {
        return switch (this) {
            case Princess -> new BasicPathRankerCostFunction(edge, board);
            case Utility -> new UtilityPathRankerCostFunction(edge, new UtilityPathRankerCostFunction.CostFunctionSwarmContext(), board);
            case ExtendedUtility -> new ExtendedCostFunction(new UtilityPathRankerCostFunction(edge, new UtilityPathRankerCostFunction.CostFunctionSwarmContext(), board));
        };
    }
}
