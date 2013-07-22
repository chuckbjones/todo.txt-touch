package com.todotxt.todotxttouch.remote;

import com.dropbox.client2.DropboxAPI;
import com.todotxt.todotxttouch.util.Path;
import com.todotxt.todotxttouch.util.Strings;

class DropboxRemoteFolder extends RemoteFolderImpl {
	private static final String ROOT_PATH = "/";
	private static final String ROOT_NAME = "Dropbox";
	
	public DropboxRemoteFolder(DropboxAPI.Entry metadata) {
		super(metadata.path, metadata.fileName(), metadata.parentPath(), Path
				.fileName(metadata.parentPath()));
	}

	public DropboxRemoteFolder(String path) {
		super(Strings.isBlank(path) ? ROOT_PATH : path);
	}
	
	@Override
	public String getName() {
		if (mPath.equals(ROOT_PATH)) {
			return ROOT_NAME;
		}
		return super.getName();
	}
	
	@Override
	public String getParentName() {
		if (mParentPath.equals(ROOT_PATH)) {
			return ROOT_NAME;
		}
		return super.getParentName();
	}
}
