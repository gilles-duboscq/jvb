package gd.twohundred.jvb.components;

import gd.twohundred.jvb.Logger;
import gd.twohundred.jvb.components.interfaces.Emulable;
import gd.twohundred.jvb.components.interfaces.InputProvider;
import gd.twohundred.jvb.components.interfaces.Interrupt;
import gd.twohundred.jvb.components.interfaces.Interrupt.InterruptType;
import gd.twohundred.jvb.components.interfaces.InterruptSource;
import gd.twohundred.jvb.components.interfaces.Screen;
import gd.twohundred.jvb.components.vip.VirtualImageProcessor;
import gd.twohundred.jvb.components.vsu.VirtualSoundUnit;

import static gd.twohundred.jvb.components.VirtualBoy.ExecutionMode.DuplexedException;

public class VirtualBoy implements Emulable {
    public static final String VERSION = "0.1.0";
    private final CPU cpu;
    private final HardwareTimer timer;
    private final VirtualImageProcessor vip;
    private final VirtualSoundUnit vsu;
    private final GamePad gamePad;
    private final Logger logger;
    private Debugger debugger;

    enum ExecutionMode {
        Normal,
        Exception,
        DuplexedException,
        Halt
    }

    private ExecutionMode executionMode;

    public VirtualBoy(Screen screen, InputProvider inputProvider, CartridgeROM rom, CartridgeRAM ram, Logger logger) {
        this.logger = logger;
        timer = new HardwareTimer(logger);
        vip = new VirtualImageProcessor(screen, logger);
        vsu = new VirtualSoundUnit();
        gamePad = new GamePad(inputProvider, logger);
        HardwareControlRegisters controlRegisters = new HardwareControlRegisters(timer, gamePad, logger);
        Bus bus = new Bus(rom, ram, vip, controlRegisters, vsu, logger);
        cpu = new CPU(bus, logger);
    }

    @Override
    public int tick(int targetCycles) {
        if (executionMode == ExecutionMode.Halt) {
            return targetCycles;
        }
        int cycles = 0;
        while (cycles < targetCycles) {
            int actualCycles = cpu.tick(targetCycles);
            timer.tickExact(actualCycles);
            vip.tickExact(actualCycles);
            vsu.tickExact(actualCycles);
            gamePad.tickExact(actualCycles);
            if (this.debugger != null) {
                this.debugger.tickExact(actualCycles);
            }
            cycles += actualCycles;
            if (handleInterrupts()) {
                if (executionMode == ExecutionMode.Halt) {
                    return targetCycles;
                }
                break;
            }
        }
        return cycles;
    }

    private static class InterruptChain {
        Interrupt head;
        Interrupt tail;
        void append(Interrupt i) {
            if (tail == null) {
                tail = i;
                head = i;
            } else {
                tail.setNext(i);
            }
            while (tail.getNext() != null) {
                tail = tail.getNext();
            }
        }
    }

    private Interrupt collectInterrupts() {
        InterruptChain chain = new InterruptChain();
        collectInterrupts(cpu, chain);
        collectInterrupts(timer, chain);
        collectInterrupts(vip, chain);
        collectInterrupts(gamePad, chain);
        return chain.head;
    }


    private void collectInterrupts(InterruptSource source, InterruptChain chain) {
        Interrupt raised = source.raised();
        if (raised != null) {
            chain.append(raised);
        }
    }

    private boolean handleInterrupts() {
        Interrupt interrupt = collectInterrupts();
        while (interrupt != null) {
            if (processInterrupt(interrupt)) {
                handleInterrupt(interrupt);
                return true;
            }
            interrupt = interrupt.getNext();
        }
        return false;
    }

    private boolean processInterrupt(Interrupt interrupt) {
        // FIXME: sort interrupts properly!
        if (interrupt.getType().isMaskable()) {
            if (executionMode != ExecutionMode.Normal) {
                return false;
            }
            int interruptLevel = interrupt.getType().getInterruptLevel();
            if (cpu.getPsw().getID() || interruptLevel < cpu.getPsw().getInt()) {
                return false;
            }
            cpu.setEipc(cpu.getPc());
            cpu.setEipsw(cpu.getPsw().getValue());
            cpu.getPsw().setInterruptLevel((interruptLevel + 1) & 0xf);
            return true;
        }
        return true;
    }

    private void handleInterrupt(Interrupt interrupt) {
        if (executionMode == DuplexedException) {
            logger.warning(Logger.Component.Interrupts, "Fatal interrupt!!");
            cpu.getBus().setWord(0, interrupt.getExceptionCode());
            cpu.getBus().setWord(4, cpu.getPc());
            cpu.getBus().setWord(8, cpu.getPsw().getValue());
            executionMode = ExecutionMode.Halt;
        } else if (executionMode == ExecutionMode.Exception) {
            cpu.setFepc(cpu.getPc());
            cpu.setFepsw(cpu.getPsw().getValue());
            cpu.setFecc(interrupt.getExceptionCode());
            cpu.getPsw().setNMIPending(true);
            cpu.getPsw().setInterruptDisable(true);
            cpu.getPsw().setAddressTrapEnable(false);
            cpu.setPc(InterruptType.DuplexedException.getHandlerAddress());
            executionMode = DuplexedException;
        } else {
            cpu.setEipc(cpu.getPc());
            cpu.setEipsw(cpu.getPsw().getValue());
            cpu.setEicc(interrupt.getExceptionCode());
            cpu.getPsw().setExceptionPending(true);
            cpu.getPsw().setInterruptDisable(true);
            cpu.getPsw().setAddressTrapEnable(false);
            cpu.setPc(interrupt.getType().getHandlerAddress());
        }
    }

    @Override
    public void reset() {
        cpu.reset();
        timer.reset();
        vip.reset();
        vsu.reset();
        gamePad.reset();
        executionMode = ExecutionMode.Normal;
    }

    public void attach(Debugger debugger) {
        this.debugger = debugger;
        this.cpu.attach(debugger);
    }
}
