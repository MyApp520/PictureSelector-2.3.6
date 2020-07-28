package com.eighteengray.procameralibrary.widget;

import android.content.Context;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;


public class ImageSaver implements Runnable {
    private ImageReader mImageReader;
    private Context context;
    private SaveImageDataListener mSaveImageDataListener;

    public ImageSaver(ImageReader mImageReader, Context c, SaveImageDataListener saveImageDataListener) {
        this.mImageReader = mImageReader;
        this.context = c;
        this.mSaveImageDataListener = saveImageDataListener;
    }

    @Override
    public void run() {
        Image image = null;
        try {
            image = mImageReader.acquireLatestImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);

            if (mSaveImageDataListener != null) {
                mSaveImageDataListener.getImageByteData(bytes);
            }

            // 下面的代码比较费时
//            ImageAvailableEvent.ImagePathAvailable imagePathAvailable = new ImageAvailableEvent.ImagePathAvailable();
//            imagePathAvailable.setImageBitmap(bitmap);
//            EventBus.getDefault().post(imagePathAvailable);

//            // 保存图像到手机图片库中
//            ImageUtils.saveBitmap2Album(context, bitmap);
        } catch (Exception e) {
            e.getStackTrace();
        } finally {
            try {
                if (image != null) {
                    image.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            mSaveImageDataListener = null;
        }
    }


    /**
     * 保存
     *
     * @param bytes
     * @param file
     * @throws IOException
     */
    private void save(byte[] bytes, File file) throws IOException {
        Log.i("JpegSaver", "save");
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            os.write(bytes);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                os.close();
            }
        }
    }

    public interface SaveImageDataListener {
        void getImageByteData(byte[] pictureByteData);
    }
}
