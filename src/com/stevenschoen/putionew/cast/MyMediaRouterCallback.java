package com.stevenschoen.putionew.cast;

import java.io.IOException;

import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;

import com.google.cast.MediaRouteHelper;

public class MyMediaRouterCallback extends MediaRouter.Callback {
	private CastService cast;
	
	public MyMediaRouterCallback(CastService cast) {
		super();
		
		this.cast = cast;
	}

	@Override
	public void onRouteSelected(MediaRouter router, RouteInfo route) {
		MediaRouteHelper.requestCastDeviceForRoute(route);
	}

	@Override
	public void onRouteUnselected(MediaRouter router, RouteInfo route) {
		try {
			if (cast.getSession() != null) {
				Log.d("asdf", "Ending session and stopping application");
				cast.getSession().setStopApplicationWhenEnding(true);
				cast.getSession().endSession();
			} else {
				Log.e("asdf", "onRouteUnselected: mSession is null");
			}
		} catch (IllegalStateException e) {
			Log.e("asdf", "onRouteUnselected:");
			e.printStackTrace();
		} catch (IOException e) {
			Log.e("asdf", "onRouteUnselected:");
			e.printStackTrace();
		}
		cast.setMessageStream(null);
		cast.setCastDevice(null);
	}
}