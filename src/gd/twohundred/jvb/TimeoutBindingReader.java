package gd.twohundred.jvb;

import org.jline.keymap.BindingReader;
import org.jline.reader.EndOfFileException;
import org.jline.utils.ClosedException;
import org.jline.utils.NonBlockingReader;

import java.io.IOError;
import java.io.IOException;

public class TimeoutBindingReader extends BindingReader {
    private final int timeout;
    public TimeoutBindingReader(NonBlockingReader reader, int timeout) {
        super(reader);
        this.timeout = timeout;
    }

    public int readCharacter() {
        if (!pushBackChar.isEmpty()) {
            return pushBackChar.pop();
        }
        try {
            int c = NonBlockingReader.READ_EXPIRED;
            int s = 0;
            while (c == NonBlockingReader.READ_EXPIRED) {
                c = reader.read(timeout);
                if (c >= 0 && Character.isHighSurrogate((char) c)) {
                    s = c;
                    c = NonBlockingReader.READ_EXPIRED;
                }
            }
            return s != 0 ? Character.toCodePoint((char) s, (char) c) : c;
        } catch (ClosedException e) {
            throw new EndOfFileException(e);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }
}
