package net.symphonious.longpoll;

import com.lmax.disruptor.EventFactory;

public interface NotificationManager<V> extends EventFactory<V>
{
    void combine(V target, V add);

    void set(V target, V newValue);
}
