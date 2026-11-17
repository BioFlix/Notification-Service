package bioflix.notification.config;

import bioflix.notification.messaging.EventRoutingKeys;
import bioflix.notification.messaging.InvalidEventException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.boot.amqp.autoconfigure.RabbitListenerRetrySettingsCustomizer;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RabbitConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.queue}")
    private String queueName;

    @Value("${app.rabbitmq.dead-letter-exchange}")
    private String deadLetterExchangeName;

    @Value("${app.rabbitmq.dead-letter-queue}")
    private String deadLetterQueueName;

    @Bean
    public Declarables notificationTopology() {
        TopicExchange exchange = new TopicExchange(exchangeName, true, false);
        DirectExchange deadLetterExchange = new DirectExchange(deadLetterExchangeName, true, false);

        Queue queue = QueueBuilder.durable(queueName)
                .deadLetterExchange(deadLetterExchangeName)
                .deadLetterRoutingKey(deadLetterQueueName)
                .build();
        Queue deadLetterQueue = QueueBuilder.durable(deadLetterQueueName).build();

        List<Declarable> declarables = new ArrayList<>(List.of(
                exchange, deadLetterExchange, queue, deadLetterQueue,
                BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(deadLetterQueueName)));

        for (String routingKey : EventRoutingKeys.SUBSCRIBED) {
            Binding binding = BindingBuilder.bind(queue).to(exchange).with(routingKey);
            declarables.add(binding);
        }
        return new Declarables(declarables);
    }

    @Bean
    public RabbitListenerRetrySettingsCustomizer skipRetryForInvalidMessages() {
        return settings -> settings.setExceptionPredicate(RabbitConfig::isRetryable);
    }

    static boolean isRetryable(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof InvalidEventException || t instanceof AmqpRejectAndDontRequeueException) {
                return false;
            }
        }
        return true;
    }
}
