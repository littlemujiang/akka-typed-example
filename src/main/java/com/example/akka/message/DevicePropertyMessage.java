package com.example.akka.message;

import lombok.Builder;
import lombok.Data;

/**
 * Simple to Introduction
 * className: com.example.akka.message.DeviceMessage
 *
 * @author mujiang
 * @version 2021/05/26 17:35
 */

@Data
@Builder
public class DevicePropertyMessage implements IDeviceMessage {

    private int msgId;
    private String thingId;
    private double current;
    private double voltage;
    private double power;

}
