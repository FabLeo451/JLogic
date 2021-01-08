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
      JSONObject jplugin = new JSONObject();
      JSONArray jnodes = new JSONArray();
      JSONArray jtypes = new JSONArray();

      jplugin.put("types", jtypes);
      jplugin.put("nodes", jnodes);

      try {
        List<URL> urls = new ArrayList<>();
        URL[] clUrls = null;

        File f = new File(plugin.getPath());
        urls.add(f.toURI().toURL());
        clUrls = new URL[urls.size()];
        clUrls = urls.toArray(clUrls);

        //Class c = Class.forName(plugin.getClassName());
        ClassLoader cl = new URLClassLoader(clUrls);
        Class c = cl.loadClass(plugin.getClassName());

        Method[] methods = c.getMethods();

        for (Method m : methods) {
          JSONObject jnode = new JSONObject();
          jnode.put("name", m.getName());

          jnodes.add(jnode);
        }

      } catch (ClassNotFoundException e) {
        logger.error("Class not found: "+e.getMessage());
        return null;
      } catch (MalformedURLException e) {
        logger.error(e.getMessage());
        return null;
      }
       /*catch (InvocationTargetException e) {
        e.printStackTrace();
        return null;
      } catch (IllegalAccessException e) {
        e.printStackTrace();
        return null;
      } catch (NoSuchMethodException e) {
        e.printStackTrace();
        return null;
      }*/

      System.out.println(jplugin.toString());

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
