package gd.twohundred.jvb;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ForwardingLogger implements Logger {
    private List<Message> delayedMessages;
    private Logger destination;

    public ForwardingLogger() {
        delayedMessages = new ArrayList<>();
    }

    private static class Message {
        final Component component;
        final Level level;
        final String format;
        final Object[] args;

        private Message(Component component, Level level, String format, Object[] args) {
            this.component = component;
            this.level = level;
            this.format = format;
            this.args = args;
        }
    }

    @Override
    public void log(Component component, Level level, String format, Object... args) {
        if (destination == null) {
            delayedMessages.add(new Message(component, level, format, args));
        } else {
            destination.log(component, level, format, args);
        }
    }

    @Override
    public boolean isLevelEnabled(Component component, Level level) {
        if (destination == null) {
            return true;
        }
        return destination.isLevelEnabled(component, level);
    }

    public void setDestination(Logger destination) {
        if (this.destination != null) {
            throw new IllegalStateException("destination should only be set once");
        }
        this.destination = Objects.requireNonNull(destination);
        for (Message message : delayedMessages) {
            this.destination.log(message.component, message.level, message.format, message.args);
        }
        this.delayedMessages = null;
    }
}
