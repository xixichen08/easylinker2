package com.easylinker.iot.v2.configure.mqtt;

import com.easylinker.iot.v2.configure.mqtt.handler.DeviceMessageReceivedHandler;
import com.easylinker.iot.v2.configure.mqtt.handler.MqttWillMessageHandler;
import com.easylinker.iot.v2.configure.mqtt.handler.MyMqttPahoMessageDrivenChannelAdapter;
import com.easylinker.iot.v2.model.device.Device;
import com.easylinker.iot.v2.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;

/**
 * Created by wwhai on 2017/11/29.
 */
@Configuration
public class MqttConfigure {
    @Autowired
    DeviceRepository deviceRepository;

    /**
     * 默认连接的是本机的EMQ节点 这里为了开发 写死了
     */
    private final String LOCALHOST_EMQ_URL = "tcp://localhost:1883";
    private final String EASY_LINKER_EMQ_USERNAME = "EASY_LINKER";
    private final String EASY_LINKER_EMQ_PASSWORD = "EASY_LINKER";
    //默认监听所有节点的所有客户端的信息
    private final String DEVICE_CONNECTED = "$SYS/brokers/+/clients/+/connected";
    private final String DEVICE_DISCONNECTED = "$SYS/brokers/+/clients/+/disconnected";
    //$SYS/brokers/+/clients/+/disconnected
    private final String EASY_LINKER_MONITOR_TOPIC = "$SYS/brokers/+/clients/+/#";
    private final String EASY_LINKER_DEVICE_TOPIC = "device/#";

    /**
     * mqtt 的工厂  用来创建mqtt连接
     *
     * @return
     */

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        /**
         * 后面这里的配置要从数据库里面加载
         */


        if (deviceRepository.findTopByDeviceName(EASY_LINKER_EMQ_USERNAME) == null) {
            Device device = new Device();
            device.setOpenId(EASY_LINKER_EMQ_USERNAME);
            device.setClientId(EASY_LINKER_EMQ_PASSWORD);
            device.setTopic(EASY_LINKER_MONITOR_TOPIC);
            device.setDeviceName(EASY_LINKER_EMQ_USERNAME);
            device.setDeviceDescribe("EASY_LINKER_MONITOR");
            deviceRepository.save(device);
        }


        factory.setServerURIs(LOCALHOST_EMQ_URL);
        factory.setUserName(EASY_LINKER_EMQ_USERNAME);
        factory.setPassword(EASY_LINKER_EMQ_PASSWORD);

        return factory;
    }

    /**
     * 这个Bean用来配置处理接受消息的处理器
     *
     * @return
     */

    @Bean
    public MessageProducerSupport mqttMessageHandler() {

        MqttPahoMessageDrivenChannelAdapter adapter = new MyMqttPahoMessageDrivenChannelAdapter(
                "mqttMessageHandler",
                mqttClientFactory());
        adapter.addTopic(EASY_LINKER_DEVICE_TOPIC);//监控消息
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(2);
        return adapter;
    }


    @Bean
    public MessageProducerSupport mqttClientHandler() {

        MqttPahoMessageDrivenChannelAdapter adapter = new MyMqttPahoMessageDrivenChannelAdapter(
                "mqttClientHandler",
                mqttClientFactory());
        adapter.addTopic(DEVICE_CONNECTED);//监控上线通知
        adapter.addTopic(DEVICE_DISCONNECTED);//监控下线通知

        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(2);
        return adapter;
    }

    /**
     * 用来处理消息适配器
     *
     * @return
     */

    @Bean
    public IntegrationFlow mqttDeviceInFlow() {
        return IntegrationFlows.from(mqttClientHandler())
                .handle(new MqttWillMessageHandler())

                .get();
    }

    @Bean
    public IntegrationFlow mqttMessageInFlow() {
        return IntegrationFlows.from(mqttMessageHandler())
                .handle(new DeviceMessageReceivedHandler())
                .get();
    }
}
