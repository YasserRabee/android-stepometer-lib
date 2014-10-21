package com.yasrab.stepometer.service;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.Message;

import com.yasrab.stepometer.sensors.StepProvider;
import com.yasrab.stepometer.sensors.StepProvider.StepListener;
import com.yasrab.stepometer.utl.ErrorNotifier;

public class StepCounterService extends BoundService implements StepListener {

	public static final int COMMAND_GET_STEPS = 4;
	public static final int RESPONE_STEPS_NUMBER = 4;

	public static final String STEPOMETER_PREF = "STEPOMETER_PREF";
	public static final String NUMBER_OF_STEPS = "NUMBER_OF_STEPS";
	public static final String BATCH_DELAY_US = "BATCH_DELAY_US";

	public static final int STEPS_MODE_CONTINUE = 1;
	public static final int STEPS_MODE_RESTART = 0;

	private int mStepsCountingMode = STEPS_MODE_RESTART;
	private StepProvider mStepProvider;
	private boolean isWorkStarted;

	public StepCounterService() {
		super();
		isWorkStarted = false;
	}

	@Override
	protected void startWork() {
		if (isWorkStarted) {
			return;
		}

		SharedPreferences mSharedPreferences = getSharedPreferences(
				STEPOMETER_PREF, MODE_PRIVATE);

		int batchDelayUs = mSharedPreferences.getInt(BATCH_DELAY_US, 0);
		long lastNumberOfSteps = 0;
		if (mStepsCountingMode == STEPS_MODE_CONTINUE)
			lastNumberOfSteps = mSharedPreferences.getLong(NUMBER_OF_STEPS, 0);
		else if (mStepsCountingMode != STEPS_MODE_RESTART) {
			ErrorNotifier.notifyError("stepsCountingMode can't equal "
					+ mStepsCountingMode);
		}

		mStepProvider = new StepProvider(getApplicationContext());
		if (!mStepProvider.start(lastNumberOfSteps, batchDelayUs)) {
			ErrorNotifier
					.notifyError("StepCounterService, Can't start StepProvider");
			return;
		}
		isWorkStarted = true;
	}

	@Override
	protected void stopWork() {
		if (mStepProvider != null) {
			mStepProvider.stop();
			if (mStepsCountingMode == STEPS_MODE_CONTINUE
					&& !getSharedPreferences(STEPOMETER_PREF, MODE_PRIVATE)
							.edit()
							.putLong(NUMBER_OF_STEPS,
									mStepProvider.getNumberOfSteps()).commit()) {
				ErrorNotifier
						.notifyError("StepCounterService, Can't save numberOfStep");
			}
		}
	}

	@Override
	protected void handleMessage(Message msg) {
		switch (msg.what) {
		case COMMAND_START_WORK:
			mStepsCountingMode = msg.arg1;
			break;
		case COMMAND_GET_STEPS:
			notifySteps(-1);
			break;
		}
	}

	private void notifySteps(long numOfSteps) {
		if (numOfSteps == -1)
			notifyMessage(Message.obtain(null, RESPONE_STEPS_NUMBER,
					mStepProvider.getNumberOfSteps()));
		else
			notifyMessage(Message
					.obtain(null, RESPONE_STEPS_NUMBER, numOfSteps));
	}

	@Override
	public void onNewSteps(long numOfSteps) {
		notifySteps(numOfSteps);
	}

	@Override
	public IBinder onBind(Intent intent) {
		startWork();
		mStepProvider.registerStepListener(StepCounterService.this);
		return super.onBind(intent);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		mStepProvider.unregisterStepListener(StepCounterService.this);
		return super.onUnbind(intent);
	}

}
