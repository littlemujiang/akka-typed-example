package com.example.akka.actor;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.example.akka.message.DeviceActionMessage;
import com.example.akka.message.DevicePropertyMessage;
import com.example.akka.message.IDeviceMessage;
import com.example.akka.message.StopMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FilterActor extends AbstractBehavior<IDeviceMessage> {

    private BehaviorConfig config;
    private ActorRef nextActorRef;

    private FilterActor(ActorContext<IDeviceMessage> context, BehaviorConfig behaviorConfig, ActorRef nextActor) {
        super(context);
        this.config = behaviorConfig;
        this.nextActorRef = nextActor;
    }

    //传2个参数，一个是自定义的配置类，一个是连接的下一个actor
    public static Behavior<IDeviceMessage> create(BehaviorConfig behaviorConfig, ActorRef nextActor) {
        return Behaviors.setup(context ->  new FilterActor(context, behaviorConfig, nextActor)
        );
    }

    @Override
    public Receive<IDeviceMessage> createReceive() {
        return newReceiveBuilder()
                //不同类型的消息，指定不同的方法处理
                .onMessage(DevicePropertyMessage.class, this::onPropertyMessage)
                .onMessage(DeviceActionMessage.class, this::onActionMessage)
                .onMessage(StopMessage.class, this::onStopMessage)
                .onSignal(PostStop.class, this::onPostStop)
                .onSignal(PreRestart.class, this::onPreRestart)
                .onSignal(Terminated.class, this::onTerminated)
                .build();
    }

    private Behavior<IDeviceMessage> onPropertyMessage(DevicePropertyMessage msg) {
//        log.info("*** [filter actor] "+ " 收到消息：{}", config.getOperation());
//        log.info("[filter actor] 收到消息："+ " actor path={} \t threadId={} \t objectId={}", getContext().getSelf().path(), Thread.currentThread().getId(), this.hashCode());
        System.out.println(String.format("[filter actor] 收到消息："+ " actor path=%s \t threadId=%s \t objectId=%s", getContext().getSelf().path(), Thread.currentThread().getId(), this.hashCode()));
//        log.info("*** [filter actor] 收到消息："+ " actor path={} \t threadId={} \t objectId={}, config={}", getContext().getSelf().path(), Thread.currentThread().getId(), this.hashCode(), config.getExpression());
        return this;
    }

    private Behavior<IDeviceMessage> onActionMessage(DeviceActionMessage msg) {
        this.config = msg.getConfig();
//        log.info("*** [filter actor] "+ " 收到消息：{}", config.getOperation());
//        log.info("*** [filter actor] 收到消息："+ " actor path={} \t threadId={} \t objectId={}, config={}", getContext().getSelf().path(), Thread.currentThread().getId(), this.hashCode(), config.getOperation());
        return this;
    }

    private Behavior<IDeviceMessage> onStopMessage(StopMessage msg) {
//        log.info("*** [filter actor] "+ " 收到消息：{}", config.getOperation());
        log.info("*** [filter actor] 收到stop消息："+ " actor path={} \t threadId={} \t objectId={} ", getContext().getSelf().path(), Thread.currentThread().getId(), this.hashCode());
//        this.config = null;
//        return Behaviors.empty();
//        return Behaviors.stopped();
        return this;
    }

    private Behavior<IDeviceMessage> onPreRestart(PreRestart restart) {
        getContext().getSystem().log().info("Master Control Program stopped");
        log.info("*** [filter actor] restart ***");
        return this;
    }

    private Behavior<IDeviceMessage> onPostStop(PostStop stop) {
        getContext().getSystem().log().info("Master Control Program stopped");
        log.info("*** [filter actor] restart ***");
        return Behaviors.stopped();
    }

    private Behavior<IDeviceMessage>  onTerminated(Terminated terminated) {
        log.info("*** [filter actor] onTerminated ***");
        return this;
    }

}
