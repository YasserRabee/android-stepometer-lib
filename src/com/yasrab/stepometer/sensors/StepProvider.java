package com.yasrab.stepometer.sensors;

import android.content.Context;

public class StepProvider {

	/**
	 * Value in meters
	 */
	public static final float AVERAGE_STEP_SPAN = 0.75f;

	public interface StepListener {
		public void onNewSteps(long numOfSteps);
	}

	public interface StepCounterInterface {
		public boolean start(long lastNumberOfSteps, int batchDelayUs);

		public void stop();

		public long getSteps();

		public boolean registerStepListener(StepListener listener);

		public boolean unregisterStepListener(StepListener listener);
	}

	private final Context mContext;
	private StepCounterInterface mStepCounter;

	public StepProvider(Context context) {
		mContext = context;
		if (HardStepCounter.isSupported(mContext)) {
			mStepCounter = new HardStepCounter(mContext);
		} else {
			mStepCounter = new SoftStepCounter(mContext);
		}
	}

	public boolean start(long lastNumberOfSteps, int batchDelayUs) {
		return mStepCounter.start(lastNumberOfSteps, batchDelayUs);
	}

	public void stop() {
		mStepCounter.stop();
	}

	public long getNumberOfSteps() {
		return mStepCounter.getSteps();
	}

	public boolean registerStepListener(StepListener listener) {
		return mStepCounter.registerStepListener(listener);
	}

	public boolean unregisterStepListener(StepListener listener) {
		return mStepCounter.unregisterStepListener(listener);
	}

}
