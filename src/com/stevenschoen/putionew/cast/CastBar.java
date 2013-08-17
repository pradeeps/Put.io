package com.stevenschoen.putionew.cast;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.stevenschoen.putionew.R;

import de.ankri.views.AutoScaleTextView;

public class CastBar extends Fragment {
	
	public interface Callbacks {
        public CastService getCastService();
    }
    
	private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public CastService getCastService() { return null; }
    };
    
    private Callbacks mCallbacks = sDummyCallbacks;
	
	private AutoScaleTextView textStatus, textPlaying;
	private ImageButton buttonPlayPause;

	private Handler updateStatusHandler;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        Log.d("asdf", "Starting statusRunner thread");
        updateStatusHandler = new Handler();
        updateStatusHandler.post(new Runnable() {
			@Override
			public void run() {
				try {
	                updateStatus();
	                updateStatusHandler.postDelayed(this, 1500);
	            } catch (Exception e) {
	                Log.e("asdf", "Thread interrupted: " + e);
	            }
			}
        });
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.castbar, container, false);
		
		textStatus = (AutoScaleTextView) view.findViewById(R.id.cast_status);
		
		textPlaying = (AutoScaleTextView) view.findViewById(R.id.cast_playing);
		
		buttonPlayPause = (ImageButton) view.findViewById(R.id.cast_play);
		OnClickListener playListener = new OnClickListener() {

			@Override
			public void onClick(View arg0) {
//				play
				buttonPlayPause.setImageResource(R.drawable.ic_cast_pause);
			}
		};
		OnClickListener pauseListener = new OnClickListener() {

			@Override
			public void onClick(View arg0) {
//				pause
				buttonPlayPause.setImageResource(R.drawable.ic_cast_play);
			}
		};
		
		
		return view;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		mCallbacks = (Callbacks) activity;
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		
		mCallbacks = sDummyCallbacks;
	}
	
	/**
     * Updates the status of the currently playing video in the dedicated message view.
     */
    public void updateStatus() {
    	getActivity().runOnUiThread(new Runnable() {
    		@Override
            public void run() {
                try {
                    updateCurrentlyPlaying();

                    if (mCallbacks.getCastService().getMessageStream() != null) {
                    	mCallbacks.getCastService().requestNewStatus();

                        String currentStatus = "Player State: "
                                + mCallbacks.getCastService()
                                .getMessageStream().getPlayerState() + "\n";
                        currentStatus += "Device " + mCallbacks.getCastService()
                        		.getCastDevice().getFriendlyName() + "\n";
                        currentStatus += "Title " + mCallbacks.getCastService()
                        		.getMessageStream().getTitle() + "\n";
                        currentStatus += "Current Position: "
                                + mCallbacks.getCastService()
                        		.getMessageStream().getStreamPosition() + "\n";
                        currentStatus += "Duration: "
                                + mCallbacks.getCastService()
                        		.getMessageStream().getStreamDuration() + "\n";
                        currentStatus += "Volume set at: "
                                + (mCallbacks.getCastService()
                                		.getMessageStream().getVolume() * 100) + "%\n";
                        currentStatus += "requestStatus: " + mCallbacks.getCastService()
                        		.getStatus().getType() + "\n";
                        textStatus.setText(currentStatus);
                    } else {
                    	textStatus.setText("tap icon to select a cast device");
                    }
                } catch (Exception e) {
                    Log.e("asdf", "Status request failed: " + e);
                }
            }
        });
    }
    
    protected void updateCurrentlyPlaying() {
        String playing = "";
        if (mCallbacks.getCastService().getCastMedia().getTitle() != null) {
            playing = "Media Selected: " + mCallbacks.getCastService().getCastMedia().getTitle();
            if (mCallbacks.getCastService().getMessageStream() != null) {
                String colorString = "<br><font color=#0066FF>";
                colorString += "Casting to " + mCallbacks.getCastService().getCastDevice().getFriendlyName();
                colorString += "</font>";
                playing += colorString;
            }
            textPlaying.setText(Html.fromHtml(playing));
        } else {
            String castString = "<font color=#FF0000>";
            castString += "nothing selected";
            castString += "</font>";
            textPlaying.setText(Html.fromHtml(castString));
        }
    }
    
    @Override
    public void onDestroy() {
    	updateStatusHandler.removeCallbacks(null);
    	
    	super.onDestroy();
    }
}