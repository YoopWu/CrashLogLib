package tt.yoopwu.library;

import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * Created by YoopWu on 2018/03/15.
 */

public class AppCrashHandler extends AppCrashLog {

	private static final String TAG = AppCrashHandler.class.getSimpleName();

	private static AppCrashHandler sCrashHandler;

	public static AppCrashHandler getInstance() {
		if (sCrashHandler == null) {
			sCrashHandler = new AppCrashHandler();
		}
		return sCrashHandler;
	}

	@Override
	public void initParams() {
		Log.i(TAG, "initParams: ");
		LOG_CACHE = Environment.getExternalStorageDirectory() + File.separator + "log";
	}

	@Override
	public void sendLogToServer(File file) {
		Log.i(TAG, "sendLogToServer: " + file.getName());
	}
}
