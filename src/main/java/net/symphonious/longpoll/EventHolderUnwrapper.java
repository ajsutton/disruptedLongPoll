package net.symphonious.longpoll;

import com.lmax.disruptor.EventHandler;

class EventHolderUnwrapper<T> implements EventHandler<EventHolder<T>>
{
    private final FullUpdateBuilder<T> fullUpdateBuilder;

    public EventHolderUnwrapper(final FullUpdateBuilder<T> fullUpdateBuilder)
    {
        this.fullUpdateBuilder = fullUpdateBuilder;
    }

    public void onEvent(final EventHolder<T> event, final long sequence, final boolean endOfBatch) throws Exception
    {
        fullUpdateBuilder.onEvent(event.getEvent(), sequence, endOfBatch);
    }
}
