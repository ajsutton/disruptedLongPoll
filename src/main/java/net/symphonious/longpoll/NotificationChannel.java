package net.symphonious.longpoll;

import com.lmax.disruptor.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NotificationChannel<V>
{
    private ExecutorService executor;
    private RingBuffer<V> notifications;
    private final BatchEventProcessor<V> eventProcessor;
    private NotificationManager<V> notificationManager;
    private V fullUpdateMessage;
    private long maximumUpdatesToSend;

    public NotificationChannel(final NotificationManager<V> notificationManager, final int maxNotificationBufferSize, final long maximumUpdatesToSend)
    {
        if (maximumUpdatesToSend >= maxNotificationBufferSize)
        {
            throw new IllegalArgumentException("maximumUpdatesToSend must be smaller than maxNotificationsBufferSize.");
        }
        this.notificationManager = notificationManager;
        this.maximumUpdatesToSend = maximumUpdatesToSend;

        executor = Executors.newCachedThreadPool();

        fullUpdateMessage = notificationManager.newInstance();

        notifications = new RingBuffer<V>(notificationManager, maxNotificationBufferSize, ClaimStrategy.Option.MULTI_THREADED, WaitStrategy.Option.BLOCKING);
        eventProcessor = new BatchEventProcessor<V>(notifications, notifications.newBarrier(), new FullUpdateBuilder());
        executor.submit(eventProcessor);
        notifications.setGatingSequences(eventProcessor.getSequence());
    }

    public V getNotificationToSend(long lastSequenceReceived)
    {
        final long cursor = notifications.getCursor();
        if (needsFullUpdate(cursor, lastSequenceReceived))
        {
            return fullUpdateMessage;
        }
        else if (cursor > lastSequenceReceived)
        {
            V messageToSend = null;
            for (long i = Math.max(lastSequenceReceived, 0); i <= cursor; i++)
            {
                final V notificationMessage = notifications.get(i);
                if (messageToSend == null)
                {
                    messageToSend = notificationMessage;
                }
                else
                {
                    notificationManager.combine(messageToSend, notificationMessage);
                }
            }
            return messageToSend;
        }
        return null;
    }

    private boolean needsFullUpdate(final long cursor, final long lastSequenceReceived)
    {
        return cursor >= 0 && lastSequenceReceived + maximumUpdatesToSend < cursor;
    }

    public void publish(final V value)
    {
        final long sequence = notifications.next();
        notificationManager.set(notifications.get(sequence), value);
        notifications.publish(sequence);
    }

    public void shutdown(final long timeout, final TimeUnit timeUnit) throws InterruptedException
    {
        eventProcessor.halt();
        executor.shutdown();
        executor.awaitTermination(timeout, timeUnit);
    }

    public void waitForSequenceToReach(final long sequenceNumber) throws AlertException, InterruptedException
    {
        notifications.newBarrier(eventProcessor.getSequence()).waitFor(sequenceNumber);
    }

    private class FullUpdateBuilder implements EventHandler<V>
    {
        public void onEvent(final V message, final long sequence, final boolean endOfBatch) throws Exception
        {
            notificationManager.combine(fullUpdateMessage, message);
        }
    }
}
