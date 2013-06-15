package com.stevenschoen.putionew.activities;

import java.util.Map;

import net.saik0.android.unifiedpreference.UnifiedPreferenceFragment;
import net.saik0.android.unifiedpreference.UnifiedSherlockPreferenceActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.util.Log;

import com.actionbarsherlock.view.MenuItem;
import com.stevenschoen.putionew.R;

public final class Preferences extends UnifiedSherlockPreferenceActivity
		implements OnSharedPreferenceChangeListener {

	// Check /res/xml/preferences.xml file for this preference
	private static final String PREFERENCE_KEY = "maxCacheSizeMb";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setHeaderRes(R.xml.preferences);
		
		setSharedPreferencesName("putio");
		setSharedPreferencesMode(Context.MODE_PRIVATE);
		
		super.onCreate(savedInstanceState);
		
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		setTitle("Settings");

		// Register for changes (for example only)
		getSharedPreferences(getSharedPreferencesName(), getSharedPreferencesMode())
				.registerOnSharedPreferenceChangeListener(this);
		
		SharedPreferences sharedPrefs = getSharedPreferences(getSharedPreferencesName(), getSharedPreferencesMode());
		
		for (Map.Entry<String,?> entry : sharedPrefs.getAll().entrySet()){
            Log.d("asdf", entry.getKey() + ": " + 
                                   entry.getValue().toString());
		}
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(PREFERENCE_KEY)) {
			sendBroadcast(new Intent(Putio.checkCacheSizeIntent));
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
		case android.R.id.home:
			Intent homeIntent = new Intent(this, Putio.class);
			homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(homeIntent);
			return true;
		}
		return (super.onOptionsItemSelected(menuItem));
	}

	public static class StoragePreferenceFragment extends UnifiedPreferenceFragment {}
}