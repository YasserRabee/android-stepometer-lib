package com.yasrab.stepometer.utl;

import android.content.Context;
import android.widget.Toast;

public class ErrorNotifier {

	private static Context mContext;

	public static void initialize(Context context) {
		mContext = context;
	}

	public static void notifyError(String msg) {
		if (mContext != null)
			Toast.makeText(mContext, "Error: " + msg, Toast.LENGTH_SHORT)
					.show();
	}

	public static void notifyWarning(String msg) {
		if (mContext != null)
			Toast.makeText(mContext, "Warning: " + msg, Toast.LENGTH_SHORT)
					.show();
	}

	public static void notifyException(Exception e, String msg) {
		if (mContext != null)
			Toast.makeText(mContext,
					"Exception: " + msg != null ? msg : e.getMessage(),
					Toast.LENGTH_SHORT).show();
	}
}
