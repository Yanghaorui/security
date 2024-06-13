package indi.haorui.authorization.server.repository.po;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Created by Yang Hao.rui on 2024/6/13
 */
@Data
@NoArgsConstructor
public class OAuth2AuthorizationPO {

    private String id;
    private String registeredClientId;
    private String principalName;
    private String authorizationGrantType;
    private Set<String> authorizedScopes;
    private OAuth2AccessTokenPO oAuth2AccessToken;
    private OAuth2RefreshTokenPO oAuth2RefreshToken;
    private Map<String, Object> attributes;

    public OAuth2AuthorizationPO(OAuth2Authorization authorization) {
        this.id = authorization.getId();
        this.registeredClientId = authorization.getRegisteredClientId();
        this.principalName = authorization.getPrincipalName();
        AuthorizationGrantType authorizationGrantType = authorization.getAuthorizationGrantType();
        if (Objects.nonNull(authorizationGrantType)){
            this.authorizationGrantType = authorizationGrantType.getValue();
        }

        this.authorizedScopes = authorization.getAuthorizedScopes();
        this.attributes = authorization.getAttributes();
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
        if (Objects.nonNull(accessToken)){
            OAuth2AccessToken token = accessToken.getToken();
            Map<String, Object> metadata = accessToken.getMetadata();
            if (Objects.nonNull(token)){
                this.oAuth2AccessToken = new OAuth2AccessTokenPO(token, metadata);
            }
        }
        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization.getRefreshToken();
        if (Objects.nonNull(refreshToken)){
            OAuth2RefreshToken token = refreshToken.getToken();
            Map<String, Object> metadata = refreshToken.getMetadata();
            this.oAuth2RefreshToken = new OAuth2RefreshTokenPO(token, metadata);
        }
    }

    public OAuth2Authorization toOauth2Authorization(RegisteredClient registeredClient) {
        OAuth2Authorization.Builder builder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .id(this.id)
                .principalName(this.principalName)
                .authorizationGrantType(new AuthorizationGrantType(this.authorizationGrantType))
                .authorizedScopes(this.authorizedScopes)
                .attributes((attrs) -> attrs.putAll(attributes));
        if (Objects.nonNull(this.oAuth2AccessToken)){
            OAuth2AccessToken token = this.oAuth2AccessToken.toOauth2AccessToken();
            builder.accessToken(token);
        }
        if (Objects.nonNull(this.oAuth2RefreshToken)){
            OAuth2RefreshToken token = this.oAuth2RefreshToken.toOauth2RefreshToken();
            builder.refreshToken(token);
        }
        return builder.build();
    }

}
