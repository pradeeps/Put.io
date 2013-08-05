package com.stevenschoen.putionew.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.example.castsample.mediaroutedialog.SampleMediaRouteDialogFactory;
import com.google.cast.ApplicationSession;
import com.google.cast.CastContext;
import com.google.cast.CastDevice;
import com.google.cast.MediaProtocolCommand;
import com.google.cast.MediaProtocolMessageStream;
import com.google.cast.MediaRouteAdapter;
import com.google.cast.MediaRouteHelper;
import com.google.cast.MediaRouteStateChangeListener;
import com.stevenschoen.putionew.PutioFileData;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.cast.CastMedia;
import com.stevenschoen.putionew.cast.CastService;
import com.stevenschoen.putionew.cast.CastService.CastBinder;
import com.stevenschoen.putionew.fragments.FileDetails;

public class FileDetailsActivity extends ActionBarActivity implements FileDetails.Callbacks,
		MediaRouteAdapter {
	
	CastService castService;
	
	private MediaRouteButton mMediaRouteButton;
    private TextView mStatusText;
	
	private FileDetails fileDetailsFragment;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		PutioFileData fileData = (PutioFileData) getIntent().getExtras().getParcelable("fileData");
		
		setContentView(R.layout.filedetailsphone);
		
		if (savedInstanceState == null) {
			FragmentManager fragmentManager = getSupportFragmentManager();
			FragmentTransaction fragmentTransaction = fragmentManager
					.beginTransaction();
			Bundle fileDetailsBundle = new Bundle();
			fileDetailsBundle.putParcelable("fileData", fileData);
			fileDetailsFragment = (FileDetails) FileDetails.instantiate(
					this, FileDetails.class.getName(), fileDetailsBundle);
			fragmentTransaction.add(R.id.DetailsHolder, fileDetailsFragment);
			fragmentTransaction.commit();
		} else {
			fileDetailsFragment = (FileDetails) getSupportFragmentManager().findFragmentById(R.id.DetailsHolder);
		}
		
		getSupportActionBar().setTitle(fileData.name);
		
		startCastService();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.cast, menu);
		
		MenuItem buttonCast = menu.findItem(R.id.menu_castbutton);
		mMediaRouteButton = (MediaRouteButton) MenuItemCompat.getActionView(buttonCast);
		mMediaRouteButton.setRouteSelector(castService.getMediaRouteSelector());
        mMediaRouteButton.setDialogFactory(new SampleMediaRouteDialogFactory());
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {		
		switch (menuItem.getItemId()) {
		case android.R.id.home:
			Intent homeIntent = new Intent(getApplicationContext(), Putio.class);
			homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(homeIntent);
			return true;
		}
		return (super.onOptionsItemSelected(menuItem));
	}
	
	private void initCast() {		
		MediaRouteHelper.registerMinimalMediaRouteProvider(castService.getCastContext(), this);
		
		Log.d("asdf", "callback added");
	}
	
	@Override
	protected void onSaveInstanceState (Bundle outState) {
	    super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onPause() {
		unbindService(mConnection);
		
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

//	This won't ever be called
	@Override
	public void onFDCancelled() { }
	
//	Neither will this
	@Override
	public void onFDFinished() { }

	
	@Override
	public boolean onFDPlay(String url) {
//		Returns true if the fragment should play it, false if casting
		if (castService.getCastDevice() == null) {
			return true;
		} else {
			castService.loadMedia(new CastMedia("title", url));
			return false;
		}
	}
	
	@Override
	public void onDeviceAvailable(CastDevice device, String deviceName,
			MediaRouteStateChangeListener listener) {
		castService.setCastDevice(device);
		
        Log.d("asdf", "Available device found: " + deviceName);
        castService.openSession();
	}

	@Override
	public void onSetVolume(double volume) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onUpdateVolume(double volume) {
		// TODO Auto-generated method stub
	}
	
	private void startCastService() {
		Intent castServiceIntent = new Intent(this, CastService.class);
		startService(castServiceIntent);
		bindService(castServiceIntent, mConnection, 0);
	}

	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			CastBinder binder = (CastBinder) service;
			castService = binder.getService();
			initCast();
			Log.d("asdf", "onServiceConnected");
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
		}
	};
}