package gd.twohundred.jvb.components.utils;

import gd.twohundred.jvb.BusError;
import gd.twohundred.jvb.components.interfaces.MappedMemory;
import gd.twohundred.jvb.components.interfaces.ReadOnlyMemory;
import gd.twohundred.jvb.components.interfaces.ReadWriteMemory;
import gd.twohundred.jvb.components.interfaces.WriteOnlyMemory;

import static gd.twohundred.jvb.BusError.Reason.Permission;

public abstract class MappedModules implements ReadWriteMemory {
    @Override
    public int getByte(int address) {
        try {
            int effectiveAddress = address & (getSize() - 1);
            MappedMemory mappedModule = getMappedModule(effectiveAddress);
            return ((ReadOnlyMemory) mappedModule).getByte(effectiveAddress - mappedModule.getStart());
        } catch (BusError be) {
            return handleBusError(address, be);
        } catch (ClassCastException cce) {
            return handlePermissionException(address);
        }
    }

    @Override
    public int getHalfWord(int address) {
        try {
            int effectiveAddress = address & (getSize() - 1);
            MappedMemory mappedModule = getMappedModule(effectiveAddress);
            return ((ReadOnlyMemory) mappedModule).getHalfWord(effectiveAddress - mappedModule.getStart());
        } catch (BusError be) {
            return handleBusError(address, be);
        } catch (ClassCastException cce) {
            return handlePermissionException(address);
        }
    }

    @Override
    public int getWord(int address) {
        try {
            int effectiveAddress = address & (getSize() - 1);
            MappedMemory mappedModule = getMappedModule(effectiveAddress);
            return ((ReadOnlyMemory) mappedModule).getWord(effectiveAddress - mappedModule.getStart());
        } catch (BusError be) {
            return handleBusError(address, be);
        } catch (ClassCastException cce) {
            return handlePermissionException(address);
        }
    }

    @Override
    public void setByte(int address, byte value) {
        try {
            int effectiveAddress = address & (getSize() - 1);
            MappedMemory mappedModule = getMappedModule(effectiveAddress);
            ((WriteOnlyMemory) mappedModule).setByte(effectiveAddress - mappedModule.getStart(), value);
        } catch (BusError be) {
            handleBusError(address, be);
        } catch (ClassCastException cce) {
            handlePermissionException(address);
        }
    }

    @Override
    public void setHalfWord(int address, short value) {
        try {
            int effectiveAddress = address & (getSize() - 1);
            MappedMemory mappedModule = getMappedModule(effectiveAddress);
            ((WriteOnlyMemory) mappedModule).setHalfWord(effectiveAddress - mappedModule.getStart(), value);
        } catch (BusError be) {
            handleBusError(address, be);
        } catch (ClassCastException cce) {
            handlePermissionException(address);
        }
    }

    @Override
    public void setWord(int address, int value) {
        try {
            int effectiveAddress = address & (getSize() - 1);
            MappedMemory mappedModule = getMappedModule(effectiveAddress);
            ((WriteOnlyMemory) mappedModule).setWord(effectiveAddress - mappedModule.getStart(), value);
        } catch (BusError be) {
            handleBusError(address, be);
        } catch (ClassCastException cce) {
            handlePermissionException(address);
        }
    }

    protected int handleBusError(int address, BusError be) {
        throw new BusError(address, be.getReason(), be);
    }

    protected int handlePermissionException(int address) {
        throw new BusError(address, Permission);
    }

    protected abstract MappedMemory getMappedModule(int address);
}
