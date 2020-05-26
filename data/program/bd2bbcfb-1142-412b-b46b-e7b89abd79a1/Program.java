import java.util.Enumeration;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.DailyRollingFileAppender;
import java.lang.Exception;

import com.lionsoft.standard.BPContext;
import com.lionsoft.standard.ExitException;

// Import section
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import org.json.simple.*;

public class Program {

  static Logger logger = Logger.getLogger(Program.class.getName());
  static BPContext context = null;

  // GLobals section

  public Program(BPContext c) {
    _init();
    context = c;
    _initLog();
  }

  public static void main(String[] argv) {
    int resultCode = 0;

    _init();

    int c;
    LongOpt[] longopts = new LongOpt[6];

    longopts[0] = new LongOpt("warn", LongOpt.NO_ARGUMENT, null, 0);
    longopts[1] = new LongOpt("trace", LongOpt.NO_ARGUMENT, null, 1);
    longopts[2] = new LongOpt("debug", LongOpt.NO_ARGUMENT, null, 2);
    longopts[3] = new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h');
    longopts[4] = new LongOpt("properties", LongOpt.REQUIRED_ARGUMENT, null, 'p');
    longopts[5] = new LongOpt("logpath", LongOpt.REQUIRED_ARGUMENT, null, 'L');

    Getopt g = new Getopt("Program", argv, "p:L:h", longopts);
    g.setOpterr(false);

    while ((c = g.getopt()) != -1) {
      switch (c) {
        case 0:
          System.out.println("Warn enabled");
          logger.setLevel(Level.WARN);
          break;

        case 1:
          System.out.println("Trace enabled");
          logger.setLevel(Level.TRACE);
          break;

        case 2:
          System.out.println("DEBUG enabled");
          logger.setLevel(Level.DEBUG);
          break;

        case 'p':
          context.loadProgramProperties(g.getOptarg());
          break;

        case 'L':
          context.setLogPath(g.getOptarg());
          _initLog();
          break;

        case 'h':
          System.out.println("Help");
          break;

        case '?':
          System.out.println("The option '" + (char) g.getOptopt() + "' is not valid");
          break;

        default:
          System.out.println("getopt() returned " + c);
          break;
      }
    }

    // System.out.println("argv.length = " + argv.length + "\n");
    // System.out.println("g.getOptind() = " + g.getOptind() + "\n");

    int k = 0, n = argv.length - g.getOptind();
    String[] _argv = null;

    if (n > 0) {
      _argv = new String[n];

      for (int i = g.getOptind(); i < argv.length; i++) _argv[k++] = argv[i];
    }

    try {
      _main(_argv);
    } catch (ExitException e) {

    } catch (Exception e) {
      logger.error(e);
    }
    // System.exit(resultCode);
  }

  public static BPContext _getContext() {
    return (context);
  }

  /** _init() */
  public static void _init() {
    BasicConfigurator.configure();
    logger.setLevel(Level.INFO);

    if (context == null) context = new BPContext();
  }

  /** _initLog() */
  public static void _initLog() {
    // https://examples.javacodegeeks.com/enterprise-java/log4j/log4j-rolling-daily-file-example/
    // DailyRollingFileAppender appender = new DailyRollingFileAppender(layout,
    // context.getLogPath()+"test.log", "'.'yyyy-MM-dd");

    PatternLayout patternLayoutObj = new PatternLayout();
    // String conversionPattern = "[%p] %d %c %M - %m%n";
    String conversionPattern = "%d [%p] [%t] %m%n";
    patternLayoutObj.setConversionPattern(conversionPattern);

    // Create Daily Rolling Log File Appender
    DailyRollingFileAppender rollingAppenderObj = new DailyRollingFileAppender();
    rollingAppenderObj.setFile(context.getLogPath() + "/" + context.getLogName() + ".log");
    rollingAppenderObj.setDatePattern("'.'yyyy-MM-dd");
    rollingAppenderObj.setLayout(patternLayoutObj);
    rollingAppenderObj.activateOptions();

    // logger.info("Log: "+context.getLogPath()+"/FirstProgram.log");

    // Configure the Root Logger
    Logger rootLoggerObj = Logger.getRootLogger();
    // System.out.println("rootLoggerObj: "+rootLoggerObj.getName());
    // rootLoggerObj.setLevel(Level.INFO);

    Enumeration<Logger> allLoggers = LogManager.getCurrentLoggers();
    while (allLoggers.hasMoreElements()) {
      Logger l = allLoggers.nextElement();
      // System.out.println("Logger: "+l.getName());

      Enumeration<Appender> allAppenders = l.getAllAppenders();
      while (allAppenders.hasMoreElements()) {
        Appender a = allAppenders.nextElement();
        // System.out.println("Appender: "+a.getName());
        logger.removeAppender(a);
      }
    }

    logger.addAppender(rollingAppenderObj);
  }

  public static void _log(String s) {
    logger.info(s);
  }

  public static void _error(String s) {
    logger.error(s);
  }

  public static int _getCode() {
    return (context.getCode());
  }

  public static void _exit(int code, String message) throws ExitException {
    if (context != null) context.setResult(code, message);

    throw new ExitException(message);
  }

  // User code

  public static void _main(String[] input) throws ExitException {

    // Node: Print String

    System.out.println(input[0]);
    return;
  }

  public static void method_File() throws ExitException {
    String _conn_content_6 = null;
    String _conn_msg_8 = null;
    String[] _conn_lines_15 = null;
    Integer _conn_count_16 = null;
    JSONObject _conn_bp_out_23 = null;

    // Node: File Load

    _conn_content_6 = null;

    try {
      _conn_content_6 =
          new String(
              Files.readAllBytes(
                  Paths.get("/media/fabio/DATA/FILES/ASCC_STAT_20191029_143000.txt")));
    } catch (NoSuchFileException e) {
      _conn_msg_8 = "File not found: " + e.getMessage();
    } catch (IOException e) {
      _conn_msg_8 = "IOException :" + e.getMessage();
    }

    if (_conn_content_6 != null) {
      // Node: String Split Lines

      _conn_lines_15 = _conn_content_6.split("\\r?\\n");
      _conn_count_16 = _conn_lines_15.length;
      // Node: ProcessRecord

      _conn_bp_out_23 = method_ProcessRecord(_conn_lines_15[0]);
      // Node: Print

      System.out.println(_conn_bp_out_23);
      return;
    } else {
      // Node: Exit

      _exit(1, _conn_msg_8);
    }
  }

  public static JSONObject method_ProcessRecord(String line) throws ExitException {
    JSONObject jo = null;
    String[] _conn_lines_8 = null;
    Integer _conn_count_9 = null;
    JSONObject _conn_jo_18 = null;

    // Node: JSON Object New

    _conn_jo_18 = new JSONObject();
    jo = _conn_jo_18;
    // Node: String Split

    _conn_lines_8 = line.split("\\|", -1);
    _conn_count_9 = _conn_lines_8.length;
    // Node: JSON Put

    jo.put("id", _conn_lines_8[0]);
    return (jo);
  }

  public static void method_TEST() throws ExitException {
    JSONObject _conn_bp_out_6 = null;

    // Node: ProcessRecord

    _conn_bp_out_6 = method_ProcessRecord("123|ABC");
    // Node: Print

    System.out.println(_conn_bp_out_6);
    return;
  }
};
