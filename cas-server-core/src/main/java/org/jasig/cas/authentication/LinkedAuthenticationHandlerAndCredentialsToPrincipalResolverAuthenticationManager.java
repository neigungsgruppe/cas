/*
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.cas.authentication;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.jasig.cas.authentication.principal.Credentials;
import org.jasig.cas.authentication.principal.CredentialsToPrincipalResolver;
import org.jasig.cas.authentication.principal.Principal;

/**
 * Ensures that all authentication handlers are tried, but if one is tried,
 * the associated CredentialsToPrincipalResolver is used.
 *
 * @author Scott Battaglia
 * @author Marvin S. Addison
 * @since 3.3.5
 */
public class LinkedAuthenticationHandlerAndCredentialsToPrincipalResolverAuthenticationManager
            extends AbstractAuthenticationManager {

    @NotNull
    @Size(min = 1)
    private final Map<AuthenticationHandler, CredentialsToPrincipalResolver> linkedHandlers;

    public LinkedAuthenticationHandlerAndCredentialsToPrincipalResolverAuthenticationManager(
            final Map<AuthenticationHandler,CredentialsToPrincipalResolver> linkedHandlers) {
        this.linkedHandlers = linkedHandlers;
    }

    @Override
    protected Pair<AuthenticationHandler, Principal> authenticateAndObtainPrincipal(
            final Credentials credentials) throws AuthenticationException {

        final Map<String, Exception> errorMap = new LinkedHashMap<String, Exception>();
        String handlerName;

        for (final AuthenticationHandler authenticationHandler : this.linkedHandlers.keySet()) {
            if (!authenticationHandler.supports(credentials)) {
                continue;
            }

            handlerName = authenticationHandler.getName();
            HandlerResult result = null;
            try {
                result = authenticationHandler.authenticate(credentials);
            } catch (final Exception e) {
                logAuthenticationHandlerError(handlerName, credentials, e);
                errorMap.put(handlerName, e);
            }

            if (result != null) {
                log.info("{} successfully authenticated {}", handlerName, credentials);
                final Principal p = this.linkedHandlers.get(authenticationHandler).resolvePrincipal(credentials);
                if (p == null) {
                    throw new PrincipalException(
                            "Resolver failed to resolve principal.",
                            errorMap,
                            Collections.singletonMap(handlerName, result));
                }
                return new Pair<AuthenticationHandler,Principal>(authenticationHandler, p);
            }
            log.info("{} failed to authenticate {}", handlerName, credentials);
        }

        if (!errorMap.isEmpty()) {
            throw new AuthenticationException(errorMap);
        }
        throw new AuthenticationException();
    }
}
