package indi.haorui.securityclient.config;

import indi.haorui.securityclient.filter.CustomizedAuthenticationFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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

public class SecurityConfig {



    @Configuration
    @EnableWebSecurity
    @EnableMethodSecurity
    @ConditionalOnExpression("'${spring.security.authentication.enable}'.equals('false')")
    public static class AuthenticationSecurityConfig {



        @Bean
        protected SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(
                    authorize -> authorize
                            .requestMatchers("/api/v2/**").authenticated()
                            .anyRequest().permitAll()
            ).oauth2ResourceServer(
                    configure -> configure.jwt(
                            jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(jwtAuthenticationConverter())
                    )
            ).csrf(AbstractHttpConfigurer::disable);
            return http.build();
        }

        // 另外使用 appid 作为 principal，用于记录是哪个客户服务发起的请求。
        @Bean
        public JwtAuthenticationConverter jwtAuthenticationConverter() {

            JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
            grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
            grantedAuthoritiesConverter.setAuthorityPrefix("");

            JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
            jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
            jwtAuthenticationConverter.setPrincipalClaimName("appid");
            return jwtAuthenticationConverter;
        }
    }

    @Configuration
    @ConditionalOnExpression("'${spring.security.customized.authentication.enable}'.equals('true')")
    public static class PermitSecurityConfig {

        @Bean
        protected SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(
                            authorize -> authorize
                                    .requestMatchers("/api/v2/**").authenticated()
                                    .anyRequest().permitAll()
                    );
            http.addFilterAfter(
                    customizedAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class
            );
                http.oauth2ResourceServer(
                        configure -> configure.jwt(
                                jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );

            return http.build();
        }

        @Bean
        public CustomizedAuthenticationFilter customizedAuthenticationFilter() {
            return new CustomizedAuthenticationFilter();
        }

        // 对JWT内容进行解析
        @Bean
        public JwtAuthenticationConverter jwtAuthenticationConverter() {
            JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
            grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
            grantedAuthoritiesConverter.setAuthorityPrefix("");

            JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
            jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
            jwtAuthenticationConverter.setPrincipalClaimName("appid");
            return jwtAuthenticationConverter;
        }

    }



}
