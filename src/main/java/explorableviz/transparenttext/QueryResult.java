package explorableviz.transparenttext;

public record QueryResult(String response, int attempt, Query query, long duration) {}
