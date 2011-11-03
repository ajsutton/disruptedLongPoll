package net.symphonious.longpoll;

import org.junit.After;
import org.junit.Test;

import javax.servlet.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class NotificationChannelTest
{
    private NotificationChannel<TestValue> notificationChannel;
    private final TestFullUpdateBuilder fullUpdateBuilder = new TestFullUpdateBuilder();

    private void createNotificationChannel()
    {
        notificationChannel = new NotificationChannel<TestValue>(fullUpdateBuilder, 16, 8);
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
        new NotificationChannel<TestValue>(fullUpdateBuilder, 16, 20);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfMaximumUpdatesToSendIsSameAsBufferSize() throws Exception
    {
        new NotificationChannel<TestValue>(fullUpdateBuilder, 16, 16);
    }

    @Test
    public void shouldNotHaveUpdatesBeforeUpdatesAreAdded() throws Exception
    {
        createNotificationChannel();
        final Collection<TestValue> expectedNotifications = new ArrayList<TestValue>();
        assertThat(notificationChannel.getNotificationsToSend(Long.MIN_VALUE), is(expectedNotifications));
    }

    @Test
    public void shouldHaveASingleUpdateWhenOneIsAdded() throws Exception
    {
        createNotificationChannel();
        final TestValue value = new TestValue("Value");
        notificationChannel.publish(value);
        fullUpdateBuilder.waitForSequenceToReach(0);

        assertNotificationsToSendAre(notificationChannel.getNotificationsToSend(-1), value);
    }

    @Test
    public void shouldCombineMultipleEvents() throws Exception
    {
        createNotificationChannel();
        final TestValue value1 = new TestValue("1");
        final TestValue value2 = new TestValue("2");
        notificationChannel.publish(value1);
        notificationChannel.publish(value2);
        fullUpdateBuilder.waitForSequenceToReach(1);

        assertNotificationsToSendAre(notificationChannel.getNotificationsToSend(-1), value1, value2);
    }

    @Test
    public void shouldSendFullUpdateIfReceiverIsTooFarBehind() throws Exception
    {
        createNotificationChannel();
        for (int i = 0; i < 50; i++)
        {
            notificationChannel.publish(new TestValue(String.valueOf(i)));
        }

        fullUpdateBuilder.waitForSequenceToReach(49);


        assertNotificationsToSendAre(notificationChannel.getNotificationsToSend(Long.MIN_VALUE), fullUpdateBuilder.getFullUpdate());
    }

    @Test
    public void shouldWaitForNextMessageAndDispatchAsyncContext() throws Exception
    {
        final StubAsyncContext asyncContext = new StubAsyncContext();

        createNotificationChannel();
        notificationChannel.dispatchOnNextNotification(-1, asyncContext);
        assertFalse("Context was dispatched before next message was received.", asyncContext.dispatched);

        notificationChannel.publish(new TestValue("1"));

        assertDispatched(asyncContext);
    }

    private void assertDispatched(final StubAsyncContext asyncContext)
    {
        final long waitStartTime = System.currentTimeMillis();
        while (!asyncContext.dispatched && System.currentTimeMillis() - waitStartTime < 3000)
        {
            Thread.yield();
        }
        assertTrue("Context was not dispatched once message was received.", asyncContext.dispatched);
    }

    private void assertNotificationsToSendAre(final Collection<TestValue> notificationsToSend, final TestValue... values)
    {
        final Collection<TestValue> expectedValues = Arrays.asList(values);
        assertThat(notificationsToSend, is(expectedValues));
    }

    private static class StubAsyncContext implements AsyncContext
    {

        volatile boolean dispatched;

        public ServletRequest getRequest()
        {
            throw new UnsupportedOperationException("Not implemented");
        }

        public ServletResponse getResponse()
        {
            throw new UnsupportedOperationException("Not implemented");
        }

        public boolean hasOriginalRequestAndResponse()
        {
            throw new UnsupportedOperationException("Not implemented");
        }

        public void dispatch()
        {
            dispatched = true;
        }

        public void dispatch(final String s)
        {
            throw new UnsupportedOperationException("Not implemented");
        }

        public void dispatch(final ServletContext servletContext, final String s)
        {
            throw new UnsupportedOperationException("Not implemented");
        }

        public void complete()
        {
            throw new UnsupportedOperationException("Not implemented");
        }

        public void start(final Runnable runnable)
        {
            new Thread(runnable).start();
        }

        public void addListener(final AsyncListener asyncListener)
        {
            throw new UnsupportedOperationException("Not implemented");
        }

        public void addListener(final AsyncListener asyncListener, final ServletRequest servletRequest, final ServletResponse servletResponse)
        {
            throw new UnsupportedOperationException("Not implemented");
        }

        public <T extends AsyncListener> T createListener(final Class<T> tClass) throws ServletException
        {
            throw new UnsupportedOperationException("Not implemented");
        }

        public void setTimeout(final long l)
        {
            throw new UnsupportedOperationException("Not implemented");
        }

        public long getTimeout()
        {
            throw new UnsupportedOperationException("Not implemented");
        }
    }
}
