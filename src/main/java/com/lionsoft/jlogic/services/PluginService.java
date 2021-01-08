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
import org.springframework.util.StringUtils;
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
import java.lang.annotation.Annotation;

@Service
public class PluginService {

    static Logger logger = LoggerFactory.getLogger(PluginService.class);

    public PluginService() {
    }

    /**
     * Create JSON specification for plugintemp
     */
    public JSONObject specFromClass(Plugin plugin) {
      final int FUNCTION = 4;
      final int OPERATOR = 5;

      Method m;

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
          boolean returnsValue = !method.getReturnType().equals(Void.TYPE);

          Annotation nodeAnnotation = method.getAnnotation(NodeAnnotation);

          if (nodeAnnotation != null) {
            JSONObject jnode = new JSONObject();
            JSONArray jinput = new JSONArray();
            JSONArray joutput = new JSONArray();
            jnode.put("input", jinput);
            jnode.put("output", joutput);
            jnode.put("name", method.getName());

            Class<? extends Annotation> node = nodeAnnotation.annotationType();

            // Name, type etc.
            m = node.getMethod("name");
            jnode.put("name", m.invoke(nodeAnnotation, (Object[])null));
            jnode.put("type", returnsValue ? OPERATOR : FUNCTION);

            // Input parameters
            if (!returnsValue) {
              JSONObject jexec = new JSONObject();
              jexec.put("label", "");
              jexec.put("type", "Exec");
              jinput.add(jexec);
              joutput.add(jexec);
            }

            for (Parameter p : method.getParameters()) {
              //System.out.println(p.toString());
              String[] parts = p.getType().toString().split("\\.");

              JSONObject jparam = new JSONObject();
              jparam.put("label", p.getName());
              jparam.put("type", parts[parts.length-1].replace(";", ""));
              jparam.put("dimensions", StringUtils.countOccurrencesOf(p.getType().toString(), "["));

              Annotation paramAnnotation = p.getAnnotation(InAnnotation);

              if (paramAnnotation != null) {
                Class<? extends Annotation> bpconnector = paramAnnotation.annotationType();
                m = bpconnector.getMethod("label");
                jparam.put("label", m.invoke(paramAnnotation, (Object[])null));
              }

              jinput.add(jparam);
            }

            // Output
            for (Annotation outAnnotation : method.getAnnotationsByType(OutAnnotation)) {
                Class<? extends Annotation> out = outAnnotation.annotationType();

                JSONObject jout = new JSONObject();
                m = out.getMethod("label");
                jout.put("label", m.invoke(outAnnotation, (Object[])null));

                joutput.add(jout);
            }

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
