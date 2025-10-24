package Configurations;

import core.utils.ExcelReader.ReadProperties;
import org.apache.log4j.Logger;

public class EnvironmentConfiguration {

    private static final Logger log = Logger.getLogger(EnvironmentConfiguration.class);
    private static String BaseURL;
    private static String Username;
    private static String Password;
    private static String SFDCBaseURL;
    private static String SFDCUsername;
    private static String SFDCPassword;
    private static String DBURL;
    private static String BrowserName;
    private static String EmailSender;
    private static String smtphost;
    private static String smtpport;
    private static String EmailReceiver;
    private static String SprintID;
    private static String smtppwd;
    private static String smtpuser;
    private static String EnvironmentName;
    private static ReadProperties properties = ReadProperties.getInstance();

    public static void intislizeEnvironment() {
        String propName = "AdminUser";
        BaseURL = properties.getEnvironmentProperties().getProperty("URL");
        DBURL = properties.getEnvironmentProperties().getProperty("DBURL");
        Username = properties.getEnvironmentProperties().getProperty("username");
        Password = properties.getEnvironmentProperties().getProperty("password");
        SFDCBaseURL = properties.getEnvironmentProperties().getProperty("sfdcURL");
        SFDCUsername = properties.getEnvironmentProperties().getProperty("sfdcusername");
        SFDCPassword = properties.getEnvironmentProperties().getProperty("sfdcpassword");
        BrowserName = properties.getEnvironmentProperties().getProperty("Browser");
        EmailSender = properties.getEnvironmentProperties().getProperty("EmailSender");
        smtphost = properties.getEnvironmentProperties().getProperty("smtphost");
        smtppwd = properties.getEnvironmentProperties().getProperty("smtppwd");
        smtpuser = properties.getEnvironmentProperties().getProperty("smtpuser");
        smtpport = properties.getEnvironmentProperties().getProperty("smtpport");
        EmailReceiver = properties.getEnvironmentProperties().getProperty("EmailReceiver");
        SprintID = properties.getEnvironmentProperties().getProperty("SprintID");

        log.info("Storing Admin user properties file values in variables.");
    }

    // Getter methods for the properties
    public static String getBaseURL() {
        return BaseURL;
    }

    public static String getUsername() {
        return Username;
    }

    public static String getPassword() {
        return Password;
    }

    public static String getSFDCBaseURL() {
        return SFDCBaseURL;
    }

    public static String getSFDCUsername() {
        return SFDCUsername;
    }

    public static String getSFDCPassword() {
        return SFDCPassword;
    }

    public static String getDBURL() {
        return DBURL;
    }

    public static String getBrowserName() {
        return BrowserName;
    }

    public static String getEmailSender() {
        return EmailSender;
    }

    public static String getSmtphost() {
        return smtphost;
    }

    public static String getSmtpport() {
        return smtpport;
    }

    public static String getEmailReceiver() {
        return EmailReceiver;
    }

    public static String getSprintID() {
        return SprintID;
    }

    public static String getSmtppwd() {
        return smtppwd;
    }

    public static String getSmtpuser() {
        return smtpuser;
    }

    public static String getEnvironmentName() {
        return EnvironmentName;
    }
}
