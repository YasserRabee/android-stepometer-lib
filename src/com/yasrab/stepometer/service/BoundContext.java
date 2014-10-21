package com.yasrab.stepometer.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.yasrab.stepometer.utl.ErrorNotifier;

public final class BoundContext {

	private Context mContext;
	private BoundContextListener mBoundListener;
	private Messenger mListenerMessenger;
	private Messenger mServiceMessenger;
	private Class<?> mServiceClass;

	private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			mServiceMessenger = new Messenger(service);
			registerMessenger();
			mBoundListener.onServiceConnected(className, service);
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			mServiceMessenger = null;
			mListenerMessenger = null;
			mBoundListener.onServiceDisconnected(className);
		}
	};

	public BoundContext(BoundContextListener boundListener) {
		mContext = boundListener.getContext();
		mServiceClass = boundListener.getServiceClass();
		mBoundListener = boundListener;
	}

	private static final class BoundListenerHandler extends Handler {

		private BoundContextListener mBoundListener;

		public BoundListenerHandler(BoundContextListener boundListener) {
			super(boundListener.getListenerLooper());
			mBoundListener = boundListener;
		}

		@Override
		public void handleMessage(Message msg) {
			mBoundListener.handleMessage(msg);
		}
	}

	public boolean bind() {
		return mContext.bindService(new Intent(mContext, mServiceClass),
				mConnection, Context.BIND_AUTO_CREATE);
	}

	public void unbind() {
		mContext.unbindService(mConnection);
	}

	public boolean startAndBind() {
		if (startBoundService() == null) {
			ErrorNotifier.notifyError("Can't start " + mServiceClass.getName()
					+ " service");
			return false;
		}

		if (!bind()) {
			ErrorNotifier.notifyError("Can't bind " + mServiceClass.getName()
					+ " service");
			return false;
		}

		return true;
	}

	public boolean unbindAndStop() {
		stopWork();
		unbind();
		return stopBoundService();
	}

	public ComponentName startBoundService() {
		return mContext.startService(new Intent(mContext, mServiceClass));
	}

	public boolean stopBoundService() {
		return mContext.stopService(new Intent(mContext, mServiceClass));
	}

	private void postMessage(Message msg) {
		try {
			mServiceMessenger.send(msg);
		} catch (RemoteException e) {
			ErrorNotifier.notifyException(e, null);
		}
	}

	public void startWork(int arg1, int arg2, Object obj) {
		postMessage(Message.obtain(null, BoundService.COMMAND_START_WORK, arg1,
				arg2, obj));
	}

	public void stopWork() {
		postMessage(Message.obtain(null, BoundService.COMMAND_STOP_WORK));
	}

	private void registerMessenger() {
		mListenerMessenger = new Messenger(new BoundListenerHandler(
				mBoundListener));
		Message msg = Message.obtain(null,
				BoundService.COMMAND_REGISTER_MESSENGER);
		msg.replyTo = mListenerMessenger;
		postMessage(msg);
	}

	public static interface BoundContextListener {

		public void onServiceConnected(ComponentName className, IBinder service);

		public Class<?> getServiceClass();

		public void onServiceDisconnected(ComponentName className);

		public void handleMessage(Message msg);

		public Looper getListenerLooper();

		public Context getContext();
	}

}
