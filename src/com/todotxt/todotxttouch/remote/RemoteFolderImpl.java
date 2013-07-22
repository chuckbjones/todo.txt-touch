package com.todotxt.todotxttouch.remote;

import com.todotxt.todotxttouch.util.Path;
import com.todotxt.todotxttouch.util.Strings;

class RemoteFolderImpl implements RemoteFolder {
	protected String mPath;
	protected String mName;
	protected String mParentPath;
	protected String mParentName;
	
	public RemoteFolderImpl(String path) {
		mPath = path;
		mName = Path.fileName(path);
		mParentPath = Path.parentPath(path);
		mParentName = Path.fileName(mParentPath);
	}

	public RemoteFolderImpl(String path, String name, String parentPath, String parentName) {
		mPath = path;
		mName = name;
		mParentPath = parentPath;
		mParentName = parentName;
	}

	@Override
	public String getName() {
		return mName;
	}

	@Override
	public String getParentName() {
		return mParentName;
	}

	@Override
	public String getPath() {
		return mPath;
	}
	
	@Override
	public String getParentPath() {
		return mParentPath;
	}

	@Override
	public boolean hasParent() {
		return !Strings.isBlank(getParentPath());
	}
	
	@Override
	public String toString() {
		return getName();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof RemoteFolder) {
			return getPath().equalsIgnoreCase(((RemoteFolder)o).getPath());
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return getPath().hashCode();
	}

}
