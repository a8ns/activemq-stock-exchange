package de.tu_berlin.cit.vs.jms.common;

import javax.jms.Topic;

public class TopicMessage extends BrokerMessage {
    private final Topic topic;
    public TopicMessage(Topic topic) {
        super(Type.TOPIC);
        this.topic = topic;
    }

    public Topic getTopic() {
        return topic;
    }
}