package to.bri.lagom.persistence.jdbi.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.inject.Inject;

import akka.Done;
import akka.japi.Pair;
import akka.stream.javadsl.Flow;
import com.lightbend.lagom.internal.javadsl.persistence.OffsetAdapter;
import com.lightbend.lagom.internal.javadsl.persistence.jdbc.SlickProvider;
import com.lightbend.lagom.javadsl.persistence.AggregateEvent;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;
import com.lightbend.lagom.javadsl.persistence.Offset;
import com.lightbend.lagom.javadsl.persistence.ReadSideProcessor.ReadSideHandler;
import com.lightbend.lagom.spi.persistence.OffsetDao;
import com.lightbend.lagom.spi.persistence.OffsetStore;
import org.jdbi.v3.core.Handle;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.compat.java8.FutureConverters;
import scala.concurrent.Future;
import to.bri.lagom.persistence.jdbi.JdbiReadSide;
import to.bri.lagom.persistence.jdbi.JdbiSession;

public class JdbiReadSideImpl implements JdbiReadSide {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final SlickProvider slickProvider;
    private final JdbiSession   session;
    private final OffsetStore   offsetStore;

    volatile private OffsetDao offsetDao;

    @Inject
    public JdbiReadSideImpl(SlickProvider slickProvider, JdbiSession session, OffsetStore offsetStore) {
        this.slickProvider = slickProvider;
        this.session = session;
        this.offsetStore = offsetStore;
    }

    @Override
    public <Event extends AggregateEvent<Event>> ReadSideHandlerBuilder<Event> builder(String readSideId) {
        return new JdbiReadSideBuilder<>(readSideId);
    }

    private class JdbiReadSideBuilder<Event extends AggregateEvent<Event>> implements ReadSideHandlerBuilder<Event> {
        private final String readSideId;

        private Consumer<Handle>                             globalPrepareCallback;
        private BiConsumer<Handle, AggregateEventTag<Event>> prepareCallback;
        private PMap<Class<?>, BiConsumer<Handle, Event>>    eventHandlers;

        private JdbiReadSideBuilder(String readSideId) {
            this.readSideId = readSideId;
            this.globalPrepareCallback = handle -> {};
            this.prepareCallback = (handle, tag) -> {};
            this.eventHandlers = HashTreePMap.empty();
        }

        @Override
        public ReadSideHandlerBuilder<Event> setGlobalPrepare(Consumer<Handle> callback) {
            this.globalPrepareCallback = callback;
            return this;
        }

        @Override
        public ReadSideHandlerBuilder<Event> setPrepare(BiConsumer<Handle, AggregateEventTag<Event>> callback) {
            this.prepareCallback = callback;
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <E extends Event> ReadSideHandlerBuilder<Event> setEventHandler(Class<E> eventClass, BiConsumer<Handle, E> handler) {
            this.eventHandlers = eventHandlers.plus(eventClass, (BiConsumer<Handle, Event>) handler);
            return this;
        }

        @Override
        public ReadSideHandler<Event> build() {
            return new JdbiReadSideHandler<>(readSideId, globalPrepareCallback, prepareCallback, eventHandlers);
        }
    }

    private class JdbiReadSideHandler<Event extends AggregateEvent<Event>> extends ReadSideHandler<Event> {
        private final String                                       readSideId;
        private final Consumer<Handle>                             globalPrepareCallback;
        private final BiConsumer<Handle, AggregateEventTag<Event>> prepareCallback;
        private final PMap<Class<?>, BiConsumer<Handle, Event>>    eventHandlers;

        private JdbiReadSideHandler(String readSideId,
            Consumer<Handle> globalPrepareCallback,
            BiConsumer<Handle, AggregateEventTag<Event>> prepareCallback,
            PMap<Class<?>, BiConsumer<Handle, Event>> eventHandlers) {
            this.readSideId = readSideId;
            this.globalPrepareCallback = globalPrepareCallback;
            this.prepareCallback = prepareCallback;
            this.eventHandlers = eventHandlers;
        }

        @Override
        public CompletionStage<Done> globalPrepare() {
            CompletionStage<Done> tablesStage = FutureConverters.toJava(slickProvider.ensureTablesCreated());
            return tablesStage.thenCompose(done -> session.useHandle(globalPrepareCallback));
        }

        @Override
        public CompletionStage<Offset> prepare(AggregateEventTag<Event> tag) {
            CompletionStage<Done> prepareStage = session.useHandle(handle ->
                prepareCallback.accept(handle, tag));

            CompletionStage<OffsetDao> daoStage = prepareStage.thenCompose(done -> {
                Future<OffsetDao> future = offsetStore.prepare(readSideId, tag.tag());
                return FutureConverters.toJava(future);
            });

            return daoStage.thenApply(offsetDao -> {
                JdbiReadSideImpl.this.offsetDao = offsetDao;
                return OffsetAdapter.offsetToDslOffset(offsetDao.loadedOffset());
            });
        }

        @Override
        public Flow<Pair<Event, Offset>, Done, ?> handle() {
            return Flow.<Pair<Event, Offset>>create().mapAsync(1, pair -> {
                Event event = pair.first();
                Offset offset = pair.second();

                BiConsumer<Handle, Event> handler = eventHandlers.get(event.getClass());
                CompletionStage<Done> handlerStage;

                if (handler != null) {
                    handlerStage = session.useHandle(handle -> handler.accept(handle, event));
                } else {
                    logger.debug("Unhandled event [{}]", event.getClass().getName());
                    handlerStage = CompletableFuture.completedFuture(Done.getInstance());
                }

                return handlerStage.thenCompose(done -> {
                    Future<Done> future = offsetDao.saveOffset(OffsetAdapter.dslOffsetToOffset(offset));
                    return FutureConverters.toJava(future);
                });
            });
        }
    }
}
