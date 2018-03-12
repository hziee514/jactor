package me.wrh.jactor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author wurenhai
 * @since 2018/3/11
 */
public class Props {

    public static Props None = new Props();

    public static Props with(Object...args) {
        return new Props(args);
    }

    private final Collection<Object> values;

    private Props(Object...args) {
        values = new ArrayList<>(args.length);
        for (Object arg : args) {
            values.add(arg);
        }
    }

    public int getCount() {
        return values.size();
    }

    public Collection<Object> getValues() {
        return Collections.unmodifiableCollection(values);
    }

}
