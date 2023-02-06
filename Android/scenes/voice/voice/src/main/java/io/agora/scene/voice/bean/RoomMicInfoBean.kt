package io.agora.scene.voice.bean

import io.agora.voice.buddy.config.ConfigConstants
import io.agora.secnceui.annotation.*

data class MicInfoBean constructor(
    var index: Int = 0,
    var userInfo: RoomUserInfoBean? = null,
    var ownerTag: Boolean = false,
    @MicStatus var micStatus: Int = MicStatus.Idle,
    var audioVolumeType: Int = ConfigConstants.VolumeType.Volume_None,
) : io.agora.scene.voice.bean.BaseRoomBean

data class BotMicInfoBean constructor(
    var blueBot: MicInfoBean,
    var redBot: MicInfoBean
) : io.agora.scene.voice.bean.BaseRoomBean

data class MicManagerBean constructor(
    val name: String,
    var enable: Boolean = true,
    @MicClickAction var micClickAction: Int = MicClickAction.Invite
) : io.agora.scene.voice.bean.BaseRoomBean