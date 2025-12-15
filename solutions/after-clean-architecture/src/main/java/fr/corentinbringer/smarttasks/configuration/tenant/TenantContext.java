package fr.corentinbringer.smarttasks.configuration.tenant;

public class TenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    public static void setTenant(String tenant) {
        CURRENT.set(tenant);
    }

    public static String getTenant() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
