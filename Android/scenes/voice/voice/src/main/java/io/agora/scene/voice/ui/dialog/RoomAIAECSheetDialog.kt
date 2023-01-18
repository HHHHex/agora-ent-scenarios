package io.agora.scene.voice.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.agora.scene.voice.databinding.VoiceDialogChatroomAiaecBinding
import io.agora.scene.voice.databinding.VoiceDialogChatroomAinsBinding
import io.agora.voice.common.ui.dialog.BaseSheetDialog

class RoomAIAECSheetDialog: BaseSheetDialog<VoiceDialogChatroomAiaecBinding>() {

    public var onClickCheckBox: ((isOn: Boolean) -> Unit)? = null

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): VoiceDialogChatroomAiaecBinding? {
        return VoiceDialogChatroomAiaecBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.accbAEC?.setOnClickListener {
            onClickCheckBox?.invoke(true)
        }
    }
}