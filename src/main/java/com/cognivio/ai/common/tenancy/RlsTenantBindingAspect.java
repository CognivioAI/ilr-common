package com.cognivio.ai.common.tenancy;

import com.cognivio.ai.common.context.TenantContext;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Binds the resolved tenant to the DB session at the start of every
 * {@code @Transactional} unit of work, so the RLS policies engage for the
 * queries that follow. It calls {@link TenantSessionInitializer#apply} against
 * the transaction-bound {@link Connection}, once per transaction (guarded by a
 * transaction-scoped marker so nested {@code @Transactional} calls don't rebind).
 *
 * <p>It only binds when a {@link TenantContext} is present (i.e. a verified JWT
 * was resolved); permit-listed/unauthenticated flows carry no tenant and are
 * skipped — those flows must not touch tenant-scoped tables.
 *
 * <p><b>Ordering / verification note.</b> For the {@code set_config} to affect
 * the same connection the business queries use, this advice must execute
 * <em>inside</em> the transaction. Because Spring's transaction advisor is at
 * {@link Ordered#LOWEST_PRECEDENCE} by default, this aspect guards on
 * {@link TransactionSynchronizationManager#isActualTransactionActive()} and only
 * acts when a transaction is genuinely active. End-to-end proof that RLS then
 * filters rows requires a Postgres integration test (Testcontainers), which is
 * out of scope for this library — see the README. The {@code set_config}
 * invocation itself is unit-tested via {@link TenantSessionInitializer}.
 */
@Aspect
public class RlsTenantBindingAspect implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(RlsTenantBindingAspect.class);
    private static final String BOUND_MARKER = RlsTenantBindingAspect.class.getName() + ".BOUND";

    private final DataSource dataSource;
    private final TenantContext tenantContext;
    private final TenantSessionInitializer initializer;

    public RlsTenantBindingAspect(DataSource dataSource, TenantContext tenantContext,
                                  TenantSessionInitializer initializer) {
        this.dataSource = dataSource;
        this.tenantContext = tenantContext;
        this.initializer = initializer;
    }

    @Before("@annotation(org.springframework.transaction.annotation.Transactional) "
            + "|| @within(org.springframework.transaction.annotation.Transactional)")
    public void bindTenant() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return;
        }
        if (Boolean.TRUE.equals(TransactionSynchronizationManager.getResource(BOUND_MARKER))) {
            return; // already bound earlier in this transaction
        }
        if (!tenantContext.isPresent()) {
            return; // no verified tenant on this request — do not touch tenant-scoped tables
        }
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            initializer.apply(connection, tenantContext.getTenantId());
            markBound();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to bind RLS tenant for the current transaction", ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private void markBound() {
        TransactionSynchronizationManager.bindResource(BOUND_MARKER, Boolean.TRUE);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (TransactionSynchronizationManager.hasResource(BOUND_MARKER)) {
                    TransactionSynchronizationManager.unbindResource(BOUND_MARKER);
                }
            }
        });
    }

    @Override
    public int getOrder() {
        // Aim to run as close to the target (inside the transaction) as ordering allows.
        return Ordered.LOWEST_PRECEDENCE;
    }
}
