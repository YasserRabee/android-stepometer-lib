package com.yasrab.stepometer.sensors;

import android.content.Context;

import com.yasrab.stepometer.sensors.StepProvider.StepCounterInterface;
import com.yasrab.stepometer.sensors.StepProvider.StepListener;

public class SoftStepCounter implements StepCounterInterface, StepListener {

	public interface StepDetectorInterface {
		public boolean start(int batchDelayUs);

		public void stop();

		public boolean registerStepListener(StepListener listner,
				int batchDelayUs);

		public boolean unregisterStepListener(StepListener listner);
	}

	private Context mContext;
	private long mNumberOfSteps;
	private int mLastBatchDelayUs;

	private boolean isRegistered;

	private StepDetectorInterface mStepDetector;
	private StepListener mStepListener;

	public SoftStepCounter(Context context) {
		mContext = context;
		if (HardStepDetecor.isSupported(mContext)) {
			mStepDetector = new HardStepDetecor(mContext);
		} else {
			mStepDetector = new SoftStepDetector(mContext);
		}
	}

	@Override
	public boolean start(long lastNumberOfSteps, int batchDelayUs) {
		mNumberOfSteps = lastNumberOfSteps;

		if (isRegistered && mLastBatchDelayUs == batchDelayUs) {
			return true;
		} else if (isRegistered) {
			mStepDetector.unregisterStepListener(SoftStepCounter.this);
		}

		isRegistered = mStepDetector.registerStepListener(SoftStepCounter.this,
				batchDelayUs);
		return isRegistered;
	}

	@Override
	public void stop() {
		mStepDetector.unregisterStepListener(SoftStepCounter.this);
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

	@Override
	public void onNewSteps(long numOfSteps) {
		synchronized (this) {
			mNumberOfSteps += numOfSteps;
		}
		if (mStepListener != null)
			mStepListener.onNewSteps(mNumberOfSteps);
	}

}
