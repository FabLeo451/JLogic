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

import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.Properties;

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
     * Create JSON specification for plugin, given path and class file
     */
    public Result getSpecFromJAR(String jarFile) {
        final int FUNCTION = 4;
        final int OPERATOR = 5;

        Plugin plugin = new Plugin();
        plugin.setJarFile(jarFile);

        Method m;
        String className;
        String methodName;
        //List<String> outGet = new ArrayList<String>();
        List<OutConnector> outConn = new ArrayList<OutConnector>();
        int nExec = 0;

        Result result = new Result();

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
            //File f = new File(Utils.getM2RepositoryDir()+"/com/lionsoft/jlogic/standard/1.0.0/standard.jar");
            //urls.add(f.toURI().toURL());

            // Classpath
            //System.out.println("className = "+className);
            //System.out.println("classPath = "+classPath);
            urls.add(new File(jarFile).toURI().toURL());

            clUrls = new URL[urls.size()];
            clUrls = urls.toArray(clUrls);

            //Class c = Class.forName(plugin.getClassName());
            URLClassLoader cl = new URLClassLoader(clUrls);

            // Get Manifest
            try {
                URL url = cl.findResource("META-INF/MANIFEST.MF");
                Manifest manifest = new Manifest(url.openStream());
                Attributes attr = manifest.getMainAttributes();
                className = attr.getValue("Main-Class");
                //logger.info("Main-class = "+className);

                plugin.setName(attr.getValue("Implementation-Title"));
                plugin.setVersion(attr.getValue("Implementation-Version"));
                plugin.setClassName(className);
                plugin.setArtifactId(attr.getValue("artifactId"));
                plugin.setGroupId(attr.getValue("groupId"));

            } catch (IOException e) {
                result.setError("Error gettin manifest: "+e.getMessage());
                return result;
            }

            // Load class
            Class c = cl.loadClass(className);
/*
            try {
                Properties properties = new Properties();
                String d = "META-INF/maven/"+c.getPackage().getName().replace(".", "/")+"/pom.properties";
                System.out.println(d);
                properties.load(c.getResourceAsStream(d));
            } catch (IOException e) {
                result.setError(e.getMessage());
                return result;
            }
*/
            Class PluginAnnotation = cl.loadClass("com.lionsoft.jlogic.standard.annotation.Plugin");
            Class NodeAnnotation = cl.loadClass("com.lionsoft.jlogic.standard.annotation.Node");
            Class InAnnotation = cl.loadClass("com.lionsoft.jlogic.standard.annotation.In");
            Class OutAnnotation = cl.loadClass("com.lionsoft.jlogic.standard.annotation.Out");
            Class TypeAnnotation = cl.loadClass("com.lionsoft.jlogic.standard.annotation.Type");

            // Plugin info
            Annotation pluginAnnotation = c.getAnnotation(PluginAnnotation);

            if (pluginAnnotation != null) {
                /*Class<? extends Annotation> pluginInfo = pluginAnnotation.annotationType();
                m = pluginInfo.getMethod("name");
                jplugin.put("name", m.invoke(pluginAnnotation, (Object[])null));
                m = pluginInfo.getMethod("version");
                jplugin.put("version", m.invoke(pluginAnnotation, (Object[])null));*/
            }

            //logger.info("Installing "+plugin.toString());

            // Types
            for (Annotation typeAnnotation : c.getAnnotationsByType(TypeAnnotation)) {
                JSONObject jtype = new JSONObject();

                Class<? extends Annotation> type = typeAnnotation.annotationType();
                m = type.getMethod("name");
                jtype.put("name", m.invoke(typeAnnotation, (Object[])null));

                m = type.getMethod("color");
                jtype.put("color", m.invoke(typeAnnotation, (Object[])null));

                m = type.getMethod("initStr");
                if (!((String) m.invoke(typeAnnotation, (Object[])null)).isEmpty())
                    jtype.put("init", m.invoke(typeAnnotation, (Object[])null));

                m = type.getMethod("importLib");
                if (!((String) m.invoke(typeAnnotation, (Object[])null)).isEmpty())
                    jtype.put("import", m.invoke(typeAnnotation, (Object[])null));

                m = type.getMethod("jar");
                if (!((String) m.invoke(typeAnnotation, (Object[])null)).isEmpty())
                    jtype.put("jar", m.invoke(typeAnnotation, (Object[])null));

                jtypes.add(jtype);
            }

            // Nodes
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

            jplugin.put("className", className);
            jplugin.put("name", plugin.getName());
            jplugin.put("version", plugin.getVersion());
            jplugin.put("groupId", plugin.getGroupId());
            jplugin.put("artifactId", plugin.getArtifactId());

            plugin.setSpec(jplugin.toString());
            result.setData(plugin);

        } catch (ClassNotFoundException e) {
            result.setResult(Result.ERROR, "Class not found: "+e.getMessage());
        } catch (MalformedURLException e) {
            result.setResult(Result.ERROR, e.getMessage());
        } catch (InvocationTargetException e) {
            result.setResult(Result.ERROR, e.getMessage());
        } catch (IllegalAccessException e) {
            result.setResult(Result.ERROR, e.getMessage());
        } catch (NoSuchMethodException e) {
            result.setResult(Result.ERROR, e.getMessage());
        }

        //System.out.println(jplugin.toString());

        return result;
    }

    /**
     * Install a jar to Maven private repository
     */
    public Result mvnInstall(Plugin plugin) {
        Result result = new Result();
        List<String> args = new ArrayList<String>();

        // mvn install:install-file -Dfile=target/test-1.0.0.jar -DgroupId=org.jlogic.plugin
        //   -DartifactId=test -Dversion=1.0.0 -Dpackaging=jar
        //   -DlocalRepositoryPath=/media/data/Source/JLogic-all/JLogic/m2/repository
        //   -DpomFile=pom.xml
        args.add("mvn");
        args.add("--batch-mode"); // Disable ansi colors
        args.add("install:install-file"); // Disable ansi colors
        args.add("-Dfile="+plugin.getJarFile());
        args.add("-DgroupId="+plugin.getGroupId());
        args.add("-DartifactId="+plugin.getArtifactId());
        args.add("-Dversion="+plugin.getVersion());
        args.add("-Dpackaging=jar");
        args.add("-DlocalRepositoryPath="+Utils.getM2RepositoryDir());
        //args.add("-DpomFile="+Utils.getM2RepositoryDir);

        String s = "";

        for (int i=0; i<args.size(); i++)
            s += args.get(i) + " ";

        logger.info(s);

        ProcessBuilder processBuilder = new ProcessBuilder();
        //processBuilder.inheritIO().command(args);
        processBuilder.command(args);
        //processBuilder.directory(new File(program.getMyDir()));

        try {
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            BufferedReader outReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String line;

            while ((line = outReader.readLine()) != null)
                output.append(line + "\n");

            while ((line = errReader.readLine()) != null)
                output.append(line + "\n");

            int exitVal = process.waitFor();

            if (exitVal == 0) {
                result.setMessage("Plugin JAR successfully installed: "+plugin.getName());
            } else {
                result.setResult(exitVal, "Maven error installing jar: "+exitVal);
            }

            result.setOutput(output.toString());
        }
        catch (IOException e) {
            result.setResult(Result.ERROR, e.getMessage());
        }
        catch (InterruptedException e) {
            result.setResult(Result.ERROR, e.getMessage());
        }

        return result;
    }

    /**
     * Install a plugin from a local file
     */
    public Result install(String jarFile) {
        Result result = new Result();

        // Get plugin info e specification
        logger.info("Getting plugin specification...");
        result = getSpecFromJAR(jarFile);

        if (!result.success()) {
            return result;
        }

        Plugin plugin = (Plugin) result.getData();

        logger.info("Found: " + System.lineSeparator() +
        "Name       : " + plugin.getName() + System.lineSeparator() +
        "Group Id   : " + plugin.getGroupId() + System.lineSeparator() +
        "Artifact Id: " + plugin.getArtifactId() + System.lineSeparator() +
        "Version    : " + plugin.getVersion() + System.lineSeparator() +
        "Class      : " + plugin.getClassName()
        );

        // Create dir
        String pluginDir = Utils.getPluginsDir()+"/"+plugin.getName();

        logger.info("Creating "+pluginDir);

        if (!new File(pluginDir).exists() && !new File(pluginDir).mkdir()) {
            result.setError("Unable to create directory for plugin: "+pluginDir);
            return result;
        }

        // Extracting pom.xml
        /*
        logger.info("Extracting pom.xml");

        try {
            Utils.extractFileFromJar(jarFile, "pom.xml", pluginDir+"pom.xml");
        } catch (IOException e) {
            result.setError(e.getMessage());
            return result;
        }*/

        // Install package
        logger.info("Installing jar for "+plugin.toString());

        result = mvnInstall(plugin);

        if (!result.success()) {
            return result;
        }

        //logger.info(result.getMessage());

        // Install specification
        logger.info("Installing plugin specification...");

        try (FileWriter file = new FileWriter(pluginDir+"/"+plugin.getName()+".json")) {
            file.write(plugin.getSpec());
            file.flush();
            file.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
            return(new Result().setError(e.getMessage()));
        }

        return result;
    }
}
