package indi.haorui.resource.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Created by Yang Hao.rui on 2024/5/20
 *
 * <p></p>
 * 根据配置文件中的 spring.security.authentication.enable 来决定是启用默认的还是自定义的
 * <p></p>
 * true: 启用自定义的
 * <p></p>
 * false: 默认的，并且在这里还 <b>EnableMethodSecurity</b>
 *
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authorize) -> authorize
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer((oauth2) -> oauth2
                        .jwt(Customizer.withDefaults())
                );
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return JwtDecoders.fromIssuerLocation("http://localhost:9001");
    }

    /*
     * Spring Security does not provide an endpoint for minting tokens.
     * However, Spring Security does provide the JwtEncoder interface along with one implementation,
     * which is NimbusJwtEncoder.
     */
//    @Bean
//    public JwtDecoder jwtDecoder(OAuth2ResourceServerProperties properties) throws IOException {
//        Resource publicKeyLocation = properties.getJwt().getPublicKeyLocation();
//
//        return NimbusJwtDecoder.withPublicKey(s).build();
//    }
//
//    private RSAPublicKey publicKey() {
//
//        // ...
//    }

//    @Configuration
//    @EnableWebSecurity
//    @EnableMethodSecurity
//    @ConditionalOnExpression("'${spring.security.authentication.enable}'.equals('false')")
//    public static class AuthenticationSecurityConfig {
//
//
//
//        @Bean
//        protected SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//            http.authorizeHttpRequests(
//                    authorize -> authorize
//                            .requestMatchers("/api/v2/**").authenticated()
//                            .anyRequest().permitAll()
//            ).oauth2ResourceServer(
//                    configure -> configure.jwt(
//                            jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(jwtAuthenticationConverter())
//                    )
//            ).csrf(AbstractHttpConfigurer::disable);
//            return http.build();
//        }
//
//        // 另外使用 appid 作为 principal，用于记录是哪个客户服务发起的请求。
//        @Bean
//        public JwtAuthenticationConverter jwtAuthenticationConverter() {
//
//            JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
//            grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
//            grantedAuthoritiesConverter.setAuthorityPrefix("");
//
//            JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
//            jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
//            jwtAuthenticationConverter.setPrincipalClaimName("appid");
//            return jwtAuthenticationConverter;
//        }
//    }
//
//    @Configuration
//    @ConditionalOnExpression("'${spring.security.customized.authentication.enable}'.equals('true')")
//    public static class PermitSecurityConfig {
//
//        @Bean
//        protected SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//            http.csrf(AbstractHttpConfigurer::disable)
//                    .authorizeHttpRequests(
//                            authorize -> authorize
//                                    .requestMatchers("/api/v2/**").authenticated()
//                                    .anyRequest().permitAll()
//                    );
//            http.addFilterAfter(
//                    customizedAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class
//            );
//                http.oauth2ResourceServer(
//                        configure -> configure.jwt(
//                                jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(jwtAuthenticationConverter())
//                        )
//                );
//
//            return http.build();
//        }
//
//        @Bean
//        public CustomizedAuthenticationFilter customizedAuthenticationFilter() {
//            return new CustomizedAuthenticationFilter();
//        }
//
//        // 对JWT内容进行解析
//        @Bean
//        public JwtAuthenticationConverter jwtAuthenticationConverter() {
//            JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
//            grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
//            grantedAuthoritiesConverter.setAuthorityPrefix("");
//
//            JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
//            jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
//            jwtAuthenticationConverter.setPrincipalClaimName("appid");
//            return jwtAuthenticationConverter;
//        }
//
//    }



}
