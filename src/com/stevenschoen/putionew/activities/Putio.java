package com.stevenschoen.putionew.activities;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;

import java.io.File;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.astuetz.viewpager.extensions.PagerSlidingTabStrip;
import com.example.castsample.mediaroutedialog.SampleMediaRouteDialogFactory;
import com.google.cast.ApplicationSession;
import com.google.cast.CastContext;
import com.google.cast.CastDevice;
import com.google.cast.MediaProtocolCommand;
import com.google.cast.MediaProtocolMessageStream;
import com.google.cast.MediaRouteAdapter;
import com.google.cast.MediaRouteHelper;
import com.google.cast.MediaRouteStateChangeListener;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.view.ViewHelper;
import com.stevenschoen.putionew.PutioNotification;
import com.stevenschoen.putionew.PutioTransferData;
import com.stevenschoen.putionew.PutioTransfersService;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.SwipeDismissTouchListener;
import com.stevenschoen.putionew.UIUtils;
import com.stevenschoen.putionew.cast.CastMedia;
import com.stevenschoen.putionew.cast.CastService;
import com.stevenschoen.putionew.cast.CastService.CastBinder;
import com.stevenschoen.putionew.fragments.Account;
import com.stevenschoen.putionew.fragments.CastBar;
import com.stevenschoen.putionew.fragments.FileDetails;
import com.stevenschoen.putionew.fragments.Files;
import com.stevenschoen.putionew.fragments.Transfers;

public class Putio extends ActionBarActivity implements
		ActionBar.TabListener, Files.Callbacks, FileDetails.Callbacks, Transfers.Callbacks, CastBar.Callbacks,
		MediaRouteAdapter {
	
	SectionsPagerAdapter mSectionsPagerAdapter;
	
	ViewPager mViewPager;
	
	int requestCode;
	
	SharedPreferences sharedPrefs;
	
	Bundle savedInstanceState;
	
	public static final String invalidateListIntent = "com.stevenschoen.putionew.invalidatelist";
	public static final String checkCacheSizeIntent = "com.stevenschoen.putionew.checkcachesize";
	public static final String fileDownloadUpdateIntent = "com.stevenschoen.putionew.filedownloadupdate";
	public static final String transfersUpdateIntent = "com.stevenschoen.putionew.transfersupdate";
	public static final String noNetworkIntent = "com.stevenschoen.putionew.nonetwork";
	
	boolean init = false;
	boolean initCast = false;
	
	CastService castService;
    private MediaRouteButton mMediaRouteButton;
    private MediaRouter.Callback mMediaRouterCallback;
	
	Account accountFragment;
	Files filesFragment;
	FileDetails fileDetailsFragment;
	Transfers transfersFragment;
	
	private String titleAccount;
	private String titleFiles;
	private String titleTransfers;
	private String[] titles;
	
	private View tabletAccountView;
	private View tabletFilesView;
	private View tabletTransfersView;
	private int accountId;
	private int filesId;
	private int fileDetailsId;
	private int transfersId;
	
	private PutioNotification[] notifs;
	
	PutioUtils utils;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		this.savedInstanceState = savedInstanceState;
		
		if (UIUtils.isTablet(this)) {
			getSupportActionBar().setDisplayShowTitleEnabled(false);
		}
		
		titleAccount = getString(R.string.account);
		titleFiles = getString(R.string.files);
		titleTransfers = getString(R.string.transfers);
		titles = new String[] {titleAccount, titleFiles, titleTransfers};
		
		if (!sharedPrefs.getBoolean("loggedIn", false)) {
			Intent setupIntent = new Intent(this, Setup.class);
			startActivityForResult(setupIntent, requestCode);
		} else {
			init();
		}
		
		if (getIntent() != null) {
			handleIntent(getIntent());
		}
		
		IntentFilter intentFilter1 = new IntentFilter(
				Putio.invalidateListIntent);
		IntentFilter intentFilter2 = new IntentFilter(
				Putio.checkCacheSizeIntent);
		IntentFilter intentFilter3 = new IntentFilter(
				Putio.fileDownloadUpdateIntent);
		IntentFilter intentFilter4 = new IntentFilter(
				Putio.transfersUpdateIntent);
		IntentFilter intentFilter5 = new IntentFilter(
				Putio.noNetworkIntent);
		
		registerReceiver(invalidateReceiver, intentFilter1);
		registerReceiver(checkCacheSizeReceiver, intentFilter2);
		if (UIUtils.isTablet(this)) {
			registerReceiver(fileDownloadUpdateReceiver, intentFilter3);
		}
		registerReceiver(transfersUpdateReceiver, intentFilter4);
		registerReceiver(noNetworkReceiver, intentFilter5);
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		handleIntent(intent);
	}
	
	private void handleIntent(Intent intent) {
		if (intent.getAction() != null) {
	        if (intent.getAction().matches(Intent.ACTION_SEARCH) && sharedPrefs.getBoolean("loggedIn", false)) {
	            String query = intent.getStringExtra(SearchManager.QUERY);
	            filesFragment.initSearch(query);
	        }
		}
	}
	
	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction fragmentTransaction) {
	}
	
	@Override
	public void onTabSelected(Tab tab, FragmentTransaction fragmentTransaction) {
		switch (tab.getPosition()) {
		case 0: setContentView(tabletAccountView); break;
		case 1: setContentView(tabletFilesView); break;
		case 2: setContentView(tabletTransfersView); break;
		}
	}
	
	@Override
	public void onTabReselected(Tab tab, FragmentTransaction fragmentTransaction) {
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		try {
			outState.putInt("currentTab", getSupportActionBar().getSelectedTab().getPosition());
		} catch (NullPointerException e) {
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (init) {
			getMenuInflater().inflate(R.menu.putio, menu);
			
			if (initCast) {
				MenuItem buttonCast = menu.findItem(R.id.menu_castbutton);
				mMediaRouteButton = (MediaRouteButton) MenuItemCompat.getActionView(buttonCast);
				mMediaRouteButton.setRouteSelector(getMediaRouteSelector());
		        mMediaRouteButton.setDialogFactory(new SampleMediaRouteDialogFactory());
			}
	        
			MenuItem buttonAdd = menu.findItem(R.id.menu_addtransfers);
			buttonAdd.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				
				public boolean onMenuItemClick(MenuItem item) {
					Intent addTransferActivityIntent = new Intent(Putio.this, AddTransfers.class);
					startActivity(addTransferActivityIntent);
					return false;
				}
			});
			
			MenuItem buttonSettings = menu.findItem(R.id.menu_settings);
			buttonSettings.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				
				public boolean onMenuItemClick(MenuItem item) {
					Intent settingsIntent = new Intent(Putio.this, Preferences.class);
					Putio.this.startActivity(settingsIntent);
					return false;
				}
			});
			
			MenuItem buttonLogout = menu.findItem(R.id.menu_logout);
			buttonLogout.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				
				public boolean onMenuItemClick(MenuItem item) {
					logOut();
					return false;
				}
			});
			
			MenuItem buttonAbout = menu.findItem(R.id.menu_about);
			buttonAbout.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				
				public boolean onMenuItemClick(MenuItem item) {
					Intent aboutIntent = new Intent(Putio.this, AboutActivity.class);
					startActivity(aboutIntent);
					return false;
				}
			});
			return true;
		}
		
		return false;
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the primary sections of the app.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {
		FragmentManager fm;
		
		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
			this.fm = fm;
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
			case 0:
				accountFragment = Account.newInstance();
				return accountFragment;
			case 1:
				filesFragment = Files.newInstance();
				return filesFragment;
			case 2:
				transfersFragment = Transfers.newInstance();
				return transfersFragment;
			}
			return null;
		}

		private String makeFragmentName(int viewId, int index) {
			return "android:switcher:" + viewId + ":" + index;
		}
		
		@Override
		public int getCount() {
			return 3;
		}
		
		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case 0:
				return titleAccount;
			case 1:
				return titleFiles;
			case 2:
				return titleTransfers;
			}
			return null;
		}
	}
	
	private void init() {
		init = true;
		startCastService();
		
		String token = sharedPrefs.getString("token", null);
		utils = new PutioUtils(token, sharedPrefs);
		
		transfersServiceIntent = new Intent(this, PutioTransfersService.class);
		
		if (UIUtils.isTablet(this)) {
			setupTabletLayout();
		} else {
			setupPhoneLayout();
		}
		
		int navItem = 1;
		if (savedInstanceState != null) {
			navItem = savedInstanceState.getInt("currentTab");
		}
		selectTab(navItem);
		
		class NotificationTask extends AsyncTask<Void, Void, PutioNotification[]> {

			@Override
			protected PutioNotification[] doInBackground(Void... nothing) {
				try {
					InputStream is = utils.getNotificationsJsonData();
					String string = PutioUtils.convertStreamToString(is);
					
					JSONObject json = new JSONObject(string);
					
					JSONArray notifications = json.getJSONArray("notifications");
					notifs = new PutioNotification[notifications.length()];
					for (int i = 0; i < notifications.length(); i++) {
						JSONObject obj = notifications.getJSONObject(i);
//						prime numbers yay
						boolean show = sharedPrefs.getInt("readNotifs", 1) % obj.getInt("id") != 0;
						notifs[i] = new PutioNotification(obj.getInt("id"), obj.getString("text"),
								show);
					}
					return notifs;
				} catch (Exception e) {
					return null;
				}
			}
			
			@SuppressLint("NewApi")
			@Override
			protected void onPostExecute(final PutioNotification[] result) {
				if (result != null) {
					for (int i = 0; i < result.length; i++) {
						if (result[i].show) {
							final LinearLayout ll = (LinearLayout) getWindow().getDecorView().
									findViewById(R.id.layout_main_root);
							final View notifView = getLayoutInflater().inflate(R.layout.notification, null);
							TextView textNotifTitle = (TextView) notifView.findViewById(
									R.id.text_main_notificationtitle);
							TextView textNotifBody = (TextView) notifView.findViewById(
									R.id.text_main_notificationbody);
							textNotifBody.setText(result[i].text);
							ImageButton buttonNotifDismiss = (ImageButton) notifView.findViewById(
									R.id.button_main_closenotification);
							
							final int ii = i;
							
							buttonNotifDismiss.setOnClickListener(new OnClickListener() {
								
								@Override
								public void onClick(View v) {
									animate(notifView)
				                        .translationX(notifView.getWidth())
				                        .alpha(0)
				                        .setDuration(getResources().getInteger(
								                android.R.integer.config_shortAnimTime))
				                        .setListener(new AnimatorListenerAdapter() {
				                            @Override
				                            public void onAnimationEnd(Animator animation) {
				                            	sharedPrefs.edit().putInt("readNotifs",
					                        			sharedPrefs.getInt("readNotifs", 1) * result[ii].id).commit();
				                            	ll.removeView(notifView);
				                            	result[ii].show = false;
				                            	NotificationTask.this.onPostExecute(result);
				                            }
				                        });
								}
							});
							
							notifView.setOnTouchListener(new SwipeDismissTouchListener(
				                    notifView,
				                    null,
				                    new SwipeDismissTouchListener.OnDismissCallback() {
				                    	
				                        @Override
				                        public void onDismiss(View view, Object token) {
			                        	sharedPrefs.edit().putInt("readNotifs",
			                        			sharedPrefs.getInt("readNotifs", 1) * result[ii].id).commit();
				                            ll.removeView(notifView);
				                            result[ii].show = false;
				                            NotificationTask.this.onPostExecute(result);
				                        }
				                    }));
							
							if (UIUtils.hasHoneycomb()) {
								final LayoutTransition transitioner = new LayoutTransition();					
						        ll.setLayoutTransition(transitioner);
							}
							
							ll.addView(notifView, 0);
							break;
						}
					}
				}
			}
		}
		new NotificationTask().execute();
	}
	
	public void logOut() {
		sharedPrefs.edit().remove("token").remove("loggedIn").commit();
		finish();
		startActivity(getIntent());
	}
	
	private void setupPhoneLayout() {
		// Create the adapter that will return a fragment for each of the three
		// primary sections
		// of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());
		
		tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
		ViewHelper.setAlpha(tabs, 0);
		tabs.setShouldExpand(true);
		tabs.setTabBackground(R.drawable.putio_tab_indicator);
		tabs.setTextColor(Color.BLACK);
		tabs.setIndicatorColorResource(R.color.putio_accent);
		
		String accountFragmentName = mSectionsPagerAdapter.makeFragmentName(R.id.pager, 0);
		accountFragment = (Account) getSupportFragmentManager().findFragmentByTag(accountFragmentName);
		String filesFragmentName = mSectionsPagerAdapter.makeFragmentName(R.id.pager, 1);
		filesFragment = (Files) getSupportFragmentManager().findFragmentByTag(filesFragmentName);
		String transfersFragmentName = mSectionsPagerAdapter.makeFragmentName(R.id.pager, 2);
		transfersFragment = (Transfers) getSupportFragmentManager().findFragmentByTag(transfersFragmentName);
		
		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setOffscreenPageLimit(3);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		
		tabs.setViewPager(mViewPager);
		animate(tabs).alpha(1).setDuration(200);
		selectTab(1);
	}
	
	private void setupTabletLayout() {
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		
		// Account
		int tabletAccountLayoutId = R.layout.tablet_account;
		
		tabletAccountView = getLayoutInflater().inflate(tabletAccountLayoutId, null);
		accountId = R.id.fragment_account;
		
		accountFragment = (Account) getSupportFragmentManager().findFragmentById(R.id.fragment_account);
		
		// Files
		int tabletFilesLayoutId = R.layout.tablet_files;
		if (!UIUtils.hasHoneycomb() && PutioUtils.dpFromPx(this, getResources().getDisplayMetrics().heightPixels) >= 600) {
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
				tabletFilesLayoutId = R.layout.tablet_filesgb600port;
			} else {
				tabletFilesLayoutId = R.layout.tablet_filesgb600;
			}
		} else if (!UIUtils.hasHoneycomb()) {
			tabletFilesLayoutId = R.layout.tablet_filesgb600;
		}
		
		tabletFilesView = getLayoutInflater().inflate(tabletFilesLayoutId, null);
		filesId = R.id.fragment_files;
		fileDetailsId = tabletFilesView.findViewById(R.id.fragment_details).getId();
		
		filesFragment = (Files) getSupportFragmentManager().findFragmentById(R.id.fragment_files);
		
		if (getSupportFragmentManager().findFragmentById(R.id.fragment_details) != null) {
			fileDetailsFragment = (FileDetails) getSupportFragmentManager().findFragmentById(R.id.fragment_details);
		}
		
		// Transfers
		int tabletTransfersLayoutId = R.layout.tablet_transfers;
		if (!UIUtils.hasHoneycomb()) {
			tabletTransfersLayoutId = R.layout.tablet_transfersgb;
		}
		
		tabletTransfersView = getLayoutInflater().inflate(tabletTransfersLayoutId, null);
		transfersId = R.id.fragment_transfers;
		
		transfersFragment = (Transfers) getSupportFragmentManager().findFragmentById(R.id.fragment_transfers);
		
		// Other		
		for (int i = 0; i < 3; i++) {
			getSupportActionBar().addTab(getSupportActionBar().newTab()
					.setText(titles[i])
					.setTabListener(this));
		}
	}
	
	public void showFilesAndHighlightFile(int parentId, int id) {
		selectTab(1);
		filesFragment.highlightFile(parentId, id);
	}
	
	@Override
	public void onFileSelected(int id) {
		Bundle fileDetailsBundle = new Bundle();
		fileDetailsBundle.putParcelable("fileData", filesFragment.getFileAtId(id));
		fileDetailsFragment = (FileDetails) FileDetails.instantiate(
			this, FileDetails.class.getName(), fileDetailsBundle);
		
		getSupportFragmentManager()
				.beginTransaction()
				.setCustomAnimations(R.anim.slide_in_left,
						R.anim.slide_out_right)
				.replace(fileDetailsId, fileDetailsFragment).commit();
	}
	
	@Override
	public void onSomethingSelected() {
		if (fileDetailsFragment != null) {
			if (fileDetailsFragment.isAdded()) {
				removeFD(true);
			}
		}
	}
	
	@Override
	public void onFDCancelled() {
		if (UIUtils.isTablet(this)) {
			removeFD(R.anim.slide_out_right);
		}
	}

	@Override
	public void onFDFinished() {
		if (UIUtils.isTablet(this)) {
			removeFD(R.anim.slide_out_left);
		}
	}
	
	@Override
	public void transfersReady() {
		if (!isTransfersServiceRunning()) {
			startService(new Intent(this, PutioTransfersService.class));
		}
	}
	
	@Override
	public void onTransferSelected(int parentId, int id) {
		showFilesAndHighlightFile(parentId, id);
	}
	
	private void removeFD(int exitAnim) {
		filesFragment.setFileChecked(fileDetailsFragment.getFileId(), false);
		getSupportFragmentManager().beginTransaction()
				.setCustomAnimations(R.anim.slide_in_left, exitAnim)
				.remove(fileDetailsFragment).commit();
	}
	
	private void removeFD(boolean askIfSave) {
		if (askIfSave && !fileDetailsFragment.getOldFilename().equals(fileDetailsFragment.getNewFilename())) {
			final Dialog confirmChangesDialog = utils.confirmChangesDialog(this, fileDetailsFragment.getOldFilename());
			confirmChangesDialog.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface arg0) {
					removeFD(R.anim.slide_out_left);
				}
			});
			
			confirmChangesDialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface arg0) {
					removeFD(R.anim.slide_out_right);
				}
			});
			
			Button apply = (Button) confirmChangesDialog.findViewById(R.id.button_confirm_apply);
			apply.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					utils.applyFileToServer(Putio.this,
							fileDetailsFragment.getFileId(),
							fileDetailsFragment.getOldFilename(),
							fileDetailsFragment.getNewFilename());
					confirmChangesDialog.dismiss();
				}
			});
			
			Button cancel = (Button) confirmChangesDialog.findViewById(R.id.button_confirm_cancel);
			cancel.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					confirmChangesDialog.cancel();
				}
			});
			
			confirmChangesDialog.show();
		} else {
			removeFD(R.anim.slide_out_right);
		}
	}
	
	private BroadcastReceiver invalidateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			filesFragment.invalidateList();
		}
	};
	
	private BroadcastReceiver checkCacheSizeReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			checkCacheSize();
		}
	};
	
	private BroadcastReceiver fileDownloadUpdateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (fileDetailsFragment.getFileId() == intent.getExtras().getInt("id")) {
				fileDetailsFragment.updatePercent(intent.getExtras().getInt("percent"));
			}
		}
	};
	
	private BroadcastReceiver transfersUpdateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Parcelable[] transferParcelables = intent.getExtras().getParcelableArray("transfers");

			PutioTransferData[] transfers = new PutioTransferData[transferParcelables.length];
			for (int i = 0; i < transferParcelables.length; i++) {
				transfers[i] = (PutioTransferData) transferParcelables[i];
			}
			
			if (transfersFragment != null) {
				transfersFragment.updateTransfers(transfers);
				transfersFragment.setHasNetwork(true);
			}
			
			if (intent.getBooleanExtra("changed", false)) {
				accountFragment.invalidateAccountInfo();
				filesFragment.invalidateList();
			}
		}
	};
	
	private BroadcastReceiver noNetworkReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			transfersFragment.setHasNetwork(false);
		}
	};
	
	private Intent transfersServiceIntent;

	private PagerSlidingTabStrip tabs;
	
	@Override
	public void onBackPressed() {
		if (UIUtils.isTablet(this)) {
			if (filesFragment.getCurrentFolderId() == 0) {
				super.onBackPressed();
			} else {
				filesFragment.goBack();
			}
		} else {
			if (hasWindowFocus()) {
				if (filesFragment.getCurrentFolderId() == 0) {
					super.onBackPressed();
				} else {
					filesFragment.goBack();
				}
			}
		}
	}
	
	private void selectTab(int position) {
		if (UIUtils.isTablet(this)) {
			getSupportActionBar().setSelectedNavigationItem(position);
		} else {
			if (mViewPager.getCurrentItem() != position) mViewPager.setCurrentItem(position, false);
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if (castService == null) startCastService();
	}
	
	@Override
	protected void onDestroy() {
		unregisterReceiver(invalidateReceiver);
		unregisterReceiver(checkCacheSizeReceiver);
		if (UIUtils.isTablet(this)) {
			unregisterReceiver(fileDownloadUpdateReceiver);
		}
		unregisterReceiver(transfersUpdateReceiver);
		unregisterReceiver(noNetworkReceiver);
		if (isTransfersServiceRunning()) {
			stopService(transfersServiceIntent);
		}
		
		unbindService(mConnection);
		
	    super.onDestroy();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			init();
		} else {
			finish();
		}
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		if (!hasFocus && isTransfersServiceRunning()) {
			stopService(transfersServiceIntent);
		} else if (hasFocus && !isTransfersServiceRunning() && transfersFragment != null) {
			startService(transfersServiceIntent);
		}
		super.onWindowFocusChanged(hasFocus);
	}
	
	private boolean isTransfersServiceRunning() {
	    ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if ("com.stevenschoen.putionew.PutioTransfersService".equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}

	public void checkCacheSize() {
		int maxSize = sharedPrefs.getInt("maxCacheSizeMb", 20);
		File cache = getCacheDir();
		if (FileUtils.sizeOf(cache) >= (FileUtils.ONE_MB * maxSize)) {
			File[] cached = cache.listFiles();
			
			for (int i = 0; i < cached.length; i++) {
				if (!cached[i].getName().matches("0")) {
					FileUtils.deleteQuietly(cached[i]);
				}
			}
		}
	}
	
	private void initCast() {
		initCast = true;
		
        MediaRouteHelper.registerMinimalMediaRouteProvider(getCastContext(), this);
        
        supportInvalidateOptionsMenu();
	}
	
	private void initCastBar() {
		if (getSupportFragmentManager().findFragmentById(R.id.holder_castbar) == null) {
			getSupportFragmentManager().beginTransaction().replace(
	        		R.id.holder_castbar, CastBar.instantiate(this, CastBar.class.getName())).commit();
		}
	}
	
	private void startCastService() {
		Intent castServiceIntent = new Intent(this, CastService.class);
		startService(castServiceIntent);
		bindService(castServiceIntent, mConnection, 0);
	}
	
	public boolean getCanCast() {
		return (getCastDevice() != null);
	}

	@Override
	public void onDeviceAvailable(CastDevice device, String deviceName,
			MediaRouteStateChangeListener listener) {
		setCastDevice(device);
		
        Log.d("asdf", "Available device found: " + deviceName);
        castService.openSession();
        
        initCastBar();
	}

	@Override
	public void onSetVolume(double volume) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onUpdateVolume(double volume) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public boolean onFDPlay(String url) {
//		Returns true if the fragment should play it, false if casting
		if (getCastDevice() == null) {
			return true;
		} else {
			castService.loadMedia(new CastMedia("title", url));
			return false;
		}
	}
	
	private CastContext getCastContext() {
		return castService.getCastContext();
	}
	
	public CastDevice getCastDevice() {
		return castService.getCastDevice();
	}
	
	public void setCastDevice(CastDevice cd) {
		castService.setCastDevice(cd);
	}
	
	public ApplicationSession getSession() {
		return castService.getSession();
	}
	
	public CastMedia getCastMedia() {
		return castService.getCastMedia();
	}
	
	public void setCastMedia(CastMedia media) {
		castService.setCastMedia(media);
	}
	
	public MediaProtocolMessageStream getMessageStream() {
		return castService.getMessageStream();
	}
	
	public void setMessageStream(MediaProtocolMessageStream stream) {
		castService.setMessageStream(stream);
	}
	
	public MediaRouter getMediaRouter() {
		return castService.getMediaRouter();
	}
	
	public void setMediaRouter(MediaRouter router) {
		castService.setMediaRouter(router);
	}
	
	public MediaRouteSelector getMediaRouteSelector() {
		return castService.getMediaRouteSelector();
	}
	
	public void setMediaRouteSelector(MediaRouteSelector selector) {
		castService.setMediaRouteSelector(selector);
	}
	
	public MediaProtocolCommand getStatus() {
		return castService.getStatus();
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			CastBinder binder = (CastBinder) service;
			castService = binder.getService();
			
			initCast();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
		}
	};

	@Override
	public CastService getCastService() {
		return castService;
	}
}