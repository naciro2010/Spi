<#import "template.ftl" as layout>

<@layout.registrationLayout displayMessage=!messagesPerField.existsError('firstName','lastName','email','username','password','password-confirm'); section>

    <#if section = "header">
        ${msg("registerTitle")}

    <#elseif section = "form">
        <form id="kc-register-form" action="${url.registrationAction}" method="post">

            <#-- First name -->
            <div class="goldcar-field">
                <label for="firstName" class="goldcar-label">${msg("firstName")}</label>
                <input id="firstName" class="goldcar-input" name="firstName" type="text"
                       value="${(register.formData.firstName!'')}"
                       aria-invalid="<#if messagesPerField.existsError('firstName')>true</#if>"
                       placeholder="Juan" />
                <#if messagesPerField.existsError('firstName')>
                    <span class="goldcar-error">${kcSanitize(messagesPerField.getFirstError('firstName'))?no_esc}</span>
                </#if>
            </div>

            <#-- Last name -->
            <div class="goldcar-field">
                <label for="lastName" class="goldcar-label">${msg("lastName")}</label>
                <input id="lastName" class="goldcar-input" name="lastName" type="text"
                       value="${(register.formData.lastName!'')}"
                       aria-invalid="<#if messagesPerField.existsError('lastName')>true</#if>"
                       placeholder="Garcia" />
                <#if messagesPerField.existsError('lastName')>
                    <span class="goldcar-error">${kcSanitize(messagesPerField.getFirstError('lastName'))?no_esc}</span>
                </#if>
            </div>

            <#-- Email -->
            <div class="goldcar-field">
                <label for="email" class="goldcar-label">${msg("email")}</label>
                <input id="email" class="goldcar-input" name="email" type="email"
                       value="${(register.formData.email!'')}"
                       autocomplete="email"
                       aria-invalid="<#if messagesPerField.existsError('email')>true</#if>"
                       placeholder="you@example.com" />
                <#if messagesPerField.existsError('email')>
                    <span class="goldcar-error">${kcSanitize(messagesPerField.getFirstError('email'))?no_esc}</span>
                </#if>
            </div>

            <#-- Username (hidden if email is username) -->
            <#if !realm.registrationEmailAsUsername>
                <div class="goldcar-field">
                    <label for="username" class="goldcar-label">${msg("username")}</label>
                    <input id="username" class="goldcar-input" name="username" type="text"
                           value="${(register.formData.username!'')}"
                           autocomplete="username"
                           aria-invalid="<#if messagesPerField.existsError('username')>true</#if>" />
                    <#if messagesPerField.existsError('username')>
                        <span class="goldcar-error">${kcSanitize(messagesPerField.getFirstError('username'))?no_esc}</span>
                    </#if>
                </div>
            </#if>

            <#-- Password -->
            <div class="goldcar-field">
                <label for="password" class="goldcar-label">${msg("password")}</label>
                <input id="password" class="goldcar-input" name="password" type="password"
                       autocomplete="new-password"
                       aria-invalid="<#if messagesPerField.existsError('password','password-confirm')>true</#if>" />
                <#if messagesPerField.existsError('password')>
                    <span class="goldcar-error">${kcSanitize(messagesPerField.getFirstError('password'))?no_esc}</span>
                </#if>
            </div>

            <#-- Confirm password -->
            <div class="goldcar-field">
                <label for="password-confirm" class="goldcar-label">${msg("passwordConfirm")}</label>
                <input id="password-confirm" class="goldcar-input" name="password-confirm" type="password"
                       autocomplete="new-password"
                       aria-invalid="<#if messagesPerField.existsError('password-confirm')>true</#if>" />
                <#if messagesPerField.existsError('password-confirm')>
                    <span class="goldcar-error">${kcSanitize(messagesPerField.getFirstError('password-confirm'))?no_esc}</span>
                </#if>
            </div>

            <#-- Register button -->
            <div class="goldcar-actions">
                <input type="submit" class="goldcar-btn goldcar-btn-primary" value="${msg("doRegister")}" />
            </div>

            <#-- Back to login -->
            <div class="goldcar-register">
                <span class="goldcar-register-text">${msg("backToLogin")!""}</span>
                <a href="${url.loginUrl}" class="goldcar-btn goldcar-btn-outline">
                    ${msg("doLogIn")}
                </a>
            </div>

        </form>
    </#if>

</@layout.registrationLayout>
