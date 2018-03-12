package me.wrh.jactor;

/**
 * @author wurenhai
 * @since 2018/3/11
 */
public class ActorPath {

    public static final String RootName = "/";

    public static final String UsrName = "usr";

    public static final String SysName = "sys";

    public static final String SystemName = "@";

    private final String parent;

    private final String value;

    public String getName() {
        return value.substring(value.lastIndexOf("/") + 1);
    }

    public String getValue() {
        return value;
    }

    private ActorPath(String parent, String name) {
        this.parent = parent;
        if (isRoot(parent)) {
            value = parent + name;
        } else if (isRoot(name)) {
            value = name;
        } else {
            value = parent + "/" + name;
        }
    }

    ActorPath(String parent) {
        this.parent = parent;
        this.value = parent;
    }

    public boolean isRoot() {
        return parent.equals(SystemName) && value.equals(RootName);
    }

    private boolean isRoot(String path) {
        return path.equals(RootName);
    }

    public ActorPath withName(String name) {
        return new ActorPath(value, name);
    }

    @Override
    public String toString() {
        return "ActorPath: " + value;
    }

}
