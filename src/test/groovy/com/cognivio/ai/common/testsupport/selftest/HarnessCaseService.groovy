package com.cognivio.ai.common.testsupport.selftest

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The transactional boundary, mirroring the estate's controller → service → repository layering.
 *
 * <p>The layering matters to the harness rather than being incidental: {@code RlsTenantBindingAspect}
 * advises {@code @Transactional} methods and binds {@code app.current_tenant_id} on the
 * transaction-bound connection. Putting {@code @Transactional} here — not on the controller — is
 * what makes the tenant binding happen on the same connection the query then runs on, which is the
 * arrangement every real service already has.
 */
@Service
class HarnessCaseService {

    private final HarnessCaseRepository repository

    HarnessCaseService(HarnessCaseRepository repository) {
        this.repository = repository
    }

    /** Every case visible to the caller — i.e. whatever RLS lets through, with no application filter. */
    @Transactional(readOnly = true)
    List<HarnessCase> visibleCases() {
        repository.findAllByOrderByNameAsc()
    }

    /**
     * Records a sign-off, or returns {@code false} if the row is not visible to the caller's tenant.
     *
     * <p>The read that decides "not visible" is itself RLS-filtered, so a cross-tenant sign-off
     * attempt is indistinguishable from a sign-off of a row that does not exist — which is the
     * behaviour we want, since a 404 leaks less than a 403.
     */
    @Transactional
    boolean signOff(UUID caseId, UUID signedOffBy) {
        HarnessCase target = repository.findById(caseId).orElse(null)
        if (target == null) {
            return false
        }
        target.signedOffBy = signedOffBy
        repository.save(target)
        return true
    }
}
