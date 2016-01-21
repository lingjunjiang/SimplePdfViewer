package com.artifex.mupdfdemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

import java.io.InputStream;
import java.util.concurrent.Executor;

class ThreadPerTaskExecutor implements Executor {
	public void execute(Runnable r) {
		new Thread(r).start();
	}
}

public class MuPDFActivity extends Activity implements FilePicker.FilePickerSupport
{
	private MuPDFCore    core;
	private String       mFileName;
	private MuPDFReaderView mDocView;


	private MuPDFCore openFile(String path)
	{
		int lastSlashPos = path.lastIndexOf('/');
		mFileName = new String(lastSlashPos == -1
					? path
					: path.substring(lastSlashPos+1));
		System.out.println("Trying to open " + path);
		try
		{
			core = new MuPDFCore(this, path);
			// New file: drop the old outline data
			//OutlineActivityData.set(null);
		}
		catch (Exception e)
		{
			System.out.println(e);
			return null;
		}
		catch (java.lang.OutOfMemoryError e)
		{
			//  out of memory is not an Exception, so we catch it separately.
			System.out.println(e);
			return null;
		}
		return core;
	}

	private MuPDFCore openBuffer(byte buffer[], String magic)
	{
		System.out.println("Trying to open byte buffer");
		try
		{
			core = new MuPDFCore(this, buffer, magic);
			// New file: drop the old outline data
			//OutlineActivityData.set(null);
		}
		catch (Exception e)
		{
			System.out.println(e);
			return null;
		}
		return core;
	}


	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if (core == null) {
			Intent intent = getIntent();
			byte buffer[] = null;

			if (Intent.ACTION_VIEW.equals(intent.getAction())) {
				Uri uri = intent.getData();
				System.out.println("URI to open is: " + uri);
				if (uri.toString().startsWith("content://")) {
					String reason = null;
					try {
						InputStream is = getContentResolver().openInputStream(uri);
						int len = is.available();
						buffer = new byte[len];
						is.read(buffer, 0, len);
						is.close();
					}
					catch (java.lang.OutOfMemoryError e) {
						System.out.println("Out of memory during buffer reading");
						reason = e.toString();
					}
					catch (Exception e) {
						System.out.println("Exception reading from stream: " + e);

						// Handle view requests from the Transformer Prime's file manager
						// Hopefully other file managers will use this same scheme, if not
						// using explicit paths.
						// I'm hoping that this case below is no longer needed...but it's
						// hard to test as the file manager seems to have changed in 4.x.
						try {
							Cursor cursor = getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
							if (cursor.moveToFirst()) {
								String str = cursor.getString(0);
								if (str == null) {
									reason = "Couldn't parse data in intent";
								}
								else {
									uri = Uri.parse(str);
								}
							}
						}
						catch (Exception e2) {
							System.out.println("Exception in Transformer Prime file manager code: " + e2);
							reason = e2.toString();
						}
					}
				}
				if (buffer != null) {
					core = openBuffer(buffer, intent.getType());
				} else {
					String path = Uri.decode(uri.getEncodedPath());
					if (path == null) {
						path = uri.toString();
					}
					core = openFile(path);
				}
				//SearchTaskResult.set(null);
			}
			if (core != null && core.needsPassword()) {
				//requestPassword(savedInstanceState);
				return;
			}
			if (core != null && core.countPages() == 0)
			{
				core = null;
			}
		}
		if (core == null)
		{
			return;
		}

		createUI(savedInstanceState);

	}

	public void createUI(Bundle savedInstanceState) {
		if (core == null)
			return;

		// Now create the UI.
		// First create the document view
		mDocView = new MuPDFReaderView(this) {
			@Override
			protected void onMoveToChild(int i) {
				if (core == null)
					return;
				super.onMoveToChild(i);
			}

			@Override
			protected void onTapMainDocArea() {
			}

			@Override
			protected void onDocMotion() {
				//hideButtons();
			}

			@Override
			protected void onHit(Hit item) {
			}
		};
		mDocView.setAdapter(new MuPDFPageAdapter(this, this, core));


		// Reenstate last state if it was recorded
		SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
		mDocView.setDisplayedViewIndex(prefs.getInt("page" + mFileName, 0));


		// Stick the document view and the buttons overlay into a parent view
		RelativeLayout layout = new RelativeLayout(this);
		layout.addView(mDocView);
		//layout.addView(mButtonsView);
		setContentView(layout);

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);
	}


	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		if (mFileName != null && mDocView != null) {
			outState.putString("FileName", mFileName);

			// Store current page in the prefs against the file name,
			// so that we can pick it up each time the file is loaded
			// Other info is needed only for screen-orientation change,
			// so it can go in the bundle
			SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
			SharedPreferences.Editor edit = prefs.edit();
			edit.putInt("page"+mFileName, mDocView.getDisplayedViewIndex());
			edit.commit();
		}

	}

	@Override
	protected void onPause() {
		super.onPause();

		if (mFileName != null && mDocView != null) {
			SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
			SharedPreferences.Editor edit = prefs.edit();
			edit.putInt("page"+mFileName, mDocView.getDisplayedViewIndex());
			edit.commit();
		}
	}

	public void onDestroy()
	{
		if (mDocView != null) {
			mDocView.applyToChildren(new ReaderView.ViewMapper() {
				void applyToView(View view) {
					((MuPDFView)view).releaseBitmaps();
				}
			});
		}
		if (core != null)
			core.onDestroy();
		core = null;
		super.onDestroy();
	}
	@Override
	protected void onStart() {

		super.onStart();
	}

	@Override
	protected void onStop() {

		super.onStop();
	}

	@Override
	public void onBackPressed() {
			super.onBackPressed();
	}

	@Override
	public void performPickFor(FilePicker picker) {

	}

}
