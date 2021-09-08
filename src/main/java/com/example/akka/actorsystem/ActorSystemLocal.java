package com.example.akka.actorsystem;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Routers;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import com.example.akka.actor.BehaviorConfig;
import com.example.akka.actor.FilterActor;
import com.example.akka.actor.HelloWorldActor;
import com.example.akka.actor.ProcessActor;
import com.example.akka.message.DevicePropertyMessage;
import com.example.akka.message.IDeviceMessage;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * akka单节点启动
 *
 * @author mujiang
 * @version 2021/07/02 17:43
 */
@Slf4j
public class ActorSystemLocal {

    public static void main(String[] args) {
        //指定akka系统启动的端口
        String port = "5001";
        //加载local.conf的配置文件，并替换"akka.remote.artery.canonical.port"这个参数
        Map<String, Object> overrides = new HashMap<>();
        overrides.put("akka.remote.artery.canonical.port", port);
        Config config = ConfigFactory.parseMap(overrides)
                .withFallback(ConfigFactory.load("local"));
        //启动akka系统，启动时指定一个behavior，并给系统命名
//        ActorSystem<Void> system = ActorSystem.create(RootBehavior.create(), "local-example", config);
//        ActorSystem<Void> system = ActorSystem.create(RootBehaviorBaseActor.create(), "local-example", config);
//        ActorSystem<Void> system = ActorSystem.create(RootBehaviorComplicatedActor.create(), "local-example", config);
//        ActorSystem<Void> system = ActorSystem.create(RootBehaviorFilterPoolRouter.create(), "local-example", config);
        ActorSystem<Void> system = ActorSystem.create(RootBehaviorFilterGroupRouter.create(), "local-example", config);
        log.info("=== actor system started ===");

    }

    //空ActorSystem
    public static class RootBehavior {
        static Behavior<Void> create() {
            return Behaviors.setup(context ->
                    Behaviors.empty()
            );
        }
    }

    //最简单的Actor
    public static class RootBehaviorBaseActor {
        static Behavior<Void> create() {
            return Behaviors.setup(context -> {
                        // 创建一个actor，并给此actor发送消息
                        ActorRef<String> hello = context.spawn(HelloWorldActor.create(), "HelloWorld");
                        for (int i = 0; i < 10; i++) {
                            hello.tell("hi");
                        }
                        return Behaviors.empty();
                    }
            );
        }
    }

    //扩展的Actor：带初始化参数，自定义消息类型
    public static class RootBehaviorComplicatedActor {
        static Behavior<Void> create() {
            return Behaviors.setup(context -> {
                        // actor的配置
                        BehaviorConfig filterConfig = new BehaviorConfig("$power > 2000");
                        // 创建filter的behavior
                        Behavior<IDeviceMessage> filterBehavior = FilterActor.create(filterConfig, null);
                        // 实例化filter actor
                        ActorRef<IDeviceMessage> filterActorRef = context.spawn(filterBehavior, "Filter");

                        DevicePropertyMessage message = DevicePropertyMessage.builder()
                                .thingId("D007")
                                .current(3.58)
                                .voltage(380.00)
                                .build();
                        filterActorRef.tell(message);
                        return Behaviors.empty();
                    }
            );
        }
    }

    //路由：pool router
    public static class RootBehaviorFilterPoolRouter {
        static Behavior<Void> create() {
            return Behaviors.setup(context -> {
                // actor的配置
                BehaviorConfig filterConfig = new BehaviorConfig("power > 2000");
                // 创建filter behavior
                Behavior<IDeviceMessage> filterBehavior = FilterActor.create(filterConfig, null);
                // 封装filter behavior 成 pool router behavior
                Behavior<IDeviceMessage> filterPoolBehavior = Routers.pool(5, filterBehavior.narrow()).withRoundRobinRouting();
                // 实例化filter pool router
                ActorRef<IDeviceMessage> filterPoolRouterRef = context.spawn(filterPoolBehavior, "FilterPoolRouter");

                // 监管策略
//                Behaviors.supervise(filterPoolBehavior).onFailure(SupervisorStrategy.restart().withStopChildren(false));

                DevicePropertyMessage message = DevicePropertyMessage.builder()
                        .thingId("D007")
                        .current(3.58)
                        .voltage(380.00)
                        .build();
                // 给pool router发送消息
                for (int i = 0; i < 10; i++) {
                    filterPoolRouterRef.tell(message);
                }
                return Behaviors.empty();
            });
        }
    }

    //路由：group router
    public static class RootBehaviorFilterGroupRouter {
        static Behavior<Void> create() {
            return Behaviors.setup(context -> {

                // actor的配置
                BehaviorConfig filterConfig = new BehaviorConfig("power > 2000");
                // 创建filter behavior
                Behavior<IDeviceMessage> filterBehavior = FilterActor.create(filterConfig, null);
                // 实例化filter actor
                ActorRef<IDeviceMessage> filterActorRef1 = context.spawn(filterBehavior, "FilterActor1");

                // 注册到filter Pool Router 到某个key
                ServiceKey<IDeviceMessage> filterServiceKey = ServiceKey.create(IDeviceMessage.class, "FilterGroupRouterKey");
                context.getSystem().receptionist().tell(Receptionist.register(filterServiceKey, filterActorRef1.narrow()));
                for(int i = 2; i <= 5; i++){
                    ActorRef<IDeviceMessage> filterActorRef = context.spawn(filterBehavior, "FilterActor" + i);
                    context.getSystem().receptionist().tell(Receptionist.register(filterServiceKey, filterActorRef.narrow()));
                }

//                Thread.sleep(2 * 1000);

                DevicePropertyMessage message = DevicePropertyMessage.builder()
                        .thingId("D007")
                        .current(3.58)
                        .voltage(380.00)
                        .build();
                // 获取group route的ActorRef
                Behavior<IDeviceMessage> filterGroupBehavior = Routers.group(filterServiceKey).withRoundRobinRouting();
                ActorRef<IDeviceMessage> filterGroupRouterRef = context.spawn(filterGroupBehavior, "FilterGroupRouter");
                // 给group router发送消息
                for (int i = 0; i < 10; i++) {
                    filterGroupRouterRef.tell(message);
                }
                return Behaviors.empty();
            });
        }
    }

    //从process actor 给filter pool router发消息
    public static class RootBehaviorProcessActor2FilterPoolActor {
        static Behavior<Void> create() {
            return Behaviors.setup(context -> {

                // actor的配置
                BehaviorConfig filterConfig = new BehaviorConfig("power > 2000");
                // 创建filter的behavior
                Behavior<IDeviceMessage> filterBehavior = FilterActor.create(filterConfig, null);
                // 把filter behavior封装成pool router behavior
                Behavior<IDeviceMessage> filterPoolBehavior = Routers.pool(5, filterBehavior.narrow()).withRoundRobinRouting();
                // 实例化 pool router
                ActorRef<IDeviceMessage> filterPoolRouterRef = context.spawn(filterPoolBehavior, "FilterPoolRouter");
                // 给pool router发送消息
                filterPoolRouterRef.tell(DevicePropertyMessage.builder().thingId("D007").build());

                BehaviorConfig processConfig = new BehaviorConfig("power = current * voltage");
                // 创建process的behavior(下一个节点指向 pool router 而不是 普通actor)
                Behavior<IDeviceMessage> processBehavior = ProcessActor.create(processConfig, filterPoolRouterRef);
                // 实例化actor
                ActorRef<IDeviceMessage> processActorRef = context.spawn(processBehavior, "Process");

                DevicePropertyMessage message = DevicePropertyMessage.builder()
                        .thingId("D007")
                        .current(3.58)
                        .voltage(380.00)
                        .build();

                for (int i = 0; i < 10; i++) {
                    processActorRef.tell(message);
                }

                return Behaviors.empty();
            });
        }
    }
}
