package indi.haorui.authorization.server.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by Yang Hao.rui on 2024/6/6
 */
@RestController
@RequestMapping("/oauth2/v1")
public class OAuth2Controller {

    @PostMapping("token")
    public ResponseEntity<Void> token(){
        return ResponseEntity.ok().build();
    }


    @PostMapping("authorize")
    public ResponseEntity<Void> authorize(){
        return ResponseEntity.ok().build();
    }

}
