package explorableviz.transparenttext.variable;

import com.google.errorprone.annotations.Var;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

public abstract class ValueOptions {
    public abstract Object get();

    public abstract Variables expandVariable(Random random, String varName);

    public static class StringValue extends ValueOptions {
        private final String value;

        public StringValue(String value) {
            this.value = value;
        }

        @Override
        public String get() {
            return value;
        }

        @Override
        public Variables expandVariable(Random random, String varName) {
            Variables v = new Variables();
            v.put(varName, switch (value) {
                case "RANDOM_INT" -> new ValueOptions.Number(random.nextInt(10));
                case "RANDOM_FLOAT" -> new ValueOptions.Number(random.nextFloat() * 10);
                case "RANDOM_STRING" -> new ValueOptions.StringValue(getRandomString(8, random).toLowerCase());
                default -> new StringValue(value);
            });
            return v;
        }
    }

    public static class Number extends ValueOptions {
        private final BigDecimal value;

        public Number(float value) {
            this.value = BigDecimal.valueOf(value);
        }

        @Override
        public BigDecimal get() {
            return value;
        }

        @Override
        public Variables expandVariable(Random random, String varName) {
            Variables v = new Variables();
            v.put(varName, this);
            return v;
        }
    }

    public static class Map extends ValueOptions {
        private final HashMap<String, String> value;

        public Map(HashMap<String, String> value) {
            this.value = value;
        }

        public void add(String key, String value) {
            this.value.put(key, value);
        }

        @Override
        public HashMap<String, String> get() {
            return value;
        }

        @Override
        public Variables expandVariable(Random random, String varName) {
            Variables v = new Variables();
            keySet().forEach(k -> v.put(STR."\{varName}.\{k}", new StringValue(value.get(k))));
            return v;
        }

        public String getValue(String key) {
            return value.get(key);
        }

        public Set<String> keySet() {
            return this.value.keySet();
        }
    }

    public static class List extends ValueOptions {
        private final java.util.List<ValueOptions> value;

        public List(java.util.List<ValueOptions> value) {
            this.value = value;
        }

        @Override
        public java.util.List<ValueOptions> get() {
            return value;
        }

        @Override
        public Variables expandVariable(Random random, String varName) {
            return value.get(random.nextInt(value.size())).expandVariable(random, varName);
        }
    }

    private static String getRandomString(int length, Random generator) {
        StringBuilder sb = new StringBuilder(length);
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        for (int i = 0; i < length; i++) {
            int randomIndex = generator.nextInt(characters.length());
            sb.append(characters.charAt(randomIndex));
        }
        return sb.toString();
    }
}


