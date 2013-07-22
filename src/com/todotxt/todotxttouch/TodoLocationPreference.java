package com.todotxt.todotxttouch;

import java.util.List;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
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
import com.todotxt.todotxttouch.util.Tree;

public class TodoLocationPreference extends DialogPreference {
	final static String TAG = TodoLocationPreference.class.getSimpleName();

	private TodoApplication mApp;
	private boolean mWarningMode = false;
	private boolean mDisplayWarning = false;
	private ArrayAdapter<String> mAdapter;
	private String mInitialPath;
	private Tree<RemoteFolder> mRootFolder;
	private Tree<RemoteFolder> mCurrentSelection;

	private ListView mListView;
	private View mEmptyView;
	private TextView mCurrentFolderTextView;

	public TodoLocationPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDialogLayoutResource(R.layout.todo_location_dialog);
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

		// If we are already displaying the default layout then either persist
		// the change or cancel, depending on which button was pressed
		if (positiveResult && mCurrentSelection != null) {
			String value = mCurrentSelection.getData().getPath();
			if (callChangeListener(value)) {
				persistString(value);
			}
		}
		mInitialPath = null;
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getString(index);
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		if (mInitialPath == null) {
			mInitialPath = restoreValue ? getPersistedString(null)
					: (String) defaultValue;
		}
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
	protected View onCreateDialogView() {
		if (mWarningMode) {
			return null;
		}
		return super.onCreateDialogView();
	}
	
	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);

		mCurrentFolderTextView = (TextView) view.findViewById(R.id.folder_name);
		mListView = (ListView) view.findViewById(android.R.id.list);
		mEmptyView = view.findViewById(android.R.id.empty);

		mAdapter = new ArrayAdapter<String>(getContext(),
				android.R.layout.simple_list_item_1);
		mListView.setAdapter(mAdapter);
		mListView.setEmptyView(mEmptyView);

		// initialize the view
		initFolderTree();
		selectFolder(mCurrentSelection);

		mListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (position == 0 && mCurrentSelection.getData().hasParent()) {
					// go back up to previous directory
					upToParent();
				} else if (position == mAdapter.getCount() - 1) {
					// FIXME: add new
					// showDialog(ADD_NEW);
				} else {
					// drill down to this directory
					int index = mCurrentSelection.getParent() == null ? position
							: position - 1;
					selectFolder(mCurrentSelection.getChild(index));
				}
			}
		});
	}

	private void initFolderTree() {
		if (mRootFolder == null) {
			mRootFolder = mCurrentSelection = new Tree<RemoteFolder>(mApp
					.getRemoteClientManager().getRemoteClient()
					.getFolder(mInitialPath));
		} else {
			// use initialPath to find the correct folder in the tree
			Tree<RemoteFolder> tree = findFolderInTree(mRootFolder,
					mInitialPath);
			if (tree != null) {
				mCurrentSelection = tree;
			}
		}

	}

	private Tree<RemoteFolder> findFolderInTree(Tree<RemoteFolder> tree,
			String path) {
		if (tree.getData().getPath().equalsIgnoreCase(path)) {
			return tree;
		}

		if (tree.isLoaded()) {
			for (Tree<RemoteFolder> child : tree.getChildren()) {
				Tree<RemoteFolder> res = findFolderInTree(child, path);
				if (res != null) {
					return res;
				}
			}
		}
		return null;
	}

	private void upToParent() {
		if (mCurrentSelection.getParent() != null) {
			selectFolder(mCurrentSelection.getParent());
			return;
		}
		RemoteFolder parent = mApp.getRemoteClientManager().getRemoteClient()
				.getFolder(mCurrentSelection.getData().getParentPath());
		mRootFolder = new Tree<RemoteFolder>(parent);
		selectFolder(mRootFolder);
	}

	private void setCurrentSelection(Tree<RemoteFolder> folder) {
		mCurrentSelection = folder;
		mCurrentFolderTextView.setText(folder.getData().getName());
		populateListView(folder.getChildren());
	}

	private void selectFolder(Tree<RemoteFolder> folder) {
		if (!folder.isLoaded()) {
			getRemoteDirectoryListing(folder);
		} else {
			setCurrentSelection(folder);
		}
	}

	private void populateListView(List<Tree<RemoteFolder>> list) {
		mAdapter.clear();
		if (mCurrentSelection.getData().hasParent()) {
			mAdapter.add(getContext().getString(R.string.todo_path_prev_folder,
					mCurrentSelection.getData().getParentName()));
		}
		if (list != null) {
			for (Tree<RemoteFolder> folder : list) {
				mAdapter.add(folder.getData().getName());
			}
		}
		mAdapter.add(getContext().getString(R.string.todo_path_add_new));
	}

	private void getRemoteDirectoryListing(final Tree<RemoteFolder> folder) {
		new AsyncTask<Void, Void, List<RemoteFolder>>() {
			@Override
			protected void onPreExecute() {
				showProgressIndicator();
				mAdapter.clear();
			}

			@Override
			protected List<RemoteFolder> doInBackground(Void... params) {
				try {
					return mApp.getRemoteClientManager().getRemoteClient()
							.getSubFolders(folder.getData().getPath());
				} catch (Exception e) {
					Log.d(TAG, "failed to get remote folder list", e);
				}
				return null;
			}

			@Override
			protected void onPostExecute(List<RemoteFolder> result) {
				Dialog dialog = getDialog();
				if (dialog == null || !dialog.isShowing()) {
					return;
				}

				if (result == null) {
					showErrorMessage();
					return;
				}

				for (RemoteFolder child : result) {
					if (mCurrentSelection.getData().equals(child)) {
						// if we just loaded the parent of our current folder
						// add it as a child so we can keep it's children
						folder.addChild(mCurrentSelection);
					} else {
						folder.addChild(child);
					}
				}

				folder.setLoaded();
				setCurrentSelection(folder);
			}
		}.execute();
	}

	protected void showErrorMessage() {
		mEmptyView.findViewById(R.id.loading_spinner).setVisibility(View.GONE);
		mEmptyView.findViewById(R.id.empty_text).setVisibility(View.VISIBLE);
	}

	protected void showProgressIndicator() {
		mEmptyView.findViewById(R.id.empty_text).setVisibility(View.GONE);
		mEmptyView.findViewById(R.id.loading_spinner).setVisibility(
				View.VISIBLE);
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
		if (mCurrentSelection != null) {
			myState.initialPath = mCurrentSelection.getData().getPath();
			//FIXME: need to save the entire tree.
		} else {
			myState.initialPath = mInitialPath;
		}
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
		mInitialPath = myState.initialPath;
		super.onRestoreInstanceState(myState.getSuperState());
	}

	private static class SavedState extends BaseSavedState {
		boolean warningMode;
		String initialPath;

		public SavedState(Parcel source) {
			super(source);
			boolean[] array = new boolean[1];
			source.readBooleanArray(array);
			warningMode = array[0];
			initialPath = source.readString();
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeBooleanArray(new boolean[] { warningMode });
			dest.writeString(initialPath);
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
