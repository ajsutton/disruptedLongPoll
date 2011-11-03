package net.symphonious.longpoll;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;

import javax.servlet.AsyncContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class NotificationChannel<T extends SequencedEvent>
{
    private static final Logger LOGGER = Logger.getLogger(NotificationChannel.class.getName());

    private ExecutorService executor;
    private RingBuffer<EventHolder<T>> notifications;
    private long maximumUpdatesToSend;
    private final SequenceBarrier notificationReadyBarrier;
    private final Disruptor<EventHolder<T>> disruptor;
    private final FullUpdateBuilder<T> fullUpdateBuilder;

    public NotificationChannel(final FullUpdateBuilder<T> fullUpdateBuilder,
                               final int maxNotificationBufferSize, final long maximumUpdatesToSend)
    {
        validateSizes(maxNotificationBufferSize, maximumUpdatesToSend);
        this.fullUpdateBuilder = fullUpdateBuilder;
        this.maximumUpdatesToSend = maximumUpdatesToSend;

        executor = Executors.newCachedThreadPool();

        disruptor = new Disruptor<EventHolder<T>>(EventHolder.<T>getFactory(), maxNotificationBufferSize, executor, ClaimStrategy.Option.MULTI_THREADED,
                                                        WaitStrategy.Option.BLOCKING);
        final EventHolderUnwrapper<T> fullUpdateBuilderProcessorThingy = new EventHolderUnwrapper<T>(fullUpdateBuilder);
        disruptor.handleEventsWith(fullUpdateBuilderProcessorThingy);
        disruptor.start();
        notifications = disruptor.getRingBuffer();
        notificationReadyBarrier = notifications.newBarrier();
    }

    public Collection<T> getNotificationsToSend(final long lastSequenceReceived)
    {
        return getNotificationsToSend(lastSequenceReceived, new ArrayList<T>());
    }

    public Collection<T> getNotificationsToSend(final long lastSequenceReceived, final Collection<T> notificationsToSend)
    {
        final long cursor = notifications.getCursor();
        if (needsFullUpdate(cursor, lastSequenceReceived))
        {
            notificationsToSend.add(fullUpdateBuilder.getFullUpdate());
        }
        else if (cursor > lastSequenceReceived)
        {
            for (long i = Math.max(lastSequenceReceived, 0); i <= cursor; i++)
            {
                notificationsToSend.add(notifications.get(i).getEvent());
            }
        }
        return notificationsToSend;
    }

    public void publish(final T value)
    {
        final long sequence = notifications.next();
        notifications.get(sequence).setEvent(value);
        value.setSequence(sequence);
        notifications.publish(sequence);
    }

    public void shutdown(final long timeout, final TimeUnit timeUnit) throws InterruptedException
    {
        disruptor.halt();
        executor.shutdown();
        executor.awaitTermination(timeout, timeUnit);
    }

    public void dispatchOnNextNotification(final long lastSequenceReceived, final AsyncContext asyncContext)
    {
        asyncContext.start(new Runnable()
        {
            public void run()
            {
                try
                {
                    notificationReadyBarrier.waitFor(lastSequenceReceived + 1);
                }
                catch (AlertException e)
                {
                    LOGGER.info("Received alert while waiting for next notification.");
                }
                catch (InterruptedException e)
                {
                    LOGGER.info("Interrupted while waiting for next notification.");
                }
                asyncContext.dispatch();
            }
        });
    }

    private boolean needsFullUpdate(final long cursor, final long lastSequenceReceived)
    {
        return cursor >= 0 && lastSequenceReceived + maximumUpdatesToSend < cursor;
    }

    private void validateSizes(final int maxNotificationBufferSize, final long maximumUpdatesToSend)
    {
        if (maximumUpdatesToSend >= maxNotificationBufferSize)
        {
            throw new IllegalArgumentException("maximumUpdatesToSend must be smaller than maxNotificationsBufferSize.");
        }
    }
}
