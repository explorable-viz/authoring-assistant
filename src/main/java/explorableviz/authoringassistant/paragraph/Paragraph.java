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

    public String toFluidSyntax() {
        return STR."Paragraph [\n\t\{stream().map(e -> {
            if (e instanceof Literal l && l.getSelectedRegion() != null)
            {
                return STR."\{e.getValue().substring(0, l.getSelectedRegion().start())} [REPLACE \{Settings.isAddExpectedValueEnabled() ? STR."value=\"\{e.getValue()}\"" : ""}]\{e.getValue().substring(l.getSelectedRegion().end())}";
            }
            else if (e instanceof Literal) return STR."Text \"\{e.getValue()}\"";
            else if (e instanceof Expression) {
                if(e.getValue().matches("-?\\d+(\\.\\d+)?"))
                    return (STR."Text (numToStr (\{((Expression) e).getExpr()}))");
                else
                    return (STR."Text (\{((Expression) e).getExpr()})");
            }
            throw new RuntimeException("Error, it is possible to have only String, Expression element");
        }).collect(Collectors.joining(",\n\t"))}\n]";
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
            if(t instanceof Literal l && l.getSelectedRegion() != null) {
                Literal start = new Literal(t.getValue().substring(0, l.getSelectedRegion().start()), null);
                if(!start.getValue().isEmpty()) p.add(start);
                p.add(expression);
                Literal end = new Literal(t.getValue().substring(l.getSelectedRegion().end()), null);
                if(!end.getValue().isEmpty()) p.add(end);
            } else {
                p.add(t);
            }
        }
        return p;
    }
}
