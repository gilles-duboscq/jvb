package gd.twohundred.jvb.components;

import gd.twohundred.jvb.Logger;
import gd.twohundred.jvb.components.ProgramStatusWord.ExecutionMode;
import gd.twohundred.jvb.components.interfaces.AudioOut;
import gd.twohundred.jvb.components.interfaces.Emulable;
import gd.twohundred.jvb.components.interfaces.InputProvider;
import gd.twohundred.jvb.components.interfaces.Interrupt;
import gd.twohundred.jvb.components.interfaces.Interrupt.InterruptType;
import gd.twohundred.jvb.components.interfaces.InterruptSource;
import gd.twohundred.jvb.components.interfaces.Screen;
import gd.twohundred.jvb.components.vip.VirtualImageProcessor;
import gd.twohundred.jvb.components.vsu.VirtualSoundUnit;

public class VirtualBoy implements Emulable {
    public static final String VERSION = "0.1.0";
    private final CPU cpu;
    private final HardwareTimer timer;
    private final VirtualImageProcessor vip;
    private final VirtualSoundUnit vsu;
    private final GamePad gamePad;
    private final Logger logger;
    private Debugger debugger;

    public VirtualBoy(Screen screen, AudioOut audioOut, InputProvider inputProvider, CartridgeROM rom, CartridgeRAM ram, Logger logger) {
        this.logger = logger;
        timer = new HardwareTimer(logger);
        vip = new VirtualImageProcessor(screen, logger);
        vsu = new VirtualSoundUnit(audioOut, logger);
        gamePad = new GamePad(inputProvider, logger);
        HardwareControlRegisters controlRegisters = new HardwareControlRegisters(timer, gamePad, logger);
        Bus bus = new Bus(rom, ram, vip, controlRegisters, vsu, logger);
        cpu = new CPU(bus, logger);
    }

    @Override
    public long tick(long targetCycles) {
        if (isHalted()) {
            return targetCycles;
        }
        long cycles = 0;
        while (cycles < targetCycles) {
            long actualCycles = cpu.tick(targetCycles);
            timer.tickExact(actualCycles);
            vip.tickExact(actualCycles);
            vsu.tickExact(actualCycles);
            gamePad.tickExact(actualCycles);
            cycles += actualCycles;
            handleInterrupts();
            if (isHalted()) {
                break;
            }
        }
        if (this.debugger != null) {
            this.debugger.tickExact(cycles);
        }
        return cycles;
    }

    public boolean isHalted() {
        return cpu.getPsw().getExecutionMode() == ExecutionMode.Halt;
    }

    private static class InterruptChain {
        Interrupt head;
        void append(Interrupt i) {
            Interrupt inserting = i;
            Interrupt previous = null;
            Interrupt current = head;
            while (inserting != null) {
                while (current != null && i.compareTo(current) <= 0) {
                    previous = current;
                    current = current.getNext();
                }
                if (previous == null) {
                    head = inserting;
                } else {
                    previous.setNext(inserting);
                }
                Interrupt next = inserting.getNext();
                inserting.setNext(current);
                inserting = next;
            }
        }
    }

    private Interrupt collectInterrupts() {
        InterruptChain chain = collectInterrupts(cpu, null);
        chain = collectInterrupts(timer, chain);
        chain = collectInterrupts(vip, chain);
        chain = collectInterrupts(gamePad, chain);
        if (chain != null) {
            return chain.head;
        }
        return null;
    }


    private InterruptChain collectInterrupts(InterruptSource source, InterruptChain chain) {
        Interrupt raised = source.raised();
        if (raised != null) {
            if (chain == null) {
                chain = new InterruptChain();
            }
            chain.append(raised);
        }
        return chain;
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
        if (interrupt.getType().isMaskable()) {
            if (cpu.getPsw().getExecutionMode() != ExecutionMode.Normal) {
                logger.debug(Logger.Component.Debugger, "Ignoring maskable interrupt: %s", interrupt);
                return false;
            }
            int interruptLevel = interrupt.getType().getInterruptLevel();
            if (cpu.getPsw().getID() || interruptLevel < cpu.getPsw().getInt()) {
                logger.debug(Logger.Component.Debugger, "Ignoring interrupt (ID:%s, level: %d < %d): %s", cpu.getPsw().getID(), interruptLevel, cpu.getPsw().getInt(), interrupt);
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
        if (cpu.getPsw().getExecutionMode() == ExecutionMode.DuplexedException) {
            logger.warning(Logger.Component.Interrupts, "Fatal interrupt!!");
            cpu.getBus().setWord(0, interrupt.getExceptionCode());
            cpu.getBus().setWord(4, cpu.getPc());
            cpu.getBus().setWord(8, cpu.getPsw().getValue());
            cpu.getPsw().setExecutionMode(ExecutionMode.Halt);
        } else if (cpu.getPsw().getExecutionMode() == ExecutionMode.Exception) {
            logger.warning(Logger.Component.Interrupts, "Duplexed Exception! %s", interrupt);
            cpu.setFepc(cpu.getPc());
            cpu.setFepsw(cpu.getPsw().getValue());
            cpu.setFecc(interrupt.getExceptionCode());
            cpu.getPsw().setNMIPending(true);
            cpu.getPsw().setInterruptDisable(true);
            cpu.getPsw().setAddressTrapEnable(false);
            cpu.setPc(InterruptType.DuplexedException.getHandlerAddress());
            cpu.getPsw().setExecutionMode(ExecutionMode.DuplexedException);
        } else {
            logger.debug(Logger.Component.Interrupts, "Interrupt: %s", interrupt);
            cpu.setEipc(cpu.getPc());
            cpu.setEipsw(cpu.getPsw().getValue());
            cpu.setEicc(interrupt.getExceptionCode());
            cpu.getPsw().setExceptionPending(true);
            cpu.getPsw().setInterruptDisable(true);
            cpu.getPsw().setAddressTrapEnable(false);
            cpu.setPc(interrupt.getType().getHandlerAddress());
            cpu.getPsw().setExecutionMode(ExecutionMode.Exception);
        }
    }

    @Override
    public void reset() {
        cpu.reset();
        timer.reset();
        vip.reset();
        vsu.reset();
        gamePad.reset();
        cpu.getPsw().setExecutionMode(ExecutionMode.Normal);
    }

    public void attach(Debugger debugger) {
        this.debugger = debugger;
        this.cpu.attach(debugger);
    }

    void halt() {
        cpu.getPsw().setExecutionMode(ExecutionMode.Halt);
    }

    CPU getCpu() {
        return cpu;
    }
}
