package com.example.chat.notifications;

public class Token {
    /*An FCM Token, or much commonly known as a registration.
    * An ID issued by the GCM connection servers to the client app that allows it to receive messages*/

    String token;

    public Token(String token) {
        this.token = token;
    }

    public Token() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
