package com.github.intheclouddan.intellijpluginld.featurestore

import com.launchdarkly.sdk.server.Components
import com.launchdarkly.sdk.server.DataModel
import com.launchdarkly.sdk.server.LDClient
import com.launchdarkly.sdk.server.LDConfig
import com.launchdarkly.sdk.server.interfaces.ClientContext
import com.launchdarkly.sdk.server.interfaces.DataStore
import com.launchdarkly.sdk.server.interfaces.DataStoreFactory
import com.launchdarkly.sdk.server.interfaces.DataStoreTypes.ItemDescriptor
import com.launchdarkly.sdk.server.interfaces.DataStoreTypes.KeyedItems
import com.launchdarkly.sdk.server.interfaces.DataStoreUpdates
import java.util.*


// This class allows us to access the LDClient's DataStore directly after the client has been created.
// It assumes that you do not create more than one client instance per instance of this class.
class DataStoreFactoryWithStoreAccess internal constructor(underlyingFactory: DataStoreFactory) : DataStoreFactory {
    private val underlyingFactory: DataStoreFactory

    @Volatile
    private var storeInstance: DataStore? = null

    override fun createDataStore(context: ClientContext?, dataStoreUpdates: DataStoreUpdates?): DataStore? {
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

fun createClientAndGetStore(token: String): Pair<DataStore?, LDClient> {
    val storeFactory = DataStoreFactoryWithStoreAccess(Components.inMemoryDataStore())
    val config: LDConfig = LDConfig.Builder()
            .dataStore(storeFactory)
            .build()
    val client = LDClient(token, config)
    // Creating the client causes the SDK to call createDataStore on the factory
    val dataStore: DataStore? = storeFactory.getStoreInstance()
    // do something with them

    return Pair(dataStore, client)
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

fun getFlagAsJSONString(store: DataStore, key: String): String {
    var ret: String = ""
    val items: KeyedItems<ItemDescriptor> = store.getAll(DataModel.FEATURES)
    for (entry in items.getItems()) {
        if (entry.key == key) {
            val json: String = DataModel.FEATURES.serialize(entry.value)
            ret = json
        }
    }
    return ret
}