package explorableviz.transparenttext.paragraph;

import explorableviz.transparenttext.LiteralParts;
import explorableviz.transparenttext.Program;
import explorableviz.transparenttext.Query;
import explorableviz.transparenttext.Settings;
import kotlin.Pair;
import org.checkerframework.checker.units.qual.A;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Paragraph extends ArrayList<TextFragment> {

    public Paragraph() {
    }

    public String toString() {
        return STR."Paragraph([\{stream().map(e -> {
            if (e instanceof Literal) return STR."\"\{e.getValue()}\"";
            if (e instanceof Expression) return (STR."Text (\{((Expression) e).getExpr()})");
            if (e instanceof SelectedLiteral) return ("[REPLACE]");
            throw new RuntimeException("Error, it is possible to have only String, Expression or SelectedLiteral element");
        }).collect(Collectors.joining(","))}])";
    }

    public List<Query> queries(Program program, List<Expression> computed) {
        int nQueries = (int) stream().filter(Expression.class::isInstance).count() - computed.size();
        return IntStream.range(0, nQueries)
                .mapToObj(i -> {
                    Paragraph p = new Paragraph();
                    Expression toCompute = null;
                    int exprCount = 0;
                    for (TextFragment element : this) {
                        if (element instanceof Literal) {
                            p.add(element);
                        } else if (element instanceof Expression expression) {
                            if (exprCount < computed.size()) {
                                p.add(computed.get(exprCount) != null ? computed.get(exprCount) : expression);
                            } else if (exprCount == computed.size() + i) { // Questa è la nuova da computare
                                p.add(new SelectedLiteral(expression.getValue()));
                                toCompute = expression;
                            } else {
                                p.add(new Literal(expression.getValue()));
                            }
                            exprCount++;
                        }
                    }
                    return new Query(program, p.toString(), toCompute);
                })
                .toList();
    }
}
