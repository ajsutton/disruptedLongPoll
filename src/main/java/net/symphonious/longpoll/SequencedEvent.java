package net.symphonious.longpoll;

public interface SequencedEvent
{
    void setSequence(long sequence);

    long getSequence();
}
