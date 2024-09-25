@file:Suppress("UNCHECKED_CAST")

import java.util.concurrent.atomic.*
import kotlin.math.absoluteValue

class ConcurrentHashTable<K : Any, V : Any>(initialCapacity: Int) {
    private val table = AtomicReference(Table<K, V>(initialCapacity))

    fun put(key: K, value: V): V? {
        while (true) {
            val currentTable = table.get()
            val putResult = currentTable.put(key, value)
            if (putResult === NEEDS_REHASH) {
                currentTable.resize()
                table.compareAndSet(currentTable, currentTable.nextTable.get())
            } else {
                return putResult as V?
            }
        }
    }

    fun get(key: K): V? {
        return table.get().get(key)
    }

    fun remove(key: K): V? {
        return table.get().remove(key)
    }

    class Table<K : Any, V : Any>(val capacity: Int) {
        data class Fixed(val value: Any)

        val keys = AtomicReferenceArray<Any?>(capacity)
        val values = AtomicReferenceArray<Any?>(capacity)
        val nextTable = AtomicReference<Table<K, V>?>(null)

        fun put(key: K, value: V): Any? {
            var index = index(key)
            repeat(MAX_PROBES) {
                while (true) {
                    val curKey = keys[index]
                    when (curKey) {
                        null -> {
                            if (!keys.compareAndSet(index, null, key)) continue
                            if (!values.compareAndSet(index, null, value)) continue
                            return null
                        }
                        key -> {
                            while (true) {
                                val currentValue = values[index]
                                if (currentValue == MOVED) return nextTable.get()!!.put(key, value)
                                if (currentValue is Fixed) {
                                    resize()
                                    continue
                                }
                                if (!values.compareAndSet(index, currentValue, value)) continue
                                return currentValue
                            }
                        }
                    }
                    index = (index + 1) % capacity
                    break
                }
            }
            return NEEDS_REHASH
        }

        fun get(key: K): V? {
            var index = index(key)
            repeat(MAX_PROBES) {
                val curKey = keys[index]
                when (curKey) {
                    key -> {
                        val curValue = values[index]
                        if (curValue == MOVED) return nextTable.get()!!.get(key)
                        if (curValue is Fixed) return curValue.value as V?
                        return values[index] as V?
                    }
                    null -> {
                        return null
                    }
                }
                index = (index + 1) % capacity
            }
            return null
        }

        fun remove(key: K): V? {
            var index = index(key)
            repeat(MAX_PROBES) {
                val curKey = keys[index]
                when (curKey) {
                    key -> {
                        while (true) {
                            val curValue = values[index]
                            if (curValue == MOVED) return nextTable.get()!!.remove(key)
                            if (curValue is Fixed) {
                                resize()
                                continue
                            }
                            if (!values.compareAndSet(index, curValue, null)) continue
                            return curValue as V?
                        }
                    }
                    null -> {
                        return null
                    }
                }
                index = (index + 1) % capacity
            }
            return null
        }

        fun resize() {
            nextTable.compareAndSet(null, Table<K, V>(capacity * 2))
            val next = nextTable.get()!!

            for (i in 0 ..< capacity) {
                while (true) {
                    val curKey = keys[i]
                    val curValue = values[i]

                    // if already moved continue
                    if (curKey == MOVED) break

                    // if empty or removed -> MOVED
                    if (curValue == null) {
                        if (!values.compareAndSet(i, null, MOVED)) continue
                        break
                    }

                    // if fixed -> put value and set MOVED
                    if (curValue is Fixed) {
                        // Set value in new table
                        next.put(curKey as K, curValue.value as V)

                        // Set value to moved in this table
                        if (!values.compareAndSet(i, curValue, MOVED)) continue
                        break
                    }

                    // if normal value -> fix
                    values.compareAndSet(i, curValue, Fixed(curValue))
                }
            }
        }
        private fun index(key: Any) = ((key.hashCode() * MAGIC) % capacity).absoluteValue
    }
}

private const val MAGIC = -0x61c88647 // golden ratio 
private const val MAX_PROBES = 2
private val NEEDS_REHASH = Any()
private val MOVED = Any()
