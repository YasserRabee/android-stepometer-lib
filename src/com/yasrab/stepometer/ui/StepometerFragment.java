package com.yasrab.stepometer.ui;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import com.yasrab.stepometer.R;
import com.yasrab.stepometer.service.BoundContext;
import com.yasrab.stepometer.service.BoundContext.BoundContextListener;
import com.yasrab.stepometer.service.StepCounterService;

/**
 * A simple {@link Fragment} subclass.
 * 
 */
public class StepometerFragment extends Fragment implements OnClickListener,
		BoundContextListener {
	/**
	 * Value in meters
	 */
	public static final float AVERAGE_STEP_SPAN = 0.75f;

	public static final String STEPOMETER_STATE = "STEPOMETER_STATE";
	public static final String UNSAVED_STEPS = "UNSAVED_STEPS";
	public static final String IS_BOUND_STATE = "IS_BOUND_STATE";
	public static final String IS_STEP_RUN_STATE = "IS_STEP_RUN_STATE";

	private BoundContext mBoundContext;
	private boolean mIsBound;
	private boolean mIsStepometerRunning;
	private int mStepsCountingMode;

	private Animation mSaveButtonEnterAnimation;
	private Animation mSaveButtonExitAnimation;
	private Button mStartStopButton;
	private Button mSaveButton;
	private TextView mStatusView;

	private long mStepsNumber;
	private float mWalkedDistance;

	public static StepometerFragment newInstance(int stepsCountingMode) {
		StepometerFragment stepometerFragment = new StepometerFragment();
		stepometerFragment.mStepsCountingMode = stepsCountingMode;
		return stepometerFragment;
	}

	public StepometerFragment() {
		// Required empty public constructor
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View rootView = inflater.inflate(R.layout.fragment_stepometer,
				container, false);

		mStartStopButton = (Button) rootView
				.findViewById(R.id.stepometer_start);
		mStartStopButton.setOnClickListener(StepometerFragment.this);
		mSaveButton = (Button) rootView
				.findViewById(R.id.stepometer_save_steps);
		mSaveButton.setOnClickListener(StepometerFragment.this);

		mSaveButtonEnterAnimation = AnimationUtils.loadAnimation(getActivity(),
				android.R.anim.fade_in);
		mSaveButtonExitAnimation = AnimationUtils.loadAnimation(getActivity(),
				android.R.anim.fade_out);
		mSaveButtonExitAnimation.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				mSaveButton.setVisibility(View.GONE);
			}
		});

		mStatusView = (TextView) rootView.findViewById(R.id.stepometer_status);

		long unsavedStepsNumber = getActivity().getSharedPreferences(
				STEPOMETER_STATE, Context.MODE_PRIVATE).getLong(UNSAVED_STEPS,
				0);

		if (getActivity().getSharedPreferences(STEPOMETER_STATE,
				Context.MODE_PRIVATE).getBoolean(IS_STEP_RUN_STATE, false)) {
			startStepometer();
		} else if (unsavedStepsNumber > 0) {
			mStepsNumber = unsavedStepsNumber;
			stopStepometer();
			mSaveButton.setVisibility(View.VISIBLE);
		}

		updateStatus();

		return rootView;
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mBoundContext != null) {
			mBoundContext.unbind();
		}
		getActivity()
				.getSharedPreferences(STEPOMETER_STATE, Context.MODE_PRIVATE)
				.edit().putLong(UNSAVED_STEPS, mStepsNumber).commit();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mBoundContext != null && mIsStepometerRunning) {
			mBoundContext.bind();
		}
	}

	@Override
	public void onViewStateRestored(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			mIsBound = savedInstanceState.getBoolean(IS_BOUND_STATE);
			mIsStepometerRunning = savedInstanceState
					.getBoolean(IS_STEP_RUN_STATE);
			if (mIsBound || mIsStepometerRunning) {
				startStepometer();
			}

		}
		super.onViewStateRestored(savedInstanceState);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(IS_BOUND_STATE, mIsBound);
		outState.putBoolean(IS_STEP_RUN_STATE, mIsStepometerRunning);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onClick(View v) {
		if (v == mStartStopButton) {
			if (mIsStepometerRunning)
				stopStepometer();
			else
				startStepometer();
		} else if (v == mSaveButton) {
			saveTakenSteps();
		}
	}

	private void saveTakenSteps() {
		hideSaveStepsButton();
		mStartStopButton.setText(R.string.stepometer_start);
		getActivity()
				.getSharedPreferences(STEPOMETER_STATE, Context.MODE_PRIVATE)
				.edit().putLong(UNSAVED_STEPS, 0).commit();
		mStepsNumber = 0;
		mWalkedDistance = 0;
		updateStatus();
	}

	private void showSaveStepsButton() {
		mSaveButton.setVisibility(View.VISIBLE);
		mSaveButton.startAnimation(mSaveButtonEnterAnimation);
	}

	private void hideSaveStepsButton() {
		mSaveButton.startAnimation(mSaveButtonExitAnimation);
	}

	private void startStepometer() {
		if (mBoundContext == null)
			mBoundContext = new BoundContext(StepometerFragment.this);

		mBoundContext.startAndBind();
	}

	private void stopStepometer() {
		if (mBoundContext != null) {
			mBoundContext.unbindAndStop();
			mBoundContext = null;
		}
		mIsBound = false;
		mIsStepometerRunning = false;
		mStartStopButton.setText(R.string.stepometer_continue);
		if (mStepsCountingMode == StepCounterService.STEPS_MODE_RESTART
				&& mStepsNumber > 0)
			showSaveStepsButton();
		getActivity()
				.getSharedPreferences(STEPOMETER_STATE, Context.MODE_PRIVATE)
				.edit().putBoolean(IS_STEP_RUN_STATE, mIsStepometerRunning)
				.commit();
	}

	@Override
	public void handleMessage(Message msg) {
		if (msg.what == StepCounterService.RESPONE_STEPS_NUMBER) {
			mStepsNumber = (long) msg.obj;
			updateStatus();
		}
	}

	private void updateStatus() {
		if (mStepsNumber > 0) {
			mWalkedDistance = mStepsNumber * AVERAGE_STEP_SPAN;
			mStatusView.setText(getString(R.string.stepometer_status,
					mStepsNumber, mWalkedDistance));
		} else {
			mStatusView.setText(getString(R.string.stepometer_zero_steps));
		}
	}

	@Override
	public void onServiceConnected(ComponentName className, IBinder service) {
		mBoundContext.startWork(mStepsCountingMode, 0, null);
		mStartStopButton.setText(R.string.stepometer_pause);
		hideSaveStepsButton();

		mStatusView.setText(getString(R.string.stepometer_status, 0, 0f, 0f));
		mIsStepometerRunning = true;
		mIsBound = true;
	}

	@Override
	public void onServiceDisconnected(ComponentName className) {
		mIsBound = false;
		mIsStepometerRunning = false;
	}

	@Override
	public Class<?> getServiceClass() {
		return StepCounterService.class;
	}

	@Override
	public Looper getListenerLooper() {
		return getActivity().getMainLooper();
	}

	@Override
	public Context getContext() {
		return getActivity().getApplicationContext();
	}

}
