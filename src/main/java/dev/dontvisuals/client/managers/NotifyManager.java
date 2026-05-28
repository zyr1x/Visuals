package dev.dontvisuals.client.managers;

import com.google.common.collect.Lists;
import dev.dontvisuals.dontvisuals;
import dev.dontvisuals.client.events.impl.EventRender2D;
import dev.dontvisuals.modules.api.Module;
import dev.dontvisuals.client.util.Wrapper;
import dev.dontvisuals.client.util.notify.Notify;
import meteordevelopment.orbit.EventHandler;

import java.util.*;

public class NotifyManager implements Wrapper {

    public NotifyManager() {
        dontvisuals.getInstance().getEventHandler().subscribe(this);
    }

    private final List<Notify> notifies = new ArrayList<>();

    public void add(Notify notify) {
        notifies.add(notify);
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (notifies.isEmpty()) return;
        float startY = mc.getWindow().getScaledHeight() / 2f + 26;
        if (notifies.size() > 10) notifies.removeFirst();
        notifies.removeIf(Notify::expired);

        for (Notify notify : Lists.newArrayList(notifies)) {
            startY = (startY - 16f);
            notify.render(e, startY + (notifies.size() * 16f));
        }
    }
}