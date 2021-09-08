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

    public static class RootBehaviorWithGroupRouter {
        static Behavior<Void> create() {
            return Behaviors.setup(context -> {

                BehaviorConfig filterConfig = new BehaviorConfig("power > 2000");
//                ActorRef globalFilter = singleton.init(SingletonActor.of(FilterActor.create(filterConfig, null), "GlobalFilter").withStopMessage(StopMessage.INSTANCE));

//              poll router
//                BehaviorConfig filterConfig = new BehaviorConfig("power > 2000");
                // 创建filter的behavior
//                Behavior<INodeMessage> filterBehavior = FilterActor.create(filterConfig, null);
//                ActorRef<INodeMessage> filterRef = context.spawn(ProcessActor.create(filterConfig, null), "filter");

                // 把filter behavior封装成pool router behavior
//                Behavior<INodeMessage> filterPoolBehavior = Routers.pool(5, filterBehavior.narrow()).withRoundRobinRouting();
                // 实例化 pool router
//                ActorRef<INodeMessage> filterPoolRouterRef = context.spawn(filterPoolBehavior, "FilterPoolRouter");

                // 注册到filter Pool Router 到某个key
//                ServiceKey<INodeMessage> FILTER_SERVICE_KEY = ServiceKey.create(INodeMessage.class, "FilterPoolRouterKey");
//                context.getSystem().receptionist().tell(Receptionist.register(FILTER_SERVICE_KEY, filterPoolRouterRef.narrow()));

                ClusterSingleton singleton = ClusterSingleton.get(context.getSystem());
                // 创建filter的behavior
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

//        ActorSystem<Void> system = ActorSystem.create(RootBehaviorFilterGroupRouter.create(), "cluster-example", config);
        ActorSystem<Void> system = ActorSystem.create(RootBehaviorWithGroupRouter.create(), "cluster-example", config);

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(system.printTree());
        log.info("=== actor system started ===");

//        ServiceDiscovery serviceDiscovery = Discovery.get(system).discovery();

    }

}
