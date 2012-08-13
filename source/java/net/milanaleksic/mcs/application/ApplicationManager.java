package net.milanaleksic.mcs.application;

import com.google.common.collect.Lists;
import net.milanaleksic.mcs.application.config.*;
import net.milanaleksic.mcs.application.gui.MainForm;
import net.milanaleksic.mcs.application.gui.helper.SplashScreenManager;
import net.milanaleksic.mcs.infrastructure.LifeCycleListener;
import net.milanaleksic.mcs.infrastructure.config.*;
import net.milanaleksic.mcs.infrastructure.util.VersionInformation;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.springframework.beans.BeansException;
import org.springframework.context.*;

import javax.inject.Inject;
import java.io.File;
import java.util.List;

public class ApplicationManager implements ApplicationContextAware {

    private static final Logger log = Logger.getLogger(ApplicationManager.class);

    public static final String LOG4J_XML = "log4j.xml"; //NON-NLS

    @Inject
    private SplashScreenManager splashScreenManager;

    @Inject
    private MainForm mainForm;

    @Inject
    private ApplicationConfigurationManager applicationConfigurationManager;

    @Inject
    private UserConfigurationManager userConfigurationManager;

    private ApplicationConfiguration applicationConfiguration;

    private UserConfiguration userConfiguration;

    private List<LifeCycleListener> lifeCycleListeners = Lists.newLinkedList();

    public ApplicationManager() {
        this(false);
    }

    public ApplicationManager(boolean explicitReadConfigurationsNow) {
        if (new File(LOG4J_XML).exists())
            DOMConfigurator.configure(LOG4J_XML);
        if (explicitReadConfigurationsNow)
            readConfigurationsWithoutDI();
    }

    public void setLifeCycleListeners(List<LifeCycleListener> lifeCycleListeners) {
        this.lifeCycleListeners = lifeCycleListeners;
    }

    public UserConfiguration getUserConfiguration() {
        return userConfiguration;
    }

    public ApplicationConfiguration getApplicationConfiguration() {
        return applicationConfiguration;
    }

    private void fireApplicationStarted() {
        final int size = lifeCycleListeners.size();
        for (int i = 0; i < size; i++) {
            if (log.isTraceEnabled())
                log.debug("Executing startup callback on listener " + (i + 1) + " of " + size); //NON-NLS
            safeCallStartedOnListener(lifeCycleListeners.get(i));
        }
    }

    private void fireApplicationShutdown() {
        final int size = lifeCycleListeners.size();
        for (int i = size - 1; i >= 0; i--) {
            if (log.isTraceEnabled())
                log.trace("Executing shutdown callback on listener " + (i + 1) + " of " + size); //NON-NLS
            safeCallShutdownOnListener(lifeCycleListeners.get(i));
        }
    }

    private void safeCallStartedOnListener(LifeCycleListener listener) {
        try {
            listener.applicationStarted(applicationConfiguration, userConfiguration);
        } catch (Exception e) {
            log.error("Application exception while calling startup callbacks", e); //NON-NLS
        }
    }

    private void safeCallShutdownOnListener(LifeCycleListener listener) {
        try {
            listener.applicationShutdown(applicationConfiguration, userConfiguration);
        } catch (Exception e) {
            log.error("Application exception while calling shutdown callbacks", e); //NON-NLS
        }
    }

    public void entryPoint() {
        setUncaughtExceptionHandler();
        fireApplicationStarted();
        try {
            mainGuiLoop();
        } catch (RuntimeException e) {
            String message = "Runtime exception caught in main GUI loop: " + e.getMessage(); //NON-NLS
            log.error(message, e);
            showTerribleErrorInGui(message);
        } finally {
            fireApplicationShutdown();
        }
    }

    public void setUncaughtExceptionHandler() {
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                final String message = "Runtime exception caught in non-primary thread: " + e.getMessage(); //NON-NLS
                log.error(message, e);
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        showTerribleErrorInGui(message);
                    }
                });
            }
        };
        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
    }

    private void showTerribleErrorInGui(String message) {
        Shell activeShell = Display.getDefault().getActiveShell();
        if (activeShell == null)
            activeShell = new Shell();
        MessageBox messageBox = new MessageBox(activeShell, SWT.APPLICATION_MODAL | SWT.ERROR);
        messageBox.setMessage(message);
        messageBox.setText("Terrible, terrible error"); //NON-NLS
        if (!activeShell.isDisposed())
            messageBox.open();
    }

    private void mainGuiLoop() {
        Display.setAppName("Movie Catalog System - v" + VersionInformation.getVersion()); //NON-NLS
        final Display display = Display.getDefault();
        try {
            mainForm.open();

            splashScreenManager.closeSplashScreen();

            while (!mainForm.isDisposed()) {
                if (!display.readAndDispatch())
                    display.sleep();
            }
        } catch (Throwable t) {
            log.error("Unhandled GUI thread exception - " + t.getMessage(), t); //NON-NLS
        } finally {
            if (!display.isDisposed())
                display.syncExec(new Runnable() {
                    @Override
                    public void run() {
                        display.dispose();
                    }
                });
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        readConfigurations();
    }

    private void readConfigurations() {
        applicationConfiguration = applicationConfigurationManager.loadApplicationConfiguration();
        userConfiguration = userConfigurationManager.loadUserConfiguration();
    }

    private void readConfigurationsWithoutDI() {
        applicationConfiguration = new ApplicationConfigurationManager().loadApplicationConfiguration();
        userConfiguration = new UserConfigurationManager().loadUserConfiguration();
    }
}