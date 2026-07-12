package com.cognivio.ai.common.tenancy;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code ilr.rls.*} namespace controlling the Postgres RLS session
 * binding.
 */
@ConfigurationProperties("ilr.rls")
public class RlsProperties {

    /** Whether to bind {@code app.current_tenant_id} at transaction start. Default {@code true}. */
    private boolean enabled = true;

    /** The Postgres session setting name the RLS policies read. Default {@code app.current_tenant_id}. */
    private String settingName = "app.current_tenant_id";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSettingName() {
        return settingName;
    }

    public void setSettingName(String settingName) {
        this.settingName = settingName;
    }
}
