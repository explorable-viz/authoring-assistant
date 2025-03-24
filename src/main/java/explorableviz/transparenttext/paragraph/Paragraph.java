package explorableviz.transparenttext.paragraph;

import explorableviz.transparenttext.LiteralParts;
import explorableviz.transparenttext.variable.Variables;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Paragraph extends ArrayList<TextFragment> {

    public Paragraph(JSONArray json_paragraph, Variables variables, Map<String, String> expectedValue) {
        this.addAll(IntStream.range(0, json_paragraph.length())
                .mapToObj(i -> {
                    JSONObject paragraphElement = json_paragraph.getJSONObject(i);
                    String type = paragraphElement.getString("type");

                    return (switch (type) {
                        case "literal" -> new Literal(paragraphElement.getString("value"));
                        case "expression" ->
                                new Expression(paragraphElement.getString("expression"), expectedValue.get(paragraphElement.getString("expression")));
                        default -> throw new RuntimeException(STR."\{type} type is invalid");
                    }).replace(variables);
                })
                .toList());
    }

    public String toString() {
        return STR."Paragraph([\{stream().map(e -> {
            if (e instanceof Literal) return STR."\"\{e.getValue()}\"";
            if (e instanceof Expression) return ((Expression) e).getExpr();
            throw new RuntimeException("Error, it is possible to have only String or Expression element");
        }).collect(Collectors.joining(","))}])";
    }

    public String toStringWithReplace(int n) {
        AtomicInteger k = new AtomicInteger(0);
        return this.stream()
                .map(textFragment -> {
                    if (textFragment instanceof Literal) {
                        return STR."\{textFragment.getValue()} ";
                    } else if (textFragment instanceof Expression){
                        int currentK = k.getAndIncrement();
                        return (currentK == n)
                                ? STR."[REPLACE id=\"id_\{currentK + 1}\"]"
                                : STR."\{textFragment.getValue()} ";
                    } else {
                        throw new RuntimeException("Illegal Textfragment Type");
                    }
                })
                .collect(Collectors.joining());
    }

    public List<String> getParagraphForQueries() {
        return IntStream.range(0, (int) this.stream()
                        .filter(textFragment -> !(textFragment instanceof Literal))
                        .count())
                .mapToObj(this::toStringWithReplace)
                .collect(Collectors.toList());
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
