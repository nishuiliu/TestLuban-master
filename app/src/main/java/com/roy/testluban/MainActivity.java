package com.roy.testluban;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import top.zibin.luban.CompressionPredicate;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;
import uk.co.senab.photoview.PhotoView;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final int TAKE_PHOTO = 100;
    private static final int PHOTO = 200;
    private static final int CAMERA = 1;

    @BindView(R.id.btn_take_photo)
    Button btnTakePhoto;
    @BindView(R.id.btn_open)
    Button btnOpen;
    @BindView(R.id.tv_yuan)
    TextView tvYuan;
    @BindView(R.id.tv_change)
    TextView tvChange;
    @BindView(R.id.iv_show_real)
    PhotoView ivShow;
    @BindView(R.id.iv_show_compress)
    PhotoView ivShowCompress;
    @BindView(R.id.et_max_size)
    EditText etMaxSize;
    @BindView(R.id.et_quality)
    EditText etQuality;

    private File photoFile;
    private int maxSize=100;
    private int qualty=60;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initPersission();
    }

    @OnClick({R.id.btn_take_photo, R.id.btn_open})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_take_photo:
                photoFile = new File(Environment.getExternalStorageDirectory() + File.separator + "TestLubanYuan.jpg");

                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                Uri photoUri = Uri.fromFile(photoFile);
                photoUri.getPath();
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(intent, TAKE_PHOTO);

                break;
            case R.id.btn_open:
                Intent intent1 = new Intent(Intent.ACTION_PICK, null);
                intent1.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(intent1, PHOTO);
                break;
        }
    }

    private void initPersission() {
        String perm[] = {Manifest.permission.CAMERA};
        if (EasyPermissions.hasPermissions(this, perm)) {

        } else {
            EasyPermissions.requestPermissions(this, "需要摄像头权限权限",
                    CAMERA, perm);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case TAKE_PHOTO:
                    Glide.with(MainActivity.this).load(photoFile).centerCrop().crossFade().into(ivShow);
                    tvYuan.setText("原图：图片大小" + photoFile.length() / 1024 + "k" + "图片尺寸："
                            + getImageSize(photoFile.getPath())[0]
                            + " * " + getImageSize(photoFile.getPath())[1]);
                    compressWithLsNew(photoFile);
                    break;
                case PHOTO:
                    if (data != null) {// 为了取消选取不报空指针用的
//                        Uri uri = data.getData();
//                        String urlPath = uri.getPath();
//
//                        //去除小米5图片返回路径携带/raw/
//                        if (urlPath.contains("/raw/")) {
//                            urlPath = urlPath.replace("/raw/", "");
//                        }

                        Uri selectedImage = data.getData(); //获取系统返回的照片的Uri
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};
                        Cursor cursor = getContentResolver().query(selectedImage,
                                filePathColumn, null, null, null);//从系统表中查询指定Uri对应的照片
                        cursor.moveToFirst();
                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        String picturePath = cursor.getString(columnIndex);  //获取照片路径
                        cursor.close();
                        File file = new File(picturePath);

//                        Log.i("TAG", "--->" + urlPath);
                        Glide.with(MainActivity.this).load(picturePath).centerCrop().crossFade().into(ivShow);
                        tvYuan.setText("原图：图片大小" + file.length() / 1024 + "k" + "图片尺寸："
                                + getImageSize(file.getPath())[0]
                                + " * " + getImageSize(file.getPath())[1]);

                        compressWithLsNew(file);
                    }
                    break;
            }
        }
    }

    /**
     * 压缩图片
     */
    private void compressWithLsNew(File file) {
        String trim = etMaxSize.getText().toString().trim();
        try {
            if (!TextUtils.isEmpty(trim)) {
                maxSize = Integer.parseInt(trim);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        String trim1 = etQuality.getText().toString().trim();
        try {
            if (!TextUtils.isEmpty(trim1)) {
                qualty = Integer.parseInt(trim1);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if(qualty<=0||qualty>100){
            Toast.makeText(this,"压缩质量为大于0小100的整数",Toast.LENGTH_SHORT);
            return;
        }
        Luban.with(this)
                .load(file.getAbsoluteFile())
                .ignoreBy(100)
                .setQuality(qualty)
                .setTargetDir(getExternalFilesDir("imageCache").getAbsolutePath())
                .filter(new CompressionPredicate() {
                    @Override
                    public boolean apply(String path) {
                        return !(TextUtils.isEmpty(path) || path.toLowerCase().endsWith(".gif"));
                    }
                })
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {
                        // TODO 压缩开始前调用，可以在方法内启动 loading UI
                    }

                    @Override
                    public void onSuccess(File file) {
                        // TODO 压缩成功后调用，返回压缩后的图片文件
                        Glide.with(MainActivity.this).load(file).centerCrop().crossFade().into(ivShowCompress);

                        tvChange.setText("压缩后： 压缩的质量：" + qualty + "\n图片大小" + file.length() / 1024 + "k" + "图片尺寸："
                                + getImageSize(file.getPath())[0]
                                + " * " + getImageSize(file.getPath())[1]);
                    }

                    @Override
                    public void onError(Throwable e) {
                        // TODO 当压缩过程出现问题时调用
                    }
                }).launch();
    }

    public int[] getImageSize(String imagePath) {
        int[] res = new int[2];

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inSampleSize = 1;
        BitmapFactory.decodeFile(imagePath, options);

        res[0] = options.outWidth;
        res[1] = options.outHeight;

        return res;
    }

//    private void compressWithRx(File file) {
//        Luban.get(this)
//                .load(file) //加载图片
//                .putGear(Luban.THIRD_GEAR)  //设置压缩等级
//                .asObservable()     //返回一个Obsetvable观察者对象
//                .subscribeOn(Schedulers.io())   //压缩指定IO线程
//                .observeOn(AndroidSchedulers.mainThread())  //回调返回主线程
//                .doOnError(new Action1<Throwable>() {
//                    @Override
//                    public void call(Throwable throwable) {     //运行异常回调
//                        throwable.printStackTrace();
//                    }
//                })
//                .onErrorResumeNext(new Func1<Throwable, Observable<? extends File>>() {
//                    @Override
//                    public Observable<? extends File> call(Throwable throwable) {   //异常处理
//                        return Observable.empty();
//                    }
//                })
//                .subscribe(new Action1<File>() {
//                    @Override
//                    public void call(File file) {
//                        Glide.with(MainActivity.this).load(file).centerCrop().crossFade().into(ivShowCompress);
//
//                        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//                        Uri uri = Uri.fromFile(file);
//                        intent.setData(uri);
//                        MainActivity.this.sendBroadcast(intent);
//
//                        tvChange.setText("压缩后：图片大小" + file.length() / 1024 + "k" + "图片尺寸："
//                                + Luban.get(getApplicationContext()).getImageSize(file.getPath())[0]
//                                + " * " + Luban.get(getApplicationContext()).getImageSize(file.getPath())[1]);
//                    }
//                });
//    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }


    //成功
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.RECORD_AUDIO)) {
//            canGO();
        }

    }

    //失败
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, list)) {
            new AppSettingsDialog.Builder(this, "需要开启摄像头权限，请到应用权限管理中打开权限")
                    .setTitle("权限需求").build().show();
        }
    }

    public static String getFormatSize(double size) {
        double kiloByte = size / 1024;
        if (kiloByte < 1) {
            return size + "Byte(s)";
        }

        double megaByte = kiloByte / 1024;
        if (megaByte < 1) {
            BigDecimal result1 = new BigDecimal(Double.toString(kiloByte));
            return result1.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "KB";
        }

        double gigaByte = megaByte / 1024;
        if (gigaByte < 1) {
            BigDecimal result2 = new BigDecimal(Double.toString(megaByte));
            return result2.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "MB";
        }

        double teraBytes = gigaByte / 1024;
        if (teraBytes < 1) {
            BigDecimal result3 = new BigDecimal(Double.toString(gigaByte));
            return result3.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "GB";
        }
        BigDecimal result4 = new BigDecimal(teraBytes);
        return result4.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "TB";
    }

}
