package tsp;

/**
 * Validates that the distance matrix forms a valid complete (full) graph.
 * A full graph requires: n*(n-1) non-zero, non-negative distances between
 * every pair of distinct nodes, and zero self-loop distances.
 */
public class GraphValidator {

    /**
     * Validates the matrix fully. Throws TSPException with specific error type on failure.
     */
    public static void validate(int[][] matrix, int n) throws TSPException {

        if (n < 2) {
            throw new TSPException(TSPException.ErrorType.INVALID_NODE_COUNT,
                "A TSP requires at least 2 nodes. You entered: " + n);
        }

        if (n > 15) {
            throw new TSPException(TSPException.ErrorType.INVALID_NODE_COUNT,
                "Maximum supported nodes is 15 (Held-Karp memory limit). You entered: " + n);
        }

        int edgesRequired = n * (n - 1);
        int edgesProvided = 0;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {

                // Self-loop must be 0
                if (i == j) {
                    if (matrix[i][j] != 0) {
                        throw new TSPException(TSPException.ErrorType.SELF_LOOP_DISTANCE,
                            "Distance from City " + i + " to itself must be 0, but got: " + matrix[i][j]);
                    }
                    continue;
                }

                // Off-diagonal must be positive
                if (matrix[i][j] < 0) {
                    throw new TSPException(TSPException.ErrorType.NEGATIVE_DISTANCE,
                        "Negative distance between City " + i + " and City " + j + ": " + matrix[i][j]);
                }

                if (matrix[i][j] == 0) {
                    throw new TSPException(TSPException.ErrorType.NOT_FULL_GRAPH,
                        "Missing edge between City " + i + " and City " + j +
                        ". A full (complete) graph requires every pair of distinct cities to have a positive distance.");
                }

                edgesProvided++;
            }
        }

        // Final completeness check
        if (edgesProvided != edgesRequired) {
            throw new TSPException(TSPException.ErrorType.NOT_FULL_GRAPH,
                "Graph is incomplete. Expected " + edgesRequired + " directed edges, " +
                "found " + edgesProvided + ". All pairs of distinct cities must be connected.");
        }
    }
}
