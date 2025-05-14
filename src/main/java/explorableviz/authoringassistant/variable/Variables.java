package explorableviz.authoringassistant.variable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.SplittableRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Variables extends HashMap<String, ValueOptions> {

    public static Variables fromJSON(JSONObject variables) {
        return variables.keySet().stream()
                .collect(Variables::new, (map, k) -> map.put(k, switch (variables.get(k)) {
                    case String s -> new ValueOptions.StringValue(s);
                    case Integer i -> new ValueOptions.Number(i);
                    case BigDecimal f -> new ValueOptions.Number(f.floatValue());
                    case JSONArray array -> processJsonArray(array, k);
                    default -> null;
                }), Map::putAll);
    }

    private static ValueOptions.List processJsonArray(JSONArray array, String varName) {
        return new ValueOptions.List((IntStream.range(0, array.length())
                .mapToObj(array::get)
                .map(e -> {
                    if (!e.getClass().equals(array.get(0).getClass())) {
                        throw new RuntimeException(STR."Different types in a list are not allowed. Check the type of '\{varName}'.");
                    }
                    return ValueOptions.of(e);
                }).collect(Collectors.toList())));
    }

    public static class Flat extends Variables {
        public static Variables.Flat expandVariables(Variables variables, SplittableRandom random) {
            return variables.entrySet().stream()
                    .map(entry -> entry.getValue().expandVariable(random, entry.getKey()))
                    .reduce(new Variables.Flat(), (acc, map) -> {
                        acc.putAll(map);
                        return acc;
                    }, (m1, m2) -> {
                        m1.putAll(m2);
                        return m1;
                    });
        }
    }
}
