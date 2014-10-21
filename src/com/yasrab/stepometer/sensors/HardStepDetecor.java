package com.yasrab.stepometer.sensors;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;

import com.yasrab.stepometer.sensors.SoftStepCounter.StepDetectorInterface;
import com.yasrab.stepometer.sensors.StepProvider.StepListener;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class HardStepDetecor implements StepDetectorInterface,
		SensorEventListener {

	private final Context mContext;
	private final SensorManager sManager;

	private StepListener stepListner;

	public HardStepDetecor(Context context) {
		mContext = context;
		sManager = (SensorManager) mContext
				.getSystemService(Context.SENSOR_SERVICE);
	}

	public static boolean isSupported(Context context) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
			return false;

		return context.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_SENSOR_STEP_DETECTOR);
	}

	@Override
	public boolean start(int batchDelayUs) {
		return sManager.registerListener(HardStepDetecor.this,
				sManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR),
				SensorManager.SENSOR_DELAY_NORMAL, batchDelayUs);
	}

	@Override
	public void stop() {
		sManager.unregisterListener(HardStepDetecor.this);
	}

	@Override
	public boolean registerStepListener(StepListener listner, int batchDelayUs) {
		if (stepListner == listner)
			return true;

		if (stepListner != null)
			return false;

		if (!start(batchDelayUs))
			return false;

		stepListner = listner;
		return true;
	}

	@Override
	public boolean unregisterStepListener(
			com.yasrab.stepometer.sensors.StepProvider.StepListener listner) {
		if (stepListner != listner)
			return false;

		stop();

		stepListner = null;
		return true;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		stepListner.onNewSteps(event.values.length);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

}
