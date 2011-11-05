package net.symphonious.longpoll;

/**
 * Provides an implementation of the {@link SequencedNotification} interface. Useful as a super class
 * for notification events.
 *
 * @author <a href="http://www.symphonious.net/" target="_top">Adrian Sutton</a>
 */
public abstract class AbstractSequencedNotification implements SequencedNotification
{
    private volatile long sequence;

    public void setSequence(final long sequence)
    {
        this.sequence = sequence;
    }

    public long getSequence()
    {
        return sequence;
    }
}
