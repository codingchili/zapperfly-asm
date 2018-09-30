package com.codingchili.zapperflyasm.exceptions;

import com.codingchili.zapperflyasm.model.ApiRequest;

import com.codingchili.core.context.CoreException;

/**
 * @author Robin Duda
 * <p>
 * Thrown when a build has been requested for a repo/branch combination that
 * is not already configured.
 */
public class NotConfiguredException extends CoreException {

    /**
     * @param request the request that cause the exception.
     */
    public NotConfiguredException(ApiRequest request) {
        super("No configuration exists for repo " + request.getRepository() +
                " and branch " + request.getBranch());
    }
}
