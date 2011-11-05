package net.symphonious.longpoll;

import com.lmax.disruptor.EventHandler;

/**
 * The <tt>FullUpdateBuilder</tt> receives every notification passed through the {@link NotificationChannel} and
 * maintains a single notification event which represents the entire latest state. This full update is sent to
 * any newly connecting clients and also to clients which fall too far behind the latest notifications.
 *
 * <p>It is expected that the <tt>FullUpdateBuilder</tt> will coalesce events in some application defined way.</p>
 *
 * <p>While the full update message will only be modified from a single thread by the <tt>FullUpdateBuilder</tt>,
 * it will be concurrently read by multiple other threads. As such, the update mechanism must be thread safe in the
 * face of concurrent reads, but not necessarily concurrent writes.</p>
 *
 * @param <T> the type of notification this update builder consumes.
 * @author <a href="http://www.symphonious.net/" target="_top">Adrian Sutton</a>
 */
public interface FullUpdateBuilder<T> extends EventHandler<T>
{
    T getFullUpdate();
}
