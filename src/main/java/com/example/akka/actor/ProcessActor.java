package com.example.akka.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.PreRestart;
import akka.actor.typed.javadsl.*;
import akka.actor.typed.receptionist.ServiceKey;
import akka.cluster.typed.ClusterSingleton;
import akka.cluster.typed.SingletonActor;
import com.example.akka.message.DeviceActionMessage;
import com.example.akka.message.DevicePropertyMessage;
import com.example.akka.message.IDeviceMessage;
import com.example.akka.message.StopMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class ProcessActor extends AbstractBehavior<IDeviceMessage> {

    static final ServiceKey<IDeviceMessage> FILTER_SERVICE_KEY = ServiceKey.create(IDeviceMessage.class, "FilterPoolRouterKey");
    private BehaviorConfig config;
    private ActorRef<IDeviceMessage> nextActorRef;
    private ActorRef<IDeviceMessage> nextGroupRouterRef;
    private ActorRef<IDeviceMessage> nextSingletonRef;


    private ProcessActor(ActorContext<IDeviceMessage> context, BehaviorConfig behaviorConfig, ActorRef nextActor) {
        super(context);
        this.config = behaviorConfig;
        this.nextActorRef = nextActor;
    }

    //返回的Behavior限定了此actor可处理的消息类型
    public static Behavior<IDeviceMessage> create(BehaviorConfig behaviorConfig, ActorRef<IDeviceMessage> nextActor) {
        return Behaviors.setup(context -> new ProcessActor(context, behaviorConfig, nextActor));
    }

    @Override
    public Receive<IDeviceMessage> createReceive() {
        return newReceiveBuilder()
                //不同类型的消息，指定不同的方法处理
                .onMessage(DevicePropertyMessage.class, this::onPropertyMessage)
                .onMessage(DeviceActionMessage.class, this::onActionMessage)
                //接收停止actor的消息
                .onMessage(StopMessage.class, this::onStopMessage)
                .onSignal(PostStop.class, this::onPostStop)
                .build();
    }

    private Behavior<IDeviceMessage> onPropertyMessage(DevicePropertyMessage msg) {
        log.info("*** [process actor] 收到消息：{} ", config.getExpression());
//        tellNext(msg);
        tellNextByGroup(msg);
//        tellNextBySingleton(msg);
        return this;
    }

    private Behavior<IDeviceMessage> onActionMessage(DeviceActionMessage msg) {
        log.info("*** [process actor] 收到消息：{} ", config.getExpression());
        return this;
    }

    private void tellNext(DevicePropertyMessage msg) {
        if (Objects.nonNull(this.nextActorRef)) {
            msg.setCurrent(msg.getCurrent() * msg.getVoltage());
            this.nextActorRef.tell(msg);
            log.info("*** [process actor] " + " 转发消息完成");
        }
    }


    private void tellNextByGroup(DevicePropertyMessage msg) {
        if (Objects.isNull(this.nextGroupRouterRef)) {
            Behavior<IDeviceMessage> filterGroupBehavior = Routers.group(FILTER_SERVICE_KEY);
            this.nextGroupRouterRef = getContext().spawn(filterGroupBehavior, "FilterGroupRouter");
        }
        this.nextGroupRouterRef.tell(msg);
        log.info("*** [process actor] " + " 转发消息");
    }

    private void tellNextBySingleton(DevicePropertyMessage msg) {
        if (Objects.isNull(this.nextSingletonRef)) {
            ClusterSingleton singleton = ClusterSingleton.get(getContext().getSystem());
            // 创建filter的behavior
            BehaviorConfig filterConfig = new BehaviorConfig("power > 1000");
            Behavior<IDeviceMessage> filterBehavior = FilterActor.create(filterConfig, null);
            // 获得单例actor的ActorRef
            SingletonActor<IDeviceMessage> filterActor = SingletonActor.of(filterBehavior, "GlobalFilter");
            this.nextSingletonRef = singleton.init(filterActor);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            DeviceActionMessage behaviorMessage = new DeviceActionMessage(filterConfig);
            this.nextSingletonRef.tell(behaviorMessage);
        }
        if (msg.getMsgId() % 2 > 0) {
            this.nextSingletonRef.tell(StopMessage.INSTANCE);
//            Terminated.apply(this.nextSingletonRef.unsafeUpcast());
            this.nextSingletonRef = null;
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return;
        } else {
            this.nextSingletonRef.tell(msg);
            log.info("*** [process actor] " + " 转发消息");
        }
    }

    private Behavior<IDeviceMessage> onStopMessage(StopMessage msg) {
        log.info("[process actor] stopped" );
        return Behaviors.stopped();
    }

    private Behavior<IDeviceMessage> onPostStop(PostStop stopped) {
        getContext().getSystem().log().info("Master Control Program stopped");
        log.info("*** [process actor] poststop ***");
        return this;
    }

}
