/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mccontrolnotifier;

import java.net.URL;
import java.util.Properties;

/**
 *
 * @author Administrator
 */
public class GP {

    public static final URL IMAGE_ICON_URL = GP.class.getResource("icon.png");
    //=================================================
    public static String MSSQL_CREATE_STATEMENT_SIMPLE = "false";
    public static int MSSQL_LOGIN_TIME_OUT;
    public static boolean SQL_LIBRARY_JTDS = true;
    public static boolean JTDS_USE_NAMED_PIPES = false;
    public static String JTDS_INSTANCE_PARAMETER = "";
    public static String JTDS_DOMAIN_WORKGROUP = "";
    //=================================================
    public static boolean LOGG_CONNECTION_STRING = true;
}
