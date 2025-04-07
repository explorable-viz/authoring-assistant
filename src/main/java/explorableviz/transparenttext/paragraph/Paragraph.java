package explorableviz.transparenttext.paragraph;

import kotlin.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Paragraph extends ArrayList<TextFragment> {

    public Paragraph() {
    }

    public String toString() {
        return STR."Paragraph([\{stream().map(e -> {
            if (e instanceof Literal l && l.getSelectedRegion() != null)
            {
                return STR."\{e.getValue().substring(0, l.getSelectedRegion().start())} [REPLACE]\{e.getValue().substring(l.getSelectedRegion().end())}";
            }
            else if (e instanceof Literal) return STR."\"\{e.getValue()}\"";
            else if (e instanceof Expression) return (STR."Text (\{((Expression) e).getExpr()})");
            throw new RuntimeException("Error, it is possible to have only String, Expression or SelectedLiteral element");
        }).collect(Collectors.joining(","))}])";
    }

    public List<Pair<Expression,Paragraph>> programs(List<Expression> computed) {
        int nPrograms = (int) stream().filter(Expression.class::isInstance).count() - computed.size();
        return IntStream.range(0, nPrograms)
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
                                p.add(new Literal(literalBuilder.toString(), null));
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
                    p.add(new Literal(literalBuilder.toString(), new Literal.SelectedRegion(replaceStart, replaceEnd)));
                    return new Pair<>(toCompute, p);
                })
                .toList();
    }
}
