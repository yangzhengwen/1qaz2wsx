package com.ocr.ui.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cdjysd.licenseplatelib.utils.Devcode;
import com.cdjysd.licenseplatelib.utils.Utils;
import com.kernal.plateid.PlateCfgParameter;
import com.kernal.plateid.PlateRecogService;
import com.kernal.plateid.PlateRecognitionParameter;
import com.ocr.R;
import com.ocr.ui.view.PlateViewfinderView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class ScenCameraActivity extends Activity implements
        SurfaceHolder.Callback, Camera.PreviewCallback {

    private SeekBar seekBar;
    private int maxZoom;    //seekbar???????????????
    private Camera camera;
    private SurfaceView surfaceView;
    // private TextView resultEditText;
    private Button back_btn, flash_btn, back;
    private PlateViewfinderView myview;
    private RelativeLayout re;
    private int width, height, screenWidth, screenHeight;
    private TimerTask timer;
    private int preWidth = 0;
    private int preHeight = 0;
    private String number = "", color = "", hpzl = "";
    private SurfaceHolder holder;
    private int iInitPlateIDSDK = -1;
    private int nRet = -1;
    private int imageformat = 6;// NV21 -->6
    private int bVertFlip = 0;
    private int bDwordAligned = 1;
    private String[] fieldvalue = new String[14];
    private int rotation = 0;
    private static int tempUiRot = 0;
    private Bitmap bitmap, bitmap1;
    private Vibrator mVibrator;
    private PlateRecognitionParameter prp = new PlateRecognitionParameter();
    private boolean setRecogArgs = true;// ????????????????????????????????????????????????????????????
    private boolean isCamera = true;// ??????????????????????????????????????? true:???????????? false:????????????
    private boolean recogType = true;// ????????????????????????????????????????????????????????? true:???????????? false:????????????
    private byte[] tempData;
    private byte[] picData;
    private Timer time;
    private boolean cameraRecogUtill = false; // cameraRecogUtill
    // true:????????????????????????????????????????????????????????????????????????????????????
    // false:??????????????? ?????????????????? ???????????????????????????
//    private String path;// ?????????????????????/**/
    public PlateRecogService.MyBinder recogBinder;
    private boolean isAutoFocus = true; // ???????????????????????? true:????????????????????? false:?????????
    // ??????????????????????????????
    private boolean sameProportion = false;   //?????????1280*960?????????????????????????????????????????????????????? ??????????????????
    private int initPreWidth = 1920; //
    private int initPreHeight = 1080;//???????????????????????????????????????????????????????????????  ???????????????????????????
    private boolean isFirstIn = true;
    public ServiceConnection recogConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            recogConn = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            recogBinder = (PlateRecogService.MyBinder) service;
            iInitPlateIDSDK = recogBinder.getInitPlateIDSDK();

            if (iInitPlateIDSDK != 0) {
                nRet = iInitPlateIDSDK;
                String[] str = {"" + iInitPlateIDSDK};
                getResult(str);
            }
            // recogBinder.setRecogArgu(recogPicPath, imageformat,
            // bGetVersion, bVertFlip, bDwordAligned);
            PlateCfgParameter cfgparameter = new PlateCfgParameter();
            cfgparameter.armpolice = 4;
            cfgparameter.armpolice2 = 16;
            cfgparameter.embassy = 12;
            cfgparameter.individual = 0;
            cfgparameter.nOCR_Th = 0;
            cfgparameter.nPlateLocate_Th = 5;
            cfgparameter.onlylocation = 15;
            cfgparameter.tworowyellow = 2;
            cfgparameter.tworowarmy = 6;
            cfgparameter.szProvince = "";
            cfgparameter.onlytworowyellow = 11;
            cfgparameter.tractor = 8;
            cfgparameter.bIsNight = 1;
            cfgparameter.newEnergy = 24;
            cfgparameter.consulate = 22;
            cfgparameter.Infactory = 18;
            cfgparameter.civilAviation = 20;
            if (cameraRecogUtill) {
                imageformat = 0;
            }
            recogBinder.setRecogArgu(cfgparameter, imageformat, bVertFlip,
                    bDwordAligned);

            // fieldvalue = recogBinder.doRecog(recogPicPath, width,
            // height);

        }
    };
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            getScreenSize();
            if (msg.what == 5) {
                getPreToChangView(preWidth, preHeight);
            } else {
                re.removeView(myview);
                setRotationAndView(msg.what);
                getPreToChangView(preWidth, preHeight);
                if (rotation == 90 || rotation == 270) {
                    myview = new PlateViewfinderView(ScenCameraActivity.this, width, height, true);
                } else {
                    myview = new PlateViewfinderView(ScenCameraActivity.this, width, height, false);
                }
                re.addView(myview);
                if (camera != null) {
                    camera.setDisplayOrientation(rotation);
                }
            }
            super.handleMessage(msg);
        }
    };

    ScaleGestureDetector gestureDetector;
    SharedPreferences mySharedPreferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int uiRot = getWindowManager().getDefaultDisplay().getRotation();// ???????????????????????????
        requestWindowFeature(Window.FEATURE_NO_TITLE);//
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_scencamera);
        PlateRecogService.initializeType = recogType;
        mySharedPreferences = getSharedPreferences("cameraParameter", Activity.MODE_PRIVATE);
        editor = mySharedPreferences.edit();
        findiew();
        setRotationAndView(uiRot);
        getScreenSize();
        tempUiRot = 0;
    }

    // ????????????????????????????????????
    private void setRotationAndView(int uiRot) {
        setScreenSize(this);
//		System.out.println("????????????" + width + "     ????????????" + height);
        rotation = Utils.setRotation(width, height, uiRot, rotation);
        if (rotation == 90 || rotation == 270) // ???????????????
        {
            setLinearButton();
        } else { // ???????????????
            setHorizontalButton();

        }

    }

    @SuppressLint("NewApi")
    private void findiew() {
        // TODO Auto-generated method stub

        seekBar = ((SeekBar) findViewById(R.id.seekbar));
        seekBar.setProgress(mySharedPreferences.getInt("zoom", 33));
        surfaceView = (SurfaceView) findViewById(R.id.surfaceViwe_video);
        back_btn = (Button) findViewById(R.id.back_camera);
        flash_btn = (Button) findViewById(R.id.flash_camera);
        back = (Button) findViewById(R.id.back);
        re = (RelativeLayout) findViewById(R.id.memory);
        re.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {

            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight,
                                       int oldBottom) {
                if ((bottom != oldBottom && right == oldRight) || (bottom == oldBottom && right != oldRight)) {
                    Message mesg = new Message();
                    mesg.what = 5;
                    handler.sendMessage(mesg);
                }

            }
        });
        // hiddenVirtualButtons(re);
        holder = surfaceView.getHolder();
        holder.addCallback(ScenCameraActivity.this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        // ???????????????????????????????????????????????? ??????????????????????????????ImageView
        // ???????????????????????????

        back_btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                onBackPressed();
            }
        });
        // ???????????????????????????
        back.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub\
                onBackPressed();
            }
        });
        // ?????????????????????
        flash_btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // b = true;
                // TODO Auto-generated method stub
                if (!getPackageManager().hasSystemFeature(
                        PackageManager.FEATURE_CAMERA_FLASH)) {
                    Toast.makeText(
                            ScenCameraActivity.this,
                            getResources().getString(
                                    getResources().getIdentifier("no_flash",
                                            "string", getPackageName())),
                            Toast.LENGTH_LONG).show();
                } else {
                    if (camera != null) {
                        Camera.Parameters parameters = camera.getParameters();
                        String flashMode = parameters.getFlashMode();
                        if (flashMode
                                .equals(Camera.Parameters.FLASH_MODE_TORCH)) {

                            parameters
                                    .setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                            parameters.setExposureCompensation(0);
                        } else {
                            parameters
                                    .setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);// ???????????????
                            parameters.setExposureCompensation(-1);

                        }
                        try {
                            camera.setParameters(parameters);
                        } catch (Exception e) {

                            Toast.makeText(
                                    ScenCameraActivity.this,
                                    getResources().getString(
                                            getResources().getIdentifier(
                                                    "no_flash", "string",
                                                    getPackageName())),
                                    Toast.LENGTH_LONG).show();
                        }
                        camera.startPreview();
                    }
                }
            }

        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double zoomF = 0.0d;
                int zoom = 0;
                if (camera != null) {
                    Camera.Parameters parameters = camera.getParameters();
                    if (parameters.isZoomSupported()) {
                        zoomF = ((double) camera.getParameters().getMaxZoom()) / 100.0d;
                        zoom = (int) (progress * zoomF);
                        editor.putInt("zoom", zoom);
                        editor.commit();
                        parameters.setZoom(zoom);
                        camera.setParameters(parameters);
                        return;
                    }
                    Toast.makeText(ScenCameraActivity.this, "???????????????", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    // ??????????????????????????????
    private void setLinearButton() {
        int back_w;
        int back_h;
        int flash_w;
        int flash_h;
        int Fheight;
        int take_h;
        int take_w;
        RelativeLayout.LayoutParams layoutParams;
        back.setVisibility(View.VISIBLE);
        back_btn.setVisibility(View.GONE);
        back_h = (int) (height * 0.066796875);
        back_w = (int) (back_h * 1);
        layoutParams = new RelativeLayout.LayoutParams(back_w, back_h);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT,
                RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP,
                RelativeLayout.TRUE);

        Fheight = (int) (width * 0.75);
        layoutParams.topMargin = (int) (((height - Fheight * 0.8 * 1.585) / 2 - back_h) / 2);
        layoutParams.leftMargin = (int) (width * 0.10486111111111111111111111111111);
        back.setLayoutParams(layoutParams);

        flash_h = (int) (height * 0.066796875);
        flash_w = (int) (flash_h * 1);
        layoutParams = new RelativeLayout.LayoutParams(flash_w, flash_h);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,
                RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP,
                RelativeLayout.TRUE);

        Fheight = (int) (width * 0.75);
        layoutParams.topMargin = (int) (((height - Fheight * 0.8 * 1.585) / 2 - flash_h) / 2);
        layoutParams.rightMargin = (int) (width * 0.10486111111111111111111111111111);
        flash_btn.setLayoutParams(layoutParams);

//        take_h = (int) (height * 0.105859375);
//        take_w = (int) (take_h * 1);
//        layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL,
//                RelativeLayout.TRUE);
//        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,
//                RelativeLayout.TRUE);
//        layoutParams.bottomMargin = (int) (height * 0.10486111111111111111111111111111);
//
//        take_pic.setLayoutParams(layoutParams);
    }

    // ?????????????????????????????????
    private void setHorizontalButton() {
        int back_w;
        int back_h;
        int flash_w;
        int flash_h;
        int Fheight;
        int take_h;
        int take_w;
        RelativeLayout.LayoutParams layoutParams;
        back_btn.setVisibility(View.VISIBLE);
        back.setVisibility(View.GONE);
        back_w = (int) (width * 0.066796875);
        back_h = (int) (back_w * 1);
        layoutParams = new RelativeLayout.LayoutParams(back_w, back_h);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT,
                RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,
                RelativeLayout.TRUE);
        Fheight = height;

        Fheight = (int) (height * 0.75);
        layoutParams.leftMargin = (int) (((width - Fheight * 0.8 * 1.585) / 2 - back_h) / 2);
        layoutParams.bottomMargin = (int) (height * 0.10486111111111111111111111111111);
        back_btn.setLayoutParams(layoutParams);

        flash_w = (int) (width * 0.066796875);
        flash_h = (int) (flash_w * 1);
        layoutParams = new RelativeLayout.LayoutParams(flash_w, flash_h);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT,
                RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP,
                RelativeLayout.TRUE);

        Fheight = (int) (height * 0.75);
        layoutParams.leftMargin = (int) (((width - Fheight * 0.8 * 1.585) / 2 - back_h) / 2);
        layoutParams.topMargin = (int) (height * 0.10486111111111111111111111111111);
        flash_btn.setLayoutParams(layoutParams);

//        take_h = (int) (width * 0.105859375);
//        take_w = (int) (take_h * 1);
//        layoutParams = new RelativeLayout.LayoutParams(take_w, take_h);
//        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL,
//                RelativeLayout.TRUE);
//        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,
//                RelativeLayout.TRUE);
//
//        layoutParams.rightMargin = (int) (width * 0.10486111111111111111111111111111);
//        take_pic.setLayoutParams(layoutParams);
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        closeCamera();
    }


    int nums = -1;
    private byte[] intentNV21data;

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        // ??????????????????????????????
        int uiRot = getWindowManager().getDefaultDisplay().getRotation();// ???????????????????????????
        if (uiRot != tempUiRot) {
            Message mesg = new Message();
            mesg.what = uiRot;
            handler.sendMessage(mesg);
            tempUiRot = uiRot;
        }
        if (setRecogArgs) {
            Intent authIntent = new Intent(ScenCameraActivity.this,
                    PlateRecogService.class);
            bindService(authIntent, recogConn, Service.BIND_AUTO_CREATE);
            setRecogArgs = false;
        }
        if (iInitPlateIDSDK == 0) {
            prp.height = preHeight;//
            prp.width = preWidth;//
            // ?????????
            prp.devCode = Devcode.DEVCODE;

            if (cameraRecogUtill) {
                // ???????????? ???????????????????????????????????? ??????????????????
                if (isCamera) {

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    options.inPurgeable = true;
                    options.inInputShareable = true;
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21,
                            preWidth, preHeight, null);
                    yuvimage.compressToJpeg(
                            new Rect(0, 0, preWidth, preHeight), 100, baos);
                    bitmap = BitmapFactory.decodeByteArray(baos.toByteArray(),
                            0, baos.size(), options);
                    Matrix matrix = new Matrix();
                    matrix.reset();
                    if (rotation == 90) {
                        matrix.setRotate(90);
                    } else if (rotation == 180) {
                        matrix.setRotate(180);
                    } else if (rotation == 270) {
                        matrix.setRotate(270);
                        //
                    }
                    bitmap1 = Bitmap
                            .createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                                    bitmap.getHeight(), matrix, true);
//                    path = savePicture(bitmap1);
//                    prp.pic = path;
                    fieldvalue = recogBinder.doRecogDetail(prp);
                    nRet = recogBinder.getnRet();
                    if (nRet != 0) {

                        Toast.makeText(this, "????????????", Toast.LENGTH_SHORT).show();
                    } else {

                        number = fieldvalue[0];
//                        color = fieldvalue[1];
                        mVibrator = (Vibrator) getApplication()
                                .getSystemService(Service.VIBRATOR_SERVICE);
                        mVibrator.vibrate(100);
                        closeCamera();
                        // ?????????????????? ??????MemoryResultActivity ??????????????? ?????????????????????
                        Intent intent = new Intent();
                        intent.putExtra("number", number);
//                        intent.putExtra("color", color);
//                        intent.putExtra("path", path);
                        // intent.putExtra("time", fieldvalue[11]);
//                        intent.putExtra("recogType", false);
//                        intent.putExtra("isatuo",true);//???????????????true??????
                        setResult(RESULT_OK, intent);
                        this.finish();
                    }
                }
            } else {
                // System.out.println("?????????????????????");

                prp.picByte = data;
                picData = data;
                if (rotation == 0) {
                    // ??????????????????,????????????????????????????????????
                    prp.plateIDCfg.bRotate = 0;
                    setHorizontalRegion();
                } else if (rotation == 90) {

                    prp.plateIDCfg.bRotate = 1;
                    setLinearRegion();

                } else if (rotation == 180) {
                    prp.plateIDCfg.bRotate = 2;
                    setHorizontalRegion();
                } else if (rotation == 270) {
                    prp.plateIDCfg.bRotate = 3;
                    setLinearRegion();
                }
                if (isCamera) {
                    // ?????????????????? ???????????????

                    fieldvalue = recogBinder.doRecogDetail(prp);

                    nRet = recogBinder.getnRet();

                    if (nRet != 0) {
                        String[] str = {"" + nRet};
                        getResult(str);
                    } else {
                        getResult(fieldvalue);
                        intentNV21data = data;
                    }

                }
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (camera != null) {
            initCamera(holder, initPreWidth, initPreHeight);
            getPreToChangView(preWidth, preHeight);

            if (rotation == 90 || rotation == 270) {
                myview = new PlateViewfinderView(ScenCameraActivity.this, width, height, true);
            } else {
                myview = new PlateViewfinderView(ScenCameraActivity.this, width, height, false);
            }
            re.addView(myview);

        }
    }

    @SuppressLint("InlinedApi")
    static final String[] PERMISSION = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CAMERA};
    private static final int PERMISSION_REQUESTCODE = 1;

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            //????????????
            ActivityCompat.requestPermissions(this, PERMISSION, PERMISSION_REQUESTCODE);
        } else {
            OpenCameraAndSetParameters();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUESTCODE:
                boolean permissionsPASS = true;
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        //??????????????????????????????????????????????????????
                        permissionsPASS = false;
                    }
                }
                if (grantResults.length > 0 && permissionsPASS) {
                    OpenCameraAndSetParameters();
                } else {
                    //?????????????????????
                    System.out.println("???????????????");
                    Toast.makeText(this, "???????????????", Toast.LENGTH_SHORT).show();
                    this.finish();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    // ??????????????????????????????
    private void setHorizontalRegion() {
        this.prp.plateIDCfg.left = preWidth / 4;
        this.prp.plateIDCfg.top = preHeight / 4;
        this.prp.plateIDCfg.right = (preWidth / 4) + (preWidth / 2);
        this.prp.plateIDCfg.bottom = preHeight - (preHeight / 4);
        System.out.println("???  ???" + prp.plateIDCfg.left + "   ???  ???" + prp.plateIDCfg.right + "     ??????" + prp.plateIDCfg.top + "    ??????" + prp.plateIDCfg.bottom);
    }

    // ??????????????????????????????
    private void setLinearRegion() {
        this.prp.plateIDCfg.left = preHeight / 24;
        this.prp.plateIDCfg.top = preWidth / 3;
        this.prp.plateIDCfg.right = (preHeight / 24) + ((preHeight * 11) / 12);
        this.prp.plateIDCfg.bottom = (preWidth / 3) + (preWidth / 3);
    }

    private void initCamera(SurfaceHolder holder, int setPreWidth, int setPreHeight) {
        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> list = parameters.getSupportedPreviewSizes();
        Camera.Size size;

        int previewWidth = 480;
        int previewheight = 640;
        int second_previewWidth = 0;
        int second_previewheight = 0;
        if (list == null) {
            size = parameters.getPreviewSize();
            previewWidth = size.width;
            previewheight = size.height;
        } else if (list.size() == 1) {
            //?????????????????????????????????
            size = list.get(0);
            previewWidth = size.width;
            previewheight = size.height;
        } else {
            Iterator paramPoint = list.iterator();
            Point point = null;
            //???????????????????????????????????????????????????
            do {
                if (!paramPoint.hasNext()) {
                    //?????????????????????????????????????????????????????????????????????
                    point = getCloselyPreSize(parameters, setPreWidth, setPreWidth);
                    break;
                }
                size = (Camera.Size) paramPoint.next();
            } while ((size.width != setPreWidth) || (size.height != setPreHeight));
            if (point != null) {
                previewWidth = point.x;
                previewheight = point.y;
            } else {
                previewWidth = setPreWidth;
                previewheight = setPreHeight;
            }
        }
        preWidth = previewWidth;
        preHeight = previewheight;
        System.out.println("??????????????????" + preWidth + "    " + preHeight);
        parameters.setPictureFormat(PixelFormat.JPEG);
        parameters.setPreviewSize(preWidth, preHeight);
        if (parameters.getSupportedFocusModes().contains(
                parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
                && !isAutoFocus) {
            isAutoFocus = false;
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
            parameters
                    .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else if (parameters.getSupportedFocusModes().contains(
                parameters.FOCUS_MODE_AUTO)) {
            isAutoFocus = true;
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        parameters.setZoom(mySharedPreferences.getInt("zoom", 33));
        camera.setParameters(parameters);
        camera.setDisplayOrientation(rotation);
        try {
            camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        camera.setPreviewCallback(this);  //?????????????????????????????????
        camera.startPreview();

    }

    /**
     * ????????????????????????????????????   ?????????????????????????????????????????????????????????   ??????1920*1080???1280*720 ????????????
     *
     * @param parameters ????????????
     * @param width      ???
     * @param height     ???
     * @return ???????????????
     */
    private Point getCloselyPreSize(Camera.Parameters parameters, int width, int height) {
        int reqtmpwidth = width;
        int reqTmpHeight = height;
        int realWidth = 0, realHeight = 0;
        float reqRatio = (float) reqtmpwidth / (float) reqTmpHeight;
        float deltaRatioMin = 3.4028235E38F;
        List preSizeList = parameters.getSupportedPreviewSizes();
        Camera.Size retSize = null;
        Iterator var13 = preSizeList.iterator();
        Camera.Size defaultSize;
        while (var13.hasNext()) {
            defaultSize = (Camera.Size) var13.next();
            float realRatio = (float) defaultSize.width / (float) defaultSize.height;
            if (reqRatio == realRatio) {
                if (defaultSize.width <= 1920) {
                    if (realWidth <= defaultSize.width) {
                        realWidth = defaultSize.width;
                        realHeight = defaultSize.height;
//                        LogUtil.E(TAG, "???????????????" + realWidth + "   " + realHeight);
                    }
                }

            }

        }
        if (realWidth == 0 || realHeight == 0) {
            while (var13.hasNext()) {
                defaultSize = (Camera.Size) var13.next();
                float curRatio = (float) defaultSize.width / (float) defaultSize.height;
                float deltaRatio = Math.abs(reqRatio - curRatio);
                if (deltaRatio < deltaRatioMin) {
                    deltaRatioMin = deltaRatio;
                    retSize = defaultSize;
                    realWidth = defaultSize.width;
                    realHeight = defaultSize.height;
                }
            }
            if (retSize == null) {
                defaultSize = parameters.getPreviewSize();
                retSize = defaultSize;
//                LogUtil.D(TAG, "?????????????????????????????????????????????: " + defaultSize);
            }
        }
        return new Point(realWidth, realHeight);
    }

    /**
     * @param @param fieldvalue ?????????????????????????????????
     * @return void ????????????
     * @Title: getResult
     * @Description: TODO(????????????)
     * @throwsbyte[]picdata
     */

    private void getResult(String[] fieldvalue) {

        if (nRet != 0)
        // ??????????????? ????????????????????????
        {
            Toast.makeText(this, "????????????", Toast.LENGTH_SHORT).show();
        } else {
            // ???????????? ??????????????????
            String result = "";
            String[] resultString;
            String timeString = "";
            String boolString = "";
            boolString = fieldvalue[0];

            if (boolString != null && !boolString.equals(""))
            // ????????????????????????????????????
            {

                resultString = boolString.split(";");
                int lenght = resultString.length;
                // Log.e("DEBUG", "nConfidence:" +
                // fieldvalue[4]);
                if (lenght > 0) {

                    String[] strarray = fieldvalue[4].split(";");

                    // ??????????????? ?????????????????????????????????75

                    if (recogType ? true : Integer.valueOf(strarray[0]) > 75) {

                        tempData = recogBinder.getRecogData();

                        if (tempData != null) {

                            if (lenght == 1) {

                                if (fieldvalue[11] != null
                                        && !fieldvalue[11].equals("")) {
                                    int time = Integer.parseInt(fieldvalue[11]);
                                    time = time / 1000;
                                    timeString = "" + time;
                                } else {
                                    timeString = "null";
                                }

//								if (null != fieldname) {

                                BitmapFactory.Options options = new BitmapFactory.Options();
                                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                                options.inPurgeable = true;
                                options.inInputShareable = true;
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                                int Height = 0, Width = 0;
                                if (rotation == 90 || rotation == 270) {
                                    Height = preWidth;
                                    Width = preHeight;
                                } else if (rotation == 180 || rotation == 0) {
                                    Height = preHeight;
                                    Width = preWidth;
                                }
                                YuvImage yuvimage = new YuvImage(tempData,
                                        ImageFormat.NV21, Width, Height,
                                        null);
                                yuvimage.compressToJpeg(new Rect(0, 0,
                                        Width, Height), 100, baos);

                                bitmap = BitmapFactory.decodeByteArray(
                                        baos.toByteArray(), 0, baos.size(),
                                        options);

                                bitmap1 = Bitmap.createBitmap(bitmap, 0, 0,
                                        bitmap.getWidth(),
                                        bitmap.getHeight(), null, true);
//                                path = savePicture(bitmap1);

                                mVibrator = (Vibrator) getApplication()
                                        .getSystemService(
                                                Service.VIBRATOR_SERVICE);
                                mVibrator.vibrate(100);
                                closeCamera();
                                ///////????????????????????????
                                Intent intent = new Intent();
                                number = fieldvalue[0];
                                this.hpzl = getHpzl(fieldvalue[3]);
                                if ((this.hpzl.equals("44")) && (fieldvalue[1].equals("???"))) {
                                    this.hpzl = "01";
                                }
                                if ((this.hpzl.equals("44")) && (fieldvalue[1].equals("???"))) {
                                    this.hpzl = "02";
                                }
                                if ((this.hpzl.equals("44")) && (fieldvalue[1].equals("???"))) {
                                    this.hpzl = "52";
                                }
                                if ((this.hpzl.equals("44")) && (fieldvalue[1].equals("??????"))) {
                                    this.hpzl = "51";
                                }
                                if (this.number.contains("???")) {
                                    this.hpzl = "03";
                                } else if (this.number.contains("???")) {
                                    this.hpzl = "04";
                                } else if (this.number.contains("???")) {
                                    this.hpzl = "15";
                                } else if (this.number.contains("???")) {
                                    this.hpzl = "16";
                                } else if (this.number.contains("???")) {
                                    this.hpzl = "26";
                                } else if (this.number.contains("???")) {
                                    this.hpzl = "27";
                                } else if (this.number.contains("???")) {
                                    this.hpzl = "32";
                                }

                                this.color = getHpColor(fieldvalue[1], fieldvalue[3]);

                                intent.putExtra("number", number);
                                intent.putExtra("hpzl", hpzl);
                                intent.putExtra("color", color);
                                setResult(RESULT_OK, intent);
                                this.finish();


                            } else {
                                String itemString = "";

                                mVibrator = (Vibrator) getApplication()
                                        .getSystemService(
                                                Service.VIBRATOR_SERVICE);
                                mVibrator.vibrate(100);
                                closeCamera();
                                Intent intent = new Intent();
                                for (int i = 0; i < lenght; i++) {

                                    itemString = fieldvalue[0];
                                    resultString = itemString.split(";");
                                    number += resultString[i] + ";\n";

                                    itemString = fieldvalue[1];
                                    color += resultString[i] + ";\n";
                                    itemString = fieldvalue[11];
                                    resultString = itemString.split(";");

                                }

                                intent.putExtra("number", number);
                                setResult(RESULT_OK, intent);
                                this.finish();

                            }
                        }
                    }

                }

            } else
            // ???????????????????????????????????????
            {
                if (!recogType)
                // ?????????????????????????????? ?????????????????? ???????????????????????????
                {
                    ;
                    if (picData != null) {

                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                        options.inPurgeable = true;
                        options.inInputShareable = true;
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        YuvImage yuvimage = new YuvImage(picData,
                                ImageFormat.NV21, preWidth, preHeight, null);
                        yuvimage.compressToJpeg(new Rect(0, 0, preWidth,
                                preHeight), 100, baos);
                        bitmap = BitmapFactory.decodeByteArray(
                                baos.toByteArray(), 0, baos.size(), options);

                        Matrix matrix = new Matrix();
                        matrix.reset();
                        if (rotation == 90) {
                            matrix.setRotate(90);
                        } else if (rotation == 180) {
                            matrix.setRotate(180);
                        } else if (rotation == 270) {
                            matrix.setRotate(270);
                            //
                        }
                        bitmap1 = Bitmap.createBitmap(bitmap, 0, 0,
                                bitmap.getWidth(), bitmap.getHeight(), matrix,
                                true);
//                        path = savePicture(bitmap1);

                        if (fieldvalue[11] != null
                                && !fieldvalue[11].equals("")) {
                            int time = Integer.parseInt(fieldvalue[11]);
                            time = time / 1000;
                            timeString = "" + time;
                        } else {
                            timeString = "null";
                        }

//						if (null != fieldname) {
                        mVibrator = (Vibrator) getApplication()
                                .getSystemService(Service.VIBRATOR_SERVICE);
                        mVibrator.vibrate(100);
                        closeCamera();
                        Intent intent = new Intent();
                        number = fieldvalue[0];
//                        color = fieldvalue[1];
                        if (fieldvalue[0] == null) {
                            number = "null";
                        }
//                        if (fieldvalue[1] == null) {
//                            color = "null";
//                        }
//                        int left = prp.plateIDCfg.left;
//                        int top = prp.plateIDCfg.top;
//                        int w = prp.plateIDCfg.right - prp.plateIDCfg.left;
//                        int h = prp.plateIDCfg.bottom - prp.plateIDCfg.top;

                        intent.putExtra("number", number);
//                        intent.putExtra("color", color);
////                        intent.putExtra("path", path);
//                        intent.putExtra("left", left);
//                        intent.putExtra("top", top);
//                        intent.putExtra("width", w);
//                        intent.putExtra("height", h);
//                        intent.putExtra("time", fieldvalue[11]);
//                        intent.putExtra("recogType", recogType);
//                        intent.putExtra("isatuo", true);//???????????????true??????
                        setResult(RESULT_OK, intent);
                        this.finish();
//						}
                    }
                }
            }
        }

        nRet = -1;
        fieldvalue = null;
    }

    private String getHpColor(String color, String prehpzl) {
        String hpzlc = getHpzl(prehpzl);
        if ((hpzlc.equals("44")) && (color.equals("???"))) {
            return "0";
        }
        if ((hpzlc.equals("44")) && (color.equals("???"))) {
            return "1";
        }
        if ((hpzlc.equals("44")) && (color.equals("???"))) {
            return "5"; ////??????????????????
        }
        if ((hpzlc.equals("44")) && (color.equals("??????"))) {
            return "5"; //??????????????????
        }
        if ("1".equals(prehpzl)) {//???????????????
            return "1";
        }
        if ("3".equals(prehpzl)) {//???????????????
            return "0";
        }
        if ("4".equals(prehpzl)) {//???????????????
            return "0";
        }
        if ("5".equals(prehpzl)) {//???????????????
            return "4";
        }
        if ("6".equals(prehpzl)) {//????????????
            return "4";
        }
        if ("7".equals(prehpzl)) {//????????????
            return "-1";
        }
        if ("8".equals(prehpzl)) {//????????????
            return "4";
        }
        if ("9".equals(prehpzl)) {//????????????
            return "4";
        }
        if ("10".equals(prehpzl)) {//???????????????
            return "3";
        }
        if ("11".equals(prehpzl)) {//???????????????
            return "3";
        }
        if ("12".equals(prehpzl)) {//???????????????
            return "2";
        }
        if ("17".equals(prehpzl)) {//???????????????
            return "5";
        }
        if ("18".equals(prehpzl)) {//???????????????
            return "5";
        }
        return "-1";

    }


    private String getHpzl(String paramString) {
        if ("0".equals(paramString)) {
            return "44";
        }
        if ("1".equals(paramString)) {//???????????????
            return "02";
        }
        if ("2".equals(paramString)) {
            return "44";
        }
        if ("3".equals(paramString)) {//???????????????
            return "01";
        }
        if ("4".equals(paramString)) {//???????????????
            return "01";
        }
        if ("5".equals(paramString)) {//???????????????
            return "23";
        }
        if ("6".equals(paramString)) {//????????????
            return "31";
        }
        if ("7".equals(paramString)) {//????????????
            return "99";
        }
        if ("8".equals(paramString)) {//????????????
            return "32";
        }
        if ("9".equals(paramString)) {//????????????
            return "32";
        }
        if ("10".equals(paramString)) {//???????????????
            return "03";
        }
        if ("11".equals(paramString)) {//???????????????
            return "26";
        }
        if ("12".equals(paramString)) {//???????????????
            return "25";
        }
        if ("17".equals(paramString)) {//???????????????
            return "52";
        }
        if ("18".equals(paramString)) {//???????????????
            return "51";
        }
        return "99";
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        if (bitmap != null) {
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
                bitmap = null;
            }

        }
        if (bitmap1 != null) {
            if (!bitmap1.isRecycled()) {
                bitmap1.recycle();
                bitmap1 = null;
            }

        }

        if (mVibrator != null) {
            mVibrator.cancel();
        }
        if (recogBinder != null) {
            unbindService(recogConn);
            recogBinder = null;
        }
    }

    /**
     * @return void ????????????
     * @throws
     * @Title: closeCamera
     * @Description: TODO(?????????????????????????????????????????????) ????????????
     */
    private void closeCamera() {
        // TODO Auto-generated method stub
        System.out.println("???????????? ");
        synchronized (this) {
            try {
                if (timer != null) {
                    timer.cancel();
                    timer = null;
                }
                if (time != null) {
                    time.cancel();
                    time = null;
                }
                if (camera != null) {
                    camera.setPreviewCallback(null);
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                }

            } catch (Exception e) {

            }
        }
    }


//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if (keyCode == KeyEvent.KEYCODE_BACK) {
//            closeCamera();
//            finish();
//        }
//        return super.onKeyDown(keyCode, event);
//    }

//    public String savePicture(Bitmap bitmap) {
//        String strCaptureFilePath = PATH + "plateID_" + pictureName() + ".jpg";
//        File dir = new File(PATH);
//        if (!dir.exists()) {
//            dir.mkdirs();
//        }
//        File file = new File(strCaptureFilePath);
//        if (file.exists()) {
//            file.delete();
//        }
//        try {
//            file.createNewFile();
//            BufferedOutputStream bos = new BufferedOutputStream(
//                    new FileOutputStream(file));
//
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
//            bos.flush();
//            bos.close();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return strCaptureFilePath;
//    }

    public String pictureName() {
        String str = "";
        Time t = new Time();
        t.setToNow(); // ?????????????????????
        int year = t.year;
        int month = t.month + 1;
        int date = t.monthDay;
        int hour = t.hour; // 0-23
        int minute = t.minute;
        int second = t.second;
        if (month < 10)
            str = String.valueOf(year) + "0" + String.valueOf(month);
        else {
            str = String.valueOf(year) + String.valueOf(month);
        }
        if (date < 10)
            str = str + "0" + String.valueOf(date + "_");
        else {
            str = str + String.valueOf(date + "_");
        }
        if (hour < 10)
            str = str + "0" + String.valueOf(hour);
        else {
            str = str + String.valueOf(hour);
        }
        if (minute < 10)
            str = str + "0" + String.valueOf(minute);
        else {
            str = str + String.valueOf(minute);
        }
        if (second < 10)
            str = str + "0" + String.valueOf(second);
        else {
            str = str + String.valueOf(second);
        }
        return str;
    }

    /**
     * @param @param context ????????????
     * @return void ????????????
     * @throws
     * @Title: setScreenSize
     * @Description: ?????????????????????????????????????????????) ??????????????????????????????????????????????????????
     */
    @SuppressLint("NewApi")
    private void setScreenSize(Context context) {
        int x, y;
        WindowManager wm = ((WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE));
        Display display = wm.getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            Point screenSize = new Point();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                display.getRealSize(screenSize);
                x = screenSize.x;
                y = screenSize.y;
            } else {
                display.getSize(screenSize);
                x = screenSize.x;
                y = screenSize.y;
            }
        } else {
            x = display.getWidth();
            y = display.getHeight();
        }
        width = x;
        height = y;
    }

    //??????????????????
    public void getScreenSize() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
    }

    //???????????????????????????????????????  ???surfaceView??????????????????????????????????????????????????????
    public void getPreToChangView(int preWidth, int preHeight) {
        //?????????
        if (width >= height) {
//			if(preWidth*screenHeight<preHeight*screenWidth){
//					//????????????
//				int tempValue=screenHeight*preWidth/preHeight;
//					LayoutParams layoutParams= new LayoutParams(tempValue, RelativeLayout.TRUE);
//					layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
//					surfaceView.setLayoutParams(layoutParams);
//			 }else if(preWidth*screenHeight>preHeight*screenWidth){//????????????
//					int tempValue=screenWidth*preHeight/preWidth;
//				 LayoutParams layoutParams= new LayoutParams(RelativeLayout.TRUE, tempValue);
//				 layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
//					surfaceView.setLayoutParams(layoutParams);
//			 }else if(preWidth*screenHeight==preHeight*screenWidth){
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.TRUE, RelativeLayout.TRUE);
            surfaceView.setLayoutParams(layoutParams);
//			 }
        }
        //?????????
        if (height >= width) {
//			if(preWidth*screenWidth<preHeight*screenHeight){//????????????
//				int tempValue=screenWidth*preWidth/preHeight;
//					LayoutParams layoutParams= new LayoutParams(RelativeLayout.TRUE,tempValue);
//					layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
//					surfaceView.setLayoutParams(layoutParams);
//				}else if(preWidth*screenWidth>preHeight*screenHeight){//????????????
//					int tempValue=screenHeight*preHeight/preWidth;
//					LayoutParams layoutParams= new LayoutParams(tempValue, RelativeLayout.TRUE);
//					 layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
//					surfaceView.setLayoutParams(layoutParams);
//				}else if(preWidth*screenWidth==preHeight*screenHeight){
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.TRUE, RelativeLayout.TRUE);
            surfaceView.setLayoutParams(layoutParams);
//				}
        }
    }

    public void OpenCameraAndSetParameters() {
        try {
            if (null == camera) {
                camera = Camera.open();
                gestureDetector = new ScaleGestureDetector(this, new ScaleGestureListener());
//                surfaceView.setCamera(camera);
            }
            if (timer == null) {
                timer = new TimerTask() {
                    @Override
                    public void run() {
                        // isSuccess=false;
                        if (camera != null) {
                            try {
                                camera.autoFocus(new Camera.AutoFocusCallback() {
                                    @Override
                                    public void onAutoFocus(boolean success,
                                                            Camera camera) {
                                        // isSuccess=success;

                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    ;
                };
            }
            time = new Timer();
            time.schedule(timer, 500, 2500);
            if (!isFirstIn) {
                initCamera(holder, initPreWidth, initPreHeight);
            }
            isFirstIn = false;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private float mLastTouchX;
    private float mLastTouchY;
    /**
     * ??????????????????
     */
    private static final int ZOOM_OUT = 0;
    /**
     * ??????????????????
     */
    private static final int ZOOM_IN = 1;

    //??????onTouchEvent?????? ????????????
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //????????????
        gestureDetector.onTouchEvent(event);
        final int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                mLastTouchX = event.getX();
                mLastTouchY = event.getY();
                break;
            }
            case MotionEvent.ACTION_UP: {
                break;
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                break;
            }
        }
        return true;
    }

    //?????????
    class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        int mScaleFactor;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor = (int) detector.getScaleFactor();
            Camera.Parameters params = camera.getParameters();
            int zoom = params.getZoom();
            if (mScaleFactor == ZOOM_IN) {
                if (zoom < params.getMaxZoom())
                    zoom += 1;
            } else if (mScaleFactor == ZOOM_OUT) {
                if (zoom > 0)
                    zoom -= 1;
            }
            params.setZoom(zoom);
            camera.setParameters(params);
            return false;
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        closeCamera();
//        Intent intent = new Intent(MemoryCameraActivity.this,
//                MainActivity.class);
//        startActivity(intent);
        finish();

    }
}
