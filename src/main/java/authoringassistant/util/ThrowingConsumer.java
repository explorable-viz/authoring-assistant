package authoringassistant.util;

import java.util.function.Consumer;

@FunctionalInterface
public interface ThrowingConsumer<T> {
    void accept(T t) throws Exception;

    static <T> Consumer<T> toConsumer(ThrowingConsumer<T> throwingConsumer) {
        return item -> {
            try {
                throwingConsumer.accept(item);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
