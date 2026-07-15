package com.cognivio.ai.common.snapstart;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Makes a service safe under AWS Lambda SnapStart (KAN-59). SnapStart snapshots the JVM at the end of
 * the Init phase — <em>after</em> the Spring context has refreshed — so the HikariCP pool is captured
 * holding live TCP connections to Postgres. Those sockets are dead when the snapshot is restored onto
 * a fresh microVM, and a request that borrows one would fail (or stall on a validation round-trip).
 *
 * <p>This CRaC {@link Resource} evicts the pooled connections just before the checkpoint, so the
 * snapshot contains no live sockets and restored functions open fresh connections lazily on first
 * use. It self-registers with the global CRaC context on construction.
 *
 * <p><b>Inert off-Lambda.</b> On an ordinary JVM the CRaC checkpoint is never triggered, so
 * {@link #beforeCheckpoint} never runs — local, container and test executions are unaffected.
 */
public class SnapStartDataSourceResource implements Resource {

    private static final Logger log = LoggerFactory.getLogger(SnapStartDataSourceResource.class);

    private final DataSource dataSource;

    public SnapStartDataSourceResource(DataSource dataSource) {
        this.dataSource = dataSource;
        Core.getGlobalContext().register(this);
    }

    @Override
    public void beforeCheckpoint(Context<? extends Resource> context) {
        if (dataSource instanceof HikariDataSource hikari && hikari.getHikariPoolMXBean() != null) {
            log.info("SnapStart checkpoint: evicting pooled DB connections so no live socket is captured in the snapshot");
            hikari.getHikariPoolMXBean().softEvictConnections();
        }
    }

    @Override
    public void afterRestore(Context<? extends Resource> context) {
        // Nothing to do: HikariCP transparently opens fresh connections on the next borrow after restore.
    }
}
