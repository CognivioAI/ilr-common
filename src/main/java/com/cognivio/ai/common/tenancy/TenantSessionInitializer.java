package com.cognivio.ai.common.tenancy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Issues the Postgres {@code set_config} call that makes the RLS policies —
 * declared in every service's V1 migration as
 * {@code USING (tenant_id = current_setting('app.current_tenant_id', true)::uuid)} —
 * actually engage. Without this, {@code current_setting('app.current_tenant_id')}
 * is unset and the policies are effectively inert.
 *
 * <p>The {@code set_config(name, value, is_local => true)} form scopes the
 * setting to the <b>current transaction</b>, so it is automatically reset at
 * transaction end and never leaks across pooled connections.
 *
 * <p>This class is intentionally a tiny, dependency-free primitive so it can be
 * unit-tested against a mocked {@link Connection}/{@link PreparedStatement}
 * (verifying the exact SQL and bound parameter). Wiring it into real
 * transactions is {@link RlsTenantBindingAspect}'s job.
 */
public class TenantSessionInitializer {

    /** Guards against SQL injection via the (config-supplied) setting name. */
    private static final Pattern SAFE_SETTING = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_.]*$");

    private final String settingName;
    private final String sql;

    public TenantSessionInitializer() {
        this("app.current_tenant_id");
    }

    public TenantSessionInitializer(String settingName) {
        if (settingName == null || !SAFE_SETTING.matcher(settingName).matches()) {
            throw new IllegalArgumentException("Unsafe RLS setting name: " + settingName);
        }
        this.settingName = settingName;
        // Setting name is validated above (never client input); the tenant value is always bound.
        this.sql = "SELECT set_config('" + settingName + "', ?, true)";
    }

    public String getSettingName() {
        return settingName;
    }

    /** The exact statement issued by {@link #apply(Connection, UUID)}. */
    public String getSql() {
        return sql;
    }

    /**
     * Binds {@code tenantId} to {@code app.current_tenant_id} for the current
     * transaction on {@code connection}. A {@code null} tenant binds the empty
     * string, which makes {@code current_setting(...,true)::uuid} evaluate to
     * NULL and the RLS policies match no rows — i.e. fail closed.
     */
    public void apply(Connection connection, UUID tenantId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tenantId != null ? tenantId.toString() : "");
            statement.execute();
        }
    }
}
