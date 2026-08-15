package com.smr.notify.websocket;

import java.security.Principal;

public class WebSocketUserPrincipal implements Principal {

    private final String userId;

    public WebSocketUserPrincipal(String userId) {
        this.userId = userId;
    }

    @Override
    public String getName() {
        return userId;
    }
}