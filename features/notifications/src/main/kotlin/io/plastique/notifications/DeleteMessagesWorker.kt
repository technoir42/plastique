package io.plastique.notifications

import android.content.Context
import androidx.work.WorkerParameters
import com.google.auto.factory.AutoFactory
import com.google.auto.factory.Provided
import io.plastique.core.work.CompletableWorker
import io.plastique.core.work.ListenableWorkerFactory
import io.reactivex.Completable
import javax.inject.Inject

@AutoFactory(implementing = [ListenableWorkerFactory::class])
class DeleteMessagesWorker @Inject constructor(
    context: Context,
    workerParams: WorkerParameters,
    @Provided private val messageRepository: MessageRepository
) : CompletableWorker(context, workerParams) {

    override fun createCompletableWork(): Completable {
        return messageRepository.deleteMarkedMessages()
    }
}
