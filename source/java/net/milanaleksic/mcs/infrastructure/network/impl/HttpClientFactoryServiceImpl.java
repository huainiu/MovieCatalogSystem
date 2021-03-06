package net.milanaleksic.mcs.infrastructure.network.impl;

import com.google.common.base.Strings;
import net.milanaleksic.mcs.infrastructure.config.UserConfiguration;
import net.milanaleksic.mcs.infrastructure.LifeCycleListener;
import net.milanaleksic.mcs.infrastructure.config.ApplicationConfiguration;
import net.milanaleksic.mcs.infrastructure.network.HttpClientFactoryService;
import net.milanaleksic.mcs.infrastructure.network.PersistentHttpContext;
import net.milanaleksic.mcs.infrastructure.util.ntlm.NTLMSchemeFactory;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.*;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.*;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.*;
import org.apache.http.protocol.HttpContext;

/**
 * User: Milan Aleksic
 * Date: 3/1/12
 * Time: 12:39 PM
 */
public class HttpClientFactoryServiceImpl implements HttpClientFactoryService, LifeCycleListener {

    public static final String AUTH_SCHEME_NTLM = "NTLM"; //NON-NLS
    public static final String SCHEME_HTTP = "http"; //NON-NLS

    public static final int INTERVAL_TIMEOUT = 2000;
    public static final int INTERVAL_KEEP_ALIVE = 60000;

    private UserConfiguration userConfiguration;

    @Override
    public PersistentHttpContext createPersistentHttpContext() {
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme(SCHEME_HTTP, PlainSocketFactory.getSocketFactory(), 80));
        UserConfiguration.ProxyConfiguration proxyConfiguration = userConfiguration.getProxyConfiguration();
        DefaultHttpClient httpClient = new DefaultHttpClient(new ThreadSafeClientConnManager(new BasicHttpParams(), registry), new BasicHttpParams());
        if (!Strings.isNullOrEmpty(proxyConfiguration.getServer())) {
            String server = proxyConfiguration.getServer();
            int port = proxyConfiguration.getPort() == 0 ? 80 : proxyConfiguration.getPort();
            final HttpHost hcProxyHost = new HttpHost(server, port, SCHEME_HTTP);
            if (!Strings.isNullOrEmpty(proxyConfiguration.getUsername()) && !Strings.isNullOrEmpty(proxyConfiguration.getPassword())) {
                Credentials credentials;
                httpClient.getAuthSchemes().register(AUTH_SCHEME_NTLM, new NTLMSchemeFactory());
                if (proxyConfiguration.isNtlm()) {
                    credentials = new NTCredentials(proxyConfiguration.getUsername(), proxyConfiguration.getPassword(), "", "");
                } else
                    credentials = new UsernamePasswordCredentials(proxyConfiguration.getUsername(), proxyConfiguration.getPassword());
                httpClient.getCredentialsProvider().setCredentials(new AuthScope(server, port), credentials);
            }
            httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, hcProxyHost);
        }
        httpClient.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy() {
            @Override
            public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
                long keepAlive = super.getKeepAliveDuration(response, context);
                if (keepAlive == -1) {
                    keepAlive = INTERVAL_KEEP_ALIVE;
                }
                return keepAlive;
            }
        });
        HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), INTERVAL_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpClient.getParams(), INTERVAL_TIMEOUT);
        return new PersistentHttpContext(httpClient);
    }

    @Override
    public void applicationStarted(ApplicationConfiguration configuration, UserConfiguration userConfiguration) {
        this.userConfiguration = userConfiguration;
    }

    @Override
    public void applicationShutdown(ApplicationConfiguration applicationConfiguration, UserConfiguration userConfiguration) {
    }
}
