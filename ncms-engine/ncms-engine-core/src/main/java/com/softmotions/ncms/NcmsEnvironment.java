package com.softmotions.ncms;

import com.softmotions.weboot.WBConfiguration;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Ncms configuration.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class NcmsEnvironment extends WBConfiguration {

    private static final String CORE_PROPS_LOCATION = "/com/softmotions/ncms/core/Core.properties";

    private final Properties coreProps;

    public String getNcmsVersion() {
        return coreProps.getProperty("project.version");
    }

    public NcmsEnvironment() {
        coreProps = new Properties();
        try (InputStream is = getClass().getResourceAsStream(CORE_PROPS_LOCATION)) {
            if (is == null) {
                throw new RuntimeException("Jar resource not found: " + CORE_PROPS_LOCATION);
            }
            coreProps.load(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void load(String location, ServletContext sctx) {
        super.load(location, sctx);
        normalizePrefix("site-files-root");
        normalizePrefix("ncms-prefix");
    }

    private void normalizePrefix(String property) {
        String val = xcfg().getString(property);
        if (!StringUtils.isBlank(val)) {
            val = val.trim();
            if (!val.startsWith("/")) {
                val = '/' + val;
            }
            if (val.endsWith("/")) {
                val = val.substring(0, val.length() - 1);
            }
            xcfg().setProperty(property, val);
        }
    }

    public String getApplicationName() {
        return xcfg().getString("app-name", "Ncms");
    }

    public String getHelpSite() {
        return xcfg().getString("help-site");
    }

    public String getLogoutRedirect() {
        return xcfg().getString("logout-redirect");
    }

    public String getNcmsPrefix() {
        String p = xcfg().getString("ncms-prefix", "/ncms");
        return p.charAt(0) != '/' ? '/' + p : p;
    }

    public String getEnvironmentType() {
        String etype = xcfg.getString("environment");
        if (etype == null) {
            throw new RuntimeException("Missing required '<environment>' " +
                                       "property in application config");
        }
        return etype;
    }

    public String getDBEnvironmentType() {
        String etype = xcfg.getString("db-environment");
        if (etype == null) {
            throw new RuntimeException("Missing required 'ncms.db.environment' " +
                                       "property in 'application.conf'");
        }
        return etype;
    }

    public String getNcmsRoot() {
        return getServletContext().getContextPath() + getNcmsPrefix();
    }

    public String getAbsoluteLink(HttpServletRequest req, String link) {
        XMLConfiguration x = xcfg();
        boolean preferRequestUrl = x.getBoolean("site-root[@preferRequestUrl]", false);
        if (preferRequestUrl) {
            link = req.getScheme() + "://" +
                   req.getServerName() +
                   ":" + req.getServerPort() +
                   link;
        } else {
            link = x.getString("site-root") + link;
        }
        return link;
    }
}