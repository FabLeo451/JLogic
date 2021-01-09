package com.lionsoft.jlogic;

import java.io.*;
import java.util.*;
import java.util.stream.*;
import java.lang.*;
import java.text.SimpleDateFormat;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpMethod;
import org.apache.commons.lang3.time.DateUtils;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

import java.net.URL;
import java.net.MalformedURLException;
import java.beans.IntrospectionException;

import java.net.URLClassLoader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.lang.annotation.Annotation;

@Service
public class PluginService {

    class OutConnector {
      public String label;
      public String type;
      public int array;
      public boolean exec;

      public OutConnector() {
        exec = false;
      }

      public void setExec() {
        exec = true;
        type = "Exec";
      }
    }

    static Logger logger = LoggerFactory.getLogger(PluginService.class);

    public PluginService() {
    }

    /**
     * Create JSON specification for plugin, given path and file
     */
    public JSONObject specFromClass(Plugin plugin) {
      final int FUNCTION = 4;
      final int OPERATOR = 5;

      Method m;
      String methodName;
      //List<String> outGet = new ArrayList<String>();
      List<OutConnector> outConn = new ArrayList<OutConnector>();
      int nExec = 0;

      JSONObject jplugin = new JSONObject();
      JSONArray jnodes = new JSONArray();
      JSONArray jtypes = new JSONArray();

      jplugin.put("types", jtypes);
      jplugin.put("nodes", jnodes);

      try {
        // Set classpath
        List<URL> urls = new ArrayList<>();
        URL[] clUrls = null;

        // Standard path
        File f = new File(Utils.getHomeDir()+"/lib/Standard.jar");
        urls.add(f.toURI().toURL());

        // Plugin path
        f = new File(plugin.getPath());
        urls.add(f.toURI().toURL());

        clUrls = new URL[urls.size()];
        clUrls = urls.toArray(clUrls);

        //Class c = Class.forName(plugin.getClassName());
        ClassLoader cl = new URLClassLoader(clUrls);
        Class c = cl.loadClass(plugin.getClassName());

        Class PluginAnnotation = cl.loadClass("com.lionsoft.jlogic.standard.Plugin");
        Class NodeAnnotation = cl.loadClass("com.lionsoft.jlogic.standard.Node");
        Class InAnnotation = cl.loadClass("com.lionsoft.jlogic.standard.In");
        Class OutAnnotation = cl.loadClass("com.lionsoft.jlogic.standard.Out");

        // Plugin info
        Annotation pluginAnnotation = c.getAnnotation(PluginAnnotation);

        if (pluginAnnotation != null) {
          Class<? extends Annotation> pluginInfo = pluginAnnotation.annotationType();
          m = pluginInfo.getMethod("name");
          plugin.setName((String) m.invoke(pluginAnnotation, (Object[])null));
          m = pluginInfo.getMethod("version");
          plugin.setVersion((String) m.invoke(pluginAnnotation, (Object[])null));
        }

        logger.info("Installing plugin "+plugin.toString());

        Method[] methods = c.getMethods();

        for (Method method : methods) {
          methodName = method.getName();

          Annotation nodeAnnotation = method.getAnnotation(NodeAnnotation);

          if (nodeAnnotation != null) {
            String returnType = Utils.getJavaTypeFromString(method.getReturnType().toString());
            int returnArray = Utils.getJavaArrayFromString(method.getReturnType().toString());

            boolean multipleOut = method.getReturnType().equals(Void.TYPE) ||
                                  (returnType.equals("Object") && returnArray == 1);

            //System.out.println(methodName+" multipleOut="+multipleOut+" "+method.getReturnType().toString());

            JSONObject jnode = new JSONObject();
            JSONArray jinput = new JSONArray();
            JSONArray joutput = new JSONArray();
            jnode.put("input", jinput);
            jnode.put("output", joutput);
            jnode.put("name", methodName);

            Class<? extends Annotation> node = nodeAnnotation.annotationType();

            // Name, type etc.
            m = node.getMethod("name");
            jnode.put("name", m.invoke(nodeAnnotation, (Object[])null));
            jnode.put("type", multipleOut ? FUNCTION : OPERATOR);

            // Input parameters
            if (multipleOut) {
              JSONObject jexec = new JSONObject();
              jexec.put("label", "");
              jexec.put("type", "Exec");
              jinput.add(jexec);
              //joutput.add(jexec);
            }

            // Input
            for (Parameter p : method.getParameters()) {
              //System.out.println(p.toString());

              //String[] parts = p.getType().toString().split("\\.");

              JSONObject jparam = new JSONObject();
              jparam.put("label", p.getName());
              //jparam.put("type", parts[parts.length-1].replace(";", ""));
              jparam.put("type", Utils.getJavaTypeFromString(p.getType().toString()));
              jparam.put("dimensions", Utils.getJavaArrayFromString(p.getType().toString()));

              Annotation paramAnnotation = p.getAnnotation(InAnnotation);

              if (paramAnnotation != null) {
                boolean b;

                Class<? extends Annotation> bpconnector = paramAnnotation.annotationType();
                m = bpconnector.getMethod("label");
                jparam.put("label", m.invoke(paramAnnotation, (Object[])null));

                m = bpconnector.getMethod("single_line");
                b = (boolean) m.invoke(paramAnnotation, (Object[])null);
                if (b)
                  jparam.put("single_line", true);

                m = bpconnector.getMethod("password");
                b = (boolean) m.invoke(paramAnnotation, (Object[])null);
                if (b)
                  jparam.put("password", true);
              }

              jinput.add(jparam);
            }

            // Output
            for (Annotation outAnnotation : method.getAnnotationsByType(OutAnnotation)) {
                Class<? extends Annotation> out = outAnnotation.annotationType();

                OutConnector conn = new OutConnector();

                JSONObject jout = new JSONObject();
                m = out.getMethod("label");
                conn.label = (String) m.invoke(outAnnotation, (Object[])null);

                m = out.getMethod("exec");

                if ((boolean) m.invoke(outAnnotation, (Object[])null)) {
                  // Exec
                  conn.setExec();
                  nExec ++;
                } else {
                  // Data
                  m = out.getMethod("type");
                  conn.type = (String) m.invoke(outAnnotation, (Object[])null);

                  m = out.getMethod("array");
                  conn.array = (Integer) m.invoke(outAnnotation, (Object[])null);

                  m = out.getMethod("variable");
                  String varName = (String) m.invoke(outAnnotation, (Object[])null);
                  /*
                  try {
                    Field field = c.getDeclaredField(varName);
                    jout.put("type", Utils.getJavaTypeFromString(field.getType().toString()));
                    jout.put("dimensions", Utils.getJavaArrayFromString(field.getType().toString()));
                    //int modifiers = field.getModifiers();
                  } catch (NoSuchFieldException e) {
                    logger.error("Variable not found: "+varName+": "+e.getMessage());
                    return null;
                  }

                  m = out.getMethod("get");
                  outGet.add((String) m.invoke(outAnnotation, (Object[])null));
                  */
                  JSONObject jreferences = new JSONObject();
                  jreferences.put("variable", varName);
                  jout.put("references", jreferences);
                }

                outConn.add(conn);

                jout.put("label", conn.label);
                jout.put("type", conn.type);

                if (conn.array > 0)
                  jout.put("array", conn.array);

                joutput.add(jout);
            }

            // Source code
            int nIn = jinput.size();
            int nOut = joutput.size();
            int start = multipleOut ? 1 : 0;

            //System.out.println(methodName+" nIn="+nIn+" nOut="+nOut+" start="+start);

            String java = ""; // Final source code
            String retVals = "", call = "", args = "", outVals = "", execAfter = "";

            for (int i=start; i<nIn; i++) {
              args += i > start ? ", " : "";
              args += "in{"+i+"}";
            }

            call = methodName+"("+args+")";

            if (!multipleOut) {
              java = call;
            } else {
              retVals = "Object[] _{node.id}_out = ";

              // Assing output values
              for (int i=0; i<outConn.size(); i++) {
                if (outConn.get(i).exec)
                  continue;

                outVals += "out{"+i+"} = ("+outConn.get(i).type+")_{node.id}_out["+i+"];"+System.lineSeparator();
              }

              // Following exec
              if (nExec > 1) {
                int added = 0;

                for (int i=0; i<outConn.size(); i++) {
                  if (outConn.get(i).exec) {
                    execAfter += "if ((Boolean) _{node.id}_out["+i+"]) { exec{"+i+"} }";
                    added ++;

                    if (added < nExec)
                      execAfter += " else ";
                  }
                }
              }

              java = retVals + call + ";" + System.lineSeparator() +
                     outVals + System.lineSeparator() +
                     execAfter;
            }

            jnode.put("java", java);

            // Add node
            jnodes.add(jnode);
          }
        }

      } catch (ClassNotFoundException e) {
        logger.error("Class not found: "+e.getMessage());
        return null;
      } catch (MalformedURLException e) {
        logger.error(e.getMessage());
        return null;
      } catch (InvocationTargetException e) {
        logger.error(e.getMessage());
        return null;
      } catch (IllegalAccessException e) {
        e.printStackTrace();
        return null;
      } catch (NoSuchMethodException e) {
        e.printStackTrace();
        return null;
      }

      //System.out.println(jplugin.toString());

      return jplugin;
    }

    public boolean importFromPack(String zipFile) {
      String unzippedDir = Utils.getTempDirectory()+"/plugintemp";

      // Unpack file
      logger.info("Unpacking "+zipFile);

      if (Utils.unpack(zipFile, unzippedDir)) {

      }

      return true;
    }
}
