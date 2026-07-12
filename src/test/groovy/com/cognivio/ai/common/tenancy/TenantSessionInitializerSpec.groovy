package com.cognivio.ai.common.tenancy

import spock.lang.Specification

import java.sql.Connection
import java.sql.PreparedStatement

class TenantSessionInitializerSpec extends Specification {

    def "issues SELECT set_config with the default setting name and binds the tenant id"() {
        given:
        def initializer = new TenantSessionInitializer()
        def tenant = UUID.randomUUID()
        Connection connection = Mock()
        PreparedStatement statement = Mock()

        when:
        initializer.apply(connection, tenant)

        then:
        1 * connection.prepareStatement("SELECT set_config('app.current_tenant_id', ?, true)") >> statement
        1 * statement.setString(1, tenant.toString())
        1 * statement.execute()
        1 * statement.close()
    }

    def "binds the empty string for a null tenant (fail-closed: RLS matches no rows)"() {
        given:
        def initializer = new TenantSessionInitializer()
        Connection connection = Mock()
        PreparedStatement statement = Mock()

        when:
        initializer.apply(connection, null)

        then:
        1 * connection.prepareStatement(_) >> statement
        1 * statement.setString(1, '')
        1 * statement.execute()
    }

    def "uses a configurable setting name in the SQL"() {
        given:
        def initializer = new TenantSessionInitializer('app.current_firm_id')

        expect:
        initializer.sql == "SELECT set_config('app.current_firm_id', ?, true)"
        initializer.settingName == 'app.current_firm_id'
    }

    def "rejects an unsafe setting name (SQL-injection guard)"() {
        when:
        new TenantSessionInitializer("app.tenant'); DROP TABLE users;--")

        then:
        thrown(IllegalArgumentException)
    }
}
