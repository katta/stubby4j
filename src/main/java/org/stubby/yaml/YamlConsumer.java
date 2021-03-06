/*
A Java-based HTTP stub server

Copyright (C) 2012 Alexander Zagniotov, Isa Goksu and Eric Mrak

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.stubby.yaml;

import org.stubby.yaml.stubs.StubHttpLifecycle;
import org.stubby.yaml.stubs.StubRequest;
import org.stubby.yaml.stubs.StubResponse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Alexander Zagniotov
 * @since 6/11/12, 9:46 PM
 */
public final class YamlConsumer {

   private static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

   public static String loadedConfig;

   private YamlConsumer() {

   }

   public static List<StubHttpLifecycle> readYaml(final File yamlFile) throws IOException {
      final String filename = yamlFile.getName().toLowerCase();
      if (filename.endsWith(".yaml") || filename.endsWith(".yml")) {
         loadedConfig = yamlFile.getAbsolutePath();
         final InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(yamlFile), Charset.forName("UTF-8"));
         final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
         logger.info("Loaded YAML " + filename);

         return transformYamlNode(bufferedReader, null);
      }
      return new LinkedList<StubHttpLifecycle>();
   }

   public static List<StubHttpLifecycle> readYaml(final InputStream yamlInputStream) throws IOException {
      final InputStreamReader inputStreamReader = new InputStreamReader(yamlInputStream, Charset.forName("UTF-8"));
      return transformYamlNode(new BufferedReader(inputStreamReader), null);
   }

   public static List<StubHttpLifecycle> readYaml(final String yamlConfigFilename) throws IOException {
      final File yamlFile = new File(yamlConfigFilename);
      return readYaml(yamlFile);
   }

   protected static List<StubHttpLifecycle> transformYamlNode(final BufferedReader bufferedReader, StubHttpLifecycle parentStub) throws IOException {
      final List<StubHttpLifecycle> httpLifecycles = new LinkedList<StubHttpLifecycle>();

      String yamlLine;
      while ((yamlLine = bufferedReader.readLine()) != null) {
         yamlLine = yamlLine.trim();
         if (yamlLine.isEmpty()) {
            continue;
         }

         final String[] keyAndValue = yamlLine.split(":", 2);
         final String nodeKey = keyAndValue[0].toLowerCase().trim();

         switch (YamlParentNodes.getFor(nodeKey)) {

            case HTTPLIFECYCLE:
               parentStub = new StubHttpLifecycle(new StubRequest(), new StubResponse());
               httpLifecycles.add(parentStub);
               break;

            case REQUEST:
               parentStub.setCurrentlyPopulated(YamlParentNodes.REQUEST);
               break;

            case RESPONSE:
               parentStub.setCurrentlyPopulated(YamlParentNodes.RESPONSE);
               break;

            case HEADERS:
               break;

            default:
               final String nodeValue = (keyAndValue.length == 2 ? keyAndValue[1].trim() : "");
               bindYamlValueToPojo(nodeKey, nodeValue, parentStub);
               break;
         }
      }
      bufferedReader.close();
      return httpLifecycles;
   }

   private static void bindYamlValueToPojo(final String nodeName, final String nodeValue, final StubHttpLifecycle parentStub) {

      if (StubRequest.isFieldCorrespondsToYamlNode(nodeName)) {
         setYamlValueToFieldProperty(parentStub, nodeName, nodeValue, YamlParentNodes.REQUEST);

      } else if (StubResponse.isFieldCorrespondsToYamlNode(nodeName)) {
         setYamlValueToFieldProperty(parentStub, nodeName, nodeValue, YamlParentNodes.RESPONSE);

      } else {
         setYamlValueToHeaderProperty(parentStub, nodeName, nodeValue);
      }
   }

   private static void setYamlValueToFieldProperty(final StubHttpLifecycle stubHttpLifecycle, final String nodeName, final String nodeValue, final YamlParentNodes type) {
      try {
         if (type.equals(YamlParentNodes.REQUEST)) {
            stubHttpLifecycle.getRequest().setValue(nodeName, nodeValue);
         } else {
            stubHttpLifecycle.getResponse().setValue(nodeName, nodeValue);
         }
      } catch (InvocationTargetException e) {
         e.printStackTrace();
      } catch (IllegalAccessException e) {
         e.printStackTrace();
      }
      stubHttpLifecycle.setCurrentlyPopulated(type);
   }

   private static void setYamlValueToHeaderProperty(final StubHttpLifecycle stubHttpLifecycle, final String nodeName, final String nodeValue) {
      if (stubHttpLifecycle.getCurrentlyPopulated().equals(YamlParentNodes.REQUEST)) {
         stubHttpLifecycle.getRequest().addHeader(nodeName, nodeValue);
      } else if (stubHttpLifecycle.getCurrentlyPopulated().equals(YamlParentNodes.RESPONSE)) {
         stubHttpLifecycle.getResponse().addHeader(nodeName, nodeValue);
      }
   }
}