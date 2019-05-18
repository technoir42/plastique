package io.plastique.core.content

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import io.plastique.core.ui.R

class EmptyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int = R.attr.emptyViewStyle,
    defStyleRes: Int = R.style.Widget_App_EmptyView
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val messageView: TextView
    private val button: Button

    var state: EmptyState? = null
        set(value) {
            field = value
            if (value != null) {
                renderState(value)
            }
        }

    init {
        orientation = VERTICAL

        val a = context.obtainStyledAttributes(attrs, R.styleable.EmptyView, defStyleAttr, defStyleRes)
        val layoutId = a.getResourceId(R.styleable.EmptyView_android_layout, 0)
        val text = a.getString(R.styleable.EmptyView_android_text)
        val buttonText = a.getString(R.styleable.EmptyView_buttonText)
        val textAppearance = a.getResourceId(R.styleable.EmptyView_android_textAppearance, 0)
        val buttonTextAppearance = a.getResourceId(R.styleable.EmptyView_buttonTextAppearance, 0)
        val showButton = a.getBoolean(R.styleable.EmptyView_showButton, true)
        a.recycle()

        View.inflate(context, layoutId, this)
        messageView = findViewById(android.R.id.message)
        button = findViewById(android.R.id.button1)

        TextViewCompat.setTextAppearance(messageView, textAppearance)
        messageView.text = text

        TextViewCompat.setTextAppearance(button, buttonTextAppearance)
        button.text = buttonText
        button.isVisible = showButton
    }

    private fun renderState(state: EmptyState) {
        when (state) {
            is EmptyState.Message -> {
                messageView.text = getMessageWithArgs(state.messageResId, state.messageArgs)
                button.isVisible = false
            }

            is EmptyState.MessageWithButton -> {
                messageView.text = getMessageWithArgs(state.messageResId, state.messageArgs)
                button.setText(state.buttonTextId)
                button.isVisible = true
            }
        }
    }

    fun setOnButtonClickListener(listener: (view: View) -> Unit) {
        button.setOnClickListener(listener)
    }

    private fun getMessageWithArgs(@StringRes messageResId: Int, args: List<Any>): CharSequence {
        val html = if (args.isNotEmpty()) {
            resources.getString(messageResId, *args.toTypedArray())
        } else {
            resources.getString(messageResId)
        }
        return HtmlCompat.fromHtml(html, 0)
    }
}