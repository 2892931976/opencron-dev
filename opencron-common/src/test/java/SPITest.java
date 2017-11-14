import org.junit.Test;
import org.opencron.common.ext.ExtensionLoader;
import org.opencron.common.serialize.Serializer;

import java.io.IOException;

public class SPITest {

    @Test
    public void testSpi() throws IOException {
        Serializer serializer = ExtensionLoader.load(Serializer.class);
        byte[] str = serializer.encode("benjobs");
        System.out.println(str);
        ExtensionLoader.load(Serializer.class);
        System.out.println("xxx");
    }

    @Test
    public void testInstance(){


    }

}
