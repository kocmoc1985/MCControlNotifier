/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mccontrolnotifier;

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Robot;
import java.awt.TextArea;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

/**
 *
 * @author Administrator
 */
public class HelpM {

    private static HashMap<String, String> properties_to_use_map = new HashMap();
    private static int nr_properties;

    public static void err_output_to_file() {
        //Write error stream to a file
        create_dir_if_missing("err_output");
        try {
            String err_file = "err_" + get_date_time() + ".txt";
            String output_path = "err_output/" + err_file;

            PrintStream out = new PrintStream(new FileOutputStream(output_path));
            System.setErr(out);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(HelpM.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void create_dir_if_missing(String path_and_folder_name) {
        File f = new File(path_and_folder_name);
        if (f.exists() == false) {
            f.mkdir();
        }
    }

    public static String get_date_time() {
        DateFormat formatter = new SimpleDateFormat("yyyy_MM_dd HH_mm_ss");
        Calendar calendar = Calendar.getInstance();
        return formatter.format(calendar.getTime());
    }

    public static String get_date() {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar = Calendar.getInstance();
        return formatter.format(calendar.getTime());
    }

    public static String define_date_format(String date) {
        if (date != null) {
            for (String parse : formats) {
                SimpleDateFormat sdf = new SimpleDateFormat(parse);
                try {
                    sdf.parse(date);
                    return parse;
                } catch (ParseException e) {
                    //Do nothing
                }
            }
        }
        return null;
    }
    /**
     * It's best not to change anything here
     */
    private static final String[] formats = {
        "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ssZ",
        "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd HH:mm:ss",
        "MM/dd/yyyy HH:mm:ss", "MM/dd/yyyy'T'HH:mm:ss.SSS'Z'",
        "MM/dd/yyyy'T'HH:mm:ss.SSSZ", "MM/dd/yyyy'T'HH:mm:ss.SSS",
        "MM/dd/yyyy'T'HH:mm:ssZ", "MM/dd/yyyy'T'HH:mm:ss",
        "yyyy:MM:dd HH:mm:ss",
        "yyyy-MM-dd", "yyyy:MM:dd", "yyyyMMdd",
        "dd/MM/yy", "dd/MM/yyyy", "dd-MM-yy", "dd-MM-yyyy",
        "dd:MM:yy", "dd:MM:yyyy"};

    public static long dateToMillisConverter3(String date, String date_format) {
        DateFormat formatter = new SimpleDateFormat(date_format);
        try {
            return formatter.parse(date).getTime();
        } catch (ParseException ex) {
            Logger.getLogger(HelpM.class.getName()).log(Level.SEVERE, null, ex);
            return -1;
        }
    }

    public static double millis_to_days_converter(double millis) {
        return millis / 86400000;
    }

    public static Properties choose_properties(String path) {
        String dialog = choose_properties_dialog_string(path);
        String property_path;
        Properties properties;
        int val = 0;
        if (nr_properties == 1) {
            property_path = (String) properties_to_use_map.get("" + 1);
            properties = HelpM.properties_load_properties(path + "/" + property_path, false);
        } else {
            try {
                val = Integer.parseInt(JOptionPane.showInputDialog(dialog));
            } catch (Exception ex) {
                System.exit(0);
            }

            property_path = (String) properties_to_use_map.get("" + val);
            properties = HelpM.properties_load_properties(path + "/" + property_path, false);
        }
        if (properties == null) {
            JOptionPane.showMessageDialog(null, "properties not found or error occured: " + property_path + " / program will close");
            System.exit(0);
            return null;
        }
        return properties;
    }

    private static String choose_properties_dialog_string(String path) {
        File[] f = new File(path).listFiles();
        String dialog = "";
        int i = 1;
        for (File file : f) {
            String file_name = file.getName();
            if (file_name.contains("properties") && file_name.contains("other") == false
                    && file_name.contains("properties") && file_name.contains("update_map") == false) {
                dialog += i + ". " + file_name + "\n";
                properties_to_use_map.put("" + i, file_name);
                i++;
                nr_properties++;
            }
        }
        return dialog.isEmpty() ? "0" : dialog;
    }

    public static void run_application_with_associated_application(File file) throws IOException {
        Desktop.getDesktop().open(file);
    }

    public static void run_application_exe_or_jar(String application_to_run_name, String path) throws IOException {
        String[] commands = new String[3];
        if (application_to_run_name.contains(".jar")) {
            commands[0] = "java";
            commands[1] = "-jar";
            commands[2] = application_to_run_name; //OBS! pay attention here
        } else {
            commands[0] = path + "/" + application_to_run_name; // and here!
            commands[1] = "";
            commands[2] = "";
        }
        ProcessBuilder builder = new ProcessBuilder(commands);
        builder.directory(new File(path));
        builder.start();
    }

    public static void run_program_with_catching_output(JTextArea textarea, String cmd1, String cmd2, String cmd3) {
        textarea.setText("");
        String[] commands2 = {cmd1, cmd2, cmd3};
        try {
            run_program_with_catching_output_overall(textarea, commands2);
        } catch (IOException ex) {
            Logger.getLogger(HelpM.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void run_program_with_catching_output_overall(JTextArea textarea, String[] commands) throws IOException {
        String line;
        OutputStream stdin;
        InputStream stderr;
        InputStream stdout;

        // launch EXE and grab stdin/stdout and stderr
        Process process = Runtime.getRuntime().exec(commands);
        stdin = process.getOutputStream();
        stderr = process.getErrorStream();
        stdout = process.getInputStream();

        // "write" the parms into stdin
        line = "param1" + "\n";
        stdin.write(line.getBytes());
        stdin.flush();

        line = "param2" + "\n";
        stdin.write(line.getBytes());
        stdin.flush();

        line = "param3" + "\n";
        stdin.write(line.getBytes());
        stdin.flush();

        stdin.close();

        // clean up if any output in stdout
        BufferedReader brCleanUp
                = new BufferedReader(new InputStreamReader(stdout));
        while ((line = brCleanUp.readLine()) != null) {
            textarea.append("" + line + "\n");
        }
        brCleanUp.close();

        // clean up if any output in stderr
        brCleanUp
                = new BufferedReader(new InputStreamReader(stderr));
        while ((line = brCleanUp.readLine()) != null) {
            textarea.append("" + line);
            System.out.println("[Stderr] " + line);
        }
        brCleanUp.close();
    }

    public static void run_with_cmd(String cmd_application, String arg) {
        String[] commands = {"cmd", "/c", "start", "\"" + cmd_application + "\"", cmd_application, arg};
        ProcessBuilder builder = new ProcessBuilder(commands);
        try {
            builder.start();
        } catch (IOException ex) {
            Logger.getLogger(HelpM.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static Properties properties_load_properties(String path_andOr_fileName, boolean list_properties) {
        Properties p = new Properties();
        try {
            p.load(new FileInputStream(path_andOr_fileName));
            if (list_properties == true) {
                p.list(System.out);
            }
        } catch (IOException ex) {
            System.out.println("" + ex);
        }
        return p;
    }

    public static String addDay(String date) {
        String[] arr = date.split("-");
        int day = Integer.parseInt(arr[2]);
        day++;
        return "" + arr[0] + "-" + arr[1] + "-" + day;
    }

    public static void copy_file(String file_to_copy, String name_of_duplicate) throws FileNotFoundException, IOException {
        File inputFile = new File(file_to_copy);
        File outputFile = new File(name_of_duplicate);

        FileInputStream in = new FileInputStream(inputFile);
        FileOutputStream out = new FileOutputStream(outputFile);
        int c;
        while ((c = in.read()) != -1) {
            out.write(c);
        }

        in.close();
        out.close();
    }

    public static int get_column_count(ResultSet rs) throws SQLException {
        ResultSetMetaData meta;
        String[] headers;
        meta = rs.getMetaData();
        headers = new String[meta.getColumnCount()];
        return headers.length;
    }

    public static void looseFocus() {
        Robot robot = null;
        try {
            robot = new Robot();
        } catch (AWTException ex) {
            Logger.getLogger(HelpM.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (robot == null) {
            return;
        }
        // This is done to loose focus ========      
        robot.keyPress(KeyEvent.VK_TAB);
        wait_(10);
        robot.keyRelease(KeyEvent.VK_TAB);
        wait_(10);

        robot.keyPress(KeyEvent.VK_ENTER);
        wait_(10);
        robot.keyRelease(KeyEvent.VK_ENTER);
        wait_(50);
        robot.keyPress(KeyEvent.VK_ESCAPE);
        wait_(10);
        robot.keyRelease(KeyEvent.VK_ESCAPE);
        //======================================
    }

    private static void wait_(int i) {
        try {
            Thread.sleep(i);
        } catch (InterruptedException ex) {
            Logger.getLogger(HelpM.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * VERY
     * IMPORTANT!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     *
     * @param path
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static byte[] filetoByteArray(String path) throws FileNotFoundException, IOException {
        byte[] content;
        FileInputStream p = new FileInputStream(path);
        content = new byte[p.available()];
        p.read(content);
        p.close();
        return content;
    }

    /**
     * !!!!!!!!!!!!!!!!!!!!!!!!! Verified! Works good with
     * 'filetoByteArray(String path)'
     *
     * @param path
     * @param arr
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static void byteArrayToFile(String path, byte[] arr) throws FileNotFoundException, IOException {
        File f2 = new File(path);
        OutputStream out;
        out = new FileOutputStream(f2);
        out.write(arr);
    }

    public static void copyFile(String file_to_copy, String duplicate_file_name) throws IOException {
        byte[] b_arr = filetoByteArray(file_to_copy);
        byteArrayToFile(duplicate_file_name, b_arr);
        System.out.println("copy files done");
    }

    public static String get_proper_date_time_same_format_on_all_computers() {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar calendar = Calendar.getInstance();
        return formatter.format(calendar.getTime());
    }

    public static String get_proper_date_time_same_format_on_all_computers_err_output() {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH_mm");
        Calendar calendar = Calendar.getInstance();
        return formatter.format(calendar.getTime());
    }

    /**
     * Use 3
     *
     * @param style
     * @return
     */
    public static String get_proper_date_adjusted_format(int style) {
        TimeZone tz = TimeZone.getDefault();
        Calendar cal = Calendar.getInstance(tz);
        DateFormat f1 = DateFormat.getDateInstance(style);
        Date d = cal.getTime();
        return f1.format(d);
    }

    public static String extractValueFromHtmlString(String str) {
        if (str.contains("<") == false) {
            return str;
        }
        String arr[] = str.split(">");
        String arr_2[] = arr[2].split("<");
        return arr_2[0];
    }
    
    public static void writeToFile(String fileName, String textToWrite) throws IOException {
        FileWriter fstream = new FileWriter(fileName, false);
        BufferedWriter out = new BufferedWriter(fstream);

        out.write(textToWrite);
        out.newLine();
        out.flush();
    }
    
    public static String get_desktop_path() {
        return System.getProperty("user.home") + "\\" + "Desktop";
    }
}
