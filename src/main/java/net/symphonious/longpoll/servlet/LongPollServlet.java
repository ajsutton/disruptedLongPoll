package net.symphonious.longpoll.servlet;

import net.symphonious.longpoll.FullUpdateBuilder;
import net.symphonious.longpoll.NotificationChannel;
import net.symphonious.longpoll.SequencedEvent;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;

public abstract class LongPollServlet<T extends SequencedEvent> extends HttpServlet
{
    public static final String SEQUENCE_PARAM_NAME = "sequence";
    private final int maximumNotificationBufferSize;
    private final long maximumUpdatesToSend;
    private NotificationChannel<T> notificationChannel;

    public LongPollServlet(final int maximumNotificationBufferSize, final long maximumUpdatesToSend)
    {
        this.maximumNotificationBufferSize = maximumNotificationBufferSize;
        this.maximumUpdatesToSend = maximumUpdatesToSend;
    }

    @Override
    public void init() throws ServletException
    {
        notificationChannel = new NotificationChannel<T>(getFullUpdateBuilder(), maximumNotificationBufferSize, maximumUpdatesToSend);
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        final long lastReceivedSequence = getLastReceivedSequence(request);
        final Collection<T> notificationsToSend = notificationChannel.getNotificationsToSend(lastReceivedSequence);
        if (notificationsToSend.size() > 0)
        {
            sendNotifications(notificationsToSend);
        }
        else
        {
            notificationChannel.dispatchOnNextNotification(lastReceivedSequence, request.startAsync());
        }
    }

    protected long getLastReceivedSequence(final HttpServletRequest request)
    {
        return Long.parseLong(request.getParameter(SEQUENCE_PARAM_NAME));
    }

    protected abstract void sendNotifications(final Collection<T> notificationsToSend);

    protected abstract FullUpdateBuilder<T> getFullUpdateBuilder();
}
