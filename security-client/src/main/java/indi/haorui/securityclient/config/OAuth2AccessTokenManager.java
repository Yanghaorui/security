package indi.haorui.securityclient.config;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.util.Objects;

/**
 * Created by Yang Hao.rui on 2023/11/10
 */
@Slf4j
public class OAuth2AccessTokenManager {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public OAuth2AccessTokenManager(ClientRegistrationRepository clientRegistrationRepository,
                                    OAuth2AuthorizedClientManager authorizedClientManager) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.authorizedClientManager = authorizedClientManager;
    }

    public String authorize(String registrationId) {
        OAuth2AccessToken accessToken = AccessTokenRenovator.get(registrationId);
        if (Objects.isNull(accessToken)){
            return null;
        }
        if (StrUtil.isNotBlank(accessToken.getTokenValue())) {
            return accessToken.getTokenValue();
        }
        OAuth2AccessToken oAuth2AccessToken = AccessTokenRenovator.register(registrationId,
                this::authorizeToken);
        return oAuth2AccessToken.getTokenValue();
    }


    private OAuth2AccessToken authorizeToken(String registrationId) {
        // 启动时， clientRegistrationRepository 会从 application.yml 读取 clientRegistration。
        // 所以这里可以根据 CLIENT_REGISTRATION_ID 获取 clientRegistration，
        // 里面包含了 local-client 对应的 client_id， client_secret，provider 等。
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(registrationId);

        // 构造 OAuth2 认证请求，用于为 local-client 获取 access token。
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId(clientRegistration.getRegistrationId())
                .principal(clientRegistration.getClientId())
                .build();

        try {
            // 执行认证请求，认证成功后，会把 accessToken，refreshToken 等信息保存为 authorizedClient。
            OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);
            if (Objects.nonNull(authorizedClient)) {
                return authorizedClient.getAccessToken();
            }
        } catch (Exception e) {
            log.error("Failed to authorize for client: {}.", registrationId, e);
        }
        return null;
    }

}
