package indi.haorui.securityclient.fegin;

import indi.haorui.securityclient.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * Created by Yang Hao.rui on 2024/5/21
 */
@FeignClient(name = "security-service", url = "http://security-service", contextId = "UserRpcService", configuration = {FeignClientConfig.class})
public interface UserRpcService {
}
