package com.cognivio.ai.common.snapstart

import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.HikariPoolMXBean
import spock.lang.Specification

import javax.sql.DataSource

/**
 * KAN-59: the SnapStart checkpoint hook must evict pooled DB connections before the JVM snapshot, so
 * restored Lambda functions never borrow a dead socket captured at checkpoint time.
 */
class SnapStartDataSourceResourceSpec extends Specification {

    def "beforeCheckpoint soft-evicts Hikari pool connections"() {
        given:
        def pool = Mock(HikariPoolMXBean)
        def hikari = Mock(HikariDataSource) { getHikariPoolMXBean() >> pool }
        def resource = new SnapStartDataSourceResource(hikari)

        when:
        resource.beforeCheckpoint(null)

        then:
        1 * pool.softEvictConnections()
    }

    def "beforeCheckpoint is a no-op when the Hikari pool is not yet started"() {
        given:
        def hikari = Mock(HikariDataSource) { getHikariPoolMXBean() >> null }
        def resource = new SnapStartDataSourceResource(hikari)

        when:
        resource.beforeCheckpoint(null)

        then:
        noExceptionThrown()
    }

    def "beforeCheckpoint ignores a non-Hikari DataSource"() {
        given:
        def resource = new SnapStartDataSourceResource(Mock(DataSource))

        when:
        resource.beforeCheckpoint(null)
        resource.afterRestore(null)

        then:
        noExceptionThrown()
    }
}
