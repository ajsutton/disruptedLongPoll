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

/**
 * Channel to send and receive notification through.
 *
 * @param <T> type of events to send through the notification channel.
 * @author <a href="http://www.symphonious.net/" target="_top">Adrian Sutton</a>
 */
public class NotificationChannel<T extends SequencedNotification>
{
    private static final Logger LOGGER = Logger.getLogger(NotificationChannel.class.getName());

    private ExecutorService executor;
    private RingBuffer<EventHolder<T>> notifications;
    private long maximumUpdatesToSend;
    private final SequenceBarrier notificationReadyBarrier;
    private final Disruptor<EventHolder<T>> disruptor;
    private final FullUpdateBuilder<T> fullUpdateBuilder;

    /**
     * Create a new notifications channel.
     *
     * @param fullUpdateBuilder the event handler that builds up a full event for new consumers.
     * @param maxNotificationBufferSize the maximum number of notifications to buffer. Must be a power of 2.
     * @param maximumUpdatesToSend the maximum number of notifications to send to client. Clients which fall further behind
     * than this limit will be sent a full update instead.
     */
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

    /**
     * Retrieve the notifications available to send to a client which last received sequence <i>lastSequenceReceived</i>.
     *
     * @param lastSequenceReceived the sequence number of the last notification received by the client.
     * @return a collection of notification to send to the client in order to bring it up to date.
     */
    public Collection<T> getNotificationsToSend(final long lastSequenceReceived)
    {
        return getNotificationsToSend(lastSequenceReceived, new ArrayList<T>());
    }

    /**
     *
     * Retrieve the notifications available to send to a client which last received sequence <i>lastSequenceReceived</i>.
     * This is a garbage free variant of {@link #getNotificationsToSend(long)}.
     *
     * @param lastSequenceReceived the sequence number of the last notification received by the client.
     * @param notificationsToSend the collection to add notifications to send to.
     * @return a collection of notification to send to the client in order to bring it up to date.
     */
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

    /**
     * Publish a notification to the channel.
     *
     * @param notification the notification to publish.
     */
    public void publish(final T notification)
    {
        final long sequence = notifications.next();
        notifications.get(sequence).setEvent(notification);
        notification.setSequence(sequence);
        notifications.publish(sequence);
    }

    /**
     * Asynchronously waits for the next notification after <i>lastSequenceReceived</i> and then calls {@link javax.servlet.AsyncContext#dispatch()}
     * on the <i>asyncContext</i>.
     *
     * @param lastSequenceReceived the last sequence number received by the client.
     * @param asyncContext the context to dispatch when a new notification is received.
     */
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

    /**
     * Shutdown the notification channel.  Any running threads are terminated.
     *
     * @param timeout the maximum time to wait
     * @param timeUnit the time unit of the timeout argument
     * @return <tt>true</tt> if the channel terminated and
     *         <tt>false</tt> if the timeout elapsed before termination
     * @throws InterruptedException if interrupted while waiting.
     */
    public boolean shutdown(final long timeout, final TimeUnit timeUnit) throws InterruptedException
    {
        disruptor.halt();
        executor.shutdown();
        return executor.awaitTermination(timeout, timeUnit);
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
