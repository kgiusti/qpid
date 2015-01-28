/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.qpid.transport.network.security.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

import org.apache.qpid.transport.TransportException;
import org.apache.qpid.transport.util.Logger;

public class SSLUtil
{
    private static final Logger log = Logger.get(SSLUtil.class);
    private static final Integer DNS_NAME_TYPE = 2;
    public static final String SSLV3_PROTOCOL = "SSLv3";

    private SSLUtil()
    {
    }

    public static void verifyHostname(SSLEngine engine,String hostnameExpected)
    {
        try
        {
            Certificate cert = engine.getSession().getPeerCertificates()[0];
            verifyHostname(hostnameExpected, (X509Certificate) cert);
        }
        catch(SSLPeerUnverifiedException e)
        {
            log.warn("Exception received while trying to verify hostname",e);
            // For some reason the SSL engine sets the handshake status to FINISH twice
            // in succession. The first time the peer certificate
            // info is not available. The second time it works !
            // Therefore have no choice but to ignore the exception here.
        }
    }

    public static void verifyHostname(final String hostnameExpected, final X509Certificate cert)
    {
        Principal p = cert.getSubjectDN();

        SortedSet<String> names = new TreeSet<>();
        String dn = p.getName();
        try
        {
            LdapName ldapName = new LdapName(dn);
            for (Rdn part : ldapName.getRdns())
            {
                if (part.getType().equalsIgnoreCase("CN"))
                {
                    names.add(part.getValue().toString());
                    break;
                }
            }

            if(cert.getSubjectAlternativeNames() != null)
            {
                for (List<?> entry : cert.getSubjectAlternativeNames())
                {
                    if (DNS_NAME_TYPE.equals(entry.get(0)))
                    {
                        names.add((String) entry.get(1));
                    }
                }
            }

            if (names.isEmpty())
            {
                throw new TransportException("SSL hostname verification failed. Certificate for did not contain CN or DNS subjectAlt");
            }

            boolean match = false;

            final String hostName = hostnameExpected.trim().toLowerCase();
            for (String cn : names)
            {

                boolean doWildcard = cn.startsWith("*.") &&
                                     cn.lastIndexOf('.') >= 3 &&
                                     !cn.matches("\\*\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");


                match = doWildcard
                        ? hostName.endsWith(cn.substring(1)) && hostName.indexOf(".") == (1 + hostName.length() - cn.length())
                        : hostName.equals(cn);

                if (match)
                {
                    break;
                }

            }
            if (!match)
            {
                throw new TransportException("SSL hostname verification failed." +
                                             " Expected : " + hostnameExpected +
                                             " Found in cert : " + names);
            }

        }
        catch (InvalidNameException e)
        {
            throw new TransportException("SSL hostname verification failed. Could not parse name " + dn, e);
        }
        catch (CertificateParsingException e)
        {
            throw new TransportException("SSL hostname verification failed. Could not parse certificate:  " + e.getMessage(), e);
        }
    }

    public static String getIdFromSubjectDN(String dn)
    {
        String cnStr = null;
        String dcStr = null;
        if(dn == null)
        {
            return "";
        }
        else
        {
            try
            {
                LdapName ln = new LdapName(dn);
                for(Rdn rdn : ln.getRdns())
                {
                    if("CN".equalsIgnoreCase(rdn.getType()))
                    {
                        cnStr = rdn.getValue().toString();
                    }
                    else if("DC".equalsIgnoreCase(rdn.getType()))
                    {
                        if(dcStr == null)
                        {
                            dcStr = rdn.getValue().toString();
                        }
                        else
                        {
                            dcStr = rdn.getValue().toString() + '.' + dcStr;
                        }
                    }
                }
                return cnStr == null || cnStr.length()==0 ? "" : dcStr == null ? cnStr : cnStr + '@' + dcStr;
            }
            catch (InvalidNameException e)
            {
                log.warn("Invalid name: '"+dn+"'. ");
                return "";
            }
        }
    }


    public static String retrieveIdentity(SSLEngine engine)
    {
        String id = "";
        Certificate cert = engine.getSession().getLocalCertificates()[0];
        Principal p = ((X509Certificate)cert).getSubjectDN();
        String dn = p.getName();
        try
        {
            id = SSLUtil.getIdFromSubjectDN(dn);
        }
        catch (Exception e)
        {
            log.info("Exception received while trying to retrieve client identity from SSL cert", e);
        }
        log.debug("Extracted Identity from client certificate : " + id);
        return id;
    }

    public static KeyStore getInitializedKeyStore(String storePath, String storePassword, String keyStoreType) throws GeneralSecurityException, IOException
    {
        KeyStore ks = KeyStore.getInstance(keyStoreType);
        InputStream in = null;
        try
        {
            File f = new File(storePath);
            if (f.exists())
            {
                in = new FileInputStream(f);
            }
            else
            {
                in = Thread.currentThread().getContextClassLoader().getResourceAsStream(storePath);
            }
            if (in == null && !"PKCS11".equalsIgnoreCase(keyStoreType)) // PKCS11 will not require an explicit path
            {
                throw new IOException("Unable to load keystore resource: " + storePath);
            }

            char[] storeCharPassword = storePassword == null ? null : storePassword.toCharArray();

            ks.load(in, storeCharPassword);
        }
        finally
        {
            if (in != null)
            {
                //noinspection EmptyCatchBlock
                try
                {
                    in.close();
                }
                catch (IOException ignored)
                {
                }
            }
        }
        return ks;
    }

    public static KeyStore getInitializedKeyStore(URL storePath, String storePassword, String keyStoreType) throws GeneralSecurityException, IOException
    {
        KeyStore ks = KeyStore.getInstance(keyStoreType);
        try(InputStream in = storePath.openStream())
        {
            if (in == null && !"PKCS11".equalsIgnoreCase(keyStoreType)) // PKCS11 will not require an explicit path
            {
                throw new IOException("Unable to load keystore resource: " + storePath);
            }

            char[] storeCharPassword = storePassword == null ? null : storePassword.toCharArray();

            ks.load(in, storeCharPassword);
        }
        return ks;
    }

    private static interface SSLEntity
    {
        String[] getEnabledCipherSuites();

        void setEnabledCipherSuites(String[] strings);

        String[] getEnabledProtocols();

        void setEnabledProtocols(String[] protocols);

        String[] getSupportedCipherSuites();

        String[] getSupportedProtocols();
    }

    private static SSLEntity asSSLEntity(final Object object, final Class<?> clazz)
    {
        return (SSLEntity) Proxy.newProxyInstance(SSLEntity.class.getClassLoader(), new Class[] { SSLEntity.class }, new InvocationHandler()
        {
            @Override
            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
            {
                Method delegateMethod = clazz.getMethod(method.getName(), method.getParameterTypes());
                return delegateMethod.invoke(object, args);
            }
        })   ;
    }

    private static void removeSSLv3Support(final SSLEntity engine)
    {
        List<String> enabledProtocols = Arrays.asList(engine.getEnabledProtocols());
        if(enabledProtocols.contains(SSLV3_PROTOCOL))
        {
            List<String> allowedProtocols = new ArrayList<>(enabledProtocols);
            allowedProtocols.remove(SSLV3_PROTOCOL);
            engine.setEnabledProtocols(allowedProtocols.toArray(new String[allowedProtocols.size()]));
        }
    }

    public static void removeSSLv3Support(final SSLEngine engine)
    {
        removeSSLv3Support(asSSLEntity(engine, SSLEngine.class));
    }

    public static void removeSSLv3Support(final SSLSocket socket)
    {
        removeSSLv3Support(asSSLEntity(socket, SSLSocket.class));
    }

    public static void removeSSLv3Support(final SSLServerSocket socket)
    {
        removeSSLv3Support(asSSLEntity(socket, SSLServerSocket.class));
    }

    private static void updateEnabledCipherSuites(final SSLEntity entity,
                                                  final Collection<String> enabledCipherSuites,
                                                  final Collection<String> disabledCipherSuites)
    {
        if(enabledCipherSuites != null && !enabledCipherSuites.isEmpty())
        {
            final Set<String> supportedSuites =
                    new HashSet<>(Arrays.asList(entity.getSupportedCipherSuites()));
            supportedSuites.retainAll(enabledCipherSuites);
            entity.setEnabledCipherSuites(supportedSuites.toArray(new String[supportedSuites.size()]));
        }

        if(disabledCipherSuites != null && !disabledCipherSuites.isEmpty())
        {
            final Set<String> enabledSuites = new HashSet<>(Arrays.asList(entity.getEnabledCipherSuites()));
            enabledSuites.removeAll(disabledCipherSuites);
            entity.setEnabledCipherSuites(enabledSuites.toArray(new String[enabledSuites.size()]));
        }

    }


    public static void updateEnabledCipherSuites(final SSLEngine engine,
                                                 final Collection<String> enabledCipherSuites,
                                                 final Collection<String> disabledCipherSuites)
    {
        updateEnabledCipherSuites(asSSLEntity(engine, SSLEngine.class), enabledCipherSuites, disabledCipherSuites);
    }

    public static void updateEnabledCipherSuites(final SSLServerSocket socket,
                                                 final Collection<String> enabledCipherSuites,
                                                 final Collection<String> disabledCipherSuites)
    {
        updateEnabledCipherSuites(asSSLEntity(socket, SSLServerSocket.class), enabledCipherSuites, disabledCipherSuites);
    }

    public static void updateEnabledCipherSuites(final SSLSocket socket,
                                                 final Collection<String> enabledCipherSuites,
                                                 final Collection<String> disabledCipherSuites)
    {
        updateEnabledCipherSuites(asSSLEntity(socket, SSLSocket.class), enabledCipherSuites, disabledCipherSuites);
    }
}
