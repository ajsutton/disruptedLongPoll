Disrupted Long Poll
===================

This is a simple library that uses the [LMAX Disruptor](http://code.google.com/p/disruptor/) to send anonymous notifictions to clients via HTTP long poll.
This is intended for use where every client should receive every notification, possibly with some coalescing. It is poorly suited to situations where
clients are logged in an only recieve a subset of events.  For example, a "live blog" where browsers use long poll to receive the latest update or for
build system notifications.

While the LMAX Disruptor was designed for low latency and high performance, in this library it is primarily being used because the underlying ring buffer
data structure fits particular well with the problem space and makes the solution straight forward. An evaluation of performance for this approach has not
yet been done, but would be welcomed.