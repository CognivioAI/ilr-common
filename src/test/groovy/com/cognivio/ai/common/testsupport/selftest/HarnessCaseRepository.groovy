package com.cognivio.ai.common.testsupport.selftest

import org.springframework.data.jpa.repository.JpaRepository

/**
 * Deliberately carries <b>no</b> tenant predicate on {@link #findAllByOrderByNameAsc}.
 *
 * <p>That is the point. Every row this returns is filtered by PostgreSQL itself, through the RLS
 * policy engaged by {@code RlsTenantBindingAspect}. If the harness's role split, the FORCE flag, the
 * policy or the tenant binding were wrong in any way, this query would return other tenants' rows and
 * {@code HarnessSecurityIT}'s cross-tenant feature would fail.
 *
 * <p>A repository like this in production would be review code P2-1 ("repository method omits the
 * tenant predicate"). Modelling it here is what lets the harness demonstrate it can catch that class
 * of defect — the estate's existing mocked-repository "tenant isolation" tests structurally cannot,
 * because a mock returns whatever it was told to.
 */
interface HarnessCaseRepository extends JpaRepository<HarnessCase, UUID> {

    List<HarnessCase> findAllByOrderByNameAsc()
}
