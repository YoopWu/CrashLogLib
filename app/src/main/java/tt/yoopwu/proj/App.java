package tt.yoopwu.proj;

import android.app.Application;

import tt.yoopwu.library.AppCrashHandler;

/**
 * Created by YoopWu on 2018/3/15 0015.
 */

public class App extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		AppCrashHandler.getInstance().init(this);
	}
}
