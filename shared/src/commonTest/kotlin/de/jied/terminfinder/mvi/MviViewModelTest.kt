package de.jied.terminfinder.mvi

import de.jied.terminfinder.feature.MainDispatcherTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.ContinuationInterceptor
import kotlin.test.Test
import kotlin.test.assertEquals

private data class Work(val value: Int)

private class Subject(
    handler: EffectHandler<Work, Int>,
    background: CoroutineDispatcher,
) : MviViewModel<Int, Int, Work, String>(0, handler, background) {

    override fun dispatch(intent: Int) = reduce { (this + intent).noEffect }

    fun start(work: Work) = reduce { perform(work) }

    fun announce(vararg notes: String) = reduce { send(*notes) }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MviViewModelTest : MainDispatcherTest() {

    @Test
    fun effectsRunOnTheBackgroundDispatcherAndTheirIntentComesBack() = runTest {
        var ranOn: ContinuationInterceptor? = null
        val background = UnconfinedTestDispatcher()
        val subject = Subject(
            handler = { work ->
                ranOn = currentCoroutineContext()[ContinuationInterceptor]
                work.value * 2
            },
            background = background,
        )

        subject.start(Work(21))

        assertEquals(background, ranOn)
        assertEquals(42, subject.state.value)
    }

    @Test
    fun aNewerEffectOfTheSameKindSupersedesTheOneInFlight() = runTest {
        val gate = CompletableDeferred<Unit>()
        val started = mutableListOf<Int>()
        val finished = mutableListOf<Int>()
        val subject = Subject(
            handler = { work ->
                started += work.value
                if (work.value == 1) gate.await()
                finished += work.value
                work.value
            },
            background = dispatcher,
        )

        subject.start(Work(1))
        subject.start(Work(2))
        gate.complete(Unit)

        assertEquals(listOf(1, 2), started)
        assertEquals(listOf(2), finished)
        assertEquals(2, subject.state.value)
    }

    @Test
    fun messagesArriveInOrder() = runTest {
        val subject = Subject(handler = { null }, background = dispatcher)

        subject.announce("first", "second")

        assertEquals(listOf("first", "second"), subject.messages.take(2).toList())
    }

    @Test
    fun aReductionWithoutMessagesSendsNothing() = runTest {
        val subject = Subject(handler = { null }, background = dispatcher)

        subject.dispatch(1)
        subject.dispatch(1)
        subject.announce("only one")

        assertEquals(2, subject.state.value)
        assertEquals(listOf("only one"), subject.messages.take(1).toList())
    }
}
