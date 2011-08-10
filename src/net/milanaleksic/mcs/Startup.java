package net.milanaleksic.mcs;

import net.milanaleksic.mcs.config.ApplicationConfiguration;
import net.milanaleksic.mcs.config.ApplicationConfigurationManager;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.*;
import java.nio.channels.FileLock;

public class Startup {

    private static final Logger log = Logger.getLogger(Startup.class);

    private static String[] programArgs;

    public static String[] getProgramArgs() {
        return programArgs;
    }

    public static void main(String[] args) {
        programArgs = args;
        loadLog4JOverride();
        FileLock lock = null;
        try {
            lock = getSingletonApplicationFileLock();

            ApplicationConfiguration applicationConfiguration = ApplicationConfigurationManager.loadApplicationConfiguration();
            ApplicationManager.setApplicationConfiguration(applicationConfiguration);

            ApplicationContext applicationContext = bootSpringContext();
            ApplicationManager applicationManager = ((ApplicationManager) applicationContext.getBean("applicationManager"));
            applicationManager.entryPoint();

        } finally {
            if (lock != null)
                closeSingletonApplicationLock(lock);
        }
    }

    private static ApplicationContext bootSpringContext() {
        long begin = System.currentTimeMillis();
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring-beans.xml");
        context.registerShutdownHook();
        log.debug(String.format("Spring booted in %dms", System.currentTimeMillis()-begin));
        return context;
    }

    private static void loadLog4JOverride() {
        if (new File("log4j.xml").exists())
            DOMConfigurator.configure("log4j.xml");
    }

    private static FileLock getSingletonApplicationFileLock() {
        final String lockFileName = ".launcher";
        File locker = new File(lockFileName);
        FileLock lock;
        try {
            if (!locker.exists())
                throw new IllegalStateException("Nisam mogao da pristupim lock fajlu!");
            lock = new RandomAccessFile(lockFileName, "rw").getChannel().tryLock();
            if (lock == null) {
                throw new IllegalStateException("Program je vec pokrenut, ne mozete pokrenuti novu instancu");
            }
        } catch (IOException e) {
            throw new IllegalStateException("IO exception while trying to acquire lock");
        }
        return lock;
    }

    private static void closeSingletonApplicationLock(FileLock lock) {
        try {
            lock.channel().close();
        } catch (IOException e) {
            log.error(e);
        }
    }
}