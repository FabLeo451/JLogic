package com.lionsoft.jlogic;

import org.springframework.boot.system.ApplicationHome;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import java.util.*;
import java.io.*;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

import java.util.zip.ZipOutputStream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Utils {

    static ApplicationHome home = new ApplicationHome(Utils.class);
    static Logger logger = LoggerFactory.getLogger(Utils.class);

    //@Autowired
    //private static ResourceLoader resourceLoader;

    static String getHomeDir() {
        File f = new File(home.getDir()+"/..");

        try {
              return(f.getCanonicalPath());
        } catch (IOException e) {
              logger.error(e.getMessage());
        }

        return (null);
    }

    static String getLibDir() { return getHomeDir()+"/lib"; }
    static String getPluginsDir() { return getHomeDir()+"/plugin"; }
    static String getM2RepositoryDir() { return getHomeDir()+"/m2/repository"; }

    static String getTempDirectory() {
    	return home.getDir()+"/../temp";
    }

    static String getJavaTypeFromString(String type) {
        String[] parts = type.split("\\.");
        return(parts[parts.length-1].replace(";", ""));
    }

    static int getJavaArrayFromString(String type) {
        return(StringUtils.countOccurrencesOf(type, "["));
    }

  /**
   * Convert a json string into a Map
   */
  static Map<String, Object> jsonToMap(String data) throws ParseException {
    JSONParser jsonParser = new JSONParser();
    JSONObject jdata;

    jdata = (JSONObject) jsonParser.parse(data);

    Map<String, Object> map = new HashMap();

    for (Iterator iterator = jdata.keySet().iterator(); iterator.hasNext();) {
        String key = (String) iterator.next();

        if (jdata.get(key) instanceof JSONArray) {
          JSONArray ja = (JSONArray) jdata.get(key);
          Object[] oa = new Object[ja.size()];

          for (int i=0; i<ja.size(); i++) {
            oa[i] = ja.get(i);
          }

          map.put(key, oa);
        }
        else
          map.put(key, jdata.get(key));
    }

    return(map);
  }

  static boolean deleteDirectory(File directoryToBeDeleted) {
      File[] allContents = directoryToBeDeleted.listFiles();

      if (allContents != null) {
          for (File file : allContents) {
              deleteDirectory(file);
              //file.delete();
          }
      }
      return directoryToBeDeleted.delete();
  }

  static boolean deleteDirectory(String d) {
      File dir = new File(d);
      return(deleteDirectory(dir));
  }

  /**
   * Load a text file
   */
  static String loadTextFile(String filename) {
    try {
      String content = new String (Files.readAllBytes(Paths.get(filename)));
      return content;
    }
    catch (IOException e) {
      e.printStackTrace();
      //return null;
    }

    return null;
  }

  /**
   * Load a text file from resources
   */
  static String loadTextFileFromResources(String filename) {
    ResourceLoader resourceLoader = new DefaultResourceLoader();
    Resource resource = resourceLoader.getResource("classpath:"+filename);

    try (Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
        return FileCopyUtils.copyToString(reader);
    } catch (IOException e) {
        throw new UncheckedIOException(e);
    }
  }

  /**
   * Load a JSONObject from a file
   */
  static JSONObject loadJsonFile(String filename) {
    JSONObject jo = null;

    try {
      String content = new String (Files.readAllBytes(Paths.get(filename)));

      try {
        JSONParser jsonParser = new JSONParser();
        jo = (JSONObject) jsonParser.parse(content);
      }
      catch (ParseException e) {
        e.printStackTrace();
        return null;
      }
    }
    catch (IOException e) {
      e.printStackTrace();
      return null;
    }

    return jo;
  }

    /**
     * Write a string into a files
     */
    static void saveFile(String s, String filename) throws IOException {
        FileWriter file = new FileWriter(filename);
        file.write(s);
        file.flush();
        file.close();
    }

	/**
	 * This method guards against writing files to the file system outside of the target folder.
	 * This vulnerability is called Zip Slip.
	 */
  public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
      File destFile = new File(destinationDir, zipEntry.getName());

      String destDirPath = destinationDir.getCanonicalPath();
      String destFilePath = destFile.getCanonicalPath();

      if (!destFilePath.startsWith(destDirPath + File.separator)) {
          throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
      }

      return destFile;
  }

  /**
   * Uncompress file
   */
  static boolean unpack(String zipFile, String targetDir) {
    File destDir = new File(targetDir);
    byte[] buffer = new byte[1024];

    try {
      ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
      ZipEntry zipEntry = zis.getNextEntry();

      while (zipEntry != null) {
         File newFile = newFile(destDir, zipEntry);

         if (zipEntry.isDirectory()) {
           if (!newFile.isDirectory() && !newFile.mkdirs()) {
               logger.error("Failed to create directory " + newFile);
               return false;
           }
         } else {
           logger.info("Extracting "+zipEntry.getName());

           File parent = newFile.getParentFile();
           if (!parent.isDirectory() && !parent.mkdirs()) {
               logger.error("Failed to create directory " + parent);
           }

           // write file content
           FileOutputStream fos = new FileOutputStream(newFile);
           int len;
           while ((len = zis.read(buffer)) > 0) {
               fos.write(buffer, 0, len);
           }
           fos.close();
         }

         zipEntry = zis.getNextEntry();
      }

      zis.closeEntry();
      zis.close();
    } catch (FileNotFoundException e) {
        logger.error(e.getMessage());
        return false;
    } catch (IOException e) {
        logger.error(e.getMessage());
        return false;
    }

    return true;
  }
}
