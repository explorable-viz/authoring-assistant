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
            if (e instanceof Expression) return (STR."Text (\{((Expression) e).getExpr()})");
            throw new RuntimeException("Error, it is possible to have only String or Expression element");
        }).collect(Collectors.joining(","))}])";
    }

    public String toQueryString() {
        AtomicInteger exprCount = new AtomicInteger();
        AtomicInteger lastExprIndex = new AtomicInteger(-1);
        List<String> elements = stream().map(e -> {
            exprCount.getAndIncrement();
            if (e instanceof Literal) {
                return STR."\"\{e.getValue()}\"";
            } else if (e instanceof Expression) {
                lastExprIndex.set(exprCount.get());
                return STR."Text (\{((Expression) e).getExpr()})";
            }
            throw new RuntimeException("Error, it is possible to have only String or Expression element");
        }).collect(Collectors.toList());

        if (lastExprIndex.get() != -1) {
            elements.set(lastExprIndex.get()-1, STR." [REPLACE id=\"\{lastExprIndex}]\"");
        }
        return STR."Paragraph([\{String.join(",", elements)}])";
    }
}
