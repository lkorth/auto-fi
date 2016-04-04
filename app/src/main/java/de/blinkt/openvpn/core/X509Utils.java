package de.blinkt.openvpn.core;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;

public class X509Utils {

	public static Certificate[] getCertificatesFromFile(String filename) throws FileNotFoundException,
            CertificateException {
		CertificateFactory factory = CertificateFactory.getInstance("X.509");

        List<Certificate> certificates = new ArrayList<>();
		if (VpnProfile.isEmbedded(filename)) {
            int index = filename.indexOf("-----BEGIN CERTIFICATE-----");
            do {
                // The java certificate reader does not ignore chars before the --BEGIN
                index = Math.max(0, index);
                InputStream inputStream = new ByteArrayInputStream(filename.substring(index).getBytes());
                certificates.add(factory.generateCertificate(inputStream));

                index = filename.indexOf("-----BEGIN CERTIFICATE-----", index + 1);
            } while (index > 0);
            return certificates.toArray(new Certificate[certificates.size()]);
        } else {
			InputStream inputStream = new FileInputStream(filename);
            return new Certificate[] {factory.generateCertificate(inputStream)};
        }
	}
}
