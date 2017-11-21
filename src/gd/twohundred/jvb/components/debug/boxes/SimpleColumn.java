package gd.twohundred.jvb.components.debug.boxes;

import org.jline.utils.AttributedStringBuilder;

public abstract class SimpleColumn<T> implements Table.Column {
    private final String name;
    private final int width;

    public SimpleColumn(String name, int width) {
        this.name = name;
        this.width = width;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int minWidth() {
        return width;
    }

    @Override
    public boolean fixedWidth() {
        return true;
    }

    @Override
    public void cell(AttributedStringBuilder asb, int row, int width) {
        T[] objects = getObjects();
        if (row < objects.length) {
            cell(asb, objects[row]);
        }
    }

    protected abstract T[] getObjects();

    protected abstract void cell(AttributedStringBuilder asb, T obj);
}
