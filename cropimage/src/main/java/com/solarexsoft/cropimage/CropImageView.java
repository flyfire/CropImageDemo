package com.solarexsoft.cropimage;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;

import com.solarexsoft.cropimage.animation.SimpleValueAnimator;
import com.solarexsoft.cropimage.animation.ValueAnimatorV14;
import com.solarexsoft.cropimage.animation.ValueAnimatorV8;
import com.solarexsoft.cropimage.callback.Callback;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/*
 * Creadted by houruhou on 2023/01/12 13:45
 */
public class CropImageView extends AppCompatImageView {
    private static final String TAG = "CropImageView";

    private static final int HANDLE_SIZE_IN_DP = 14;
    private static final int MIN_FRAME_SIZE_IN_DP = 50;
    private static final int FRAME_STROKE_WEIGHT_IN_DP = 1;
    private static final int GUIDE_STROKE_WEIGHT_IN_DP = 1;
    private static final float DEFAULT_INITIAL_FRAME_SCALE = 1f;
    private static final int DEFAULT_ANIMATION_DURATION_MILLIS = 100;
    private static final int DEBUG_TEXT_SIZE_IN_DP = 15;

    private static final int TRANSPARENT = 0x00000000;
    private static final int TRANSLUCENT_WHITE = 0xBBFFFFFF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int TRANSLUCENT_BLACK = 0xBB000000;

    private int mViewWidth = 0;
    private int mViewHeight = 0;
    private float mScale = 1.0f;
    private float mAngle = 0.0f;
    private float mImgWidth = 0.0f;
    private float mImgHeight = 0.0f;

    private boolean mIsInitialized = false;
    private Matrix mMatrix = null;
    private Paint mPaintTranslucent;
    private Paint mPaintFrame;
    private Paint mPaintBitmap;
    private Paint mPaintDebug;
    private RectF mFrameRect;
    private RectF mInitialFrameRect;
    private RectF mImageRect;
    private PointF mCenter = new PointF();
    private float mLastX, mLastY;
    private boolean mIsRotating = false;
    private boolean mIsAnimating = false;
    private SimpleValueAnimator mAnimator = null;
    private final Interpolator DEFAULT_INTERPOLATOR = new DecelerateInterpolator();
    private Interpolator mInterpolator = DEFAULT_INTERPOLATOR;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Uri mSourceUri = null;
    private Uri mSaveUri = null;
    private int mExifRotation = 0;
    private int mOutputMaxWidth;
    private int mOutputMaxHeight;
    private int mOutputWidth = 0;
    private int mOutputHeight = 0;
    private boolean mIsDebug = false;
    private Bitmap.CompressFormat mCompressFormat = Bitmap.CompressFormat.PNG;
    private int mCompressQuality = 100;
    private int mInputImageWidth = 0;
    private int mInputImageHeight = 0;
    private int mOutputImageWidth = 0;
    private int mOutputImageHeight = 0;
    private AtomicBoolean mIsLoading = new AtomicBoolean(false);
    private AtomicBoolean mIsCropping = new AtomicBoolean(false);
    private AtomicBoolean mIsSaving = new AtomicBoolean(false);
    private ExecutorService mExecutor;

    private TouchArea mTouchArea = TouchArea.OUT_OF_BOUNDS;
    private CropMode mCropMode = CropMode.SQUARE;
    private ShowMode mGuideShowMode = ShowMode.SHOW_ALWAYS;
    private ShowMode mHandleShowMode = ShowMode.SHOW_ALWAYS;
    private float mMinFrameSize;
    private int mHandleSize;
    private int mTouchPadding = 0;
    private boolean mShowGuide = true;
    private boolean mShowHandle = true;
    private boolean mIsCropEnabled = true;
    private boolean mIsEnabled = true;
    private PointF mCustomRatio = new PointF(1.0f, 1.0f);
    private float mFrameStrokeWeight = 2.0f;
    private float mGuideStrokeWeight = 2.0f;
    private int mBackgroundColor;
    private int mOverlayColor;
    private int mFrameColor;
    private int mHandleColor;
    private int mGuideColor;
    private float mInitialFrameScale;
    private boolean mIsAnimationEnabled = true;
    private int mAnimationDurationMillis = DEFAULT_ANIMATION_DURATION_MILLIS;
    private boolean mIsHandleShadowEnabled = true;

    public CropImageView(Context context) {
        this(context, null);
    }

    public CropImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CropImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mExecutor = Executors.newSingleThreadExecutor();
        float density = getDensity();
        mHandleSize = (int) (density * HANDLE_SIZE_IN_DP);
        mMinFrameSize = density * MIN_FRAME_SIZE_IN_DP;
        mFrameStrokeWeight = density * FRAME_STROKE_WEIGHT_IN_DP;
        mGuideStrokeWeight = density * GUIDE_STROKE_WEIGHT_IN_DP;

        mPaintFrame = new Paint();
        mPaintTranslucent = new Paint();
        mPaintBitmap = new Paint();
        mPaintBitmap.setFilterBitmap(true);
        mPaintDebug = new Paint();
        mPaintDebug.setAntiAlias(true);
        mPaintDebug.setStyle(Paint.Style.STROKE);
        mPaintDebug.setColor(WHITE);
        mPaintDebug.setTextSize((float) DEBUG_TEXT_SIZE_IN_DP * density);

        mMatrix = new Matrix();
        mScale = 1.0f;
        mBackgroundColor = TRANSPARENT;
        mFrameColor = WHITE;
        mOverlayColor = TRANSLUCENT_BLACK;
        mHandleColor = WHITE;
        mGuideColor = TRANSLUCENT_WHITE;

        handleStyleable(context, attrs, defStyleAttr, density);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.mode = this.mCropMode;
        ss.backgroundColor = this.mBackgroundColor;
        ss.overlayColor = this.mOverlayColor;
        ss.frameColor = this.mFrameColor;
        ss.guideShowMode = this.mGuideShowMode;
        ss.handleShowMode = this.mHandleShowMode;
        ss.showGuide = this.mShowGuide;
        ss.showHandle = this.mShowHandle;
        ss.handleSize = this.mHandleSize;
        ss.touchPadding = this.mTouchPadding;
        ss.minFrameSize = this.mMinFrameSize;
        ss.customRatioX = this.mCustomRatio.x;
        ss.customRatioY = this.mCustomRatio.y;
        ss.frameStrokeWeight = this.mFrameStrokeWeight;
        ss.guideStrokeWeight = this.mGuideStrokeWeight;
        ss.isCropEnabled = this.mIsCropEnabled;
        ss.handleColor = this.mHandleColor;
        ss.guideColor = this.mGuideColor;
        ss.initialFrameScale = this.mInitialFrameScale;
        ss.angle = this.mAngle;
        ss.isAnimationEnabled = this.mIsAnimationEnabled;
        ss.animationDuration = this.mAnimationDurationMillis;
        ss.exifRotation = this.mExifRotation;
        ss.sourceUri = this.mSourceUri;
        ss.saveUri = this.mSaveUri;
        ss.compressFormat = this.mCompressFormat;
        ss.compressQuality = this.mCompressQuality;
        ss.isDebug = this.mIsDebug;
        ss.outputMaxWidth = this.mOutputMaxWidth;
        ss.outputMaxHeight = this.mOutputMaxHeight;
        ss.outputWidth = this.mOutputWidth;
        ss.outputHeight = this.mOutputHeight;
        ss.isHandleShadowEnabled = this.mIsHandleShadowEnabled;
        ss.inputImageWidth = this.mInputImageWidth;
        ss.inputImageHeight = this.mInputImageHeight;
        ss.outputImageWidth = this.mOutputImageWidth;
        ss.outputImageHeight = this.mOutputImageHeight;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        this.mCropMode = ss.mode;
        this.mBackgroundColor = ss.backgroundColor;
        this.mOverlayColor = ss.overlayColor;
        this.mFrameColor = ss.frameColor;
        this.mGuideShowMode = ss.guideShowMode;
        this.mHandleShowMode = ss.handleShowMode;
        this.mShowGuide = ss.showGuide;
        this.mShowHandle = ss.showHandle;
        this.mHandleSize = ss.handleSize;
        this.mTouchPadding = ss.touchPadding;
        this.mMinFrameSize = ss.minFrameSize;
        this.mCustomRatio = new PointF(ss.customRatioX, ss.customRatioY);
        this.mFrameStrokeWeight = ss.frameStrokeWeight;
        this.mGuideStrokeWeight = ss.guideStrokeWeight;
        this.mIsCropEnabled = ss.isCropEnabled;
        this.mHandleColor = ss.handleColor;
        this.mGuideColor = ss.guideColor;
        this.mInitialFrameScale = ss.initialFrameScale;
        this.mAngle = ss.angle;
        this.mIsAnimationEnabled = ss.isAnimationEnabled;
        this.mAnimationDurationMillis = ss.animationDuration;
        this.mExifRotation = ss.exifRotation;
        this.mSourceUri = ss.sourceUri;
        this.mSaveUri = ss.saveUri;
        this.mCompressFormat = ss.compressFormat;
        this.mCompressQuality = ss.compressQuality;
        this.mIsDebug = ss.isDebug;
        this.mOutputMaxWidth = ss.outputMaxWidth;
        this.mOutputMaxHeight = ss.outputMaxHeight;
        this.mOutputWidth = ss.outputWidth;
        this.mOutputHeight = ss.outputHeight;
        this.mIsHandleShadowEnabled = ss.isHandleShadowEnabled;
        this.mInputImageWidth = ss.inputImageWidth;
        this.mInputImageHeight = ss.inputImageHeight;
        this.mOutputImageWidth = ss.outputImageWidth;
        this.mOutputImageHeight = ss.outputImageHeight;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        final int viewHeight = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(viewWidth, viewHeight);

        mViewWidth = viewWidth - getPaddingLeft() - getPaddingRight();
        mViewHeight = viewHeight - getPaddingTop() - getPaddingBottom();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (getDrawable() != null) {
            setupLayout(mViewWidth, mViewHeight);
        }
    }

    private void setupLayout(int viewW, int viewH) {
        if (viewW == 0 || viewH == 0) return;
        setCenter(new PointF(getPaddingLeft() + viewW * 0.5f, getPaddingTop() + viewH * 0.5f));
        setScale(calcScale(viewW, viewH, mAngle));
        setMatrix();
        mImageRect = calcImageRect(new RectF(0f, 0f, mImgWidth, mImgHeight), mMatrix);
        // todo
    }

    private void setCenter(PointF center) {
        this.mCenter = center;
    }

    private void setScale(float scale) {
        this.mScale = scale;
    }

    private void setMatrix() {
        mMatrix.reset();
        mMatrix.postTranslate(mCenter.x - mImgWidth * 0.5f, mCenter.y - mImgHeight * 0.5f);
        mMatrix.postScale(mScale, mScale, mCenter.x, mCenter.y);
        mMatrix.postRotate(mAngle, mCenter.x, mCenter.y);
    }

    private RectF calcImageRect(RectF rectF, Matrix matrix) {
        RectF applied = new RectF();
        matrix.mapRect(applied, rectF);
        return applied;
    }

    private float calcScale(int viewW, int viewH, float angle) {
        mImgWidth = getDrawable().getIntrinsicWidth();
        mImgHeight = getDrawable().getIntrinsicHeight();
        if (mImgWidth <= 0) {
            mImgWidth = viewW;
        }
        if (mImgHeight <= 0) {
            mImgHeight = viewH;
        }
        float viewRatio = (float) viewW / (float) viewH;
        float imgRatio = getRotatedWidth(angle) / getRotatedHeight(angle);
        float scale = 1.0f;
        if (imgRatio >= viewRatio) {
            scale = viewW / getRotatedWidth(angle);
        } else if (imgRatio < viewRatio) {
            scale = viewH / getRotatedHeight(angle);
        }
        return scale;
    }

    private void handleStyleable(Context context, AttributeSet attrs, int defStyle, float mDensity) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.scv_CropImageView, defStyle, 0);
        Drawable drawable;
        mCropMode = CropMode.SQUARE;
        try {
            drawable = ta.getDrawable(R.styleable.scv_CropImageView_scv_img_src);
            if (drawable != null) setImageDrawable(drawable);
            for (CropMode mode : CropMode.values()) {
                if (ta.getInt(R.styleable.scv_CropImageView_scv_crop_mode, 3) == mode.getId()) {
                    mCropMode = mode;
                    break;
                }
            }
            mBackgroundColor = ta.getColor(R.styleable.scv_CropImageView_scv_background_color, TRANSPARENT);
            mOverlayColor = ta.getColor(R.styleable.scv_CropImageView_scv_overlay_color, TRANSLUCENT_BLACK);
            mFrameColor = ta.getColor(R.styleable.scv_CropImageView_scv_frame_color, WHITE);
            mHandleColor = ta.getColor(R.styleable.scv_CropImageView_scv_handle_color, WHITE);
            mGuideColor = ta.getColor(R.styleable.scv_CropImageView_scv_guide_color, TRANSLUCENT_WHITE);
            for (ShowMode mode : ShowMode.values()) {
                if (ta.getInt(R.styleable.scv_CropImageView_scv_guide_show_mode, 1) == mode.getId()) {
                    mGuideShowMode = mode;
                    break;
                }
            }

            for (ShowMode mode : ShowMode.values()) {
                if (ta.getInt(R.styleable.scv_CropImageView_scv_handle_show_mode, 1) == mode.getId()) {
                    mHandleShowMode = mode;
                    break;
                }
            }
            setGuideShowMode(mGuideShowMode);
            setHandleShowMode(mHandleShowMode);
            mHandleSize = ta.getDimensionPixelSize(R.styleable.scv_CropImageView_scv_handle_size, (int) (HANDLE_SIZE_IN_DP * mDensity));
            mTouchPadding = ta.getDimensionPixelSize(R.styleable.scv_CropImageView_scv_touch_padding, 0);
            mMinFrameSize = ta.getDimensionPixelSize(R.styleable.scv_CropImageView_scv_min_frame_size, (int) (MIN_FRAME_SIZE_IN_DP * mDensity));
            mFrameStrokeWeight = ta.getDimensionPixelSize(R.styleable.scv_CropImageView_scv_frame_stroke_weight, (int) (FRAME_STROKE_WEIGHT_IN_DP * mDensity));
            mGuideStrokeWeight = ta.getDimensionPixelSize(R.styleable.scv_CropImageView_scv_guide_stroke_weight, (int) (GUIDE_STROKE_WEIGHT_IN_DP * mDensity));
            mIsCropEnabled = ta.getBoolean(R.styleable.scv_CropImageView_scv_crop_enabled, true);
            mInitialFrameScale = constrain(ta.getFloat(R.styleable.scv_CropImageView_scv_initial_frame_scale, DEFAULT_INITIAL_FRAME_SCALE), 0.01f, 1.0f, DEFAULT_INITIAL_FRAME_SCALE);
            mIsAnimationEnabled = ta.getBoolean(R.styleable.scv_CropImageView_scv_animation_enabled, true);
            mAnimationDurationMillis = ta.getInt(R.styleable.scv_CropImageView_scv_animation_duration, DEFAULT_ANIMATION_DURATION_MILLIS);
            mIsHandleShadowEnabled = ta.getBoolean(R.styleable.scv_CropImageView_scv_handle_shadow_enabled, true);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ta.recycle();
        }
    }

    public void setGuideShowMode(ShowMode mode) {
        mGuideShowMode = mode;
        switch (mode) {
            case SHOW_ALWAYS:
                mShowGuide = true;
                break;
            case NOT_SHOW:
            case SHOW_ON_TOUCH:
                mShowGuide = false;
                break;
        }
        invalidate();
    }

    public void setHandleShowMode(ShowMode mode) {
        mHandleShowMode = mode;
        switch (mode) {
            case SHOW_ALWAYS:
                mShowHandle = true;
                break;
            case NOT_SHOW:
            case SHOW_ON_TOUCH:
                mShowHandle = false;
                break;
        }
        invalidate();
    }

    private float getDensity() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.density;
    }

    private float sq(float value) {
        return value * value;
    }

    private float constrain(float val, float min, float max, float defaultVal) {
        if (val < min || val > max) return defaultVal;
        return val;
    }

    private void postErrorOnMainThread(final Callback callback, final Throwable e) {
        if (callback == null) return;
        if (Looper.myLooper() == Looper.getMainLooper()) {
            callback.onError(e);
        } else {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onError(e);
                }
            });
        }
    }

    private Bitmap getBitmap() {
        Bitmap bm = null;
        Drawable d = getDrawable();
        if (d != null && d instanceof BitmapDrawable) bm = ((BitmapDrawable) d).getBitmap();
        return bm;
    }

    private float getRotatedWidth(float angle) {
        return getRotatedWidth(angle, mImgWidth, mImgHeight);
    }

    private float getRotatedWidth(float angle, float width, float height) {
        return angle % 180 == 0 ? width : height;
    }

    private float getRotatedHeight(float angle) {
        return getRotatedHeight(angle, mImgWidth, mImgHeight);
    }

    private float getRotatedHeight(float angle, float width, float height) {
        return angle % 180 == 0 ? height : width;
    }

    private Bitmap getRotatedBitmap(Bitmap bitmap) {
        Matrix rotateMatrix = new Matrix();
        rotateMatrix.setRotate(mAngle, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotateMatrix, true);
    }

    // Animation ///////////////////////////////////////////////////////////////////////////////////

    private SimpleValueAnimator getAnimator() {
        setupAnimatorIfNeeded();
        return mAnimator;
    }

    private void setupAnimatorIfNeeded() {
        if (mAnimator == null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                mAnimator = new ValueAnimatorV8(mInterpolator);
            } else {
                mAnimator = new ValueAnimatorV14(mInterpolator);
            }
        }
    }

    private enum TouchArea {
        OUT_OF_BOUNDS, CENTER, LEFT_TOP, RIGHT_TOP, LEFT_BOTTOM, RIGHT_BOTTOM
    }

    public enum CropMode {
        FIT_IMAGE(0), RATIO_4_3(1), RATIO_3_4(2), SQUARE(3), RATIO_16_9(4), RATIO_9_16(5), FREE(6), CUSTOM(7), CIRCLE(8), CIRCLE_SQUARE(9);
        private final int ID;

        CropMode(final int id) {
            this.ID = id;
        }

        public int getId() {
            return ID;
        }
    }

    public enum ShowMode {
        SHOW_ALWAYS(1), SHOW_ON_TOUCH(2), NOT_SHOW(3);
        private final int ID;

        ShowMode(final int id) {
            this.ID = id;
        }

        public int getId() {
            return ID;
        }
    }

    public enum RotateDegrees {
        ROTATE_90D(90), ROTATE_180D(180), ROTATE_270D(270), ROTATE_M90D(-90), ROTATE_M180D(-180), ROTATE_M270D(-270);

        private final int VALUE;

        RotateDegrees(final int value) {
            this.VALUE = value;
        }

        public int getValue() {
            return VALUE;
        }
    }

    // Save/Restore support ////////////////////////////////////////////////////////////////////////

    public static class SavedState extends BaseSavedState {
        CropMode mode;
        int backgroundColor;
        int overlayColor;
        int frameColor;
        ShowMode guideShowMode;
        ShowMode handleShowMode;
        boolean showGuide;
        boolean showHandle;
        int handleSize;
        int touchPadding;
        float minFrameSize;
        float customRatioX;
        float customRatioY;
        float frameStrokeWeight;
        float guideStrokeWeight;
        boolean isCropEnabled;
        int handleColor;
        int guideColor;
        float initialFrameScale;
        float angle;
        boolean isAnimationEnabled;
        int animationDuration;
        int exifRotation;
        Uri sourceUri;
        Uri saveUri;
        Bitmap.CompressFormat compressFormat;
        int compressQuality;
        boolean isDebug;
        int outputMaxWidth;
        int outputMaxHeight;
        int outputWidth;
        int outputHeight;
        boolean isHandleShadowEnabled;
        int inputImageWidth;
        int inputImageHeight;
        int outputImageWidth;
        int outputImageHeight;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            mode = (CropMode) in.readSerializable();
            backgroundColor = in.readInt();
            overlayColor = in.readInt();
            frameColor = in.readInt();
            guideShowMode = (ShowMode) in.readSerializable();
            handleShowMode = (ShowMode) in.readSerializable();
            showGuide = (in.readInt() != 0);
            showHandle = (in.readInt() != 0);
            handleSize = in.readInt();
            touchPadding = in.readInt();
            minFrameSize = in.readFloat();
            customRatioX = in.readFloat();
            customRatioY = in.readFloat();
            frameStrokeWeight = in.readFloat();
            guideStrokeWeight = in.readFloat();
            isCropEnabled = (in.readInt() != 0);
            handleColor = in.readInt();
            guideColor = in.readInt();
            initialFrameScale = in.readFloat();
            angle = in.readFloat();
            isAnimationEnabled = (in.readInt() != 0);
            animationDuration = in.readInt();
            exifRotation = in.readInt();
            sourceUri = in.readParcelable(Uri.class.getClassLoader());
            saveUri = in.readParcelable(Uri.class.getClassLoader());
            compressFormat = (Bitmap.CompressFormat) in.readSerializable();
            compressQuality = in.readInt();
            isDebug = (in.readInt() != 0);
            outputMaxWidth = in.readInt();
            outputMaxHeight = in.readInt();
            outputWidth = in.readInt();
            outputHeight = in.readInt();
            isHandleShadowEnabled = (in.readInt() != 0);
            inputImageWidth = in.readInt();
            inputImageHeight = in.readInt();
            outputImageWidth = in.readInt();
            outputImageHeight = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flag) {
            super.writeToParcel(out, flag);
            out.writeSerializable(mode);
            out.writeInt(backgroundColor);
            out.writeInt(overlayColor);
            out.writeInt(frameColor);
            out.writeSerializable(guideShowMode);
            out.writeSerializable(handleShowMode);
            out.writeInt(showGuide ? 1 : 0);
            out.writeInt(showHandle ? 1 : 0);
            out.writeInt(handleSize);
            out.writeInt(touchPadding);
            out.writeFloat(minFrameSize);
            out.writeFloat(customRatioX);
            out.writeFloat(customRatioY);
            out.writeFloat(frameStrokeWeight);
            out.writeFloat(guideStrokeWeight);
            out.writeInt(isCropEnabled ? 1 : 0);
            out.writeInt(handleColor);
            out.writeInt(guideColor);
            out.writeFloat(initialFrameScale);
            out.writeFloat(angle);
            out.writeInt(isAnimationEnabled ? 1 : 0);
            out.writeInt(animationDuration);
            out.writeInt(exifRotation);
            out.writeParcelable(sourceUri, flag);
            out.writeParcelable(saveUri, flag);
            out.writeSerializable(compressFormat);
            out.writeInt(compressQuality);
            out.writeInt(isDebug ? 1 : 0);
            out.writeInt(outputMaxWidth);
            out.writeInt(outputMaxHeight);
            out.writeInt(outputWidth);
            out.writeInt(outputHeight);
            out.writeInt(isHandleShadowEnabled ? 1 : 0);
            out.writeInt(inputImageWidth);
            out.writeInt(inputImageHeight);
            out.writeInt(outputImageWidth);
            out.writeInt(outputImageHeight);
        }

        public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
            public SavedState createFromParcel(final Parcel inParcel) {
                return new SavedState(inParcel);
            }

            public SavedState[] newArray(final int inSize) {
                return new SavedState[inSize];
            }
        };
    }
}
