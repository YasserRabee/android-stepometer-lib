package com.yasrab.stepometer.sensors;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;

import com.yasrab.stepometer.sensors.StepProvider.StepCounterInterface;
import com.yasrab.stepometer.sensors.StepProvider.StepListener;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class HardStepCounter implements StepCounterInterface,
		SensorEventListener {

	private Context mContext;
	private long mNumberOfSteps;
	private SensorManager sManager;
	private int mLastBatchDelayUs;

	private boolean isRegistered;
	private boolean isFirst;
	private long mStepsOffset;

	StepListener mStepListener;

	public HardStepCounter(Context context) {
		mContext = context;
		sManager = (SensorManager) mContext
				.getSystemService(Context.SENSOR_SERVICE);
		isRegistered = false;
		isFirst = true;
	}

	@Override
	public boolean start(long lastNumberOfSteps, int batchDelayUs) {

		mNumberOfSteps = lastNumberOfSteps;
		mStepsOffset -= lastNumberOfSteps;

		if (isRegistered && batchDelayUs == mLastBatchDelayUs) {
			return true;
		} else if (isRegistered) {
			sManager.unregisterListener(HardStepCounter.this);
		}
		mLastBatchDelayUs = batchDelayUs;

		// // Sleep the thread to insure that sensor has released
		// try {
		// Thread.sleep(100);
		// } catch (InterruptedException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

		isRegistered = sManager.registerListener(HardStepCounter.this,
				sManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER),
				SensorManager.SENSOR_DELAY_NORMAL, batchDelayUs);

		return isRegistered;
	}

	@Override
	public void stop() {
		sManager.unregisterListener(HardStepCounter.this);
		isRegistered = false;
	}

	@Override
	public long getSteps() {
		synchronized (this) {
			return mNumberOfSteps;
		}
	}

	@Override
	public boolean registerStepListener(StepListener listener) {
		if (listener == mStepListener)
			return true;
		if (mStepListener != null)
			return false;

		mStepListener = listener;
		return false;
	}

	@Override
	public boolean unregisterStepListener(StepListener listener) {
		if (mStepListener != listener)
			return false;

		mStepListener = null;
		return false;
	}

	@SuppressLint("InlinedApi")
	public static boolean isSupported(Context context) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) // 19
			return false;

		PackageManager pm = context.getPackageManager();
		return pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {

		if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
			long numOfSteps = (long) event.values[0];
			if (isFirst) {
				mStepsOffset += numOfSteps;
				isFirst = false;
				return;
			}

			synchronized (this) {
				mNumberOfSteps = numOfSteps - mStepsOffset;
			}
			if (mStepListener != null)
				mStepListener.onNewSteps(mNumberOfSteps);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

}
