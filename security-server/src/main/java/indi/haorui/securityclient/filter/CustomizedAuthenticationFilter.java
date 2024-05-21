package indi.haorui.securityclient.filter;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Yang Hao.rui on 2024/1/18
 * <p>
 * Modified from {@link org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter}
 * Support JWT & UUID to authentication at the same time
 */
@Slf4j
public class CustomizedAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Autowired
    private JwtDecoder jwtDecoderByJwkKeySetUri;

    @Autowired
    private OAuth2ResourceServerProperties properties;

    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    private final AuthenticationEntryPoint authenticationEntryPoint = new BearerTokenAuthenticationEntryPoint();

    private final AuthenticationFailureHandler authenticationFailureHandler = new AuthenticationEntryPointFailureHandler(this.authenticationEntryPoint);

    private final BearerTokenResolver bearerTokenResolver = new DefaultBearerTokenResolver();

    private final AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new WebAuthenticationDetailsSource();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token;
        try {
            // 获取token
            token = this.bearerTokenResolver.resolve(request);
        } catch (OAuth2AuthenticationException invalid) {
            log.error("Sending to authentication entry point since failed to resolve bearer token", invalid);
            this.authenticationEntryPoint.commence(request, response, invalid);
            return;
        }

        if (StrUtil.isBlank(token)) {
            log.warn("Did not process request since did not find bearer token");
            filterChain.doFilter(request, response);
            return;
        }

        BearerTokenAuthenticationToken authenticationRequest = new BearerTokenAuthenticationToken(token);
        authenticationRequest.setDetails(this.authenticationDetailsSource.buildDetails(request));

        try {
            Authentication authenticate = this.getAuthenticationProvider(request).authenticate(authenticationRequest);
            // 获取到Authentication securityContextHolderStrategy中
            // 因为SecurityConfig 里面配置了 ("/api/v1/**").authenticated() ，意味着需要authentication 需要是验证通过的，authentication要放在securityContextHolderStrategy里面, 否则默认的匿名authentication对象通不过验证
            // 而且开启了MethodSecurity, 接口上方的@PreAuthorize(hasAnyAuthorize(`pii-read`)) 也需要把authentication放进 securityContextHolderStrategy 才有对应的authorize
            SecurityContext context = this.securityContextHolderStrategy.createEmptyContext();
            context.setAuthentication(authenticate);
            this.securityContextHolderStrategy.setContext(context);
//            this.securityContextRepository.saveContext(context, request, response);
            filterChain.doFilter(request, response);
        }
        catch (AuthenticationException failed) {
            log.error("Failed to process authentication request", failed);
            this.securityContextHolderStrategy.clearContext();
            authenticationFailureHandler.onAuthenticationFailure(request, response, failed);
        }
    }




    // 配置了JwkKeySetUri, 会通过public key 检验token 是否有效
    private AuthenticationProvider getAuthenticationProvider(HttpServletRequest request){
        String token = this.bearerTokenResolver.resolve(request);
        if (StrUtil.isNotBlank(token)){
            // jwtDecoderByJwkKeySetUri， 会判断时间
            JwtAuthenticationProvider jwtAuthenticationProvider = new JwtAuthenticationProvider(jwtDecoderByJwkKeySetUri);
            jwtAuthenticationProvider.setJwtAuthenticationConverter(jwtAuthenticationConverter);
            return jwtAuthenticationProvider;
        }
        // 构造了一个decoder，配置了JwtSetUri，会通过public key 检验token是否有效
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(this.properties.getJwt().getJwkSetUri()).build();
        // 构造一个不需要验证时间的 jwt decoder
        decoder.setJwtValidator(validators());
        JwtAuthenticationProvider jwtAuthenticationProvider = new JwtAuthenticationProvider(decoder);
        jwtAuthenticationProvider.setJwtAuthenticationConverter(jwtAuthenticationConverter);
        return jwtAuthenticationProvider;
    }

    /**
     * 自定义了issuer & audience 的 jwt 校验器
     * 默认的 jwt校验器会有时间，签名
     * @return issuer & audience 的 jwt 校验器
     */
    private OAuth2TokenValidator<Jwt> validators() {
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(new JwtIssuerValidator(this.properties.getJwt().getIssuerUri()));
        validators.add(new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                (aud) -> aud != null && !Collections.disjoint(aud, this.properties.getJwt().getAudiences())));
        return new DelegatingOAuth2TokenValidator<>(validators);
    }
}
