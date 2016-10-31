/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mccontrolnotifier;

import java.util.HashMap;

/**
 *
 * @author KOCMOC
 */
public class SQL_Select {

    public static HashMap<Integer, String> MILLS_LINE_MAIN_TABLE_MAP_DEFAULT = new HashMap<Integer, String>();
    public static HashMap<Integer, String> MILLS_LINE_MAIN_TABLE_MAP_CP = new HashMap<Integer, String>();
    public static HashMap<Integer, String> MILLS_LINE_MAIN_TABLE_MAP_FEDMOG = new HashMap<Integer, String>();

    static {
        MILLS_LINE_MAIN_TABLE_MAP_FEDMOG.put(1, "");
    }

    static {
        MILLS_LINE_MAIN_TABLE_MAP_CP.put(10, "2");
        MILLS_LINE_MAIN_TABLE_MAP_CP.put(20, "");
    }

    static {
        MILLS_LINE_MAIN_TABLE_MAP_DEFAULT.put(1, "");
        MILLS_LINE_MAIN_TABLE_MAP_DEFAULT.put(2, "2");
        MILLS_LINE_MAIN_TABLE_MAP_DEFAULT.put(3, "3");
        MILLS_LINE_MAIN_TABLE_MAP_DEFAULT.put(4, "4");
        MILLS_LINE_MAIN_TABLE_MAP_DEFAULT.put(5, "5");
        MILLS_LINE_MAIN_TABLE_MAP_DEFAULT.put(6, "6");
        MILLS_LINE_MAIN_TABLE_MAP_DEFAULT.put(7, "7");
    }

    public static String get_date_latest_batch_mixer(int line_nr) {
        //
        String tableName = "MC_BATCHINFO";
        String date = "PROD_DATE";
        String line = "LINE";
        //
        if (MCControl_Notifier.COMPANY_NAME.equals(MCControl_Notifier.COMPANY_NAME_QEW)) {
            tableName = "sysq";
            date = "A7";
            line = "Round(A9,0)";
        }
        //
        return "SELECT * FROM " + tableName + " "
                + "WHERE "
                + line + "=" + line_nr + " "
                + "ORDER BY " + date + " DESC";
        //
    }

    public static String get_date_latest_batch_extruder(int line_nr) {
        return "SELECT * FROM main_table "
                + "WHERE "
                + "line_nr =" + line_nr + " "
                + "ORDER BY datum DESC";
    }

    public static String get_date_latest_batch_mills(int line_nr) {
        String mainTable_apendix;
        //
        if (MCControl_Notifier.COMPANY_NAME.equals(MCControl_Notifier.COMPANY_NAME_DEFAULT)) {
            mainTable_apendix = MILLS_LINE_MAIN_TABLE_MAP_DEFAULT.get(line_nr);
        } else if (MCControl_Notifier.COMPANY_NAME.equals(MCControl_Notifier.COMPANY_NAME_WALTERHAUSEN)) {
            mainTable_apendix = MILLS_LINE_MAIN_TABLE_MAP_DEFAULT.get(line_nr);
        } else if (MCControl_Notifier.COMPANY_NAME.equals(MCControl_Notifier.COMPANY_NAME_COMPOUNDS)) {
            mainTable_apendix = MILLS_LINE_MAIN_TABLE_MAP_CP.get(line_nr);
            line_nr = line_nr / 10;// This is done because i after some time changed the Nr abbriviation on mills, Line 20=2 & 10=1
        } else {
            mainTable_apendix = MILLS_LINE_MAIN_TABLE_MAP_DEFAULT.get(line_nr);
        }
        //
        return "SELECT * FROM MainTable" + mainTable_apendix + " "
                + "WHERE "
                + "Line =" + line_nr + " "
                + "ORDER BY Datum DESC";
    }
}
