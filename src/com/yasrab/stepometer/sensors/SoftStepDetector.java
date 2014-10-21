/*
 *  Pedometer - Android App
 *  Copyright (C) 2009 Levente Bagi
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.yasrab.stepometer.sensors;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;

import com.yasrab.stepometer.sensors.SoftStepCounter.StepDetectorInterface;
import com.yasrab.stepometer.sensors.StepProvider.StepListener;

/**
 * Represents a software step detector based on accelerometer
 * 
 * @author Levente Bagi Edited by YasserRabee
 */
public class SoftStepDetector implements StepDetectorInterface,
		SensorEventListener {
	private final static float[] sensitivityMap = { 1.97f, 2.96f, 4.44f, 6.66f,
			10.00f, 15.00f, 22.50f, 33.75f, 50.62f };
	private float mLimit;
	private float mLastValues[] = new float[3 * 2];
	private float mScale[] = new float[2];
	private float mYOffset;

	private float mLastDirections[] = new float[3 * 2];
	private float mLastExtremes[][] = { new float[3 * 2], new float[3 * 2] };
	private float mLastDiff[] = new float[3 * 2];
	private int mLastMatch = -1;

	private final Context mContext;
	private final SensorManager sManager;

	private StepListener stepListner;

	public SoftStepDetector(Context context) {
		mContext = context;
		sManager = (SensorManager) mContext
				.getSystemService(Context.SENSOR_SERVICE);

		setSensitivity(6);
		mYOffset = 240;
		mScale[0] = -(mYOffset * (1.0f / (SensorManager.STANDARD_GRAVITY * 2)));
		mScale[1] = -(mYOffset * (1.0f / (SensorManager.MAGNETIC_FIELD_EARTH_MAX)));
	}

	/**
	 * Set sensitivity of the detector
	 * 
	 * @param sensitivity
	 *            Takes values from 1 to 9; 1 is minimum, 5 is default.
	 */
	public void setSensitivity(int sensitivity) {
		if (sensitivity < 0 || sensitivity > sensitivityMap.length)
			return;
		mLimit = sensitivityMap[sensitivity - 1];
	}

	/**
	 * Check if the passed accelerometer values is a step.
	 * 
	 * @param values
	 *            accelerometer sample values
	 * @return true if it is a step, false otherwise
	 */
	private boolean isStep(float[] values) {
		boolean isStep = false;
		float vSum = 0;
		for (int i = 0; i < 3; i++) {
			final float v = mYOffset + values[i] * mScale[1];
			vSum += v;
		}
		int k = 0;
		float v = vSum / 3;

		float direction = (v > mLastValues[k] ? 1 : (v < mLastValues[k] ? -1
				: 0));
		if (direction == -mLastDirections[k]) {
			// Direction changed
			int extType = (direction > 0 ? 0 : 1); // minumum or
													// maximum?
			mLastExtremes[extType][k] = mLastValues[k];
			float diff = Math.abs(mLastExtremes[extType][k]
					- mLastExtremes[1 - extType][k]);

			if (diff > mLimit) {

				boolean isAlmostAsLargeAsPrevious = diff > (mLastDiff[k] * 2 / 3);
				boolean isPreviousLargeEnough = mLastDiff[k] > (diff / 3);
				boolean isNotContra = (mLastMatch != 1 - extType);

				if (isAlmostAsLargeAsPrevious && isPreviousLargeEnough
						&& isNotContra) {
					isStep = true;
					mLastMatch = extType;
				} else {
					mLastMatch = -1;
				}
			}
			mLastDiff[k] = diff;
		}
		mLastDirections[k] = direction;
		mLastValues[k] = v;
		return isStep;
	}

	@SuppressLint("NewApi")
	@Override
	public boolean start(int batchDelayUss) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
			return sManager.registerListener(SoftStepDetector.this,
					sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
					SensorManager.SENSOR_DELAY_NORMAL);
		else
			return sManager.registerListener(SoftStepDetector.this,
					sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
					SensorManager.SENSOR_DELAY_NORMAL, batchDelayUss);
	}

	@Override
	public void stop() {
		sManager.unregisterListener(SoftStepDetector.this);
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
	public boolean unregisterStepListener(StepListener listner) {
		if (stepListner != listner)
			return false;

		stop();

		stepListner = null;
		return true;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (isStep(event.values))
			stepListner.onNewSteps(1);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

}