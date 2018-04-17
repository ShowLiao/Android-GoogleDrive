package com.example.user.canvaspainter;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import static java.lang.StrictMath.abs;

class Panel extends View {
    List<PointF> points=new ArrayList<PointF>();

    Bitmap vBitmap;
    Canvas vBitmapCanvas;
    Paint mpaint = new Paint();

    final String STR_SD_CARD = "/Android/";
    final String STR_GOOGLE_DRIVE = "googleDrive";
    final String STR_FILE_NAME = "fileName";

    public Panel(Context context) {
        super(context);
        mpaint.setColor(Color.RED);
        mpaint.setStyle(Paint.Style.STROKE);
        mpaint.setStrokeWidth(10);

        //取得手機解析度
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(dm);

        //設定bitmap大小
        vBitmap = Bitmap.createBitmap(dm.widthPixels, dm.heightPixels, Bitmap.Config.RGB_565);
        vBitmapCanvas = new Canvas(vBitmap);
        vBitmapCanvas.drawColor(Color.WHITE);

    }

    @Override
    public void onDraw (Canvas canvas){
        super.onDraw(canvas);
        for (int i = 1; i < points.size(); i++) {
            PointF p1 = points.get(i - 1);
            PointF p2 = points.get(i);
            if(abs(p1.x-p2.x)<50 && abs(p1.y-p2.y)<50) {
                canvas.drawLine(p1.x, p1.y, p2.x, p2.y, mpaint);
                vBitmapCanvas.drawLine(p1.x, p1.y, p2.x, p2.y, mpaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent (MotionEvent event){
        for (int i = 0; i < event.getHistorySize(); i++) {
            points.add(new PointF(event.getHistoricalX(i), event.getHistoricalY(i)));
        }
        Panel.this.invalidate();
        return true;
    }

    //Reset
    public void resetCanvas() {
        points.clear();
        Panel.this.invalidate();
        vBitmapCanvas.drawColor(Color.WHITE);
    }

    //save as picture
    public void savePicture(String fileName){

//        Log.e("** file", STR_SD_CARD + fileName);
//        try{
//            FileOutputStream fout = new FileOutputStream(STR_SD_CARD + fileName);
//            vBitmap.compress(Bitmap.CompressFormat.PNG,100,fout);
//            fout.close();
//
////            showDlg();
            sendIntent(fileName);
//        }catch(Exception x){
//            x.printStackTrace();
//        }

    }

    private void sendIntent(String fileName) {

        Intent intent = new Intent();
        intent.setAction(STR_GOOGLE_DRIVE);
        intent.putExtra(STR_FILE_NAME, fileName);
        getContext().sendBroadcast(intent);

    }

    public void showQRCodeDlg(String url) {
        Dialog qr = new Dialog(getContext());
        qr.setContentView(R.layout.qrcode_layout);
        qr.setTitle("QR Code");
        Log.e("showDlg", "showLog");
        ImageView qrCode = (ImageView)qr.findViewById(R.id.imgView_qrcode);

        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();

        BitMatrix bitMatrix = null;
        try {
            bitMatrix = multiFormatWriter.encode(url, BarcodeFormat.QR_CODE,200,200);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            qrCode.setImageBitmap(bitmap);

        } catch (WriterException e) {
            e.printStackTrace();
        }

        qr.show();
    }


}