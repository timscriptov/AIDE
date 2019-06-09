package com.aide.ndk;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Messenger;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.appfour.nativelibemptyarm.BuildConfig;
import com.google.android.vending.expansion.downloader.DownloadProgressInfo;
import com.google.android.vending.expansion.downloader.DownloaderClientMarshaller;
import com.google.android.vending.expansion.downloader.DownloaderServiceMarshaller;
import com.google.android.vending.expansion.downloader.Helpers;
import com.google.android.vending.expansion.downloader.IDownloaderClient;
import com.google.android.vending.expansion.downloader.IDownloaderService;
import com.google.android.vending.expansion.downloader.IStub;
//import android.os.Build;
//import android.Manifest;
//import android.content.pm.PackageManager;
//import android.provider.Settings;

public class AideNdkDownloaderActivity extends Activity implements IDownloaderClient
{
    private static final String LOG_TAG = "LVLDownloader";
    private TextView mAverageSpeed;
    private View mCellMessage;
    private View mDashboard;
    private IStub mDownloaderClientStub;
    private ProgressBar mPB;
    private Button mPauseButton;
    private TextView mProgressFraction;
    private TextView mProgressPercent;
    private IDownloaderService mRemoteService;
    private int mState;
    private boolean mStatePaused;
    private TextView mStatusText;
    private TextView mTimeRemaining;
    private Button mWiFiSettingsButton;

    private void setState(int newState)
	{
        if (mState != newState)
		{
            mState = newState;
            mStatusText.setText(Helpers.getDownloaderStringResourceIDFromState(newState));
        }
    }

    private void setButtonPausedState(boolean paused)
	{
        mStatePaused = paused;
        mPauseButton.setText(paused ? R.string.text_button_resume : R.string.text_button_pause);
    }

    private void initializeDownloadUI()
	{
        mDownloaderClientStub = DownloaderClientMarshaller.CreateStub(this, AideNdkDownloaderService.class);
        setContentView(R.layout.main);
		
		//if (Build.VERSION.SDK_INT >= 23)
		//{
		//	if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Settings.ACTION_MANAGE_OVERLAY_PERMISSION) != PackageManager.PERMISSION_GRANTED)
		//	{
		//		requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Settings.ACTION_MANAGE_OVERLAY_PERMISSION}, 1);
		//	}
		//}
		
        mPB = findViewById(R.id.progressBar);
        mStatusText = findViewById(R.id.statusText);
        mProgressFraction = findViewById(R.id.progressAsFraction);
        mProgressPercent = findViewById(R.id.progressAsPercentage);
        mAverageSpeed = findViewById(R.id.progressAverageSpeed);
        mTimeRemaining = findViewById(R.id.progressTimeRemaining);
        mDashboard = findViewById(R.id.downloaderDashboard);
        mCellMessage = findViewById(R.id.approveCellular);
        mPauseButton = findViewById(R.id.pauseButton);
        mWiFiSettingsButton = findViewById(R.id.wifiSettingsButton);
        mPauseButton.setOnClickListener(new OnClickListener() {
				public void onClick(View view)
				{
					if (mStatePaused)
					{
						mRemoteService.requestContinueDownload();
					}
					else
					{
						mRemoteService.requestPauseDownload();
					}
					setButtonPausedState(!mStatePaused);
				}
			});
        mWiFiSettingsButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v)
				{
					startActivity(new Intent("android.settings.WIFI_SETTINGS"));
				}
			});
        ((Button) findViewById(R.id.resumeOverCellular)).setOnClickListener(new OnClickListener() {
				public void onClick(View view)
				{
					mRemoteService.setDownloadFlags(1);
					mRemoteService.requestContinueDownload();
					mCellMessage.setVisibility(8);
				}
			});
    }

    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        initializeDownloadUI();
        try
		{
            Intent launchIntent = getIntent();
            Intent intentToLaunchThisActivityFromNotification = new Intent(this, getClass());
            intentToLaunchThisActivityFromNotification.setFlags(335544320);
            intentToLaunchThisActivityFromNotification.setAction(launchIntent.getAction());
            if (launchIntent.getCategories() != null)
			{
                for (String category : launchIntent.getCategories())
				{
                    intentToLaunchThisActivityFromNotification.addCategory(category);
                }
            }
            if (DownloaderClientMarshaller.startDownloadServiceIfRequired(this, PendingIntent.getActivity(this, 0, intentToLaunchThisActivityFromNotification, 134217728), AideNdkDownloaderService.class) == 0)
			{
                onDownloadStateChanged(5);
            }
        }
		catch (NameNotFoundException e)
		{
            Log.e(LOG_TAG, "Cannot find own package! MAYDAY!");
            e.printStackTrace();
        }
    }

    public void onStart()
	{
        if (mDownloaderClientStub != null)
		{
            mDownloaderClientStub.connect(this);
        }
        super.onStart();
    }

    public void onStop()
	{
        if (mDownloaderClientStub != null)
		{
            mDownloaderClientStub.disconnect(this);
        }
        super.onStop();
    }

    public void onServiceConnected(Messenger m)
	{
        mRemoteService = DownloaderServiceMarshaller.CreateProxy(m);
        mRemoteService.onClientUpdated(mDownloaderClientStub.getMessenger());
    }

    public void onDownloadStateChanged(int newState)
	{
        boolean paused;
        boolean indeterminate;
        int newDashboardVisibility;
        int cellMessageVisibility;
        setState(newState);
        boolean showDashboard = true;
        boolean showCellMessage = false;
        switch (newState)
		{
            case BuildConfig.VERSION_CODE:
                paused = false;
                indeterminate = true;
                break;
            case 2:
            case 3:
                showDashboard = true;
                paused = false;
                indeterminate = true;
                break;
            case 4:
                paused = false;
                showDashboard = true;
                indeterminate = false;
                break;
            case 5:
                showDashboard = false;
                paused = false;
                indeterminate = false;
                break;
            case 7:
                paused = true;
                indeterminate = false;
                break;
            case 8:
            case 9:
                showDashboard = false;
                paused = true;
                indeterminate = false;
                showCellMessage = true;
                break;
            case 12:
            case 14:
                paused = true;
                indeterminate = false;
                break;
            case 15:
            case 16:
            case 18:
            case 19:
                paused = true;
                showDashboard = false;
                indeterminate = false;
                break;
            default:
                paused = true;
                indeterminate = true;
                showDashboard = true;
                break;
        }
        if (showDashboard)
		{
            newDashboardVisibility = 0;
        }
		else
		{
            newDashboardVisibility = 8;
        }
        if (mDashboard.getVisibility() != newDashboardVisibility)
		{
            mDashboard.setVisibility(newDashboardVisibility);
        }
        if (showCellMessage)
		{
            cellMessageVisibility = 0;
        }
		else
		{
            cellMessageVisibility = 8;
        }
        if (mCellMessage.getVisibility() != cellMessageVisibility)
		{
            mCellMessage.setVisibility(cellMessageVisibility);
        }
        mPB.setIndeterminate(indeterminate);
        setButtonPausedState(paused);
    }

    public void onDownloadProgress(DownloadProgressInfo progress)
	{
        mAverageSpeed.setText(getString(R.string.kilobytes_per_second, new Object[]
		{
			Helpers.getSpeedString(progress.mCurrentSpeed)
		}));
        mTimeRemaining.setText(getString(R.string.time_remaining, new Object[]
		{
			Helpers.getTimeRemaining(progress.mTimeRemaining)
		}));
        progress.mOverallTotal = progress.mOverallTotal;
        mPB.setMax((int) (progress.mOverallTotal >> 8));
        mPB.setProgress((int) (progress.mOverallProgress >> 8));
        mProgressPercent.setText(Long.toString((progress.mOverallProgress * 100) / progress.mOverallTotal) + "%");
        mProgressFraction.setText(Helpers.getDownloadProgressString(progress.mOverallProgress, progress.mOverallTotal));
    }
}
