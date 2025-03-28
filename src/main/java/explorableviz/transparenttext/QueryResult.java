package explorableviz.transparenttext;

import explorableviz.transparenttext.paragraph.Expression;

public record QueryResult(Expression response, int attempt, long duration) {}
