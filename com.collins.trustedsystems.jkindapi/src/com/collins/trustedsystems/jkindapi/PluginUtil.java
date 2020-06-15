package com.collins.trustedsystems.jkindapi;

import java.io.File;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import jkind.JKindException;

public class PluginUtil {

	public static String getJKindJar() {
		Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
		URL url = bundle.getEntry("dependencies/jkind.jar");
		try {
			URL fileUrl = FileLocator.toFileURL(url);
			return new File(fileUrl.getPath()).getCanonicalPath();
		} catch (Exception e) {
			throw new JKindException("Unable to extract jkind.jar from plug-in", e);
		}
	}

}
