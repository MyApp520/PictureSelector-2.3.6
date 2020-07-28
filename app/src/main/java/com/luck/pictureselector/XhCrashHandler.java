package com.luck.pictureselector;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by xh_peng on 2018/2/12.
 */
public class XhCrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "XhCrashHandler";
    private static final String SDCARD_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;

    private static final String CRASH_LOG_FILE_DIR = "pictureSelectorDemo";
    private static final String FILE_NAME_SUFFIX = ".txt";
    private final int MAX_LOG_COUNT = 20;
    private final int MIN_LOG_COUNT = 10;//保存的错误日志文件数量 = MAX_LOG_COUNT - MIN_LOG_COUNT


    private static XhCrashHandler sInstance = new XhCrashHandler();
    private Thread.UncaughtExceptionHandler mDefaultCrasHandler;

    private Context mContext;

    private XhCrashHandler() {
    }

    public static XhCrashHandler getsInstance() {
        return sInstance;
    }

    public void init(Context context) {
        mContext = context.getApplicationContext();
        mDefaultCrasHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * 这是最关键的函数，当程序中有未捕获的异常，系统将会自动调用这个方法，
     * thread为出现未捕获异常的线程，ex为为未捕获的异常，有了这个ex，我们就可以得到
     * 异常信息了
     *
     * @param thread
     * @param ex
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        //如果系统提供了默认的异常处理器，则交给系统去结束程序，否则就由自己结束自己
        if (!catchCrashExection(ex) && mDefaultCrasHandler != null) {
            mDefaultCrasHandler.uncaughtException(thread, ex);
        } else {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 3、在此处 可以干掉所有activity
//            MyApplication.getInstance().exitAll(false);
            // 4、杀死进程
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    private boolean catchCrashExection(Throwable ex) {
        if (ex == null) {
            return false;
        }
        new Thread() {
            public void run() {
                Looper.prepare();
                Toast.makeText(mContext, "程序异常，正在退出。。。", Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
        }.start();

        // 1、上传错误日志到服务器
        uploadExceptionToServer();

        // 2、导出异常信息到SD卡中
        try {
            dumpExceptionToSDCard(ex);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        ex.printStackTrace();
        return true;
    }

    private void dumpExceptionToSDCard(Throwable ex) throws IOException {
        //如果SD卡不存在或无法使用，则无法把异常信息写入SD卡中
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Log.d(TAG, "dumpExceptionToSDCard: SD卡不存在");
            return;
        }
        File rootDir = new File(SDCARD_PATH);
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }

        File errorLogFileDir = new File(SDCARD_PATH + CRASH_LOG_FILE_DIR);
        if (!errorLogFileDir.exists()) {
            errorLogFileDir.mkdirs();
        }

        PrintWriter pw = null;
        try {
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINESE).format(new Date(System.currentTimeMillis()));
            pw = new PrintWriter(new BufferedWriter(new FileWriter(errorLogFileDir + File.separator + time + FILE_NAME_SUFFIX)));
            pw.println(time);
            dumpPhoneInfo(pw);
            pw.println();
            ex.printStackTrace(pw);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, TAG + " dumpExceptionToSDCard: " + e);
        } finally {
            if (pw != null) {
                pw.close();
            }
            //删除之前的异常信息
            deleteFile(errorLogFileDir, false);
        }
    }

    /**
     * 写入手机的基本信息
     *
     * @param pw
     */
    private void dumpPhoneInfo(PrintWriter pw) throws PackageManager.NameNotFoundException {
        PackageManager pm = mContext.getPackageManager();
        PackageInfo pi = pm.getPackageInfo(mContext.getPackageName(), PackageManager.GET_ACTIVITIES);

        pw.println("名称：" + pm.getApplicationLabel(pi.applicationInfo));
        pw.println("包名：" + pi.packageName);
        pw.println("App VersionName: " + pi.versionName);
        pw.println("versionCode: " + pi.versionCode);

        //Android版本号
        pw.print("OS Version: ");
        pw.print(Build.VERSION.RELEASE);
        pw.print('_');
        pw.println(Build.VERSION.SDK_INT);

        //手机制造商
        pw.print("Vendor: ");
        pw.println(Build.MANUFACTURER);

        //手机型号
        pw.print("Model: ");
        pw.println(Build.MODEL);

        //CUP架构
        pw.print("CUP ABI: ");
        pw.println(Build.CPU_ABI);

        //奔溃发生时间
        pw.print("奔溃发生时间 CURRENT DATE: ");
        Date currentDate = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        pw.println(dateFormat.format(currentDate));
    }

    /**
     * 递归删除文件和文件夹
     *
     * @param file         要删除的文件或文件夹
     * @param isDeleteSelf 如果是文件夹，是否删除文件夹本身
     */
    private void deleteFile(File file, boolean isDeleteSelf) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isFile()) {
            file.delete();
            return;
        }
        if (file.isDirectory()) {
            File[] childFileList = file.listFiles();
            if (childFileList == null || childFileList.length < 1) {
                file.delete();
            } else if (childFileList.length > MAX_LOG_COUNT) {
                for (int m = 0; m < MAX_LOG_COUNT - MIN_LOG_COUNT; m++) {
                    deleteFile(childFileList[m], true);
                }
                if (isDeleteSelf) {
                    file.delete();
                }
            }
        }
    }

    private void uploadExceptionToServer() {
        //TODO Upload Exception Message To Your Web Server
    }
}
