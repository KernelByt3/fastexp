package exp.nefor.client.event.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {

    private static final Logger LOGGER = LoggerFactory.getLogger("Nefor/event");

    private record Listener(Object owner, Method method) {}

    private static final Map<Class<? extends Event>, List<Listener>> LISTENERS = new ConcurrentHashMap<>();

    public static void subscribe(Object object) {
        for (Class<?> type = object.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(EventHandler.class)) continue;

                Class<?>[] params = method.getParameterTypes();
                if (params.length != 1 || !Event.class.isAssignableFrom(params[0])) {
                    LOGGER.warn("@EventHandler {}#{} must take a single Event argument",
                            type.getSimpleName(), method.getName());
                    continue;
                }

                method.setAccessible(true);
                List<Listener> listeners = LISTENERS.computeIfAbsent(
                        (Class<? extends Event>) params[0], key -> new CopyOnWriteArrayList<>());
                Listener listener = new Listener(object, method);
                if (!listeners.contains(listener)) listeners.add(listener);
            }
        }
    }

    public static void unsubscribe(Object object) {
        for (List<Listener> listeners : LISTENERS.values()) {
            listeners.removeIf(listener -> listener.owner() == object);
        }
    }

    public static <T extends Event> T post(T event) {
        List<Listener> listeners = LISTENERS.get(event.getClass());
        if (listeners == null) return event;

        for (Listener listener : listeners) {
            try {
                listener.method().invoke(listener.owner(), event);
            } catch (Throwable throwable) {
                LOGGER.error("Event handler {}#{} failed for {}",
                        listener.owner().getClass().getSimpleName(),
                        listener.method().getName(), event.getClass().getSimpleName(), throwable);
            }
        }
        return event;
    }
}
