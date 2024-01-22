package io.agora.scene.cantata.ui.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import io.agora.scene.base.component.BaseBottomSheetDialogFragment
import io.agora.scene.base.component.BaseRecyclerViewAdapter
import io.agora.scene.base.component.BaseRecyclerViewAdapter.BaseViewHolder
import io.agora.scene.base.component.BaseViewBindingFragment
import io.agora.scene.base.component.OnItemClickListener
import io.agora.scene.cantata.R
import io.agora.scene.cantata.databinding.CantataDialogMusicSettingBinding
import io.agora.scene.cantata.databinding.CantataItemEffectvoiceBinding
import io.agora.scene.cantata.ui.fragment.EarBackFragment
import io.agora.scene.widget.DividerDecoration

/**
 * 控制台
 */
class MusicSettingDialog constructor(private val mSetting: MusicSettingBean, private val isPause: Boolean) :
    BaseBottomSheetDialogFragment<CantataDialogMusicSettingBinding>() {

    companion object {
        const val TAG = "MusicSettingDialog"
    }

    private var mEffectAdapter:
            BaseRecyclerViewAdapter<CantataItemEffectvoiceBinding, EffectVoiceBean, EffectVoiceHolder>? = null

    private fun getCurrentPitch(value: Int): Int {
        return when (value) {
            12 -> 11
            10 -> 10
            8 -> 9
            6 -> 8
            4 -> 7
            2 -> 6
            -2 -> 4
            -4 -> 3
            -6 -> 2
            -8 -> 1
            -10 -> 0
            -12 -> -1
            0 -> 5
            else -> 5
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.let { window ->
            ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v: View?, insets: WindowInsetsCompat ->
                val inset = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                mBinding.root.apply {
                    setPadding(inset.left, 0, inset.right, inset.bottom)
                }
                WindowInsetsCompat.CONSUMED
            }
        }


        // 升降调
        tuningTone(null)
        mBinding.sbVol1.progress = mSetting.getVolMic()
        mBinding.sbVol2.progress = mSetting.getVolMusic()
        mBinding.changeToneView.progress = getCurrentPitch(mSetting.getToneValue())
        mBinding.btnToneDownDialogSetting.setOnClickListener { v: View? -> tuningTone(false) }
        mBinding.btnToneUpDialogSetting.setOnClickListener { v: View? -> tuningTone(true) }
        if (isPause) {
            mBinding.sbRemoteVol.isEnabled = false
            mBinding.btnRemoteVolumeUpDialogSetting.isEnabled = false
            mBinding.btnRemoteVolumeDownDialogSetting.isEnabled = false
            mBinding.sbRemoteVol.progress = 100
        } else {
            mBinding.sbRemoteVol.isEnabled = true
            mBinding.btnRemoteVolumeUpDialogSetting.isEnabled = true
            mBinding.btnRemoteVolumeDownDialogSetting.isEnabled = true
            mBinding.sbRemoteVol.progress = mSetting.remoteVolume
        }
        mBinding.root.context.apply {
            if (mSetting.isEar()) {
                mBinding.switchEar.text = getString(R.string.cantata_switch_open)
            } else {
                mBinding.switchEar.text = getString(R.string.cantata_switch_close)
            }
        }

        mBinding.switchEar.setOnClickListener { v: View -> showEarBackPage(v) }

        mBinding.btVol1Down.setOnClickListener { v -> tuningMicVolume(false) }
        mBinding.btVol1Up.setOnClickListener { v -> tuningMicVolume(true) }

        mBinding.sbVol1.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                if (seekBar.isPressed) mSetting.setVolMic(i)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        mBinding.btVol2Down.setOnClickListener { v -> tuningMusicVolume(false) }
        mBinding.btVol2Up.setOnClickListener { v -> tuningMusicVolume(true) }

        mBinding.sbVol2.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                if (seekBar.isPressed) mSetting.setVolMusic(i)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        mBinding.btnRemoteVolumeDownDialogSetting.setOnClickListener { v: View? ->
            val volume = mSetting.remoteVolume
            var newVolume = volume - 1
            if (newVolume < 0) newVolume = 0
            mBinding.sbRemoteVol.progress = newVolume
            mSetting.remoteVolume = newVolume
            mBinding.cbSwitch.isChecked = newVolume == 0
        }
        mBinding.btnRemoteVolumeUpDialogSetting.setOnClickListener { v: View? ->
            val volume = mSetting.remoteVolume
            var newVolume = volume + 1
            if (newVolume > 100) newVolume = 100
            mBinding.sbRemoteVol.progress = newVolume
            mSetting.remoteVolume = newVolume
            mBinding.cbSwitch.isChecked = newVolume == 0
        }

        mBinding.sbRemoteVol.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                if (seekBar.isPressed) {
                    mSetting.remoteVolume = i
                    mBinding.sbRemoteVol.progress = i
                    mBinding.cbSwitch.isChecked = i == 0
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        val stringArray = resources.getStringArray(R.array.cantata_audioPreset)
        val list: MutableList<EffectVoiceBean> = ArrayList()
        for (i in stringArray.indices) {
            val drawable: Int = if (i % 4 == 0) {
                R.mipmap.bg_sound_mode_4
            } else if (i % 3 == 0) {
                R.mipmap.bg_sound_mode_3
            } else if (i % 2 == 0) {
                R.mipmap.bg_sound_mode_2
            } else {
                R.mipmap.bg_sound_mode_1
            }
            val audioEffect: Int = mSetting.getEffectIndex(i)
            list.add(EffectVoiceBean(i, audioEffect, drawable, stringArray[i]))
        }
        for (item in list) {
            item.setSelect(mSetting.audioEffect == item.audioEffect)
        }
        mEffectAdapter = BaseRecyclerViewAdapter(
            list, object : OnItemClickListener<EffectVoiceBean> {

                override fun onItemClick(data: EffectVoiceBean, view: View?, position: Int, viewType: Long) {
                    super.onItemClick(data, view, position, viewType)
                    mEffectAdapter?.let {
                        for (i in it.dataList.indices) {
                            it.dataList[i].setSelect(i == position)
                            it.notifyItemChanged(i)
                        }
                    }

                    mSetting.setAudioEffectPreset(data.audioEffect)
                }
            },
            EffectVoiceHolder::class.java
        )

        mBinding.rvVoiceEffectList.adapter = mEffectAdapter
        mBinding.rvVoiceEffectList.addItemDecoration(DividerDecoration(10, 20, 0))

        mBinding.cbSwitch.isChecked = mSetting.remoteVolume == 0
        mBinding.cbSwitch.setOnCheckedChangeListener { buttonView, ischecked ->
            if (!buttonView.isPressed) return@setOnCheckedChangeListener
            if (ischecked) {
                mBinding.sbRemoteVol.progress = 0
                mSetting.remoteVolume = 0
            } else {
                mBinding.sbRemoteVol.progress = 30
                mSetting.remoteVolume = 30
            }
        }
    }

    private fun showEarBackPage(v: View) {
        mBinding.root.removeAllViews()
        val earBackFragment: BaseViewBindingFragment<*> = EarBackFragment(mSetting)
        val ft = childFragmentManager.beginTransaction()
        ft.add(mBinding.root.id, earBackFragment, EarBackFragment.TAG)
        ft.commit()
    }

    /**
     * IMediaPlayer.java
     * / **
     * Sets the pitch of the current media file.
     * pitch Sets the pitch of the local music file by chromatic scale. The default value is 0,
     * which means keeping the original pitch. The value ranges from -12 to 12, and the pitch value
     * between consecutive values is a chromatic value. The greater the absolute value of this
     * parameter, the higher or lower the pitch of the local music file.
     * *
     * - 0: Success.
     * - < 0: Failure.
     * int setAudioMixingPitch(int pitch);
     *
     * @param toneUp true -> +1 | false -> -1 | null -> update value
     */
    private fun tuningTone(toneUp: Boolean?) {
        var newToneValue = mSetting.getToneValue()
        toneUp?.let { tone ->
            if (tone) {
                newToneValue += 2
            } else {
                newToneValue -= 2
            }
            if (newToneValue > 12) newToneValue = 12
            if (newToneValue < -12) newToneValue = -12
            if (newToneValue != mSetting.getToneValue()) mSetting.setToneValue(newToneValue)
        }
        mBinding.changeToneView.progress = newToneValue
    }

    private fun tuningMicVolume(volumeUp: Boolean) {
        var newVocalVolume = mSetting.getVolMic()
        if (volumeUp) {
            newVocalVolume += 1
        } else {
            newVocalVolume -= 1
        }
        if (newVocalVolume > 100) newVocalVolume = 100
        if (newVocalVolume < 0) newVocalVolume = 0
        if (newVocalVolume != mSetting.getVolMic()) {
            mSetting.setVolMic(newVocalVolume)
        }
        mBinding.sbVol1.progress = newVocalVolume
    }

    private fun tuningMusicVolume(volumeUp: Boolean) {
        var newMusicVolume = mSetting.getVolMusic()
        if (volumeUp) {
            newMusicVolume += 1
        } else {
            newMusicVolume -= 1
        }
        if (newMusicVolume > 100) newMusicVolume = 100
        if (newMusicVolume < 0) newMusicVolume = 0
        if (newMusicVolume != mSetting.getVolMusic()) {
            mSetting.setVolMusic(newMusicVolume)
        }
        mBinding.sbVol2.progress = newMusicVolume
    }


    fun onStopPlayer() {
        mBinding.sbRemoteVol.progress = 100
    }

    fun onResumePlayer() {
        mBinding.sbRemoteVol.progress = mSetting.remoteVolume
    }
}

class EffectVoiceHolder constructor(mBinding: CantataItemEffectvoiceBinding) :
    BaseViewHolder<CantataItemEffectvoiceBinding, EffectVoiceBean>(mBinding) {
    override fun binding(data: EffectVoiceBean?, selectedIndex: Int) {
        data ?: return
        mBinding.ivBg.setImageResource(data.resId)
        mBinding.tvTitle.text = data.title
        mBinding.select.isVisible = data.isSelect
    }
}