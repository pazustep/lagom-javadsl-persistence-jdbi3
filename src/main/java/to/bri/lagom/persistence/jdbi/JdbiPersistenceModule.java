package to.bri.lagom.persistence.jdbi;

import com.google.inject.AbstractModule;
import to.bri.lagom.persistence.jdbi.impl.JdbiReadSideImpl;
import to.bri.lagom.persistence.jdbi.impl.JdbiSessionImpl;

public class JdbiPersistenceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(JdbiSession.class).to(JdbiSessionImpl.class);
        bind(JdbiReadSide.class).to(JdbiReadSideImpl.class);
    }

}
