package io.agora.scene.ktv.widget.lrcView;

import android.animation.Animator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.vectordrawable.graphics.drawable.Animatable2Compat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.util.List;

import io.agora.karaoke_view.v11.KaraokeEvent;
import io.agora.karaoke_view.v11.KaraokeView;
import io.agora.karaoke_view.v11.LyricsView;
import io.agora.karaoke_view.v11.ScoringView;
import io.agora.karaoke_view.v11.model.LyricsLineModel;
import io.agora.karaoke_view.v11.model.LyricsModel;
import io.agora.scene.base.GlideApp;
import io.agora.scene.base.utils.DownloadUtils;
import io.agora.scene.base.utils.ZipUtils;
import io.agora.scene.ktv.R;
import io.agora.scene.ktv.databinding.KtvLayoutLrcControlViewBinding;
import io.agora.scene.ktv.databinding.KtvLayoutLrcPrepareBinding;
import io.agora.scene.ktv.ktvapi.ILrcView;
import io.agora.scene.ktv.service.RoomSelSongModel;
import io.agora.scene.widget.basic.OutlineSpan;
import io.agora.scene.widget.toast.CustomToast;
import io.agora.scene.widget.utils.UiUtils;

/**
 * 歌词控制View
 */
public class LrcControlView extends FrameLayout implements View.OnClickListener, ILrcView {

    protected KtvLayoutLrcControlViewBinding mBinding;
    protected KtvLayoutLrcPrepareBinding mPrepareBinding;

    protected KaraokeView mKaraokeView;

    protected int mCumulativeScoreInPercentage;

    public int getCumulativeScoreInPercentage() {
        return mCumulativeScoreInPercentage;
    }

    protected ComboControl mComboControl;

    public LyricsView getLyricsView() {
        if (mBinding != null && mBinding.ilActive != null) {
            return mBinding.ilActive.lyricsView;
        } else {
            return null;
        }
    }

    public ScoringView getScoringView() {
        return mBinding.ilActive.scoringView;
    }

    public KaraokeView getKaraokeView() {
        return mKaraokeView;
    }

    @Override
    public void onHighPartTime(long highStartTime, long highEndTime) {

    }

    public enum Role {
        Singer, Listener, CoSinger
    }

    public Role mRole = Role.Listener;

    public enum AudioTrack {
        Origin, Acc, DaoChang
    }

    private AudioTrack mAudioTrack = AudioTrack.Acc;
    private OnKaraokeEventListener mOnKaraokeActionListener;

    public LrcControlView(@NonNull Context context) {
        this(context, null);
    }

    public LrcControlView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LrcControlView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mBinding = KtvLayoutLrcControlViewBinding.inflate(LayoutInflater.from(context), this, true);

        mPrepareBinding = KtvLayoutLrcPrepareBinding.bind(mBinding.getRoot());

        mBinding.ilIDLE.getRoot().setVisibility(View.VISIBLE);
        mBinding.ilActive.getRoot().setVisibility(View.GONE);

        mKaraokeView = new KaraokeView(mBinding.ilActive.lyricsView, mBinding.ilActive.scoringView);

        mBinding.ilActive.btnVocalHighlight.setVisibility(View.GONE);

        initListener();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mBinding = null;
        mPrepareBinding = null;
    }

    private int chorusScore = 0;

    private void initListener() {
        mBinding.ilActive.switchOriginal.setOnClickListener(this);
        mBinding.ilActive.ivMusicStart.setOnClickListener(this);
        mBinding.ilActive.ivChangeSong.setOnClickListener(this);
        mBinding.ilActive.ivSkipPostludeSkip.setOnClickListener(this);
        mBinding.ilActive.ivSkipPreludeSkip.setOnClickListener(this);
        mBinding.ilActive.ivSkipPostludeCancel.setOnClickListener(this);
        mBinding.ilActive.ivSkipPreludeCancel.setOnClickListener(this);
        mBinding.ilActive.ivJoinChorusBtn.setOnClickListener(this);
        mBinding.ilActive.ivLeaveChorus.setOnClickListener(this);
        mBinding.ilActive.downloadLrcFailedBtn.setOnClickListener(this);

        mBinding.ilActive.btnVocalHighlight.setOnClickListener(this);
        mBinding.ilActive.btnVocalHighlight.bringToFront();

        mKaraokeView.setKaraokeEvent(new KaraokeEvent() {
            @Override
            public void onDragTo(KaraokeView view, long position) {
                if (mOnKaraokeActionListener != null) {
                    mOnKaraokeActionListener.onDragTo(position);
                }
            }

            @Override
            public void onRefPitchUpdate(float refPitch, int numberOfRefPitches) {
                if (mOnKaraokeActionListener != null) {
                    mOnKaraokeActionListener.onRefPitchUpdate(refPitch, numberOfRefPitches);
                }
            }

            @Override
            public void onLineFinished(KaraokeView view, LyricsLineModel line, int score, int cumulativeScore, int index, int total) {
                if (mRole == Role.Singer && mOnKaraokeActionListener != null) {
                    mOnKaraokeActionListener.onLineFinished(line, score, cumulativeScore, index, total);
                } else if (mRole == Role.CoSinger) {
                    chorusScore += score;
                    updateScore(score, chorusScore, /** Workaround(Hai_Guo)*/total * 100);
                }
            }
        });
    }

    public void setOnLrcClickListener(OnKaraokeEventListener karaokeActionListener) {
        this.mOnKaraokeActionListener = karaokeActionListener;
    }

    public void onSelfJoinedChorus() {
        mBinding.tvCumulativeScore.setText(String.format(getResources().getString(R.string.ktv_score_formatter), "" + chorusScore));
        this.mRole = Role.CoSinger;
        mBinding.ilActive.ivMusicStart.setVisibility(View.INVISIBLE);
        mBinding.ilActive.switchOriginal.setVisibility(View.VISIBLE);
        mBinding.ilActive.switchOriginal.setChecked(false); // reset ui icon for mAudioTrackMode
        mBinding.ilActive.switchOriginal.setIconResource(R.mipmap.ic_play_original_off);
        mBinding.ilActive.switchOriginal.setText(R.string.ktv_room_original);
        mBinding.ilActive.ivJoinChorusBtn.setVisibility(View.INVISIBLE);
        mBinding.ilActive.ivLeaveChorus.setVisibility(View.VISIBLE);
        mBinding.ilActive.ivJoinChorusLoading.setVisibility(INVISIBLE);
        mPrepareBinding.statusPrepareViewLrc.setVisibility(GONE);

        if (isMineOwner) {
            mBinding.ilActive.ivChangeSong.setVisibility(VISIBLE);
        }
    }

    public void onSelfJoinedChorusFailed() {
        mBinding.ilActive.ivJoinChorusBtn.setVisibility(VISIBLE);
        mBinding.ilActive.ivJoinChorusLoading.setVisibility(INVISIBLE);
        mPrepareBinding.statusPrepareViewLrc.setVisibility(GONE);
    }

    public void onSelfLeavedChorus() {
        mBinding.tvCumulativeScore.setText(String.format(getResources().getString(R.string.ktv_score_formatter), "" + 0));
        this.mRole = Role.Listener;
        mBinding.ilActive.ivMusicStart.setVisibility(View.GONE);
        mBinding.ilActive.switchOriginal.setVisibility(View.INVISIBLE);
        mBinding.ilActive.ivJoinChorusBtn.setVisibility(View.VISIBLE);
        mBinding.ilActive.ivLeaveChorus.setVisibility(View.INVISIBLE);
    }

    private boolean isSeatFull = false;

    public void onSeatFull(boolean isFull) {
        this.isSeatFull = isFull;
        if (!isOnSeat && this.mRole == Role.Listener) {
            mBinding.ilActive.ivJoinChorusBtn.setVisibility(isFull ? View.INVISIBLE : View.VISIBLE);
        }
    }

    private boolean isOnSeat = false;

    public void onSeat(boolean isOnSeat) {
        this.isOnSeat = isOnSeat;
    }

    private boolean isMineOwner = false;

    public void onPrepareStatus(boolean isMineOwner) {
        chorusScore = 0;
        this.isMineOwner = isMineOwner;
        mBinding.ilIDLE.getRoot().setVisibility(View.GONE);
        mBinding.clActive.setVisibility(View.VISIBLE);
        mPrepareBinding.tvContent.setText(String.format(getResources().getString(R.string.ktv_loading_music), "0%"));
        mPrepareBinding.pbLoadingMusic.setProgress(0);
        mPrepareBinding.statusPrepareViewLrc.setVisibility(View.VISIBLE);
        mBinding.ilActive.getRoot().setVisibility(View.GONE);

        changeViewByRole();
    }

    public void onMusicLoadProgress(int percent) {
        mPrepareBinding.tvContent.setText(String.format(getResources().getString(R.string.ktv_loading_music), percent + "%"));
        mPrepareBinding.pbLoadingMusic.setProgress(percent);
    }

    private RoomSelSongModel songPlaying;

    public void onPlayStatus(RoomSelSongModel songPlaying) {
        this.songPlaying = songPlaying;

        mBinding.ilIDLE.getRoot().setVisibility(View.GONE);
        mBinding.clActive.setVisibility(View.VISIBLE);
        setLrcViewBackground(R.color.ktv_music_view_bg);
        mPrepareBinding.statusPrepareViewLrc.setVisibility(View.GONE);
        mBinding.ilActive.getRoot().setVisibility(View.VISIBLE);

        mBinding.ilActive.ivMusicStart.setIconResource(R.mipmap.ktv_ic_pause);
        mBinding.ilActive.ivMusicStart.setText(R.string.ktv_room_player_pause);
        mAudioTrack = AudioTrack.Acc;
    }

    private void changeViewByRole() {
        mBinding.ilActive.downloadLrcFailedView.setVisibility(View.INVISIBLE);
        mBinding.ilActive.downloadLrcFailedBtn.setVisibility(View.INVISIBLE);
        if (this.mRole == Role.Singer) {
            mBinding.ilActive.lyricsView.enableDragging(false);
            mBinding.ilActive.ivMusicStart.setVisibility(View.VISIBLE);
            mBinding.ilActive.switchOriginal.setVisibility(View.VISIBLE);
            mBinding.ilActive.rlMusicControlMenu.setVisibility(View.VISIBLE);
            mBinding.ilActive.ivSkipPostlude.setVisibility(View.INVISIBLE);
            mBinding.ilActive.switchOriginal.setChecked(false); // reset ui icon for mAudioTrackMode
            mBinding.ilActive.switchOriginal.setIconResource(R.mipmap.ic_play_original_off);
            mBinding.ilActive.switchOriginal.setText(R.string.ktv_room_original);
            mBinding.ilActive.ivJoinChorusBtn.setVisibility(View.INVISIBLE);
            mBinding.ilActive.ivLeaveChorus.setVisibility(View.INVISIBLE);
        } else if (this.mRole == Role.Listener) {
            mBinding.ilActive.lyricsView.enableDragging(false);
            mBinding.ilActive.rlMusicControlMenu.setVisibility(View.GONE);
            if (!isSeatFull || isOnSeat) {
                mBinding.ilActive.ivJoinChorusBtn.setVisibility(View.VISIBLE);
            }
            mBinding.ilActive.ivLeaveChorus.setVisibility(View.INVISIBLE);
        }
        if (isMineOwner) {
            mBinding.ilActive.rlMusicControlMenu.setVisibility(View.VISIBLE);
            mBinding.ilActive.ivMusicStart.setVisibility(View.VISIBLE);
            mBinding.ilActive.switchOriginal.setVisibility(View.VISIBLE);
            mBinding.ilActive.ivSkipPostlude.setVisibility(View.INVISIBLE);
            if (this.mRole == Role.Listener) {
                mBinding.ilActive.ivMusicStart.setVisibility(View.GONE);
                mBinding.ilActive.switchOriginal.setVisibility(View.INVISIBLE);
                mBinding.ilActive.ivSkipPostlude.setVisibility(View.INVISIBLE);
                mBinding.ilActive.ivSkipPrelude.setVisibility(View.INVISIBLE);
            }
        }
    }

    public void onPauseStatus() {
        mBinding.ilIDLE.getRoot().setVisibility(View.GONE);
        mBinding.clActive.setVisibility(View.VISIBLE);
        mPrepareBinding.statusPrepareViewLrc.setVisibility(View.GONE);
        mBinding.ilActive.getRoot().setVisibility(View.VISIBLE);

        mBinding.ilActive.ivMusicStart.setIconResource(R.drawable.ktv_ic_play);
        mBinding.ilActive.ivMusicStart.setText(R.string.ktv_room_player_play);
    }

    public void onIdleStatus() {
        mBinding.ilIDLE.getRoot().setVisibility(View.VISIBLE);
        mBinding.clActive.setVisibility(View.GONE);
        mPrepareBinding.statusPrepareViewLrc.setVisibility(View.GONE);
        mBinding.ilActive.getRoot().setVisibility(View.GONE);
    }

    public void setRole(@NonNull Role mRole) {
        this.mRole = mRole;
        lrcUrl = null;
        changeViewByRole();
    }

    public Role getRole() {
        return this.mRole;
    }

    public void setMusic(@NonNull RoomSelSongModel mMusic) {
        mKaraokeView.reset();
        if (mComboControl != null) {
            mComboControl.reset(mBinding);
        }

        mBinding.tvMusicName.setText(mMusic.getSongName() + "-" + mMusic.getSinger());

        mBinding.ivCumulativeScoreGrade.setVisibility(INVISIBLE);
        mBinding.tvCumulativeScore.setText(String.format(getResources().getString(R.string.ktv_score_formatter), "0"));
        mBinding.gradeView.setScore(0, 0, 0);
    }

    public void setLrcViewBackground(@DrawableRes int resId) {
        int defaultColor = ContextCompat.getColor(getContext(), R.color.pink_b4);
        mBinding.ilActive.lyricsView.setCurrentLineHighlightedTextColor(defaultColor);

        defaultColor = ContextCompat.getColor(getContext(), R.color.white);
        mBinding.ilActive.lyricsView.setCurrentLineTextColor(defaultColor);
        mBinding.clActive.setBackgroundResource(resId);
    }

    public void updateScore(double score, double cumulativeScore, double perfectScore) {
        mCumulativeScoreInPercentage = (int) ((cumulativeScore / perfectScore) * 100);

        mBinding.gradeView.setScore((int) score, (int) cumulativeScore, (int) perfectScore);

        mBinding.tvCumulativeScore.setText(String.format(getResources().getString(R.string.ktv_score_formatter), "" + (int) cumulativeScore));
        int gradeDrawable = mBinding.gradeView.getCumulativeDrawable();
        if (gradeDrawable == 0) {
            mBinding.ivCumulativeScoreGrade.setVisibility(INVISIBLE);
        } else {
            mBinding.ivCumulativeScoreGrade.setImageResource(gradeDrawable);
            mBinding.ivCumulativeScoreGrade.setVisibility(VISIBLE);
        }

        if (mComboControl == null) {
            mComboControl = new ComboControl();
        }
        mComboControl.checkAndShowCombos(mBinding, (int) score, (int) cumulativeScore);
    }

    private static class ComboControl {
        private GifDrawable mComboIconDrawable;

        private int mNumberOfCombos;

        private void reset(KtvLayoutLrcControlViewBinding binding) {
            mNumberOfCombos = 0;
            binding.comboView.getRoot().setVisibility(INVISIBLE);
        }

        private void checkAndShowCombos(KtvLayoutLrcControlViewBinding binding, int score, int cumulativeScore) {
            binding.comboView.getRoot().setVisibility(VISIBLE);

            showComboAnimation(binding.comboView.getRoot(), score);
            showScoreAnimation((View) binding.comboView.getRoot().getParent(), score);
        }

        private int mComboOfLastTime; // Only for showComboAnimation

        private void showComboAnimation(View comboView, int score) {
            int comboIconRes = 0;

            if (score >= 90) {
                comboIconRes = R.drawable.combo_excellent_3x;
            } else if (score >= 75) {
                comboIconRes = R.drawable.combo_good_3x;
            } else if (score >= 60) {
                comboIconRes = R.drawable.combo_fair_3x;
            }

            ImageView comboIcon = comboView.findViewById(R.id.combo_icon);
            TextView comboText = comboView.findViewById(R.id.combo_text);

            boolean sameWithLastTime = (comboIconRes == mComboOfLastTime);
            mComboOfLastTime = comboIconRes;

            if (comboIconRes > 0) {
                if (sameWithLastTime) {
                    mNumberOfCombos++;
                } else {
                    mNumberOfCombos = 1;
                }

                RequestOptions options = new RequestOptions().diskCacheStrategy(DiskCacheStrategy.RESOURCE);
                OutlineSpan outlineSpan = new OutlineSpan(Color.parseColor("#368CFF"), 10F
                );
                Glide.with(comboView.getContext()).asGif().load(comboIconRes).apply(options).addListener(new RequestListener<GifDrawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<GifDrawable> target, boolean isFirstResource) {
                        return true;
                    }

                    @Override
                    public boolean onResourceReady(GifDrawable resource, Object model, Target<GifDrawable> target, DataSource dataSource, boolean isFirstResource) {
                        mComboIconDrawable = resource;

                        resource.registerAnimationCallback(new Animatable2Compat.AnimationCallback() {
                            @Override
                            public void onAnimationStart(Drawable drawable) {
                                super.onAnimationStart(drawable);
                            }

                            @Override
                            public void onAnimationEnd(Drawable drawable) {
                                super.onAnimationEnd(drawable);

                                comboText.setAlpha(0f);
                                comboIcon.setVisibility(INVISIBLE);
                                comboText.setVisibility(INVISIBLE);

                                mComboIconDrawable.unregisterAnimationCallback(this);
                            }
                        });

                        resource.setLoopCount(1);

                        comboIcon.setVisibility(VISIBLE);

                        comboText.setAlpha(0f);
                        comboText.setVisibility(mNumberOfCombos == 1 ? INVISIBLE : VISIBLE); // Per request from product team, do not show `+X` view for first one
                        if (mNumberOfCombos != 1) {
                            String text = "x" + mNumberOfCombos;
                            SpannableString spannable = new SpannableString(text);
                            spannable.setSpan(outlineSpan, 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            comboText.setText(spannable);
                            comboText.animate().alpha(1f).setDuration(500).setStartDelay(0).start();
                        }

                        return false;
                    }
                }).into(comboIcon);
            } else {
                mNumberOfCombos = 0;
                comboIcon.setVisibility(INVISIBLE);
                comboText.setVisibility(INVISIBLE);
            }
        }

        private float mInitialYOfScoreView; // Only for showScoreAnimation

        private void showScoreAnimation(View lyricsControlView, double score) {
            TextView lineScore = lyricsControlView.findViewById(R.id.line_score);
            int widthOfParent = ((View) (lineScore.getParent())).getWidth();
            int marginLeft = (int) (widthOfParent * 0.4);
            ((MarginLayoutParams) (lineScore.getLayoutParams())).leftMargin = marginLeft;
            ((MarginLayoutParams) (lineScore.getLayoutParams())).setMarginStart(marginLeft);

            lineScore.setText("+" + (int) score);
            lineScore.setAlpha(1.0f);
            lineScore.setVisibility(VISIBLE);
            if (mInitialYOfScoreView == 0) {
                mInitialYOfScoreView = lineScore.getY();
            }

            float movingPixels = 200;
            lineScore.animate().translationY(-movingPixels).setDuration(1000).setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    lineScore.animate().alpha(0).setDuration(100).setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            lineScore.setY(mInitialYOfScoreView);
                            lineScore.setVisibility(INVISIBLE);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }
                    }).start();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            }).start();

        }

    }

    private float mInitialYOfChorus;

    public int getYOfChorusBtn() {
        if (mInitialYOfChorus == 0) {
            mInitialYOfChorus = mBinding.ilActive.ivJoinChorusBtn.getY();
        }
        return (int) mInitialYOfChorus;
    }

    @Override
    public void onClick(View v) {
        if (v == mBinding.ilActive.switchOriginal) {
            if (mRole == Role.Singer) {
                if (mAudioTrack == AudioTrack.Acc) {
                    mAudioTrack = AudioTrack.DaoChang;
                    mOnKaraokeActionListener.onSwitchOriginalClick(AudioTrack.DaoChang);
                    mBinding.ilActive.switchOriginal.setIconResource(R.mipmap.ktv_ic_daochang);
                    mBinding.ilActive.switchOriginal.setText(R.string.ktv_room_daochang);
                } else if (mAudioTrack == AudioTrack.DaoChang) {
                    mAudioTrack = AudioTrack.Origin;
                    mOnKaraokeActionListener.onSwitchOriginalClick(AudioTrack.Origin);
                    mBinding.ilActive.switchOriginal.setIconResource(R.mipmap.ic_play_original_on);
                    mBinding.ilActive.switchOriginal.setText(R.string.ktv_room_original);
                } else {
                    mAudioTrack = AudioTrack.Acc;
                    mOnKaraokeActionListener.onSwitchOriginalClick(AudioTrack.Acc);
                    mBinding.ilActive.switchOriginal.setIconResource(R.mipmap.ic_play_original_off);
                }
            } else if (mRole == Role.CoSinger) {
                if (mAudioTrack == AudioTrack.Acc) {
                    mAudioTrack = AudioTrack.Origin;
                    mOnKaraokeActionListener.onSwitchOriginalClick(AudioTrack.Origin);
                    mBinding.ilActive.switchOriginal.setIconResource(R.mipmap.ic_play_original_on);
                    mBinding.ilActive.switchOriginal.setText(R.string.ktv_room_original);
                } else {
                    mAudioTrack = AudioTrack.Acc;
                    mOnKaraokeActionListener.onSwitchOriginalClick(AudioTrack.Acc);
                    mBinding.ilActive.switchOriginal.setIconResource(R.mipmap.ic_play_original_off);
                }
            }
        } else if (v == mBinding.ilActive.ivMusicStart) {
            mOnKaraokeActionListener.onPlayClick();
        } else if (v == mBinding.ilActive.ivChangeSong) {
            mOnKaraokeActionListener.onChangeMusicClick();
        } else if (v == mBinding.ilActive.ivSkipPreludeSkip) {
            mOnKaraokeActionListener.onSkipPreludeClick();
            mBinding.ilActive.ivSkipPrelude.setVisibility(INVISIBLE);
        } else if (v == mBinding.ilActive.ivSkipPostludeSkip) {
            mOnKaraokeActionListener.onSkipPostludeClick();
        } else if (v == mBinding.ilActive.ivSkipPreludeCancel) {
            mBinding.ilActive.ivSkipPrelude.setVisibility(INVISIBLE);
        } else if (v == mBinding.ilActive.ivSkipPostludeCancel) {
            mBinding.ilActive.ivSkipPostlude.setVisibility(INVISIBLE);
        } else if (v == mBinding.ilActive.ivJoinChorusBtn) {
            if (UiUtils.isFastClick(2000)) {
                CustomToast.show(R.string.ktv_too_fast, Toast.LENGTH_SHORT);
                return;
            }
            mOnKaraokeActionListener.onJoinChorus();
            mBinding.ilActive.ivJoinChorusLoading.setVisibility(VISIBLE);
            mPrepareBinding.tvContent.setText(String.format(getResources().getString(R.string.ktv_loading_music), "0%"));
            mPrepareBinding.pbLoadingMusic.setProgress(0);
            mPrepareBinding.statusPrepareViewLrc.setVisibility(VISIBLE);

            if (isMineOwner) {
                mBinding.ilActive.ivChangeSong.setVisibility(INVISIBLE);
            }
        } else if (v == mBinding.ilActive.ivLeaveChorus) {
            mOnKaraokeActionListener.onLeaveChorus();
        } else if (v == mBinding.ilActive.downloadLrcFailedBtn) {
            mBinding.ilActive.downloadLrcFailedView.setVisibility(View.INVISIBLE);
            mBinding.ilActive.downloadLrcFailedBtn.setVisibility(View.INVISIBLE);
            if (lrcUrl == null) {
                mOnKaraokeActionListener.onReGetLrcUrl();
            } else {
                downloadAndSetLrcData();
            }
        } else if (v == mBinding.ilActive.btnVocalHighlight) {
            if (mOnKaraokeActionListener != null) {
                mOnKaraokeActionListener.onVocalHighlightClick();
            }
        }
    }

    public void setSwitchOriginalChecked(boolean checked) {
        mBinding.ilActive.switchOriginal.setChecked(checked);
    }

    // ------------------ ILrcView ------------------
    @Override
    public void onUpdatePitch(Float pitch) {
        if (mKaraokeView == null) return;
        mKaraokeView.setPitch(pitch);
    }

    @Override
    public void onUpdateProgress(Long progress) {
        if (mKaraokeView.getLyricsData() == null) return;
        if (mRole == Role.Singer) {
            if (progress >= mKaraokeView.getLyricsData().startOfVerse - 2000) {
                mBinding.ilActive.ivSkipPrelude.setVisibility(INVISIBLE);
            }

            if (progress >= mKaraokeView.getLyricsData().duration) {
                mBinding.ilActive.ivSkipPostlude.setVisibility(VISIBLE);
            } else {
                mBinding.ilActive.ivSkipPostlude.setVisibility(INVISIBLE);
            }
        }
        mKaraokeView.setProgress(progress);
    }

    private String lrcUrl;

    @Override
    public void onDownloadLrcData(String url) {
        this.lrcUrl = url;
        downloadAndSetLrcData();
    }

    private void downloadAndSetLrcData() {
        DownloadUtils.getInstance().download(getContext(), lrcUrl, file -> {
            if (file.getName().endsWith(".zip")) {
                ZipUtils.unzipOnlyPlainXmlFilesAsync(file.getAbsolutePath(),
                        file.getAbsolutePath().replace(".zip", ""),
                        new ZipUtils.UnZipCallback() {
                            @Override
                            public void onFileUnZipped(List<String> unZipFilePaths) {
                                String xmlPath = "";
                                for (String path : unZipFilePaths) {
                                    if (path.endsWith(".xml")) {
                                        xmlPath = path;
                                        break;
                                    }
                                }

                                if (TextUtils.isEmpty(xmlPath)) {
                                    CustomToast.show("The xml file not exist!", Toast.LENGTH_SHORT);
                                    mBinding.ilActive.downloadLrcFailedView.setVisibility(View.VISIBLE);
                                    mBinding.ilActive.downloadLrcFailedBtn.setVisibility(View.VISIBLE);
                                    return;
                                }

                                File xmlFile = new File(xmlPath);
                                LyricsModel lyricsModel = KaraokeView.parseLyricsData(xmlFile);

                                if (lyricsModel == null) {
                                    CustomToast.show("Unexpected content from " + xmlPath, Toast.LENGTH_SHORT);
                                    mBinding.ilActive.downloadLrcFailedView.setVisibility(View.VISIBLE);
                                    mBinding.ilActive.downloadLrcFailedBtn.setVisibility(View.VISIBLE);
                                    return;
                                }

                                if (mKaraokeView != null) {
                                    mBinding.ilActive.downloadLrcFailedView.setVisibility(View.INVISIBLE);
                                    mKaraokeView.setLyricsData(lyricsModel);
                                }
                            }

                            @Override
                            public void onError(Exception e) {
                                mBinding.ilActive.downloadLrcFailedView.setVisibility(View.VISIBLE);
                                mBinding.ilActive.downloadLrcFailedBtn.setVisibility(View.VISIBLE);
                                if (e.getMessage()!=null){
                                    CustomToast.show(e.getMessage(), Toast.LENGTH_SHORT);
                                }
                            }
                        });
            } else {
                LyricsModel lyricsModel = KaraokeView.parseLyricsData(file);

                if (lyricsModel == null) {
                    CustomToast.show("Unexpected content from " + file, Toast.LENGTH_SHORT);
                    mBinding.ilActive.downloadLrcFailedView.setVisibility(View.VISIBLE);
                    mBinding.ilActive.downloadLrcFailedBtn.setVisibility(View.VISIBLE);
                    return;
                }

                if (mKaraokeView != null) {
                    mBinding.ilActive.downloadLrcFailedView.setVisibility(View.INVISIBLE);
                    mBinding.ilActive.downloadLrcFailedBtn.setVisibility(View.INVISIBLE);
                    mKaraokeView.setLyricsData(lyricsModel);
                }
            }
        }, exception -> {
            if (exception.getMessage() != null) {
                CustomToast.show(exception.getMessage(), Toast.LENGTH_SHORT);
            }
            mBinding.ilActive.downloadLrcFailedView.setVisibility(View.VISIBLE);
            mBinding.ilActive.downloadLrcFailedBtn.setVisibility(View.VISIBLE);
        });
    }

    public void onNoLrc() {
        lrcUrl = null;
        mBinding.ilActive.downloadLrcFailedView.setVisibility(View.VISIBLE);
        mBinding.ilActive.downloadLrcFailedBtn.setVisibility(View.VISIBLE);
    }

    public void onReceiveSingleLineScore(int score, int index, int cumulativeScore, int total) {
        if (mRole == Role.Listener) {
            updateScore(score, cumulativeScore, /** Workaround(Hai_Guo)*/total);
        }
    }

    public void setHighLightPersonHeadUrl(String url) {
        GlideApp.with(mBinding.getRoot())
                .load(url)
                .error(R.mipmap.ktv_highlight_head_bg)
                .apply(RequestOptions.circleCropTransform())
                .into(mBinding.ilActive.ivVocalHighlight);
    }

    public void showHighLightButton(boolean show) {
        if (show) {
            mBinding.ilActive.btnVocalHighlight.setVisibility(View.VISIBLE);
            mBinding.ilActive.btnVocalHighlight.bringToFront();
        } else {
            mBinding.ilActive.btnVocalHighlight.setVisibility(View.GONE);
        }
    }

    public interface OnKaraokeEventListener {
        default void onSwitchOriginalClick(AudioTrack audioTrack) { // 0: origin 1: acc 2: daochang
        }

        default void onPlayClick() {
        }

        default void onChangeMusicClick() {
        }

        default void onStartSing() {
        }

        default void onJoinChorus() {
        }

        default void onLeaveChorus() {
        }

        default void onDragTo(long position) {
        }

        default void onRefPitchUpdate(float refPitch, int numberOfRefPitches) {
        }

        default void onLineFinished(LyricsLineModel line, int score, int cumulativeScore, int index, int total) {
        }

        default void onSkipPreludeClick() {
        }

        default void onSkipPostludeClick() {
        }

        default void onReGetLrcUrl() {
        }

        default void onVocalHighlightClick() {

        }
    }
}
