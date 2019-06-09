package com.aide.ui;

import java.io.*;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;

import android.app.*;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.content.*;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.*;
import android.net.*;
import android.os.*;
import android.preference.*;
import android.view.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;

import com.aide.analytics.*;
import com.aide.common.*;
import com.aide.ui.activities.*;
import com.aide.ui.browsers.*;
import com.aide.ui.build.*;
import com.aide.ui.dialogs.*;
import com.aide.ui.htmluidesigner.*;
import com.aide.ui.preferences.*;
import com.aide.ui.scm.*;
import com.aide.ui.services.ErrorService.EngineErrorListener;
import com.aide.ui.services.*;
import com.aide.ui.services.OpenFileService.OpenFileModel;
import com.aide.ui.util.*;
import com.aide.ui.views.CodeEditText.OnSoftKeyboardShownListener;
import com.aide.ui.views.*;
import com.aide.uidesigner.*;
import com.aide.util.*;

public class MainActivity extends ThemedActivity implements EngineErrorListener, OnSoftKeyboardShownListener,
		OnSharedPreferenceChangeListener
{
	private static final String ANALYTICS_API_KEY = "288Y7TNVNBKQ2GJJN2C9";
<<<<<<< HEAD
	private static final GregorianCalendar RELEASE_DATE = new GregorianCalendar(2013, 5, 25);
	private static final int RELEASE_SALE_DAYS = 4;
=======
	private static final GregorianCalendar RELEASE_DATE = new GregorianCalendar(2013, 9, 22);
	private static final int RELEASE_SALE_DAYS = 6;
>>>>>>> codedroid

	private static final String BROWSER_PREF_SETTINGS_NAME = "BrowserLayout";
	private static final String BROWSER_HORIZONTAL_SETTING_PREFIX = "IsHorizontal";
	private static final String BROWSER_CURRENT_SETTING = "CurrentBrowser";
	
	private static final String EXTRA_SHOWN_FROM_NOTIFICATION = "ShowErrors";
	private static final String EXTRA_NAVIGATE_PLAY_STORE_INTENT_SENDER = "NavigatePlayStoreIntentSender";

	private static final int REQUEST_GOTO = 12;
	
	private long lastBackTime;

	private KeyStrokeDetector keyStrokeDetector;
	private ProgressDialogHandler progressDialog = new ProgressDialogHandler();
	

	private SearchBarHandler searchBar = new SearchBarHandler(this);

	private boolean inNavigateMode;

	private Object startAppLock = new Object();

	private boolean restartAfterThemeChange;
	private boolean newlyInstalled;

	private int firstSeenVersion = -1;
	
	private boolean started;
	
	private int autoSaveTime;
	private Handler autoSaveHandler;
	private AutoSaveRunnable autoSaveRunnable = new AutoSaveRunnable();
	
	public static void navigateToPurchase(Activity caller, IntentSender playStoreIntentSender)
	{
		Intent intent = new Intent(caller, MainActivity.class);
		intent.putExtra(EXTRA_NAVIGATE_PLAY_STORE_INTENT_SENDER, playStoreIntentSender);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		caller.startActivity(intent);
	}

	public static Intent getShowFromNotificationIntent()
	{
		Intent intent = new Intent(App.getContext(), MainActivity.class);
		intent.putExtra(EXTRA_SHOWN_FROM_NOTIFICATION, true);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return intent;
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		AppLog.s(this, "onNewIntent");

		super.onNewIntent(intent);

		if (intent != null && intent.getExtras() != null 
				&& intent.getExtras().getParcelable(EXTRA_NAVIGATE_PLAY_STORE_INTENT_SENDER) != null)
		{
			IntentSender playStoreIntentSender = (IntentSender) intent.getExtras().getParcelable(EXTRA_NAVIGATE_PLAY_STORE_INTENT_SENDER);
			InAppBillingService.doInAppPurchase(this, playStoreIntentSender);
		}
		else if (intent != null && intent.getExtras() != null
				&& intent.getExtras().getBoolean(EXTRA_SHOWN_FROM_NOTIFICATION, false))
		{
			if (App.getErrorService().getErrorCount() > 0)
				openErrorBrowser();
			if (App.getBuildService().isBuilding() && !App.getBuildService().isRegeneratingRJavaAndRunningAidlInBackground())
				showBuildProgressDialog();
			else if (App.getGitService().isExclusiveOperationRunning())
				showGitOperationProgressDialog();
			else if (App.getDropboxService().isOperationRunning())
				showDropboxOperationProgressDialog();
		}
		else if (intent != null && intent.getData() != null)
		{
			navigateTo(intent.getData().getPath());
		}
		else if (intent != null && intent.getExtras() != null
				&& intent.getExtras().getString(AIDEActivityStarter.EXTRA_NAVIGATE_FILE) != null)
		{
			int line = intent.getExtras().getInt(AIDEActivityStarter.EXTRA_NAVIGATE_LINE);
			int column = intent.getExtras().getInt(AIDEActivityStarter.EXTRA_NAVIGATE_COLUMN);
			navigateTo(new FileSpan(intent.getExtras().getString(AIDEActivityStarter.EXTRA_NAVIGATE_FILE), line, column, line,
					column));
		}
	}
	
	@Override
	protected void onPause()
	{
		AppLog.s(this, "onPause");
		
		if (autoSaveTime > 0)
		{
			App.getOpenFileService().saveOpenFiles(true, true);
		}
		
		super.onPause();
	}

	@Override
	protected void onDestroy()
	{
		AppLog.s(this, "onDestroy - finishing: " + isFinishing());
		
		super.onDestroy();

		autoSaveHandler.removeCallbacks(autoSaveRunnable);

		App.getErrorService().removeEngineErrorListener(this);
		getEditor().onDestroy();
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);

		App.shutdown();
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		// TODO optimize: call only when pref activity is closed

		if (key.equals(AppPreferences.SEND_ANALYTICS_DATA))
		{
			Analytics.isEnabledChanged();
		}
		else
		{
			Analytics.logEvent("Setting changed", Collections.singletonMap("key", key));
		}

		autoSaveTime = AppPreferences.autoSaveTime();

		if (AppPreferences.ANDROID_JAR.equals(key))
			App.getProjectService().refresh();

		App.getEngine().configureEngineOptions();

		getLogCatBrowser().configure();

		if (AppPreferences.LIGHT_THEME.equals(key) 
				|| AppPreferences.SHOW_EDITOR_TABS.equals(key)
				|| AppPreferences.SEND_ANALYTICS_DATA.equals(key))
			restartAfterThemeChange = true;

		getEditor().configure();
		if (AppPreferences.EDITOR_FONT_SIZE.equals(key))
			getEditor().configureFontSize();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		getKeyStrokeDetector().onConfigChange(this);
	}

	@Override
	public Object onRetainNonConfigurationInstance()
	{
		return "dummy";
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		synchronized (startAppLock)
		{
			startAppLock.notifyAll();
		}

		if (restartAfterThemeChange)
		{
			exitApp();
		}
		else
		{
			App.getDropboxService().onActivityResume();

			refreshExternallyModifiedFiles();
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		AppLog.s(this, "onCreate");
		
		this.autoSaveHandler = new Handler();

		newlyInstalled = AppPreferences.initDefaults(this);

		super.onCreate(savedInstanceState);

		if (!Analytics.isInitialized())
		{
			Analytics.initialize(this, ANALYTICS_API_KEY, new AnalyticsOptionsCallback()
			{
				@Override
				public boolean isAnalyticsEnabled()
				{
					return AppPreferences.sendAnalyticsData();
				}
			});
		}

		String filepath = null;
		if (getIntent() != null && getIntent().getData() != null)
		{
			filepath = getIntent().getData().getPath();
		}

		App.init(this, filepath);

		keyStrokeDetector = new KeyStrokeDetector(this);

		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
		autoSaveTime = AppPreferences.autoSaveTime();

		if (android.os.Build.VERSION.SDK_INT > 10)
		{
			if (!AppPreferences.showEditorTabs())
			{
				getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
			}
		}
		
		setContentView(R.layout.main);

		if (android.os.Build.VERSION.SDK_INT <= 10)
		{
			findViewById(R.id.mainActionBarContainer10).setVisibility(View.VISIBLE);
			findViewById(R.id.mainActionBarContainer11).setVisibility(View.GONE);

			LinearLayout bar = (LinearLayout) findViewById(R.id.mainActionBarContainer10);
			LayoutInflater inflater = LayoutInflater.from(this);
			View content = inflater.inflate(R.layout.mainactionbar, null);
			bar.removeAllViews();
			bar.addView(content);

			findViewById(R.id.mainActionBar10).setBackgroundDrawable(new ActionBarBackground(this));
		}
		else
		{
			findViewById(R.id.mainActionBarContainer10).setVisibility(View.GONE);
			findViewById(R.id.mainActionBarContainer11).setVisibility(View.VISIBLE);

			if (AppPreferences.showEditorTabs())
			{
				getActionBar().setDisplayShowTitleEnabled(false);
				getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
			}
			else
			{
				getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
				getActionBar().setDisplayShowCustomEnabled(true);
				getActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar_bg));
				
				findViewById(R.id.mainActionBar11).setBackgroundDrawable(new ActionBarBackground(this));
			}
		}
		findViewById(R.id.mainSearchBar).setBackgroundDrawable(new ActionBarBottomBackground(this));

		App.getOpenFileService().init(filepath);

		getLogCatBrowser().initLayout();

		App.getHistoryService().remember(getEditor().getCurrentFileSpan());

		App.getErrorService().addEngineErrorListener(this);

		registerActionBarOnClickListeners();

		getEditor().setSoftKeyboardListener(this);

		refreshActionBar();

		boolean inConfigurationChange = getLastNonConfigurationInstance() != null;
		if (!inConfigurationChange)
		{
			ensureNoMediaFileExists();

			showDialogsAfterStartup();
			
			openBrowserAfterStartup();
			
			if (NdkSupport.areOldVersionInstalled())
			{
				Thread th = new Thread("Old NDK Version Deleter")
				{
					@Override
					public void run()
					{
						NdkSupport.deleteOldVersions();
					}
				};
				th.setDaemon(true);
				th.setPriority(Thread.MIN_PRIORITY);
				th.start();
			}
		}
	}

	public void hideAppTip()
	{
		final View popup = findViewById(R.id.mainAppTip);
		popup.setVisibility(View.GONE);
	}
	
	public void showAppTip(final AppTip tip)
	{
		final View popup = findViewById(R.id.mainAppTip);

		TextView textView = (TextView) popup.findViewById(R.id.mainAppTipText);
		textView.setText(tip.getText());

		if (android.os.Build.VERSION.SDK_INT > 10)
		{
			Animation animationFadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
			popup.startAnimation(animationFadeIn);
		}

		popup.setVisibility(View.VISIBLE);
		popup.setOnClickListener(new Button.OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{
				popup.setVisibility(View.GONE);
			}
		});
		textView.setOnClickListener(new Button.OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{
				popup.setVisibility(View.GONE);
				tip.run();
			}
		});
	}
	
	private void ensureNoMediaFileExists()
	{
		String nomediaFile = new File(App.getFileBrowserService().getDefaultProjectsDir(), ".nomedia").getPath();
		if (Filesystem.exists(App.getFileBrowserService().getDefaultProjectsDir())
			&& !Filesystem.exists(nomediaFile))
		{
			try
			{
				Filesystem.createFile(nomediaFile);
			}
			catch (IOException e)
			{
				AppLog.e(e);
			}
		}
	}

	private Spinner getActionBarTabSpinner()
	{
	    try
	    {
	        int id = getResources().getIdentifier("action_bar", "id", "android");
	        View actionBarView = findViewById(id);

	        Class<?> actionBarViewClass = actionBarView.getClass();
	        Field mTabScrollViewField = actionBarViewClass.getDeclaredField("mTabScrollView");
	        mTabScrollViewField.setAccessible(true);

	        Object mTabScrollView = mTabScrollViewField.get(actionBarView);
	        if (mTabScrollView == null) {
	            return null;
	        }

	        Field mTabSpinnerField = mTabScrollView.getClass().getDeclaredField("mTabSpinner");
	        mTabSpinnerField.setAccessible(true);

	        Object mTabSpinner = mTabSpinnerField.get(mTabScrollView);
	        if (mTabSpinner != null)
	        {
	            return (Spinner)mTabSpinner;
	        }
	    } 
	    catch (Exception e) {
	        return null;
	    }

	    return null;
	}
	
	public void removeTab(int index)
	{
		if (android.os.Build.VERSION.SDK_INT > 10 && AppPreferences.showEditorTabs())
		{
			ActionBar actionBar = getActionBar();
			if (index >= 0 && index < actionBar.getTabCount())
			{
				actionBar.removeTab(actionBar.getTabAt(index));
			}
		}
	}
	
	public void selectTab(int index)
	{
		if (android.os.Build.VERSION.SDK_INT > 10 && AppPreferences.showEditorTabs())
		{
			ActionBar actionBar = getActionBar();
			if (index >= 0 && index < actionBar.getTabCount() && index != actionBar.getSelectedNavigationIndex())
			{
				actionBar.selectTab(actionBar.getTabAt(index));
				Spinner spinner = getActionBarTabSpinner();
				if (spinner != null)
				{
					spinner.setSelection(index);
				}
			}
		}
	}

	public void refreshTabTitles()
	{
		if (android.os.Build.VERSION.SDK_INT > 10 && AppPreferences.showEditorTabs())
		{
			ActionBar actionBar = getActionBar();
			List<AIDEEditor> editors = getEditor().getFileEditors();
			for (int i = 0; i < editors.size(); i++)
			{
				OpenFileModel model = editors.get(i).getModel();
				String title = Filesystem.getName(model.getFilePath());
				if (model.isModified())
					title += " *";
				Tab tab = actionBar.getTabAt(i);
				tab.setText(title);
			}
		}		
	}

	public void addTab(final String filepath)
	{
		if (android.os.Build.VERSION.SDK_INT > 10 && AppPreferences.showEditorTabs())
		{
			final ActionBar actionBar = getActionBar();
			final Tab newTab = actionBar.newTab().setText(Filesystem.getName(filepath));
			newTab.setTabListener(new TabListener()
			{
				@Override
				public void onTabUnselected(Tab tab, FragmentTransaction ft)
				{
				}

				@Override
				public void onTabSelected(Tab tab, FragmentTransaction ft)
				{
					if (newTab == tab && !filepath.equals(App.getOpenFileService().getVisibleFile()))
						App.getOpenFileService().showOpenFile(filepath);
				}

				@Override
				public void onTabReselected(Tab tab, FragmentTransaction ft)
				{
					if (newTab == tab)
						showOpenFilesList();
				}
			});
			actionBar.addTab(newTab, false);
		}
	}

	public int getFirstSeenVersion()
	{
		return firstSeenVersion;
	}

	private void openBrowserAfterStartup()
	{
		SharedPreferences prefs = getSharedPreferences(BROWSER_PREF_SETTINGS_NAME, 0);
		int browser = prefs.getInt(BROWSER_CURRENT_SETTING, -1);
		if (browser >= 0)
		{
			openBrowser(browser);
		}
		else
		{
			if (!App.getOpenFileService().isFileVisible())
			{
				openFileBrowser();
			}
		}
	}

	private void showDialogsAfterStartup()
	{
		if (isExpired())
		{
			showExpirationDialog();
			return;
		}

		firstSeenVersion = determineFirstSeenVersion();
		
		String prevVersion = getPreviousAppVersionOrNull(true);
		if (prevVersion != null)
		{
			MessageBox.showInfo(this, "AIDE has been updated", getChangelog(prevVersion), new Runnable()
			{
				public void run()
				{
					showDialogsAfterStartupContinue2();
				}
			});
			return;
		}

		showDialogsAfterStartupContinue2();
	}

	private void showDialogsAfterStartupContinue2()
	{
		GregorianCalendar releaseSaleExpiryDate = (GregorianCalendar) RELEASE_DATE.clone();	
		releaseSaleExpiryDate.add(GregorianCalendar.DATE, RELEASE_SALE_DAYS);
		releaseSaleExpiryDate.add(GregorianCalendar.SECOND, -1);
		if (App.getLicenseService().isPremiumKeyOutdated())
		{
			MessageBox.showInfo(this, "AIDE Premium key outdated", "Your AIDE Premium Key is outdated. Premium features have been disabled. Please upgrade it to the latest version.",
					true, "Update", new Runnable()
					{
						public void run()
						{
							App.getLicenseService().openPremiumPurchasePage(MainActivity.this, "key_outdated");
							showDialogsAfterStartupContinue3();
						}
					},
					"Not now",
					new Runnable()
					{
						public void run()
						{
							showDialogsAfterStartupContinue3();
						}
					});
			return;
		}
		else if (App.CURRENT.equals(App.AIDE_ANDROID) && !App.getLicenseService().isPremiumKeyInstalled() && new GregorianCalendar().before(releaseSaleExpiryDate))
		{
			MessageBox.showInfo(this, "AIDE Premium Sale", "Get the AIDE Premium Key 50% off until "
					+ DateFormat.getDateInstance(SimpleDateFormat.MEDIUM).format(releaseSaleExpiryDate.getTime()) + "!",
					true, "Purchase", new Runnable()
					{
						public void run()
						{
							App.getLicenseService().openPremiumPurchasePage(MainActivity.this, "50percentoffsale");
							showDialogsAfterStartupContinue3();
						}
					},
					"Not now",
					new Runnable()
					{
						public void run()
						{
							showDialogsAfterStartupContinue3();
						}
					});
			return;
		}

		showDialogsAfterStartupContinue3();
	}
	
	private void showDialogsAfterStartupContinue3()
	{
		if (!App.getOpenFileService().isFileVisible())
		{
			if (!App.getProjectService().isProjectOpen())
			{
				Analytics.logEvent("App init: Showing create project dialog");
				showCreateProjectDialog(App.getFileBrowserService().getDefaultProjectsDir());
				return;
			}
		}

		if (App.getOpenFileService().isFileVisible())
		{
			App.getDropboxService().onFileOpened(App.getOpenFileService().getVisibleFile());
		}
		if (App.getProjectService().isProjectOpen())
		{
			App.getDropboxService().onProjectOpened(App.getProjectService().getCurrentProjectHomeDirs());
		}
		
		showDialogsAfterStartupContinue4();
	}

	private void showDialogsAfterStartupContinue4()
	{
		App.getAppTipService().onStartUpWithoutDialogs();
	}

	private String getChangelog(String prevVersion)
	{
		InputStream is;
		try
		{
			if (App.CURRENT.equals(App.AIDE_ANDROID))
			{
				is = getAssets().open("changelog-android.txt");
			}
			else
			{
				is = getAssets().open("changelog-phonegap.txt");
			}
			try
			{
				InputStreamReader r = new InputStreamReader(is);
				StringWriter w = new StringWriter();
				PrintWriter pw = new PrintWriter(w);
				pw.println("Changelog");
				pw.println();
				if (prevVersion != null && !"".equals(prevVersion))
				{
					pw.println("Current version: " + getCurrentVersionName());
					pw.println("Previous version: " + prevVersion);
					pw.println();
				}
				pw.flush();
				StreamUtilities.transfer(r, w);
				return w.toString();
			}
			finally
			{
				is.close();
			}
		}
		catch (IOException e)
		{
			AppLog.e(e);
			return "";
		}
	}

	private int determineFirstSeenVersion()
	{
		try
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			int firstSeenVersion = prefs.getInt("first_seen_version", -1);
			if (firstSeenVersion == -1)
			{
				// version without first_seen_version or new install, check last_seen_version
				String prevAppVersion = getPreviousAppVersionOrNull(false);
				if (prevAppVersion == null)
				{
					// never installed
					firstSeenVersion = getCurrentVersionCode();
				}
				else
				{
					// older version installed before
					firstSeenVersion = 103;
				}
				SharedPreferences.Editor editor = prefs.edit();
				editor.putInt("first_seen_version", getCurrentVersionCode());
				editor.commit();
			}
			return firstSeenVersion;
		}
		catch (Throwable t)
		{
			AppLog.e(t);
			return 0;
		}
	}
	
	/**
	 * 
	 * @return previous version if updated can be determined, "" if updated but
	 *         prev version unknown, null if clean install
	 */
	private String getPreviousAppVersionOrNull(boolean updateLastSeenVersion)
	{
		try
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			String lastSeenVersion = prefs.getString("last_seen_version", "");
			String currentVersion = getCurrentVersionName();
			if (updateLastSeenVersion && ("".equals(lastSeenVersion) || !currentVersion.equals(lastSeenVersion)))
			{
				SharedPreferences.Editor editor = prefs.edit();
				editor.putString("last_seen_version", currentVersion);
				editor.commit();
			}
			if (currentVersion == null || "".equals(currentVersion))
			{
				return null;
			}
			if ("".equals(lastSeenVersion))
			{
				if (newlyInstalled)
				{
					// was never installed
					return null;
				}
				else
				{
					return "";
				}
			}
			if (!currentVersion.equals(lastSeenVersion))
			{
				return lastSeenVersion;
			}
			return null;
		}
		catch (Throwable t)
		{
			AppLog.e(t);
			return null;
		}
	}

	private String getCurrentVersionName()
	{
		try
		{
			return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		}
		catch (NameNotFoundException e)
		{
			AppLog.e(e);
			return "";
		}
	}

	private int getCurrentVersionCode()
	{
		try
		{
			return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
		}
		catch (NameNotFoundException e)
		{
			AppLog.e(e);
			return 0;
		}
	}

	private void forceCloseIfExpired()
	{
		if (isExpired())
		{
			finish();
		}
	}

	private boolean isExpired()
	{
		return false;
	}

	private void showExpirationDialog()
	{
		MessageBox.showInfo(this, "AIDE", "This beta version of AIDE is out-dated. Please update to a newer version!",
				false, new Runnable()
				{
					public void run()
					{
						openMarket("expired");
						finish();
					}
				}, null);
	}

	public void openMarket(String linkId)
	{
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(Analytics.getMarketLinkUrl(App.CURRENT, App.CURRENT, linkId)));
		intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		try
		{
			startActivity(intent);
		}
		catch (ActivityNotFoundException e)
		{
			MessageBox.showError(this, "Google Play Store", "Google Play Store App could not be opened. Not installed?");
		}
	}

	public void updateErrors()
	{
		refreshAnalysisProgressBar();
		refreshErrorsView();
		getErrorBrowser().refreshContent();
	}

	public void updateErrors(final String filepath)
	{
		refreshAnalysisProgressBar();
		refreshErrorsView();
	}

	private void registerActionBarOnClickListeners()
	{
		if (android.os.Build.VERSION.SDK_INT <= 10)
		{
			for (AppCommand command : AppCommands.getCommands())
			{
				if (command instanceof ActionBarCommand)
				{
					final ActionBarCommand actionBarCommand = (ActionBarCommand) command;
					View view = findViewById(actionBarCommand.getActionButtonViewID());
					if (view != null)
					{
						view.setOnClickListener(new View.OnClickListener()
						{
							public void onClick(View v)
							{
								if (actionBarCommand.isEnabled())
								{
									v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,
											HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
									Analytics.logEvent("Action bar command: " + actionBarCommand.getLabel());
									actionBarCommand.run();
								}
							}
						});
					}
				}
			}
		}
	}

	public void refreshActionBar()
	{
		if (android.os.Build.VERSION.SDK_INT <= 10)
		{
			for (AppCommand command : AppCommands.getCommands())
			{
				if (command instanceof ActionBarCommand)
				{
					final ActionBarCommand actionBarCommand = (ActionBarCommand) command;
					View view = findViewById(actionBarCommand.getActionButtonViewID());
					if (view != null)
					{
						view.setVisibility(actionBarCommand.isVisible() ? View.VISIBLE : View.GONE);
					}
				}
			}
		}
		else
		{
			invalidateOptionsMenu();
		}
	}

	private void refreshAnalysisProgressBar()
	{
		AIDEAnalysisProgressBar progressBar = (AIDEAnalysisProgressBar) findViewById(R.id.mainErrorProgress);
		progressBar.refresh();
	}

	private void refreshErrorsView()
	{
		AIDEErrorsView errorsView = getErrorsView();
		if (errorsView != null)
		{
			errorsView.refresh();
			if (android.os.Build.VERSION.SDK_INT > 10)
			{
				findViewById(R.id.mainActionBar11).invalidate();
			}
		}
	}

	public float getActionBarLeft()
	{
		if (android.os.Build.VERSION.SDK_INT > 10)
		{
			AIDEErrorsView leftMostView = getErrorsView();
			if (leftMostView != null)
			{
				float left = ((View) leftMostView.getParent()).getLeft();
				float density = getResources().getDisplayMetrics().density;
				return left - density * 10;
			}
		}

		return 0;
	}

	private AIDEErrorsView getErrorsView()
	{
		if (android.os.Build.VERSION.SDK_INT <= 10)
		{
			AIDEErrorsView errorsView = (AIDEErrorsView) findViewById(R.id.mainErrorsView);
			return errorsView;
		}
		else
		{
			AIDEErrorsView errorsView = (AIDEErrorsView) findViewById(R.id.mainMenuErrorsView);

			if (errorsView != null)
			{
				return errorsView;
			}

			View view = findViewById(R.id.errorsViewContainer);
			if (view != null)
			{
				ViewParent parent = view.getParent();
				if (parent instanceof AIDEErrorsView)
				{
					return (AIDEErrorsView) parent;
				}
			}

			return null;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		keyStrokeDetector.onActivityKeyDown(keyCode, event);
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		keyStrokeDetector.onActivityKeyUp(keyCode, event);
		return super.onKeyUp(keyCode, event);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == REQUEST_GOTO && resultCode == RESULT_OK)
		{
			FileSpan fileSpan = GotoBrowserActivity.getReturnedFileSpan(data);
			navigateTo(fileSpan);
		}
		else if (requestCode == InAppBillingService.REQUEST_IN_APP_PURCHASE)
		{
			App.getInAppBillingService().processInAppPurchaseActivityResult(data);
		}
	}	
	
	public void openProgressDialog()
	{
		progressDialog.openDialogDelayed();
	}

	public void closeProgressDialog()
	{
		progressDialog.closeDialog();
	}

	public void navigateTo(String filepath)
	{
		String ext = Filesystem.getExtension(filepath);
		String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
		if (ext.equals("java") || ext.equals("xml") || mimeType == null || mimeType.startsWith("text"))
		{
			if (!Filesystem.isBinary(filepath))
			{
				navigateTo(new FileSpan(filepath, 1, 1, 1, 1));
			}
		}
		else
		{
			Intent intent = new Intent();
			intent.setAction(android.content.Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.fromFile(new java.io.File(filepath)), mimeType);
			try
			{
				startActivity(intent);
			}
			catch (ActivityNotFoundException e)
			{
				Toast.makeText(App.getContext(), "No handler found for type " + mimeType, Toast.LENGTH_SHORT).show();
			}
		}
	}

	public void navigateTo(final FileSpan fileSpan)
	{
		if (fileSpan != null && Filesystem.isFile(fileSpan.filePath))
		{
			if (fileSpan.filePath.equals(App.getOpenFileService().getVisibleFile()))
			{
				getEditor().select(fileSpan.startLine, fileSpan.startColumn, fileSpan.endLine, fileSpan.endColumn);
				getEditor().focus();
				App.getHistoryService().setRememberHistoryEnabled(true);
			}
			else
			{
				forceCloseIfExpired();
				
				App.getProjectService().openProjectForFile(fileSpan.filePath);

				final boolean wasRememberHistoryEnabled = App.getHistoryService().isRememberHistoryEnabled();
				App.getHistoryService().setRememberHistoryEnabled(false);
				try
				{
					App.getOpenFileService().openFile(fileSpan.filePath, true);
				}
				catch (IOException e)
				{
					Toast.makeText(App.getContext(), fileSpan.filePath + " could not be loaded!", Toast.LENGTH_LONG).show();
					return;
				}
				finally
				{
					if (wasRememberHistoryEnabled)
					{
						App.getHistoryService().setRememberHistoryEnabled(true);
					}
				}
				getEditor().select(fileSpan.startLine, fileSpan.startColumn, fileSpan.endLine, fileSpan.endColumn);
				getEditor().ensureCaretVisibilityDelayed();
				App.getHistoryService().setRememberHistoryEnabled(true);
			}
		}
	}

	@Override
	protected Dialog onCreateDialog(int id, Bundle args)
	{
		return MessageBox.onCreateDialog(this, id);
	}

	public void onUserLeaveHint() 
	{
		getEditor().hideSoftKeyboard();
        super.onUserLeaveHint();
    }
	
	@Override
	public void onBackPressed()
	{
		if (searchBar.isOpen())
		{
			searchBar.hide();
		}
		else if (getSplitView().isSplit())
		{
			closeBrowser();
		}
		else
		{
			if (navigateBack())
			{
			}
			else
			{
				long time = System.currentTimeMillis();
				if (time - lastBackTime < 2000)
				{
					exitApp();
				}
				else
				{
					lastBackTime = time;
					Toast.makeText(App.getContext(), "Press back once more to exit", Toast.LENGTH_SHORT).show();
				}
			}
		}
	}

	public boolean navigateBack()
	{
		FileSpan span = App.getHistoryService().back();
		if (span != null)
		{
			App.getHistoryService().setRememberHistoryEnabled(false);
			navigateTo(span);
			return true;
		}
		return false;
	}

	public void navigateForward()
	{
		FileSpan span = App.getHistoryService().forward();
		if (span != null)
		{
			App.getHistoryService().setRememberHistoryEnabled(false);
			navigateTo(span);
		}

	}

	private static class AutoSaveRunnable implements Runnable
	{
		@Override
		public void run()
		{
			App.getOpenFileService().saveOpenFiles(true, true);
		}
	}
	
	public void sheduleAutoSave()
	{
		autoSaveHandler.removeCallbacks(autoSaveRunnable);
		if (autoSaveTime > 0)
		{
			autoSaveHandler.postDelayed(autoSaveRunnable, (long) autoSaveTime * 1000);
		}
	}

	public void openProjectUser(final String dirPath)
	{
		if (!App.getProjectService().isSupportedProject(dirPath))
		{
			App.getLicenseService().showOtherAideEditionDialog(this,
					App.getProjectService().getSupportingEditionPackage(dirPath), "open_project");
		}
		else if (App.getOpenFileService().isModifiedFileOpen())
		{
			MessageBox.queryYesNo(this, "Switch project", "Save files before switching project?", new Runnable()
			{
				public void run()
				{
					App.getOpenFileService().saveOpenFiles(false, false);
					doOpenProjectUser(dirPath);
				}
			}, new Runnable()
			{
				public void run()
				{
					doOpenProjectUser(dirPath);
				}
			});
		}
		else
		{
			doOpenProjectUser(dirPath);
		}

	}

	private void doOpenProjectUser(final String dirPath)
	{
		if (!App.getProjectService().isFullySupportedProject(dirPath))
		{
			App.getLicenseService().showOtherAideEditionDialog(this,
					App.getProjectService().getSupportingEditionPackage(dirPath), "open_project_2");
		}
		
		App.getProjectService().openProjectUser(dirPath);
		App.getFileBrowserService().refresh();
	}

	public void exitApp()
	{
		if (App.getOpenFileService().isModifiedFileOpen())
		{
			MessageBox.queryYesNo(this, "Exit App", "Save files before exiting?", new Runnable()
			{
				public void run()
				{
					App.getOpenFileService().saveOpenFiles(false, false);
					finish();
				}
			}, new Runnable()
			{
				public void run()
				{
					finish();
				}
			});
		}
		else
		{
			finish();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		if (android.os.Build.VERSION.SDK_INT <= 10)
			inflater.inflate(R.menu.main_options_menu, menu);
		else
			inflater.inflate(R.menu.main_options_menu_v11, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		boolean res = super.onPrepareOptionsMenu(menu);

		if (!App.isShutdown()) setMenuItemsState(menu);

		return res;
	}

	private void setMenuItemsState(Menu menu)
	{
		for (int i = 0; i < menu.size(); i++)
		{
			MenuItem item = menu.getItem(i);
			MenuCommand menuCommand = AppCommands.getMenuCommand(item.getItemId());
			if (menuCommand != null)
			{
				item.setEnabled(menuCommand.isEnabled());
				if (menuCommand instanceof MenuCommandWithVisibility)
				{
					item.setVisible(((MenuCommandWithVisibility) menuCommand).isVisible());
				}
			}
			ActionBarCommand actionBarCommand = AppCommands.getActionBarCommand(item.getItemId());
			if (actionBarCommand != null)
			{
				item.setVisible(actionBarCommand.isVisible());
				item.setEnabled(actionBarCommand.isEnabled());
			}
			if (item.hasSubMenu())
			{
				setMenuItemsState(item.getSubMenu());
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == android.R.id.home)
		{
			showOpenFilesList();
		}
		
		MenuCommand menuCommand = AppCommands.getMenuCommand(item.getItemId());
		if (menuCommand != null && menuCommand.isEnabled())
		{
			Analytics.logEvent("Main Menu: " + item.getTitle());
			menuCommand.run();
			return true;
		}
		ActionBarCommand actionBarcommand = AppCommands.getActionBarCommand(item.getItemId());
		if (actionBarcommand != null && actionBarcommand.isVisible())
		{
			Analytics.logEvent("Action bar command: " + actionBarcommand.getLabel());
			actionBarcommand.run();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void showProjectProperties()
	{
		MessageBox.showDialog(this, new ProjectPropertiesDialog());
	}

	public void showExportAPKDialog()
	{
		MessageBox.showDialog(this, new ExportAPKDialog());
	}

	public void showCreateProjectDialog(String dirpath)
	{
		MessageBox.showDialog(this, new CreateProjectDialog(dirpath));
	}

	public void showGitCloneRepositoryDialog(String dirpath)
	{
		MessageBox.showDialog(this, new GitCloneRepositoryDialog(dirpath));
	}

	public void showDownloadDropboxDialog(String dirpath)
	{
		MessageBox.showDialog(this, new DownloadDropboxDialog(dirpath));
	}
	
	public void startPhonegapDesignActivity(String rootDirPath, String filePath)
	{
		PhonegapDesignActivity.show(this, rootDirPath, filePath);
	}

	public void startXmlLayoutDesignActivity(String filePath)
	{
		XmlLayoutDesignActivity.show(this, filePath, !App.getLicenseService().isUIDesignerLicensed(), false);
	}

	public void startHelpViewActivity(DocCategory category, String referenceUrl)
	{
		getEditor().hideSoftKeyboard();
		AIDEHelpActivityStarter.showHelp(this, referenceUrl, category.toString());
	}

	public void startSettingsActivity()
	{
		startSettingsActivity(-1);
	}

	public void startSettingsActivity(int page)
	{
		if (android.os.Build.VERSION.SDK_INT <= 10)
		{
			PreferencesActivity.show(this, page);
		}
		else
		{
			PreferencesActivity11.show(this, page);
		}
	}

	public void startGotoActivity(boolean symbols)
	{
		GotoBrowserActivity.querySymbol(this, symbols, REQUEST_GOTO);
	}

	public void startCommitActivity(GitStatus status, String currentBranch)
	{
		CommitActivity.showCommitActivity(this, status, currentBranch);
	}

	public void enableNavigateMode()
	{
		if (!isInNavigateMode())
		{
			enableNavigateMode(true);
			getEditor().hideSoftKeyboard();
			getEditor().setIdentifierClickingEnabled(isInNavigateMode());
			refreshActionBar();
		}
	}

	public void enableEditMode()
	{
		if (isInNavigateMode())
		{
			enableNavigateMode(false);
			getEditor().setIdentifierClickingEnabled(isInNavigateMode());
			refreshActionBar();
		}
	}

	public void closeBrowser()
	{
		SharedPreferences prefs = getSharedPreferences(BROWSER_PREF_SETTINGS_NAME, 0);
		Editor edt = prefs.edit();
		edt.putInt(BROWSER_CURRENT_SETTING, -1);
		edt.commit();
		
		getSplitView().closeSplit();
		refreshActionBar();
		getEditor().focus();
	}

	public void toggleSplit()
	{
		getSplitView().toggleSplit();
		setIsHorizontalSplit(getBrowsers().getCurrentBrowser(), getSplitView().isHorizontalSplit());
		if (getSplitView().isSplit())
		{
			getEditor().hideSoftKeyboard();
		}
		refreshActionBar();
	}

	private void openBrowser(int browser)
	{
		getSplitView().split(isHorizontalSplit(browser));
		getBrowsers().openBrowser(browser);
		refreshActionBar();
	}

	public void browserChanged(int browser)
	{
		SharedPreferences prefs = getSharedPreferences(BROWSER_PREF_SETTINGS_NAME, 0);
		Editor edt = prefs.edit();
		edt.putInt(BROWSER_CURRENT_SETTING, browser);
		edt.commit();
	}

	private void setIsHorizontalSplit(int browser, boolean horizontal)
	{
		SharedPreferences prefs = getSharedPreferences(BROWSER_PREF_SETTINGS_NAME, 0);
		Editor edt = prefs.edit();
		edt.putBoolean(BROWSER_HORIZONTAL_SETTING_PREFIX + browser, horizontal);
		edt.commit();
	}

	private boolean isHorizontalSplit(int browser)
	{
		SharedPreferences prefs = getSharedPreferences(BROWSER_PREF_SETTINGS_NAME, 0);
		return prefs.getBoolean(BROWSER_HORIZONTAL_SETTING_PREFIX + browser, false);
	}

	public void toggleErrorBrowser()
	{
		if (getSplitView().isSplit() && getBrowsers().getCurrentBrowser() == BrowserPager.ERROR_BROWSER)
		{
			closeBrowser();
		}
		else
		{
			openErrorBrowser();
		}
	}

	public void openFileBrowser()
	{
		getEditor().hideSoftKeyboard();
		openBrowser(BrowserPager.FILE_BROWSER);
	}

	public void openSearchBrowser()
	{
		getEditor().hideSoftKeyboard();
		openBrowser(BrowserPager.SEARCH_BROWSER);
	}

	public void openErrorBrowser()
	{
		getEditor().hideSoftKeyboard();
		openBrowser(BrowserPager.ERROR_BROWSER);
	}

	public void openLogCatBrowser()
	{
		getEditor().hideSoftKeyboard();
		openBrowser(BrowserPager.LOGCAT_BROWSER);
	}

	public void onSoftKeyboardToggeled()
	{
		// AppLog.d("SoftKeyboard " + getEditor().isSoftKeyboardShown());
		refreshActionBar();
		if (getEditor().isSoftKeyboardShown() && AndroidHelper.getScreenHeightDip(MainActivity.this) <= 800)
		{
			getEditor().postDelayed(new Runnable()
			{
				public void run()
				{
					closeBrowser();
				}
			}, 100);
		}
	}

	public BrowserPager getBrowsers()
	{
		return (BrowserPager) this.findViewById(R.id.mainBrowserPager);
	}
	
	public SearchBrowser getSearchBrowser()
	{
		return getBrowsers().getSearchBrowser();
	}

	public ErrorBrowser getErrorBrowser()
	{
		return getBrowsers().getErrorBrowser();
	}

	public FileBrowser getFileBrowser()
	{
		return getBrowsers().getFileBrowser();
	}

	public LogCatBrowser getLogCatBrowser()
	{
		return getBrowsers().getLogCatBrowser();
	}

	public SplitView getSplitView()
	{
		return (SplitView) this.findViewById(R.id.mainSplitView);
	}

	public AIDEEditorPager getEditor()
	{
		return (AIDEEditorPager) findViewById(R.id.mainCodePageView);
	}

	public KeyStrokeDetector getKeyStrokeDetector()
	{
		return keyStrokeDetector;
	}

	public class ProgressDialogHandler
	{
		private ProgressDialog progressDialog;
		private Handler handler = new Handler();
		private Runnable showProgressAction;

		public void openDialogDelayed()
		{
			closeDialog();
			showProgressAction = new Runnable()
			{
				public void run()
				{
					showDialog();
				}
			};
			handler.postDelayed(showProgressAction, 500);
		}

		public void closeDialog()
		{
			if (showProgressAction != null)
			{
				handler.removeCallbacks(showProgressAction);
				showProgressAction = null;
			}
			if (progressDialog != null)
			{
				progressDialog.dismiss();
				progressDialog = null;
			}
		}

		public void openDialog()
		{
			closeDialog();
			showDialog();
		}

		private void showDialog()
		{
			progressDialog = new ProgressDialog(MainActivity.this);
			progressDialog.setMessage("Analyzing...");
			progressDialog.setOnCancelListener(new OnCancelListener()
			{
				public void onCancel(DialogInterface dialog)
				{
					App.getEngine().stopSearch();
					App.getEngine().stopRefactoring();
				}
			});
			progressDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
			progressDialog.show();
		}
	}

	public void showBuildProgressDialog()
	{
		MessageBox.showDialog(this, new BuildProgressDialog());
	}

	public void showGitOperationProgressDialog()
	{
		MessageBox.showDialog(this, new GitOperationProgressDialog());
	}

	public void showDropboxOperationProgressDialog()
	{
		MessageBox.showDialog(this, new DropboxProgressDialog());
	}

	private void enableNavigateMode(boolean b)
	{
		this.inNavigateMode = b;
	}

	public boolean isInNavigateMode()
	{
		return inNavigateMode;
	}

	public void refreshExternallyModifiedFiles()
	{
		if (App.getCurrentActivity() == this)
		{
			App.getOpenFileService().reloadExternallyModifiedFiles();
		}
	}

	public void refreshAfterExternalChanges()
	{
		refreshExternallyModifiedFiles();
		App.getProjectService().refresh();
		App.getFileBrowserService().refresh();
		App.getEngine().synchronizeFilesystem();
	}

	public void openSearchBox()
	{
		searchBar.openSearch();
	}

	public void openGotoLineBox()
	{
		searchBar.openGotoLine();
	}
	
	public void showOpenFilesList()
	{
		final List<AIDEEditor> editors = getEditor().getFileEditors();
		List<String> openFileNames = new ArrayList<String>();
		for (AIDEEditor editor : editors)
		{
			String filepath = editor.getFilePath();
			String name = Filesystem.getName(filepath);
			if (App.getOpenFileService().getOpenFileModel(filepath).isModified())
				name += " *";
			openFileNames.add(name);
		}
		MessageBox.queryIndexFromList(App.getMainActivity(), "Open Files", openFileNames, "Close Files...", new ValueRunnable<Integer>()
			{
				public void run(Integer i)
				{
					App.getOpenFileService().showOpenFile(editors.get(i).getFilePath());
				}
			},
			new Runnable()
			{
				public void run()
				{
					closeFiles();
				}
			});
	}

	private void closeFiles()
	{
		final List<AIDEEditor> editors = getEditor().getFileEditors();
		List<String> files = new ArrayList<String>();
		List<Boolean> selectedFiles = new ArrayList<Boolean>();
		for (AIDEEditor editor : editors)
		{
			String filepath = editor.getFilePath();
			String name = Filesystem.getName(filepath);
			boolean modified = App.getOpenFileService().getOpenFileModel(filepath).isModified();
			if (modified)
				name += " *";
			files.add(name);
			selectedFiles.add(!modified);
		}
		MessageBox.queryMultipleChoiceFromList(App.getMainActivity(), "Close Files", files, selectedFiles,
			new ValueRunnable<List<Integer>>()
			{
				public void run(List<Integer> filesToClose)
				{
					for (Integer i : filesToClose)
					{
						App.getOpenFileService().close(editors.get(i).getFilePath());
					}
				}
			});

	}
	
	public void startApp(String targetApkPath, String packageName)
	{
		getEditor().hideSoftKeyboard();
		App.runOnBackgroundThread(App.getMainActivity(), "Starting App...", new Runnable()
		{
			public void run()
			{
				synchronized (startAppLock)
				{
					try
					{
						startAppLock.wait(5000);
					}
					catch (InterruptedException e)
					{
					}
				}
			}
		});
		App.getExecutionService().runApp(targetApkPath, packageName);
	}
	
	@Override
	protected void onStart()
	{
		super.onStart();
		Map<String, String> analyticsParams = new HashMap<String, String>();
		analyticsParams.put("isPremiumKeyInstalled", Boolean.toString(App.getLicenseService().isPremiumKeyInstalled()));
		analyticsParams.put("isPremium", Boolean.toString(App.getLicenseService().isPremium()));
		analyticsParams.put("isPremiumHacked", Boolean.toString(!App.getLicenseService().isPremiumKeyInstalled() && App.getLicenseService().isPremium()));
		analyticsParams.put("isUiBuilderKeyInstalled", Boolean.toString(App.getLicenseService().isUiBuilderKeyInstalled()));
		analyticsParams.put("isUiBuilderLicensed", Boolean.toString(App.getLicenseService().isUIDesignerLicensed()));

		analyticsParams.put("tipsEnabled", Boolean.toString(App.getAppTipService().isEnabled()));
		String premiumHistory;
		if (!App.getAppTipService().isPremium())
			premiumHistory = "not_premium";
		else if (App.getAppTipService().wasPremium())
			premiumHistory = "was_premium";
		else if (App.getAppTipService().isEnabled())
			premiumHistory = "converted_with_tips";
		else
			premiumHistory = "converted_without_tips";
		analyticsParams.put("tipsPremiumConversion", premiumHistory);

		Analytics.startActivity(this, "Main", analyticsParams);
		
		Map<String, String> analyticsParams2 = new HashMap<String, String>();

		String osArch = System.getProperty("os.arch");
		if (osArch == null) osArch = "unknown";
		analyticsParams2.put("arch", osArch);

		analyticsParams2.put("cores", Integer.toString(Runtime.getRuntime().availableProcessors()));
		analyticsParams2.put("maxMemory", Long.toString(Runtime.getRuntime().maxMemory() / 1024 / 1024));
		analyticsParams2.put("firstSeenVersion", Integer.toString(firstSeenVersion));
		Analytics.logEvent("Session data", analyticsParams2);
		
		File installReferrerDataDir = getDir(AnalyticsInstallReferrerReceiver.DATA_DIR_NAME, Context.MODE_PRIVATE);
		Analytics.logInstalledEvents("AIDE installed", installReferrerDataDir);
		if (!App.CURRENT.equals(App.AIDE_PHONEGAP))
		{
			File premimumInstallReferrerDataDir = new File(installReferrerDataDir.getPath().replace(
					getPackageName(), PremiumKeyLicensingServiceInterface.AIDE_PREMIUM_KEY_PACKAGE_NAME));
			Analytics.logInstalledEvents("AIDE Premium Key installed", premimumInstallReferrerDataDir);
		}
		started = true;
	}
	
	@Override
	protected void onStop()
	{
		super.onStop();
		Analytics.endActivity(this, "Main");
		started = false;
	}
	
	public boolean isActive()
	{
		return started;
	}
}
