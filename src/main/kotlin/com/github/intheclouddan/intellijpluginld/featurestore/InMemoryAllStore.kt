package com.github.intheclouddan.intellijpluginld.featurestore


import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.launchdarkly.sdk.server.interfaces
import com.launchdarkly.sdk.server.interfaces.DataStore
import com.launchdarkly.sdk.server.interfaces.DataStoreStatusProvider.CacheStats
import com.launchdarkly.sdk.server.interfaces.DataStoreTypes.DataKind
import com.launchdarkly.sdk.server.interfaces.DataStoreTypes.FullDataSet
import com.launchdarkly.sdk.server.interfaces.DataStoreTypes.ItemDescriptor
import com.launchdarkly.sdk.server.interfaces.DataStoreTypes.KeyedItems
import java.io.IOException


/**
 * A thread-safe, versioned store for feature flags and related data based on a
 * [HashMap]. This is the default implementation of [DataStore].
 *
 * As of version 5.0.0, this is package-private; applications must use the factory method
 * [Components.inMemoryDataStore].
 */
class InMemoryDataStore : DataStore {
    private var initialized = false

    @Volatile
    private var allData: ImmutableMap<DataKind, Map<String, ItemDescriptor>> = ImmutableMap.of<DataKind, Map<String, ItemDescriptor>>()


    private val writeLock = Any()
    override fun init(allData: FullDataSet<ItemDescriptor?>) {
        synchronized(writeLock) {
            val newData: ImmutableMap.Builder<DataKind, Map<String, ItemDescriptor>> = ImmutableMap.builder<DataKind, Map<String, ItemDescriptor>>()
            for (entry in allData.getData()) {
                newData.put(entry.key, ImmutableMap.copyOf<String, ItemDescriptor>(entry.value.getItems()))
            }
            this.allData = newData.build() // replaces the entire map atomically
            this.initialized = true
        }
    }

    override operator fun get(kind: DataKind?, key: String): ItemDescriptor? {
        val items: Map<String, ItemDescriptor> = allData[kind] ?: return null
        return items[key]
    }

    override fun getAll(kind: DataKind?): KeyedItems<ItemDescriptor> {
        val items: Map<String, ItemDescriptor> = allData[kind] ?: return KeyedItems<ItemDescriptor>(null)
        return KeyedItems<ItemDescriptor>(ImmutableList.copyOf<Map.Entry<String, ItemDescriptor>>(items.entries))
    }

    override fun upsert(kind: DataKind, key: String, item: ItemDescriptor): Boolean {
        synchronized(writeLock) {
            val existingItems: Map<String, ItemDescriptor>? = allData[kind]
            var oldItem: ItemDescriptor? = null
            if (existingItems != null) {
                oldItem = existingItems[key]
                if (oldItem != null && oldItem.getVersion() >= item.getVersion()) {
                    return false
                }
            }
            // The following logic is necessary because ImmutableMap.Builder doesn't support overwriting an existing key
            val newData: ImmutableMap.Builder<DataKind, Map<String, ItemDescriptor>> = ImmutableMap.builder<DataKind, Map<String, ItemDescriptor>>()
            for ((key1, value) in allData) {
                if (key1 != kind) {
                    newData.put(key1, value)
                }
            }
            if (existingItems == null) {
                newData.put(kind, ImmutableMap.of<String, ItemDescriptor>(key, item))
            } else {
                val itemsBuilder: ImmutableMap.Builder<String, ItemDescriptor> = ImmutableMap.builder<String, ItemDescriptor>()
                if (oldItem == null) {
                    itemsBuilder.putAll(existingItems)
                } else {
                    for ((key1, value) in existingItems) {
                        if (key1 != key) {
                            itemsBuilder.put(key1, value)
                        }
                    }
                }
                itemsBuilder.put(key, item)
                newData.put(kind, itemsBuilder.build())
            }
            allData = newData.build() // replaces the entire map atomically
            return true
        }
    }

    override fun isInitialized(): Boolean {
        return initialized
    }

    override fun isStatusMonitoringEnabled(): Boolean {
        return false
    }

    override fun getCacheStats(): CacheStats? {
        return null
    }

    /**
     * Does nothing; this class does not have any resources to release
     *
     * @throws IOException will never happen
     */
    @Throws(IOException::class)
    override fun close() {
        return
    }
}