package io.plastique.feed.settings

import android.content.Context
import com.github.technoir42.kotlin.extensions.replaceIf
import com.sch.neon.EffectHandler
import com.sch.neon.MainLoop
import com.sch.neon.StateReducer
import com.sch.neon.StateWithEffects
import com.sch.neon.next
import com.sch.neon.timber.TimberLogger
import io.plastique.common.ErrorMessageProvider
import io.plastique.core.mvvm.BaseViewModel
import io.plastique.feed.R
import io.plastique.feed.settings.FeedSettingsEffect.LoadFeedSettingsEffect
import io.plastique.feed.settings.FeedSettingsEvent.FeedSettingsLoadedEvent
import io.plastique.feed.settings.FeedSettingsEvent.LoadErrorEvent
import io.plastique.feed.settings.FeedSettingsEvent.RetryClickEvent
import io.plastique.feed.settings.FeedSettingsEvent.SetEnabledEvent
import io.reactivex.Observable
import io.reactivex.rxkotlin.ofType
import timber.log.Timber
import javax.inject.Inject

class FeedSettingsViewModel @Inject constructor(
    stateReducer: FeedSettingsStateReducer,
    effectHandler: FeedSettingsEffectHandler
) : BaseViewModel() {

    private val loop = MainLoop(
        reducer = stateReducer,
        effectHandler = effectHandler,
        listener = TimberLogger(LOG_TAG))

    val state: Observable<FeedSettingsViewState> by lazy(LazyThreadSafetyMode.NONE) {
        loop.loop(FeedSettingsViewState.Loading, LoadFeedSettingsEffect).disposeOnDestroy()
    }

    fun dispatch(event: FeedSettingsEvent) {
        loop.dispatch(event)
    }

    companion object {
        private const val LOG_TAG = "FeedSettingsViewModel"
    }
}

class FeedSettingsEffectHandler @Inject constructor(
    private val context: Context,
    private val feedSettingsManager: FeedSettingsManager
) : EffectHandler<FeedSettingsEffect, FeedSettingsEvent> {

    override fun handle(effects: Observable<FeedSettingsEffect>): Observable<FeedSettingsEvent> {
        return effects.ofType<LoadFeedSettingsEffect>()
            .switchMapSingle {
                feedSettingsManager.getSettings()
                    .map<FeedSettingsEvent> { feedSettings -> FeedSettingsLoadedEvent(feedSettings, createOptions(feedSettings)) }
                    .doOnError(Timber::e)
                    .onErrorReturn { error -> LoadErrorEvent(error) }
            }
    }

    private fun createOptions(feedSettings: FeedSettings): List<OptionItem> {
        val keysAndTitles = context.resources.getStringArray(R.array.feed_settings_options)
        val result = ArrayList<OptionItem>(keysAndTitles.size)
        keysAndTitles.forEach {
            val (key, title) = it.split('|')
            if (feedSettings.include.containsKey(key)) {
                result += OptionItem(key, title, feedSettings.include.getValue(key))
            }
        }
        return result
    }
}

class FeedSettingsStateReducer @Inject constructor(
    private val errorMessageProvider: ErrorMessageProvider
) : StateReducer<FeedSettingsEvent, FeedSettingsViewState, FeedSettingsEffect> {

    override fun reduce(state: FeedSettingsViewState, event: FeedSettingsEvent): StateWithEffects<FeedSettingsViewState, FeedSettingsEffect> = when (event) {
        is FeedSettingsLoadedEvent -> {
            next(FeedSettingsViewState.Content(settings = event.settings, items = event.items, changedSettings = FeedSettings(emptyMap())))
        }

        is LoadErrorEvent -> {
            next(FeedSettingsViewState.Empty(emptyState = errorMessageProvider.getErrorState(event.error)))
        }

        RetryClickEvent -> {
            next(FeedSettingsViewState.Loading, LoadFeedSettingsEffect)
        }

        is SetEnabledEvent -> {
            if (state is FeedSettingsViewState.Content) {
                val items = state.items.replaceIf({ it.key == event.optionKey }) { it.copy(isChecked = event.isEnabled) }
                val include = items.asSequence()
                    .filter { it.isChecked != state.settings.include[it.key] }
                    .associateBy({ it.key }, { it.isChecked })
                next(state.copy(items = items, changedSettings = FeedSettings(include)))
            } else {
                next(state)
            }
        }
    }
}
