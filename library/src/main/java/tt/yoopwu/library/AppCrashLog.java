package tt.yoopwu.library;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by YoopWu on 2018/03/15.
 */

public abstract class AppCrashLog implements Thread.UncaughtExceptionHandler {

	private static final String TAG = AppCrashLog.class.getSimpleName();

	private static final int LIMIT_LOG_COUNT = 20;

	public static String LOG_CACHE = Environment.getExternalStorageDirectory() + File.separator;

	private SimpleDateFormat mDateFormat;

	private LinkedHashMap<String, String> crashInfos = new LinkedHashMap<>();

	private Thread.UncaughtExceptionHandler mDefaultExceptionHandler;

	private Context mContext;

	public abstract void initParams();

	public abstract void sendLogToServer(File file);

	public void init(Context context) {
		try {
			this.mContext = context;
			mDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
			initParams();
			Thread.setDefaultUncaughtExceptionHandler(this);
		} catch (Exception e) {
			e.printStackTrace();
			Log.i(TAG, "init: exception with " + e.getMessage());
		}
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		try {
			if (handleException(e) && mDefaultExceptionHandler != null) {
				mDefaultExceptionHandler.uncaughtException(t, e);
			} else {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				android.os.Process.killProcess(android.os.Process.myPid());
				System.exit(1);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
			Log.i(TAG, "uncaughtException: " + e1.getMessage());
		}
	}

	private boolean handleException(Throwable throwable) {
		try {
			if (throwable == null)
				return false;

			// collect device info
			collectDeviceInfo();

			writeCrashExceptionToFile(throwable);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			Log.i(TAG, "handleException: " + e.getMessage());
		}
		return false;
	}

	private void writeCrashExceptionToFile(Throwable throwable) {
		if (throwable == null) {
			return;
		}
		StringBuffer sb = new StringBuffer();
		if (crashInfos != null && crashInfos.size() > 0) {
			for (Map.Entry<String, String> map : crashInfos.entrySet()) {
				sb.append(map.getKey() + ":" + map.getValue() + "\n");
			}
		}

		// 获取异常日志信息
		Writer writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		throwable.printStackTrace(printWriter);
		Throwable cause = throwable.getCause();
		while (cause != null) {
			cause.printStackTrace(printWriter);
			cause = cause.getCause();
		}
		printWriter.close();
		sb.append("Exception:\n");
		sb.append(writer.toString());

		// 设置日志路径与名称
		if (mDateFormat == null) {
			mDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		}
		String tm = mDateFormat.format(new Date());
		// log file name
		String logName = "crash-" + tm + "-" + System.currentTimeMillis() + ".log";
		exceptionWriteToSD(LOG_CACHE, logName, sb.toString());
		logLimitCount(LIMIT_LOG_COUNT);
	}

	private void logLimitCount(int limitLogCount) {
		File file = new File(LOG_CACHE);
		if (file != null && file.isDirectory()) {
			// 过滤文件类型
			File[] files = file.listFiles(new FileLogFilter());
			if (files != null && files.length > 0) {
				Arrays.sort(files, comparator);
				for (int i = 0; i < files.length - limitLogCount; i++) {
					files[i].delete();
				}
			}
		}
	}

	private Comparator<File> comparator = new Comparator<File>() {
		@Override
		public int compare(File left, File right) {
			if (left.lastModified() > right.lastModified()) {
				return 1;
			}
			if (left.lastModified() < right.lastModified()) {
				return -1;
			}
			return 0;
		}
	};

	private class FileLogFilter implements FileFilter {

		@Override
		public boolean accept(File pathname) {
			return pathname != null && pathname.getName().endsWith(".log");
		}
	}

	private void exceptionWriteToSD(String logCache, String logName, String buffer) {
		try {
			File file = new File(logCache);
			if (!file.exists()) {
				file.mkdirs();
			}
			File logFile = new File(file.getAbsolutePath() + File.separator + logName);
			if (!logFile.exists()) {
				logFile.createNewFile();
			}
			BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));
			writer.append(buffer);
			writer.flush();
			writer.close();
			sendLogToServer(logFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void collectDeviceInfo() {
		try {
			if (mContext == null) {
				return;
			}
			PackageManager packageManager = mContext.getPackageManager();
			if (packageManager != null) {
				PackageInfo packageInfo = packageManager.getPackageInfo(mContext.getPackageName(), PackageManager.GET_ACTIVITIES);
				if (packageInfo != null) {
					crashInfos.put("versionName", packageInfo.versionName + "\n");
					crashInfos.put("versionCode", packageInfo.versionCode + "\n");
					crashInfos.put("packageName", packageInfo.packageName + "\n");
				}
			}
			// app info
			crashInfos.put("手机型号", Build.MODEL + "\n");
			crashInfos.put("系统版本", Build.VERSION.SDK + "\n");
			crashInfos.put("Android版本", Build.VERSION.RELEASE + "\n");

			Field[] fields = Build.class.getDeclaredFields();
			if (fields != null && fields.length > 0) {
				for (Field field : fields) {
					if (field != null) {
						field.setAccessible(true);
						crashInfos.put(field.getName(), field.get(null).toString() + "\n");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.i(TAG, "collectDeviceInfo: " + e.getLocalizedMessage());
		}
	}
}
