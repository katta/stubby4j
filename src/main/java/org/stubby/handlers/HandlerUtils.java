package org.stubby.handlers;

import org.stubby.exception.Stubby4JException;
import org.stubby.server.JettyOrchestrator;

import java.io.InputStream;
import java.util.Scanner;

/**
 * @author Alexander Zagniotov
 * @since 6/24/12, 1:00 AM
 */
final class HandlerUtils {

   static String getHtmlResourceByName(final String templateSuffix) {
      final String htmlTemplatePath = String.format("/html/%s.html", templateSuffix);
      final InputStream postBodyInputStream = HandlerUtils.class.getResourceAsStream(htmlTemplatePath);
      if (postBodyInputStream == null) {
         throw new Stubby4JException(String.format("Could not find resource %s", htmlTemplatePath));
      }
      return inputStreamToString(postBodyInputStream);
   }

   static String constructHeaderServerName() {
      final Package pkg = HandlerUtils.class.getPackage();
      final String implementationVersion = pkg.getImplementationVersion() == null ?
            "x.x.x" : pkg.getImplementationVersion();
      final String implementationTitle = pkg.getImplementationTitle() == null ?
            "Java-based HTTP stub server" : pkg.getImplementationTitle();
      return String.format("stubby4j/%s (%s)", implementationVersion, implementationTitle);
   }

   static String inputStreamToString(final InputStream inputStream) {
      // Regex \A matches the beginning of input. This effectively tells Scanner to tokenize
      // the entire stream, from beginning to (illogical) next beginning.
      return new Scanner(inputStream, "UTF-8").useDelimiter("\\A").next();
   }

   static String escapeHtmlEntities(final String toBeEscaped) {
      return toBeEscaped.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
   }

   static String linkifyRequestUrl(final Object uri) {
      return String.format("<a target='_blank' href='http://%s:%s%s'>%s</a>",
            JettyOrchestrator.currentHost, JettyOrchestrator.currentClientPort, uri, uri);
   }

   static String populateHtmlTemplate(final String templateName, final Object... params) {
      final StringBuilder builder = new StringBuilder();
      builder.append(String.format(getHtmlResourceByName(templateName), params));
      return builder.toString();
   }
}

