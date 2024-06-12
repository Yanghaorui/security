package indi.haorui.resource.server.dto;

import lombok.Data;

/**
 * Created by Yang Hao.rui on 2024/6/5
 */
@Data
public class TokenRequestDTO {

    private String client_id;
    private String client_secret;
    private String code;
    private String redirect_uri;

}
