package io.agora.scene.show.beauty

import android.app.Application
import android.content.Context
import android.util.Log
import com.effectsar.labcv.effectsdk.RenderManager
import io.agora.scene.show.utils.FileUtils
import java.util.concurrent.Executors
import java.util.concurrent.Future

object BytedanceBeautySDK {
    private val TAG = "ByteDanceBeautySDK"

    private val LICENSE_NAME = "Agora_test_20230815_20231115_io.agora.test.entfull_4.5.0_599.licbag"
    private val workerThread = Executors.newSingleThreadExecutor()
    private var initFuture: Future<*>? = null
    private var context: Application? = null
    private var storagePath = ""
    private var assetsPath = ""

    val renderManager = RenderManager()
    var licensePath = ""
    var modelsPath = ""
    var beautyNodePath = ""
    var beauty4ItemsNodePath = ""
    var reSharpNodePath = ""
    var stickerPath = ""


    fun initBeautySDK(context: Context){
        this.context = context.applicationContext as? Application
        storagePath = context.getExternalFilesDir("")?.absolutePath ?: return
        assetsPath = "beauty_bytedance"

        initFuture = workerThread.submit {
            // copy license
            licensePath = "$storagePath/beauty_bytedance/LicenseBag.bundle/$LICENSE_NAME"
            FileUtils.copyAssets(context, "$assetsPath/LicenseBag.bundle/$LICENSE_NAME", licensePath)

            // copy models
            modelsPath = "$storagePath/beauty_bytedance/ModelResource.bundle"
            FileUtils.copyAssets(context, "$assetsPath/ModelResource.bundle", modelsPath)

            // copy beauty node
            beautyNodePath = "$storagePath/beauty_bytedance/ComposeMakeup.bundle/ComposeMakeup/beauty_Android_lite"
            FileUtils.copyAssets(context, "$assetsPath/ComposeMakeup.bundle/ComposeMakeup/beauty_Android_lite", beautyNodePath)

            // copy beauty 4items node
            beauty4ItemsNodePath = "$storagePath/beauty_bytedance/ComposeMakeup.bundle/ComposeMakeup/beauty_4Items"
            FileUtils.copyAssets(context, "$assetsPath/ComposeMakeup.bundle/ComposeMakeup/beauty_4Items", beauty4ItemsNodePath)

            // copy resharp node
            reSharpNodePath = "$storagePath/beauty_bytedance/ComposeMakeup.bundle/ComposeMakeup/reshape_lite"
            FileUtils.copyAssets(context, "$assetsPath/ComposeMakeup.bundle/ComposeMakeup/reshape_lite", reSharpNodePath)


            // copy stickers
            stickerPath = "$storagePath/beauty_bytedance/StickerResource.bundle/stickers"
            FileUtils.copyAssets(context, "$assetsPath/StickerResource.bundle/stickers", stickerPath)
        }
    }

    fun hasLicense() = licensePath.isNotEmpty()

    // GL Thread
    fun initEffect(context: Context){
        initFuture?.let {
            it.get()
            initFuture = null
        }
        val ret = renderManager.init(
            context,
            modelsPath, licensePath, false, false, 0
        )
        if(!checkResult("RenderManager init ", ret)){
            return
        }
        renderManager.useBuiltinSensor(true)
        renderManager.set3Buffer(false)
        renderManager.appendComposerNodes(arrayOf(beautyNodePath, beauty4ItemsNodePath, reSharpNodePath))
        renderManager.loadResourceWithTimeout(-1)
    }

    // GL Thread
    fun unInitEffect(){
        renderManager.release()
    }

    private fun checkResult(msg: String, ret: Int): Boolean {
        if (ret != 0 && ret != -11 && ret != 1) {
            val log = "$msg error: $ret"
            Log.e(TAG, log)
            return false
        }
        return true
    }

    private var currMakeupNodePath = ""
    fun setMakeUp(style: String, identity: Float){
        if(!currMakeupNodePath.split("/").lastOrNull().equals(style)){
            if(currMakeupNodePath.isNotEmpty()){
                renderManager.removeComposerNodes(arrayOf(currMakeupNodePath))
            }
            if(style.isEmpty()){
                currMakeupNodePath = ""
                return
            }
            currMakeupNodePath = "$storagePath/beauty_bytedance/ComposeMakeup.bundle/ComposeMakeup/style_makeup/$style"
            FileUtils.copyAssets(context!!, "$assetsPath/ComposeMakeup.bundle/ComposeMakeup/style_makeup/$style", currMakeupNodePath)
            renderManager.appendComposerNodes(arrayOf(currMakeupNodePath))
            renderManager.loadResourceWithTimeout(-1)
        }
        renderManager.updateComposerNodes(
            currMakeupNodePath,
            "Filter_ALL",
            identity
        )
        renderManager.updateComposerNodes(
            currMakeupNodePath,
            "Makeup_ALL",
            identity
        )
    }

    fun setBeauty(
        smooth: Float? = null,
        whiten: Float? = null,
        thinFace: Float? = null,
        enlargeEye: Float? = null,
        redden: Float? = null,
        shrinkCheekbone: Float? = null,
        shrinkJawbone: Float? = null,
        whiteTeeth: Float? = null,
        hairlineHeight: Float? = null,
        narrowNose: Float? = null,
        mouthSize: Float? = null,
        chinLength: Float? = null,
        brightEye: Float? = null,
        darkCircles: Float? = null,
        nasolabialFolds: Float? = null,
    ){
        // 磨皮
        smooth?.let { renderManager.updateComposerNodes(beautyNodePath, "smooth", it) }

        // 美白
        whiten?.let { renderManager.updateComposerNodes(beautyNodePath, "whiten", it) }

        // 红润
        redden?.let { renderManager.updateComposerNodes(beautyNodePath, "sharp", it) }


        // 瘦脸
        thinFace?.let { renderManager.updateComposerNodes(reSharpNodePath, "Internal_Deform_Overall", it) }

        // 大眼
        enlargeEye?.let { renderManager.updateComposerNodes(reSharpNodePath, "Internal_Deform_Eye", it) }


        // 瘦颧骨
        shrinkCheekbone?.let { renderManager.updateComposerNodes(reSharpNodePath, "Internal_Deform_Zoom_Cheekbone", it) }

        // 下颌骨
        shrinkJawbone?.let { renderManager.updateComposerNodes(reSharpNodePath, "Internal_Deform_Zoom_Jawbone", it) }

        // 美牙
        whiteTeeth?.let { renderManager.updateComposerNodes(reSharpNodePath, "BEF_BEAUTY_WHITEN_TEETH", it) }

        // 额头
        hairlineHeight?.let { renderManager.updateComposerNodes(reSharpNodePath, "Internal_Deform_Forehead", it) }

        // 瘦鼻
        narrowNose?.let { renderManager.updateComposerNodes(reSharpNodePath, "Internal_Deform_Nose", it) }

        // 嘴形
        mouthSize?.let { renderManager.updateComposerNodes(reSharpNodePath, "Internal_Deform_ZoomMouth", it) }

        // 下巴
        chinLength?.let { renderManager.updateComposerNodes(reSharpNodePath, "Internal_Deform_Chin", it) }

        // 亮眼
        brightEye?.let { renderManager.updateComposerNodes(beauty4ItemsNodePath, "BEF_BEAUTY_BRIGHTEN_EYE", it) }

        // 祛黑眼圈
        darkCircles?.let { renderManager.updateComposerNodes(beauty4ItemsNodePath, "BEF_BEAUTY_REMOVE_POUCH", it) }

        // 祛法令纹
        nasolabialFolds?.let { renderManager.updateComposerNodes(beauty4ItemsNodePath, "BEF_BEAUTY_SMILES_FOLDS", it) }

    }
}