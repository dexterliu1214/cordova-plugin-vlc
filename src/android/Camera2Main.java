package itri.icl.k400.vlcid.app;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;

import itri.icl.k400.vlcid.libAns.VlcIdAns;
import itri.icl.k400.vlcid.libAns.VlcIdRec;
import itri.icl.k400.vlcid.libDecoder.Decoder;

import android.content.Context;
import android.content.res.Resources;

public class Camera2Main extends AppCompatActivity {
    public class DebugDef {
        public static final boolean printDebug = true;
        public static final boolean showDebugBtn = true;
        public static final boolean showDebugBar = false;
        public static final boolean traniningService = false;
    }

    class MyResource {
        Context app = null;

        public MyResource(Context context) {
            app = context;
        }

        public int get(String name, String type) {
            String package_name = app.getPackageName();
            Resources resources = app.getResources();
            return resources.getIdentifier(name, type, package_name);
        }

        public int layout(String name) {
            return get(name, "layout");
        }

        public int id(String name) {
            return get(name, "id");
        }
    }

    private MyResource R = null;
    private static final String TAG = "Cam2Main-->";

    public boolean mAlreadLauchBackgrandService = false;

    private Button vlcIdButton;
    private Button DebugButton;
    private Button SaveButton;
    private Button BitSizeButton;

    private boolean mIsDebug = false;
    private boolean mSave = false;
    public static boolean mBitSizeTraining = false;

    private TextureView textureView;

    private SurfaceView scanPreview = null;
    private RelativeLayout scanContainer;
    private RelativeLayout scanCropView;
    private ImageView scanLine;

    protected int mPreviewCount;
    private static final int SwitchingModeCount = 1;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    protected CameraDevice mCameraDevice;
    protected CameraCaptureSession mCameraCaptureSession;
    protected CaptureRequest.Builder captureRequestBuilder;

    private ImageReader mImageReader;

    private static final int REQUEST_CAMERA_PERMISSION = 200;

    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private Handler mainHandler;

    private AsyncTask mDecodeTask;
    Vibrator vibrator;
    private static final int VIBRATE_DURATION = 500;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("TAG", "onDestroy");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("TAG", "onStop");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("TAG", "onCreate");

        R = new MyResource(getApplicationContext());

        vibrator = (Vibrator) (this.getSystemService(Context.VIBRATOR_SERVICE));
        mainHandler = new Handler(getMainLooper());

        /********************
         * setRequestedOrientation 參數說明 ******************** 系統預設
         * ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED 鎖定直式
         * ActivityInfo.SCREEN_ORIENTATION_PORTRAIT 鎖定橫式
         * ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE 隨使用者當下
         * ActivityInfo.SCREEN_ORIENTATION_USER 與活動線程下相同的設定
         * ActivityInfo.SCREEN_ORIENTATION_BEHIND 不隨SENSOR改變
         * ActivityInfo.SCREEN_ORIENTATION_NOSENSOR 隨SENSOR改變
         * ActivityInfo.SCREEN_ORIENTATION_SENSOR
         ************************************************************************/
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout("activity_camera2"));

        //---- Animate Window begin
        Window window = getWindow();

        scanContainer = (RelativeLayout) findViewById(R.id("capture_container1"));
        scanCropView = (RelativeLayout) findViewById(R.id("capture_crop_view1"));
        scanLine = (ImageView) findViewById(R.id("capture_scan_line1"));
        //---- Animate Window end

        setupActionBar();

        // Web Content Test
        loadPic = new DownloadWebPicture();
        // Main Thread: 繪製完成網路下載的圖片即更新UI
        myHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                case 1:
                    // Intent i = new Intent();
                    // i.setClass(Camera2Main.this, WebContent.class);
                    // startActivity(i);
                    //Camera2Main.this.finish();
                    break;
                }
                super.handleMessage(msg);
            }
        };

        textureView = (TextureView) findViewById(R.id("texture"));
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);

        if (DebugDef.showDebugBar == false) {
            View view = findViewById(R.id("trainingBar"));
            view.setVisibility(View.GONE);
        } else {
            vlcIdButton = (Button) findViewById(R.id("btn_vlcId"));
            DebugButton = (Button) findViewById(R.id("btn_debug"));
            SaveButton = (Button) findViewById(R.id("btn_Save"));
            BitSizeButton = (Button) findViewById(R.id("btn_BitSize"));

            if (DebugDef.showDebugBtn == false) {
                DebugButton.setVisibility(View.GONE);
                mIsDebug = false;
            }

            DebugButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mIsDebug = !mIsDebug;
                    if (mIsDebug == true) {
                        try {
                            //stop preview
                            mCameraCaptureSession.stopRepeating();
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        } catch (Exception err) {
                            err.printStackTrace();
                        }
                        // ((Button) v).setText(R.string.DeubgTrue);
                    } else {
                        //start preview
                        updatePreview();
                        // ((Button) v).setText(R.string.DeubgFalse);
                    }
                }
            });

            SaveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSave = true;
                }
            });

            BitSizeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    // Intent newAct = new Intent();
                    // newAct.setClass(Camera2Main.this, BitSizeTraining.class);

                    // startActivity(newAct);

                    // Camera2Main.this.finish();
                }
            });
        }
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            // open your camera here
            Log.e(TAG, "texture Avalible --------------------");

            TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.9f);
            animation.setDuration(4500);
            animation.setRepeatCount(-1);
            animation.setRepeatMode(Animation.RESTART);
            scanLine.startAnimation(animation);

            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width
            // and height
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    private final CameraDevice.StateCallback openCameraStateCallBack = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            // This is called when the camera is open
            Log.e(TAG, "onOpened");
            mCameraDevice = camera;
            initCaptureSession();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            mCameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    };
    private Surface mSurface;

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    private void buildDecode(CaptureRequest.Builder builder) {
        try {
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
            builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
            //builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
            builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, (long) mExposureLower);
            builder.set(CaptureRequest.SENSOR_SENSITIVITY, mIsoMax);
            //            builder.set(CaptureRequest.LENS_FOCAL_LENGTH, (float) 0.25);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "this camera not support!", Toast.LENGTH_SHORT).show();
        }
    }

    final CameraCaptureSession.CaptureCallback decodeCaptureListener = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);

            Log.d(TAG, "Exposure time = " + result.get(CaptureResult.SENSOR_EXPOSURE_TIME));
            Log.d(TAG, "Frame duration = " + result.get(CaptureResult.SENSOR_FRAME_DURATION));
            Log.d(TAG, "Sensor sensitivity = " + result.get(CaptureResult.SENSOR_SENSITIVITY));

            Log.d(TAG, "lens focusDistance = " + result.get(CaptureResult.LENS_FOCUS_DISTANCE));

            Log.i(TAG, "usedMemory: " + Debug.getNativeHeapSize() / 1048576L);
            /*Log.d(TAG, "COLOR_CORRECTION_TRANSFORM = " + result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM));
            Log.d(TAG, "COLOR_CORRECTION_GAINS = " + result.get(CaptureResult.COLOR_CORRECTION_GAINS));
            */
        }

    };

    private void takeDecodePicture() {
        try {
            CaptureRequest.Builder builder = mCameraCaptureSession.getDevice()
                    .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(mImageReader.getSurface());

            if (mIsDebug) {
                //when in Debug mode, show the decode picture
                builder.addTarget(mSurface);
            }

            /*
             * Auto param builder.set(CaptureRequest.CONTROL_AF_MODE,
             * CaptureRequest.CONTROL_AF_MODE_AUTO);
             * builder.set(CaptureRequest.CONTROL_AF_TRIGGER,
             * CameraMetadata.CONTROL_AF_TRIGGER_START);
             * builder.set(CaptureRequest.JPEG_ORIENTATION, 90);
             */
            buildDecode(builder);
            mCameraCaptureSession.capture(builder.build(), decodeCaptureListener, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    private void initImageReader() {
        SurfaceTexture texture = textureView.getSurfaceTexture();
        assert texture != null;
        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        mSurface = new Surface(texture);

        mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(),
                ImageFormat.YUV_420_888, 10);

        mImageReader.setOnImageAvailableListener(imageReaderListener, mBackgroundHandler);

    }

    private void launchDecodeTask(byte[] bytes) {
        mDecodeTask = new DecodeTask(bytes).execute();
    }

    ImageReader.OnImageAvailableListener imageReaderListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d(TAG, "IMG ready ------------");
            Image image = null;
            try {
                image = reader.acquireLatestImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);

                if (mSave == true) {
                    UtilS.save(bytes, image.getHeight(), image.getWidth());
                    //save(bytes);
                    mSave = false;
                }

                launchDecodeTask(bytes);

            } catch (Exception err) {
                err.printStackTrace();
            } finally {
                if (image != null) {
                    image.close();
                }
            }
        }
    };

    private void initCaptureSession() {
        initImageReader();
        try {
            mCameraDevice.createCaptureSession(Arrays.asList(mSurface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }
                            // When the session is ready, we start displaying
                            // the preview.
                            mCameraCaptureSession = cameraCaptureSession;

                            if (!mIsDebug)
                                updatePreview();

                            takeDecodePicture();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Toast.makeText(Camera2Main.this, "Configuration change", Toast.LENGTH_SHORT).show();
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    final CameraCaptureSession.CaptureCallback previewCaptureListener = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);

            mPreviewCount++;
            if (mPreviewCount < SwitchingModeCount) {
                // createCameraPreview();
                // Do nothing ..... keep Burst --- setRepeatingRequest
            } else {
                //mPreviewCount = 0;
                //ruby debug ..........
                //closeCamera();
                //takeDecodePicture();
            }

            /*
             * Log.d(TAG, "Exposure time = " +
             * result.get(CaptureResult.SENSOR_EXPOSURE_TIME)); Log.d(TAG,
             * "Frame duration = " +
             * result.get(CaptureResult.SENSOR_FRAME_DURATION)); Log.d(TAG,
             * "Sensor sensitivity = " +
             * result.get(CaptureResult.SENSOR_SENSITIVITY));
             *
             * Log.d(TAG, "lens focusDistance = " +
             * result.get(CaptureResult.LENS_FOCUS_DISTANCE));
             *
             * Log.d(TAG, "COLOR_CORRECTION_TRANSFORM = " +
             * result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM)); Log.d(TAG,
             * "COLOR_CORRECTION_GAINS = " +
             * result.get(CaptureResult.COLOR_CORRECTION_GAINS));
             */
        }
    };

    private void buildPreview() {
        try {
            captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception err) {
            err.printStackTrace();
        }

        try {
            captureRequestBuilder.addTarget(mSurface);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
            //            captureRequestBuilder.set(CaptureRequest.LENS_FOCAL_LENGTH, (float) 0.25);
            captureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, (long) mExposureLower);
            captureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, mIsoMax);
        } catch (Exception err) {
            Toast.makeText(getApplicationContext(), "this camera not support---Preview!", Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePreview() {
        if (null == mCameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        /*
         * auto captureRequestBuilder.set(CaptureRequest.CONTROL_MODE,
         * CameraMetadata.CONTROL_MODE_AUTO);
         */
        buildPreview();
        try {
            mCameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), previewCaptureListener,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    private void closeCamera() {
        if (null != mCameraCaptureSession) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }

        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
        if (null != mSurface) {
            mSurface.release();
            mSurface = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(Camera2Main.this, "Sorry!!!, you can't use this app without granting permission",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.android_camera2_api, menu);
        return true;
    }
    */

    /*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	// Handle action bar item clicks here. The action bar will
    	// automatically handle clicks on the Home/Up button, so long
    	// as you specify a parent activity in AndroidManifest.xml.
    	switch (item.getItemId()) {
    	case android.R.id.home: // setDisplayHomeAsUpEnabled(false)，目前沒有Home鍵
    		this.finish();
    		return super.onOptionsItemSelected(item);
    	case R.id.action_frontCamera: // 前鏡頭選項
    		writePrefsToSetCameraFace(this, FRONT_FACE); // 前鏡頭選項
    		releaseResource();
    		reInitResource();
    		return true;
    	case R.id.action_backCamera: // 後鏡頭選項
    		writePrefsToSetCameraFace(this, BACK_FACE); // 設定為後鏡頭
    		releaseResource();
    		reInitResource();
    		return true;
    	default:
    		return super.onOptionsItemSelected(item);
    	}
    }
    */

    private void reInitResource() {
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    private void releaseResource() {
        closeCamera();
        stopBackgroundThread();
    }

    @Override
    protected void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        reInitResource();

        //reset the show content
        isBrowserOpened = false;
        isWebContentPosted = false;
        if (vlcIdButton != null) {
            vlcIdButton.setText("No Signal");
        }
        //btnSensorTrigger.setTextColor(Color.BLACK);
        //mHandler.obtainMessage(SHOW, "No Signal").sendToTarget();
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        releaseResource();
        super.onPause();
    }

    /* Get camara Param */
    private boolean mIsFrontCamera;
    private Size mPreviewSize;
    private Integer mIsoMax = 2500;
    private Integer mIsoMin;
    private Long mExposureUpper;
    private Long mExposureLower = 50000L;
    private int mIsoDecode;
    private int mIsoPreview;
    private Long mExposureDecode;
    private long mExposurePreview;

    private Size getPreferredPreviewSize(Size[] mapSizes) {

        for (Size s : mapSizes)
            Log.e("Camera2 Resolution", "(" + s.getWidth() + ", " + s.getHeight() + ")");

        // 找出Camera所支援解析度的最大高度以作為相機preview解析度的height
        int maxHeight = mapSizes[0].getHeight();
        for (Size s : mapSizes)
            maxHeight = (maxHeight < s.getHeight()) ? s.getHeight() : maxHeight;

        // 找出在最大Height下所對應到的最小寬度以作為相機preview解析度的width
        int minWidth = 1000000;
        for (Size s : mapSizes) {
            if (s.getHeight() == maxHeight)
                minWidth = (minWidth > s.getWidth()) ? s.getWidth() : minWidth;
        }

        Log.d(TAG, "(" + minWidth + "," + maxHeight + ")");

        return new Size(minWidth, maxHeight);
        // return new Size(2688, 1512);
        // return new Size(1024, 768);
        // return new Size(640, 480);
        // return new Size(3264, 2448); // Nexus 6P Test
        // return new Size(4032, 2268);
    }

    private void openCamera() {
        CameraCharacteristics characteristics;
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        // Add permission for camera and let user grant the permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(Camera2Main.this,
                    new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE },
                    REQUEST_CAMERA_PERMISSION);
            return;
        }

        UtilS.setDefaultCameraFace(this, UtilS.BACK_FACE);
        // 查詢menu所選擇的item為前鏡頭或後鏡頭
        if (UtilS.readPrefsToGetCameraFace(this).equals(UtilS.FRONT_FACE))
            mIsFrontCamera = true;
        else if (UtilS.readPrefsToGetCameraFace(this).equals(UtilS.BACK_FACE))
            mIsFrontCamera = false;

        try {
            for (String cameraId : manager.getCameraIdList()) {
                if (!mIsFrontCamera) {
                    // 後鏡頭
                    characteristics = manager.getCameraCharacteristics(cameraId);
                    if (characteristics
                            .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                        continue;
                    }

                    int hwLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                    Log.d("Img", "INFO_SUPPORTED_HARDWARE_LEVEL " + hwLevel);

                    int ManualSensor = -1;
                    int[] capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                    for (int capability : capabilities) {
                        if (capability == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) {
                            ManualSensor = 1;
                        }
                    }

                    if (ManualSensor != 1) {
                        //camera doesn't support any auto function
                        AlertDialog.Builder dialog = new AlertDialog.Builder(Camera2Main.this);

                        dialog.setTitle("抱歉 您的手機鏡頭尚不能支援可見光技術");
                        dialog.setMessage("結束可見光ID應用程式");

                        dialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                // TODO Auto-generated method stub
                                //Toast.makeText(Camera2Main.this, "我了解了",Toast.LENGTH_SHORT).show();
                                finish();
                            }

                        });
                        dialog.show();
                        return;
                    }

                    StreamConfigurationMap map = characteristics
                            .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    // mPreviewSize =
                    // getPreferredPreviewSize(map.getOutputSizes(SurfaceTexture.class));
                    //mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];
                    mPreviewSize = getPreferredPreviewSize(map.getOutputSizes(SurfaceTexture.class));
                    String Id = cameraId;
                    Range<Integer> isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
                    if (isoRange != null) {
                        mIsoMax = isoRange.getUpper();
                        mIsoMin = isoRange.getLower();
                        Log.d("Img", "isoMin= " + mIsoMin + " isoMax= " + mIsoMax);
                        mIsoDecode = ((50 * (mIsoMax - mIsoMin)) / 100 + mIsoMin);
                        mIsoPreview = ((50 * (mIsoMax - mIsoMin)) / 100 + mIsoMin);
                    } else {
                        Log.d("Img", "No IsoRange");
                    }

                    Range<Long> exposureTimeRange = characteristics
                            .get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
                    if (exposureTimeRange != null) {
                        mExposureUpper = exposureTimeRange.getUpper();
                        mExposureLower = exposureTimeRange.getLower();
                        Log.d("Img", "expoMin= " + mExposureLower + " expoMax= " + mExposureUpper);
                        mExposureDecode = mExposureLower;
                        // mExposurePreview = ((50 * (mExposureUpper -
                        // mExposureLower)) / 100 + mExposureLower);\
                        mExposurePreview = ((80 * (mExposureUpper - mExposureLower)) / 100 + mExposureLower);
                    } else {
                        Log.d("Img", "No ExposeRange");
                    }

                    manager.openCamera(Id, openCameraStateCallBack, null);
                    return;
                } else {
                    // 前鏡頭
                    characteristics = manager.getCameraCharacteristics(cameraId);
                    if (characteristics
                            .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                        int imgHwLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                        if (imgHwLevel != 1) {
                            //Toast.makeText(getApplicationContext(), "Not Full Camera2 is supported", Toast.LENGTH_LONG).show();
                        }
                        Log.d("Img", "INFO_SUPPORTED_HARDWARE_LEVEL "
                                + characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL));
                        StreamConfigurationMap map = characteristics
                                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                        mPreviewSize = getPreferredPreviewSize(map.getOutputSizes(SurfaceTexture.class));
                        String Id = cameraId;
                        Range<Integer> isoRange = characteristics
                                .get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
                        if (isoRange != null) {
                            mIsoMax = isoRange.getUpper();
                            mIsoMin = isoRange.getLower();
                            Log.d("Img", "isoMin= " + mIsoMin + " isoMax= " + mIsoMax);
                            mIsoDecode = ((50 * (mIsoMax - mIsoMin)) / 100 + mIsoMin);
                            mIsoPreview = ((50 * (mIsoMax - mIsoMin)) / 100 + mIsoMin);
                        } else {
                            Log.d("Img", "No IsoRange");
                        }

                        Range<Long> exposureTimeRange = characteristics
                                .get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
                        if (exposureTimeRange != null) {
                            mExposureUpper = exposureTimeRange.getUpper();
                            mExposureLower = exposureTimeRange.getLower();
                            Log.d("Img", "expoMin= " + mExposureLower + " expoMax= " + mExposureUpper);
                            mExposureDecode = mExposureLower;
                            // mExposurePreview = ((50 * (mExposureUpper -
                            // mExposureLower)) / 100 + mExposureLower);\
                            mExposurePreview = ((80 * (mExposureUpper - mExposureLower)) / 100 + mExposureLower);
                        } else {
                            Log.d("Img", "No ExposeRange");
                        }
                        manager.openCamera(Id, openCameraStateCallBack, null);
                        return;
                    }
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    /* async task to wrap the decoder */
    // 以AsyncTask方式在背景執行非同步任務，以Handler傳遞訊息至UI Thread來更新UI
    private class DecodeTask extends AsyncTask<Void, Void, Void>
    // <Params,
    // Progress,
    // Result>
    {
        private Decoder mDecoder;
        private String Lifi_ID = null;
        private int startBitSize = UtilS.getSavedBitSize(getApplicationContext());

        public DecodeTask(byte[] data) {
            super();

            //this.mData = data.clone();
            mDecoder = new Decoder(mPreviewSize.getHeight(), mPreviewSize.getWidth(), 0, data, false, "01110", 8);
        }

        // 要在背景執行的內容
        @Override
        protected Void doInBackground(Void... params) {

            VlcIdRec vlcIdRec = null;

            vlcIdRec = mDecoder.getReceivedID();
            if (vlcIdRec != null) {
                VlcIdAns vlcIdAns = vlcIdRec.getBestVlcId();
                Lifi_ID = vlcIdAns.getBestVlcIdStr();
                if (Lifi_ID != null) {

                    if (mBitSizeTraining) {
                        Lifi_ID = Lifi_ID + ", count=" + vlcIdAns.getBestVlcIdCount();
                    }
                    Log.d(TAG, "Lifi_ID=" + Lifi_ID + " count=" + vlcIdAns.getBestVlcIdCount());

                    int get_bit_size = vlcIdAns.getBestBitSize();
                    UtilS.saveBitSize(getApplicationContext(), get_bit_size);
                }
            } else {
                //decode get nothing
            }
            return (Void) null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mDecodeTask = null;
            Log.e(TAG, "Lifi_ID->-> " + Lifi_ID);
            if (Lifi_ID != null)
                showVlcId(Lifi_ID);
            else
                takeDecodePicture();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mDecodeTask = null;
        }
    }

    // Web Content Test
    private static final int SHOW = 1;
    private MyInnerHandler mHandler = new MyInnerHandler(Camera2Main.this); // Inner Class的物件: 作為UI Thread之Handler
    private DownloadWebPicture loadPic;
    private Handler myHandler;
    public static Bitmap bmp;
    private static final String url105 = "http://cloudmeter.org/vlcNew/product/販賣孩子的商人_large.png";
    private static final String url106 = "http://cloudmeter.org/vlcNew/product/販賣孩子的商人_large.png";
    private static final String url113 = "http://cloudmeter.org/vlcNew/product/LED可見光通訊技術_large.png";
    private static final String url114 = "http://cloudmeter.org/vlcNew/product/認證圖檔_large.png";
    private boolean isWebContentPosted = false;
    private boolean isBrowserOpened = false;

    // Inner Class: 作為UI Thread之Handler
    private static class MyInnerHandler extends Handler {
        WeakReference<Camera2Main> mMainActivity;

        MyInnerHandler(Camera2Main mainActivity) {
            mMainActivity = new WeakReference<Camera2Main>(mainActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            Camera2Main theActivity = mMainActivity.get();
            String receive = null;
            switch (msg.what) {
            case SHOW:
                if (msg.obj instanceof String)
                    receive = (String) msg.obj;
                if (receive != null) {
                    theActivity.showVlcId(receive);
                }
                break;
            default:
                break;
            }
        }
    }

    // 顯示ID + 振動
    private void showVlcId(String vlcID) {
        assert (vlcID != null);

        Boolean parsable = true;
        int vlcid = 0;
        try {
            vlcid = Integer.parseInt(vlcID, 2);
        } catch (NumberFormatException e) {
            parsable = false;
        }

        if (parsable == false) {
            //This Id can not be parsed ????
            takeDecodePicture();
            return;
        }

        Log.d(TAG, "showVlcId : " + vlcID);

        vibrator.vibrate(VIBRATE_DURATION);
        scanLine.clearAnimation();

        Intent intent = new Intent();
        intent.putExtra("SCAN_RESULT", vlcID);
        this.setResult(RESULT_OK, intent);
        finish();
        // Toast.makeText(getApplicationContext(), "連結至網頁中..."+vlcID, Toast.LENGTH_SHORT).show();

        // if (vlcIdButton != null) {
        //     vlcIdButton.setText("連結至網頁中");
        //     vlcIdButton.setTextColor(Color.BLUE);
        // }

        //String url = "http://139.162.118.12/ajax-crud/redirect.php?vlcid="+vlcid;
        // String url = "https://www.youtube.com/watch?v=E4I2GVfw1HI";
        // Uri itriUri = Uri.parse(url);
        // Intent it = new Intent(Intent.ACTION_VIEW, itriUri);
        // startActivity(it);
    }

    // Web Content Test
    public class DownloadWebPicture {
        public synchronized Bitmap getUrlPic(String url) {

            Bitmap webImg = null;

            try {

                URL imgUrl = new URL(url); // 建立URL
                HttpURLConnection httpURLConnection = (HttpURLConnection) imgUrl.openConnection(); // 建立連線
                httpURLConnection.connect(); // 初始化連線

                if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    // 開始讀取資料
                    InputStream inputStream = httpURLConnection.getInputStream();
                    int length = (int) httpURLConnection.getContentLength(); // 目標資料的長度
                    int tmpLength = 512;
                    int readLen = 0, desPos = 0;
                    byte[] img = new byte[length];
                    byte[] tmp = new byte[tmpLength];

                    if (length != -1) {
                        // 持續讀取，直到沒有資料進來
                        while ((readLen = inputStream.read(tmp)) > 0) { // 讀取的資料存放在tmp，readLen為讀取到的資料長度
                            System.arraycopy(tmp, 0, img, desPos, readLen); // copy array data from tmp to img
                            desPos += readLen; // 移動存放目標的當前位置
                        }
                        webImg = BitmapFactory.decodeByteArray(img, 0, img.length);
                        if (desPos != length) {
                            throw new IOException("Only read" + desPos + "bytes");
                        }
                    }
                }
                httpURLConnection.disconnect();
            } catch (IOException e) {
                Log.e("IOException", e.toString());
            }
            return webImg;
        }

        public void handleWebPic(final String url, final Handler handler) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    bmp = getUrlPic(url);
                    Message msg = new Message();
                    msg.what = 1;
                    handler.sendMessage(msg); // 下載完圖片，通知主執行緒更新UI畫面
                }
            }).start();
        }

        public Bitmap getImg() {
            return bmp;
        }
    }

    protected void setupActionBar() {
        Toolbar myToolbar = (Toolbar) findViewById(R.id("my_toolbar"));
        setSupportActionBar(myToolbar);
    }

}
