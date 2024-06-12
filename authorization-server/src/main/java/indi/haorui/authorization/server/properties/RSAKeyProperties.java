package indi.haorui.authorization.server.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

/**
 * Created by Yang Hao.rui on 2024/6/11
 */
@Data
@ConfigurationProperties(prefix = "jwt")
public class RSAKeyProperties {

    private List<Key> keys;

    @Data
    public static class Key{

        private String id;

        private RSAPrivateKey priv;

        private RSAPublicKey pub;
    }
}
