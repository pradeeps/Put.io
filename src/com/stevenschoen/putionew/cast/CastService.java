package com.stevenschoen.putionew.cast;

import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;

import com.google.cast.ApplicationChannel;
import com.google.cast.ApplicationMetadata;
import com.google.cast.ApplicationSession;
import com.google.cast.CastContext;
import com.google.cast.CastDevice;
import com.google.cast.ContentMetadata;
import com.google.cast.MediaProtocolCommand;
import com.google.cast.MediaProtocolMessageStream;
import com.google.cast.MediaRouteHelper;
import com.google.cast.SessionError;
import com.stevenschoen.putionew.R;

public class CastService extends Service {
	private final IBinder binder = new CastBinder();

	public class CastBinder extends Binder {
		public CastService getService() {
			return CastService.this;
		}
	}
	
    private CastContext mCastContext;
    private CastDevice mCastDevice;
    private ApplicationSession mSession;
    private CastMedia mMedia;
    private ContentMetadata mMetaData;
    private MediaProtocolMessageStream mMessageStream;
    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private MediaProtocolCommand mStatus;
    MyMediaRouterCallback mMediaRouterCallback;
    
    @Override
    public void onCreate() {
    	super.onCreate();
    	
    	try {
            mCastContext = new CastContext(getApplicationContext());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        mMetaData = new ContentMetadata();
        mMediaRouter = MediaRouter.getInstance(getApplicationContext());
        mMediaRouteSelector = MediaRouteHelper
                .buildMediaRouteSelector(MediaRouteHelper.CATEGORY_CAST);
        mMediaRouterCallback = new MyMediaRouterCallback(this);
        getMediaRouter().addCallback(getMediaRouteSelector(), mMediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	super.onStartCommand(intent, flags, startId);
        
    	return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
    	try {
			mSession.endSession();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	mCastContext.dispose();
    	
    	super.onDestroy();
    }
    
	public void openSession() {
	    mSession = new ApplicationSession(getCastContext(), getCastDevice());
	
	    // TODO: The below lines allow you to specify either that your application uses the default
	    // implementations of the Notification and Lock Screens, or that you will be using your own.
	    int flags = 0;
	
	    // Comment out the below line if you are not writing your own Notification Screen.
	    // flags |= ApplicationSession.FLAG_DISABLE_NOTIFICATION;
	
	    // Comment out the below line if you are not writing your own Lock Screen.
	    // flags |= ApplicationSession.FLAG_DISABLE_LOCK_SCREEN_REMOTE_CONTROL;
	    mSession.setApplicationOptions(flags);
	
	    Log.d("asdf", "Beginning session with context: " + mCastContext);
	    Log.d("asdf", "The session to begin: " + mSession);
	    mSession.setListener(new com.google.cast.ApplicationSession.Listener() {
	
	        @Override
	        public void onSessionStarted(ApplicationMetadata appMetadata) {
	        	Log.d("asdf", "Getting channel after session start");
	            ApplicationChannel channel = mSession.getChannel();
	            if (channel == null) {
	                Log.e("asdf", "channel = null");
	                return;
	            }
	            Log.d("asdf", "Creating and attaching Message Stream");
	            mMessageStream = new MediaProtocolMessageStream();
	            channel.attachMessageStream(mMessageStream);
	
	            if (mMessageStream.getPlayerState() == null) {
	                if (mMedia != null) {
	                    loadMedia(mMedia);
	                }
	            } else {
	                Log.d("asdf", "Found player already running");
	            }
	        }
	
	        @Override
	        public void onSessionStartFailed(SessionError error) {
	            Log.e("asdf", "onStartFailed " + error);
	        }
	
	        @Override
	        public void onSessionEnded(SessionError error) {
	            Log.i("asdf", "onEnded " + error);
	        }
	    });
	    
	    try {
	        Log.d("asdf", "Starting session with app name " + getString(R.string.cast_name));
	        
	        getSession().startSession(getString(R.string.cast_name));
	    } catch (IOException e) {
	        Log.e("asdf", "Failed to open session", e);
	    }
	}



	public void loadMedia(CastMedia media) {
			Log.d("asdf", "Loading selected media on device");
			mMetaData.setTitle(media.getTitle());
			try {
				MediaProtocolCommand cmd = mMessageStream.loadMedia(media.getUrl(), mMetaData, true);
				cmd.setListener(new MediaProtocolCommand.Listener() {
	
					@Override
					public void onCompleted(MediaProtocolCommand mPCommand) {
						Log.d("asdf", "Load completed - starting playback");
	//					onSetVolume(0.5);
					}
	
					@Override
					public void onCancelled(MediaProtocolCommand mPCommand) {
						Log.d("asdf", "Load cancelled");
					}
				});
	
			} catch (IllegalStateException e) {
				Log.e("asdf", "Problem occurred with MediaProtocolCommand during loading", e);
			} catch (IOException e) {
				Log.e("asdf", "Problem opening MediaProtocolCommand during loading", e);
			}
		}



	/**
	 * Returns the CastContext associated with this application's context.
	 */
	public CastContext getCastContext() {
	    return mCastContext;
	}



	/**
	 * Returns the currently selected device, or null if no device is selected.
	 */
	public CastDevice getCastDevice() {
	    return mCastDevice;
	}



	/**
	 * Sets the currently selected device.
	 */
	public void setCastDevice(CastDevice device) {
	    mCastDevice = device;
	}



	public ApplicationSession getSession() {
		return mSession;
	}



	public CastMedia getCastMedia() {
		return mMedia;
	}



	public void setCastMedia(CastMedia media) {
		mMedia = media;
	}



	public MediaProtocolMessageStream getMessageStream() {
		return mMessageStream;
	}



	public void setMessageStream(MediaProtocolMessageStream stream) {
		this.mMessageStream = stream;
	}



	public MediaRouter getMediaRouter() {
		return mMediaRouter;
	}



	public void setMediaRouter(MediaRouter mMediaRouter) {
		this.mMediaRouter = mMediaRouter;
	}



	public MediaRouteSelector getMediaRouteSelector() {
		return mMediaRouteSelector;
	}



	public void setMediaRouteSelector(MediaRouteSelector mMediaRouteSelector) {
		this.mMediaRouteSelector = mMediaRouteSelector;
	}



	public MediaProtocolCommand getStatus() {
		try {
			return mMessageStream.requestStatus();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.d("asdf", "onBind");
		return binder;
	}
}