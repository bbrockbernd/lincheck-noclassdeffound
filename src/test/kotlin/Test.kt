import org.jetbrains.kotlinx.lincheck.LoggingLevel
import org.jetbrains.kotlinx.lincheck.Options
import org.jetbrains.kotlinx.lincheck.annotations.Operation
import org.jetbrains.kotlinx.lincheck.check
import org.jetbrains.kotlinx.lincheck.strategy.managed.modelchecking.ModelCheckingOptions
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
abstract class MyLinCheckTestBase {

    @Test
    fun modelCheckingTest() = ModelCheckingOptions()
        .iterations(300)
        .invocationsPerIteration(10_000)
        .actorsBefore(1)
        .threads(3)
        .actorsPerThread(3)
        .actorsAfter(0)
        .checkObstructionFreedom(true)
        .hangingDetectionThreshold(100)
        .sequentialSpecification(SequentialHashTableIntInt::class.java)
        .logLevel(LoggingLevel.INFO)
        .apply { customConfiguration() }
        .check(this::class.java)

    fun Options<*, *>.customConfiguration() {}
}

class MyLinCheckTestImplementation : MyLinCheckTestBase() {
    private val map = ConcurrentHashTable<Int, Int>(2)
    @Operation
    fun put(k: Int, v: Int): Int? = map.put(k, v)

    @Operation
    fun get(k: Int): Int? = map.get(k)

    @Operation
    fun remove(k: Int): Int? = map.remove(k)
}

class SequentialHashTableIntInt {
    private val map = HashMap<Int, Int>()

    fun put(key: Int, value: Int): Int? = map.put(key, value)

    fun get(key: Int): Int? = map.get(key)

    fun remove(key: Int): Int? = map.remove(key)
}


