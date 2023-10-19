package io.agora.scene.show.beauty

import android.app.Application
import android.view.View
import io.agora.beautyapi.bytedance.Config
import io.agora.beautyapi.bytedance.EventCallback
import io.agora.beautyapi.bytedance.createByteDanceBeautyAPI
import io.agora.rtc2.RtcEngine

class BeautyProcessor(private val context: Application) : IBeautyProcessor() {

    private val beautyApi by lazy {
        createByteDanceBeautyAPI()
    }

    init {
        BytedanceBeautySDK.initBeautySDK(context)
    }

    override fun initialize(rtcEngine: RtcEngine) {
        beautyApi.initialize(Config(
            context,
            rtcEngine,
            BytedanceBeautySDK.renderManager,
            EventCallback(
                onEffectInitialized = {
                    BytedanceBeautySDK.initEffect(context)
                },
                onEffectDestroyed = {
                    BytedanceBeautySDK.unInitEffect()
                }
            )
        ))
    }

    override fun release() {
        super.release()
        beautyApi.release()
    }

    override fun setBeautyEnable(enable: Boolean) {
        super.setBeautyEnable(enable)
        beautyApi.enable(enable)
    }

    override fun setupLocalVideo(videoView: View, renderMode: Int) {
        beautyApi.setupLocalVideo(videoView, renderMode)
    }

    override fun setFaceBeautifyAfterCached(itemId: Int, intensity: Float) {
        beautyApi.runOnProcessThread {
            BytedanceBeautySDK.setBeauty(
                smooth = BeautyCache.getItemValue(ITEM_ID_BEAUTY_SMOOTH),
                whiten = BeautyCache.getItemValue(ITEM_ID_BEAUTY_WHITEN),
                thinFace = BeautyCache.getItemValue(ITEM_ID_BEAUTY_OVERALL),
                shrinkCheekbone = BeautyCache.getItemValue(ITEM_ID_BEAUTY_CHEEKBONE),
                shrinkJawbone = BeautyCache.getItemValue(ITEM_ID_BEAUTY_JAWBONE),
                enlargeEye = BeautyCache.getItemValue(ITEM_ID_BEAUTY_EYE),
                whiteTeeth = BeautyCache.getItemValue(ITEM_ID_BEAUTY_TEETH),
                hairlineHeight = BeautyCache.getItemValue(ITEM_ID_BEAUTY_FOREHEAD),
                narrowNose = BeautyCache.getItemValue(ITEM_ID_BEAUTY_NOSE),
                mouthSize = BeautyCache.getItemValue(ITEM_ID_BEAUTY_MOUTH),
                chinLength = BeautyCache.getItemValue(ITEM_ID_BEAUTY_CHIN),
                brightEye = BeautyCache.getItemValue(ITEM_ID_BEAUTY_BRIGHT_EYE),
                darkCircles = BeautyCache.getItemValue(ITEM_ID_BEAUTY_REMOVE_DARK_CIRCLES),
                nasolabialFolds = BeautyCache.getItemValue(ITEM_ID_BEAUTY_REMOVE_NASOLABIAL_FOLDS),
                redden = BeautyCache.getItemValue(ITEM_ID_BEAUTY_REDDEN),
            )
        }
    }

    override fun setFilterAfterCached(itemId: Int, intensity: Float) {
        when (itemId) {
            ITEM_ID_FILTER_NONE -> {
            }
            ITEM_ID_FILTER_CREAM -> {
            }
            ITEM_ID_FILTER_MAKALONG -> {
            }
        }
    }

    override fun setEffectAfterCached(itemId: Int, intensity: Float) {
        beautyApi.runOnProcessThread {
            when (itemId) {
                ITEM_ID_EFFECT_YUANQI -> {
                    BytedanceBeautySDK.setMakeUp("yuanqi", intensity)
                }
                ITEM_ID_EFFECT_CWEI -> {
                    BytedanceBeautySDK.setMakeUp("cwei", intensity)
                }
                ITEM_ID_EFFECT_NONE -> {
                    BytedanceBeautySDK.setMakeUp("", 0f)
                }
            }
        }
    }

    override fun setStickerAfterCached(itemId: Int) {
        beautyApi.runOnProcessThread {
            if (itemId == ITEM_ID_STICKER_NONE) {
                BytedanceBeautySDK.renderManager.setSticker(null)
            } else if (itemId == ITEM_ID_STICKER_HUAHUA) {
                BytedanceBeautySDK.renderManager.setSticker("${BytedanceBeautySDK.stickerPath}/huahua")
            }
        }
    }
}