package com.lukekorth.auto_fi.services;

import android.app.IntentService;
import android.content.Intent;

import com.lukekorth.auto_fi.BuildConfig;
import com.lukekorth.auto_fi.openvpn.OpenVpnSetup;
import com.lukekorth.auto_fi.utilities.FileUtils;
import com.lukekorth.auto_fi.utilities.Logger;
import com.lukekorth.auto_fi.utilities.StreamUtils;

import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;
import org.spongycastle.pkcs.PKCS10CertificationRequest;
import org.spongycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.spongycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.spongycastle.util.io.pem.PemObject;
import org.spongycastle.util.io.pem.PemWriter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import javax.security.auth.x500.X500Principal;

import static java.net.HttpURLConnection.HTTP_OK;

public class OpenVpnConfigurationIntentService extends IntentService {

    public OpenVpnConfigurationIntentService() {
        super(OpenVpnConfigurationIntentService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(
                    new X500Principal("CN=auto-fi client"), keyPair.getPublic());
            PKCS10CertificationRequest csr = p10Builder.build(
                    new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate()));

            String encodedCsr = encodeKey(csr.getEncoded(), "CERTIFICATE REQUEST");
            String publicKey = exchangeCsrForPublicKey(encodedCsr);
            FileUtils.writeToDisk(this, publicKey, "open_vpn_public_key.crt");

            String encodedPrivateKey = encodeKey(keyPair.getPrivate().getEncoded(), "PRIVATE KEY");
            FileUtils.writeToDisk(this, encodedPrivateKey, "open_vpn_private_key.key");

            OpenVpnSetup.writeConfigurationFile(this, publicKey, encodedPrivateKey);
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    private String encodeKey(byte[] key, String label) throws IOException {
        PemObject pemObject = new PemObject(label, key);

        StringWriter stringWriter = new StringWriter();
        PemWriter pemWriterPrivateKey = new PemWriter(stringWriter);
        pemWriterPrivateKey.writeObject(pemObject);
        pemWriterPrivateKey.close();
        stringWriter.close();

        return stringWriter.toString();
    }

    private String exchangeCsrForPublicKey(String csr) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL("http://" + BuildConfig.SERVER_ADDRESS + "/sign-csr/")
                    .openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            Writer out = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
            out.write(csr, 0, csr.length());
            out.flush();
            out.close();

            int responseCode = connection.getResponseCode();
            if (responseCode == HTTP_OK) {
                return StreamUtils.readStream(connection.getInputStream());
            } else {
                throw new IOException("Request returned HTTP code " + responseCode);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
