package gd.twohundred.jvb;

import static gd.twohundred.jvb.Logger.Level.Debug;
import static gd.twohundred.jvb.Logger.Level.Error;
import static gd.twohundred.jvb.Logger.Level.Info;
import static gd.twohundred.jvb.Logger.Level.Warning;

public interface Logger {
    enum Component {
        CPU,
        VIP,
        VSU,
        Memory,
        CartridgeROM,
        HardwareControlRegs,
        Interrupts,
        GamePad, Timer, Misc
    }

    enum Level {
        Error,
        Warning,
        Info,
        Debug
    }

    default void debug(Component component, String format, Object... args) {
        log(component, Debug, format, args);
    }

    default void info(Component component, String format, Object... args) {
        log(component, Info, format, args);
    }

    default void warning(Component component, String format, Object... args){
        log(component, Warning, format, args);
    }

    default void error(Component component, String format, Object... args){
        log(component, Error, format, args);}

    void log(Component component, Level level, String format, Object... args);

    boolean isLevelEnabled(Component component, Level level);
}
