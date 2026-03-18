<#import "template.ftl" as layout>

<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=realm.password && realm.registrationAllowed; section>

    <#if section = "header">
        ${msg("loginAccountTitle")}

    <#elseif section = "form">

        <#-- ==================== Google SSO Button ==================== -->
        <#if social.providers?? && social.providers?size gt 0>
            <div id="kc-social-providers" class="goldcar-social">
                <#list social.providers as p>
                    <a id="social-${p.alias}" class="goldcar-btn goldcar-btn-google" href="${p.loginUrl}">
                        <svg width="20" height="20" viewBox="0 0 24 24" style="margin-right: 10px; vertical-align: middle;">
                            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z"/>
                            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                        </svg>
                        Continue with Google
                    </a>
                </#list>

                <div class="goldcar-separator">
                    <span>or</span>
                </div>
            </div>
        </#if>

        <#-- ==================== Login Form ==================== -->
        <#if realm.password>
            <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">

                <#-- Email field -->
                <div class="goldcar-field">
                    <label for="username" class="goldcar-label">
                        <#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if>
                    </label>
                    <input id="username" class="goldcar-input" name="username" type="text"
                           value="${(login.username!'')}"
                           autofocus autocomplete="username"
                           aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                           placeholder="you@example.com" />
                    <#if messagesPerField.existsError('username','password')>
                        <span class="goldcar-error">${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}</span>
                    </#if>
                </div>

                <#-- Password field -->
                <div class="goldcar-field">
                    <label for="password" class="goldcar-label">${msg("password")}</label>
                    <input id="password" class="goldcar-input" name="password" type="password"
                           autocomplete="current-password"
                           aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>" />
                </div>

                <#-- Remember me + Forgot password row -->
                <div class="goldcar-options">
                    <#if realm.rememberMe && !usernameHidden??>
                        <label class="goldcar-checkbox">
                            <input id="rememberMe" name="rememberMe" type="checkbox"
                                   <#if login.rememberMe??>checked</#if>>
                            ${msg("rememberMe")}
                        </label>
                    </#if>

                    <#if realm.resetPasswordAllowed>
                        <a href="${url.loginResetCredentialsUrl}" class="goldcar-link-forgot">
                            ${msg("doForgotPassword")}
                        </a>
                    </#if>
                </div>

                <#-- Sign In button -->
                <div class="goldcar-actions">
                    <input name="login" id="kc-login" type="submit" class="goldcar-btn goldcar-btn-primary" value="${msg("doLogIn")}" />
                </div>

            </form>
        </#if>

        <#-- ==================== Create Account Button ==================== -->
        <#if realm.password && realm.registrationAllowed>
            <div class="goldcar-register">
                <span class="goldcar-register-text">${msg("noAccount")}</span>
                <a href="${url.registrationUrl}" class="goldcar-btn goldcar-btn-secondary">
                    ${msg("doRegister")}
                </a>
            </div>
        </#if>

    </#if>

</@layout.registrationLayout>
