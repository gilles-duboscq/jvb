package gd.twohundred.jvb;

import java.io.PrintStream;
import java.util.EnumMap;
import java.util.Formatter;
import java.util.Map;

import static gd.twohundred.jvb.Logger.Level.Error;

public class StdLogger implements Logger {
    private final Map<Component, Level> levels = new EnumMap<>(Component.class);

    public StdLogger() {
        for (Component c : Component.values()) {
            levels.put(c, Level.Warning);
        }
    }

    @Override
    public void log(Component component, Level level, String format, Object... args) {
        if (isLevelEnabled(component, level)) {
            PrintStream stream = level.ordinal() <= Error.ordinal() ? System.err : System.out;
            StringBuilder sb = new StringBuilder();
            Formatter formatter = new Formatter(sb);
            formatter.format("[%7s][%6s] ", level, component);
            formatter.format(format, args);
            sb.append(System.lineSeparator());
            stream.print(sb);
        }
    }

    @Override
    public boolean isLevelEnabled(Component component, Level level) {
        return levels.get(component).ordinal() >= level.ordinal();
    }
}
