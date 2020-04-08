package cn.lehome.dispatcher.utils.config;

import cn.lehome.base.api.tool.compoment.idgenerator.RedisIdGeneratorComponent;
import cn.lehome.base.api.tool.compoment.jms.EventBusComponent;
import cn.lehome.framework.base.api.core.compoment.jms.EventBusJmsVirtualTopicQueueFactoryBean;
import cn.lehome.framework.base.api.core.compoment.jms.SimpleJmsQueueFactoryBean;
import cn.lehome.framework.configuration.properties.jms.ActiveMQProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by zuoguodong on 2018/4/3
 */
@Configuration
public class InitConfig {

    @Autowired
    private ActiveMQProperties activeMQProperties;

    @Bean
    public SimpleJmsQueueFactoryBean simpleJmsQueueFactoryBean() {
        SimpleJmsQueueFactoryBean simpleJmsQueueFactoryBean = new SimpleJmsQueueFactoryBean();
        simpleJmsQueueFactoryBean.setGlobalPrefix(activeMQProperties.getPrefix());
        return simpleJmsQueueFactoryBean;
    }

    @Bean
    public EventBusJmsVirtualTopicQueueFactoryBean eventBusJmsVirtualTopicQueueFactoryBean() {
        EventBusJmsVirtualTopicQueueFactoryBean eventBusJmsVirtualTopicQueueFactoryBean = new EventBusJmsVirtualTopicQueueFactoryBean();
        eventBusJmsVirtualTopicQueueFactoryBean.setGlobalPrefix(activeMQProperties.getPrefix());
        return eventBusJmsVirtualTopicQueueFactoryBean;
    }

    @Bean
    public EventBusComponent eventBusComponent() {
        return new EventBusComponent();
    }

    @Bean
    public RedisIdGeneratorComponent redisIdGeneratorComponent() {
        return new RedisIdGeneratorComponent();
    }
}
