package com.github.intheclouddan.intellijpluginld.featurestore

import com.launchdarkly.sdk.LDValue
import com.launchdarkly.sdk.server.interfaces.DataStore
import com.launchdarkly.sdk.server.interfaces.DataStoreFactory
import com.launchdarkly.sdk.server.interfaces.DataStoreUpdates
import com.launchdarkly.sdk.server.interfaces.DiagnosticDescription
import com.sun.tools.javac.util.BasicDiagnosticFormatter.BasicConfiguration
import org.apache.http.client.protocol.ClientContext


class InMemoryDataStoreFactory : DataStoreFactory, DiagnosticDescription {
    fun createDataStore(context: ClientContext?, dataStoreUpdates: DataStoreUpdates = DataStoreUpdatesImpl()): DataStore {
        return InMemoryDataStore()
    }

    fun describeConfiguration(basicConfiguration: BasicConfiguration?): LDValue {
        return LDValue.of("memory")
    }

    companion object {
        val INSTANCE: DataStoreFactory = InMemoryDataStoreFactory()
    }
}