package com.unitecker.app.editor.editimage;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.appcompat.app.AlertDialog;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.unitecker.app.editor.BaseActivity;
import com.unitecker.app.R;
import com.unitecker.app.editor.editimage.fragment.AddTextFragment;
import com.unitecker.app.editor.editimage.fragment.CropFragment;
import com.unitecker.app.editor.editimage.fragment.MainMenuFragment;
import com.unitecker.app.editor.editimage.fragment.PaintFragment;
import com.unitecker.app.editor.editimage.fragment.RotateFragment;
import com.unitecker.app.editor.editimage.fragment.StickerFragment;
import com.unitecker.app.editor.editimage.utils.BitmapUtils;
import com.unitecker.app.editor.editimage.utils.FileUtil;
import com.unitecker.app.editor.editimage.view.CropImageView;
import com.unitecker.app.editor.editimage.view.CustomPaintView;
import com.unitecker.app.editor.editimage.view.CustomViewPager;
import com.unitecker.app.editor.editimage.view.RotateImageView;
import com.unitecker.app.editor.editimage.view.StickerView;
import com.unitecker.app.editor.editimage.view.TextStickerView;
import com.unitecker.app.editor.editimage.view.imagezoom.ImageViewTouch;
import com.unitecker.app.editor.editimage.view.imagezoom.ImageViewTouchBase;
import com.unitecker.app.editor.editimage.widget.RedoUndoController;


public class EditImageActivity extends BaseActivity {
    public static final String FILE_PATH = "file_path";
    public static final String EXTRA_OUTPUT = "extra_output";
    public static final String SAVE_FILE_PATH = "save_file_path";

    public static final String IMAGE_IS_EDIT = "image_is_edit";

    public static final int MODE_NONE = 0;
    public static final int MODE_STICKERS = 1;// ????????????
    public static final int MODE_FILTER = 2;// ????????????
    public static final int MODE_CROP = 3;// ????????????
    public static final int MODE_ROTATE = 4;// ????????????
    public static final int MODE_TEXT = 5;// ????????????
    public static final int MODE_PAINT = 6;//????????????
    public static final int MODE_BEAUTY = 7;//????????????

    public String filePath;// ????????????????????????
    public String saveFilePath;// ????????????????????????
    private int imageWidth, imageHeight;// ?????????????????? ??? ???
    private LoadImageTask mLoadImageTask;

    public int mode = MODE_NONE;// ??????????????????

    protected int mOpTimes = 0;
    protected boolean isBeenSaved = false;

    private EditImageActivity mContext;
    private Bitmap mainBitmap;// ????????????Bitmap
    public ImageViewTouch mainImage;
    private View backBtn;

    public ViewFlipper bannerFlipper;
    private View applyBtn;// ????????????
    private View saveBtn;// ????????????

    public StickerView mStickerView;// ?????????View
    public CropImageView mCropPanel;// ??????????????????
    public RotateImageView mRotatePanel;// ??????????????????
    public TextStickerView mTextStickerView;//??????????????????View
    public CustomPaintView mPaintView;//??????????????????

    public CustomViewPager bottomGallery;// ??????gallery
    private BottomGalleryAdapter mBottomGalleryAdapter;// ??????gallery
    private MainMenuFragment mMainMenuFragment;// Menu
    public StickerFragment mStickerFragment;// ??????Fragment
    public CropFragment mCropFragment;// ????????????Fragment
    public RotateFragment mRotateFragment;// ????????????Fragment
    public AddTextFragment mAddTextFragment;//??????????????????
    public PaintFragment mPaintFragment;//????????????Fragment
    private SaveImageTask mSaveImageTask;

    private RedoUndoController mRedoUndoController;//????????????

    /**
     * @param context
     * @param editImagePath
     * @param outputPath
     * @param requestCode
     */
    public static void start(Activity context, final String editImagePath, final String outputPath, final int requestCode) {
        if (TextUtils.isEmpty(editImagePath)) {
            Toast.makeText(context, R.string.no_choose, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent it = new Intent(context, EditImageActivity.class);
        it.putExtra(EditImageActivity.FILE_PATH, editImagePath);
        it.putExtra(EditImageActivity.EXTRA_OUTPUT, outputPath);
        context.startActivityForResult(it, requestCode);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkInitImageLoader();
        setContentView(R.layout.activity_image_edit);
        initView();
        getData();
    }

    private void getData() {
        filePath = getIntent().getStringExtra(FILE_PATH);
        saveFilePath = getIntent().getStringExtra(EXTRA_OUTPUT);// ??????????????????
        loadImage(filePath);
    }

    private void initView() {
        mContext = this;
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        imageWidth = metrics.widthPixels / 2;
        imageHeight = metrics.heightPixels / 2;

        bannerFlipper = (ViewFlipper) findViewById(R.id.banner_flipper);
        bannerFlipper.setInAnimation(this, R.anim.in_bottom_to_top);
        bannerFlipper.setOutAnimation(this, R.anim.out_bottom_to_top);
        applyBtn = findViewById(R.id.apply);
        applyBtn.setOnClickListener(new ApplyBtnClick());
        saveBtn = findViewById(R.id.save_btn);
        saveBtn.setOnClickListener(new SaveBtnClick());

        mainImage = (ImageViewTouch) findViewById(R.id.main_image);
        backBtn = findViewById(R.id.back_btn);// ????????????
        backBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        mStickerView = (StickerView) findViewById(R.id.sticker_panel);
        mCropPanel = (CropImageView) findViewById(R.id.crop_panel);
        mRotatePanel = (RotateImageView) findViewById(R.id.rotate_panel);
        mTextStickerView = (TextStickerView) findViewById(R.id.text_sticker_panel);
        mPaintView = (CustomPaintView) findViewById(R.id.custom_paint_view);

        // ??????gallery
        bottomGallery = (CustomViewPager) findViewById(R.id.bottom_gallery);
        //bottomGallery.setOffscreenPageLimit(7);
        mMainMenuFragment = MainMenuFragment.newInstance();
        mBottomGalleryAdapter = new BottomGalleryAdapter(
                this.getSupportFragmentManager());
        mStickerFragment = StickerFragment.newInstance();
        mCropFragment = CropFragment.newInstance();
        mRotateFragment = RotateFragment.newInstance();
        mAddTextFragment = AddTextFragment.newInstance();
        mPaintFragment = PaintFragment.newInstance();

        bottomGallery.setAdapter(mBottomGalleryAdapter);


        mainImage.setFlingListener(new ImageViewTouch.OnImageFlingListener() {
            @Override
            public void onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                //System.out.println(e1.getAction() + " " + e2.getAction() + " " + velocityX + "  " + velocityY);
                if (velocityY > 1) {
                    closeInputMethod();
                }
            }
        });

        mRedoUndoController = new RedoUndoController(this, findViewById(R.id.redo_uodo_panel));
    }

    /**
     * ???????????????
     */
    private void closeInputMethod() {
        if (mAddTextFragment.isAdded()) {
            mAddTextFragment.hideInput();
        }
    }


    private final class BottomGalleryAdapter extends FragmentPagerAdapter {
        public BottomGalleryAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int index) {
            // System.out.println("createFragment-->"+index);
            switch (index) {
                case MainMenuFragment.INDEX:// ?????????
                    return mMainMenuFragment;
                case StickerFragment.INDEX:// ??????
                    return mStickerFragment;

                case CropFragment.INDEX://??????
                    return mCropFragment;
                case RotateFragment.INDEX://??????
                    return mRotateFragment;
                case AddTextFragment.INDEX://????????????
                    return mAddTextFragment;
                case PaintFragment.INDEX:
                    return mPaintFragment;//??????

            }//end switch
            return MainMenuFragment.newInstance();
        }

        @Override
        public int getCount() {
            return 8;
        }
    }// end inner class

    /**
     * ????????????????????????
     *
     * @param filepath
     */
    public void loadImage(String filepath) {
        if (mLoadImageTask != null) {
            mLoadImageTask.cancel(true);
        }
        mLoadImageTask = new LoadImageTask();
        mLoadImageTask.execute(filepath);
    }

    /**
     * ????????????????????????
     */
    private final class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... params) {
            return BitmapUtils.getSampledBitmap(params[0], imageWidth,
                    imageHeight);
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            changeMainBitmap(result, false);
        }
    }// end inner class

    @Override
    public void onBackPressed() {
        switch (mode) {
            case MODE_STICKERS:
                mStickerFragment.backToMain();
                return;

            case MODE_CROP:// ??????????????????
                mCropFragment.backToMain();
                return;
            case MODE_ROTATE:// ??????????????????
                mRotateFragment.backToMain();
                return;
            case MODE_TEXT:
                mAddTextFragment.backToMain();
                return;
            case MODE_PAINT:
                mPaintFragment.backToMain();
                return;


        }// end switch

        if (canAutoExit()) {
            onSaveTaskDone();
        } else {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage(R.string.exit_without_save)
                    .setCancelable(false).setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    mContext.finish();
                }
            }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
    }


    private final class ApplyBtnClick implements OnClickListener {
        @Override
        public void onClick(View v) {
            switch (mode) {
                case MODE_STICKERS:
                    mStickerFragment.applyStickers();// ????????????
                    break;

                case MODE_CROP:// ??????????????????
                    mCropFragment.applyCropImage();
                    break;
                case MODE_ROTATE:// ??????????????????
                    mRotateFragment.applyRotateImage();
                    break;
                case MODE_TEXT://???????????? ????????????
                    mAddTextFragment.applyTextImage();
                    break;
                case MODE_PAINT://????????????
                    mPaintFragment.savePaintImage();
                    break;

                default:
                    break;
            }// end switch
        }
    }// end inner class


    private final class SaveBtnClick implements OnClickListener {
        @Override
        public void onClick(View v) {
            if (mOpTimes == 0) {//??????????????????
                onSaveTaskDone();
            } else {
                doSaveImage();
            }
        }
    }// end inner class

    protected void doSaveImage() {
        if (mOpTimes <= 0)
            return;

        if (mSaveImageTask != null) {
            mSaveImageTask.cancel(true);
        }

        mSaveImageTask = new SaveImageTask();
        mSaveImageTask.execute(mainBitmap);
    }

    /**
     * @param newBit
     * @param needPushUndoStack
     */
    public void changeMainBitmap(Bitmap newBit, boolean needPushUndoStack) {
        if (newBit == null)
            return;

        if (mainBitmap == null || mainBitmap != newBit) {
            if (needPushUndoStack) {
                mRedoUndoController.switchMainBit(mainBitmap,newBit);
                increaseOpTimes();
            }
            mainBitmap = newBit;
            mainImage.setImageBitmap(mainBitmap);
            mainImage.setDisplayType(ImageViewTouchBase.DisplayType.FIT_TO_SCREEN);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLoadImageTask != null) {
            mLoadImageTask.cancel(true);
        }

        if (mSaveImageTask != null) {
            mSaveImageTask.cancel(true);
        }

        if (mRedoUndoController != null) {
            mRedoUndoController.onDestroy();
        }
    }

    public void increaseOpTimes() {
        mOpTimes++;
        isBeenSaved = false;
    }

    public void resetOpTimes() {
        isBeenSaved = true;
    }

    public boolean canAutoExit() {
        return isBeenSaved || mOpTimes == 0;
    }

    protected void onSaveTaskDone() {

      Intent returnIntent = new Intent();
        returnIntent.putExtra(FILE_PATH, filePath);
        returnIntent.putExtra(EXTRA_OUTPUT, saveFilePath);
        returnIntent.putExtra(IMAGE_IS_EDIT, mOpTimes > 0);

        FileUtil.ablumUpdate(this, saveFilePath);
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    /**
     * ????????????
     * ???????????????
     */
    private final class SaveImageTask extends AsyncTask<Bitmap, Void, Boolean> {
        private Dialog dialog;

        @Override
        protected Boolean doInBackground(Bitmap... params) {
            if (TextUtils.isEmpty(saveFilePath))
                return false;

            return BitmapUtils.saveBitmap(params[0], saveFilePath);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            dialog.dismiss();
        }

        @Override
        protected void onCancelled(Boolean result) {
            super.onCancelled(result);
            dialog.dismiss();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = EditImageActivity.getLoadingDialog(mContext, R.string.saving_image, false);
            dialog.show();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            dialog.dismiss();

            if (result) {
                resetOpTimes();
                onSaveTaskDone();
            } else {
                Toast.makeText(mContext, R.string.save_error, Toast.LENGTH_SHORT).show();
            }
        }
    }//end inner class

    public Bitmap getMainBit() {
        return mainBitmap;
    }

}// end class
