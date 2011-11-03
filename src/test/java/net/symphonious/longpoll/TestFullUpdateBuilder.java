package net.symphonious.longpoll;

public class TestFullUpdateBuilder implements FullUpdateBuilder<TestValue>
{
    private final TestValue fullUpdate = new TestValue("Full Update");
    private volatile long sequence;

    public TestValue getFullUpdate()
    {
        return fullUpdate;
    }

    public void onEvent(final TestValue event, final long sequence, final boolean endOfBatch) throws Exception
    {
        this.sequence = sequence;
        synchronized (this)
        {
            notifyAll();
        }
    }

    public void waitForSequenceToReach(final long expectedSequence)
    {
        while (sequence < expectedSequence)
        {
            synchronized (this)
            {
                try
                {
                    wait();
                }
                catch (InterruptedException e)
                {
                    // Ignore
                }
            }
        }
    }
}
