package com.lionsoft.jlogic;

import org.springframework.boot.system.ApplicationHome;
//import java.nio.file.Files;
//import java.nio.file.Paths;
import java.io.*;

//import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.concurrent.locks.*;

import java.util.Properties;
import java.util.Enumeration;

class PropertiesManager {
  private ReadWriteLock rwLock = new ReentrantReadWriteLock(); 

  String filename;
  Properties prop = null;
  InputStream input;
  
  public PropertiesManager (String filename) {
    this.filename = filename;
    load();
  }

  public void load() {
    try {
      input = new FileInputStream(filename);
      prop = new Properties();
      prop.load(input);
      input.close();
    } catch (IOException ex) {
      ex.printStackTrace();
    } 
  }
  
  public void save() {
    try (OutputStream output = new FileOutputStream(filename)) {
        // save properties to project root folder
        prop.store(output, null);
    } catch (IOException io) {
        io.printStackTrace();
    }
  }
  
  public void merge(Properties p) {
    @SuppressWarnings("unchecked")
    Enumeration<String> enums = (Enumeration<String>) p.propertyNames();
    while (enums.hasMoreElements()) {
      String key = enums.nextElement();
      String value = p.getProperty(key);
      prop.setProperty(key, value);
    }
  }
  
  public String get(String p) {
    return (prop.getProperty(p));
  }
  
  public void put(String key, String value) {
    prop.setProperty(key, value);
  }
  
  public void remove(String key) {
    prop.remove(key);
  }
  
  public Properties getProperties() {
    return prop;
  }
  
  public Lock lockRead () {
    Lock readLock = rwLock.readLock();
    readLock.lock();
    return readLock;
  }
  
  public Lock lockWrite () {
    Lock writeLock = rwLock.writeLock();
    writeLock.lock();
    return writeLock;
  }
  
  public void unlock (Lock l) {
    l.unlock();
  }
}


class GlobalProperties extends PropertiesManager {

  private static GlobalProperties instance = null;
  static ApplicationHome home = new ApplicationHome(PropertiesManager.class);

  public GlobalProperties () {
    super(home.getDir()+"/../data/global.properties");
    load();
  }
  
  public static GlobalProperties getInstance() {
    if (instance == null) {
      instance = new GlobalProperties();
    }
    return instance;
  }
}

