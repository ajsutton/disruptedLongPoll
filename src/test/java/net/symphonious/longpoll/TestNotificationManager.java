package net.symphonious.longpoll;

public class TestNotificationManager implements NotificationManager<TestValue>
{
    public void combine(final TestValue target, final TestValue add)
    {
        target.append(add.getValue());
    }

    public void set(final TestValue target, final TestValue newValue)
    {
        target.setValue(newValue.getValue());
    }

    public TestValue newInstance()
    {
        return new TestValue("");
    }
}
