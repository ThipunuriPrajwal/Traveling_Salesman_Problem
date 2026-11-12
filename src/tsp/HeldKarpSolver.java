package tsp;

import java.util.ArrayList;
import java.util.List;

/**
 * Held-Karp dynamic programming algorithm for exact TSP solution.
 * Time complexity: O(n^2 * 2^n) | Space: O(n * 2^n)
 */
public class HeldKarpSolver {

    private final int[][] dist;
    private final int n;
    private int[][] dp;
    private int[][] parent;
    private List<Integer> bestPath;
    private int bestCost;

    public HeldKarpSolver(int[][] dist) {
        this.dist = dist;
        this.n = dist.length;
    }

    public void solve() throws TSPException {
        if (n < 2) throw new TSPException(TSPException.ErrorType.INVALID_NODE_COUNT,
                "Need at least 2 nodes to solve TSP.");

        int states = 1 << n;
        dp = new int[states][n];
        parent = new int[states][n];

        // Initialize DP table
        for (int s = 0; s < states; s++)
            for (int i = 0; i < n; i++) {
                dp[s][i] = Integer.MAX_VALUE / 2;
                parent[s][i] = -1;
            }

        // Start at node 0
        dp[1][0] = 0;

        for (int mask = 1; mask < states; mask++) {
            if ((mask & 1) == 0) continue; // must include node 0
            for (int u = 0; u < n; u++) {
                if ((mask & (1 << u)) == 0) continue;
                if (dp[mask][u] == Integer.MAX_VALUE / 2) continue;
                for (int v = 0; v < n; v++) {
                    if ((mask & (1 << v)) != 0) continue;
                    int newMask = mask | (1 << v);
                    int newCost = dp[mask][u] + dist[u][v];
                    if (newCost < dp[newMask][v]) {
                        dp[newMask][v] = newCost;
                        parent[newMask][v] = u;
                    }
                }
            }
        }

        // Find best last node
        int fullMask = states - 1;
        bestCost = Integer.MAX_VALUE;
        int lastNode = -1;
        for (int u = 1; u < n; u++) {
            if (dp[fullMask][u] == Integer.MAX_VALUE / 2) continue;
            int cost = dp[fullMask][u] + dist[u][0];
            if (cost < bestCost) {
                bestCost = cost;
                lastNode = u;
            }
        }

        // Reconstruct path
        bestPath = new ArrayList<>();
        int mask = fullMask;
        int cur = lastNode;
        while (cur != -1) {
            bestPath.add(0, cur);
            int prev = parent[mask][cur];
            mask ^= (1 << cur);
            cur = prev;
        }
        bestPath.add(0); // Return to start
    }

    public List<Integer> getBestPath() { return bestPath; }
    public int getBestCost() { return bestCost; }

    public String getPathString() {
        if (bestPath == null) return "Not solved yet";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bestPath.size(); i++) {
            sb.append("City ").append(bestPath.get(i));
            if (i < bestPath.size() - 1) sb.append(" → ");
        }
        return sb.toString();
    }
}
