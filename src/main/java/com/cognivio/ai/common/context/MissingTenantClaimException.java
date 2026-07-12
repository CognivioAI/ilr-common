package com.cognivio.ai.common.context;

import com.cognivio.ai.common.web.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a request is authenticated with a verified JWT that nonetheless
 * carries no usable tenant claim. This is a server/token-provisioning fault
 * rather than normal client error, and — because tenant isolation (ADR-011)
 * depends on it — the request must be refused rather than served without a
 * tenant. Rendered as 403 by {@code CommonExceptionHandler} (it extends
 * {@link DomainException}).
 */
public class MissingTenantClaimException extends DomainException {

    public MissingTenantClaimException(String message) {
        super("TENANT_CLAIM_MISSING", message, HttpStatus.FORBIDDEN);
    }
}
