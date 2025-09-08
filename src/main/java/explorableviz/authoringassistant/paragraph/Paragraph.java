package explorableviz.authoringassistant.paragraph;

import explorableviz.authoringassistant.Settings;
import kotlin.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Paragraph extends ArrayList<TextFragment> {

    public Paragraph() {
    }

    public String toFluidSyntax(boolean onlyValue) {
        return STR."\"\"\"\n\t\{stream().map(e -> {
            if (e instanceof Literal l) {
                if (!onlyValue && l.getSelectedRegion() != null) {
                    final String replace = Settings.isAddExpectedValueEnabled() ? STR."value=\"\{e.getValue().substring(l.getSelectedRegion().start(), l.getSelectedRegion().end())}\"" : "";
                    return STR."\{e.getValue().substring(0, l.getSelectedRegion().start())} [REPLACE \{replace}]\{e.getValue().substring(l.getSelectedRegion().end())}";
                }
                else {
                    return e.getValue();
                }
            }
            else if (e instanceof Expression e_) {
                if (!onlyValue) {
                    return (STR."${\{e_.getExpr()}}");
                } else if (Settings.isAddExpectedValueEnabled()) {
                    return e.getValue();
                } else {
                    return ("${?}");
                }
            } else {
                throw new RuntimeException("Literal or expression expected.");
            }
        }).collect(Collectors.joining(" "))}\n\"\"\"";
    }

    public List<Pair<Expression, Paragraph>> asIndividualEdits(Paragraph template) {
        final int numComputedExpr;
        if (template == this) {
            numComputedExpr = 0;
        } else {
            numComputedExpr = countExpressions(this);
        }
        return IntStream.range(0, countExpressions(template) - numComputedExpr)
                .mapToObj(i -> testParagraph(template, numComputedExpr, i))
                .toList();
    }

    private int countExpressions(Paragraph paragraph) {
        return (int) paragraph.stream().filter(Expression.class::isInstance).count();
    }

    private Pair<Expression, Paragraph> testParagraph(Paragraph template, int numComputedExpr, int index) {
        Paragraph p = new Paragraph();
        Expression toCompute = null;
        int exprCount = 0;
        StringBuilder literalBuilder = new StringBuilder();
        int replaceStart = -1, replaceEnd = -1;

        for (int k = 0; k < template.size(); k++) {
            var element = template.get(k);

            if (element instanceof Literal literal) {
                literalBuilder.append(literal.getValue());
            } else if (element instanceof Expression expression) {
                if (exprCount < numComputedExpr) {
                    Expression computedExpr = (Expression) this.get(k);
                    p.add(new Literal(literalBuilder.toString(), null));
                    literalBuilder.setLength(0);
                    p.add(computedExpr != null ? computedExpr : expression);
                } else if (exprCount == numComputedExpr + index) {
                    replaceStart = literalBuilder.length();
                    literalBuilder.append(expression.getValue());
                    replaceEnd = literalBuilder.length();
                    toCompute = expression;
                } else {
                    literalBuilder.append(expression.getValue());
                }
                exprCount++;
            }
        }

        p.add(new Literal(literalBuilder.toString(), new Literal.SelectedRegion(replaceStart, replaceEnd)));
        return new Pair<>(toCompute, p);
    }

    public Paragraph splice(Expression expression) {
        Paragraph p = new Paragraph();
        for (TextFragment t : this) {
            if (t instanceof Literal l && l.getSelectedRegion() != null) {
                Literal start = new Literal(t.getValue().substring(0, l.getSelectedRegion().start()), null);
                if (!start.getValue().isEmpty()) p.add(start);
                p.add(expression);
                Literal end = new Literal(t.getValue().substring(l.getSelectedRegion().end()), null);
                if (!end.getValue().isEmpty()) p.add(end);
            } else {
                p.add(t);
            }
        }
        return p;
    }
}
