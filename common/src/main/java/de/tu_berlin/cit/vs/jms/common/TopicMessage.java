package de.tu_berlin.cit.vs.jms.common;

import javax.jms.Topic;

public class TopicMessage extends BrokerMessage {
    private final Topic topic;
    private boolean Subscribing; //true = create subsription; 0 = remove it
    public TopicMessage(Topic topic, boolean following) {
        super(Type.TOPIC);
        this.topic = topic;
        this.Subscribing = following;
    }

    public Topic getTopic() {
        return topic;
    }

    public boolean isSetSubscribing() {
        return Subscribing;
    }
}