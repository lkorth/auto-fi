/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Vector;

public class X509Utils {

	public static Certificate[] getCertificatesFromFile(String certfilename) throws FileNotFoundException, CertificateException {
		CertificateFactory certFact = CertificateFactory.getInstance("X.509");

        Vector<Certificate> certificates = new Vector<>();
		if(VpnProfile.isEmbedded(certfilename)) {
            int subIndex = certfilename.indexOf("-----BEGIN CERTIFICATE-----");
            do {
                // The java certifcate reader is ... kind of stupid
                // It does NOT ignore chars before the --BEGIN ...

                subIndex = Math.max(0, subIndex);
                InputStream inStream = new ByteArrayInputStream(certfilename.substring(subIndex).getBytes());
                certificates.add(certFact.generateCertificate(inStream));

                subIndex = certfilename.indexOf("-----BEGIN CERTIFICATE-----", subIndex+1);
            } while (subIndex > 0);
            return certificates.toArray(new Certificate[certificates.size()]);
        } else {
			InputStream inStream = new FileInputStream(certfilename);
            return new Certificate[] {certFact.generateCertificate(inStream)};
        }


	}
}
