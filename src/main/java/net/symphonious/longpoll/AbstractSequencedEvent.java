package net.symphonious.longpoll;

public class AbstractSequencedEvent implements SequencedEvent
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
