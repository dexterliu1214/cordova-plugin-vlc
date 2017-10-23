package itri.icl.k400.vlcid.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by 940763 on 2017/5/3.
 */

public class UtilS {

    private final static String TAG = "utils";

    public static String currentDateFormat() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd-HH-mm-ss");
        String currentTimeStamp = dateFormat.format(new Date());
        return currentTimeStamp;
    }

    /* parame save */
    public static final String PREF_FILE = "PREF_FILE";
    public static final String PREF_BITSIZE_FRONT = "PREF_BITSIZE_FRONT";
    public static final String PREF_BITSIZE_BACK = "PREF_BITSIZE_BACK";
    public static final String PREF_CAMERA_FACE = "PREF_CAMERA_FACE";
    public static final String FRONT_FACE = "FRONT_FACE";
    public static final String BACK_FACE = "BACK_FACE";

    public static void setDefaultCameraFace(Context context, String face) {
        assert((face.equals(BACK_FACE)) || (face.equals(FRONT_FACE)));
        // 查詢前後鏡頭的Bit Size
        Log.e("Bit Size", "front: " + readPrefsToGetFrontBitSize(context));
        Log.e("Bit Size", "back: " + readPrefsToGetBackBitSize(context));
        /*
        Toast.makeText(context,
                "前鏡頭: " + readPrefsToGetFrontBitSize(context) + "\n後鏡頭: " + readPrefsToGetBackBitSize(context),
                Toast.LENGTH_LONG).show();
        */
        // 預設為前鏡頭
        // Decoder.writePrefsToSetCameraFace(this, Decoder.FRONT_FACE);
        // 預設為後鏡頭
        writePrefsToSetCameraFace(context, face);
        /*
        if(face == BACK_FACE){
            Toast.makeText(context,
                    "後鏡頭: " + readPrefsToGetBackBitSize(context),
                    Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(context,
                    "前鏡頭: " + readPrefsToGetFrontBitSize(context),
                    Toast.LENGTH_SHORT).show();
        }
        */
    }

    public static int readPrefsToGetFrontBitSize(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_FILE, 0);
        return prefs.getInt(PREF_BITSIZE_FRONT, 0);
    }

    public static void writePrefsToSetFrontBitSize(Context context, int size) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_FILE, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_BITSIZE_FRONT, size);
        editor.commit();
    }

    public static int readPrefsToGetBackBitSize(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_FILE, 0);
        return prefs.getInt(PREF_BITSIZE_BACK, 0);
    }

    public static void writePrefsToSetBackBitSize(Context context, int size) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_FILE, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_BITSIZE_BACK, size);
        editor.commit();
    }

    public static String readPrefsToGetCameraFace(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_FILE, 0);
        return prefs.getString(PREF_CAMERA_FACE, "FRONT_FACE");
    }

    public static void writePrefsToSetCameraFace(Context context, String face) {
        assert((face.equals(BACK_FACE)) || (face.equals(FRONT_FACE)));
        SharedPreferences prefs = context.getSharedPreferences(PREF_FILE, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_CAMERA_FACE, face);
        editor.commit();
    }

    public static int getSavedBitSize(Context mContext) {
        boolean isFrontCamera = false;
        boolean isKnowBitSize = false;
        int bit_size = 0;

        // 查詢menu所選擇的item為前鏡頭或後鏡頭
        if (readPrefsToGetCameraFace(mContext).equals(FRONT_FACE))
            isFrontCamera = true;
        else if (readPrefsToGetCameraFace(mContext).equals(BACK_FACE))
            isFrontCamera = false;

        if (isFrontCamera) // 前鏡頭
        {
            bit_size = readPrefsToGetFrontBitSize(mContext);
        } else // 後鏡頭
        {
            bit_size = readPrefsToGetBackBitSize(mContext);
        }

        return bit_size;
    }

    public static void saveBitSize(Context mContext, int bit_size) {
        boolean isFrontCamera = false;

        //
        // 查詢menu所選擇的item為前鏡頭或後鏡頭
        if (readPrefsToGetCameraFace(mContext).equals(FRONT_FACE))
            isFrontCamera = true;
        else if (readPrefsToGetCameraFace(mContext).equals(BACK_FACE))
            isFrontCamera = false;

        if (isFrontCamera) {
            writePrefsToSetFrontBitSize(mContext, (int) bit_size);
        } else {
            writePrefsToSetBackBitSize(mContext, (int) bit_size);
        }
    }


    private static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    private static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
            return true;
        }
        return false;
    }


    public static String saveFile(Context context, String destFileName, byte[] inputByteArray) {
        File[] Dirs = ContextCompat.getExternalFilesDirs(context, null);
        File destFile = new File(Dirs[0], destFileName);

        try{
            FileOutputStream fos = new FileOutputStream(destFile);
            fos.write(inputByteArray);
            fos.close();
        }
        catch (IOException e){
            e.printStackTrace();
            return null;

        }
        return destFile.getPath();

    }


    public static byte[] readFile (String destFileName) {
        File destFile = new File(destFileName);
        byte[] returnBytes=null;

        if(destFile!=null){
            try {
                returnBytes = fullyReadFileToBytes(destFile);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        else{
            return null;
        }
        return returnBytes;
    }

    public static byte[] fullyReadFileToBytes(File f) throws IOException {
        int size = (int) f.length();
        byte bytes[] = new byte[size];
        byte tmpBuff[] = new byte[size];
        FileInputStream fis= new FileInputStream(f);;
        try {

            int read = fis.read(bytes, 0, size);
            if (read < size) {
                int remain = size - read;
                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain);
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
                    remain -= read;
                }
            }
        }  catch (IOException e){
            throw e;
        } finally {
            fis.close();
        }

        return bytes;
    }


    public static void save(byte[] bytes) {
        final File file = new File(Environment.getExternalStorageDirectory() + "/" + currentDateFormat() + "pic.jpg");
        OutputStream output = null;
        try {
            output = new FileOutputStream(file);
            output.write(bytes);
        } catch (IOException e) {
            Log.e(TAG, "save file error !");
            e.printStackTrace();
        }
        catch (Exception err){
            err.printStackTrace();
        }
        finally {
            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }catch (Exception err){
                    err.printStackTrace();
                }
            }
        }
    }

    public static void save(byte[] bytes, int height, int width) {
        // 以單次型執行緒繪圖
        class OneShotTask implements Runnable {
            byte[] bytes;
            int h, w;

            OneShotTask(byte[] inbytes, int inh, int inw) {
                //bytes = inbytes.clone();
                bytes = inbytes;
                h = inh;
                w = inw;
            }

            public void run() {
                saveBitMap(bytes, h, w);
            }
        }
        Thread t = new Thread(new OneShotTask(bytes, height, width));
        t.start();

    }

    /* create bitmap from grayscale */
    public static Bitmap createBitMap(byte[] imageArray, int height, int width) {
        Bitmap bm;
        int value;
        bm = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                value = ((int) imageArray[y * width + x]) & 0x000000FF;
                // byte
                // ->
                // int
                // (4
                // bytes)，僅保留原來byte值的部分
                // bm.setPixel((h-1)-y, (w-1)-x, Color.argb(255, value, value,
                // value));
                // 透明度0%
                if (y < 100) {
                    if(x<500) bm.setPixel(x, y, Color.rgb(0, 255, 0)); // 透明度0%
                    else
                        bm.setPixel(x, y, Color.rgb(255, 0, 0)); // 透明度0%
                } else {
                    bm.setPixel(x, y, Color.rgb(value, value, value)); // 透明度0%
                }
            }
        }
        return bm;
    }

    public static void saveBitMap(byte[] bytes, int height, int width) {
        final File file = new File(Environment.getExternalStorageDirectory() + "/" + currentDateFormat() + "pic.jpg");
        Bitmap bm = createBitMap(bytes, height, width);
        OutputStream output = null;
        try {
            output = new FileOutputStream(file);
            bm.compress(Bitmap.CompressFormat.JPEG, 100, output);
            // output.write(bytes);
        } catch (IOException e) {
            Log.e(TAG, "save file error !");
            e.printStackTrace();
        } catch (Exception err){
            err.printStackTrace();
        }
        finally {
            if (null != output) {
                try {
                    if ((bm != null) && (!bm.isRecycled())) {
                        bm.recycle(); // 回收圖片所占的記憶體
                        // system.gc(); //提醒系統及時回收
                    }
                    output.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }catch (Exception err){
                    err.printStackTrace();
                }
            }
        }
    }


    private static final String REGISTER_URL = "http://139.162.118.12/vlcinfo.php";
    //public static final String REGISTER_URL = "http://139.162.118.12/insert.php";

    public static final String KEY_MANUFACTURER = "manufacturer";
    public static final String KEY_MODEL = "model";
    public static final String KEY_PRODUCT = "product";
    public static final String KEY_RELVER = "releaseVer";
    public static final String KEY_SDKVER = "sdkVer";
    public static final String KEY_BITSIZE = "bitSize";
    public static final String KEY_HWLEVEL = "hwLevel";

    public static void reportInfo(String _manufacturer, String _model, String _product, String _releaseVer, String _sdkVer, String _bitSize, String _hwLevel,
                                  Context _context){

        final String manufacturer = _manufacturer!=null?_manufacturer:"-1";
        final String model = _model!=null?_model:"-1";
        final String product = _product!=null?_product:"-1";
        final String releaseVer = _releaseVer!=null?_releaseVer:"-1";
        final String sdkVer = _sdkVer!=null?_sdkVer:"-1";
        final String bitSize = _bitSize!=null?_bitSize:"-1";
        final String hwLevel = _hwLevel!=null?_hwLevel:"-1";
        final Context context = _context;


        StringRequest stringRequest = new StringRequest(Request.Method.POST, REGISTER_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Toast.makeText(Camera2Main.this,response,Toast.LENGTH_LONG).show();
                        Log.d("DB---",response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //Toast.makeText(Camera2Main.this,error.toString(),Toast.LENGTH_LONG).show();
                        Log.d("DB---",error.toString());
                    }
                }){
            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<String, String>();
                params.put(KEY_MANUFACTURER,manufacturer);
                params.put(KEY_MODEL,model);
                params.put(KEY_PRODUCT, product);
                params.put(KEY_RELVER, releaseVer);
                params.put(KEY_SDKVER, sdkVer);
                params.put(KEY_BITSIZE, bitSize);
                params.put(KEY_HWLEVEL, hwLevel);

                return params;
            }

        };

        RequestQueue requestQueue = Volley.newRequestQueue(context);
        requestQueue.add(stringRequest);

    }


    public static HashMap<String, String> gatherHandsetInfo(){
        String info="";
        // 主機版名稱
        String board = Build.BOARD;
        info = info+"主機版名稱:"+board;

        // releaseVer
        String releaseVer = Build.VERSION.RELEASE;
        info = info+"\nreleaseVer:"+releaseVer;

        //sdkVersion
        int _sdkVersion = Build.VERSION.SDK_INT;
        String sdkVersion = Integer.toString(_sdkVersion);
        info = info+"\nsdkVersion:"+sdkVersion;

        // 品牌名稱
        String brand = Build.BRAND;
        info = info+"\n品牌名稱:"+brand;

        // CPU + ABI
        String cpu = Build.CPU_ABI;
        info = info+"\nCPU + ABI:"+cpu;

        // 設備名稱
        String device = Build.DEVICE;
        info = info+"\n設備名稱:"+device;

        // 版本號碼
        String display = Build.DISPLAY;
        info = info+"\n版本號碼:"+display;

        // 設備識別碼
        String fingerprint = Build.FINGERPRINT;
        info = info+"\n設備識別碼:"+fingerprint;

        // HOST
        String host = Build.HOST;
        info = info+"\nHOST:"+host;

        // 版本號碼
        String id = Build.ID;
        info = info+"\n版本號碼:"+id;

        // 製造商
        String manufacturer = Build.MANUFACTURER;
        info = info+"\n製造商:"+manufacturer;

        // 模組號碼
        String model = Build.MODEL;
        info = info+"\n模組號碼:"+model;

        // 產品名稱
        String product = Build.PRODUCT;
        info = info+"\n產品名稱:"+product;

        // 設備描述
        String tags = Build.TAGS;
        info = info+"\n設備描述:"+tags;

        // 設備類別; user or eng
        String type = Build.TYPE;
        info = info+"\n設備類別:"+type;

        // USER
        String user = Build.USER;
        info = info+"\nUSER:"+user;

        //Log.d("INFO---",info);

        HashMap<String, String> handsetInfo = new HashMap<String, String>();
        handsetInfo.put("manufacturer",manufacturer);
        handsetInfo.put("product",product);
        handsetInfo.put("model",model);
        handsetInfo.put("releaseVer",releaseVer);
        handsetInfo.put("sdkVer",sdkVersion);

        return handsetInfo;
    }

}
