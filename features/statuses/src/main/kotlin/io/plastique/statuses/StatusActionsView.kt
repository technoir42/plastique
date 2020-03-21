package io.plastique.statuses

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.github.technoir42.android.extensions.layoutInflater
import io.plastique.comments.OnCommentsClickListener
import io.plastique.statuses.databinding.ViewStatusActionsBinding

class StatusActionsView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val binding: ViewStatusActionsBinding
    private lateinit var state: StatusActionsState

    var onCommentsClick: OnCommentsClickListener = {}
    var onShareClick: OnShareClickListener = {}

    init {
        orientation = HORIZONTAL
        binding = ViewStatusActionsBinding.inflate(layoutInflater, this)

        binding.buttonComments.setOnClickListener { onCommentsClick(state.commentThreadId) }
        binding.buttonShare.setOnClickListener { onShareClick(state.shareObjectId!!) }
    }

    fun render(state: StatusActionsState) {
        this.state = state
        binding.buttonComments.text = state.commentCount.toString()
        binding.buttonShare.isVisible = state.shareObjectId != null
    }
}
