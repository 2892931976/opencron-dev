import org.junit.Test;
import org.opencron.common.extension.ExtensionLoader;
import org.opencron.common.serialize.Serializer;

import java.io.IOException;

public class SPITest {

    @Test
    public void testSpi() throws IOException {
        Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class).getExtension();
        byte[] str = serializer.encode("benjobs");
        System.out.println(str);
    }

}
