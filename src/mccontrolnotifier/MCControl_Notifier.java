/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mccontrolnotifier;

import PidCloser.ProcessCloserRemote;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author KOCMOC
 */
public class MCControl_Notifier implements Runnable {

    private final JFrame jframe;
    //
    private String INITIAL_TITLE = "MCControl Monitor";
    public static final String COMPANY_NAME_COMPOUNDS = "cp";
    public static final String COMPANY_NAME_WALTERHAUSEN = "wh";
    public static final String COMPANY_NAME_CONTI_HUNG = "conti_hung";
    public static final String COMPANY_NAME_FEDMOG = "fedmog";
    public static final String COMPANY_NAME_QEW = "qew";
    public static final String COMPANY_NAME_DEFAULT = "default";
    //
    private static final Properties MAIN_PROPS = HelpM.properties_load_properties("control_notifier.properties", false);
    public static final String COMPANY_NAME = MAIN_PROPS.getProperty("company", "default");
    public static final int UPDATE_RATE_MINUTES = Integer.parseInt(MAIN_PROPS.getProperty("update_rate_min", "10"));
    //
    private Sql sql = new Sql();
    private Sql sql_mixer = new Sql();
    private Sql sql_mills_extruder = new Sql();
    private ArrayList<Integer> mixer_lines = new ArrayList<Integer>();// this should be taken from properties
    private ArrayList<Integer> mills_lines = new ArrayList<Integer>();// this should be taken from properties
    private ArrayList<Integer> extruder_lines = new ArrayList<Integer>();// this should be taken from properties
    //
    private HashMap<Integer, ArrayList<Integer>> all_lines = new HashMap<Integer, ArrayList<Integer>>();
    //
    private JPanel CONTAINER_MAIN;
    //
    private final static int MIXER = 1;
    private final static int MILLS = 2;
    private final static int EXTRUDER = 3;
    //
    private static HashMap<Integer, String> device_name_map = new HashMap<Integer, String>();

    static {
        device_name_map.put(MIXER, "Mixer");
        device_name_map.put(MILLS, "Mills");
        device_name_map.put(EXTRUDER, "Extruder");
    }
    //
    private String HOST;
    private String PORT;
    private boolean USE_JTDS_LIB;
    private String DB_NAME_MIXER;
    private String DB_NAME_MILLS_EXT;
    private String USER_MIXER;
    private String USER_MILLS_EXT;
    private String PASS_MIXER;
    private String PASS_MILLS_EXT;
    //
    private static final String LOGG_FILE = "log.txt";

    public MCControl_Notifier(JFrame frame) {
        this.jframe = frame;
        init();
    }

    private void init() {
        define_lines();
        define_properties();
        prepare_sql_connections();
        new Thread(this).start();
    }

    public JPanel getContainerMain() {
        return this.CONTAINER_MAIN;
    }

    public Sql getSql() {
        return this.sql;
    }

    public String getNpmsHost() {
        if (COMPANY_NAME.equals(COMPANY_NAME_WALTERHAUSEN)) {
            return "172.18.98.251";
        } else if (COMPANY_NAME.equals(COMPANY_NAME_CONTI_HUNG)) {
            return "172.18.118.9";
        } else {
            return HOST;
        }
    }

    public ArrayList<String> getExeProgramsToMonitor() {
        //
        ArrayList<String> toReturn = new ArrayList<String>();
        //
        String str = MAIN_PROPS.getProperty("exe_progs_to_monitor", "");
        //
        if (str.isEmpty()) {
            return null;
        }
        //
        String arr[] = str.split(";");
        //
        toReturn.addAll(Arrays.asList(arr));
        //
        return toReturn;
    }

    private void define_lines() {
        String mixer_lines_to_monitor = MAIN_PROPS.getProperty("mixer_lines_to_monitor", "");
        String[] arr = mixer_lines_to_monitor.split(";");
        //
        for (String line : arr) {
            mixer_lines.add(Integer.parseInt(line));
        }
        //
        String mills_lines_to_monitor = MAIN_PROPS.getProperty("mills_lines_to_monitor", "");
        String[] arr2 = mills_lines_to_monitor.split(";");
        for (String line : arr2) {
            if (line.isEmpty()) {
                //
            } else {
                mills_lines.add(Integer.parseInt(line));
            }
        }
        //
        String extruder_lines_to_monitor = MAIN_PROPS.getProperty("extruder_lines_to_monitor", "");
        String[] arr3 = extruder_lines_to_monitor.split(";");
        for (String line : arr3) {
            if (line.isEmpty()) {
                //
            } else {
                extruder_lines.add(Integer.parseInt(line));
            }
        }
        //
        all_lines.put(MIXER, mixer_lines);
        all_lines.put(MILLS, mills_lines);
        all_lines.put(EXTRUDER, extruder_lines);
        //
    }

    private void define_properties() {
        HOST = MAIN_PROPS.getProperty("host", "localhost");
        PORT = MAIN_PROPS.getProperty("port", "1433");
        //
        USE_JTDS_LIB = Boolean.parseBoolean(MAIN_PROPS.getProperty("use_jtds_lib", "false"));
        GP.SQL_LIBRARY_JTDS = USE_JTDS_LIB;
        //
        DB_NAME_MIXER = MAIN_PROPS.getProperty("db_name_mixer", "");
        DB_NAME_MILLS_EXT = MAIN_PROPS.getProperty("db_name_mills_extruder", "");
        USER_MIXER = MAIN_PROPS.getProperty("user_mixer", "");
        USER_MILLS_EXT = MAIN_PROPS.getProperty("user_mills_extruder", "");
        PASS_MIXER = MAIN_PROPS.getProperty("pass_mixer", "");
        PASS_MILLS_EXT = MAIN_PROPS.getProperty("pass_mills_extruder", "");
    }

    private void prepare_sql_connections() {
        boolean mixer_connect = false;
        boolean mills_connect = false;
        //
        if (mixer_lines.isEmpty() == false) {
            mixer_connect = connect(sql_mixer, HOST, PORT, DB_NAME_MIXER, USER_MIXER, PASS_MIXER);
        }
        if (mills_lines.isEmpty() == false || extruder_lines.isEmpty() == false) {
            mills_connect = connect(sql_mills_extruder, HOST, PORT, DB_NAME_MILLS_EXT, USER_MILLS_EXT, PASS_MILLS_EXT);
        }
        //
        if (mixer_connect == false && mills_connect == false) {
            SimpleLoggerLight.logg(LOGG_FILE, "Lines to monitor are empty for all devices!");
            System.exit(0);
        }
    }

    private boolean connect(Sql sql, String host, String port, String db_name, String user, String pass) {
        try {
            sql.connect(host, port, db_name, user, pass);
            return true;
        } catch (SQLException ex) {
            SimpleLoggerLight.logg(LOGG_FILE, "Connection to: " + db_name + " failed");
            Logger.getLogger(MCControl_Notifier.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public void go() {
        //
        if (CONTAINER_MAIN != null) {
            jframe.remove(CONTAINER_MAIN);
        }
        //
        CONTAINER_MAIN = new JPanel();
        //        CONTAINER_MAIN.setBackground(Color.yellow);
        //
        set_layout_of_container_main(mixer_lines.size() + mills_lines.size() + extruder_lines.size());
        //
        Set set = all_lines.keySet();
        Iterator it = set.iterator();
        while (it.hasNext()) {
            int key = (Integer) it.next();
            ArrayList<Integer> value = (ArrayList<Integer>) all_lines.get(key);
            build(value, key);
        }
        //
        synchronized (jframe) {
            jframe.notify();
        }
        //
        jframe.setTitle(INITIAL_TITLE + " (last update: " + HelpM.get_proper_date_time_same_format_on_all_computers() + ")");
        //
//        addRestartProcessBtn();
        //

    }

    private void addRestartProcessBtn() {
        JButton restart_btn = new JButton("Restart rec. program");
        CONTAINER_MAIN.add(restart_btn);
        restart_btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                java.awt.EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        new ProcessCloserRemote(sql, HOST, 1111, getExeProgramsToMonitor()).setVisible(true);
                    }
                });
            }
        });
    }

    private void set_layout_of_container_main(int lines) {
        if (lines <= 2) {
            CONTAINER_MAIN.setLayout(new GridLayout(1, 2));
        } else if (lines <= 4) {
            CONTAINER_MAIN.setLayout(new GridLayout(2, 2));
        } else if (lines <= 6) {
            CONTAINER_MAIN.setLayout(new GridLayout(3, 2));
        } else if (lines <= 8) {
            CONTAINER_MAIN.setLayout(new GridLayout(4, 2));
        } else {
            CONTAINER_MAIN.setLayout(new GridLayout(6, 4));
        }
    }

    private void build(ArrayList<Integer> lines, int device) {

        for (int i = 0; i < lines.size(); i++) {
            int line_nr = lines.get(i);
            //
            String date = get_date_line_x_from_db(line_nr, device);//=================================>>>> OBS!
            //
            if (date.isEmpty()) {
                continue;
            }
            //
            JPanel container = new JPanel(new GridLayout(2, 1));
            container.setBorder(BorderFactory.createRaisedBevelBorder());
            container.setBackground(Color.white);
            //
            JPanel line_nr_title = new JPanel(new GridLayout(1, 1));
            JLabel line_label = new JLabel("<html><p style='margin-left:10px;margin-right:5px;font-size:18pt'>"
                    + device_name_map.get(device) + " Line: " + line_nr + "</p></html>");
            line_nr_title.add(line_label);
            //
            //
            //
            JPanel last_rec_panel = new JPanel(new GridLayout(1, 1));
            //
            double days_offline = get_days_offline(date);
            //
            JLabel last_rec_label = new JLabel("<html><p style='margin-left:15;margin-right:5px;'>"
                    + "Last recorded batch: " + "<u>" + date + "</u></p></html>");
            format_label(container, days_offline);
            last_rec_panel.add(last_rec_label);
            //
            line_nr_title.setBackground(Color.WHITE);
            last_rec_panel.setBackground(Color.WHITE);
            container.add(line_nr_title);
            container.add(last_rec_panel);
            //
            CONTAINER_MAIN.add(container);
        }
        //
        //
    }

    private String get_date_line_x_from_db(int line_nr, int device) {
        String q;
        String date_column_name;
        //
        if (device == MIXER) {
            sql = sql_mixer;
            q = SQL_Select.get_date_latest_batch_mixer(line_nr);
            date_column_name = "PROD_DATE";
            //
            if(COMPANY_NAME.equals(COMPANY_NAME_QEW)){
                date_column_name = "A7";
            }
            //
        } else if (device == MILLS) {
            sql = sql_mills_extruder;
            q = SQL_Select.get_date_latest_batch_mills(line_nr);
            date_column_name = "Datum";
        } else if (device == EXTRUDER) {
            sql = sql_mills_extruder;
            q = SQL_Select.get_date_latest_batch_extruder(line_nr);
            date_column_name = "datum";
        } else {
            logg_line_not_found(line_nr, device);
            return "";
        }

        try {
            System.out.println("" + q);
            SimpleLoggerLight.logg(LOGG_FILE, q);
            ResultSet rs = sql.execute(q);
            //
            if (rs.next()) {
                return rs.getString(date_column_name);
            }
            //
        } catch (SQLException ex) {
            logg_line_not_found(line_nr, device);
            Logger.getLogger(MCControl_Notifier.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }
        logg_line_not_found(line_nr, device);
        return "";
    }

    private void logg_line_not_found(int line_nr, int device) {
        SimpleLoggerLight.logg(LOGG_FILE, "Device: " + device_name_map.get(device) + "/ Line: " + line_nr + "  not found");
    }

    private double get_days_offline(String date) {
        String date_format = HelpM.define_date_format(date);
        long date_to_millis = HelpM.dateToMillisConverter3(date, date_format);
        long now = System.currentTimeMillis();
        long diff = now - date_to_millis;
        double days = HelpM.millis_to_days_converter(diff);
        return (int) days;
    }
    

    private void format_label(JPanel panel, double days_offline) {
        if (days_offline == 0.6) {
            panel.setBorder(BorderFactory.createLineBorder(Color.orange, 5));
        } else if (days_offline > 1.7) {
            panel.setBorder(BorderFactory.createLineBorder(Color.red, 5));
        }
    }

    public static Point position_window_in_center_of_the_screen(JFrame window) {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        return new Point((d.width - window.getSize().width) / 2, (d.height - window.getSize().height) / 2);
    }

    @Override
    public void run() {
        while (true) {
            go();
            wait_(UPDATE_RATE_MINUTES * 60000);
        }
    }

    private void wait_(int millis) {
        synchronized (this) {
            try {
                wait(millis);
            } catch (InterruptedException ex) {
                Logger.getLogger(MCControl_Notifier.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
