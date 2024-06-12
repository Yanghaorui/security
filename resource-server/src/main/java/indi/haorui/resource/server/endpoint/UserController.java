package indi.haorui.resource.server.endpoint;

import indi.haorui.resource.server.config.SecurityConfig;
import indi.haorui.resource.server.dto.UserDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.GetExchange;

import java.util.Collection;
import java.util.List;

/**
 * Created by Yang Hao.rui on 2024/5/20
 */
@RestController
public class UserController {


    /**
     * token中的roles必须带有 admin 角色才能访问
     * token的解析配置：
     * {@link SecurityConfig.AuthenticationSecurityConfig#jwtAuthenticationConverter()}
     * {@link SecurityConfig.PermitSecurityConfig#jwtAuthenticationConverter()}
     *
     * 另外 {@link SecurityConfig} 中需要配置 @EnableMethodSecurity
     */
    @GetExchange("/api/v1/users")
    @PreAuthorize("hasAnyRole('admin')")
    public ResponseEntity<Collection<UserDTO>> list(){
        return ResponseEntity.ok(List.of(new UserDTO("admin", "admin@go.com")));
    }

}
