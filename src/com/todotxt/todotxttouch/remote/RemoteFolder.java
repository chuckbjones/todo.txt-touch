package com.todotxt.todotxttouch.remote;

public interface RemoteFolder {

	/**
	 * @return The folder's name without any path information.
	 */
	String getName();
	
	/**
	 * @return The full path of the folder
	 */
	String getPath();
}
