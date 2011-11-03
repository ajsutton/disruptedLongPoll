package net.symphonious.longpoll;

import com.lmax.disruptor.EventHandler;

public interface FullUpdateBuilder<T> extends EventHandler<T>
{
    T getFullUpdate();
}
