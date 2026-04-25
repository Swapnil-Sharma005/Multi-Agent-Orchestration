package org.example.multiagentorchestration.model;

public class Message {

    String role;
    String content;

    public Message(String role, String content){
        this.role = role;
        this.content = content;
    }
}
