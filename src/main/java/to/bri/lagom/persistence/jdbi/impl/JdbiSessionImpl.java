package to.bri.lagom.persistence.jdbi.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import javax.inject.Inject;
import javax.sql.DataSource;

import play.api.db.DBApi;

import akka.actor.ActorSystem;
import akka.dispatch.MessageDispatcher;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import to.bri.lagom.persistence.jdbi.JdbiSession;

public class JdbiSessionImpl implements JdbiSession {
    @SuppressWarnings("FieldCanBeLocal")
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final MessageDispatcher executionContext;
    private final Jdbi              jdbi;

    @Inject
    public JdbiSessionImpl(ActorSystem system, DBApi dbApi) {
        executionContext = lookupDispatcher(system);
        logger.debug("Using dispatcher ", executionContext);

        DataSource datasource = dbApi.database("default").dataSource();
        logger.debug("Using datasource ", datasource);

        this.jdbi = Jdbi.create(datasource);
        jdbi.installPlugins();
    }

    private MessageDispatcher lookupDispatcher(ActorSystem system) {
        return system.dispatchers().hasDispatcher("jdbi")
            ? system.dispatchers().lookup("jdbi")
            : system.dispatchers().defaultGlobalDispatcher();
    }

    @Override
    public <T> CompletionStage<T> withHandle(Function<Handle, T> block) {
        return CompletableFuture.supplyAsync(() ->
            jdbi.withHandle(block::apply), executionContext);
    }

}
