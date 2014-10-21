package com.yasrab.stepometer.service;

import com.yasrab.stepometer.utl.ErrorNotifier;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

public abstract class BoundService extends Service {

	protected static final int START_MODE = START_STICKY;

	public static final int COMMAND_START_WORK = 1;
	public static final int COMMAND_STOP_WORK = 2;
	public static final int COMMAND_REGISTER_MESSENGER = 3;

	protected Messenger notificationMessenger;
	protected Messenger serviceMessenger;
	protected HandlerThread serviceThread;
	protected Looper serviceLooper;
	protected BoundServiceHandler serviceHandler;

	protected static final class BoundServiceHandler extends Handler {
		BoundService mBoundService;

		public BoundServiceHandler(Looper serviceLooper,
				BoundService boundService) {
			super(serviceLooper);
			mBoundService = boundService;
		}

		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
			case COMMAND_START_WORK:
				if (msg.replyTo == null) {
					// TODO Error
					return;
				}
				mBoundService.startWork();
				break;

			case COMMAND_STOP_WORK:
				mBoundService.stopWork();
				break;
			case COMMAND_REGISTER_MESSENGER:
				mBoundService.notificationMessenger = msg.replyTo;
				break;
			}

			mBoundService.handleMessage(msg);
		}
	}

	public BoundService() {
		super();
		serviceThread = new HandlerThread("StepsCounterServiceThread");
		serviceThread.start();
		serviceLooper = serviceThread.getLooper();
		serviceHandler = new BoundServiceHandler(serviceLooper,
				BoundService.this);
		serviceMessenger = new Messenger(serviceHandler);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return serviceMessenger.getBinder();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_MODE;
	}

	@SuppressLint("NewApi")
	@Override
	public void onDestroy() {
		stopWork();
		serviceMessenger = null;
		serviceHandler = null;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2)
			serviceLooper.quit();
		else
			serviceLooper.quitSafely();
		serviceLooper = null;
		serviceThread.interrupt();
		serviceThread = null;
	}

	protected abstract void startWork();

	protected abstract void stopWork();

	protected abstract void handleMessage(Message msg);

	protected final void notifyMessage(Message msg) {
		try {
			notificationMessenger.send(msg);
		} catch (RemoteException e) {
			ErrorNotifier.notifyException(e, null);
		}
	}
}
