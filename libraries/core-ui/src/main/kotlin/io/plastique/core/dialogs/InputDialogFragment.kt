package io.plastique.core.dialogs

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.plastique.core.extensions.args
import io.plastique.core.extensions.findCallback
import io.plastique.core.ui.R

interface OnInputDialogResultListener {
    fun onInputDialogResult(dialog: InputDialogFragment, text: String)
}

class InputDialogFragment : BaseDialogFragment() {
    private var onInputDialogResultListener: OnInputDialogResultListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        onInputDialogResultListener = findCallback<OnInputDialogResultListener>()
    }

    override fun onDetach() {
        super.onDetach()
        onInputDialogResultListener = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val contentView = View.inflate(requireContext(), R.layout.dialog_input, null)
        val editText = contentView.findViewById<EditText>(R.id.edit)
        editText.hint = getString(args.getInt(ARG_HINT))
        editText.requestFocus()

        val maxLength = args.getInt(ARG_MAX_LENGTH)
        if (maxLength > 0) {
            editText.filters = arrayOf(InputFilter.LengthFilter(maxLength))
        }

        val listener = DialogInterface.OnClickListener { _, which ->
            if (which == DialogInterface.BUTTON_POSITIVE) {
                onInputDialogResultListener?.onInputDialogResult(this, editText.text.toString())
            }
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(args.getInt(ARG_TITLE))
            .setView(contentView)
            .setPositiveButton(args.getInt(ARG_POSITIVE_BUTTON), listener)
            .setNegativeButton(args.getInt(ARG_NEGATIVE_BUTTON), listener)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = editText.text!!.isNotBlank()
        }
        editText.doAfterTextChanged { text ->
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = !text.isNullOrBlank()
        }

        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        return dialog
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_HINT = "hint"
        private const val ARG_POSITIVE_BUTTON = "positive_button"
        private const val ARG_NEGATIVE_BUTTON = "negative_button"
        private const val ARG_MAX_LENGTH = "max_length"

        fun newArgs(
            @StringRes title: Int,
            @StringRes hint: Int,
            @StringRes positiveButton: Int = R.string.common_button_ok,
            @StringRes negativeButton: Int = R.string.common_button_cancel,
            maxLength: Int = -1
        ): Bundle = Bundle().apply {
            putInt(ARG_TITLE, title)
            putInt(ARG_HINT, hint)
            putInt(ARG_POSITIVE_BUTTON, positiveButton)
            putInt(ARG_NEGATIVE_BUTTON, negativeButton)
            putInt(ARG_MAX_LENGTH, maxLength)
        }
    }
}