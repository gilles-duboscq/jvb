package gd.twohundred.jvb.components.debug;

import gd.twohundred.jvb.Logger.Component;
import gd.twohundred.jvb.Logger.Level;

public class LogMessage {
    private final long cycle;
    private final Level level;
    private final Component source;
    private final String message;

    public LogMessage(long cycle, Level level, Component source, String message) {
        this.cycle = cycle;
        this.level = level;
        this.source = source;
        this.message = message;
    }

    public Component getSource() {
        return source;
    }

    public Level getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public long getCycle() {
        return cycle;
    }
}
