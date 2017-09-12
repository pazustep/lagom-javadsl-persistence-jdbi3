package to.bri.lagom.persistence.jdbi;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.lightbend.lagom.javadsl.persistence.AggregateEvent;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;
import com.lightbend.lagom.javadsl.persistence.ReadSideProcessor;
import org.jdbi.v3.core.Handle;

public interface JdbiReadSide {
    <Event extends AggregateEvent<Event>> ReadSideHandlerBuilder<Event> builder(String readSideId);

    interface ReadSideHandlerBuilder<Event extends AggregateEvent<Event>> {
        ReadSideHandlerBuilder<Event> setGlobalPrepare(Consumer<Handle> callback);

        ReadSideHandlerBuilder<Event> setPrepare(BiConsumer<Handle, AggregateEventTag<Event>> callback);

        <E extends Event> ReadSideHandlerBuilder<Event> setEventHandler(Class<E> eventClass,
            BiConsumer<Handle, E> handler);

        ReadSideProcessor.ReadSideHandler<Event> build();
    }

}
