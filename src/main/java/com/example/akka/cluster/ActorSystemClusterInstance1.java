package com.example.akka.cluster;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Routers;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import akka.cluster.typed.ClusterSingleton;
import akka.cluster.typed.SingletonActor;
import com.example.akka.actor.BehaviorConfig;
import com.example.akka.actor.FilterActor;
import com.example.akka.message.DevicePropertyMessage;
import com.example.akka.message.IDeviceMessage;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * akka集群实例1
 *
 * @author mujiang
 * @version 2021/07/02 17:43
 */
@Slf4j
public class ActorSystemClusterInstance1 {

    //注册FilterActor到group router
    public static class RootBehaviorFilterGroupRouter {
        static Behavior<Void> create() {
            return Behaviors.setup(context -> {

                // actor的配置
                BehaviorConfig filterConfig = new BehaviorConfig("power > 2000");
                // 创建filter behavior
                Behavior<IDeviceMessage> filterBehavior = FilterActor.create(filterConfig, null);

                // 注册到filter Pool Router 到某个key
                ServiceKey<IDeviceMessage> filterServiceKey = ServiceKey.create(IDeviceMessage.class, "FilterGroupRouterKey");
                for(int i = 1; i <= 3; i++){
                    ActorRef<IDeviceMessage> filterActorRef = context.spawn(filterBehavior, "FilterActor" + i);
                    context.getSystem().receptionist().tell(Receptionist.register(filterServiceKey, filterActorRef.narrow()));
                }

                return Behaviors.empty();
            });
        }
    }

    // 单例actor
    public static class RootBehaviorSingletonActor {
        static Behavior<Void> create() {
            return Behaviors.setup(context -> {

                ClusterSingleton singleton = ClusterSingleton.get(context.getSystem());
                // 创建filter的behavior
                BehaviorConfig filterConfig = new BehaviorConfig("power > 2000");
                Behavior<IDeviceMessage> filterBehavior = FilterActor.create(filterConfig, null);
                // 创建单例actor
                SingletonActor<IDeviceMessage> filterSingletonActor = SingletonActor.of(filterBehavior, "GlobalFilter");
                ActorRef globalFilter = singleton.init(filterSingletonActor);

                return Behaviors.empty();
            });
        }
    }

    public static void main(String[] args) {
//      akka系统启动的端口
        String port = "5001";

        Map<String, Object> overrides = new HashMap<>();
        overrides.put("akka.remote.artery.canonical.port", port);

        Config config = ConfigFactory.parseMap(overrides)
                .withFallback(ConfigFactory.load("cluster"));

        ActorSystem<Void> system = ActorSystem.create(RootBehaviorFilterGroupRouter.create(), "cluster-example", config);
//        ActorSystem<Void> system = ActorSystem.create(RootBehaviorSingletonActor.create(), "cluster-example", config);

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(system.printTree());
        log.info("=== actor system started ===");

    }

}
