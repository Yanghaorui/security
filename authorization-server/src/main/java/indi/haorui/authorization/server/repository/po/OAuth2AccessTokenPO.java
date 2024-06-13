package indi.haorui.authorization.server.repository.po;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Created by Yang Hao.rui on 2024/6/13
 */
@Data
@NoArgsConstructor
public class OAuth2AccessTokenPO {

    private String tokenType;

    private Set<String> scopes;

    private String tokenValue;

    private Instant issuedAt;

    private Instant expiresAt;

    private Map<String, Object> metadata;

    public OAuth2AccessTokenPO(OAuth2AccessToken accessToken, Map<String, Object> metadata) {
        OAuth2AccessToken.TokenType type = accessToken.getTokenType();
        if (Objects.nonNull(type)) {
            this.tokenType = type.getValue();
        }
        this.scopes = accessToken.getScopes();
        this.tokenValue = accessToken.getTokenValue();
        this.issuedAt = accessToken.getIssuedAt();
        this.expiresAt = accessToken.getExpiresAt();
        this.metadata = metadata;
    }


    public OAuth2AccessToken toOauth2AccessToken() {

        return new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, this.tokenValue, this.issuedAt, this.expiresAt, this.scopes);
    }

}
