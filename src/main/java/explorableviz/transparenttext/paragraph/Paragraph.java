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
            if (e instanceof Literal l && l.getSelectedRegion().isEmpty()) return STR."\"\{e.getValue()}\"";
            if (e instanceof Literal l && l.getSelectedRegion().isPresent())
            {
                SelectedRegion region = l.getSelectedRegion().get();
                return STR."\{e.getValue().substring(0, region.getStart())} [REPLACE]\{e.getValue().substring(region.getEnd())}";
            }
            if (e instanceof Expression) return (STR."Text (\{((Expression) e).getExpr()})");
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
                    StringBuilder literalBuilder = new StringBuilder();
                    int replaceStart = -1, replaceEnd = -1;
                    for (TextFragment element : this) {
                        if (element instanceof Literal literal) {
                            literalBuilder.append(STR." \{literal.getValue()}");
                        } else if (element instanceof Expression expression) {
                            if (exprCount < computed.size()) {
                                Expression computedExpr = computed.get(exprCount);
                                p.add(new Literal(literalBuilder.toString(), Optional.empty()));
                                literalBuilder.setLength(0);
                                //@todo maybe the value?
                                p.add(computedExpr != null ? computedExpr : expression);
                            } else if (exprCount == computed.size() + i) {
                                replaceStart = literalBuilder.toString().length();
                                literalBuilder.append(STR." \{expression.getValue()}");
                                replaceEnd = literalBuilder.toString().length();
                                toCompute = expression;
                            } else {
                                literalBuilder.append(STR." \{expression.getValue()}");
                            }
                            exprCount++;
                        }
                    }
                    Literal finalLiteral = new Literal(literalBuilder.toString(), Optional.of(new SelectedRegion(replaceStart, replaceEnd)));
                    p.add(finalLiteral);
                    return new Query(program, p.toString(), toCompute);
                })
                .toList();
    }
}
