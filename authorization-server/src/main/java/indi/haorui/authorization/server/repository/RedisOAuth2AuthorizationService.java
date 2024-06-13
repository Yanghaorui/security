package indi.haorui.authorization.server.repository;

import cn.hutool.core.collection.CollUtil;
import indi.haorui.authorization.server.repository.po.OAuth2AuthorizationPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2DeviceCode;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2UserCode;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Created by Yang Hao.rui on 2024/6/12
 */
@Slf4j
@RequiredArgsConstructor
public class RedisOAuth2AuthorizationService implements OAuth2AuthorizationService {

    /*
        OAuth2AccessToken 无法被反序列化，需要自定义序列化器
     */
    private final RedisTemplate<String, OAuth2AuthorizationPO> redisTemplate;

    private final RegisteredClientRepository registeredClientRepository;

    private final static String PREFIX = "access:token:";

    @Override
    public void save(OAuth2Authorization authorization) {
        OAuth2AccessToken token = authorization.getAccessToken().getToken();
        Duration duration = Duration.ofMillis(Objects.requireNonNull(token.getExpiresAt()).toEpochMilli() - System.currentTimeMillis());
        redisTemplate.opsForValue().set(PREFIX + authorization.getId(), new OAuth2AuthorizationPO(authorization), duration);
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        redisTemplate.delete(PREFIX + authorization.getId());
    }

    @Override
    public OAuth2Authorization findById(String id) {
        return this.get(PREFIX + id);
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        Set<String> keys = redisTemplate.keys(PREFIX + "*");
        if (CollUtil.isEmpty(keys)){
            return null;
        }
        Optional<OAuth2Authorization> any = keys.stream()
                .map(this::get)
                .filter(authorization -> {
                    if (Objects.isNull(authorization)) {
                        return false;
                    }
                    OAuth2AccessToken accessToken = authorization.getAccessToken().getToken();
                    return Objects.nonNull(accessToken) && hasToken(authorization, token, tokenType);
                }).findAny();
        return any.orElse(null);
    }

    private OAuth2Authorization get(String key){
        try {
            OAuth2AuthorizationPO oAuth2AuthorizationPo = redisTemplate.opsForValue().get(key);
            if (Objects.isNull(oAuth2AuthorizationPo)){
                return null;
            }
            RegisteredClient registeredClient = registeredClientRepository.findByClientId(oAuth2AuthorizationPo.getRegisteredClientId());
            return oAuth2AuthorizationPo.toOauth2Authorization(registeredClient);
        } catch (Exception e){
            log.error("",e);
        }
        return null;
    }


    private static boolean hasToken(OAuth2Authorization authorization, String token, @Nullable OAuth2TokenType tokenType) {
        if (tokenType == null) {
            return matchesState(authorization, token) ||
                    matchesAuthorizationCode(authorization, token) ||
                    matchesAccessToken(authorization, token) ||
                    matchesIdToken(authorization, token) ||
                    matchesRefreshToken(authorization, token) ||
                    matchesDeviceCode(authorization, token) ||
                    matchesUserCode(authorization, token);
        } else if (OAuth2ParameterNames.STATE.equals(tokenType.getValue())) {
            return matchesState(authorization, token);
        } else if (OAuth2ParameterNames.CODE.equals(tokenType.getValue())) {
            return matchesAuthorizationCode(authorization, token);
        } else if (OAuth2TokenType.ACCESS_TOKEN.equals(tokenType)) {
            return matchesAccessToken(authorization, token);
        } else if (OidcParameterNames.ID_TOKEN.equals(tokenType.getValue())) {
            return matchesIdToken(authorization, token);
        } else if (OAuth2TokenType.REFRESH_TOKEN.equals(tokenType)) {
            return matchesRefreshToken(authorization, token);
        } else if (OAuth2ParameterNames.DEVICE_CODE.equals(tokenType.getValue())) {
            return matchesDeviceCode(authorization, token);
        } else if (OAuth2ParameterNames.USER_CODE.equals(tokenType.getValue())) {
            return matchesUserCode(authorization, token);
        }
        return false;
    }

    private static boolean matchesState(OAuth2Authorization authorization, String token) {
        return token.equals(authorization.getAttribute(OAuth2ParameterNames.STATE));
    }

    private static boolean matchesAuthorizationCode(OAuth2Authorization authorization, String token) {
        OAuth2Authorization.Token<OAuth2AuthorizationCode> authorizationCode =
                authorization.getToken(OAuth2AuthorizationCode.class);
        return authorizationCode != null && authorizationCode.getToken().getTokenValue().equals(token);
    }

    private static boolean matchesAccessToken(OAuth2Authorization authorization, String token) {
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken =
                authorization.getToken(OAuth2AccessToken.class);
        return accessToken != null && accessToken.getToken().getTokenValue().equals(token);
    }

    private static boolean matchesRefreshToken(OAuth2Authorization authorization, String token) {
        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken =
                authorization.getToken(OAuth2RefreshToken.class);
        return refreshToken != null && refreshToken.getToken().getTokenValue().equals(token);
    }

    private static boolean matchesIdToken(OAuth2Authorization authorization, String token) {
        OAuth2Authorization.Token<OidcIdToken> idToken =
                authorization.getToken(OidcIdToken.class);
        return idToken != null && idToken.getToken().getTokenValue().equals(token);
    }

    private static boolean matchesDeviceCode(OAuth2Authorization authorization, String token) {
        OAuth2Authorization.Token<OAuth2DeviceCode> deviceCode =
                authorization.getToken(OAuth2DeviceCode.class);
        return deviceCode != null && deviceCode.getToken().getTokenValue().equals(token);
    }

    private static boolean matchesUserCode(OAuth2Authorization authorization, String token) {
        OAuth2Authorization.Token<OAuth2UserCode> userCode =
                authorization.getToken(OAuth2UserCode.class);
        return userCode != null && userCode.getToken().getTokenValue().equals(token);
    }
}
