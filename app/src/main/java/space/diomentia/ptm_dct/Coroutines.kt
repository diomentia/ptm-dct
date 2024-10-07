package space.diomentia.ptm_dct

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

fun CoroutineScope.queueJob(queue: Channel<Job>, block: suspend CoroutineScope.() -> Unit): Job {
    val job = launch(start = CoroutineStart.LAZY, block = block)
    queue.trySend(job)
    return job
}

fun CoroutineScope.runQueue(queue: Channel<Job>) = launch {
    for (job in queue) job.join()
}


fun CoroutineScope.cancelWithQueue(queue: Channel<Job>) {
    queue.cancel()
    cancel()
}

@OptIn(InternalCoroutinesApi::class)
suspend fun Job.await() {
    val kite = Channel<Unit>(Channel.RENDEZVOUS)
    this.invokeOnCompletion(onCancelling = true) {
        kite.trySend(Unit)
    }
    if (!isCompleted && !isCancelled) {
        kite.receive()
    }
}