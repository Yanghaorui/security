package indi.haorui.authorization.server.repository.po;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;

import java.time.Instant;
import java.util.Map;

/**
 * Created by Yang Hao.rui on 2024/6/13
 */
@Data
@NoArgsConstructor
public class OAuth2RefreshTokenPO {
    private String tokenValue;

    private Instant issuedAt;

    private Instant expiresAt;

    private Map<String, Object> metadata;

    public OAuth2RefreshTokenPO(OAuth2RefreshToken refreshToken, Map<String, Object> metadata) {
        this.tokenValue = refreshToken.getTokenValue();
        this.issuedAt = refreshToken.getIssuedAt();
        this.expiresAt = refreshToken.getExpiresAt();
        this.metadata = metadata;
    }

    public OAuth2RefreshToken toOauth2RefreshToken() {
        return new OAuth2RefreshToken(this.tokenValue, this.issuedAt, this.expiresAt);
    }
}
