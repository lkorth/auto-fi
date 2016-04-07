package de.blinkt.openvpn.core;

import com.lukekorth.auto_fi.BuildConfig;
import com.lukekorth.auto_fi.R;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

public class ProxyDetection {

	public static SocketAddress detectProxy() {
		try {
			URL url = new URL(String.format("https://" + BuildConfig.SERVER_ADDRESS + ":" + BuildConfig.SERVER_PORT));
			Proxy proxy = getFirstProxy(url);

			if(proxy == null) {
				return null;
			}

			SocketAddress addr = proxy.address();
			if (addr instanceof InetSocketAddress) {
				return addr; 
			}
		} catch (MalformedURLException e) {
			VpnStatus.logError(R.string.getproxy_error, e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			VpnStatus.logError(R.string.getproxy_error, e.getLocalizedMessage());
		}

		return null;
	}

	private static Proxy getFirstProxy(URL url) throws URISyntaxException {
		System.setProperty("java.net.useSystemProxies", "true");

		List<Proxy> proxylist = ProxySelector.getDefault().select(url.toURI());
		if (proxylist != null) {
			for (Proxy proxy: proxylist) {
				SocketAddress addr = proxy.address();
				if (addr != null) {
					return proxy;
				}
			}

		}

		return null;
	}
}