package net.symphonious.longpoll;

/**
 * Interface for all notifications passed through the {@link NotificationChannel}. Allows notifications to carry a sequence
 * number to track which notification have already been received by a client.
 *
 * @author <a href="http://www.symphonious.net/" target="_top">Adrian Sutton</a>
 */
public interface SequencedNotification
{
    /**
     * Set the sequence number for this notification.
     *
     * @param sequence the sequence for this notification.
     */
    void setSequence(long sequence);

    /**
     * Get the sequence number for this notification.
     *
     * @return the sequence number.
     */
    long getSequence();
}
