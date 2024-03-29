package com.example.camera2demo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CameraAct extends AppCompatActivity {
    private static final String TAG = "Camera_Act";


    private final int REQUEST_PERMISSION_CAMERA = 100;
    private boolean mbFaceDetAvailable;
    private int miMaxFaceCount = 0;
    private int miFaceDetMode;
    private Size mPreviewSize = null;
    private CameraDevice mCameraDevice = null;
    private CaptureRequest.Builder mPreviewBuilder = null;
    private CameraCaptureSession mCameraPreviewCaptureSession = null,
            mCameraTakePicCaptureSession = null;
    byte[] bytes;


    // 當UI的TextureView建立時，會執行onSurfaceTextureAvailable()
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
                                              int width, int height) {
            // 檢查是否取得使用camera的權限
            if (askForPermissions())
                openCamera();   // 開啟camera.
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
                                                int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    private View.OnClickListener btnPic_click = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            askForPermissions();
            takePicture();
        }
    };

    private View.OnClickListener btn2_click = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String encode = Base64.encodeToString(bytes, 0);
            File base64txt = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath(), "base64.txt");
            try {
                base64txt.createNewFile();
                FileOutputStream outPutStream = new FileOutputStream(base64txt);
                OutputStreamWriter myOutWriter = new OutputStreamWriter(outPutStream );
                myOutWriter.append(encode);
                myOutWriter.close();
                outPutStream.flush();
                outPutStream.close();
                Log.d("Pic Encode","-----txt file saved");
            } catch (IOException e) {
                e.printStackTrace();
            }

            //上傳base64 code
             PostData process = new PostData(encode);
             process.execute();
        }
    };

    public class PostData extends AsyncTask<Void,Void,Void> {

        public PostData(String strEncode) {
            this.strEncode = strEncode;
        }

        String strEncode;


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            JSONObject ob = new JSONObject();
            try {

                ob.put("SAMPLE_DATE","20190726");
                ob.put("CUSTOMER_NO","42856576");
                ob.put("DEVICE_NO","1");
                ob.put("PATIENT_NO","123");
                ob.put("SAMPLE_QTY","1");
                ob.put("SAMPLE_TYPE","Urine");
                ob.put("SAMPLE1",strEncode);
                ob.put("SAMPLE2","");
                ob.put("SAMPLE3","");
                ob.put("SAMPLE4","");
                ob.put("SAMPLE5","");

                String strSearch= ob.toString();
                Log.d(TAG,"--POST BODY:\n"+ strSearch);
                //網址轉碼
                URL url = new URL("http://210.17.120.51/json/APP01-PushSample.ashx");
                //取得連線
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Content-Type","application/json; charset=utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setDoInput(true); //允許輸入流，即允許下載
                conn.setDoOutput(true); //允許輸出流，即允許上傳
                conn.setUseCaches(false); //設置是否使用緩存
                OutputStream os = conn.getOutputStream();
                DataOutputStream writer = new DataOutputStream(os);

                writer.writeBytes(strSearch);
                writer.flush();
                writer.close();
                os.close();
                //取得串流
                InputStream streamIn = conn.getInputStream();
                //準備開始解碼，首先，把剛剛的串流讀進來，製作一個串流讀取器(BufferReader)
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(streamIn));
                //做一個StringBuilder,接著不斷地去讀取串流，讀到他是NULL為止，在這之前則把每一行 append  到 StringBuilder 裡面
                StringBuilder html  = new StringBuilder();

                String line ;

                while ( (line = bufferedReader.readLine()) != null){
                    html.append(line);
                }
                String strJson = html.toString();
                Log.d(TAG,"--Response Sring :\n "+strJson);

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_CAMERA:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    openCamera();
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private  boolean askForPermissions() {
        Log.d(TAG,"askForPermissions:start");
        // App需要用的功能權限清單
        String[] permissions= new String[] {
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};

        // 檢查是否已經取得權限
        final List<String> listPermissionsNeeded = new ArrayList<>();
        boolean bShowPermissionRationale = false;

        for (String p: permissions) {
            int result = ContextCompat.checkSelfPermission(CameraAct.this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);

                // 檢查是否需要顯示說明
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        CameraAct.this, p))
                    bShowPermissionRationale = true;
            }
        }

        // 向使用者徵詢還沒有許可的權限
        if (!listPermissionsNeeded.isEmpty()) {
            if (bShowPermissionRationale) {
                AlertDialog.Builder altDlgBuilder =
                        new AlertDialog.Builder(CameraAct.this);
                altDlgBuilder.setTitle("提示");
                altDlgBuilder.setMessage("App需要您的許可才能執行。");
                altDlgBuilder.setIcon(android.R.drawable.ic_dialog_info);
                altDlgBuilder.setCancelable(false);
                altDlgBuilder.setPositiveButton("確定",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(CameraAct.this,
                                        listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                                        REQUEST_PERMISSION_CAMERA);
                            }
                        });
                altDlgBuilder.show();
            } else
                ActivityCompat.requestPermissions(CameraAct.this,
                        listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                        REQUEST_PERMISSION_CAMERA);

            return false;
        }

        return true;
    }

    private void openCamera() {
        // 取得 CameraManager
        CameraManager camMgr = (CameraManager) getSystemService(CAMERA_SERVICE);

        try{
            // 取得相機背後的 camera
            String cameraId = camMgr.getCameraIdList()[0];
            CameraCharacteristics camChar = camMgr.getCameraCharacteristics(cameraId);

            // 取得解析度
            StreamConfigurationMap map = camChar.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];

            // 檢查是否有人臉偵測功能
            int[] iFaceDetModes = camChar.get(
                    CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES);
            if (iFaceDetModes == null) {
                mbFaceDetAvailable = false;
                Toast.makeText(CameraAct.this, "不支援人臉偵測", Toast.LENGTH_LONG)
                        .show();
            } else {
                mbFaceDetAvailable = false;
                for (int mode : iFaceDetModes) {
                    if (mode == CameraMetadata.STATISTICS_FACE_DETECT_MODE_SIMPLE) {
                        mbFaceDetAvailable = true;
                        miFaceDetMode = CameraMetadata.STATISTICS_FACE_DETECT_MODE_SIMPLE;
                        break;   // Find the desired mode, so stop searching.
                    } else if (mode == CameraMetadata.STATISTICS_FACE_DETECT_MODE_FULL) {
                        // This is a candidate mode, keep searching.
                        mbFaceDetAvailable = true;
                        miFaceDetMode = CameraMetadata.STATISTICS_FACE_DETECT_MODE_FULL;
                    }
                }
            }

            if (mbFaceDetAvailable) {
                miMaxFaceCount = camChar.get(
                        CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT);

                Toast.makeText(CameraAct.this, "人臉偵測功能: " + String.valueOf(miFaceDetMode) +
                        "\n人臉樹最大值: " + String.valueOf(miMaxFaceCount), Toast.LENGTH_LONG)
                        .show();
            }

            // 啟動 camera
            if (ContextCompat.checkSelfPermission(CameraAct.this,
                    Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                camMgr.openCamera(cameraId, mCameraStateCallback, null);
        }
        catch(CameraAccessException e)
        {
            e.printStackTrace();
        }
    }

    private CameraDevice.StateCallback mCameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            Toast.makeText(CameraAct.this, "無法使用camera", Toast.LENGTH_LONG)
                    .show();
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            Toast.makeText(CameraAct.this, "Camera開啟錯誤", Toast.LENGTH_LONG)
                    .show();
        }
    };

    // Camera的CaptureSession狀態改變時執行
    private CameraCaptureSession.StateCallback mCameraCaptureSessionCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
            Log.d(TAG,"狀態改變 ");
            closeAllCameraCaptureSession();

            // 記下這個capture session，使用完畢要刪除
            mCameraPreviewCaptureSession = cameraCaptureSession;

            mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            mPreviewBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, miFaceDetMode);

            HandlerThread backgroundThread = new HandlerThread("CameraPreview");
            backgroundThread.start();
            Handler backgroundHandler = new Handler(backgroundThread.getLooper());

            try {
                mCameraPreviewCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), null, backgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
            Toast.makeText(CameraAct.this, "Camera預覽錯誤", Toast.LENGTH_LONG)
                    .show();
        }
    };

    private void startPreview() {
        // 從UI元件的TextureView取得SurfaceTexture
        // 依照 camera的解析度，設定TextureView的解析度
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

        // 依照TextureView的解析度建立一個 surface 給camera使用
        Surface surface = new Surface(surfaceTexture);

        // 設定camera的CaptureRequest和CaptureSession
        try {
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e){
            e.printStackTrace();
        }

        mPreviewBuilder.addTarget(surface);

        try {
            mCameraDevice.createCaptureSession(Arrays.asList(surface), mCameraCaptureSessionCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // 建立新的Camera Capture Session之前
    // 呼叫這個方法，清除舊的Camera Capture Session
    private void closeAllCameraCaptureSession() {
        if (mCameraPreviewCaptureSession != null) {
            mCameraPreviewCaptureSession.close();
            mCameraPreviewCaptureSession = null;
        }

        if (mCameraTakePicCaptureSession != null) {
            mCameraTakePicCaptureSession.close();
            mCameraTakePicCaptureSession = null;
        }
    }

    private void takePicture() {
        Log.d(TAG,"takePicture:start");
        if(mCameraDevice == null) {
            Toast.makeText(CameraAct.this, "Camera錯誤", Toast.LENGTH_LONG).show();
            return;
        }

        // 準備影像檔
        final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath(), "photo.jpg");

        // 準備OnImageAvailableListener
        ImageReader.OnImageAvailableListener imgReaderOnImageAvailable =
                new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader imageReader) {
                        // 把影像資料寫入檔案
                        Image image = null;
                        try {
                            image = imageReader.acquireLatestImage();
                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                            bytes = new byte[buffer.capacity()];
                            buffer.get(bytes);
                            OutputStream output = null;
                            try {
                                output = new FileOutputStream(file);
                                output.write(bytes);
                            } finally {
                                if (null != output)
                                    output.close();
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (image != null)
                                image.close();
                        }
                    }
                };

        // 取得 CameraManager
        CameraManager camMgr = (CameraManager) getSystemService(CAMERA_SERVICE);

        try {
            CameraCharacteristics camChar = camMgr.getCameraCharacteristics(mCameraDevice.getId());

            // 設定拍照的解析度
            Size[] jpegSizes = null;
            if (camChar != null)
                jpegSizes = camChar.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            int picWidth = 640;
            int picHeight = 480;
            if (jpegSizes != null && jpegSizes.length > 0) {
                picWidth = jpegSizes[0].getWidth();
                picHeight = jpegSizes[0].getHeight();
            }

            // 設定照片要輸出給誰: 1. 儲存為影像檔； 2. 輸出給UI的TextureView顯示
//            ImageReader imgReader = ImageReader.newInstance(picWidth, picHeight, ImageFormat.JPEG, 1);
            ImageReader imgReader = ImageReader.newInstance(256, 256, ImageFormat.JPEG, 1);


            // 準備拍照用的thread
            HandlerThread thread = new HandlerThread("CameraTakePicture");
            thread.start();
            final Handler backgroudHandler = new Handler(thread.getLooper());

            // 把OnImageAvailableListener和thread設定給ImageReader
            imgReader.setOnImageAvailableListener(imgReaderOnImageAvailable, backgroudHandler);

            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(imgReader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));

            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imgReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            // 決定照片的方向（直的或橫的）
            SparseIntArray PICTURE_ORIENTATIONS = new SparseIntArray();
            PICTURE_ORIENTATIONS.append(Surface.ROTATION_0, 90);
            PICTURE_ORIENTATIONS.append(Surface.ROTATION_90, 0);
            PICTURE_ORIENTATIONS.append(Surface.ROTATION_180, 270);
            PICTURE_ORIENTATIONS.append(Surface.ROTATION_270, 180);

            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, PICTURE_ORIENTATIONS.get(rotation));

            // 準備拍照的callback
            final CameraCaptureSession.CaptureCallback camCaptureCallback =
                    new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                            super.onCaptureCompleted(session, request, result);

                            Integer mode = result.get(CaptureResult.STATISTICS_FACE_DETECT_MODE);
                            Face[] faces = result.get(CaptureResult.STATISTICS_FACES);
                            if(faces != null && mode != null)
                                Toast.makeText(CameraAct.this, "人臉: " + faces.length, Toast.LENGTH_SHORT).show();

                            // 播放快門音效檔
                            Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.sound_camera_shutter);
                            MediaPlayer mp = MediaPlayer.create(CameraAct.this, uri);
                            mp.start();

                            Toast.makeText(CameraAct.this, "拍照完成\n影像檔: " + file, Toast.LENGTH_SHORT).show();
                            startPreview();
                        }

                        @Override
                        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
                        }
                    };

            // 最後一步就是建立Capture Session
            // 然後啟動拍照
            mCameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            try {
                                closeAllCameraCaptureSession();

                                // 記下這個capture session，使用完畢要刪除
                                mCameraTakePicCaptureSession = cameraCaptureSession;

                                cameraCaptureSession.capture(captureBuilder.build(), camCaptureCallback, backgroudHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {

                        }
                    },
                    backgroudHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_act);
        InitialComponent();
    }

    @Override
    protected void onResume() {
        super.onResume();
        textureView.setSurfaceTextureListener(mSurfaceTextureListener);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mCameraDevice != null)
        {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void InitialComponent() {
        textureView = findViewById(R.id.textureView);
        btnPic = findViewById(R.id.btnPic);
        btnPic.setOnClickListener(btnPic_click);
        btn2 = findViewById(R.id.btn2);
        btn2.setOnClickListener(btn2_click);
    }

    Button btnPic, btn2;
    TextureView textureView;
    TextView txt;
    ImageView imageView;

}
