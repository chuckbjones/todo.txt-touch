package com.todotxt.todotxttouch;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.todotxt.todotxttouch.remote.RemoteFolder;

public class TodoLocationPreference extends DialogPreference {
	final static String TAG = TodoLocationPreference.class.getSimpleName();

	private TodoApplication mApp;
	private boolean mWarningMode = false;
	private boolean mDisplayWarning = false;
	private int mClickedDialogEntryIndex;
	private ArrayAdapter<String> mAdapter;
	private Tree<RemoteFolder> mDirectoryTree;
	private Tree<RemoteFolder> mCurrentSelection;

	private ListView mListView;
	private TextView mCurrentFolderTextView;
	
	public TodoLocationPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDialogLayoutResource(R.layout.path_activity);
	}

	public boolean shouldDisplayWarning() {
		return mDisplayWarning;
	}

	public void setDisplayWarning(boolean shouldDisplay) {
		mDisplayWarning = shouldDisplay;
	}

	public void setApplication(TodoApplication app) {
		mApp = app;
	}

	private CharSequence getWarningMessage() {
		SpannableString ss = new SpannableString(getContext().getString(
				R.string.todo_path_warning));
		ss.setSpan(new ForegroundColorSpan(Color.RED), 0, ss.length(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		return ss;
	}

	@Override
	protected void onClick() {
		// Called when the preference is clicked
		// This method displays the dialog.
		// When mDisplayWarning is set, we want to display
		// a warning message instead of the actual dialog
		mWarningMode = mDisplayWarning;
		super.onClick();
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		// If we are displaying the warning message and the user
		// clicked "I'm feeling dangerous", then redisplay the
		// dialog with the default layout
		if (mWarningMode && positiveResult) {
			mWarningMode = false;
			showDialog(null);
			return;
		}

		// If we are already displaying the default layout
		// do the default processing (either persist the change
		// or cancel, depending on which button was pressed)
		super.onDialogClosed(positiveResult);
	}

	@Override
	protected void onPrepareDialogBuilder(Builder builder) {
		// Display the warning message if necessary.
		// Otherwise, just use the default layout.
		if (mWarningMode) {
			builder.setMessage(getWarningMessage());
			builder.setPositiveButton(R.string.todo_path_warning_override, this);
		} else {
			// nothing to do here...
		}
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);

		mCurrentFolderTextView = (TextView) view.findViewById(R.id.folder_name);
		mListView = (ListView) view.findViewById(android.R.id.list);

		mAdapter = new ArrayAdapter<String>(getContext(),
				android.R.layout.simple_list_item_1);

		mListView.setAdapter(mAdapter);

		mDirectoryTree = new Tree<RemoteFolder>(mApp.getRemoteClientManager()
				.getRemoteClient().getRootFolder());
		setCurrentSelection(mDirectoryTree);
		
		mListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				mClickedDialogEntryIndex = position;				
				if (position == 0 && mCurrentSelection.getParent() != null) {
					// go back up to previous directory
					setCurrentSelection(mCurrentSelection.getParent());
				} else if (position == mAdapter.getCount() - 1) {
					// FIXME: add new
					// showDialog(ADD_NEW);
				} else {
					// drill down to this directory
					int index = mCurrentSelection.getParent() == null ? position
							: position - 1;
					setCurrentSelection(mCurrentSelection.getChild(index));
				}
			}
		});
	}

	// @Override
	// protected void showDialog(Bundle state) {
	// super.showDialog(state);
	// Dialog dialog = getDialog();
	// DisplayMetrics displaymetrics = new DisplayMetrics();
	// dialog.getWindow().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
	// int height = (int) (displaymetrics.heightPixels * 0.67);
	//
	// WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
	// params.height = height;
	// dialog.getWindow().setAttributes(params);
	// }

	class Tree<E> {
		private Tree<E> parent = null;
		private List<Tree<E>> children = null;
		private E data;

		public Tree(E data) {
			this.data = data;
		}

		public Tree(Tree<E> parent, E data) {
			this.parent = parent;
			this.data = data;
		}

		public Tree<E> addChild(E data) {
			Tree<E> child = new Tree<E>(this, data);
			if (children == null)
				children = new ArrayList<Tree<E>>();
			children.add(child);
			return child;
		}

		public E getData() {
			return data;
		}

		public Tree<E> getParent() {
			return parent;
		}

		public boolean isLoaded() {
			return children != null;
		}

		public void setLoaded() {
			if (children == null)
				children = new ArrayList<Tree<E>>();
		}

		public List<Tree<E>> getChildren() {
			return children;
		}

		public Tree<E> getChild(int position) {
			return children.get(position);
		}
	}

	private void setCurrentSelection(Tree<RemoteFolder> folder) {
		mCurrentSelection = folder;
		mCurrentFolderTextView.setText(folder.getData().getName());
		populateListView();
	}

	private void populateListView() {
		if (!mCurrentSelection.isLoaded()) {
			getRemoteDirectoryListing();
		} else {
			populateListView(mCurrentSelection.getChildren());
		}
	}

	private void populateListView(List<Tree<RemoteFolder>> list) {
		mAdapter.clear();
		Tree<RemoteFolder> parent = mCurrentSelection.getParent();
		if (parent != null) {
			mAdapter.add(getContext().getString(R.string.todo_path_prev_folder,
					parent.getData().getName()));
		}
		if (list != null) {
			for (Tree<RemoteFolder> folder : list) {
				mAdapter.add(folder.getData().getName());
			}
		}
		mAdapter.add(getContext().getString(R.string.todo_path_add_new));
	}

	private void getRemoteDirectoryListing() {
		new AsyncTask<Void, Void, List<RemoteFolder>>() {

			@Override
			protected void onPreExecute() {
				// showProgressDialog(getContext().getString(R.string.todo_path_loading));
			}

			@Override
			protected List<RemoteFolder> doInBackground(Void... params) {
				try {
					return mApp
							.getRemoteClientManager()
							.getRemoteClient()
							.getSubFolders(
									mCurrentSelection.getData().getPath());
				} catch (Exception e) {
					Log.d(TAG, "failed to get remote folder list", e);
				}
				return new ArrayList<RemoteFolder>();
			}

			@Override
			protected void onPostExecute(List<RemoteFolder> result) {
				// PathActivity.currentActivityPointer.dismissProgressDialog();
				for (RemoteFolder folder : result) {
					mCurrentSelection.addChild(folder);
				}
				mCurrentSelection.setLoaded();
				populateListView(mCurrentSelection.getChildren());
			}
		}.execute();
	}

	// protected boolean needInputMethod() {
	// // We want the input method to show, if possible, when edit dialog is
	// // displayed, but not when warning message is displayed
	// return !mWarningMode;
	// }

	@Override
	protected Parcelable onSaveInstanceState() {
		final Parcelable superState = super.onSaveInstanceState();

		final SavedState myState = new SavedState(superState);
		myState.warningMode = mWarningMode;
		return myState;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (state == null || !state.getClass().equals(SavedState.class)) {
			// Didn't save state for us in onSaveInstanceState
			super.onRestoreInstanceState(state);
			return;
		}

		SavedState myState = (SavedState) state;
		mWarningMode = myState.warningMode;
		super.onRestoreInstanceState(myState.getSuperState());
	}

	private static class SavedState extends BaseSavedState {
		boolean warningMode;

		public SavedState(Parcel source) {
			super(source);
			boolean[] array = new boolean[1];
			source.readBooleanArray(array);
			warningMode = array[0];
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeBooleanArray(new boolean[] { warningMode });
		}

		public SavedState(Parcelable superState) {
			super(superState);
		}

		@SuppressWarnings("unused")
		public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}

}
