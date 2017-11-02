import org.junit.Test;
import org.opencron.common.extension.ExtensionLoader;
import org.opencron.common.serialize.Serializer;
import org.opencron.rpc.Client;
import org.opencron.rpc.Server;

import java.io.IOException;

public class SPITest {

    @Test
    public void testSpi() throws IOException {
        Server server = ExtensionLoader.getExtensionLoader(Server.class).getExtension();
        System.out.println(server.getClass());

        Client client = ExtensionLoader.getExtensionLoader(Client.class).getExtension();
        System.out.println(client.getClass());
    }

}
