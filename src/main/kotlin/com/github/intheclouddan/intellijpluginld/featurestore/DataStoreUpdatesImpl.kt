package com.github.intheclouddan.intellijpluginld.featurestore

import com.launchdarkly.sdk.server.EventBroadcasterImpl
import java.util.concurrent.atomic.AtomicReference

internal class DataStoreUpdatesImpl(
        statusBroadcaster: EventBroadcasterImpl<DataStoreStatusProvider.StatusListener?, DataStoreStatusProvider.Status?>?
) : DataStoreUpdates {
    // package-private because it's convenient to use these from DataStoreStatusProviderImpl
    val statusBroadcaster: EventBroadcasterImpl<DataStoreStatusProvider.StatusListener?, DataStoreStatusProvider.Status?>?
    val lastStatus: AtomicReference<DataStoreStatusProvider.Status?>?
    override fun updateStatus(newStatus: DataStoreStatusProvider.Status?) {
        if (newStatus != null) {
            val oldStatus: DataStoreStatusProvider.Status? = lastStatus!!.getAndSet(newStatus)
            if (newStatus != oldStatus) {
                statusBroadcaster.broadcast(newStatus)
            }
        }
    }

    init {
        this.statusBroadcaster = statusBroadcaster
        lastStatus = AtomicReference<DataStoreStatusProvider.Status?>(DataStoreStatusProvider.Status(true, false)) // initially "available"
    }
}