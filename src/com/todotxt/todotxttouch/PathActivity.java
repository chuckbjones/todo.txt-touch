package com.todotxt.todotxttouch;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListActivity;
import com.todotxt.todotxttouch.remote.RemoteFolder;

public class PathActivity extends SherlockListActivity {
	final static String TAG = PathActivity.class.getSimpleName();

	private static final int ADD_NEW = 1;

	private static PathActivity currentActivityPointer = null;

	private TodoApplication mApp;
	private Tree<RemoteFolder> mDirectoryTree;
	private Tree<RemoteFolder> mCurrentSelection;

	private ProgressDialog mProgressDialog = null;
	String mDialogText = "";
	Boolean mDialogActive = false;

	private TextView mCurrentFolderTextView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		currentActivityPointer = this;

		setContentView(R.layout.path_activity);

		mApp = (TodoApplication) getApplication();
		mCurrentFolderTextView = (TextView) findViewById(R.id.folder_name);

		// Inflate a "Done/Discard" custom action bar view.
		LayoutInflater inflater = (LayoutInflater) getSupportActionBar()
				.getThemedContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		final View customActionBarView = inflater.inflate(
				R.layout.actionbar_custom_view_done_discard, null);
		customActionBarView.findViewById(R.id.actionbar_done)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						// "Done"
						setResult(RESULT_OK, new Intent().putExtra(mApp.m_prefs
								.todo_path_key(), mCurrentSelection.getData()
								.getPath()));
						finish();
					}
				});
		customActionBarView.findViewById(R.id.actionbar_discard)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						// "Discard"
						finish();
					}
				});

		// Show the custom action bar view and hide the normal Home icon and
		// title.
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
				ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
						| ActionBar.DISPLAY_SHOW_TITLE);
		actionBar.setCustomView(customActionBarView,
				new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.MATCH_PARENT));

		setListAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1));

		mDirectoryTree = new Tree<RemoteFolder>(mApp.getRemoteClientManager()
				.getRemoteClient().getRootFolder());
		setCurrentSelection(mDirectoryTree);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (position == 0 && mCurrentSelection.getParent() != null) {
			// go back up to previous directory
			setCurrentSelection(mCurrentSelection.getParent());
		} else if (position == l.getAdapter().getCount() - 1) {
			// add new
			showDialog(ADD_NEW);
		} else {
			// drill down to this directory
			int index = mCurrentSelection.getParent() == null ? position
					: position - 1;
			setCurrentSelection(mCurrentSelection.getChild(index));
		}
	}

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
		@SuppressWarnings("unchecked")
		ArrayAdapter<String> adapter = (ArrayAdapter<String>) getListAdapter();
		adapter.clear();
		Tree<RemoteFolder> parent = mCurrentSelection.getParent();
		if (parent != null) {
			adapter.add(getString(R.string.todo_path_prev_folder, parent.getData().getName()));
		}
		if (list != null) {
			for (Tree<RemoteFolder> folder : list) {
				adapter.add(folder.getData().getName());
			}
		}
		adapter.add(getString(R.string.todo_path_add_new));
	}

	private void getRemoteDirectoryListing() {
		new AsyncTask<Void, Void, List<RemoteFolder>>() {

			@Override
			protected void onPreExecute() {
				showProgressDialog(getString(R.string.todo_path_loading));
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
				PathActivity.currentActivityPointer.dismissProgressDialog();
				for (RemoteFolder folder : result) {
					mCurrentSelection.addChild(folder);
				}
				mCurrentSelection.setLoaded();
				populateListView(mCurrentSelection.getChildren());
			}
		}.execute();
	}

	protected void dismissProgressDialog() {
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
			mDialogActive = false;
		}
	}

	protected ProgressDialog showProgressDialog(String message) {
		if (mProgressDialog != null) {
			dismissProgressDialog();
		}
		mDialogText = message;
		mDialogActive = true;
		return (mProgressDialog = ProgressDialog.show(PathActivity.this,
				message, getString(R.string.wait_progress), true));
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean("DialogActive", mDialogActive);
		outState.putString("DialogText", mDialogText);

		dismissProgressDialog();
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		mDialogActive = state.getBoolean("DialogActive");
		mDialogText = state.getString("DialogText");
		if (mDialogActive) {
			showProgressDialog(mDialogText);
		}
	}

	@Override
	protected Dialog onCreateDialog(final int id) {
		if (id == ADD_NEW) {
			AlertDialog.Builder addNew = new AlertDialog.Builder(this);
			addNew.setTitle(R.string.todo_path_add_new_title);
			final EditText input = new EditText(this);
			addNew.setView(input);
			addNew.setPositiveButton(R.string.todo_path_add_new_ok_button,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface arg0, int arg1) {
							Tree<RemoteFolder> folder = mCurrentSelection
									.addChild(new RemoteFolder() {
										String name = input.getText()
												.toString();
										String path = new java.io.File(
												mCurrentSelection.getData()
														.getPath(), name)
												.getPath();

										@Override
										public String getName() {
											return name;
										}

										@Override
										public String getPath() {
											return path;
										}
									});
							setCurrentSelection(folder);
						}
					});
			addNew.setNegativeButton(R.string.todo_path_add_new_cancel_button,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface arg0, int arg1) {
						}
					});
			addNew.setOnCancelListener(new OnCancelListener() {
				@SuppressWarnings("deprecation")
				@Override
				public void onCancel(DialogInterface dialog) {
					removeDialog(id);
				}
			});
			return addNew.create();
		}
		return null;
	}
}
