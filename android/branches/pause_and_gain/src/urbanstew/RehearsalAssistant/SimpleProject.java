package urbanstew.RehearsalAssistant;

import java.util.Timer;
import java.util.TimerTask;

import urbanstew.RehearsalAssistant.Rehearsal.Sessions;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class SimpleProject extends ProjectBase
{
	static long getSessionId(ContentResolver resolver, long projectId)
	{
        // a simple project must have exactly one session
        Cursor cursor = resolver.query(Sessions.CONTENT_URI, sessionsProjection, Sessions.PROJECT_ID + "=" + projectId, null,
                Sessions.DEFAULT_SORT_ORDER);
        // add the session if it is not there
        if(cursor.getCount() < 1)
        {
        	Log.d("Rehearsal Assistant", "Inserting Session for Simple Project ID: " + projectId);
        	ContentValues values = new ContentValues();
        	values.put(Sessions.PROJECT_ID, projectId);
        	values.put(Sessions.TITLE, "Simple Session");
      		values.put(Sessions.START_TIME, 0);
      		resolver.insert(Sessions.CONTENT_URI, values);
        	cursor.requery();
        }
        long sessionId;
        if(cursor.getCount() < 1)
        {
        	Log.w("Rehearsal Assistant", "Can't create session for simple project ID: " + projectId);
        	sessionId=-1;
        }
        else
        {
        	cursor.moveToFirst();
        	sessionId = cursor.getLong(SESSIONS_ID);
        }
        cursor.close();
        return sessionId;
	}
    public void onCreate(Bundle savedInstanceState)
    {
        setContentView(R.layout.simple);

        super.onCreate(savedInstanceState);
        super.setSimpleProject(true);
        
        mRecordButton = (ImageButton) findViewById(R.id.button);
        mRecordButton.setOnClickListener(mClickListener);
        mCurrentTime = (TextView) findViewById(R.id.playback_time);
        mEnvelopeView = (VolumeEnvelopeView) findViewById(R.id.volume_envelope);
        mViewSwitcher = (ViewSwitcher) findViewById(R.id.view_switcher);
        mGainSlider = (SeekBar) findViewById(R.id.gain_slider);
        mGainSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

			public void onProgressChanged(SeekBar bar, int progress, boolean fromUser)
			{
				if(mRecordService != null)
					try
					{
						mRecordService.setGain((progress - 512) / 8.0);
					} catch (RemoteException e)
					{
					}
			}

			public void onStartTrackingTouch(SeekBar arg0)
			{
			}

			public void onStopTrackingTouch(SeekBar bar)
			{
			}
        
        });
        ((ImageButton)findViewById(R.id.record_pause_button)).setOnClickListener(new OnClickListener()
        {

			public void onClick(View arg0)
			{
				if(mRecordService != null)
					try
					{
						mRecordService.pauseRecording(mSessionId);
					} catch (RemoteException e)
					{
					}				
			}
        });
        
        mSessionId = getSessionId(getContentResolver(), projectId());
        if(mSessionId < 0)
        {
    		Toast.makeText(this, "There was a problem opening a Simple Project.", Toast.LENGTH_LONG).show();
        	finish();
        }
        mSessionPlayback = new SessionPlayback(savedInstanceState, this, ContentUris.withAppendedId(Sessions.CONTENT_URI, mSessionId));
        scrollToEndOfList();
        
        bindService(new Intent(IRecordService.class.getName()),
                mServiceConnection, Context.BIND_AUTO_CREATE);
        
        ((ListView)findViewById(R.id.annotation_list)).getAdapter()
        	.registerDataSetObserver(new DataSetObserver()
        	{
        		public void onChanged()
        		{
        			reviseInstructions();
        			if(mUpdateListSelection)
        				scrollToEndOfList();
        			mUpdateListSelection = false;
        		}
        	}
        	);

        reviseInstructions();
    }
            
    public void onResume()
    {
    	super.onResume();
    	mSessionPlayback.onResume();
    	try
    	{
    		updateInterface();
    	} catch (RemoteException e)
    	{}
    	
    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    	mVolumeEnvelopeEnabled = preferences.getBoolean("recording_waveform", true);
    	
		if(mRecordService != null)
			try
			{
				mRecordService.setSession(mSessionId);
			} catch (RemoteException e)
			{
			}

    	mCurrentTimeTask = new TimerTask()
    		{
    			public void run()
    			{
    				SimpleProject.this.runOnUiThread(new Runnable()
    				{
    					public void run()
    					{
    						if(mRecordService != null)
    				        try
    				        {
    							if(mRecordService.getState() == RecordService.State.RECORDING.ordinal())
    							{
    								mCurrentTime.setText(mSessionPlayback.playTimeFormatter().format(mRecordService.getTimeInRecording()));
    								if(mVolumeEnvelopeEnabled)
    									mEnvelopeView.setNewVolume(mRecordService.getMaxAmplitude());
    								return;
    							}
    				    	} catch (RemoteException e)
    				    	{
    				    	}
    				    	if(mVolumeEnvelopeEnabled)
    				    		mEnvelopeView.clearVolume();
    					}
    				});              
    			}
    		};
		mTimer.scheduleAtFixedRate(
				mCurrentTimeTask,
				0,
				100);
    }

    public void onPause()
    {
    	mCurrentTimeTask.cancel();
    	mSessionPlayback.onPause();
    	
    	super.onPause();
    }

    public void onDestroy()
    {
    	mTimer.cancel();
    	unbindService(mServiceConnection);
    	mSessionPlayback.onDestroy();

    	super.onDestroy();
    }
    
    
    
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
    	super.onRestoreInstanceState(savedInstanceState);
    	mSessionPlayback.onRestoreInstanceState(savedInstanceState);
    }

    protected void onSaveInstanceState(Bundle outState)
    {
    	super.onSaveInstanceState(outState);
    	mSessionPlayback.onSaveInstanceState(outState);
    }
    
    public boolean onCreateOptionsMenu(Menu menu)
    {
        mSessionPlayback.onCreateOptionsMenu(menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	if(!super.onOptionsItemSelected(item))
    	{
			if(item == mHelpMenuItem)
			{
				Request.notification(this, "Instructions", getResources().getString(R.string.simple_instructions));
				return true;
			}
			else
				return mSessionPlayback.onOptionsItemSelected(item);
    	}
    	return true;
    }
    
    public void onPlaybackStarted()
    {
		if(mRecordService == null)
			return;

		try
		{
	    	if(mRecordService.getState() == RecordService.State.RECORDING.ordinal())
	    	{
	    		mRecordService.toggleRecording(mSessionId);
	    		updateInterface();
	    	}
		} catch (RemoteException e)
		{}
    }
	public boolean onContextItemSelected(MenuItem item)
	{
		return mSessionPlayback.onContextItemSelected(item);
	}

	void reviseInstructions()
	{
    	TextView noAnnotations = (TextView)findViewById(R.id.no_annotations);
        if(mSessionPlayback.annotationsCursor().getCount() == 0)
        {
        	noAnnotations.setText(getResources().getString(R.string.simple_no_annotations_instructions));
        	noAnnotations.setVisibility(View.VISIBLE);
        }
        else
        	noAnnotations.setVisibility(View.INVISIBLE);
	}

	void scrollToEndOfList()
	{
        ListView list = (ListView)findViewById(R.id.annotation_list);
        list.setSelection(list.getCount()-1);
    }
	
    void updateInterface() throws RemoteException
    {
		if(mRecordService == null)
			return;

    	if(mRecordService.getState() == RecordService.State.STARTED.ordinal())
    	{
    		mRecordButton.setImageResource(R.drawable.media_record);
        	mViewSwitcher.setDisplayedChild(0);
        	mUpdateListSelection = true;
        	runOnUiThread(new Runnable()
    		{
    			public void run()
    			{
    		    	scrollToEndOfList();
    			}
    		});
        }
    	else
    	{
    		mRecordButton.setImageResource(R.drawable.media_recording);
        	mViewSwitcher.setDisplayedChild(1);
    	}
    }
    /** Called when the button is pushed */
    View.OnClickListener mClickListener = new View.OnClickListener()
    {
        public void onClick(View v)
        {
        	if(mRecordService == null)
        		return;
	        try
	        {
	        	mSessionPlayback.stopPlayback();
	        	mRecordService.toggleRecording(mSessionId);
	        	updateInterface();
	    	} catch (RemoteException e)
	    	{
	    		
	    	}
        }
    };
    
    TimerTask mCurrentTimeTask;
	
    /**
     * Class for interacting with the secondary interface of the service.
     */
    private ServiceConnection mServiceConnection = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
        	mRecordService = IRecordService.Stub.asInterface(service);
        	try
        	{
        		mRecordService.setSession(mSessionId);
        		updateInterface();
                mRecordButton.setClickable(true);
        	} catch (RemoteException e)
        	{}
        }

        public void onServiceDisconnected(ComponentName className) {
        	mRecordService = null;
            mRecordButton.setClickable(false);
        }
    };

    IRecordService mRecordService = null;

    TextView mCurrentTime;
    VolumeEnvelopeView mEnvelopeView;
    
    Timer mTimer = new Timer();
    
    ImageButton mRecordButton;
    ViewSwitcher mViewSwitcher;
    SeekBar mGainSlider;
    
    long mSessionId;
    
    SessionPlayback mSessionPlayback;
    
    boolean mUpdateListSelection = false;
    boolean mVolumeEnvelopeEnabled;

}
