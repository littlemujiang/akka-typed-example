package com.example.akka.actor;

import akka.actor.typed.ActorRef;
import lombok.Builder;
import lombok.Data;

import java.beans.ConstructorProperties;
import java.io.Serializable;

/**
 * Simple to Introduction
 * className: BehaviorConfig
 *
 * @author mujiang
 * @version 2021/05/25 15:27
 */
@Data
public class BehaviorConfig implements Serializable{
    private String expression;
    public BehaviorConfig(String operation) {
        this.expression = operation;
    }
}
