package com.todotxt.todotxttouch.remote;

import com.dropbox.client2.DropboxAPI;

class DropboxRemoteFolder implements RemoteFolder {
	private DropboxAPI.Entry metadata;
	
	public DropboxRemoteFolder(DropboxAPI.Entry metadata) {
		this.metadata = metadata;
	}

	@Override
	public String getName() {
		return metadata.fileName();
	}

	@Override
	public String getPath() {
		return metadata.path;
	}

	@Override
	public String toString() {
		return getName();
	}

}
