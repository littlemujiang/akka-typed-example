package com.example.akka.actor;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class HelloWorldActor extends AbstractBehavior<String> {

    //常规操作：私有化构造方法
    private HelloWorldActor(ActorContext<String> context) {
        super(context);
    }

    //常规操作：通过create()静态方法返回经过封装的Behavior
    public static Behavior<String> create() {
//        return Behaviors.setup(HelloWorldActor::new);
        return Behaviors.setup(context -> new HelloWorldActor(context));
    }

    @Override
    public Receive<String> createReceive() {
        return newReceiveBuilder()
                .onMessage(String.class, this::onMsg)
                .build();
    }
    //收到消息后，会进入此方法，即在此方法中实现处理消息的逻辑
    private Behavior<String> onMsg(String msg) {
        log.info("****** hello world actor"+ " 收到消息：{} \t {}\t{}", msg, getContext().getSelf().getClass(), this.hashCode());
        return this;
    }

}
