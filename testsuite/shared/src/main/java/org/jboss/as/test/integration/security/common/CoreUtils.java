/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2014, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */
package org.jboss.as.test.integration.security.common;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.network.NetworkUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;

/**
 * Common utilities for JBoss AS security tests.
 *
 * @author Jan Lanik
 * @author Josef Cacek
 */
public class CoreUtils {

    private static final Logger LOGGER = Logger.getLogger(CoreUtils.class);

    public static final String UTF_8 = "UTF-8";

    /**
     * Return MD5 hash of the given string value, encoded with given {@link Coding}. If the value or coding is <code>null</code>
     * then original value is returned.
     *
     * @return encoded MD5 hash of the string or original value if some of parameters is null
     */
    public static String hashMD5(String value, Coding coding) {
        return (coding == null || value == null) ? value : hash(value, "MD5", coding);
    }

    public static String hash(String target, String algorithm, Coding coding) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] bytes = target.getBytes();
        byte[] byteHash = md.digest(bytes);

        String encodedHash = null;

        switch (coding) {
            case BASE_64:
                encodedHash = Base64.getEncoder().encodeToString(byteHash);
                break;
            case HEX:
                encodedHash = toHex(byteHash);
                break;
            default:
                throw new IllegalArgumentException("Unsuported coding:" + coding.name());
        }

        return encodedHash;
    }

    public static String toHex(byte[] bytes) {
        StringBuffer sb = new StringBuffer(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            // top 4 bits
            char c = (char) ((b >> 4) & 0xf);
            if (c > 9) { c = (char) ((c - 10) + 'a'); } else { c = (char) (c + '0'); }
            sb.append(c);
            // bottom 4 bits
            c = (char) (b & 0xf);
            if (c > 9) { c = (char) ((c - 10) + 'a'); } else { c = (char) (c + '0'); }
            sb.append(c);
        }
        return sb.toString();
    }

    public static URL getResource(String name) {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        return tccl.getResource(name);
    }

    private static final long STOP_DELAY_DEFAULT = 0;

    /**
     * stops execution of the program indefinitely useful in testsuite debugging
     */
    public static void stop() {
        stop(STOP_DELAY_DEFAULT);
    }

    /**
     * stop test execution for a given time interval useful for debugging
     *
     * @param delay interval (milliseconds), if delay<=0, interval is considered to be infinite (Long.MAX_VALUE)
     */
    public static void stop(long delay) {
        long currentTime = System.currentTimeMillis();
        long remainingTime = 0 < delay ? currentTime + delay - System.currentTimeMillis() : Long.MAX_VALUE;
        while (remainingTime > 0) {
            try {
                Thread.sleep(remainingTime);
            } catch (InterruptedException ex) {
                remainingTime = currentTime + delay - System.currentTimeMillis();
                continue;
            }
        }
    }

    public static void applyUpdates(final List<ModelNode> updates, final ModelControllerClient client) throws Exception {
        for (ModelNode update : updates) {
            applyUpdate(update, client);
        }
    }

    public static void applyUpdate(ModelNode update, final ModelControllerClient client) throws Exception {
        ModelNode result = client.execute(new OperationBuilder(update).build());
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Client update: " + update);
            LOGGER.info("Client update result: " + result);
        }
        if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
            LOGGER.debug("Operation succeeded.");
        } else if (result.hasDefined("failure-description")) {
            throw new RuntimeException(result.get("failure-description").toString());
        } else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
        }
    }

    /**
     * Read the contents of an HttpResponse's entity and return it as a String. The content is converted using the character set
     * from the entity (if any), failing that, "ISO-8859-1" is used.
     *
     * @param response
     * @return
     * @throws java.io.IOException
     */
    public static String getContent(HttpResponse response) throws IOException {
        return EntityUtils.toString(response.getEntity());
    }

    /**
     * Makes HTTP call with FORM authentication.
     *
     * @param URL
     * @param user
     * @param pass
     * @param expectedStatusCode
     * @throws Exception
     */
    public static void makeCall(String URL, String user, String pass, int expectedStatusCode) throws Exception {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()){
            HttpGet httpget = new HttpGet(URL);

            HttpResponse response = httpclient.execute(httpget);

            HttpEntity entity = response.getEntity();
            if (entity != null) { EntityUtils.consume(entity); }

            // We should get the Login Page
            StatusLine statusLine = response.getStatusLine();
            System.out.println("Login form get: " + statusLine);
            assertEquals(200, statusLine.getStatusCode());

            // We should now login with the user name and password
            HttpPost httpost = new HttpPost(URL + "/j_security_check");

            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("j_username", user));
            nvps.add(new BasicNameValuePair("j_password", pass));

            httpost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));

            response = httpclient.execute(httpost);
            entity = response.getEntity();
            if (entity != null) { EntityUtils.consume(entity); }

            statusLine = response.getStatusLine();

            // Post authentication - we have a 302
            assertEquals(302, statusLine.getStatusCode());
            Header locationHeader = response.getFirstHeader("Location");
            String location = locationHeader.getValue();

            HttpGet httpGet = new HttpGet(location);
            response = httpclient.execute(httpGet);

            entity = response.getEntity();
            if (entity != null) { EntityUtils.consume(entity); }

            // Either the authentication passed or failed based on the expected status code
            statusLine = response.getStatusLine();
            assertEquals(expectedStatusCode, statusLine.getStatusCode());
        }
    }

    /**
     * Returns response body for the given URL request as a String. It also checks if the returned HTTP status code is the
     * expected one. If the server returns {@link HttpServletResponse#SC_UNAUTHORIZED} and username is provided, then a new
     * request is created with the provided credentials (basic authentication).
     *
     * @param url URL to which the request should be made
     * @param user Username (may be null)
     * @param pass Password (may be null)
     * @param expectedStatusCode expected status code returned from the requested server
     * @return HTTP response body
     * @throws IOException
     * @throws URISyntaxException
     */
    public static String makeCallWithBasicAuthn(URL url, String user, String pass, int expectedStatusCode) throws IOException, URISyntaxException {
        LOGGER.info("Requesting URL " + url);
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()){
            final HttpGet httpGet = new HttpGet(url.toURI());
            HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            if (401 != statusCode || StringUtils.isEmpty(user)) {
                assertEquals("Unexpected HTTP response status code.", expectedStatusCode, statusCode);
                return EntityUtils.toString(response.getEntity());
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("HTTP response was SC_UNAUTHORIZED, let's authenticate the user " + user);
            }
            HttpEntity entity = response.getEntity();
            if (entity != null)
                EntityUtils.consume(entity);

            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(new AuthScope(url.getHost(), url.getPort()), new UsernamePasswordCredentials(user, pass));

            final HttpClientContext context = HttpClientContext.create();
            context.setCredentialsProvider(credsProvider);
            response = httpClient.execute(httpGet, context);
            statusCode = response.getStatusLine().getStatusCode();
            assertEquals("Unexpected status code returned after the authentication.", expectedStatusCode, statusCode);
            return EntityUtils.toString(response.getEntity());
        }
    }

    /**
     * Exports given archive to the given file path.
     *
     * @param archive
     * @param filePath
     */
    public static void saveArchive(Archive<?> archive, String filePath) {
        archive.as(ZipExporter.class).exportTo(new File(filePath), true);
    }

    /**
     * Exports given archive to the given folder.
     *
     * @param archive    archive to export (not-<code>null</code>)
     * @param folderPath
     */
    public static void saveArchiveToFolder(Archive<?> archive, String folderPath) {
        final File exportFile = new File(folderPath, archive.getName());
        LOGGER.info("Exporting archive: " + exportFile.getAbsolutePath());
        archive.as(ZipExporter.class).exportTo(exportFile, true);
    }



    /**
     * Requests given URL and checks if the returned HTTP status code is the
     * expected one. Returns HTTP response body
     *
     * @param url                url to which the request should be made
     * @param httpClient         httpClient to test multiple access
     * @param expectedStatusCode expected status code returned from the requested server
     * @return HTTP response body
     * @throws java.io.IOException
     * @throws java.net.URISyntaxException
     */
    public static String makeCallWithHttpClient(URL url, HttpClient httpClient, int expectedStatusCode)
            throws IOException, URISyntaxException {

        String httpResponseBody = null;
        HttpGet httpGet = new HttpGet(url.toURI());
        HttpResponse response = httpClient.execute(httpGet);
        int statusCode = response.getStatusLine().getStatusCode();
        LOGGER.info("Request to: " + url + " responds: " + statusCode);

        assertEquals("Unexpected status code", expectedStatusCode, statusCode);

        HttpEntity entity = response.getEntity();
        if (entity != null) {
            httpResponseBody = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(entity);
        }
        return httpResponseBody;
    }


    /**
     * Sets or removes (in case value==null) a system property. It's only a helper method, which avoids
     * {@link NullPointerException} thrown from {@link System#setProperty(String, String)} method, when the value is
     * <code>null</code>.
     *
     * @param key   property name
     * @param value property value
     * @return the previous string value of the system property
     */
    public static String setSystemProperty(final String key, final String value) {
        return value == null ? System.clearProperty(key) : System.setProperty(key, value);
    }

    /**
     * Returns canonical hostname form of the given address.
     *
     * @param address hosname or IP address
     * @return
     */
    public static final String getCannonicalHost(final String address) {
        String host = stripSquareBrackets(address);
        try {
            host = InetAddress.getByName(host).getCanonicalHostName();
        } catch (UnknownHostException e) {
            LOGGER.warn("Unable to get canonical host name", e);
        }
        return host.toLowerCase(Locale.ENGLISH);
    }

    /**
     * Returns given URI with the replaced hostname. If the URI or host is null, then the original URI is returned.
     *
     * @param uri
     * @param host
     * @return
     * @throws java.net.URISyntaxException
     */
    public static final URI replaceHost(final URI uri, final String host) throws URISyntaxException {
        final String origHost = uri == null ? null : uri.getHost();
        final String newHost = NetworkUtils.formatPossibleIpv6Address(host);
        if (origHost == null || newHost == null || newHost.equals(origHost)) {
            return uri;
        }
        return new URI(uri.toString().replace(origHost, newHost));
    }

    /**
     * Generates content of jboss-ejb3.xml file as a ShrinkWrap asset with the given security domain name.
     *
     * @param securityDomain security domain name
     * @return Asset instance
     */
    public static Asset getJBossEjb3XmlAsset(final String securityDomain) {
        final StringBuilder sb = new StringBuilder();
        sb.append("<jboss:ejb-jar xmlns:jboss='http://www.jboss.com/xml/ns/javaee'");
        sb.append("\n\txmlns='http://java.sun.com/xml/ns/javaee'");
        sb.append("\n\txmlns:s='urn:security'");
        sb.append("\n\tversion='3.1'");
        sb.append("\n\timpl-version='2.0'>");
        sb.append("\n\t<assembly-descriptor><s:security>");
        sb.append("\n\t\t<ejb-name>*</ejb-name>");
        sb.append("\n\t\t<s:security-domain>").append(securityDomain).append("</s:security-domain>");
        sb.append("\n\t</s:security></assembly-descriptor>");
        sb.append("\n</jboss:ejb-jar>");
        return new StringAsset(sb.toString());
    }

    /**
     * Generates content of jboss-web.xml file as a ShrinkWrap asset with the given security domain name and given valve class.
     *
     * @param securityDomain  security domain name (not-<code>null</code>)
     * @param valveClassNames valve class (e.g. an Authenticator) which should be added to jboss-web file (may be
     *                        <code>null</code>)
     * @return Asset instance
     */
    public static Asset getJBossWebXmlAsset(final String securityDomain, final String... valveClassNames) {
        final StringBuilder sb = new StringBuilder();
        sb.append("<jboss-web>");
        sb.append("\n\t<security-domain>").append(securityDomain).append("</security-domain>");
        if (valveClassNames != null) {
            for (String valveClassName : valveClassNames) {
                if (StringUtils.isNotEmpty(valveClassName)) {
                    sb.append("\n\t<valve><class-name>").append(valveClassName).append("</class-name></valve>");
                }
            }
        }
        sb.append("\n</jboss-web>");
        return new StringAsset(sb.toString());
    }

    /**
     * Generates content of the jboss-deployment-structure.xml deployment descriptor as a ShrinkWrap asset. It fills the given
     * dependencies (module names) into it.
     *
     * @param dependencies AS module names
     * @return
     */
    public static Asset getJBossDeploymentStructure(String... dependencies) {
        final StringBuilder sb = new StringBuilder();
        sb.append("<jboss-deployment-structure><deployment><dependencies>");
        if (dependencies != null) {
            for (String moduleName : dependencies) {
                sb.append("\n\t<module name='").append(moduleName).append("'/>");
            }
        }
        sb.append("\n</dependencies></deployment></jboss-deployment-structure>");
        return new StringAsset(sb.toString());
    }

    /**
     * Creates content of users.properties and/or roles.properties files for given array of role names.
     * <p/>
     * For instance if you provide 2 roles - "role1", "role2" then the result will be:
     * <p/>
     * <pre>
     * role1=role1
     * role2=role2
     * </pre>
     * <p/>
     * If you use it as users.properties and roles.properties, then <code>roleName == userName == password</code>
     *
     * @param roles role names (used also as user names and passwords)
     * @return not-<code>null</code> content of users.properties and/or roles.properties
     */
    public static String createUsersFromRoles(String... roles) {
        final StringBuilder sb = new StringBuilder();
        if (roles != null) {
            for (String role : roles) {
                sb.append(role).append("=").append(role).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Strips square brackets - '[' and ']' from the given string. It can be used for instance to remove the square brackets
     * around IPv6 address in a URL.
     *
     * @param str string to strip
     * @return str without square brackets in it
     */
    public static String stripSquareBrackets(final String str) {
        return StringUtils.strip(str, "[]");
    }


    /**
     * Copies server and clients keystores and truststores from this package to
     * the given folder. Server truststore has accepted certificate from client keystore and vice-versa
     *
     * @param workingFolder folder to which key material should be copied
     * @throws java.io.IOException      copying of keystores fails
     * @throws IllegalArgumentException workingFolder is null or it's not a directory
     */
    public static void createKeyMaterial(final File workingFolder) throws IOException, IllegalArgumentException {
        if (workingFolder == null || !workingFolder.isDirectory()) {
            throw new IllegalArgumentException("Provide an existing folder as the method parameter.");
        }
        createTestResource(new File(workingFolder, SecurityTestConstants.SERVER_KEYSTORE));
        createTestResource(new File(workingFolder, SecurityTestConstants.SERVER_TRUSTSTORE));
        createTestResource(new File(workingFolder, SecurityTestConstants.SERVER_CRT));
        createTestResource(new File(workingFolder, SecurityTestConstants.CLIENT_KEYSTORE));
        createTestResource(new File(workingFolder, SecurityTestConstants.CLIENT_TRUSTSTORE));
        createTestResource(new File(workingFolder, SecurityTestConstants.CLIENT_CRT));
        createTestResource(new File(workingFolder, SecurityTestConstants.UNTRUSTED_KEYSTORE));
        createTestResource(new File(workingFolder, SecurityTestConstants.UNTRUSTED_CRT));
        LOGGER.info("Key material created in " + workingFolder.getAbsolutePath());
    }

    /**
     * Copies a resource file from current package to location denoted by given
     * {@link java.io.File} instance.
     *
     * @param file
     * @throws java.io.IOException
     */
    private static void createTestResource(File file) throws IOException {
        FileOutputStream fos = null;
        LOGGER.info("Creating test file " + file.getAbsolutePath());
        try {
            fos = new FileOutputStream(file);
            IOUtils.copy(CoreUtils.class.getResourceAsStream(file.getName()), fos);
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }

    public static String propertiesReplacer(String originalFile, File keystoreFile, File trustStoreFile, String keystorePassword) {
        return propertiesReplacer(originalFile, keystoreFile.getAbsolutePath(), trustStoreFile.getAbsolutePath(), keystorePassword, null);
    }

    public static String propertiesReplacer(String originalFile, File keystoreFile, File trustStoreFile, String keystorePassword,
                                            String vaultConfig) {
        return propertiesReplacer(originalFile, keystoreFile.getAbsolutePath(), trustStoreFile.getAbsolutePath(), keystorePassword,
                vaultConfig);
    }

    /**
     * Replace keystore paths and passwords variables in original configuration file with given values
     * and set ${hostname} variable from system property: node0
     *
     * @return String content
     */
    public static String propertiesReplacer(String originalFile, String keystoreFile, String trustStoreFile, String keystorePassword,
                                            String vaultConfig) {
        String hostname = System.getProperty("node0");

        // expand possible IPv6 address
        try {
            hostname = NetworkUtils.formatPossibleIpv6Address(InetAddress.getByName(hostname).getHostAddress());
        } catch (UnknownHostException ex) {
            String message = "Cannot resolve host address: " + hostname + " , error : " + ex.getMessage();
            LOGGER.error(message);
            throw new RuntimeException(ex);
        }

        final Map<String, String> map = new HashMap<String, String>();
        String content = "";
        if (vaultConfig == null) {
            map.put("vaultConfig", "");
        } else {
            map.put("vaultConfig", vaultConfig);
        }
        map.put("hostname", hostname);
        map.put("keystore", keystoreFile);
        map.put("truststore", trustStoreFile);
        map.put("password", keystorePassword);

        try {
            content = StrSubstitutor.replace(
                    IOUtils.toString(CoreUtils.class.getResourceAsStream(originalFile), "UTF-8"), map);
        } catch (IOException ex) {
            String message = "Cannot find or modify configuration file " + originalFile + " , error : " + ex.getMessage();
            LOGGER.error(message);
            throw new RuntimeException(ex);
        }

        return content;
    }

    /**
     * Makes HTTP call without authentication. Returns response body as a String.
     *
     * @param uri                requested URL
     * @param expectedStatusCode expected status code - it's checked after the request is executed
     * @throws Exception
     */
    public static String makeCall(URI uri, int expectedStatusCode) throws Exception {

        try (final CloseableHttpClient httpclient = HttpClients.createDefault()){
            final HttpGet httpget = new HttpGet(uri);
            final HttpResponse response = httpclient.execute(httpget);
            int statusCode = response.getStatusLine().getStatusCode();
            assertEquals("Unexpected status code returned after the authentication.", expectedStatusCode, statusCode);
            return EntityUtils.toString(response.getEntity());
        }
    }

    /**
     * Returns param/value pair in form "urlEncodedName=urlEncodedValue". It can be used for instance in HTTP get queries.
     *
     * @param paramName  parameter name
     * @param paramValue parameter value
     * @return "[urlEncodedName]=[urlEncodedValue]" string
     */
    public static String encodeQueryParam(final String paramName, final String paramValue) {
        String response = null;
        try {
            response = StringUtils.isEmpty(paramValue) ? null : (URLEncoder.encode(paramName, UTF_8) + "=" + URLEncoder.encode(
                    StringUtils.defaultString(paramValue, StringUtils.EMPTY), UTF_8));
        } catch (UnsupportedEncodingException e) {
            // should never happen - everybody likes the "UTF-8" :)
        }
        return response;
    }
}
