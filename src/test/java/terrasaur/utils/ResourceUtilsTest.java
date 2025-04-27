package terrasaur.utils;

import static org.junit.Assert.assertEquals;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;

public class ResourceUtilsTest {

  @Test
  public void testWriteResourceToFile() throws IOException {
    String path = "/resources/kernels/lsk/naif0012.tls";

    ByteSource byteSrc = Resources.asByteSource(ResourceUtilsTest.class.getResource(path));
    InputStream is;

    is = byteSrc.openBufferedStream();

    byte[] buffer = new byte[is.available()];
    HashCode resource = Hashing.sha256().hashBytes(buffer);

    File lsk = ResourceUtils.writeResourceToFile(path);

    is = FileUtils.openInputStream(lsk);
    buffer = new byte[is.available()];
    HashCode copy = Hashing.sha256().hashBytes(buffer);

    assertEquals(resource, copy);
  }

}
