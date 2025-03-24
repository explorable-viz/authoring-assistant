package explorableviz.transparenttext;

public record QueryResult(String response, int attempt, SubQuery query, long duration) {}
