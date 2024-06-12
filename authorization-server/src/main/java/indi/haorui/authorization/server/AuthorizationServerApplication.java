package indi.haorui.authorization.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Created by Yang Hao.rui on 2024/6/6
 */
@SpringBootApplication
@EnableConfigurationProperties
@ConfigurationPropertiesScan
public class AuthorizationServerApplication {

        public static void main(String[] args) {
            SpringApplication.run(AuthorizationServerApplication.class, args);
        }
}
