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

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

@Service
public class PluginService {

	class OutConnector {
		public String label;
		public String type;
		public int array;
		public boolean exec;
		public boolean assign;

		public OutConnector() {
			exec = false;
			assign = true;
		}

		public void setExec() {
			exec = true;
			type = "Exec";
		}
	}

	static Logger logger = LoggerFactory.getLogger(PluginService.class);

	public PluginService() {}

	public String getPluginDir(Plugin plugin) {
		return(Utils.getPluginsDir() + "/" + plugin.getName());
	}

	/**
	 * Get plugin info from jar file
	 */
	public Plugin getInfoFromJAR(String jarFile) {
		Plugin plugin = new Plugin();
		plugin.setJarFile(jarFile);

		try {
			// Set classpath
			/*
			List<URL> urls = new ArrayList<>();
			URL[] clUrls = null;

			// Classpath
			urls.add(new File(jarFile).toURI().toURL());
			clUrls = new URL[urls.size()];
			clUrls = urls.toArray(clUrls);
			*/
			URL[] clUrls = new URL[1];
			clUrls[0] = new File(jarFile).toURI().toURL();

			//Class c = Class.forName(plugin.getClassName());
			URLClassLoader pluginCl = new URLClassLoader(clUrls);

			// Get Manifest
			try {
				URL url = pluginCl.findResource("META-INF/MANIFEST.MF");

				if (url == null) {
					logger.error("Can't find META-INF/MANIFEST.MF");
					return null;
				}

				Manifest manifest = new Manifest(url.openStream());
				Attributes attr = manifest.getMainAttributes();
				plugin.setClassName(attr.getValue("Main-Class"));
				//logger.info("Main-class = "+className);

				plugin.setName(attr.getValue("Implementation-Title"));
				plugin.setVersion(attr.getValue("Implementation-Version"));
				//plugin.setClassName(className);
				plugin.setArtifactId(attr.getValue("artifactId"));
				plugin.setGroupId(attr.getValue("groupId"));

				pluginCl.close();

				return plugin;

			} catch (IOException e) {
				logger.error("Error getting manifest: " + e.getMessage());
				return null;
			}
		} catch (MalformedURLException e) {
			logger.error(e.getMessage());
		}

		return null;
	}

	/**
	 * Create JSON specification for plugin, given a jar file
	 */
	public String getSpec(Plugin plugin) {
		final int PROCEDURE = 4;
		final int OPERATOR = 5;

		Method m;
		String methodName;
		List<OutConnector> outConn;
		int nExecOut = 0;
		String spec = null;
		URL[] clUrls = null;

		JSONObject jplugin = new JSONObject();
		JSONArray jnodes = new JSONArray();
		JSONArray jtypes = new JSONArray();

		jplugin.put("types", jtypes);
		jplugin.put("nodes", jnodes);

		try {
			List<URL> urls = Utils.getURLs(getPluginDir(plugin) + "/classpath");

			if (urls == null) {
				logger.error("Classpath file not found");
				return null;
			}

			// Add jar path
			try {
				urls.add(new File(plugin.getJarFile()).toURI().toURL());

				clUrls = new URL[urls.size()];
				clUrls = urls.toArray(clUrls);
			} catch (MalformedURLException e) {
				logger.error(e.getMessage());
				return null;
			}

			// Add Standard path ad create the main class loader with all classpaths
			/*
			urls.add(new File(Utils.getM2RepositoryDir()+"/com/lionsoft/jlogic/standard/1.0.0/standard-1.0.0.jar").toURI().toURL());
			urls.add(new File(jarFile).toURI().toURL());
			clUrls = new URL[urls.size()];
			clUrls = urls.toArray(clUrls);*/

			URLClassLoader cl = new URLClassLoader(clUrls);

			// Load class
			logger.info("Loading class " + plugin.getClassName() + "...");
			Class c = cl.loadClass(plugin.getClassName());
			//Class c = Class.forName(plugin.getClassName(), false, cl);

			Class PluginAnnotation = cl.loadClass("com.lionsoft.jlogic.standard.annotation.Plugin");
			Class NodeAnnotation = cl.loadClass("com.lionsoft.jlogic.standard.annotation.Node");
			Class InAnnotation = cl.loadClass("com.lionsoft.jlogic.standard.annotation.In");
			Class OutAnnotation = cl.loadClass("com.lionsoft.jlogic.standard.annotation.Out");
			Class TypeAnnotation = cl.loadClass("com.lionsoft.jlogic.standard.annotation.Type");
			Class InputArrayAnnotation = cl.loadClass("com.lionsoft.jlogic.standard.annotation.InputArray");
			Class IncludeAnnotation = cl.loadClass("com.lionsoft.jlogic.standard.annotation.Include");

			// Plugin info
			Annotation pluginAnnotation = c.getAnnotation(PluginAnnotation);

			JSONArray jimport = new JSONArray();

			if (pluginAnnotation != null) {
				Class<? extends Annotation > pluginInfo = pluginAnnotation.annotationType();
				m = pluginInfo.getMethod("importList");
				Object obj = m.invoke(pluginAnnotation, (Object[]) null);
				String importList[] = (String[]) obj;

				for (int i = 0; i<importList.length; i++)
					jimport.add(importList[i]);
			}

			//logger.info("Installing "+plugin.toString());

			// Types
			logger.info("Getting plugin types...");
			for (Annotation typeAnnotation: c.getAnnotationsByType(TypeAnnotation)) {
				JSONObject jtype = new JSONObject();

				Class<? extends Annotation > type = typeAnnotation.annotationType();
				m = type.getMethod("name");
				jtype.put("name", m.invoke(typeAnnotation, (Object[]) null));

				m = type.getMethod("color");
				jtype.put("color", m.invoke(typeAnnotation, (Object[]) null));

				m = type.getMethod("initStr");
				if (!((String) m.invoke(typeAnnotation, (Object[]) null)).isEmpty())
					jtype.put("init", m.invoke(typeAnnotation, (Object[]) null));

				m = type.getMethod("importLib");
				if (!((String) m.invoke(typeAnnotation, (Object[]) null)).isEmpty())
					jtype.put("import", m.invoke(typeAnnotation, (Object[]) null));

				m = type.getMethod("jar");
				if (!((String) m.invoke(typeAnnotation, (Object[]) null)).isEmpty())
					jtype.put("jar", m.invoke(typeAnnotation, (Object[]) null));

				jtypes.add(jtype);
			}

			// Include
			for (Annotation includeAnnotation: c.getAnnotationsByType(IncludeAnnotation)) {
				JSONObject jinclude = new JSONObject();

				Class<? extends Annotation > include = includeAnnotation.annotationType();
				m = include.getMethod("file");
				String resourceName = (String) m.invoke(includeAnnotation, (Object[]) null);

				try {
					InputStream inputStream = cl.getResourceAsStream(resourceName);

					JSONParser jsonParser = new JSONParser();
					JSONObject jitem = (JSONObject) jsonParser.parse(new InputStreamReader(inputStream, "UTF-8"));
					jnodes.add(jitem);

					logger.info("Included " + resourceName);
				} catch (IllegalArgumentException e) {
					logger.error("File not found: " + resourceName + ": " + e.getMessage());
				} catch (IOException e) {
					logger.error("IOException: " + e.getMessage());
				} catch (ParseException e) {
					logger.error("ParseException: " + e.getMessage());
				}
			}

			// Nodes
			logger.info("Getting plugin nodes...");
			Method[] methods = c.getDeclaredMethods();

			for (Method method: methods) {
				methodName = method.getName();

				Annotation nodeAnnotation = method.getAnnotation(NodeAnnotation);

				if (nodeAnnotation != null) {
					boolean includeInputArray = false;
					String returnType = Utils.getJavaTypeAsString(method.getReturnType());
					int returnArray = Utils.getJavaArrayFromString(method.getReturnType().toString());

					boolean multipleOut = returnType.equals("Object") && returnArray == 1;
					boolean isProcedure = method.getReturnType().equals(Void.TYPE) || multipleOut;

					//System.out.println(methodName+" isProcedure="+isProcedure+" multipleOut="+multipleOut+" "+method.getReturnType().toString());
					//System.out.println(Utils.getJavaTypeAsString(method.getReturnType()));

					JSONObject jnode = new JSONObject();

					JSONObject jpackage = new JSONObject();
					jpackage.put("artifactId", plugin.getArtifactId());
					jpackage.put("groupId", plugin.getGroupId());
					jpackage.put("version", plugin.getVersion());
					jnode.put("package", jpackage);

					JSONArray jinput = new JSONArray();
					JSONArray joutput = new JSONArray();
					jnode.put("input", jinput);
					jnode.put("output", joutput);
					jnode.put("name", methodName);

					Class<? extends Annotation > node = nodeAnnotation.annotationType();

					// Name, type etc.
					m = node.getMethod("name");
					jnode.put("name", m.invoke(nodeAnnotation, (Object[]) null));
					m = node.getMethod("icon");
					jnode.put("icon", m.invoke(nodeAnnotation, (Object[]) null));
					jnode.put("type", isProcedure ? PROCEDURE : OPERATOR);
					jnode.put("import", jimport);

					// Input parameters
					if (isProcedure) {
						JSONObject jexec = new JSONObject();
						jexec.put("label", "");
						jexec.put("type", "Exec");
						jinput.add(jexec);
						//joutput.add(jexec);
					}

					// Input
					int nIn = 0;

					for (Parameter p: method.getParameters()) {
						//System.out.println(p.toString());

						nIn++;

						JSONObject jparam = new JSONObject();
						jparam.put("label", p.getName());
						//String type = Utils.getJavaTypeAsString(p.getType().toString());
						String type = Utils.getJavaTypeAsString(p.getType());
						jparam.put("type", type);
						int inArray = Utils.getJavaArrayFromString(p.getType().toString());
						jparam.put("dimensions", inArray);

						Annotation inputArrayAnnotation = p.getAnnotation(InputArrayAnnotation);

						if (inputArrayAnnotation != null) {
							// "options": { "javaInputArray": true }
							JSONObject jopt = new JSONObject();
							jopt.put("javaInputArray", true);
							jnode.put("options", jopt);
							includeInputArray = true;
						}

						Annotation inAnnotation = p.getAnnotation(InAnnotation);

						if (inAnnotation != null) {
							boolean b, addMore = false;

							Class<? extends Annotation > bpconnector = inAnnotation.annotationType();

							m = bpconnector.getMethod("addMore");
							addMore = (boolean) m.invoke(inAnnotation, (Object[]) null);

							//if (includeParam) {
							// Normal parameter
							m = bpconnector.getMethod("label");
							jparam.put("label", m.invoke(inAnnotation, (Object[]) null));

							m = bpconnector.getMethod("value");

							if (inArray == 0) {
								String value = (String) m.invoke(inAnnotation, (Object[]) null);

								if (type.equals("String"))
									jparam.put("value", value);
								else if (type.equals("Integer"))
									jparam.put("value", value.isEmpty() ? 0 : Integer.parseInt(value));
								else if (type.equals("Long"))
									jparam.put("value", value.isEmpty() ? 0 : Long.parseLong(value));
								else if (type.equals("Boolean"))
									jparam.put("value", value.isEmpty() ? false : value.equalsIgnoreCase("false") ? false : true);
							}

							m = bpconnector.getMethod("single_line");
							b = (boolean) m.invoke(inAnnotation, (Object[]) null);
							if (b)
								jparam.put("single_line", true);

							m = bpconnector.getMethod("not_null");
							b = (boolean) m.invoke(inAnnotation, (Object[]) null);
							if (b)
								jparam.put("not_null", true);

							m = bpconnector.getMethod("password");
							b = (boolean) m.invoke(inAnnotation, (Object[]) null);
							if (b)
								jparam.put("password", true);

							m = bpconnector.getMethod("enumValues");
							Object obj = m.invoke(inAnnotation, (Object[]) null);
							String enumValues[] = (String[]) obj;

							if (enumValues.length > 1) {
								JSONArray jenum = new JSONArray();

								for (int i = 0; i<enumValues.length; i++)
									jenum.add(enumValues[i]);

								jparam.put("enum", jenum);
							}

							jinput.add(jparam);

							if (addMore) {
								// "addInput": { "type": "String", "label": "Key", "value": "", "single_line": true },
								jnode.put("addInput", jparam);
							}
							//}
						}
					}

					// Output
					nExecOut = 0;
					outConn = new ArrayList<OutConnector> ();
					for (Annotation outAnnotation: method.getAnnotationsByType(OutAnnotation)) {
						Class<? extends Annotation > out = outAnnotation.annotationType();

						OutConnector conn = new OutConnector();

						JSONObject jout = new JSONObject();
						m = out.getMethod("label");
						conn.label = (String) m.invoke(outAnnotation, (Object[]) null);

						m = out.getMethod("exec");

						if ((boolean) m.invoke(outAnnotation, (Object[]) null)) {
							// Exec
							conn.setExec();
							nExecOut++;
						} else {
							// Data
							m = out.getMethod("type");
							String varType = (String) m.invoke(outAnnotation, (Object[]) null);

							if (varType.isEmpty())
								conn.type = returnType + (returnArray > 0 ? "[]" : "");
							else
								conn.type = varType;

							m = out.getMethod("array");
							conn.array = (Integer) m.invoke(outAnnotation, (Object[]) null);

							// Declare a new variable and assign value
							m = out.getMethod("variable");
							String varName = (String) m.invoke(outAnnotation, (Object[]) null);

							if (!varName.isEmpty()) {
								JSONObject jreferences = new JSONObject();
								jreferences.put("variable", varName);
								JSONObject jjava = new JSONObject();
								jjava.put("references", jreferences);
								jout.put("java", jjava);
							}

							// References an input parameter
							m = out.getMethod("input");
							int inputRef = (int) m.invoke(outAnnotation, (Object[]) null);

							if (inputRef >= 0) {
								conn.assign = false;
								JSONObject jreferences = new JSONObject();
								jreferences.put("input", inputRef);
								JSONObject jjava = new JSONObject();
								jjava.put("references", jreferences);
								jout.put("java", jjava);
							}

						}

						outConn.add(conn);

						jout.put("label", conn.label);
						jout.put("type", conn.type);

						if (conn.array > 0)
							jout.put("dimensions", conn.array);

						joutput.add(jout);
					}

					logger.info("Found node '" + ((String) jnode.get("name")) + "' on method '" + methodName + "' isProcedure=" + isProcedure);

					// Source code
					int nOut = joutput.size();
					/* isProcedure ? 1..n : 0..n-1 */
					int start = isProcedure ? 1 : 0;
					int end = isProcedure ? nIn : nIn - 1;

					//System.out.println(methodName+" nIn="+nIn+" nOut="+nOut+" start="+start);

					String java = ""; // Final source code
					String retVals = "", call = "", args = "", outVals = "", execAfter = "";

					for (int i = start; i<= end; i++) {
						args += i > start ? ", " : "";

						if (includeInputArray && i == end)
							args += ", _{node.id}_in";
						else
							args += "in{" + i + "}";
					}

					/*if (includeInputArray)
					    args += ", _{node.id}_in";*/

					call = plugin.getClassName() + "." + methodName + "(" + args + ")";
					/*
					                    if (!isProcedure) {
					                        java = call;
					                    } else {*/

					if (isProcedure) {
						if (multipleOut) {
							retVals = "Object[] _{node.id}_out = ";

							// Assing output values
							for (int i = 0; i<outConn.size(); i++) {
								if (outConn.get(i).exec || !outConn.get(i).assign)
									continue;

								outVals += "out{" + i + "} = (" + outConn.get(i).type + (outConn.get(i).array > 0 ? "[]" : "") + ")_{node.id}_out[" + i + "];" + System.lineSeparator();
							}

							// Following exec
							if (nExecOut > 1) {
								int added = 0;

								for (int i = 0; i<outConn.size(); i++) {
									if (outConn.get(i).exec) {
										execAfter += "if ( _{node.id}_out[" + i + "] != null) { exec{" + i + "} }";
										added++;

										if (added<nExecOut)
											execAfter += " else ";
									}
								}
							}
						} //else {

						java = retVals + call + ";" + System.lineSeparator() +
							outVals + System.lineSeparator() +
							execAfter;
						//}
					} else {
						java = call;
					}

					jnode.put("java", java);

					// Add node
					jnodes.add(jnode);
				}
			}

			spec = jplugin.toString();

			try {
				cl.close();
			} catch (IOException e) {}

		} catch (ClassNotFoundException e) {
			logger.error("Class not found: " + e.getMessage());
		} catch (InvocationTargetException e) {
			logger.error(e.getMessage());
		} catch (IllegalAccessException e) {
			logger.error(e.getMessage());
		} catch (NoSuchMethodException e) {
			logger.error("Method not found: " + e.getMessage());
		}

		//System.out.println(jplugin.toString());

		return spec;
	}

	/**
	 * Install a jar to Maven local repository
	 */
	public Result mvnInstall(Plugin plugin) {
		Result result = new Result();
		List<String> args = new ArrayList<String> ();

		// mvn install:install-file -Dfile=target/test-1.0.0.jar -DgroupId=org.jlogic.plugin
		//   -DartifactId=test -Dversion=1.0.0 -Dpackaging=jar
		//   -DlocalRepositoryPath=/media/data/Source/JLogic-all/JLogic/m2/repository
		//   -DpomFile=pom.xml
		args.add("mvn");
		args.add("--batch-mode"); // Disable ansi colors
		args.add("install:install-file"); // Disable ansi colors
		args.add("-Dfile=" + plugin.getJarFile());
		args.add("-DgroupId=" + plugin.getGroupId());
		args.add("-DartifactId=" + plugin.getArtifactId());
		args.add("-Dversion=" + plugin.getVersion());
		args.add("-Dpackaging=jar");
		//args.add("-DlocalRepositoryPath="+Utils.getM2RepositoryDir());
		args.add("-DpomFile=" + getPluginDir(plugin) + "/pom.xml");

		result = Utils.execute(args, null);
		return result;
	}

	/**
	 * Create classpath file
	 */
	public Result createCP(Plugin plugin) {
		Result result = new Result();
		String pluginDir = getPluginDir(plugin);
		List<String> args = new ArrayList<String> ();

		args.add("mvn");
		args.add("--batch-mode"); // Disable ansi colors
		args.add("dependency:build-classpath");
		args.add("-Dmdep.outputFile=" + pluginDir + "/classpath");

		result = Utils.execute(args, pluginDir);
		return result;
	}

	/**
	 * Install a plugin from a local file
	 */
	public Result install(String jarFile) {
		Result result = new Result();

		Plugin plugin = getInfoFromJAR(jarFile);

		if (plugin == null) {
			result.setError("Unable to get plugin info");
			return result;
		}

		logger.info("Plugin found: " + System.lineSeparator() +
			"Name       : " + plugin.getName() + System.lineSeparator() +
			"Group Id   : " + plugin.getGroupId() + System.lineSeparator() +
			"Artifact Id: " + plugin.getArtifactId() + System.lineSeparator() +
			"Version    : " + plugin.getVersion() + System.lineSeparator() +
			"Class      : " + plugin.getClassName()
		);

		if (plugin.getGroupId() == null || plugin.getArtifactId() == null || plugin.getVersion() == null) {
			result.setError("Missing manifest values");
			return result;
		}

		// Create dir
		String pluginDir = getPluginDir(plugin);

		logger.info("Creating " + pluginDir);

		if (!new File(pluginDir).exists() && !new File(pluginDir).mkdir()) {
			result.setError("Unable to create directory for plugin: " + pluginDir);
			return result;
		}

		// Extracting pom.xml
		logger.info("Extracting pom.xml");

		try {
			if (!Utils.extractFileFromJar(jarFile,
					"META-INF/maven/" + plugin.getGroupId() + "/" + plugin.getArtifactId() + "/pom.xml",
					pluginDir + "/pom.xml")) {
				result.setError("Unable to extraxt pom.xml");
				return result;
			}
		} catch (IOException e) {
			result.setError(e.getMessage());
			return result;
		}

		// Install package
		logger.info("Installing jar for " + plugin.toString());

		result = mvnInstall(plugin);

		if (!result.success()) {
			return result;
		}

		createCP(plugin);

		//logger.info(result.getMessage());

		// Get plugin info e specification
		logger.info("Getting plugin specification...");
		String spec = getSpec(plugin);

		if (spec == null) {
			result.setError("Can't get plugin specification");
			return result;
		}

		plugin.setSpec(spec);

		//System.out.println(spec);

		// Install specification

		logger.info("Installing plugin specification...");

		try (FileWriter file = new FileWriter(pluginDir + "/" + plugin.getName() + ".json")) {
			file.write(plugin.getSpec());
			file.flush();
			file.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
			return (new Result().setError(e.getMessage()));
		}

		result.setMessage("Plugin " + plugin.getName() + " successfully installed");
		result.setData(plugin);

		return result;
	}

    /**
	 * List installed plugins
	 */
	public List<Plugin> list() {
        // Get local repository path
/*
		String localRepPath = Utils.getLocalRepositoryDir();

		if (localRepPath == null) {
	        List<String> args = new ArrayList<String> ();

	        // mvn help:evaluate -Dexpression=settings.localRepository -q -DforceStdout
	        args.add("mvn");
	        args.add("--batch-mode"); // Disable ansi colors
	        args.add("help:evaluate");
	        args.add("-Dexpression=settings.localRepository");
	        args.add("-q");
	        args.add("-DforceStdout");

	        Result result = Utils.execute(args);

	        if (result.errors()) {
	            logger.error("Unable to get Maven local repository path:");
	            logger.error(result.getStdErr());
	            return null;
	        }

			localRepPath = result.getStdOut().replace("\n", "");
			Utils.setLocalRepositoryDir(localRepPath);
		}

        logger.info("Maven local repository: "+localRepPath);
*/
        List<Plugin> plugins = new ArrayList<Plugin>();

        //String pluginPath = localRepPath + "/org/jlogic/plugin";

        try (Stream<Path> paths = Files.walk(Paths.get(/*pluginPath*/Utils.getPluginsDir()))) {
			//List<String> list = paths.map(p -> p.toString()).filter(f -> f.endsWith(".pom")).collect(Collectors.toList());
			List<String> list = paths.map(p -> p.toString()).filter(f -> f.endsWith("/pom.xml")).collect(Collectors.toList());

            for (String pom: list) {
                //System.out.println("Found "+pom);
                MavenXpp3Reader reader = new MavenXpp3Reader();
                Model model = reader.read(new FileReader(pom));

                Plugin p = new Plugin();
                p.setName(model.getName());
                p.setVersion(model.getVersion());
                p.setGroupId(model.getGroupId());
				p.setArtifactId(model.getArtifactId());
                p.setDescription(model.getDescription());
                p.setUrl(model.getUrl());
                plugins.add(p);
            }
        } catch (XmlPullParserException e) {
            logger.error("XmlPullParserException: " + e.getMessage());
            return null;
        } catch (IOException e) {
            logger.error("IOException: " + e.getMessage());
            return null;
        }

        return plugins;
    }
}
