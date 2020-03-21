package io.plastique.statuses

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.github.technoir42.android.extensions.layoutInflater
import io.plastique.core.image.ImageLoader
import io.plastique.core.image.TransformType
import io.plastique.core.time.ElapsedTimeFormatter
import io.plastique.statuses.databinding.IncStatusesSharedDeviationImageBinding
import io.plastique.statuses.databinding.IncStatusesSharedDeviationLiteratureBinding
import io.plastique.statuses.databinding.IncStatusesSharedObjectDeletedBinding
import io.plastique.statuses.databinding.IncStatusesSharedStatusBinding
import io.plastique.util.dimensionRatio

class ShareView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    private var share: ShareUiModel = ShareUiModel.None
    private var renderer: ShareRenderer = NoneRenderer(this)

    fun setShare(share: ShareUiModel, imageLoader: ImageLoader, elapsedTimeFormatter: ElapsedTimeFormatter) {
        if (this.share.javaClass != share.javaClass) {
            removeAllViews()
            renderer = createRenderer(share, imageLoader, elapsedTimeFormatter)
        }
        if (this.share != share) {
            this.share = share
            renderer.render(share)
        }
    }

    private fun createRenderer(share: ShareUiModel, imageLoader: ImageLoader, elapsedTimeFormatter: ElapsedTimeFormatter): ShareRenderer {
        return when (share) {
            ShareUiModel.None -> NoneRenderer(this)
            is ShareUiModel.ImageDeviation -> ImageDeviationRenderer(this, imageLoader)
            is ShareUiModel.LiteratureDeviation -> LiteratureDeviationRenderer(this, imageLoader, elapsedTimeFormatter)
            is ShareUiModel.Status -> StatusRenderer(this, imageLoader, elapsedTimeFormatter)
            ShareUiModel.DeletedDeviation,
            ShareUiModel.DeletedStatus -> DeletedRenderer(this)
        }
    }
}
