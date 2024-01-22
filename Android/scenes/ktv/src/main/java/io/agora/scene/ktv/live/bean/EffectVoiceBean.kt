package io.agora.scene.ktv.live.bean

import androidx.annotation.DrawableRes

data class EffectVoiceBean constructor(
    var id: Int,
    var audioEffect: Int,
    @field:DrawableRes var resId: Int,
    var title: String,
    var isSelect: Boolean = false)
{
}
