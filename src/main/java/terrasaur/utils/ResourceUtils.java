/*
 * The MIT License
 * Copyright © 2025 Johns Hopkins University Applied Physics Laboratory
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package terrasaur.utils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Methods to work with resource files stored inside a jar.
 * 
 * @author Hari.Nair@jhuapl.edu
 *
 */
public class ResourceUtils {

  private final static Logger logger = LogManager.getLogger(ResourceUtils.class);

  /**
   * Write a jar resource to a specified file.
   * 
   * @param path path to resource (e.g. /resources/kernels/lsk/naif0012.tls)
   * @param file file to hold the resource.
   * @param deleteOnExit delete file on program exit
   * @return
   */
  public static File writeResourceToFile(String path, File file, boolean deleteOnExit) {
    URL input = ResourceUtils.class.getResource(path);
    try {
      FileUtils.copyURLToFile(input, file);
      if (deleteOnExit)
        file.deleteOnExit();
    } catch (IOException e) {
      logger.warn(e.getLocalizedMessage());
    }
    return file;
  }

  /**
   * Write a jar resource to a specified file. Calls
   * {@link #writeResourceToFile(String, File, boolean)} with deleteOnExit set to
   * {@link Boolean#TRUE}.
   * 
   * @param path path to resource (e.g. /resources/kernels/lsk/naif0012.tls)
   * @param file file to hold the resource.
   * @return
   */
  public static File writeResourceToFile(String path, File file) {
    return writeResourceToFile(path, file, true);
  }

  /**
   * Write a jar resource to a temporary file. The file will be deleted on program exit.
   * 
   * @param path path to resource (e.g. /resources/kernels/lsk/naif0012.tls)
   * @return pointer to file created
   */
  public static File writeResourceToFile(String path) {

    try {
      return writeResourceToFile(path, File.createTempFile("resource-", ".tmp"));
    } catch (IOException e) {
      logger.warn(e.getLocalizedMessage());
    }
    return null;
  }

  /**
   * Return a {@link List} of all Paths contained within the desired resource (e.g. /targets)
   * 
   * @param path resource path to search
   * @return paths under the resource path
   */
  public static List<Path> getResourcePaths(String path) {
    List<Path> paths = new ArrayList<>();
    try {

      URI uri = ResourceUtils.class.getResource(path).toURI();
      if (uri.getScheme().equals("jar")) {

        /*-
         From <a href=
             "https://mkyong.com/java/java-read-a-file-from-resources-folder/">https://mkyong.com/java/java-read-a-file-from-resources-folder/</a>
        */

        String jarPath = ResourceUtils.class.getProtectionDomain().getCodeSource().getLocation()
            .toURI().getPath();

        uri = URI.create("jar:file:" + jarPath);
        try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
          paths = Files.walk(fs.getPath(path)).filter(Files::isRegularFile)
              .collect(Collectors.toList());
        }

      } else {
        Path myPath = Paths.get(uri);

        Stream<Path> walk = Files.walk(myPath, 1);
        for (Iterator<Path> it = walk.iterator(); it.hasNext();) {
          Path p = it.next();
          Path relative = myPath.relativize(p);
          if (relative.toString().trim().length() == 0)
            continue;
          Path thisPath = Paths.get(path, relative.toString());
          if (Files.isDirectory(p)) {
            paths.addAll(getResourcePaths(thisPath.toString()));
          } else {
            paths.add(thisPath);
          }
        }
        walk.close();
      }
      Collections.sort(paths);
    } catch (URISyntaxException | IOException e) {
      e.printStackTrace();
    }
    return paths;
  }


}
