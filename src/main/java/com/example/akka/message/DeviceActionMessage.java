package com.example.akka.message;

import com.example.akka.actor.BehaviorConfig;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Simple to Introduction
 * className: com.example.akka.message.DeviceMessage
 *
 * @author mujiang
 * @version 2021/05/26 17:35
 */

@Data
//@Builder
@AllArgsConstructor
public class DeviceActionMessage implements IDeviceMessage {

    private BehaviorConfig config;

}
