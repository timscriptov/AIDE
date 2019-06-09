package com.aide.ui.activities;

import java.util.*;

import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

import com.aide.ui.*;

public class CommunityActivity extends Activity
{
	public static void show(Activity parent)
	{
		Intent intent = new Intent(parent, CommunityActivity.class);
		parent.startActivity(intent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.community);

		List<CommunityListEntry> entries = new ArrayList<CommunityListEntry>();
		entries.add(new CommunityListEntry(R.drawable.community_twitter, "Tweet", new Runnable()
		{
			public void run()
			{
				String message = "Coding " + getOnMyDeviceString() + " with AIDE - Android Java IDE (@AndroidIDE) http://bit.ly/wYpqZD";

				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("http://twitter.com/?status=" + Uri.encode(message)));
				intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				startActivity(intent);
			}
		}));
		entries.add(new CommunityListEntry(R.drawable.community_twitter_follow, "Follow", new Runnable()
		{
			public void run()
			{
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("https://twitter.com/#!/AndroidIDE"));
				intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				startActivity(intent);
			}
		}));
		entries.add(new CommunityListEntry(R.drawable.community_googleplus, "Google+", new Runnable()
		{
			public void run()
			{
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("https://plus.google.com/101304250883271700981/posts"));
				intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				startActivity(intent);
			}
		}));
		entries.add(new CommunityListEntry(R.drawable.community_facebook, "facebook", new Runnable()
		{
			public void run()
			{
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("http://www.facebook.com/pages/AIDE-Android-Java-IDE/239564276138537"));
				intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				startActivity(intent);
			}
		}));
		entries.add(new CommunityListEntry(R.drawable.community_market, "Review", new Runnable()
		{
			public void run()
			{
				App.getActivity().openMarket();
			}
		}));
		entries.add(new CommunityListEntry(R.drawable.community_email, "Email", new Runnable()
		{
			public void run()
			{
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType("message/rfc822"); // use from live device
				intent.putExtra(Intent.EXTRA_EMAIL, new String[]
				{
					"ide4android@googlemail.com"
				});
				intent.putExtra(Intent.EXTRA_SUBJECT, "AIDE " + getOnMyDeviceString() + " SDK " + android.os.Build.VERSION.SDK_INT);
				startActivity(Intent.createChooser(intent, "Select email application."));
			}
		}));

		final ListView listView = (ListView) findViewById(R.id.communityList);
		listView.setAdapter(new CommunityListEntryAdapter(this, entries));

		listView.setOnItemClickListener(new OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				CommunityListEntry entry = (CommunityListEntry) listView.getItemAtPosition(position);
				entry.runnable.run();
			}
		});
	}

	private String getOnMyDeviceString()
	{
		String model = android.os.Build.MODEL;
		String device;
		if (model == null || model.length() > 40)
			device = "";
		else
			device = "on my " + model;
		return device;
	}

	private class CommunityListEntry
	{
		private String label;
		private int icon;
		private Runnable runnable;

		public CommunityListEntry(int icon, String label, Runnable runnable)
		{
			this.icon = icon;
			this.label = label;
			this.runnable = runnable;
		}
	}

	private class CommunityListEntryAdapter extends ArrayAdapter<CommunityListEntry>
	{
		public CommunityListEntryAdapter(Context context, List<CommunityListEntry> entries)
		{
			super(context, R.layout.community_entry, entries);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View view = convertView;
			if (view == null)
			{
				LayoutInflater inflater = LayoutInflater.from(getContext());
				view = inflater.inflate(R.layout.community_entry, parent, false);
			}

			CommunityListEntry entry = (CommunityListEntry) getItem(position);

			TextView labelView = (TextView) view.findViewById(R.id.communityEntryLabel);
			labelView.setText(entry.label);

			ImageView imageView = (ImageView) view.findViewById(R.id.communityEntryImage);
			imageView.setImageResource(entry.icon);

			return view;
		}
	}
}
