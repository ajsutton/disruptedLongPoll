package net.symphonious.longpoll;

import com.lmax.disruptor.EventFactory;

class EventHolder<T>
{
    public static <T> EventFactory<EventHolder<T>> getFactory()
    {
        return new EventFactory<EventHolder<T>>()
        {
            public EventHolder<T> newInstance()
            {
                return new EventHolder<T>();
            }
        };
    }

    private T event;

    public T getEvent()
    {
        return event;
    }

    public void setEvent(final T event)
    {
        this.event = event;
    }
}
