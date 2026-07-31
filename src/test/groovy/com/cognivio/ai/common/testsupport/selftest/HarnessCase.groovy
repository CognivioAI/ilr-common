package com.cognivio.ai.common.testsupport.selftest

import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * A tenant-scoped row, standing in for the {@code cases} / {@code users} / {@code review_items}
 * tables of the real services.
 *
 * <p>Its only job is to be mapped against the Flyway-created {@code harness_cases} table under
 * {@code spring.jpa.hibernate.ddl-auto=validate}. That makes the mapping itself an assertion: if the
 * migration and this class disagree on a table or column name, the context fails to start and
 * {@code ApplicationContextIT} fails. In a real service that same mechanism covers every entity at
 * once, which is why the harness restates {@code validate} rather than trusting the service default.
 */
@Entity
@Table(name = 'harness_cases')
@Access(AccessType.FIELD)
class HarnessCase {

    @Id
    @Column(name = 'id', nullable = false)
    UUID id

    /** The RLS discriminator. The policy in V1 compares this against {@code app.current_tenant_id}. */
    @Column(name = 'tenant_id', nullable = false)
    UUID tenantId

    @Column(name = 'name', nullable = false)
    String name

    /** Set by the sign-off endpoint, so a write path is exercised under RLS as well as a read path. */
    @Column(name = 'signed_off_by')
    UUID signedOffBy
}
