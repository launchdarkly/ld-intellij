package com.github.intheclouddan.intellijpluginld.featurestore

import com.launchdarkly.sdk.server.LDConfig
import com.launchdarkly.sdk.server.inMemoryStore
import com.launchdarkly.sdk.server.interfaces.DataStore
import com.launchdarkly.sdk.server.interfaces.DataStoreTypes.KeyedItems
import org.apache.http.client.protocol.ClientContext
import java.util.*


// This class allows us to access the LDClient's DataStore directly after the client has been created.
// It assumes that you do not create more than one client instance per instance of this class.
class DataStoreFactoryWithStoreAccess internal constructor(underlyingFactory: DataStoreFactory) : DataStoreFactory {
    private val underlyingFactory: DataStoreFactory

    @Volatile
    private var storeInstance: DataStore? = null
    fun createDataStore(context: ClientContext?, dataStoreUpdates: DataStoreUpdates?): DataStore? {
        storeInstance = underlyingFactory.createDataStore(context, dataStoreUpdates)
        return storeInstance
    }

    fun getStoreInstance(): DataStore? {
        return storeInstance
    }

    init {
        this.underlyingFactory = underlyingFactory
    }
}

fun createClientAndGetStore() {
    val storeFactory = DataStoreFactoryWithStoreAccess(inMemoryDataStore())
    val config: LDConfig = Builder()
            .dataStore(storeFactory)
            .build()
    val client = LDClient("sdk-key", config)
    // Creating the client causes the SDK to call createDataStore on the factory
    val dataStore: DataStore? = storeFactory.getStoreInstance()
    // do something with them
}

fun getFlagsAsJSONStrings(store: DataStore): List<String>? {
    val ret: MutableList<String> = ArrayList()
    val items: KeyedItems<ItemDescriptor> = store.getAll(DataModel.FEATURES)
    for (entry in items.getItems()) {
        val json: String = DataModel.FEATURES.serialize(entry.value)
        ret.add(json)
    }
    return ret
}