package com.lukekorth.auto_fi.services;

import android.app.IntentService;
import android.content.Intent;

import com.lukekorth.auto_fi.BuildConfig;
import com.lukekorth.auto_fi.openvpn.OpenVpnConfiguration;
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
import java.util.UUID;

import javax.security.auth.x500.X500Principal;

import static java.net.HttpURLConnection.HTTP_OK;

public class OpenVpnConfigurationIntentService extends IntentService {

    public OpenVpnConfigurationIntentService() {
        super(OpenVpnConfigurationIntentService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (OpenVpnConfiguration.isSetup(this)) {
            return;
        }

        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(
                    new X500Principal(generateCommonName()), keyPair.getPublic());
            PKCS10CertificationRequest csr = p10Builder.build(
                    new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate()));

            String encodedCsr = encodeKey(csr.getEncoded(), "CERTIFICATE REQUEST");
            String publicKey = exchangeCsrForPublicKey(encodedCsr);

            OpenVpnConfiguration.writeKeyPair(this, publicKey,
                    encodeKey(keyPair.getPrivate().getEncoded(), "PRIVATE KEY"));
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
            connection = (HttpURLConnection) new URL("http://" + BuildConfig.SERVER_IP + "/sign-csr/")
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

    private String generateCommonName() {
        String debug = BuildConfig.DEBUG ? "debug" : "release";

        return "CN=auto_fi|" + BuildConfig.VERSION_NAME + "-" + debug + "|" +
                UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
