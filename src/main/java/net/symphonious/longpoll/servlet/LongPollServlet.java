package net.symphonious.longpoll.servlet;

import net.symphonious.longpoll.FullUpdateBuilder;
import net.symphonious.longpoll.NotificationChannel;
import net.symphonious.longpoll.SequencedNotification;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * A base class that uses the {@link NotificationChannel} to send notification events to clients via long poll. Both GET and POST
 * requests are supported.
 *
 * <p>Requests to the long poll servlet are required to provide a <tt>lastSequence</tt> parameter indicating the sequence number
 * of the last notification they received.  Any negative number, the absence of the parameter or non-integral value indicates that
 * no notifications have been received.</p>
 *
 * <p>The <tt>lastSequence</tt> parameter can be customized using the {@link #LongPollServlet(int, long, String)} constructor.</p>
 *
 * @param <T> the type of notification to be sent.
 * @author <a href="http://www.symphonious.net/" target="_top">Adrian Sutton</a>
 */
public abstract class LongPollServlet<T extends SequencedNotification> extends HttpServlet
{
    private  final String sequenceParamName;
    private final int maximumNotificationBufferSize;
    private final long maximumUpdatesToSend;
    private NotificationChannel<T> notificationChannel;


    /**
     * Create the servlet with specified configuration parameters.
     *
     * @param maximumNotificationBufferSize the maximum number of notifications to buffer. Must be a power of 2.
     * @param maximumUpdatesToSend the maximum number of notifications to send to client. Clients which fall further behind
     * than this limit will be sent a full update instead.
     */
    public LongPollServlet(final int maximumNotificationBufferSize, final long maximumUpdatesToSend)
    {
        this(maximumNotificationBufferSize, maximumUpdatesToSend, "lastSequence");
    }

    /**
     * Create the servlet with specified configuration parameters.
     *
     * @param maximumNotificationBufferSize the maximum number of notifications to buffer. Must be a power of 2.
     * @param maximumUpdatesToSend the maximum number of notifications to send to client. Clients which fall further behind
     * than this limit will be sent a full update instead.
     * @param sequenceParamName the request parameter name the client sends the last received sequence number in.
     */
    public LongPollServlet(final int maximumNotificationBufferSize, final long maximumUpdatesToSend, final String sequenceParamName)
    {
        this.maximumUpdatesToSend = maximumUpdatesToSend;
        this.maximumNotificationBufferSize = maximumNotificationBufferSize;
        this.sequenceParamName = sequenceParamName;
    }

    @Override
    public void init() throws ServletException
    {
        notificationChannel = new NotificationChannel<T>(getFullUpdateBuilder(), maximumNotificationBufferSize, maximumUpdatesToSend);
    }

    @Override
    public void destroy()
    {
        try
        {
            notificationChannel.shutdown(10, SECONDS);
        }
        catch (InterruptedException e)
        {
            // Ignore.
        }
        super.destroy();
    }

    /**
     * Get the notification channel used by this servlet. This can then be used to publish notifications.
     *
     * @return the notification channel.
     */
    @SuppressWarnings("unused")
    protected NotificationChannel<T> getNotificationChannel()
    {
        return notificationChannel;
    }

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        sendNotifications(request, response);
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        sendNotifications(request, response);
    }

    private void sendNotifications(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        final long lastReceivedSequence = getLastReceivedSequence(request);
        final Collection<T> notificationsToSend = notificationChannel.getNotificationsToSend(lastReceivedSequence);
        if (notificationsToSend.size() > 0)
        {
            sendNotifications(request, response, notificationsToSend);
        }
        else
        {
            notificationChannel.dispatchOnNextNotification(lastReceivedSequence, request.startAsync());
        }
    }

    /**
     * Get the last received sequence number from the request.  By default this is done by looking for a <tt>lastSequence</tt>
     * parameter but can be overridden here to customize the behaviour.
     *
     * @param request the request to get the last received sequence number from.
     * @return the last received sequence number. A negative number indicates that no notifications have been received.
     */
    protected long getLastReceivedSequence(final HttpServletRequest request)
    {
        try
        {
            return Long.parseLong(request.getParameter(sequenceParamName));
        }
        catch (NumberFormatException e)
        {
            return -2;
        }
    }

    /**
     * Send the notifications to the client.  This method is responsible for serializing and writing the actual notifications.
     * Notifications may be coalesced by this method so long as the maximum sequence number sent to the client is exactly equal
     * to the maximum sequence number of any notification in <i>notificationsToSend</i>.
     *
     * @param request the incoming request.
     * @param response the response to send notifications on.
     * @param notificationsToSend the notifications to send.
     * @throws ServletException if the request could not be handled.
     * @throws IOException if an IO error is detected while sending the notifications.
     */
    protected abstract void sendNotifications(HttpServletRequest request, HttpServletResponse response, final Collection<T> notificationsToSend)
    throws ServletException, IOException;

    /**
     * Get or create the {@link FullUpdateBuilder} used by the {@link NotificationChannel} to maintain
     * a complete notification event to send to new clients.
     *
     * @return the FullUpdateBulder.
     */
    protected abstract FullUpdateBuilder<T> getFullUpdateBuilder();
}
