package gd.twohundred.jvb;

public class BusError extends RuntimeException {
    public enum Reason {
        Unmapped("Unmapped address"),
        Permission("Forbidden access"),
        Unimplemented("Unimplemented"),
        Error("Emulator error");

        private final String msg;

        Reason(String msg) {
            this.msg = msg;
        }

        public String getMsg() {
            return msg;
        }
    }
    private final Reason reason;

    public Reason getReason() {
        return reason;
    }


    public BusError(int localAddress, Reason reason, String msg) {
        this(localAddress, reason, msg, null);
    }

    public BusError(int localAddress, Reason reason) {
        this(localAddress, reason, "", null);
    }

    public BusError(int localAddress, Reason reason, Throwable cause) {
        this(localAddress, reason, "", cause);
    }

    public BusError(int localAddress, Reason reason, String msg, Throwable cause) {
        super(String.format("%s! %s 0x%08x", reason.getMsg(), msg, localAddress), cause);
        this.reason = reason;
    }
}
