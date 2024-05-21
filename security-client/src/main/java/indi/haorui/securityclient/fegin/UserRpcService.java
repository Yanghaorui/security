package indi.haorui.securityclient.fegin;

import indi.haorui.securityclient.config.FeignClientConfig;
import indi.haorui.securityclient.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;

import java.util.Collection;

/**
 * Created by Yang Hao.rui on 2024/5/21
 */
@FeignClient(name = "security-service", url = "http://security-service", contextId = "UserRpcService", configuration = {FeignClientConfig.class})
public interface UserRpcService {

    ResponseEntity<Collection<UserDTO>> list();

}
