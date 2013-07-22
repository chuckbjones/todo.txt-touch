package com.todotxt.todotxttouch.remote;

public interface RemoteFolder {

	/**
	 * @return The folder's name without any path information.
	 */
	String getName();
	
	/**
	 * @return The full path of the folder.
	 */
	String getPath();

	/**
	 * @return The name of the parent without any path information.
	 */
	String getParentName();

	/**
	 * @return The full path of the folder's parent.
	 */
	String getParentPath();

	/**
	 * @return True if this is not a root folder.
	 */
	boolean hasParent();

}
