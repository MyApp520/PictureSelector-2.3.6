package com.eighteengray.procameralibrary.dataevent;


import android.graphics.Bitmap;
import android.media.ImageReader;

public class ImageAvailableEvent {

    public static class ImageReaderAvailable {
        private ImageReader imageReader;

        public ImageReader getImageReader() {
            return imageReader;
        }

        public void setImageReader(ImageReader imageReader) {
            this.imageReader = imageReader;
        }
    }

    public static class ImagePathAvailable {
        private String imagePath;
        private Bitmap imageBitmap;

        public String getImagePath() {
            return imagePath;
        }

        public void setImagePath(String imagePath) {
            this.imagePath = imagePath;
        }

        public Bitmap getImageBitmap() {
            return imageBitmap;
        }

        public void setImageBitmap(Bitmap imageBitmap) {
            this.imageBitmap = imageBitmap;
        }
    }
}
