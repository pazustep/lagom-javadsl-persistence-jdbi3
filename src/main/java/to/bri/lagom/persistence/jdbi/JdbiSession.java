package to.bri.lagom.persistence.jdbi;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

import akka.Done;
import org.jdbi.v3.core.Handle;

public interface JdbiSession {

    <T> CompletionStage<T> withHandle(Function<Handle, T> block);

    default CompletionStage<Done> useHandle(Consumer<Handle> block) {
        return withHandle(handle -> {
            block.accept(handle);
            return Done.getInstance();
        });
    }
}
