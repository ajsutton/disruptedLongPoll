package net.symphonious.longpoll;

import org.junit.After;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class NotificationChannelTest
{
    private NotificationChannel<TestValue> notificationChannel;

    private void createNotificationChannel()
    {
        notificationChannel = new NotificationChannel<TestValue>(new TestNotificationManager(), 16, 8);
    }

    @After
    public void tearDown() throws Exception
    {
        if (notificationChannel != null)
        {
            notificationChannel.shutdown(10, TimeUnit.SECONDS);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfMaximumUpdatesToSendIsBiggerThanBufferSize() throws Exception
    {
        new NotificationChannel<TestValue>(new TestNotificationManager(), 16, 20);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfMaximumUpdatesToSendIsSameAsBufferSize() throws Exception
    {
        new NotificationChannel<TestValue>(new TestNotificationManager(), 16, 16);
    }

    @Test
    public void shouldNotHaveUpdatesBeforeUpdatesAreAdded() throws Exception
    {
        createNotificationChannel();
        assertThat(notificationChannel.getNotificationToSend(Long.MIN_VALUE), is(nullValue(TestValue.class)));
    }

    @Test
    public void shouldHaveASingleUpdateWhenOneIsAdded() throws Exception
    {
        createNotificationChannel();
        notificationChannel.publish(new TestValue("Value"));
        notificationChannel.waitForSequenceToReach(0);

        assertThat(notificationChannel.getNotificationToSend(Long.MIN_VALUE).getValue(), is(equalTo("Value")));
    }

    @Test
    public void shouldCombineMultipleEvents() throws Exception
    {
        createNotificationChannel();
        notificationChannel.publish(new TestValue("1"));
        notificationChannel.publish(new TestValue("2"));
        notificationChannel.waitForSequenceToReach(1);

        assertThat(notificationChannel.getNotificationToSend(Long.MIN_VALUE).getValue(), is(equalTo("12")));
    }

    @Test
    public void shouldSendFullUpdateIfReceiverIsTooFarBehind() throws Exception
    {
        createNotificationChannel();
        final StringBuilder expected = new StringBuilder();
        for (int i = 0; i < 50; i++)
        {
            expected.append(i);
            notificationChannel.publish(new TestValue(String.valueOf(i)));
        }

        notificationChannel.waitForSequenceToReach(49);

        assertThat(notificationChannel.getNotificationToSend(4).getValue(), is(equalTo(expected.toString())));
    }
}
