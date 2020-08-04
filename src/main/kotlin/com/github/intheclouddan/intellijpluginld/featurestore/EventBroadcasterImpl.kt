package com.github.intheclouddan.intellijpluginld.featurestore

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.function.BiConsumer


/**
 * A generic mechanism for registering event listeners and broadcasting events to them. The SDK maintains an
 * instance of this for each available type of listener (flag change, data store status, etc.). They are all
 * intended to share a single executor service; notifications are submitted individually to this service for
 * each listener.
 *
 * @param <ListenerT> the listener interface class
 * @param <EventT> the event class
</EventT></ListenerT> */
internal class EventBroadcasterImpl<ListenerT, EventT>
/**
 * Creates an instance.
 *
 * @param broadcastAction a lambda that calls the appropriate listener method for an event
 * @param executor the executor to use for running notification tasks on a worker thread; if this
 * is null (which should only be the case in test code) then broadcasting an event will be a no-op
 */(private val broadcastAction: BiConsumer<ListenerT, EventT>, private val executor: ExecutorService?) {
    private val listeners = CopyOnWriteArrayList<ListenerT>()

    /**
     * Registers a listener for this type of event. This method is thread-safe.
     *
     * @param listener the listener to register
     */
    fun register(listener: ListenerT) {
        listeners.add(listener)
    }

    /**
     * Unregisters a listener. This method is thread-safe.
     *
     * @param listener the listener to unregister
     */
    fun unregister(listener: ListenerT) {
        listeners.remove(listener)
    }

    /**
     * Returns true if any listeners are currently registered. This method is thread-safe.
     *
     * @return true if there are listeners
     */
    fun hasListeners(): Boolean {
        return !listeners.isEmpty()
    }

    /**
     * Broadcasts an event to all available listeners.
     *
     * @param event the event to broadcast
     */
    fun broadcast(event: EventT) {
        if (executor == null) {
            return
        }
        for (l in listeners) {
            executor.execute(Runnable {
                try {
                    broadcastAction.accept(l, event)
                } catch (e: Exception) {
                    Loggers.MAIN.warn("Unexpected error from listener ({0}): {1}", l.javaClass, e.toString())
                    Loggers.MAIN.debug(e.toString(), e)
                }
            })
        }
    }

    companion object {
        fun forFlagChangeEvents(executor: ExecutorService?): EventBroadcasterImpl<FlagChangeListener, FlagChangeEvent> {
            return EventBroadcasterImpl<FlagChangeListener, FlagChangeEvent>(BiConsumer<FlagChangeListener, FlagChangeEvent> { obj: FlagChangeListener, flagChangeEvent: FlagChangeEvent? -> obj.onFlagChange(flagChangeEvent) }, executor)
        }

        fun forDataSourceStatus(executor: ExecutorService?): EventBroadcasterImpl<DataSourceStatusProvider.StatusListener, DataSourceStatusProvider.Status> {
            return EventBroadcasterImpl<DataSourceStatusProvider.StatusListener, DataSourceStatusProvider.Status>(BiConsumer<DataSourceStatusProvider.StatusListener, DataSourceStatusProvider.Status> { status: DataSourceStatusProvider.StatusListener? -> DataSourceStatusProvider.StatusListener.dataSourceStatusChanged(status) }, executor)
        }

        fun forDataStoreStatus(executor: ExecutorService?): EventBroadcasterImpl<DataStoreStatusProvider.StatusListener, DataStoreStatusProvider.Status> {
            return EventBroadcasterImpl<DataStoreStatusProvider.StatusListener, DataStoreStatusProvider.Status>(BiConsumer<DataStoreStatusProvider.StatusListener, DataStoreStatusProvider.Status> { status: DataStoreStatusProvider.StatusListener? -> DataStoreStatusProvider.StatusListener.dataStoreStatusChanged(status) }, executor)
        }
    }

}