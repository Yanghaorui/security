package indi.haorui.resource.server.config;

import cn.hutool.core.util.StrUtil;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import java.util.Map;
import java.util.Objects;

/**
 * Created by Yang Hao.rui on 2024/5/21
 */
//@Component // 加载到spring容器中会导致所有的feign client都会加载这个配置，所以不要加，我们在@FeignClient中指定了配置类
@Slf4j
public class FeignClientConfig {

    @Bean
    RequestInterceptor requestInterceptor(OAuth2AccessTokenManager oAuth2AccessTokenManager) {

        // 从 authorizedClient 中获取 access token，添加到请求的 header 中。
        return requestTemplate -> {
            try {
                if (Objects.isNull(requestTemplate.feignTarget()) || StrUtil.isBlank(requestTemplate.feignTarget().name())) {
                    return;
                }
                String name = requestTemplate.feignTarget().name();
                String accessToken = oAuth2AccessTokenManager.authorize(name);
                requestTemplate.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
                log.debug("core accessToken: {}", accessToken);
            } catch (Exception e) {
                log.error("core accessToken error: {}", e.getMessage());
            }
        };
    }


    @Bean
    @ConditionalOnMissingBean
    public OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository clientRegistrationRepository,
                                                                 OAuth2AuthorizedClientService authorizedClientService) {

        OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository, authorizedClientService);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }

    @Bean
    @ConditionalOnMissingBean
    public OAuth2AccessTokenManager oAuth2AccessTokenManager(ClientRegistrationRepository clientRegistrationRepository,
                                                             OAuth2AuthorizedClientManager authorizedClientManager) {
        return new OAuth2AccessTokenManager(clientRegistrationRepository, authorizedClientManager);
    }

    @Bean
    @ConditionalOnMissingBean
    ClientRegistrationRepository clientRegistrationRepository(OAuth2ClientProperties properties) {
        OAuth2ClientPropertiesMapper propertiesMapper = new OAuth2ClientPropertiesMapper(properties);
        Map<String, ClientRegistration> registrations = propertiesMapper.asClientRegistrations();
        return new InMemoryClientRegistrationRepository(registrations);
    }



}
