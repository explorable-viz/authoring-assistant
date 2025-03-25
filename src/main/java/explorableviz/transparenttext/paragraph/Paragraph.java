package explorableviz.transparenttext.paragraph;

import explorableviz.transparenttext.LiteralParts;
import explorableviz.transparenttext.Settings;
import kotlin.Pair;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Paragraph extends ArrayList<TextFragment> {

    public Paragraph() {
    }

    public String toString() {
        return STR."Paragraph([\{stream().map(e -> {
            if (e instanceof Literal) return STR."\"\{e.getValue()}\"";
            if (e instanceof Expression) return ((Expression) e).getExpr();
            throw new RuntimeException("Error, it is possible to have only String or Expression element");
        }).collect(Collectors.joining(","))}])";
    }

    public String getValueFromExpression(String expression) {
        return this.stream().filter(e -> e instanceof Expression && ((Expression) e).getExpr().equals(expression)).findFirst().get().getValue();
    }

    public Pair<String, Expression> toStringWithReplace(int n) {
        AtomicInteger k = new AtomicInteger(0);
        AtomicReference<Expression> expression = new AtomicReference<>(null);
        String paragraphWithReplace = this.stream()
                .map(textFragment -> {
                    if (textFragment instanceof Literal) {
                        return STR."\{textFragment.getValue()} ";
                    } else if (textFragment instanceof Expression) {
                        int currentK = k.getAndIncrement();
                        if (currentK == n) {
                            expression.set(((Expression) textFragment));
                            return STR."[REPLACE id=\"id_\{currentK}\" \{(Settings.isAddExpectedValueEnabled() ? STR."value=\"\{textFragment.getValue()}\"" : "")}]";
                        } else {
                            return STR."\{textFragment.getValue()} ";
                        }
                    } else {
                        throw new RuntimeException("Illegal Textfragment Type");
                    }
                })
                .collect(Collectors.joining());
        return new Pair<>(paragraphWithReplace, expression.get());
    }

    public void spliceExpression(String expression) {
        List<TextFragment> paragraph = this;
        ListIterator<TextFragment> iterator = paragraph.listIterator();

        while (iterator.hasNext()) {
            TextFragment textFragment = iterator.next();
            if (textFragment instanceof Literal) {
                splitLiteral(textFragment).ifPresentOrElse(expectedValue -> {
                    iterator.remove();
                    iterator.add(expectedValue.beforeTag());
                    iterator.add(new Expression(expression, expectedValue.tag().getValue()));
                    iterator.add(expectedValue.afterTag());
                }, () -> {
                    throw new RuntimeException("REPLACE tag not found");
                });
            }
        }
    }

    public static Optional<LiteralParts> splitLiteral(TextFragment literal) {
        Matcher valueReplaceMatcher = Pattern.compile("(.*)\\[REPLACE id=\".*?\" value=\"(.*?)\"](.*)").matcher(literal.getValue());
        if (!valueReplaceMatcher.find()) {
            return Optional.empty();
        }
        return Optional.of(new LiteralParts(new Literal(valueReplaceMatcher.group(1)), new Literal(valueReplaceMatcher.group(2)), new Literal(valueReplaceMatcher.group(3))));
    }
}
