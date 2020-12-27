/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.sample.post.authn.handler;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.client.utils.URIBuilder;
import org.wso2.carbon.identity.application.authentication.framework.config.ConfigurationFacade;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.PostAuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.handler.request.AbstractPostAuthnHandler;
import org.wso2.carbon.identity.application.authentication.framework.handler.request.PostAuthnHandlerFlowStatus;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RememberMePostAuthnHandler extends AbstractPostAuthnHandler {
    private static final String REMEMBER_ME_PROMPTED = "rememberMePrompted";

    @Override
    public PostAuthnHandlerFlowStatus handle(HttpServletRequest request,
                                             HttpServletResponse response,
                                             AuthenticationContext context)
            throws PostAuthenticationFailedException {

        AuthenticatedUser authenticatedUser = getAuthenticatedUser(context);
        if (authenticatedUser == null) {
            return PostAuthnHandlerFlowStatus.SUCCESS_COMPLETED;
        }

        if (context.isPreviousSessionFound()) {
            return PostAuthnHandlerFlowStatus.SUCCESS_COMPLETED;
        }

        if (isRememberMePrompted(context)) {
            return handleRememberMe(request, response, context);
        } else {
            redirectToRememberMePage(response, context);
            setRememberMePoppedUpState(context);
            return PostAuthnHandlerFlowStatus.INCOMPLETE;
        }
    }

    private void redirectToRememberMePage(HttpServletResponse response, AuthenticationContext authenticationContext)
            throws PostAuthenticationFailedException {

        URIBuilder uriBuilder;
        try {
            uriBuilder = getUriBuilder(authenticationContext);
            response.sendRedirect(uriBuilder.build().toString());
        } catch (IOException e) {
            throw new PostAuthenticationFailedException(
                    "Authentication failed. Error while processing remember me " +
                            "requirements.", "Error while redirecting to remember me page.", e
            );
        } catch (URISyntaxException e) {
            throw new PostAuthenticationFailedException(
                    "Authentication failed. Error while processing remember me " +
                            "requirements.", "Error while building redirect URI.", e
            );
        }
    }

    private URIBuilder getUriBuilder(AuthenticationContext context) throws URISyntaxException {
        final String LOGIN_ENDPOINT = "login.do";
        final String REMEMBER_ME_ENDPOINT = "remember_me.jsp";

        String REMEMBER_ME_ENDPOINT_URL = ConfigurationFacade.getInstance().getAuthenticationEndpointURL()
                .replace(LOGIN_ENDPOINT, REMEMBER_ME_ENDPOINT);

        URIBuilder uriBuilder;
        uriBuilder = new URIBuilder(REMEMBER_ME_ENDPOINT_URL);

        uriBuilder.addParameter(FrameworkConstants.SESSION_DATA_KEY,
                context.getContextIdentifier());
        uriBuilder.addParameter(FrameworkConstants.REQUEST_PARAM_SP,
                context.getSequenceConfig().getApplicationConfig().getApplicationName());

        return uriBuilder;
    }

    private void setRememberMePoppedUpState(AuthenticationContext authenticationContext) {
        authenticationContext.addParameter(REMEMBER_ME_PROMPTED, true);
    }

    private boolean isRememberMePrompted(AuthenticationContext authenticationContext) {
        return authenticationContext.getParameter(REMEMBER_ME_PROMPTED) != null;
    }

    private AuthenticatedUser getAuthenticatedUser(AuthenticationContext authenticationContext) {
        return authenticationContext.getSequenceConfig().getAuthenticatedUser();
    }

    protected PostAuthnHandlerFlowStatus handleRememberMe(HttpServletRequest request,
                                                          HttpServletResponse response,
                                                          AuthenticationContext context)
            throws PostAuthenticationFailedException {

        try {
            if (context.isRememberMe()) {
                return PostAuthnHandlerFlowStatus.SUCCESS_COMPLETED;
            }

            final String REMEMBER_ME_OPT_ON = "on";

            String rememberMe = request.getParameter(FrameworkConstants.RequestParams.REMEMBER_ME);

            context.setRememberMe(REMEMBER_ME_OPT_ON.equalsIgnoreCase(rememberMe));

            return PostAuthnHandlerFlowStatus.SUCCESS_COMPLETED;
        } catch (Exception e) {
            throw new PostAuthenticationFailedException(
                    "Authentication Failed",
                    "Something went wrong, please try again", e);
        }

    }
}
