package tsp;

/**
 * Custom exception for TSP validation and runtime errors.
 */
public class TSPException extends Exception {

    public enum ErrorType {
        INVALID_NODE_COUNT("Invalid Node Count"),
        NOT_FULL_GRAPH("Graph is Not Complete (Full Graph Required)"),
        INVALID_DISTANCE("Invalid Distance Value"),
        SELF_LOOP_DISTANCE("Self-loop Distance Must Be Zero"),
        NEGATIVE_DISTANCE("Negative Distance Not Allowed"),
        DATABASE_ERROR("Database Connection Error"),
        MISSING_VALUE("Missing Distance Value");

        private final String label;
        ErrorType(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private final ErrorType errorType;

    public TSPException(ErrorType type, String message) {
        super(message);
        this.errorType = type;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    @Override
    public String toString() {
        return "[" + errorType.getLabel() + "] " + getMessage();
    }
}
