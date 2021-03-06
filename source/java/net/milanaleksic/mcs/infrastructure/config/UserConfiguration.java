package net.milanaleksic.mcs.infrastructure.config;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * User: Milan Aleksic
 * Date: 8/1/11
 * Time: 11:50 PM
 */
@XmlRootElement
public class UserConfiguration {

    public static class ProxyConfiguration {
        private String server;
        private int port;
        private String username;
        private String password;
        private boolean ntlm;

        public boolean isNtlm() {
            return ntlm;
        }

        public void setNtlm(boolean ntlm) {
            this.ntlm = ntlm;
        }

        public String getServer() {
            return server;
        }

        public void setServer(String server) {
            this.server = server;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }


        @Override
        @SuppressWarnings({"HardCodedStringLiteral"})
        public String toString() {
            return "ProxyConfiguration{" +
                    "server='" + server + '\'' +
                    ", port=" + port +
                    ", username='" + username + '\'' +
                    ", ntlm=" + ntlm +
                    '}';
        }
    }

    public static class TenrecConfiguration {

        private boolean checkForNewVersionsOnStartup = false;

        private String username;

        private String password;

        public boolean isCheckForNewVersionsOnStartup() {
            return checkForNewVersionsOnStartup;
        }

        public void setCheckForNewVersionsOnStartup(boolean checkForNewVersionsOnStartup) {
            this.checkForNewVersionsOnStartup = checkForNewVersionsOnStartup;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        @Override
        @SuppressWarnings({"HardCodedStringLiteral"})
        public String toString() {
            return "TenrecConfiguration{" +
                    "checkForNewVersionsOnStartup=" + checkForNewVersionsOnStartup +
                    ", username='" + username + '\'' +
                    '}';
        }
    }

    private ProxyConfiguration proxyConfiguration;

    private TenrecConfiguration tenrecConfiguration;

    private int elementsPerPage;

    private String localeLanguage;

    public UserConfiguration() {
        elementsPerPage = 28;
        proxyConfiguration = new ProxyConfiguration();
        tenrecConfiguration = new TenrecConfiguration();
        localeLanguage = "en"; //NON-NLS
    }

    public TenrecConfiguration getTenrecConfiguration() {
        return tenrecConfiguration;
    }

    public void setTenrecConfiguration(TenrecConfiguration tenrecConfiguration) {
        this.tenrecConfiguration = tenrecConfiguration;
    }

    public void setLocaleLanguage(String localeLanguage) {
        this.localeLanguage = localeLanguage;
    }

    public String getLocaleLanguage() {
        return localeLanguage;
    }

    public int getElementsPerPage() {
        return elementsPerPage;
    }

    public void setElementsPerPage(int elementsPerPage) {
        this.elementsPerPage = elementsPerPage;
    }

    public ProxyConfiguration getProxyConfiguration() {
        return proxyConfiguration;
    }

    public void setProxyConfiguration(ProxyConfiguration proxyConfiguration) {
        this.proxyConfiguration = proxyConfiguration;
    }

    @Override
    public String toString() {
        return "UserConfiguration{" +
                "proxyConfiguration=" + proxyConfiguration +
                ", tenrecConfiguration=" + tenrecConfiguration +
                ", elementsPerPage=" + elementsPerPage +
                ", localeLanguage='" + localeLanguage + '\'' +
                '}';
    }
}
