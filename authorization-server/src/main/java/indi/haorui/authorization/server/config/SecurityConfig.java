package indi.haorui.authorization.server.config;

import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import indi.haorui.authorization.server.properties.RSAKeyProperties;
import indi.haorui.authorization.server.repository.RedisOAuth2AuthorizationService;
import indi.haorui.authorization.server.repository.po.OAuth2AuthorizationPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Created by Yang Hao.rui on 2024/6/6
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        return http.formLogin(Customizer.withDefaults()).build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain standardSecurityFilterChain(HttpSecurity http) throws Exception {
        // @formatter:off
        http
                .authorizeHttpRequests((authorize) -> authorize
                        .anyRequest().authenticated()
                )
                .formLogin(Customizer.withDefaults());
        // @formatter:on

        return http.build();
    }

//    @Bean
//    public RegisteredClientRepository registeredClientRepository() {
//        // @formatter:off
//        RegisteredClient loginClient = RegisteredClient.withId(UUID.randomUUID().toString())
//                .clientId("login-client")
//                .clientSecret("{noop}openid-connect")
//                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
//                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
//                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
//                .redirectUri("http://127.0.0.1:8080/login/oauth2/code/login-client")
//                .redirectUri("http://127.0.0.1:8080/authorized")
//                .scope(OidcScopes.OPENID)
//                .scope(OidcScopes.PROFILE)
//                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
//                .build();
//        RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
//                .clientId("messaging-client")
//                .clientSecret("{noop}secret")
//                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
//                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
//                .scope("message:read")
//                .scope("message:write")
//                .build();
//        // @formatter:on
//
//        return new InMemoryRegisteredClientRepository(loginClient, registeredClient);
//    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(RSAKeyProperties rsaKeyProperties) {
//        new InMemoryRegisteredClientRepository(loginClient, registeredClient);
        List<JWK> list = rsaKeyProperties.getKeys()
                .stream()
                .map(
                        k -> {
                            RSAKey build = new RSAKey.Builder(k.getPub())
                                    .privateKey(k.getPriv())
                                    .keyID(k.getId())
                                    .build();
                            return (JWK) build;
                        }
                )
                .toList();
        // @formatter:on

        JWKSet jwkSet = new JWKSet(list);
        return new ImmutableJWKSet<>(jwkSet);
    }

//    @Bean
//    public JwtDecoder jwtDecoder(KeyPair keyPair) {
//        return NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();
//    }

//    @Bean
//    public AuthorizationServerSettings providerSettings() {
//        return AuthorizationServerSettings.builder().issuer("http://localhost:9000").build();
//    }

    @Bean
    public UserDetailsService userDetailsService() {
        // @formatter:off
        UserDetails userDetails = User.withDefaultPasswordEncoder()
                .username("user")
                .password("password")
                .roles("USER")
                .build();
        // @formatter:on

        return new InMemoryUserDetailsManager(userDetails);
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(RedisTemplate<String, OAuth2AuthorizationPO> redisTemplate, RegisteredClientRepository registeredClientRepository) {
        return new RedisOAuth2AuthorizationService(redisTemplate, registeredClientRepository);
    }

    /**
     * 会注入到这里
     * {@link org.springframework.security.oauth2.server.authorization.token.JwtGenerator.jwtCustomizer}
     * context.getJwsHeader().keyId(jwks.get(0).getKeyID());
     * 这里set的keyid 最后会通过{@linkplain org.springframework.security.oauth2.server.authorization.token.JwtGenerator }到
     * {@link NimbusJwtEncoder#encode(JwtEncoderParameters)}
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer(JWKSource<SecurityContext> jwkSource){
        return context -> {
            JWKSelector jwkSelector = new JWKSelector(new JWKMatcher.Builder().build());
            try {
                List<JWK> jwks = jwkSource.get(jwkSelector, null);
                context.getJwsHeader().keyId(jwks.get(1).getKeyID());
            } catch (KeySourceException e) {
                throw new RuntimeException(e);
            }
        };
    }

}
